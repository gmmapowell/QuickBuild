#!/bin/sh

echo "This script really needs to process options like local, no-check-git and ignore-main-changes and pass them down"
(cd ../ZinUtils ; scripts/build.sh ; exit $?) || exit 1
cp ../ZinUtils/ZinUtils/qbout/ZinUtils.jar qb/libs

bash -ex scripts/quickbuild.sh "$@" qb/quickbuild.qb
cp QuickBuild/qbout/QuickBuild.jar scripts
cp Utils/qbout/Utils.jar scripts
