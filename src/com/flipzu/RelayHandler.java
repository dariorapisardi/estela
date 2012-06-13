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

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

/**
 * Handler that checks if a broadcast is available for relaying or not.
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class RelayHandler {
	private static RelayHandler INSTANCE = new RelayHandler();
	private final Debug debug = Debug.getInstance();
	
	private Hashtable<String, ChannelGroup> waitingChannels = new Hashtable<String, ChannelGroup>();
	private Hashtable<UUID, ClientBootstrap> connections = new Hashtable<UUID, ClientBootstrap>();
	
	private RelayHandler() { }
	
	public static RelayHandler getInstance() {
		return INSTANCE;
	}
	
	public synchronized void addWaitingChannel( String username, Channel channel ) {
		debug.logRelayHandler("RelayHandler, addWaitingChannel for " + username);
		
		if ( !waitingChannels.containsKey(username)) {
			ChannelGroup ch_group = new DefaultChannelGroup();
			ch_group.add(channel);
			waitingChannels.put(username, ch_group);
			debug.logRelayHandler("RelayHandler, new channelgroup " + username + " has " + ch_group.size() + " waiting channels");
			return;
		}
		
		ChannelGroup ch_group = waitingChannels.get(username);
		ch_group.add(channel);
		debug.logRelayHandler("RelayHandler, " + username + " has " + ch_group.size() + " waiting channels");
	}
	
	public synchronized void closeWaitingChannels( String username ) {
		if ( !waitingChannels.containsKey(username)) {
			debug.logRelayHandler("RelayHandler, closeWaitingChannels(), no channels found for " + username);
			return;
		}
		
		closeChannels(username, waitingChannels.get(username));
	}
		
	public synchronized void closeChannels( String username, ChannelGroup ch_group ) {
				
		debug.logRelayHandler("RelayHandler, closeChannels(), closing " + ch_group.size() + " channels for " + username);
		
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
		ch_group.write(response).addListener(new ChannelGroupFutureListener() {
			@Override
			public void operationComplete(ChannelGroupFuture future)
					throws Exception {
				future.getGroup().close();					
			}			
		});		
		
		ch_group.remove(username);	
	}
	
	public synchronized void startRelay( Broadcast bcast, EstelaServer server ) {
		String username = bcast.getUsername();
		
		// add to shows, for stats server
		Shows.getInstance().addBroadcast(bcast);
		
		if ( connections.containsKey(bcast.getUUID())) {
			debug.logRelayHandler("RelayHandler, startRelay(), already a connection for " + username);
			return;
		}
		
		/* create new connection and save it */
		debug.logRelayHandler("RelayHandler, startRelay(), first connection for " + username + ", creating");
		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();	
				int chunksize = Config.getInstance().getBufferSize()*2;
				final Timer timer;
				timer = new HashedWheelTimer();
				pipeline.addLast("timeout", new ReadTimeoutHandler(timer, Config.getInstance().getBcastTimeout()));
				pipeline.addLast("timeout-handler", new TimeoutHandler());
				pipeline.addLast("codec", new HttpClientCodec(4096, chunksize, chunksize));
//				pipeline.addLast("http-decoder", new HttpRequestDecoder());
//				pipeline.addLast("http-encoder", new HttpResponseEncoder());
				pipeline.addLast("handler", new RelayResponseHandler());
				pipeline.addLast("write-handler", new RelayWriteHandler());
				return pipeline;
			}
		});
		
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(server.getIp(), server.getReadPort()));
	
		connections.put(bcast.getUUID(), bootstrap);
		
		future.addListener(new RelayConnectedListener(bcast));
		
	}
	
	public synchronized void closeConnection( Broadcast bcast ) {
		String username = bcast.getUsername();
		if ( !connections.containsKey(bcast.getUUID())) {
			debug.logRelayHandler("RelayHandler, closeConnections(), no channels found for " + username);
			return;
		}
		
		connections.remove(bcast.getUUID());				
	}
	
	public synchronized void cleanup( Broadcast bcast ) {
		Shows.getInstance().delBroadcast(bcast.getUUID());
		closeWaitingChannels(bcast.getUsername());
		closeConnection(bcast);
	}
		
	public void write( Broadcast bcast, ChannelBuffer buf ) {
		String username = bcast.getUsername();
		
		if ( waitingChannels.containsKey(username)) {
			ChannelGroup ch_group = waitingChannels.get(username);
			HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
					HttpResponseStatus.OK);
			response.setHeader(HttpHeaders.Names.SERVER, Config.getInstance().getServerName());
			response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "audio/mpeg");
			response.setHeader(HttpHeaders.Names.CONNECTION, "close");

			ch_group.write(response);
			addToActiveChannels(bcast, ch_group);
		}
		
		
		if ( !Shows.getInstance().hasBroadcast(bcast.getUUID())) {
			debug.logRelayHandler("RelayHandler, write(), no channels found for " + username);
			return;		
		}
		
		if ( bcast.getChannels().size() == 0 ) {
			debug.logRelayHandler("RelayHandler, 0 channels left, closing");
			cleanup(bcast);
		}
		
		debug.logRelayHandler("RelayHandler, write(), writing into " + bcast.getChannels().size() + " channels");
		bcast.getChannels().write(buf);
	}
	
	public void addToActiveChannels( Broadcast bcast, ChannelGroup ch_group ) {
		Iterator<Channel> i = ch_group.iterator();
			while ( i.hasNext() ) {
				Channel channel = i.next();
				Shows.getInstance().addChannelToBroadcast(bcast.getUUID(), channel);
		}			
		
		waitingChannels.remove(bcast.getUsername());
		
		Stats.getInstance().sendStats(bcast);
		
	}
}
