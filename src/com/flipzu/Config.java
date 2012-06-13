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


import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Represents server configuration (properties file)
 *
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class Config {
	private static Config INSTANCE = new Config();
	
	private static Configuration config;
	private Config() {
		try {
			config = new PropertiesConfiguration("estela.properties");
		} catch (ConfigurationException e) {
			config = null;
			Debug.getInstance().logError("Config, Config() exception ", e); 
		}
	}
	
	public static Config getInstance() {
		return INSTANCE;
	}
	
	public Integer getReadPort() {
		return config.getInt("readPort", 10003);
	}
	
	public Integer getWritePort() {
		return config.getInt("writePort", 10002);
	}
	
	public boolean isTranscoder() {
		return config.getBoolean("transcoder", true);
	}
	
	public String getServerName() {
		return config.getString("serverName", "estela");	
	}

	public boolean isLogAuth() {
		return config.getBoolean("logAuth", true);
	}

	public boolean isLogChannelWriter() {
		return config.getBoolean("logChannelWriter", true);
	}

	public boolean isLogEstelaHandler() {
		return config.getBoolean("logEstelaHandler", true);
	}

	public boolean isLogFileWriter() {
		return config.getBoolean("logFileWriter", true);
	}
	
	public boolean isLogHttpClientHandler() {
		return config.getBoolean("logHttpClientHandler", true);
	}
	
	public boolean isLogShows() {
		return config.getBoolean("logShows", true);
	}
	
	public boolean isLogTranscoder() {
		return config.getBoolean("logTranscoder", true);
	}
	
	public boolean isLogBroadcast() {
		return config.getBoolean("logBroadcast",true);
	}
	
	public boolean isLogBufferWriter() {
		return config.getBoolean("logBufferWriter",true);
	}
	
	public Integer getBufferSize () {
		Integer bufferSize = (config.getInteger("bufferLength", 20)*this.getOutputBitrate())/8;
		
		return bufferSize; // in bytes
	}
	
	public boolean isLogBcastDecoder() {
		return config.getBoolean("logBcastDecoder", true);
	}
	
	public boolean isLogFlipInterface() {
		return config.getBoolean("logFlipInterface", true);
	}

	public boolean isTestMode() {
		return config.getBoolean("testMode", false);
	}

	public String getTestUsername() {
		return config.getString("testUsername", "tester");
	}

	public Integer getFrameSize() {
		return config.getInt("frameSize", 2048);
	}
	
	public Integer getMinBytes() {
		Integer minBytes = this.getFrameSize();
		
		if ( !this.isTestMode() ) {
			minBytes += this.getKeyLength();
		}
		return minBytes;
	}

	public String getOutputCodec() {
		return config.getString("outputCodec", "mp3");
	}

	public boolean isFileWriter() {
		return config.getBoolean("fileWriter", true);
	}

	public String getTcoder() {
		return config.getString("tcoder", "ffmpeg");
	}

	public Integer getOutputBitrate() {
		return config.getInteger("outputBitrate", 32768);
	}

	public boolean isLogStats() {
		return config.getBoolean("logStats", true);
	}

	public String getLoggerHost() {
		return config.getString("loggerHost", "127.0.0.1");
	}

	public Integer getLoggerPort() {
		return config.getInteger("loggerPort", 10006);
	}
	
	public boolean isLogEstela() {
		return config.getBoolean("logEstela", true);
	}
	
	public String getFileWriterDestDir() {
		return config.getString("fileWriterDestDir", "/aircasts");
	}
	
	public String getFileWriterExtension() {
		return config.getString("fileWriterExtension", ".mp3");
	}
	
	public Integer getTestBcastId() {
		return config.getInt("testBcastId", 1234);
	}
	
	public Integer getKeyLength() {
		return config.getInt("leyLength", 32);
	}
	
	public String getReadAddress() {
		return config.getString("readAddress", "0.0.0.0");
	}
	
	public String getWriteAddress() {
		return config.getString("writeAddress", "0.0.0.0");
	}
	
	public boolean isLogTimeout() {
		return config.getBoolean("logTimeout", true);
	}
	
	public Integer getBcastTimeout() {
		return config.getInt("bcastTimeout", 60);
	}
	
	public boolean isLogCleanup() {
		return config.getBoolean("logCleanup", true);
	}
	
	public boolean isPostprocessor() {
		return config.getBoolean("postprocessor", true);
	}
	
	public Integer getPostProcDelay() {
		return config.getInt("postProcDelay", 60);
	}
	
	public boolean useS3() {
		return config.getBoolean("useS3", true);
	}
	
	public String getS3dir () {
		return config.getString("S3dir", "aircasts");
	}
	
	public String getFfmpegCommand() {
		return config.getString("ffmpegCmd", "/usr/bin/ffmpeg");
	}
	
	public boolean isLogPostProc () {
		return config.getBoolean("logPostProc", true);
	}
	
	public String getWSServer() {
		return config.getString("WSServer", "http://flipzu.com");
	}
	
	public String getS3Bucket() {
		return config.getString("S3bucket", "fz_aircasts_bucket");
	}
	
	public boolean isLogQueryResponses() {
		return config.getBoolean("logQueryResponses", true);
	}
	
	public boolean isLogRelayHandler() {
		return config.getBoolean("logRelayHandler", true);
	}
	
	public String getFzFormatCmd() {
		return config.getString("fzFormatCmd", "/usr/bin/getformat");
	}
	
	public Integer getOutputChannels() {
		return config.getInteger("outputChannels", 1);
	}
	
	public Integer getOutputSampleRate() {
		return config.getInteger("outputSampleRate", 22050);
	}
	
	public String getOutputCodecFlag() {
		return config.getString("outputCodecFlag", "-f");
	}
	
	public String getOutputBitrateFlag() {
		return config.getString("outputBitrateFlag", "-ab");
	}
	
	public String getOutputFileFlag() {
		return config.getString("outputFileFlag", "");
	}
	
	public String getInputFileFlag() {
		return config.getString("inputFileFlag", "-i");
	}
	
	public String getSCKey() {
		return config.getString("scKey", "null");
	}
	
	public String getSCSecret() {
		return config.getString("scSecret", "null");
	}
	
	public String getSCArtwork() {
		return config.getString("scArtwork", "/jestela/sc.png");
	}
	
	public Integer getMinimumLength() {
		return config.getInteger("minLength", 5);
	}
	
	public boolean deleteSmallBcasts() {
		return config.getBoolean("deleteSmallBcasts", false);
	}
	
	public boolean useAuth() {
		return config.getBoolean("useAuth", false);
	}
	
	public String getMemcacheAddress() {
		return config.getString("memcacheAddress", "127.0.0.1");
	}
	
	public Integer getMemcachePort() {
		return config.getInteger("memcachePort", 11211);
	}
	
	public boolean isLogClientAuth() {
		return config.getBoolean("logClientAuth", true);
	}
}
