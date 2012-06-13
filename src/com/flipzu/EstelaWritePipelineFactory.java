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
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

/**
 * Pipeline factory for broadcasters
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */

public class EstelaWritePipelineFactory implements ChannelPipelineFactory {
	
	private final ExecutionHandler executionhandler;
	private Timer timer;
	
	public EstelaWritePipelineFactory(ExecutionHandler executionhandler) {
		this.executionhandler = executionhandler;
		this.timer = new HashedWheelTimer();
	}
	
	public ChannelPipeline getPipeline() throws Exception {
		
		// default pipeline implementation
		ChannelPipeline pipeline = pipeline();

		// timeout handler
		pipeline.addLast("timeout", new ReadTimeoutHandler(timer, Config.getInstance().getBcastTimeout()));
		pipeline.addLast("timeout-handler", new TimeoutHandler());
		
		// default decoder
		pipeline.addLast("bcast-decoder", new BcastDecoder());
		
		// flipzu auth
		pipeline.addLast("auth", new AuthHandler());
		
		// transcoder
		if (Config.getInstance().isTranscoder()) {
			pipeline.addLast("execution-handler", executionhandler);
			pipeline.addLast("transcoder", new TranscodeHandler());
		}

		// file writer
		if ( Config.getInstance().isFileWriter() ) {
			pipeline.addLast("file-writer", new FileWriteHandler());	
		}
		
		// channels writer
		pipeline.addLast("socket-writer", new ChannelWriterHandler());
		
		// postprocessing handler
		if ( Config.getInstance().isPostprocessor() ) {
			pipeline.addLast("postprocesser", new PostProcesserHandler());
		}
	
		// cleanup handler
		pipeline.addLast("cleanup", new CleanupHandler());		
		
		// string encoder, for responses
		pipeline.addLast("string-encoder", new StringEncoder());
		
		return pipeline;
	}
}
