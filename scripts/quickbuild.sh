#!/bin/bash

if [ $# -lt 1 ] ; then
  echo "Usage: quickbuild [options] <input file>" >&2
  exit 1
fi

ROOTDIR="`dirname $0`"

java -cp $ROOTDIR/dp.jar:$ROOTDIR/spritzerc.jar:$ROOTDIR/quickbuild.jar:$ROOTDIR/utils.jar com.gmmapowell.quickbuild.app.QuickBuild "$@"
