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
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;

/**
 * Detects hanged connections and closes them.
 * 
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class TimeoutHandler extends IdleStateAwareChannelHandler {
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
	    if (e.getCause() instanceof ReadTimeoutException) {
	    	ctx.getChannel().close();
	    	Debug.getInstance().logTimeout("TimeoutHandler, closing channel for timeout");
	    } else {
	    	Debug.getInstance().logError("TimeoutHandler unknown exception: ", e.getCause());
	    }
	}
	
	@Override
	protected void finalize() throws Throwable {
		Debug.getInstance().logTimeout("TimeoutHandler, finalize, cleaning up");
		super.finalize();
	}
}
