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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Writes the broadcast data into the client's TCP
 * connection.
 * The "stream" properly speaking.
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class ChannelWriterHandler extends SimpleChannelUpstreamHandler {

	private final Debug debug = Debug.getInstance();
	
	@Override
	public void channelConnected ( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
		debug.logChannelWriter("ChannelWriterHandler(), channelConnected()");
		ctx.sendUpstream(e);
	}
	
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		AudioPacket p = (AudioPacket) e.getMessage();
		
		debug.logChannelWriter("ChannelWriterHandler(), messageReceived() for " + p.getUuid());
		
		// write to channels
		Broadcast bcast = Shows.getInstance().getBroadcast(p.getUuid());
		
		if ( bcast == null ) {
			debug.logChannelWriter("ChannelWriterHandler(), messageReceived(), NO BROADCAST FOUND!");
			return;
		}
			
		
		bcast.writeBuffer( (ChannelBuffer) p.getBuffer() );		
						
		ctx.sendUpstream(e);
    }
	
	@Override
	public void channelClosed (ChannelHandlerContext ctx, ChannelStateEvent e) {
		debug.logChannelWriter("ChannelWriterHandle(), channelClosed()");
				
		ctx.sendUpstream(e);
	}

	
	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, ExceptionEvent e) {
		debug.logError("ChannelWriterHandler, exceptionCaught() ", e.getCause()); 
		e.getChannel().close();
	}	
	
}
