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
 * fzformat.c
 * JNI interface to get a frame codec name
 * Dario Rapisardi <dario@rapisardi.org> May 2011 
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <jni.h>

#include <libavformat/avformat.h>

JNIEXPORT jstring JNICALL Java_com_flipzu_FzFormat_getFormat
  (JNIEnv *env, jclass jobj, jbyteArray buf, jint buf_size) {

	if ( !buf )
		return NULL;

	if ( buf_size < 16 ) // arbitrary
		return NULL;

	char in_buf[buf_size];

	/* get audio data from java */
	(*env)->GetByteArrayRegion(env, buf, 0, buf_size, (jbyte*)in_buf);

	av_register_all();

	AVProbeData probe_data;
	probe_data.buf_size = buf_size;
	probe_data.buf = (unsigned char*) in_buf;
	AVInputFormat *fmt = av_probe_input_format(&probe_data, 1);

	jstring ret = NULL;
	if ( fmt )
		ret = (*env)->NewStringUTF(env, fmt->name);

	return ret;
}

