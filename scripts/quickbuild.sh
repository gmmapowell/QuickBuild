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
for i in $ROOTDIR/../qb/libs/* `find $ROOTDIR/../qb/mvncache/ -name '*.jar'` ; do
  CLASSPATH="$CLASSPATH$SEP$i"
done

if [ "`hostname`" = "oldmajor" ] ; then
  $JAVA_HOME/bin/java -Duser.home=C:/cygwin/home/SYSTEM -cp "$CLASSPATH" com.gmmapowell.quickbuild.app.QuickBuild "$@"
else
  $JAVA_HOME/bin/java -cp "$CLASSPATH" com.gmmapowell.quickbuild.app.QuickBuild "$@"
fi
