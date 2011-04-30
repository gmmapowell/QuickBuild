#!/bin/bash -x

if [ $# -lt 1 ] ; then
  echo "Usage: bci [options] <jar file>" >&2
  exit 1
fi

ROOTDIR="`dirname $0`"

#files=`jar tf $1 | sed -n 's/\\.class//p' | sed 's%/%.%'g`
files=`jar tf $1 | grep '\\.class' | sed 's%^%/%'`
java -cp $ROOTDIR/../../Ziniki/Zamples/bin/classes:$ROOTDIR/utils.jar:$1 com.gmmapowell.bytecode.ByteCodeInspector $files
#  /org/ziniki/zamples/ChatZone.class
