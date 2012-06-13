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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Transcoder Handler based on FFMPEG command line tool
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class FfmpegTranscoder  extends FzTranscoder implements Runnable {

	private final Debug debug = Debug.getInstance();
	private final Integer outputBitrate = Config.getInstance()
			.getOutputBitrate();
	private final String outputCodec = Config.getInstance().getOutputCodec();
	private final String ffmpegCmd = Config.getInstance().getFfmpegCommand();
	private final String outputCodecFlag = Config.getInstance().getOutputCodecFlag();
	private final String outputBitrateFlag = Config.getInstance().getOutputBitrateFlag();
	private final String outputFileFlag = Config.getInstance().getOutputFileFlag();
	private final String inputFileFlag = Config.getInstance().getInputFileFlag();
	private Process ffmpeg = null;
	private BufferedInputStream stdOut = null;
	private BufferedOutputStream stdIn = null;
	
	private boolean mIsRunning = false;

	public ChannelBuffer transcode (ChannelBuffer buf) throws IOException {
		
		if ( mIsRunning ) {
			try {
				byte[] b = new byte[buf.readableBytes()];
				buf.readBytes(b);
				stdIn.write(b);
			} catch (IOException e) {
				debug.logError("FfmpegTranscoder, ffmpegTranscode() exception ", e);
				throw e;
			}			
		}
		

		int len = 0;
		try {
			len = stdOut.available();
		} catch (IOException e) {
			debug.logError("FfmpegTranscoder, ffmpeg exception ", e);
		}
		debug.logTranscoder("FfmpegTranscoder(), decoded bytes: " + len);
		ChannelBuffer out = buffer(len);
		try {
			out.writeBytes(stdOut, len);
		} catch (IOException e) {
			debug.logError("FfmpegTranscoder, ffmpeg exception ", e);
		}
		
		if ( !mIsRunning ) {
			try {
				stdIn.close();
			} catch (IOException e1) {
				debug.logError("FfmpegTranscoder, transcode(), stop exception ", e1);
			}					
		}

		return out;
	}

	protected void stop() {
		mIsRunning = false;
		
		debug.logTranscoder("FfmpegTranscoder(), stopping");
	}

	protected void finalize() throws Throwable {				
		try {
			// flush everything
			stdOut.close();
		} catch (IOException e1) {
			debug.logError("FfmpegTranscoder, finalize() exception ", e1);
		} finally {
			debug.logTranscoder("FfmpegTranscoder(), ffmpeg.destroy()");
			ffmpeg.destroy();
		}
		debug.logTranscoder("FfmpegTranscoder(), finalize()");
		super.finalize();
	}

	@Override
	public void run() {
		debug.logTranscoder("FfmpegTranscoder(), run()");

		try {
			ffmpeg = new ProcessBuilder(ffmpegCmd,inputFileFlag,"-",outputBitrateFlag,outputBitrate.toString(),outputCodecFlag,outputCodec,outputFileFlag,"-").start();
		} catch (IOException e) {
			debug.logError("FfmpegTranscoder, run() exception ", e);
		}

		stdOut = new BufferedInputStream(ffmpeg.getInputStream());
		stdIn = new BufferedOutputStream(ffmpeg.getOutputStream());		
		
		mIsRunning = true;
	}
}
