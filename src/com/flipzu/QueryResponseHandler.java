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


import java.util.UUID;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.flipzu.EstelaProtocol.BroadcastQueryResponse;
import com.flipzu.EstelaProtocol.EstelaMessage;

/**
 * Handles API responsed from estelaStats
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class QueryResponseHandler extends SimpleChannelUpstreamHandler {
	private final Debug debug = Debug.getInstance();

	@Override	
	public void messageReceived ( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
		debug.logQueryResponse("QueryResponseHandler, messageReceived()" );
		
		EstelaProtocol.EstelaMessage response = (EstelaProtocol.EstelaMessage) e.getMessage();
		
		if ( response.getMessageType() != EstelaMessage.EstelaMessageType.BROADCAST_QUERY ) {
			debug.logQueryResponse("QueryResponseHandler, messageReceived(), wrong message type!" );
			e.getChannel().close();
			return;
		}
		
		EstelaProtocol.BroadcastQueryResponse query_response = response.getBcastQueryResponse();
		
		EstelaProtocol.Broadcast bcast_msg = query_response.getBroadcast();
		
		if ( query_response.getResponseCode() == BroadcastQueryResponse.ResponseCode.NOT_FOUND ) {
			debug.logQueryResponse("QueryResponseHandler, " + bcast_msg.getUsername() + " NOT FOUND");
			RelayHandler.getInstance().closeWaitingChannels(bcast_msg.getUsername());
			ctx.sendUpstream(e);
			return;
		}
		
		String server_ip = query_response.getServerIp();
		String hostname = query_response.getHostname();
		Integer write_port = query_response.getWritePort();
		Integer read_port = query_response.getReadPort();
		
		EstelaServer server = new EstelaServer(hostname, server_ip, read_port, write_port);

		Broadcast bcast = Shows.getInstance().getBroadcast(UUID.fromString(bcast_msg.getUuid()));
		if ( bcast == null || bcast.getState() != BroadcastState.RELAYING ) {
			// first relay for this broadcast
			bcast = new Broadcast();
			bcast.setUsername(bcast_msg.getUsername());
			bcast.setId(bcast_msg.getId());
			bcast.setUuid(UUID.fromString(bcast_msg.getUuid()));
			bcast.start();
			bcast.setState(BroadcastState.RELAYING);			
		} 
						
		
		debug.logQueryResponse("QueryResponseHandler, messageReceived(), got " + server + " for " + bcast);
		
		RelayHandler.getInstance().startRelay(bcast, server);		
		
		ctx.sendUpstream(e);
	}
	
	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, ExceptionEvent e) {
		debug.logError("QueryResponseHandler, exceptionCaught() ", e.getCause());
		e.getChannel().close();
	}	
}
