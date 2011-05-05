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

java -cp "$ROOTDIR/dp.jar$SEP$ROOTDIR/spritzerc.jar$SEP$ROOTDIR/QuickBuild.jar$SEP$ROOTDIR/Utils.jar" com.gmmapowell.quickbuild.app.QuickBuild "$@"
