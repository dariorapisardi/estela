#!/bin/bash

if [ -z "$ESTELA_HOME" ]; then 
  export ESTELA_HOME=`pwd`; 
fi

INSTANCE_TYPE=`/usr/bin/wget -q -O - http://169.254.169.254/latest/meta-data/instance-type`

XUGGLE_HOME=/usr/local/xuggler
export LD_LIBRARY_PATH=${XUGGLE_HOME}/lib:${LD_LIBRARY_PATH}
export PATH=${XUGGLE_HOME}/bin:${PATH}

case $INSTANCE_TYPE in
        "t1.micro" )
                echo "t1.micro instance detected"
                JAVA_MAX_RAM="480m"
                ;;
        "m1.small" )
                echo "mi1.small instance detected"
                JAVA_MAX_RAM="1280m"
                ;;
        "c1.medium" )
                echo "c1.medium instance detected"
                JAVA_MAX_RAM="1280m"
                ;;
        * )
                echo "no amazon instance "
                JAVA_MAX_RAM="32m"
                ;;
esac

P=":" # The default classpath separator
OS=`uname`
case "$OS" in
  CYGWIN*|MINGW*) # Windows Cygwin or Windows MinGW
  P=";" # Since these are actually Windows, let Java know
  ;;
  Darwin*)

  ;;
  *)
  # Do nothing
  ;;
esac

echo "Running on " $OS

# JAVA options
# You can set JAVA_OPTS to add additional options if you want
# Set up logging options
EXTRA_OPTS="-Dfile.encoding=UTF-8 -Dlog4j.debug=true -XX:+UseParallelGC -Xms${JAVA_MAX_RAM} -Xmx${JAVA_MAX_RAM} -XX:+AggressiveOpts -XX:+UseFastAccessorMethods"
export JAVA_OPTS="$EXTRA_OPTS $JAVA_OPTS"

if [ -z "$ESTELA_MAINCLASS" ]; then
  export ESTELA_MAINCLASS=com.flipzu.Estela
fi

for JAVA in "${JAVA_HOME}/bin/java" "${JAVA_HOME}/Home/bin/java" "/usr/bin/java" "/usr/local/bin/java"
do
  if [ -x "$JAVA" ]
  then
    break
  fi
done

if [ ! -x "$JAVA" ]
then
  echo "Unable to locate Java. Please set JAVA_HOME environment variable."
  exit
fi

ESTELA_CLASSPATH="${ESTELA_HOME}/bin"
for i in `ls -1 ${ESTELA_HOME}/lib`;do
	ESTELA_CLASSPATH="${ESTELA_CLASSPATH}${P}${ESTELA_HOME}/lib/${i}"
done
ESTELA_CLASSPATH="${ESTELA_CLASSPATH}${P}${CLASSPATH}"
export ESTELA_CLASSPATH

# start 
echo "Starting Estela"
exec "$JAVA" $JAVA_OPTS -cp "${ESTELA_CLASSPATH}" "$ESTELA_MAINCLASS"  
