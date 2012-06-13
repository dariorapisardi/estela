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


import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * Handles response messages for relayed broadcasts
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class RelayResponseHandler extends SimpleChannelUpstreamHandler {
	
	private final Debug debug = Debug.getInstance();
	
	@Override	
	public void messageReceived ( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
		debug.logRelayHandler("RelayResponseHandler, messageReceived()");
		
		
		HttpResponse response;
		try {
			response = (HttpResponse) e.getMessage();	
		} catch ( ClassCastException ex ) {
			debug.logRelayHandler("messageReceived: got ClasCastException ");
			return;
		}
		
		
		Broadcast bcast = SessionAttrs.bcast.get(e.getChannel());
		
		if ( bcast == null ) {
			debug.logRelayHandler("RelayResponseHandler, messageReceived(), can't fetch bcast");
			return;
		}
		
		if ( response.getStatus().getCode() != 200 ) {
			debug.logRelayHandler("RelayResponseHandler, messageReceived(), request did't succeed for " + bcast);
			RelayHandler.getInstance().cleanup(bcast);
			e.getChannel().close();
			return;
		}
		
		debug.logRelayHandler("RelayResponseHandler, messageReceived(), ok, got 200");		
		
		// remove ourselves and don't send upstream
		ctx.getPipeline().remove("codec");
		ctx.getPipeline().remove(this);
		
	}
	
	@Override 
	public void channelClosed (ChannelHandlerContext ctx, ChannelStateEvent e) {
		Broadcast bcast = SessionAttrs.bcast.get(e.getChannel());
		
		debug.logRelayHandler("RelayResponseHandler, channelClosed() for " + bcast);
		
		if ( bcast != null ) {
			RelayHandler.getInstance().cleanup(bcast);			
		}
		
		ctx.sendUpstream(e);
	}
	
	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, ExceptionEvent e) {
		debug.logError("RelayResponseHandler, exceptionCaught " , e.getCause());
		e.getChannel().close();
		
	}

}
