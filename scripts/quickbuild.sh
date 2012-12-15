#!/bin/bash

if [ $# -lt 1 ] ; then
  echo "Usage: quickbuild [options] <input file>" >&2
  exit 1
fi

ROOTDIR="`dirname $0`"

SEP=':'
case `uname -s` in
   CYGWIN*)
     SEP=';'
     ROOTDIR=`echo $ROOTDIR | sed 's%/cygdrive/\(.\)%\1:%'`
     ;;
esac

CLASSPATH="$CLASSPATH$SEP$ROOTDIR/QuickBuild.jar$SEP$ROOTDIR/Utils.jar"
for i in $ROOTDIR/../qb/libs/* ; do
  CLASSPATH="$CLASSPATH$SEP$i"
done

java -cp "$CLASSPATH" com.gmmapowell.quickbuild.app.QuickBuild "$@"
