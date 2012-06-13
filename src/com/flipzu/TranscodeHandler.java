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


import static org.jboss.netty.buffer.ChannelBuffers.buffer;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;


/**
 * Estela Transcoding handler, using FFMPEG or Xuggler
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */

public class TranscodeHandler  extends SimpleChannelUpstreamHandler {
	private final Debug debug = Debug.getInstance();
	private Integer frameSize = Config.getInstance().getFrameSize();
	private String outputCodec = Config.getInstance().getOutputCodec();
	private FzTranscoder transcoder = null;

	public TranscodeHandler() {
		if ( Config.getInstance().getTcoder().equals("xuggler")) {
			transcoder = new XugglerTranscoder();
		} else if ( Config.getInstance().getTcoder().equals("ffmpeg")) {
			transcoder = new FfmpegTranscoder();
			transcoder.run();			
		}
	}
	
	@Override
	public void channelConnected ( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
		debug.logTranscoder("TranscodeHandler(), channelConnected()");
		ctx.sendUpstream(e);
	}
	
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		
		AudioPacket p = (AudioPacket) e.getMessage();
		
		if ( p.getUuid() == null ) 
			return;
		
		if ( p.getBuffer().readableBytes() == 0 ) 
			return;
		
		Integer minBytes = frameSize;
		if ( p.getBuffer().readableBytes() < frameSize )
			minBytes = p.getBuffer().readableBytes();
		

		
		/* fetch codec from audio frame */  
		Broadcast bcast = Shows.getInstance().getBroadcast(p.getUuid());
		
		if ( bcast == null ) {
			debug.logTranscoder("TranscodeHandler(), no Broadcast found for " + p.getUuid());
			return;
		}
		
		debug.logTranscoder("TranscodeHandler(), messageReceived() for " + bcast + ", minBytes " + minBytes);
		
		if ( bcast.getCodec() == null ) { // check format							
			// get frame format
			debug.logTranscoder("TranscodeHandler(), messageReceived() we have " + p.getBuffer().readableBytes() + " readable bytes for " + bcast);
			byte[] b = new byte[minBytes];
			p.getBuffer().readBytes(b);

			FzFormat fzFormat = new FzFormat();
			String codec = fzFormat.getFormat(b);
			p.getBuffer().readerIndex(0);
			if ( codec == null ) {
				// invalid codec, shut down this
				debug.logTranscoder("TranscodeHandler(), messageReceived(), GOT NO CODEC for " + bcast);
				e.getChannel().close();
				return;
			}
			if ( codec.equals(outputCodec) ) {
				bcast.setTranscode(false);					
			} else {
				bcast.setTranscode(true);
			}
			bcast.setCodec(codec);
			debug.logTranscoder("TranscodeHandler(), messageReceived(), got codec " + codec + " for " + bcast);
		}
		
		
		/* remove handler if we don't need transcoding */
		if ( bcast.getCodec() != null && !bcast.isTranscode()) {
			try {
				transcoder.finalize();
			} catch (Throwable err) {
				debug.logError("TranscodeHandler, messageReceived() exception ", err);
			}
			debug.logTranscoder("TranscodeHandler, messageReceived(), removing transcoder for " + bcast);
			ctx.getPipeline().remove(this);
			ctx.sendUpstream(e);
			return;
		}
		
		/* transcoding */
		try {
			p.setBuffer(transcoder.transcode(p.getBuffer()));
		} catch ( IOException err ) {
			e.getChannel().close();
			return;
		}
		
		/* stop here if we don't have anything to transmit */
		if ( p.getBuffer().readableBytes() == 0 ) {
			return;
		}
		
		ctx.sendUpstream(e);
    }
	
	@Override
	public void channelClosed ( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
		UUID uuid = SessionAttrs.uuid.get(e.getChannel());
		
		
		if ( uuid == null ) { 
			debug.logTranscoder("TranscodeHandler, channelClosed(), got no UUID!");
		} else {
			debug.logTranscoder("TranscodeHandler, channelClosed(), sending EOF for " + uuid.toString());	
		}
		
		/* send EOF and flush buffers if using FFMPEG */
		Broadcast bcast = Shows.getInstance().getBroadcast(uuid);		
		if ( Config.getInstance().getTcoder().equals("ffmpeg") && bcast != null) {
			transcoder.stop();
			ChannelBuffer buf = buffer(1);
			buf.writeByte(0xF);
			ChannelBuffer ret;
			Integer count = 0;
			do {
				synchronized(this) {
					this.wait(1000);
				}
				ret = transcoder.transcode(buf);
				count++;			
				/* write remaining into listeners */
				if ( bcast != null && ret != null ) {
					debug.logTranscoder("TranscodeHandler, channelClosed(), writing remaining of " + ret.readableBytes() + " for " + bcast );
					bcast.getChannels().write(ret).awaitUninterruptibly(5, TimeUnit.SECONDS);
					/* write remaining into file */
					if ( Config.getInstance().isFileWriter() && ret != null && bcast.isStorage()) {
						AudioPacket p = new AudioPacket();
						p.setUuid(uuid);
						ret.readerIndex(0);
						p.setBuffer(ret);
						FileWriteHandler fileWriter = (FileWriteHandler) ctx.getPipeline().get("file-writer");
						fileWriter.closeFile(p);
					}
				} else { 
					break;
				}
			} while ( count <= 5);
		}
		
		/* and finalize this */
		try {
			transcoder.finalize();
			transcoder = null;
		} catch (Throwable err) {
			debug.logError("TranscodeHandler, channelClosed() exception ", err);
		}
		ctx.sendUpstream(e);
	}
	
	@Override
	protected void finalize() throws Throwable {
		debug.logTranscoder("TranscodeHandler, finalize() " );
		if ( transcoder != null ) {
			transcoder.finalize();
		}
		super.finalize();
	}
}
