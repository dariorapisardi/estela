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


import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

/**
 * Estela streaming server setup
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */

public class Estela {
	
	public static void main(String[] args) throws Exception {
		ChannelFactory factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());
		

		ServerBootstrap writeBootstrap = new ServerBootstrap(factory); // broadcasters write here
		ServerBootstrap readBootstrap = new ServerBootstrap(factory); // clients read from here
		
		readBootstrap.setPipelineFactory(new EstelaPipelineFactory());
		
		// execution handler para operaciones de I/O (ffmpeg)
		ExecutionHandler executionhandler = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(128, 0, 0));
		writeBootstrap.setPipelineFactory(new EstelaWritePipelineFactory(executionhandler));
		
		// opciones para los clientes de streaming
		readBootstrap.setOption("child.tcpNoDelay", true);
		readBootstrap.setOption("child.keepAlive", true);
		readBootstrap.setOption("child.sendBufferSize", Config.getInstance().getBufferSize()*2);

		// bind a puertos de escritura y lectura
		writeBootstrap.bind(new InetSocketAddress(Config.getInstance().getWriteAddress(), Config.getInstance().getWritePort()));
		readBootstrap.bind(new InetSocketAddress(Config.getInstance().getReadAddress(), Config.getInstance().getReadPort()));
		
		/* singleton class, but we call it now to early start connection with the Stats server */
		Stats stats = Stats.getInstance();
		stats.toString();
		
		Debug.getInstance().logEstela("Estela start");
	}

}
