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


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * Tries to guess the audio format, based on "getformat"
 * external tool (included in the "cmd" directory)
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class FzFormat {
	
	public String getFormat( byte[] buf ) {
		
		byte[] b = null;
		
		try {
            Process p = Runtime.getRuntime().exec(Config.getInstance().getFzFormatCmd());
            
            BufferedInputStream stdOut = new BufferedInputStream(p.getInputStream());
            BufferedOutputStream stdIn = new BufferedOutputStream(p.getOutputStream());
            
            stdIn.write(buf);
            stdIn.flush();
            stdIn.close();
	            
            b = new byte[10]; // just in case, returning string should be like 3 bytes long...
            stdOut.read(b);
	    } catch (IOException e) {
	    	Debug.getInstance().logError("FzFormat, getFormat error", e);
	    }
	   
	    String ret = null;
	    
	    if ( b != null ) {
	    	ret = new String(b);
	    	ret = ret.trim();
	    	if ( ret.equalsIgnoreCase("none") ) {
	    		ret = null;
	    	}
	    }

		return ret;
	}
}
