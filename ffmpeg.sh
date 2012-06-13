#!/bin/bash

FFMPEG="/usr/local/xuggler/bin/ffmpeg"
TEMP=`getopt -o yo:i:a:f: -n 'ffmpeg.sh' -- "$@"`

if [ $? != 0 ] ; then echo "Terminating..." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

while true ; do
	case "$1" in
		-o) 
			OUTPUT=$2
			shift 2 ;;
		-i)
			INPUT=$2
			shift 2 ;;
		-a)
			BITRATE=$2
			shift 2 ;;
		-f) 
			CODEC=$2
			shift 2 ;;
		-y) 
			OVERWRITE="-y"
			shift ;;
		--) shift ; break ;;
		*) echo "Internal error!" ; exit 1 ;;
	esac
done

if [ ! -z $INPUT ];then
	INPUT_ARG="-i $INPUT"
fi

if [ ! -z $OUTPUT ];then
	OUTPUT_ARG="$OUTPUT"
fi

if [ ! -z $BITRATE ];then
	BITRATE_ARG="-ab $BITRATE"
fi

if [ ! -z $CODEC ];then
	CODEC_ARG="-f $CODEC"
fi

$FFMPEG $INPUT_ARG $BITRATE_ARG $CODEC_ARG $OVERWRITE $OUTPUT_ARG 2>/dev/null
