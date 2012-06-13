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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * Handler for connecting clients (listeners)
 * 
 * @author Dario Rapisardi <dario@rapisardi.org>
 * 
 */
public class HttpClientHandler extends SimpleChannelUpstreamHandler {

	private final Debug debug = Debug.getInstance();

	private final String serverName = Config.getInstance().getServerName();
	private final Integer readPort = Config.getInstance().getReadPort();
	private final Integer writePort = Config.getInstance().getWritePort();
	
	@Override 
	public void channelConnected (ChannelHandlerContext ctx, ChannelStateEvent e) {
		debug.logHttpClientHandler("ClientHandler, channelConnected()");

		ctx.sendUpstream(e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {

		HttpRequest request = (HttpRequest) e.getMessage();
		
		debug.logHttpClientHandler("ClientHandler, messageReceived() method "
				+ request.getMethod());
		
		// quit if not GET method
		if (!request.getMethod().toString().equals("GET")) {
			e.getChannel().close();
			return;
		}

		String username = getPath(request.getUri());
		debug.logHttpClientHandler("ClientHandler, messageReceived() path "
				+ username);

		if (username.equals("crossdomain.xml")) {
			sendCrossDomain(e);
			return;
		}

		// check if @username is not broadcasting locally
		Broadcast bcast = Shows.getInstance().getBroadcast(username);
		if (bcast == null || 
				(bcast.getState() != BroadcastState.LIVE)) {
			// add waiting channel
			RelayHandler.getInstance().addWaitingChannel(username,
					e.getChannel());

			// check if it's not in a relay
			debug.logHttpClientHandler("ClientHandler, checking relay for " + username );
			Stats.getInstance().whoHasBroadcast(username);
			return;
		}
		
		// Authenticate user
		if ( Config.getInstance().useAuth() ) {
			String user_id = getGetValue(request.getUri(), "u");
			if ( ! AuthUser.getInstance().isAuthenticated(username, user_id, bcast.getUUID()) ) {
				debug.logHttpClientHandler("ClientHandler, user " + user_id + " NO AUTH for " + username);
				sendHttp404(e);
				return;
			} else {
				SessionAttrs.user_id.set(e.getChannel(), user_id);
				debug.logHttpClientHandler("ClientHandler, user " + user_id + " AUTH OK for " + username);
			}			
		}

		sendHttpResponse(e);

		// add our channel
		Shows.getInstance().addChannelToBroadcast(bcast.getUUID(),
				ctx.getChannel());

		// add username to our session
		SessionAttrs.uuid.set(e.getChannel(), bcast.getUUID());

		// refresh stats in server
		Stats.getInstance().sendStats(bcast);

		ctx.sendUpstream(e);
	}

	public void sendHttpResponse(MessageEvent e) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
				HttpResponseStatus.OK);
		response.setHeader(HttpHeaders.Names.SERVER, serverName);
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "audio/mpeg");
		response.setHeader(HttpHeaders.Names.CONNECTION, "close");

		e.getChannel().write(response);
	}

	public void sendHttp404(MessageEvent e) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
				HttpResponseStatus.NOT_FOUND);
		e.getChannel().write(response);
		e.getChannel().close();
	}

	public void sendCrossDomain(MessageEvent e) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
				HttpResponseStatus.OK);
		response.setHeader(HttpHeaders.Names.SERVER, serverName);
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html");
		response.setHeader(HttpHeaders.Names.CONNECTION, "close");

		String msg = "<?xml version=\"1.0\"?>\r\n";
		msg += "<!DOCTYPE cross-domain-policy SYSTEM \"http://www.adobe.com/xml/dtds/cross-domain-policy.dtd\">\r\n";
		msg += "<cross-domain-policy>\r\n";
		msg += "<allow-access-from domain=\"*\" to-ports=\"" + readPort + ","
				+ writePort + "\"/>\r\n";
		msg += "</cross-domain-policy>\r\n";
		msg += "\r\n";

		ChannelBuffer buf = str2cb(msg);

		response.setContent(buf);

		e.getChannel().write(response);

		e.getChannel().close();

	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {		
		debug.logHttpClientHandler("HttpClientHandler, channelClosed()");
		UUID uuid = SessionAttrs.uuid.get(e.getChannel());

		if (uuid == null) {
			Stats.getInstance().updateAll();
//			ctx.sendUpstream(e);
			return;
		}

		Broadcast bcast = Shows.getInstance().getBroadcast(uuid);

		if (bcast == null) {
			Stats.getInstance().updateAll();
			ctx.sendUpstream(e);
			return;
		}

		// refresh stats in server
		Stats.getInstance().sendStats(bcast);

		SessionAttrs.uuid.remove(ctx.getChannel());
		
		if ( Config.getInstance().useAuth() ) {
			String user_id = SessionAttrs.user_id.get(e.getChannel());
			if ( user_id != null ) {
				AuthUser.getInstance().removeListener(bcast.getUUID(), user_id);
			} else {
				/* something went wrong, clear everything just in case */
				debug.logHttpClientHandler("channelClosed, clearing all in AuthUser");
				AuthUser.getInstance().removeAll();
			}
		}

		ctx.sendUpstream(e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		debug.logError("HttpClientHandler, exceptionCaught() exception ", e.getCause());		
		e.getChannel().close();
	}

	private String getPath(String uri) {
		String url = uri.substring(1);

		String[] ret;

		ret = url.split("\\?", 2);

		if (ret.length == 0)
			return null;

		return ret[0];
	}
	
	private String getGetValue(String uri, String name) {
		String url = uri.substring(1);
		
		String[] ret;
		
		ret = url.split("\\?", 2);

		try {
			ret = ret[1].split("&");	
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
		
		
		for ( String s : ret ) {
			String arg[] = s.split("=",2);
			if ( arg[0].equalsIgnoreCase(name)) {
				return arg[1];
			}	
		}
		
		return null;
	}

	/* make a ChannelBuffer from a String */
	private ChannelBuffer str2cb(String msg) {
		ChannelBuffer buf = buffer(msg.length());
		byte[] b = new byte[msg.length()];
		ByteArrayInputStream bais = null;
		try {
			bais = new ByteArrayInputStream(msg.getBytes("ISO-8859-1"));
		} catch (UnsupportedEncodingException e2) {
			debug.logError("FzTranscoder, str2cb() exception ", e2);
		}
		try {
			if (bais != null)
				bais.read(b);
		} catch (IOException e1) {
			debug.logError("FzTranscoder, str2cb() exception ", e1);
		}
		buf.writeBytes(b);

		return buf;
	}
}
