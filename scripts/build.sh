#!/bin/sh

scripts/quickbuild.sh "$@" qb/quickbuild.qb
cp QuickBuild/qbout/QuickBuild.jar scripts
cp Utils/qbout/Utils.jar scripts
