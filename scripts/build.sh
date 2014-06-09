#!/bin/sh

(cd ../ZinUtils ; scripts/build.sh ; exit $?) || exit 1
cp ../ZinUtils/ZinUtils/qbout/ZinUtils.jar qb/libs

scripts/quickbuild.sh "$@" qb/quickbuild.qb
cp QuickBuild/qbout/QuickBuild.jar scripts
cp Utils/qbout/Utils.jar scripts
