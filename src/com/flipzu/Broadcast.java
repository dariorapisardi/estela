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

import static org.jboss.netty.buffer.ChannelBuffers.directBuffer;

import java.util.Date;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * Represents a Broadcast, with related client Channels,
 * buffers, etc.
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class Broadcast {
	private final Debug debug = Debug.getInstance();
	private String username;
	private Integer uid;
	private Integer id;
	private String title;
	private ChannelGroup channels = new DefaultChannelGroup();
	// shared buffer for this broadcast
	private ChannelBuffer bcast_buffer = directBuffer(Config.getInstance().getBufferSize());
	private String codec = null;
	private boolean transcode = false;
	private BroadcastState state = null;
	private String filename = null;
	private UUID uuid = null;
	private Date startTime = null;
	private boolean storage = true;
	private boolean sc_share = false;
	private String sc_token;
	private Integer nr_channels;
	private Integer bitrate;
	private Integer encrate;
	private String key;
	 
	
	public Broadcast() {		
		bcast_buffer.clear();
		uuid = UUID.randomUUID();
		debug.logBroadcast("Broadcast(), constructor with UUID " + uuid);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public ChannelGroup getChannels() {
		return channels;
	}
	
	public void addChannel( Channel channel ) {
		channels.add(channel);
	}
	
	public ChannelBuffer getBuffer ( ) {
		return bcast_buffer.duplicate();
	}
	
	public void writeBuffer ( ChannelBuffer buf ) {		
		// check if we have space in buffer
		if ( bcast_buffer.writableBytes() < buf.readableBytes() ) {
//			debug.logBroadcast("Broadcast(), writeBuffer, discarding bytes...");
			// make space
			bcast_buffer.readerIndex(buf.readableBytes());
			bcast_buffer.discardReadBytes();
			// last measure, just in case...
			if ( bcast_buffer.writableBytes() < buf.readableBytes() ) {
				debug.logBroadcast("Broadcast(), writeBuffer, clearing buffer!!");
				bcast_buffer.clear();
			}
		}
				
		// write into general buffer
		int index = buf.readerIndex();
		bcast_buffer.writeBytes(buf);
		debug.logBroadcast("Broadcast(), writeBuffer user " + username + " available: " + bcast_buffer.writableBytes() + " channels " + channels.size());
		
		// write into channels
		buf.readerIndex(index);
		if ( channels.size() > 0 ) {
			channels.write(buf.readBytes(buf.readableBytes()));
		}
	}

	public Integer getUid() {
		return uid;
	}

	public void setUid(Integer uid) {
		this.uid = uid;
	}

	public String getCodec() {
		return codec;
	}

	public void setCodec(String codec) {
		this.codec = codec;
	}

	public boolean isTranscode() {
		return transcode;
	}

	public void setTranscode(boolean transcode) {
		this.transcode = transcode;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public BroadcastState getState() {
		return state;
	}

	public void setState(BroadcastState state) {
		this.state = state;
	}
	
	public void start() {
		this.startTime = new Date();
		this.setState(BroadcastState.LIVE);
	}
	
	public void stop() {
		this.setState(BroadcastState.PROCESSING);
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	@Override
	public Broadcast clone() {
		debug.logBroadcast("Broadcast, cloning " + this.uuid); 
		Broadcast ret = new Broadcast();
		ret.username = this.username;
		ret.uid = this.uid;
		ret.id = this.id;
		ret.codec = this.codec;
		ret.title = this.title;
		ret.transcode = this.transcode;
		ret.state = this.state;
		ret.filename = this.filename;
		ret.uuid = this.uuid;
		ret.startTime = this.startTime;
		ret.storage = this.storage;
		ret.sc_share = this.sc_share;
		ret.sc_token = this.sc_token;
		ret.nr_channels = this.nr_channels;
		ret.bitrate = this.bitrate;
		ret.encrate = this.encrate;
		ret.key = this.key;
		
		return ret;
	}
	
	public UUID getUUID () {
		return this.uuid;
	}
	
	public Date getStartTime () {
		return this.startTime;
	}
	
	@Override
	public String toString() {
		return this.username + "/" + this.id + "/" + this.uuid.toString();
	}
	
	@Override
	protected void finalize() throws Throwable {
		debug.logBroadcast("Broadcast, finalize " + this.uuid);		
		super.finalize();
	}	
	
	public void setUuid( UUID uuid ) {
		this.uuid = uuid;
	}

	public boolean isStorage() {
		return storage;
	}

	public void setStorage(boolean storage) {
		this.storage = storage;
	}
	
	public boolean isScShare() {
		return sc_share;
	}
	
	public void setScShare(boolean scShare) {
		this.sc_share = scShare;
	}
	
	public void setScToken(String token){
		this.sc_token = token;
	}
	
	public String getScToken() {
		return sc_token;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return this.title;
	}

	public Integer getNrChannels() {
		return nr_channels;
	}

	public void setNrChannels(Integer nrChannels) {
		nr_channels = nrChannels;
	}

	public Integer getBitrate() {
		return bitrate;
	}

	public void setBitrate(Integer bitrate) {
		this.bitrate = bitrate;
	}

	public Integer getEncrate() {
		return encrate;
	}

	public void setEncrate(Integer encrate) {
		this.encrate = encrate;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}		
	
}
