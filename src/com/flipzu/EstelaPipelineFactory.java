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


import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

/**
 * Pipeline factory for clients (listeners)
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */

public class EstelaPipelineFactory implements ChannelPipelineFactory {
	public ChannelPipeline getPipeline() throws Exception {
		
		// default pipeline implementation
		ChannelPipeline pipeline = pipeline();
		
		pipeline.addLast("http-decoder", new HttpRequestDecoder());
		pipeline.addLast("http-encoder", new HttpResponseEncoder());
		// http handler
		pipeline.addLast("client-handler", new HttpClientHandler());
		// buffer writer for new connections
		pipeline.addLast("buffer-writer", new BufferWriterHandler());

		return pipeline;
	}
}
