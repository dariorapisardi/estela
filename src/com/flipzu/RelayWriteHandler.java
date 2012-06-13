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
 * This handler does the proper relaying to the client.
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class RelayWriteHandler extends SimpleChannelUpstreamHandler {
private final Debug debug = Debug.getInstance();
	
	@Override	
	public void messageReceived ( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
		
		Broadcast bcast = SessionAttrs.bcast.get(e.getChannel());
		
		if ( bcast == null ) {
			debug.logRelayHandler("RelayWriteHandler, messageReceived(), can't fetch broadcast");
			e.getChannel().close();
			return;
		}
		
		if ( !Shows.getInstance().hasBroadcast(bcast.getUUID()) ) {
			debug.logRelayHandler("RelayWriteHandler, messageReceived(), no relaying anymore, closing");
			e.getChannel().close();			
			return;
		}
		
		ChannelBuffer buf = (ChannelBuffer) e.getMessage();
		debug.logRelayHandler("RelayWriteHandler, messageReceived(), got " + buf.readableBytes() + " bytes");
		
		RelayHandler.getInstance().write(bcast, buf);
		
		ctx.sendUpstream(e);
	}
	
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		Broadcast bcast = SessionAttrs.bcast.get(e.getChannel());
		debug.logRelayHandler("RelayWriteHandler, channelClosed() for " + bcast);
		
		if ( bcast != null ) {
			RelayHandler.getInstance().cleanup(bcast);
		}
		
		ctx.sendUpstream(e);
	}
	
	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, ExceptionEvent e) {
		debug.logError("RelayWriteHandler, exceptionCaught " , e.getCause());
		e.getChannel().close();
	}
	
}
