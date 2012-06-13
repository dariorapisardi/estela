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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for Apache Logging.
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class Debug {
	private static Debug INSTANCE = new Debug();

    private Log log = LogFactory.getLog(Debug.class);

    private final boolean logEstela = Config.getInstance().isLogEstela();
	private final boolean logAuth = Config.getInstance().isLogAuth();
	private final boolean logChannelWriter = Config.getInstance().isLogChannelWriter();
	private final boolean logEstelaHandler = Config.getInstance().isLogEstelaHandler();
	private final boolean logFileWriter = Config.getInstance().isLogFileWriter();
	private final boolean logHttpClientHandler = Config.getInstance().isLogHttpClientHandler();
	private final boolean logShows = Config.getInstance().isLogShows();
	private final boolean logTranscoder = Config.getInstance().isLogTranscoder();
	private final boolean logBroadcast = Config.getInstance().isLogBroadcast();
	private final boolean logBufferWriter = Config.getInstance().isLogBufferWriter();
	private final boolean logBcastDecoder = Config.getInstance().isLogBcastDecoder();
	private final boolean logFlipInterface = Config.getInstance().isLogFlipInterface();
	private final boolean logStats = Config.getInstance().isLogStats();
	private final boolean logTimeout = Config.getInstance().isLogTimeout();
	private final boolean logCleanup = Config.getInstance().isLogCleanup();
	private final boolean logPostProc = Config.getInstance().isLogPostProc();
	private final boolean logQueryResponses = Config.getInstance().isLogQueryResponses();
	private final boolean logRelayHandler = Config.getInstance().isLogRelayHandler();
	private final boolean logClientAuth = Config.getInstance().isLogClientAuth();
	
	private Debug() {}
	
	public static Debug getInstance() {
		return INSTANCE;
	}
	
	public void logAuth(String msg) {
		if ( logAuth ){
			log.info(msg);	
		}
	}
	
	public void logChannelWriter( String msg ) {
		if ( logChannelWriter ) {
			log.info(msg);
		}
	}
	
	public void logEstelaHandler( String msg ) {
		if ( logEstelaHandler ) {
			log.info(msg);
		}
	}
	
	public void logError ( String msg, Exception e ) {
		log.error(msg, e);
	}
	
	public void logError ( String msg, Throwable e ) {
		log.error(msg, e);
	}
	
	public void logFileWriter( String msg ) {
		if ( logFileWriter ) {
			log.info(msg);
		}
	}
	
	public void logHttpClientHandler ( String msg ) {
		if ( logHttpClientHandler ) {
			log.info(msg);
		}
	}
	
	public void logShows ( String msg ) {
		if ( logShows ) {
			log.info(msg);
		}
	}
	
	public void logTranscoder ( String msg ) {
		if ( logTranscoder ) {
			log.info(msg);
		}
	}
	
	public void logBroadcast( String msg ) {
		if ( logBroadcast ) {
			log.info(msg);
		}
	}
	
	public void logBufferWriter( String msg ) {
		if ( logBufferWriter ) {
			log.info(msg);
		}
	}
	
	public void logBcastDecoder ( String msg ) {
		if ( logBcastDecoder ) {
			log.info(msg);
		}
	}
	
	public void logFlipInterface ( String msg ) {
		if ( logFlipInterface ) {
			log.info(msg);
		}
	}
	
	public void logStats ( String msg ) {
		if ( logStats ) {
			log.info(msg);
		}
	}
	
	public void logEstela ( String msg ) {
		if ( logEstela ) {
			log.info(msg);
		}
	}
	
	public void logTimeout ( String msg ) {
		if ( logTimeout ) {
			log.info(msg);
		}
	}
	
	public void logCleanup ( String msg ) {
		if ( logCleanup ) {
			log.info(msg);
		}
	}
	
	public void logPostProc ( String msg ) {
		if ( logPostProc ) {
			log.info(msg);
		}
	}
	
	public void logQueryResponse ( String msg ) {
		if ( logQueryResponses ) { 
			log.info(msg);
		}
	}
	
	public void logRelayHandler ( String msg ) {
		if ( logRelayHandler ) {
			log.info(msg);
		}
	}
	
	public void logClientAuth ( String msg ) {
		if ( logClientAuth ) {
			log.info(msg);
		}
	}
}
