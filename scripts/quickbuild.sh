#!/bin/bash

if [ $# -lt 1 ] ; then
  echo "Usage: quickbuild [options] <input file>" >&2
  exit 1
fi

java -cp dp.jar:quickbuild.jar:utils.jar com.gmmapowell.quickbuild.app.QuickBuild "$@"
