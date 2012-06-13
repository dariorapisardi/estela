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


import java.util.Timer;
import java.util.UUID;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * latest handler. It sets up a thread for offline tasks.
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class PostProcesserHandler extends SimpleChannelUpstreamHandler {
	
	private Debug debug = Debug.getInstance();

	@Override 
	public void channelClosed (ChannelHandlerContext ctx, ChannelStateEvent e) {
		UUID uuid = SessionAttrs.uuid.get(e.getChannel());
				
		Timer timer = new Timer();
		
		// pass a copy of the Broadcast object to the timer
		// in case it changed in the future after de delay
		Broadcast bcast = Shows.getInstance().getBroadcast(uuid);
		if ( bcast != null ) {
			bcast.stop();
			Broadcast newBcast = bcast.clone();			
			
			PostProcThread postproc = new PostProcThread(newBcast);
			
			timer.schedule(postproc, Config.getInstance().getPostProcDelay()*1000);
			debug.logPostProc("PostProcesserHandler, channelClosed for " + bcast);
		}				
		ctx.sendUpstream(e);
	}
	
	@Override
	protected void finalize() throws Throwable {
		debug.logPostProc("PostProcesserHandler, finalize()");
	}
	
	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, ExceptionEvent e) {
		debug.logError("PostProcesserHandler, exceptionCaught", e.getCause());
	}
}
