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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Estela Interface with Flipzu Web Services
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class FlipInterface {
	private final Debug debug = Debug.getInstance();

	private static final String url = Config.getInstance().getWSServer();
	private static final String verify_key = "/api/verify_key.xml2/";
	private static final String start_rec = "/api_priv/start_rec.xml/";
	private static final String stop_rec = "/api_priv/stop_rec.xml/";
	private static final String delete_aircast = "/api/delete_aircast/";
	
	private String username = null;
	private Integer uid = null;
	private Integer bcastId = null;
	private String bcastTitle = null;
	private boolean storage = true;
	private boolean sc_share = false;
	private String sc_token = null;
	private Integer channels = null;
	private Integer bitrate = null;
	private Integer encrate = null;
	
	public boolean verifyKey ( String key ) {
		boolean ret = false;
		
		debug.logFlipInterface("verifyKey(), got key " + key);
		
		if ( key == null ) 
			return ret;		
		
		if (!this.getVerifyKey(key))
			return ret;
		
		if(!this.startRec(key))
			return ret;
		
		ret = true;
		
		return ret;
	}
	
	public String getUsername () {
		return username;
	}
	
	public Integer getUid () {
		return uid;
	}
	
	public Integer getBcastId() {
		return bcastId;
	}
	
	public boolean isStorage() {
		return this.storage;
	}
	
	public boolean isScShare() {
		return this.sc_share;
	}
	
	public String getBcastTitle() {
		return this.bcastTitle;
	}
	
	public String getScToken() {
		return this.sc_token;
	}
	
	public Integer getChannels() {
		return channels;
	}

	public Integer getBitrate() {
		return bitrate;
	}

	public Integer getEncrate() {
		return encrate;
	}
	
    private String getViaStreamConnection(String server, String path) throws IOException {
        HttpURLConnection c = null;
        InputStream s = null;

        URL url = new URL(server + path);
        String respString = null;

        try {

            c = (HttpURLConnection)url.openConnection();

            s = c.getInputStream();

            byte[] data = new byte[8192];
            int ch;
            int i = 0;
            while ((ch = s.read()) != -1) {
                if ( i < data.length )
                        data[i] = ((byte)ch);
                i++;

            }
            respString = new String(data);

        } catch (Exception ex) {

                throw new IOException ("Error: " + ex);

        } finally {
            if (s != null)
                s.close();
            if (c != null)
                c.disconnect();
        }
        return respString;
    }

    private boolean getVerifyKey( String key ) {
    	boolean ret = false;
    	String response = null;
    	String path = verify_key + key;
    	
    	debug.logFlipInterface("getVerifyKey(), using path " + path);
    	
    	try {
			response = this.getViaStreamConnection(url, path);
			if ( response != null ) {
				response = response.trim();
				Element rootElement = getRootElement(response);
				if (getResponse(rootElement).equals("OK")) {
					debug.logFlipInterface("getVerifyKey OK");
					uid = Integer.parseInt(getNodeValue(rootElement, "user"));
					username = getNodeValue(rootElement, "username");
					channels = Integer.parseInt(getNodeValue(rootElement, "channels"));
					bitrate = Integer.parseInt(getNodeValue(rootElement, "bitrate"));
					encrate = Integer.parseInt(getNodeValue(rootElement, "encrate"));
					int storage = Integer.parseInt(getNodeValue(rootElement, "storage"));
					if ( storage == 0 ) {
						this.storage = false;
					}
					int sc_share = Integer.parseInt(getNodeValue(rootElement, "sc_share"));
					if ( sc_share == 1 ) {
						this.sc_share = true;
						sc_token = getNodeValue(rootElement, "sc_token");						
					}
					bcastTitle = getNodeValue(rootElement, "title");
					debug.logFlipInterface("getVerifyKey(), got username " + username + " title " + bcastTitle + " uid " + uid  + " storage " + storage + " sc_share " + sc_share + " token " + sc_token);
					if ( username != null && uid != null )
						ret = true;					
				}
			} else {
				debug.logFlipInterface("getVerifyKey(), got NULL response");
			}
		} catch (Exception e) {
			debug.logFlipInterface("FlipInterface, getVerifyKey() can't process " + path + " " + e.getMessage()); 
		}		
				
		return ret;
    }
    
    private boolean startRec ( String key ) {
    	boolean ret = false;
    	String response = null;
    	String path = start_rec + key;
    	
    	debug.logFlipInterface("startRec(), using path " + path);
    	
    	try {
			response = this.getViaStreamConnection(url, path);
		} catch (IOException e) {
			debug.logFlipInterface("FlipInterface, startRec() can't process " + path + " " + e.getMessage()); 
		}
		
		if ( response != null ) {
			response = response.trim();
			debug.logFlipInterface("startRec(), got response " + response);
			String[] fields = response.split("//", 2);
			if (fields[0].equalsIgnoreCase("OK")) {
				bcastId = Integer.parseInt(fields[1]);				
				debug.logFlipInterface("startRec(), got bcastId " + bcastId);
				if ( bcastId != null )
					ret = true;
			}
		} else {
			debug.logFlipInterface("startRec(), got NULL response!");
		}
    	
    	return ret;
    }
    
    public boolean stopRec( ) {
    	boolean ret = false;
    	String response = null;
    	String path = stop_rec + bcastId;
    	
    	if ( bcastId == null ) {
    		debug.logFlipInterface("stopRec(), stopRec(), no bcastId");
    		return ret;
    	}
    	
    	debug.logFlipInterface("stopRec(), using path " + path);
    	
    	try {
			response = this.getViaStreamConnection(url, path);
		} catch (IOException e) {
			debug.logFlipInterface("FlipInterface, stopRec() can't process " + path + " " + e.getMessage());
		}


		if ( response != null ) {
			response = response.trim();
			debug.logFlipInterface("stopRec(), got response " + response);
			String[] fields = response.split("//", 2);
			if (fields[0].equalsIgnoreCase("OK")) {
				ret = true;
			}
		} else {
			debug.logFlipInterface("stopRec(), got NULL response!");
		}
    	
    	return ret;
    }
    
    public Element getRootElement(String resp) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        resp = this.replaceAll(resp, "\0", "");

        byte[] bytes = resp.getBytes(); // IMPORTANT FOR NULL CHARACTERS
        
        Document document = null;
        try {
        	document = builder.parse(new ByteArrayInputStream(bytes));	
        } catch ( SAXParseException e ) {
        	debug.logError("FlipInterface, getRootElement, ", e);
        }

        // Normalize the root element of the XML document.  This ensures that all Text 
        // nodes under the root node are put into a "normal" form, which means that 
        // there are neither adjacent Text nodes nor empty Text nodes in the document.
        // See Node.normalize().
        
        Element rootElement = null;
        if ( document != null ) {
        	rootElement = document.getDocumentElement();	
        	rootElement.normalize();
        }

        return rootElement;

    }

	public String getResponse(Element rootElement) {
	
		if ( rootElement == null ) {
			debug.logFlipInterface("FlipInterface, getResponse, got NULL rootElement");
			return null;
		} 
		debug.logFlipInterface("FlipInterface, getResponse(): " + rootElement.getChildNodes());
		return rootElement.getChildNodes().item(1).getAttributes().getNamedItem("status").getNodeValue();
	
	}
	
	public String getNodeValue(Element rootElement, String itemname) {
	        NodeList i = rootElement.getElementsByTagName(itemname);
	
	        Node r = null;
	        if((r=i.item(0)) != null) {
	        	debug.logFlipInterface("FlipInterface, getNodevalue(): " + r.getChildNodes().item(0).getNodeValue());
	        	return r.getChildNodes().item(0).getNodeValue();
	        } else {
	        	return null;
	        }
	}
	
    public String replaceAll(String text, String searchString, String replacementString) {
    	StringBuffer sBuffer = new StringBuffer();
    	int pos = 0;
    	while ((pos = text.indexOf(searchString)) != -1) {
    			sBuffer.append(text.substring(0, pos) + replacementString);
    			text = text.substring(pos + searchString.length());
    	}
    	sBuffer.append(text);
    	return sBuffer.toString();
    }
    
    public boolean deleteAircast ( String key ) {
    	boolean ret = false;
    	String response = null;
    	String path = delete_aircast + key;
    	
    	debug.logFlipInterface("deleteAircast(), using path " + path);
    	
    	try {
			response = this.getViaStreamConnection(url, path);
		} catch (IOException e) {
			debug.logFlipInterface("FlipInterface, deleteAircast() can't process " + path + " " + e.getMessage()); 
		}
		
		if ( response != null ) {
			response = response.trim();
			debug.logFlipInterface("deleteAircast(), got response " + response);
			String[] fields = response.split("//", 2);
			if (fields[0].equalsIgnoreCase("OK")) {
				bcastId = Integer.parseInt(fields[1]);				
				debug.logFlipInterface("deleteAircast(), got bcastId " + bcastId);
				if ( bcastId != null )
					ret = true;
			}
		} else {
			debug.logFlipInterface("deleteAircast(), got NULL response!");
		}
    	
    	return ret;
    }
}
