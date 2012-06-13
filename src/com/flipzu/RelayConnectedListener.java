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


import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * Estela Future Listener for connection with other Estelas
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class RelayConnectedListener implements ChannelFutureListener {
	
	private final Debug debug = Debug.getInstance();

	private Broadcast bcast;

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		if (!future.isSuccess()) {
			debug.logRelayHandler("RelayConnectedListener, operationComplete(), can't connect for " + bcast);
			RelayHandler.getInstance().cleanup(bcast);
			return;
		}				
		Channel channel = future.getChannel();				
		
		/* write /GET */				
		HttpRequest request = new DefaultHttpRequest(
				 HttpVersion.HTTP_1_1, HttpMethod.GET, "/" + bcast.getUsername());
		request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
		channel.write(request);
		
		/* save username for this channel */
		SessionAttrs.bcast.set(channel, bcast);				
	}
	
	public RelayConnectedListener ( Broadcast bcast ) { 
		this.bcast = bcast;
	}

}
