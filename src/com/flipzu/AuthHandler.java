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

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Authentication Handler
 * Validates keys for broadcasting.
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */

public class AuthHandler extends SimpleChannelUpstreamHandler {
	
	private final Debug debug = Debug.getInstance();
	
	private FlipInterface fi = null;
	
	@Override
	public void channelConnected ( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
		debug.logAuth("AuthHandler(), channelConnected()");
		ctx.sendUpstream(e);
	}
	
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		UUID uuid = SessionAttrs.uuid.get(e.getChannel());
		Integer uid = 0;
		Integer bcastId = null;
		String bcastTitle = null;
		String username = null;
		boolean storage = true;
		boolean sc_share = false;
		String sc_token = null;
		Integer channels = null;
		Integer encrate = null;
		Integer bitrate = null;
		String key = null;

//		debug.logAuth("AuthHandler(), messageReceived() for uuid " + uuid.toString());
		
		AudioPacket p = (AudioPacket) e.getMessage();
		
		if (  uuid != null ) { // ongoing broadcast			
			p.setUuid(uuid);									
			ctx.sendUpstream(e);
			return;
		}

		// new broadcast
		if ( Config.getInstance().isTestMode() ) { // TEST MODE			
			username = Config.getInstance().getTestUsername();
			bcastId = Config.getInstance().getTestBcastId();
		} else {
			// parse the key, which should be before the newline character
			ChannelBuffer buf = (ChannelBuffer) p.getBuffer();
			byte b = '\n';
			int key_len = buf.bytesBefore(b)+1; // +1 is for the newline character
			byte[] byte_key = new byte[key_len];
			for ( int i = 0; i < key_len; i++ ) {
				byte_key[i] = buf.readByte();
			}
			try {
				key = new String(byte_key, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				debug.logError("authHandler, messageReceived() exception", e1 );
			}
			
			debug.logAuth("messageReceived(), got key " + key);
			
			if ( key == null ) {
				writeErrMessage(e.getChannel(), "AUTH FAILED\n");
				return;
			}
			
			if ( fi == null ) {
				fi = new FlipInterface();
			}
			
			if (!fi.verifyKey(key)) {
				writeErrMessage(e.getChannel(), "AUTH FAILED\n");
				return;
			}
			
			username = fi.getUsername();
			uid = fi.getUid();
			bcastId = fi.getBcastId();
			bcastTitle = fi.getBcastTitle();
			storage = fi.isStorage();
			sc_share = fi.isScShare();
			sc_token = fi.getScToken();
			channels = fi.getChannels();
			bitrate = fi.getBitrate();
			encrate = fi.getEncrate();
			
			if ( username == null || uid == null || bcastId == null ) {
				writeErrMessage(e.getChannel(), "AUTH FAILED\n");
				return;
			}
		}
				
		Broadcast bcast = new Broadcast();
		bcast.setUsername(username);
		bcast.setUid(uid);
		bcast.setId(bcastId);
		bcast.setTitle(bcastTitle);
		bcast.setStorage(storage);
		bcast.setScShare(sc_share);
		bcast.setScToken(sc_token);
		bcast.setNrChannels(channels);
		bcast.setBitrate(bitrate);
		bcast.setEncrate(encrate);
		bcast.setKey(key.trim());
		bcast.start();
		
		SessionAttrs.uuid.set(e.getChannel(), bcast.getUUID());

		if ( ! e.getChannel().isWritable() )
			return;
		
		e.getChannel().write("AUTH OK " + bcastId + "\n");
	
		
		// discard key from buffer
		p.getBuffer().discardReadBytes();
		
		p.setUuid(bcast.getUUID());
		
		// store the broadcast
		debug.logAuth("AuthHandler, messageReceived, adding " + bcast);
		Shows.getInstance().addBroadcast(bcast);
				
		ctx.sendUpstream(e);
    }
	
	@Override
	public void channelClosed (ChannelHandlerContext ctx, ChannelStateEvent e) {
		debug.logAuth("AuthHandler, channelClosed(), disconnected " + e.getChannel().getRemoteAddress().toString());

		UUID uuid = SessionAttrs.uuid.get(e.getChannel());
		
		if ( uuid == null ) {
			debug.logAuth("AuthHandler, channelClosed(), got no UUID");
			if ( fi != null ) {
				debug.logAuth("AuthHandler, channelClosed(), calling stopRec()");
				fi.stopRec();
			}
			ctx.sendUpstream(e);
			return;	
		}
		
		Broadcast bcast = Shows.getInstance().getBroadcast(uuid);
		if ( bcast == null ) {
			debug.logAuth("AuthHandler, channelClosed(), got no bcast");
			ctx.sendUpstream(e);
			return;
		}
		
		if ( fi != null && bcast.getState() != BroadcastState.RELAYING ) {
			debug.logAuth("AuthHandler, channelClosed(), calling stopRec()");
			if (!fi.stopRec()) {
				debug.logAuth("AuthHandler, channelClosed(), calling stopRec() again");
				fi.stopRec(); // second and last try, just in case
			}				
		}
		
		ctx.sendUpstream(e);
	}
	
	private void writeErrMessage( Channel ch, String msg ) {
		ChannelFuture f = ch.write(msg);
		f.addListener(ChannelFutureListener.CLOSE);
	}	
}
