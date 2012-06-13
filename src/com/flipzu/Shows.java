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


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;

import org.jboss.netty.channel.Channel;


/**
 * Shows singleton class, keeps track of everything 
 * that's happening in Estela.
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class Shows {
	
	private final Debug debug = Debug.getInstance();

	private static Shows INSTANCE = new Shows();

	private static Hashtable<UUID, Broadcast> shows = new Hashtable<UUID, Broadcast>();
	private Shows() {}
	
	public static Shows getInstance() {
		return INSTANCE;
	}
	
	public synchronized void addBroadcast( Broadcast bcast ) {
		debug.logShows("Shows(), addBroadcast for uuid " + bcast);
		shows.put(bcast.getUUID(), bcast);
		Stats.getInstance().sendStats(bcast);
	}
	
	public Broadcast getBroadcast( UUID uuid ) {
		if ( shows == null || uuid == null )
			return null;
		
		if ( shows.containsKey(uuid) ) {
			Broadcast b = shows.get(uuid);
			debug.logShows("Shows, getBroadcast(), returning show for " + b);
			return b;
		}
		
		debug.logShows("Shows, getBroadcast(), no broadcast for " + uuid.toString());
		return null;
	}
	
	/*
	 * checks if a broadcast exists for @uuid
	 */
	public boolean hasBroadcast( UUID uuid ) {
		if ( shows.containsKey(uuid )) {
//			debug.logShows("Shows, hasBroadcast() TRUE for " + uuid.toString());
			return true;
		}
		
		debug.logShows("Shows, hasBroadcast() FALSE for " + uuid.toString());
		return false;
	}
	
	public synchronized void addChannelToBroadcast( UUID uuid, Channel channel ) {
		if ( !shows.containsKey(uuid)) {
			debug.logShows("Shows, addChannelToBroadcast(). UUID " + uuid.toString() + " not found");
			return;
		}
		
		Broadcast bcast = shows.get(uuid);
		bcast.addChannel(channel);
		shows.put(uuid, bcast);
		
		debug.logShows("Shows, addChannelToBroadcast() for " + bcast);
	}
	
	public synchronized void delBroadcast( UUID uuid ) {
		if ( uuid == null ) {
			debug.logShows("Shows, delBroadcast(), got NULL uuid");
			Stats.getInstance().updateAll();
			return;
		}

		debug.logShows("Shows, delBroadcast() deleting broadcast for " + uuid.toString());
		
		Broadcast bcast = shows.get(uuid);
		
		if ( shows.remove(uuid) == null ) {
			debug.logShows("Shows, delBroadcast(), " + uuid.toString() + " not found...");
			Stats.getInstance().updateAll();
			return;
		}
				
		if ( bcast == null ) {
			debug.logShows("Shows, delBroadcast(), bcast is NULL for " + uuid.toString());
		} else {
			Stats.getInstance().closeBcast(bcast);	
		}
	}
	
	public ArrayList<Broadcast> getBroadcasts() {
		Enumeration<Broadcast> e = shows.elements();
		
		ArrayList<Broadcast> ret = new ArrayList<Broadcast>();
		
		while ( e.hasMoreElements() ) {
			Broadcast b = e.nextElement();	
			ret.add(b);
		}
		
		return ret;
	}
	
	public synchronized void stopBroadcast( UUID uuid ) {
		if (!shows.containsKey(uuid)) {
			debug.logShows("Shows, stopBroadcast() stop broadcast for " + uuid.toString() + " not found ");
			return;
		}

		Broadcast bcast = shows.get(uuid);
		bcast.stop();
		
		debug.logShows("Shows, stopBroadcast() stop broadcast for " + bcast);
	}
	
	/* returns latest broadcast by username, if any */
	public Broadcast getBroadcast( String username ) {
		Enumeration<Broadcast> e = shows.elements();
		Broadcast retBcast = null;
		
		while ( e.hasMoreElements() ) {
			Broadcast b = e.nextElement();
			if ( b.getUsername().equals(username)) {
				if ( retBcast == null ) 
					retBcast = b;
				else {
					if ( b.getStartTime().after(retBcast.getStartTime()))
						retBcast = b;
				}
			}
		}
		
		return retBcast;
	}
	
	/* returns if this broadcast is the latest for this user */
	public boolean isActiveBroadcast( Broadcast bcast ) {
		boolean ret = false;
		
		Broadcast b = getBroadcast(bcast.getUsername());
		
		if (b.getUUID().equals(bcast.getUUID())) {
			debug.logShows("Shows, isActiveBroadcast TRUE for " + bcast.getUUID());
			ret = true;
		} else {
			debug.logShows("Shows, isActiveBroadcast FALSE for " + bcast.getUUID());
		}
		
		return ret;
	}
	
}
