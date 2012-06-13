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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * Main decoder for incoming broadcaster packets 
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class BcastDecoder extends FrameDecoder {
	private final Debug debug = Debug.getInstance();
	
	private Integer minBytes = Config.getInstance().getKeyLength();

	@Override
	protected Object decode( ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer ) throws Exception {
		
		if ( buffer.readableBytes() < minBytes ) 
			return null;
		
		debug.logBcastDecoder("BcastDecoder, decode()  readable " + buffer.readableBytes());
		minBytes = Config.getInstance().getFrameSize();
        
		AudioPacket p = new AudioPacket();

		p.setBuffer(buffer);
				
		return p;
	}
	
	public void setMinBytes ( Integer bytes ) {
		minBytes = bytes;
	}
	
	public Integer getMinBytes () {
		return minBytes;
	}
}
