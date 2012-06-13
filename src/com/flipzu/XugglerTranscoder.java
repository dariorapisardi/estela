package com.flipzu;
/**
* Copyright 2011 Flipzu
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  
*  Initial Release: Dario Rapisardi <dario@rapisardi.org>
*  
*/


import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IAudioSamples.Format;
import com.xuggle.xuggler.ICodec.ID;

/**
 * Estela Xuggler Transcoder
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class XugglerTranscoder extends FzTranscoder {
	private IContainer mOContainer = null;
	private IContainerFormat oFmt = null;

	private IPacket firstPacket = null; // contains stream metadata
	private IPacket lastPacket = null;
	
	private IStream os = null;
	private IStreamCoder oc = null;

	private final Debug debug = Debug.getInstance();
	
	private boolean initEncode() {
		
		debug.logTranscoder("XugglerTranscoder, initEncode() called");
		
		if (mOContainer == null)
			mOContainer = IContainer.make();

		if (oFmt == null) {
			oFmt = IContainerFormat.make();
			oFmt.setOutputFormat(Config.getInstance().getOutputCodec(), null, null);
		}
		
		if ( !mOContainer.isOpened() ) {
			int retval = 0;

			retval = mOContainer.open("/dev/null", IContainer.Type.WRITE, oFmt);
			if (retval < 0) {
				debug.logTranscoder("XugglerTranscoder, initEncode, can't open output container");
				return false;
			}
			
		}

		/* create output audio coder */
		if ( os == null ) {
			os = mOContainer.addNewStream(0);
			oc = os.getStreamCoder();
			if ( Config.getInstance().getOutputCodec().equals("mp3"))
				oc.setCodecID(ID.CODEC_ID_MP3);
			else if ( Config.getInstance().getOutputCodec().equals("aac")) 
				oc.setCodec(ID.CODEC_ID_AAC);
			else
				oc.setCodec(ID.CODEC_ID_MP3); // default
			oc.setChannels(Config.getInstance().getOutputChannels());
			oc.setBitRate(Config.getInstance().getOutputBitrate());
			oc.setSampleRate(Config.getInstance().getOutputSampleRate());

			if (oc.open() < 0) {
				debug.logTranscoder("XugglerTranscoder, initEncode, can't open output codec");
				return false;
			}
		}
		
		return true;
	}
	
	public ChannelBuffer transcode ( ChannelBuffer buf ) throws IOException {
		
		
		ChannelBuffer decoded_data = decode(buf);
		
		if ( decoded_data == null ) {
			debug.logTranscoder("XugglerTranscoder, transcode, got NULL buffer from decode()");
		} else {
			debug.logTranscoder("XugglerTranscoder, transcode, got " + decoded_data.readableBytes() + " decoded bytes");	
		}
		
		ChannelBuffer encoded_data = encode( decoded_data );
		
		if ( encoded_data == null ) {
			debug.logTranscoder("XugglerTranscoder, transcode, got NULL buffer from encode()");
		} else {
			debug.logTranscoder("XugglerTranscoder, transcode, got " + encoded_data.readableBytes() + " encoded bytes");
		}
		
		return encoded_data;
	}
	
	/* receives a ChannelBuffer with encoded data (like AAC) */ 
	/* returns a ChannelBuffer with PCM raw data */
	public synchronized ChannelBuffer decode ( ChannelBuffer buf ) {
		
		/* create dynamic return buffer */
		ChannelBuffer ret_buf = null;
		
		if (buf == null) {
			return buf;		
		}

		if (buf.readableBytes() == 0)
			return ret_buf;
		
		IContainer mIContainer = IContainer.make();
		IContainerFormat mICfmt = IContainerFormat.make();			
		
		int retval = 0;

		ChannelBufferInputStream cb = new ChannelBufferInputStream(buf);

		retval = mIContainer.open((InputStream) cb, mICfmt, false, false);
		if (retval < 0) {
			debug.logTranscoder("XugglerTranscoder, decode, can't open stream");
			return ret_buf;
		}
		
		int numStreams = mIContainer.getNumStreams();

		if (numStreams != 1) {
			debug.logTranscoder("XugglerTranscoder, decode, wrong number of streams");
			mIContainer.close();
			return ret_buf;
		}

		mIContainer.queryStreamMetaData();

		/* look for AUDIO stream */		
		IStream is = mIContainer.getStream(0);
		IStreamCoder ic = is.getStreamCoder();
		ICodec.Type cType = ic.getCodecType();
		
		IAudioSamples inSamples = null;
		if ( cType == ICodec.Type.CODEC_TYPE_AUDIO  ) {
			if (ic.open() < 0) {
				debug.logTranscoder("XugglerTranscoder, decode, can't open input codec");
				mIContainer.close();
				return ret_buf;
			}

			debug.logTranscoder("XugglerTranscoder, got channels " + ic.getChannels() + " sample rate " + ic.getSampleRate() + " for codec " + ic.getCodecID());

			inSamples = IAudioSamples.make(1024, ic.getChannels());
			if ( inSamples == null ) {
				debug.logTranscoder("XugglerTranscoder, decode, can't allocate IAudioSamples");
				return ret_buf;
			}
		} else {
			debug.logTranscoder("XugglerTranscoder, decode, can't find audio stream");
			return ret_buf;
		}
		
		IPacket iPacket = IPacket.make();
	
		retval = 0;
				
		/* consume stream metadata. Xuggler needs this */
		if ( firstPacket != null ) {
			retval = ic.decodeAudio(inSamples, firstPacket, 0);
			debug.logTranscoder("XugglerTranscoder, decode, firstPacket retval " + retval + " with size " + firstPacket.getSize());
		}
		
		/* see if we have to decode an uncomplete packet from previous call */
		if ( lastPacket != null ) {
			debug.logTranscoder("XugglerTranscoder, decode, writing lastPacket");
			
			/* create new concatenated packet */
			if ( mIContainer.readNextPacket(iPacket) == 0) {
				IBuffer buf1 = lastPacket.getData();
				IBuffer buf2 = iPacket.getData();
				
				IBuffer new_buf = IBuffer.make(null, buf1.getSize()+buf2.getSize());
				byte[] ba1 = new byte[buf1.getSize()];
				byte[] ba2 = new byte[buf2.getSize()];
				
				ba1 = buf1.getByteArray(0, buf1.getSize());
				ba2 = buf2.getByteArray(0, buf2.getSize());
				
				new_buf.put(ba1, 0, 0, ba1.length);
				new_buf.put(ba2, 0, ba1.length, ba2.length);
				
				IPacket new_packet = IPacket.make(new_buf);
				
				retval = ic.decodeAudio(inSamples, new_packet, 0);
				debug.logTranscoder("XugglerTranscoder, decode, lastPacket decode retval " + retval);
				
				if ( inSamples != null && retval > 0 ) {
					if (inSamples != null && inSamples.getSize() > 0) {
						if ( ret_buf == null ) 
							ret_buf = dynamicBuffer(inSamples.getSize());
						ret_buf.writeBytes(inSamples.getByteBuffer());
					}
				}
				
			}
		}
		
		/* start of the decoding loop */
		while (mIContainer.readNextPacket(iPacket) == 0) {
			if ( firstPacket == null ) {
				firstPacket = IPacket.make(iPacket, true);
				debug.logTranscoder("XugglerTranscoder, decode, firstPacket size " + firstPacket.getSize());
			}

			if ( inSamples != null ) 
				retval = ic.decodeAudio(inSamples, iPacket, 0);
			else 
				break;

//			debug.logTranscoder("XugglerTranscoder, decode, got retval " + retval);

			if (retval > 0) {				
				/* write to file */
				if (inSamples != null && inSamples.getSize() > 0) {
					if ( ret_buf == null ) 
						ret_buf = dynamicBuffer(inSamples.getSize());
					ret_buf.writeBytes(inSamples.getByteBuffer());
				}
			}
			lastPacket = IPacket.make(iPacket, true);
		}
		
		ic.close();
		is.delete();
		mIContainer.close();
		mIContainer = null;
		ic = null;
		is = null;
		
		return ret_buf;
	}
	
	
	/* receives a ChannelBuffer with PCM raw data */
	/* returns a ChannelBuffer with encoded data (like MP3) */
	public synchronized ChannelBuffer encode ( ChannelBuffer buf ) {
		
		if (!initEncode()) 
			return null;
		
		/* create dynamic return buffer */
		ChannelBuffer ret_buf = null;
		
		if (buf == null) {
			return buf;		
		}

		if (buf.readableBytes() == 0)
			return ret_buf;	

		/* copy raw data to IAudioSamples for encoding */
		IPacket oPacket = IPacket.make();
		byte[] raw_buf = new byte[buf.readableBytes()];
		buf.getBytes(0, raw_buf);
		IBuffer raw_data = IBuffer.make(null, raw_buf, 0, raw_buf.length);
		IAudioSamples outSamples = IAudioSamples.make(raw_data, oc.getChannels(), IAudioSamples.Format.FMT_S16);
		int numSamples = oc.getAudioFrameSize()*(raw_buf.length/(oc.getAudioFrameSize()*2));
		outSamples.setComplete(true, numSamples, oc.getSampleRate(), oc.getChannels(), Format.FMT_S16, IAudioSamples.samplesToDefaultPts(numSamples, oc.getSampleRate()));
		
		int retval = 0;
		
		int numSamplesConsumed = 0;
		
		/* encoding loop */
		while (numSamplesConsumed < outSamples.getNumSamples()) {
			retval = oc.encodeAudio(oPacket, outSamples, numSamplesConsumed);
//			debug.logTranscoder("XugglerTranscoder, encode, retval for encodeAudio " + retval);
			numSamplesConsumed += retval;

		
			if ( oPacket.isComplete()) {
				int sz = oPacket.getByteBuffer().capacity();
				if ( ret_buf == null ) 
					ret_buf = dynamicBuffer(sz);
				ret_buf.writeBytes(oPacket.getByteBuffer());
//				debug.logTranscoder("XugglerTranscoder, encoded " + sz + " bytes");
			}
		}
		
		return ret_buf;
	}

	protected void finalize() throws Throwable {		
		debug.logTranscoder("XugglerTranscoder(), finalize()");
		
		if ( mOContainer != null ) {
			mOContainer.close();
			mOContainer = null;	
		}
		
		if ( oc != null ) {
			oc.close();
			oc = null;
		}
		
		if ( os != null ) {
			os.delete();
			os = null;			
		}

		super.finalize();
	}	
}
