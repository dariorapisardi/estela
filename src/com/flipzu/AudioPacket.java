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


import static org.jboss.netty.buffer.ChannelBuffers.buffer;

import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Represents and audio packet.
 * UUID is the UUID of the broadcast owning this packet.
 * p_buffer contains the data
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class AudioPacket {
	private UUID uuid;
	private ChannelBuffer p_buffer;
	
	public ChannelBuffer getBuffer() {
		return p_buffer;
	}
	
	public ChannelBuffer getBufferCopy() {
		return p_buffer.copy();
	}
	
	public void setBuffer(ChannelBuffer buf) {
		if ( buf == null ) {
			p_buffer.clear();
			return;
		}		
		p_buffer = buffer(buf.readableBytes());
		p_buffer.writeBytes(buf);
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}	
}
