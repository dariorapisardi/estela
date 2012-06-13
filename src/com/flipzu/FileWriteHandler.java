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


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Estela File Writer handler
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */

public class FileWriteHandler extends SimpleChannelUpstreamHandler  {
	private final Debug debug = Debug.getInstance();
	private final String destDir = Config.getInstance().getFileWriterDestDir();
	private final String fileExtension = Config.getInstance().getFileWriterExtension();
	
	FileOutputStream fstream = null;
	BufferedOutputStream outBuffer = null;

	@Override
	public void channelConnected ( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
		debug.logFileWriter("FileWriteHandler(), channelConnected()");
		
		ctx.sendUpstream(e);
	}
	
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		
		
		AudioPacket p = (AudioPacket) e.getMessage();
		
		ChannelBuffer buf = (ChannelBuffer) p.getBuffer();		
		int index = buf.readerIndex();
		
		byte[] byte_buf = new byte[buf.readableBytes()];
		buf.readBytes(byte_buf);
		
		UUID uuid = p.getUuid();
		Broadcast bcast = Shows.getInstance().getBroadcast(uuid);
		
		if ( !bcast.isStorage() ) {
			ctx.getPipeline().remove(this);
			ctx.sendUpstream(e);
			return;
		}
		
		try {
			if ( fstream == null ) {
				String dest = destDir + "/" + bcast.getId() + fileExtension;
				bcast.setFilename(dest);
				debug.logFileWriter("FileWriteHandler(), messageReceived(), writing to  " + dest);
				fstream = new FileOutputStream(dest, true);
				outBuffer = new BufferedOutputStream(fstream);
			}
			debug.logFileWriter("FileWriteHandler(), writing " + byte_buf.length + " for " + bcast);			
			outBuffer.write(byte_buf);
		} catch (IOException e1) {
			debug.logError("FileWriteHandler, messageReceived() exception ", e1);
		}
		
		// restore index
		buf.readerIndex(index);
		
		ctx.sendUpstream(e);
    }
	
	public void closeFile( AudioPacket p ) {
		ChannelBuffer buf = p.getBuffer();
		byte[] byte_buf = new byte[buf.readableBytes()];
		buf.readBytes(byte_buf);
		
		UUID uuid = p.getUuid();
		
		if ( uuid == null )
			return;
		
		Broadcast bcast = Shows.getInstance().getBroadcast(uuid);
		
		if ( bcast == null ) 
			return;
		
		try {
			if ( fstream == null ) {
				String dest = destDir + "/" + bcast.getId() + fileExtension;
				debug.logFileWriter("FileWriteHandler(), finalWrite(), writing to  " + dest);
				fstream = new FileOutputStream(dest, true);
				outBuffer = new BufferedOutputStream(fstream);
			}
			debug.logFileWriter("FileWriteHandler(), writing " + byte_buf.length + " for " + bcast);
			outBuffer.write(byte_buf);	
		} catch (IOException e1) {
			debug.logError("FileWriteHandler, finalWrite() exception ", e1);
		}		
	}
	
	@Override 
	public void channelClosed (ChannelHandlerContext ctx, ChannelStateEvent e) {
		debug.logFileWriter("FileWriteHandler(), channelClose()");
		try {
			if ( outBuffer != null )
				outBuffer.close();
		} catch (IOException e1) {
			debug.logError("FileWriteHandler, channelClosed() exception ", e1);
		}
		
		ctx.sendUpstream(e);
	}
}
