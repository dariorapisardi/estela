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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TimerTask;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Env;
import com.soundcloud.api.Http;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

/**
 * post processing tasks thread
 * (upload to Amazon S3, SoundCloud, etc)
 * @author Dario Rapisardi <dario@rapisardi.org>
 *
 */
public class PostProcThread extends TimerTask {

	private Debug debug = Debug.getInstance();
	
	private Broadcast bcast;
	
	public PostProcThread ( Broadcast bcast ) {
		this.bcast = bcast;
	}
	
	@Override
	public void run() {
		
		if ( bcast == null ) {
			debug.logPostProc("PostProcThread, run(), got NULL bcast!");
			return;
		}
		
		debug.logPostProc("PostProcThread, run() for " + bcast);
				
		Broadcast nowBcast = Shows.getInstance().getBroadcast(bcast.getUsername());

		/* if we're still live, exit */
		if ( bcast.getState() == BroadcastState.LIVE ) {
			debug.logPostProc("PostProcThread, " + bcast + "still LIVE");
			return;
		}

		/* check if we reconnected */
		if ( bcast.getId().equals(nowBcast.getId())) {
			if ( bcast.getUUID().equals(nowBcast.getUUID()) == false ) {
				debug.logPostProc("PostProcThread for " + bcast + ", UUID MISMATCH orig UUID " + bcast.getUUID() + " new " + nowBcast.getUUID() + " removing old one");
				Shows.getInstance().delBroadcast(bcast.getUUID());
				return;
			}
			
			if ( nowBcast.getState() == BroadcastState.LIVE ) {
				debug.logPostProc("PostProcThread, " + bcast + " reconnected");
				return;
			}			
			
//			if ( nowBcast.getStartTime().after(bcast.getStartTime())) {
//				debug.logPostProc("PostProcThread, this was not the last broadcast for " + bcast);
//				return;				
//			}
		}
		
		/* in case we have a small reconnect... */
		if ( bcast.getFilename() == null ) {
			String dest = Config.getInstance().getFileWriterDestDir() + "/" + bcast.getId() + Config.getInstance().getFileWriterExtension();
			bcast.setFilename(dest);
			
		}
		
		/* file operations, if we have a file :) */
		if ( bcast.isStorage() ) {
			// consolidate file if it was uploaded from another instance before
			if ( Config.getInstance().useS3() ) {
				consolidateS3(bcast);
			}
			
			// then upload, AS IS and ASAP
			if ( Config.getInstance().useS3() ) {
				// retry 5 times in case it fails...
				for ( int i=0; i < 5; i++ ) {
					if (uploadToS3(bcast, false))
						break;
				}
						
			}

			// cleanup, might take some time...
			ffmpegCleanUp(bcast);
			
			// upload to SoundCloud, if necessary
			if ( bcast.isScShare() ) {
				uploadToSoundCloud(bcast);
			}					
			
			// upload cleaned file to S3
			if ( Config.getInstance().useS3() ) {
				// retry 5 times in case it fails...
				for ( int i=0; i < 5; i++ ) {
					if (uploadToS3(bcast, true))
						break;
				}				
			}			
		}
		
		// cleanup from Shows table.
		debug.logPostProc("PostProcThread, " + bcast + " not live, deleting.");
		Shows.getInstance().delBroadcast(bcast.getUUID());			
			 		
	}
	
	private void ffmpegCleanUp( Broadcast bcast ) {
		debug.logPostProc("PostProcThread, ffmpegCleanUp for " + bcast.getFilename());
		
		Process ffmpeg = null;
		String outputCodec = Config.getInstance().getOutputCodec();
		String inputFilename = bcast.getFilename();
		Integer outputBitrate = bcast.getBitrate();
		String outputFilename = Config.getInstance().getFileWriterDestDir() + "/" + bcast.getId() + "-postproc" + Config.getInstance().getFileWriterExtension();
		String ffmpegCmd = Config.getInstance().getFfmpegCommand();
		String inputFileFlag = Config.getInstance().getInputFileFlag();
		String outputBitrateFlag = Config.getInstance().getOutputBitrateFlag();
		String outputCodecFlag = Config.getInstance().getOutputCodecFlag();
		String outputFileFlag = Config.getInstance().getOutputFileFlag();
		
		try {
			ffmpeg = new ProcessBuilder(ffmpegCmd,inputFileFlag,inputFilename,outputBitrateFlag,outputBitrate.toString(),outputCodecFlag,outputCodec,"-y",outputFileFlag,outputFilename).start();
		} catch (IOException e) {
			Debug.getInstance().logError("ffmpegClenup exception", e);
		}
		
		int retcode = -1;
		try {
			retcode = ffmpeg.waitFor();
		} catch (InterruptedException e) {
			Debug.getInstance().logError("ffmpegClenup exception", e);
		}
		
		if ( retcode == 0 ) {
			File f = new File(inputFilename);
			debug.logPostProc("ffmpegCleanup, deleting " + inputFilename);
			f.delete();
			bcast.setFilename(outputFilename);
		}

		ffmpeg.destroy();
		
	}
	
	private boolean uploadToS3( Broadcast bcast, boolean delete ) {
		debug.logPostProc("PostProcThread, S3 upload for " + bcast.getFilename());
		
		if ( bcast.getFilename() == null ) {
			debug.logPostProc("PostProcThread, uploadToS3, filename is null");
			return false;
		}
		
		File file = new File(bcast.getFilename());
		if ( !file.exists() ) {
			debug.logPostProc("PostProcThread, uploadToS3, " + bcast.getFilename() + " does not exist");
			return false;
		}
		
		AmazonS3 s3 = null;

		try {
			InputStream is = new FileInputStream("aws.properties");
			s3 = new AmazonS3Client(new PropertiesCredentials(is));
		} catch (Exception e) {
			Debug.getInstance().logError("uploadToS3 Error " , e);
			return false;
		}
		
		String bucketName = Config.getInstance().getS3Bucket();
		String dirName = Config.getInstance().getS3dir();
		String objName = dirName + "/" + bcast.getId() + Config.getInstance().getFileWriterExtension();
		
		PutObjectRequest po = new PutObjectRequest(bucketName, objName, file);
		
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType("audio/mpeg");
		po.setMetadata(metadata);
		po.setCannedAcl(CannedAccessControlList.PublicRead);
		
		try {
			s3.putObject(po);
		} catch (AmazonServiceException ase) {
			debug.logPostProc("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            debug.logPostProc("Error Message:    " + ase.getMessage());
            debug.logPostProc("HTTP Status Code: " + ase.getStatusCode());
            debug.logPostProc("AWS Error Code:   " + ase.getErrorCode());
            debug.logPostProc("Error Type:       " + ase.getErrorType());
            debug.logPostProc("Request ID:       " + ase.getRequestId());
            return false;

		}  catch (AmazonClientException ace) {
			debug.logPostProc("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            debug.logPostProc("Error Message: " + ace.getMessage());
            return false;
		}
		
		if ( delete ) {
			if ( Config.getInstance().deleteSmallBcasts() ) 
				/* check and remove empty/short broadcasts */
				cleanCrappyBroadcasts(bcast.getKey(), file);
			
			debug.logPostProc("uploadToS3, deleting file " + bcast.getFilename());
			file.delete();	
		}		
		
		return true;
		
	}
	
	private void consolidateS3( Broadcast bcast ) {
		debug.logPostProc("PostProcThread, consolidate S3 for " + bcast);
		
		File file = new File(bcast.getFilename());
		if ( !file.exists() ) {
			debug.logPostProc("consolidateS3, empty broadcast, doing nothing");
			return;
		}
							
		AmazonS3 s3 = null;

		try {
			InputStream is = new FileInputStream("aws.properties");
			s3 = new AmazonS3Client(new PropertiesCredentials(is));
		} catch (Exception e) {
			debug.logError("consolidateS3 Error " , e);
			return;
		}
		
		String bucketName = Config.getInstance().getS3Bucket();
		String dirName = Config.getInstance().getS3dir();
		String objName = dirName + "/" + bcast.getId() + Config.getInstance().getFileWriterExtension();
		
		S3Object obj = null;
		try {
			obj = s3.getObject(bucketName, objName);
		} catch (AmazonServiceException ase) {
			debug.logPostProc("consolidateS3 for " + bcast + ". File not found, doing nothing...");
            return;
		}  catch (AmazonClientException ace) {
			debug.logPostProc("consolidateS3 for " + bcast + ". File not found, doing nothing...");
            return;
		}
		
		if ( obj == null ) {
			debug.logPostProc("consolidateS3 for " + bcast + ". File not found, doing nothing.");
			return;
		}
		
		debug.logPostProc("consolidateS3 for " + bcast + ". File found, consolidating.");
		
		String auxFile = Config.getInstance().getFileWriterDestDir() + "/" + bcast.getId() + "-aux" + Config.getInstance().getFileWriterExtension();
		
		BufferedOutputStream bosAux = null;
		try {
			FileOutputStream fos = new FileOutputStream(auxFile);
			bosAux = new BufferedOutputStream(fos);
		} catch (FileNotFoundException e) {
			debug.logError("consolidateS3 for, error creating output stream", e);
			return;
		}
		
		BufferedInputStream is = new BufferedInputStream(obj.getObjectContent());

		/* fetch file from S3 */
		int r = 0;
		do {
			byte[] b = new byte[1024];
			try {
				r = is.read(b);
				if ( r > 0 )
					bosAux.write(b, 0, r);
			} catch (IOException e) {
				debug.logError("consolidateS3 error", e);
				/* cleanup */
				File aFile = new File(auxFile);
				aFile.delete();
				return;
			}			
		} while ( r > 0 );
		
		try {
			is.close();
		} catch (IOException e) {
			debug.logError("consolidateS3 error", e);
		}
		
		/* append our file to aux file */
		BufferedInputStream bis;
		try {
			FileInputStream fis = new FileInputStream(bcast.getFilename());
			bis = new BufferedInputStream(fis);
		} catch (FileNotFoundException e) {
			debug.logPostProc("consolidateS3 error, FileNotFoundException");
			return;
		}
		
		r = 0;
		do {
			byte[] b = new byte[1024];
			try {
				r = bis.read(b);
				bosAux.write(b);
			} catch (IOException e) {
				debug.logError("consolidateS3 error", e);
				return;
			}			
		} while ( r > 0 );
		
		try {
			bis.close();
			bosAux.close();
		} catch (IOException e) {
			debug.logError("consolidateS3 error", e);
		}

		/* delete old crap */
		file.delete();

		bcast.setFilename(auxFile);
		
		debug.logPostProc("consolidateS3 for " + bcast + ". File consolidated in " + bcast.getFilename());
		
		return;
	}
	
	private boolean uploadToSoundCloud( Broadcast bcast) {
		
		if ( bcast == null ) {
			debug.logPostProc("uploadToSoundCloud, bcast is null");
			return false;
		}
		
		debug.logPostProc("uploadToSoundCloud for " + bcast.getFilename());
		

		if ( bcast.getScToken() == null ) {
			debug.logPostProc("uploadToSoundCloud, NULL TOKEN!");
			return false;
		}
		
		Token token = new Token(bcast.getScToken(), Token.ACCESS_TOKEN);
		debug.logPostProc("uploadToSoundCloud, authenticating with " + Config.getInstance().getSCKey() + " " + Config.getInstance().getSCSecret());
		ApiWrapper wrapper = new ApiWrapper(Config.getInstance().getSCKey(), Config.getInstance().getSCSecret(), null, token, Env.LIVE);
				
		
		File file = new File(bcast.getFilename());
		File artwork = new File(Config.getInstance().getSCArtwork());
		
		if (!file.exists()) {
			debug.logPostProc("uploaToSoundCloud: file " + bcast.getFilename() + " doesn't exist");
			return false;
		}
		
		String title = bcast.getTitle();
		if ( title == null ) {
			title = bcast.getUsername() + "'s recorded broadcast from Flipzu"; 
		}
		
		try {			
			HttpResponse resp = wrapper.post(Request.to(Endpoints.TRACKS)
                    .add(Params.Track.TITLE, title)
                    .add(Params.Track.TAG_LIST, "Flipzu " + bcast.getUsername())
                    .add(Params.Track.DESCRIPTION, "Live Broadcast from Flipzu - http://flipzu.com/" + bcast.getUsername())
                    .withFile(Params.Track.ARTWORK_DATA, artwork )
                    .withFile(Params.Track.ASSET_DATA, file));
                    // you can add more parameters here, e.g.
                    // .withFile(Params.Track.ARTWORK_DATA, file)) /* to add artwork */
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
            	debug.logPostProc("uploadToSoundCloud, 201 Created "+resp.getFirstHeader("Location").getValue());
            	debug.logPostProc("uploadToSoundCloud "+ Http.getJSON(resp).toString(4));
            } else {
            	debug.logPostProc("uploadToSoundCloud, invalid status received "+ resp.getStatusLine());
            }
		} catch (IOException e) {
			debug.logError("uploadToSoundCloud, IOException ", e);
			return false;
		} catch (JSONException e) {
			debug.logError("uploadToSoundCloud, JSONException ", e);
			return false;
		} 
		
		return true;
	}
	
	private long getDurationWithMp3Spi(File file) {

	    AudioFileFormat fileFormat;
		try {
			fileFormat = AudioSystem.getAudioFileFormat(file);
		    if (fileFormat instanceof TAudioFileFormat) {
		        Map<?, ?> properties = ((TAudioFileFormat) fileFormat).properties();
		        String key = "duration";
		        Long microseconds = (Long) properties.get(key);
		        long sec = (microseconds / 1000000);
		        return sec;
		    } else {
		    	return 0;
		    }
		} catch (UnsupportedAudioFileException e) {
			debug.logPostProc("getDurationWithMp3Spi UnsupportedAudioFile error ");
		} catch (IOException e) {
			debug.logPostProc("getDurationWithMp3Spi IOException error ");
		} 
		return 0;
	}
	
	private void cleanCrappyBroadcasts(String key, File file) {
		Integer minLength = Config.getInstance().getMinimumLength();		
		
		long secs = getDurationWithMp3Spi(file);
		
		if ( secs <= minLength ) {
			debug.logPostProc("cleanCrappyBroadcasts got " + secs + "<" + minLength + " secs, deleting...");
			
			FlipInterface fi = new FlipInterface();
			if ( fi.deleteAircast(key) ) {
				debug.logPostProc("cleanCrappyBroadcasts key " + key + " deleted OK");
			} else {
				debug.logPostProc("cleanCrappyBroadcasts key " + key + " NOT deleted?");
			}
		}
		

	}
}
