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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * This handler writes the broadcast initial buffer into the
 * client's TCP channel. 
 * 
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class BufferWriterHandler extends SimpleChannelUpstreamHandler {
	private final Debug debug = Debug.getInstance();

	@Override
	public void messageReceived ( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {

		debug.logBufferWriter("BufferWriterHandler, messageReceived()");
		// get the uuid for this channel
		UUID uuid = SessionAttrs.uuid.get(e.getChannel());

		Broadcast bcast = Shows.getInstance().getBroadcast(uuid);
		
		if ( bcast == null )  {
			debug.logBufferWriter("BufferWriterHandler, messageReceived() no broadcast?. Returning");
			return;
		}

		// write to this channel the broadcast buffer, just once
		ChannelBuffer buf = bcast.getBuffer();
		debug.logBufferWriter("BufferWriterHandler, messageReceived() writing bytes: " + buf.readableBytes());
		e.getChannel().write(buf.readBytes(buf.readableBytes()));		
		
		// and remove ourselves...
		ctx.getPipeline().remove(this);
	}

}
