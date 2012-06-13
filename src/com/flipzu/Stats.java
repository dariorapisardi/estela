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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import com.flipzu.EstelaProtocol.StatsMessage.Builder;

/**
 * Estela singleton object for connection with StatsServer
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class Stats {
	private static Stats INSTANCE = new Stats();
	private final Debug debug = Debug.getInstance();
	private Channel channel = null;
	private ClientBootstrap bootstrap;
	private String hostname;
	private String writeIp = Config.getInstance().getWriteAddress();
	private Integer readPort = Config.getInstance().getReadPort();
	private Integer writePort = Config.getInstance().getWritePort();

	private Stats() {
		debug.logStats("Stats constructor");
		
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			try {
				hostname = InetAddress.getLocalHost().getAddress().toString();
			} catch (UnknownHostException e1) {
				hostname = "unknown";
			}
		}
		
		connect();
	}
	
	public static Stats getInstance() {
		return INSTANCE;
	}
	
	private void connect() {
		
		String host = Config.getInstance().getLoggerHost();
		int port = Config.getInstance().getLoggerPort();
		
		bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();
		
				pipeline.addLast("frameDecoder",  new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
				pipeline.addLast("protobufDecoder", new ProtobufDecoder(EstelaProtocol.EstelaMessage.getDefaultInstance()));
				
				pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
				pipeline.addLast("protobufEncoder", new ProtobufEncoder());
				pipeline.addLast("handler", new QueryResponseHandler());
				return pipeline;
			}
		});
		
		
		debug.logShows("Stats, connect(), connecting to " + host + ":" + port);
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
				
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future)
					throws Exception {
				channel = future.getChannel();				
				registerServer();
				updateAll();
			}
		});
	}
	
	private void registerServer() {
		debug.logStats("Stats, registerServer()");
				
		EstelaProtocol.EstelaMessage.Builder builder = EstelaProtocol.EstelaMessage.newBuilder();
		
		builder.setMessageType(EstelaProtocol.EstelaMessage.EstelaMessageType.STATS_MESSAGE);
		
		builder.setStatsMessage(EstelaProtocol.StatsMessage.newBuilder()
				.setServerName(hostname)
				.setServerIp(writeIp)
				.setReadPort(readPort)
				.setWritePort(writePort)
				.setMessageType(EstelaProtocol.StatsMessage.StatsMessageType.REGISTER_SERVER)
				);

		if ( channel.isWritable() )
			channel.write(builder.build());
	}
	
	public void updateAll() {
		debug.logStats("Stats, updateAll()");
		
		ArrayList<Broadcast> bcasts = Shows.getInstance().getBroadcasts();
		
		EstelaProtocol.EstelaMessage.Builder builder = EstelaProtocol.EstelaMessage.newBuilder();
		
		builder.setMessageType(EstelaProtocol.EstelaMessage.EstelaMessageType.STATS_MESSAGE);
		
		Builder stats_message_builder = EstelaProtocol.StatsMessage.newBuilder();
		
		stats_message_builder.setServerName(hostname)
			.setServerIp(writeIp)
			.setReadPort(readPort)
			.setWritePort(writePort)
			.setMessageType(EstelaProtocol.StatsMessage.StatsMessageType.UPDATE_STATS);
						
		for ( Broadcast b : bcasts ) {
			stats_message_builder.addBroadcast(EstelaProtocol.Broadcast.newBuilder()
					.setListeners(b.getChannels().size())
					.setUsername(b.getUsername())
					.setId(b.getId())
					.setUuid(b.getUUID().toString())
					.build()
					);			
		}
		
		
		builder.setStatsMessage(stats_message_builder);

		if ( channel.isWritable() )
			channel.write(builder.build());
	}
	
	public void sendStats( Broadcast bcast ) {
		debug.logStats("Stats, sendStats() called");		
		
		sendMessage(EstelaProtocol.StatsMessage.StatsMessageType.UPDATE_STATS, bcast);
	}

	public void closeBcast( Broadcast bcast ) {
		debug.logStats("Stats, closeBcast() called");
		
		sendMessage(EstelaProtocol.StatsMessage.StatsMessageType.CLOSE_BCAST, bcast);
	}
		
	public void sendMessage( EstelaProtocol.StatsMessage.StatsMessageType type, Broadcast bcast ) {
		/* check if we have to reconnect */
		if ( channel == null || !channel.isConnected() ) {
			connect();
		}
		
		EstelaProtocol.StatsMessage.Builder stats_message_builder = EstelaProtocol.StatsMessage.newBuilder();		
		
		stats_message_builder.setServerName(hostname)
		.setServerIp(writeIp)
		.setReadPort(readPort)
		.setWritePort(writePort)
		.setMessageType(type);
		
		stats_message_builder.addBroadcast(EstelaProtocol.Broadcast.newBuilder()
				.setListeners(bcast.getChannels().size())
				.setUsername(bcast.getUsername())
				.setId(bcast.getId())
				.setUuid(bcast.getUUID().toString())
				.build()
				);
		
		EstelaProtocol.EstelaMessage.Builder builder = EstelaProtocol.EstelaMessage.newBuilder();
		
		builder.setMessageType(EstelaProtocol.EstelaMessage.EstelaMessageType.STATS_MESSAGE);

		builder.setStatsMessage(stats_message_builder);

		if ( channel != null && channel.isWritable() ) 
			channel.write(builder.build());		
	}
	
	public void whoHasBroadcast( String username ) {
		debug.logStats("Stats, whoHasBroadcast for " + username);
		
		/* check if we have to reconnect */
		if ( channel == null || !channel.isConnected() ) {
			connect();
		}
		
		EstelaProtocol.BroadcastQuery.Builder query_builder = EstelaProtocol.BroadcastQuery.newBuilder();
		
		query_builder.setUsername(username).build();
		
		EstelaProtocol.EstelaMessage.Builder builder = EstelaProtocol.EstelaMessage.newBuilder();
		
		builder.setMessageType(EstelaProtocol.EstelaMessage.EstelaMessageType.BROADCAST_QUERY);
		
		builder.setBcastQuery(query_builder);
				
		channel.write(builder.build());
		
	}	
	
	@Override
	protected void finalize() throws Throwable {
		debug.logStats("Stats destructor");
		channel.close();	
		
		bootstrap.releaseExternalResources();
	}
	
}
