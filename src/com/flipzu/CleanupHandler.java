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
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Called when a connection is closed. Cleans up everything.
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class CleanupHandler extends SimpleChannelUpstreamHandler {
	@Override 
	public void channelClosed (ChannelHandlerContext ctx, ChannelStateEvent e) {	
		UUID uuid = SessionAttrs.uuid.get(e.getChannel());

		if ( uuid == null ) {
			Debug.getInstance().logCleanup("CleanupHandler, channelClosed(), got NULL uuid!");
		} else {
			Debug.getInstance().logCleanup("CleanupHandler, channelClosed() for " + uuid.toString());	
		}
		
		
		if ( uuid != null ) {
			Broadcast bcast = Shows.getInstance().getBroadcast(uuid);
			if ( bcast != null ) {
				bcast.getChannels().close();
			}
			// if we're using a postprocessor, then it will delete the broadcast.
			if ( Config.getInstance().isPostprocessor() ) {
				Shows.getInstance().stopBroadcast(uuid);	
			} else {
				Shows.getInstance().delBroadcast(uuid);
			}			
			SessionAttrs.uuid.remove(e.getChannel());			
		}
		
		ctx.sendUpstream(e);
	}
	
	@Override
	protected void finalize() throws Throwable {
		Debug.getInstance().logCleanup("CleanupHandler, finalize()");
	}
	
	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, ExceptionEvent e) {
		Debug.getInstance().logError("CleanupHandler, exceptionCaught", e.getCause());
	}
}
