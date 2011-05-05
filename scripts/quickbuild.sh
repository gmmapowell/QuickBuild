#!/bin/bash

if [ $# -lt 1 ] ; then
  echo "Usage: quickbuild [options] <input file>" >&2
  exit 1
fi

SEP=':'
case `uname -s` in
   CYGWIN*)
     SEP=';'
     ;;
esac

ROOTDIR="`dirname $0`"

java -cp "$ROOTDIR/dp.jar$SEP$ROOTDIR/spritzerc.jar$SEP$ROOTDIR/QuickBuild.jar$SEP$ROOTDIR/Utils.jar" com.gmmapowell.quickbuild.app.QuickBuild "$@"
