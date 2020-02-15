#!/bin/sh

LOCAL=0
COPY=1
BUILDMODULES=1
DQUICK=""
FORCECOPY=0
IM=
NCG=
TP=../ThirdParty
if [ ! -d $TP ] ; then
  TP=../../ThirdParty
fi

while [ $# -gt 0 ] ; do
  if [ "$1" = "--local" ] ; then
    LOCAL=1
    COPY=0
    BUILDMODULES=0
    shift
  elif [ "$1" = "--dq" ] ; then
    DQUICK="--doublequick"
    shift
  elif [ "$1" = "--copy" ] ; then
    LOCAL=1
    shift
  elif [ "$1" = "--force-copy" ] ; then
    FORCECOPY=1
    shift
  elif [ "$1" = "--nomodules" ] ; then
    BUILDMODULES=0
    shift
  elif [ "$1" = "--ignore-main-changes" ] ; then
    IM="--ignore-main-changes"
    shift
  elif [ "$1" = "--modules" ] ; then
    BUILDMODULES=1
    shift
  elif [ "$1" = "--no-check-git" ] ; then
    NCG="$1"
    shift
  else
    break
  fi
done

if [ "$COPY" = 1 ] ; then
  cp ~/Ziniki/Over/Tools/utils/qbout/utils.jar qb/libs/ZinUtils.jar
fi
bash -e scripts/quickbuild.sh $IM $NCG $DQUICK "$@" qb/quickbuild.qb
cp QuickBuild/qbout/QuickBuild.jar scripts
cp Utils/qbout/Utils.jar scripts
