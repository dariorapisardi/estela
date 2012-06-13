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
import java.util.UUID;

import com.manybrain.persistent.MemCacheClient;


/**
 * Singleton object that keeps track of listeners, to avoid
 * duplicates.
 * This is validated against a memcached DDBB. 
 * It's not necessary and wasn't really used, it was more a proof of concept.
 * 
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class AuthUser {
	private static AuthUser INSTANCE = new AuthUser();
	private Debug debug = Debug.getInstance(); 
	
	private ArrayList<String> listeners = new ArrayList<String>();
	private MemCacheClient c = null;
	private AuthUser() {
	}
	
	public static AuthUser getInstance() {
		return INSTANCE;
	}
	
	public boolean isAuthenticated(String show_name, String user_id, UUID uuid) {
		String key = ":1:" + show_name + user_id;
		
		if ( c == null ) {			
			try {
				Config conf = Config.getInstance();
				String[] servers ={ conf.getMemcacheAddress() + ":" + conf.getMemcachePort().toString()};
				int weights[] ={ 3 };
				c = new MemCacheClient(servers, weights );
			} catch (Exception e) {
				debug.logError("AuthUser, error connecting with memcache ", e);
				return true;
			}			
		}
		

		try {
			c.set("test", "value");	
		} catch ( Exception e ) {
			/* something went wrong, validate */
			debug.logClientAuth("isAuthenticated: something wrong, validating...");
			c = null;
			return true;
		}
		
		
		if ( c != null ) {
			Object ret;
			try {
				ret = c.get(key);
			} catch (Exception e) {
				debug.logError("isAuthenticated error in get ", e);
				return true;
			}

			
			if ( ret == null ) {
				debug.logClientAuth("isAuthenticated, GOT NULL");				
				return false;
			} else {
				String lkey = uuid.toString() + user_id;
				if ( listeners.contains(lkey)) {
					debug.logClientAuth("isAuthenticated: duplicated key " + lkey);
					return false;
				}
				debug.logClientAuth("isAuthenticated: adding key " + lkey);
				listeners.add(lkey);
				return true;
			}
		}
			
		
		/* in case we have no memcache connection */
		debug.logClientAuth("isAuthenticated: something wrong, validating...");
		return true;
	}
	
	public void removeListener(UUID uuid, String user_id) {
		String key = uuid.toString() + user_id;
		listeners.remove(key);
		
		debug.logClientAuth("removeListener, key " + key + " list len " + listeners.size());
	}
	
	public void removeAll() {
		debug.logClientAuth("removeAll() called");
		
		listeners.clear();
	}
	
}
