#!/bin/bash -x

if [ $# -lt 1 ] ; then
  echo "Usage: bci [options] <jar file>" >&2
  exit 1
fi

ROOTDIR="`dirname $0`"

classpath="$ROOTDIR/../../Ziniki/Zamples/bin/classes:$ROOTDIR/utils.jar"
files=""
while [ $# -gt 0 ] ; do
  case $1 in
    -path)
      shift
      classpath="$classpath:$1"
      shift
      ;;
    *.class)
      files="$files /$1"
      shift
       ;;
    *.jar)
      classpath="$classpath:$1"
      files="$files `jar tf $1 | grep '\\.class' | sed 's%^%/%'`"
      shift
      ;;
  esac
done

java -cp $classpath com.gmmapowell.bytecode.ByteCodeInspector $files
