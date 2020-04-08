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
 * format.c
 * Dario Rapisardi <dario@rapisrdi.org>
 */

#include <stdio.h>
#include <string.h>

#include <libavformat/avformat.h>

#define INBUF_SIZE 4096

char* get_format( unsigned char* buf, int size ) {

	if (!buf)
		return NULL;

	AVProbeData probe_data;
	probe_data.buf_size = size;
	probe_data.buf = buf;

	AVInputFormat *aif = av_probe_input_format(&probe_data, 1);
        if ( !aif )
		return NULL;

	return (char*)aif->name;
}

int main(int argc, char **argv)
{
	int i, nc;

	nc = 0;
	unsigned char *buf = malloc(INBUF_SIZE);

	while ( i != EOF && nc < INBUF_SIZE ) {
		nc++;
		i = getchar();
		if ( i != EOF ) {
			buf[nc] = i;
		}
	}

	char *fmt = get_format(buf, nc);
	if ( fmt != NULL )
		if ( strcmp(fmt,"vc1test") == 0 ) {
			printf("mp3");
		} else {
			printf("%s", fmt);
		}
	else
		printf("none");

	free(buf);

	return 0;
}
