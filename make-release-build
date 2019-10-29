#!/bin/bash
# bash is required because we need bash's printf to guarantee a cross-platform
# timestamp format.

set -e
set -x

# make sure we're on a signed tag that matches the version name
describe=`git describe --tags --always`
versionName=`echo $describe | sed 's,-[0-9][0-9]*-g.*,,'`
if [ "$versionName" != "$describe" ]; then
    echo "WARNING: building $describe, which is not the latest release ($versionName)"
else
    # make a clearer warning above by putting this here
    set +x
    echo ""
    echo ""
    echo "Checking git tag signature for release build:"
    gpg --list-key 9F0FE587374BBE81 || gpg --recv-key 9F0FE587374BBE81
    gpg --list-key E9E28DEA00AA5556 || gpg --recv-key E9E28DEA00AA5556
    gpg --list-key A801183E69B37AA9 || gpg --recv-key A801183E69B37AA9
    git tag -v $versionName
    echo ""
    echo ""
    set -x
fi


if [ -z $ANDROID_HOME ]; then
    if [ -e ~/.android/bashrc-ant-build ]; then
        . ~/.android/bashrc-ant-build
    else
        echo "ANDROID_HOME must be set!"
        exit 1
    fi
fi

if [ -z $ANDROID_NDK_HOME ]; then
    if which ndk-build 2>&1 /dev/null; then
        ANDROID_NDK_HOME=`which ndk-build |  sed 's,/ndk-build,,'`
    else
        echo "ANDROID_NDK_HOME not set and 'ndk-build' not in PATH"
        exit 1
    fi
fi

projectroot=`pwd`
projectname=`sed -n 's,.*name="app_name">\(.*\)<.*,\1,p' app/src/main/res/values/strings.xml`

# standardize timezone to reduce build differences
export TZ=UTC

git reset --hard
git clean -fdx
git submodule foreach git reset --hard
git submodule foreach git clean -fdx
git submodule sync
git submodule foreach git submodule sync
git submodule update --init --recursive


if [ -e ~/.android/ant.properties ]; then
    cp ~/.android/ant.properties $projectroot/
else
    echo "skipping release ant.properties"
fi

cd $projectroot/orbotservice/src/main
$ANDROID_NDK_HOME/ndk-build
cd $projectroot

#clean, build, clean and build!
make -C external clean
APP_ABI=armeabi make -C external
#make -C external clean
#APP_ABI=x86 make -C external

./gradlew assembleRelease
ls -l $projectroot/app/build/outputs/apk/
apk=`ls -1 $projectroot/app/build/outputs/apk/*-release-unsigned.apk |head -1`

# echo the checksum to build logs
sha256sum $apk

if which gpg > /dev/null; then
    if [ -z "`gpg --list-secret-keys`" ]; then
        echo "No GPG secret keys found, not signing APK"
    else
        gpg --armor --detach-sign $apk
    fi
else
    echo "gpg not found, not signing APK"
fi
