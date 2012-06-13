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


/**
 * Represents a Estela Server (for API calls with EstelaStats server)
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class EstelaServer {
	private String hostname;
	private String ip;
	private Integer read_port;
	private Integer write_port;

	public EstelaServer( String hostname, String ip, Integer read_port, Integer write_port ) {
		this.hostname = hostname;
		this.ip = ip;
		this.read_port = read_port;
		this.write_port = write_port;
	}
	
	public String getHostname() {
		return hostname;
	}

	public String getIp() {
		return ip;
	}
	
	public void setIp( String ip ) {
		this.ip = ip;
	}

	public Integer getReadPort() {
		return read_port;
	}
	
	public Integer getWritePort() {
		return write_port;
	}
	
	@Override
	public boolean equals(Object other) {
		EstelaServer o = (EstelaServer) other;
		
		if ( this.hostname.equals(o.hostname) && 
				this.ip.equals(o.ip) &&
				this.read_port.equals(o.read_port) &&
				this.write_port.equals(o.write_port)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return this.hostname + "/" + this.ip + ":" + this.read_port + ":" + this.write_port;
	}
}

