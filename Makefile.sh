#!/bin/sh

export ORBOT_BASE=$PWD

# begin by testing NDK_ROOT
if test -z $NDK_ROOT; then echo "NDK_ROOT is not exported, do so by something close to this: export NDK_ROOT=/path/to/android-ndk-r5"; exit; fi

#clean
cd $ORBOT_BASE
rm -rf native
rm res/raw/privoxy
rm res/raw/tor
rm -rf libs

#create the native folder if it doesn't exist
mkdir native
mkdir native/lib
mkdir native/include
mkdir libs

#Build openssl using default ndk-build
echo "BUILDING OPENSSL STATIC..."
cd external/openssl-static
ndk-build
cp obj/local/armeabi/*.a $ORBOT_BASE/native/lib
cp -R include/openssl $ORBOT_BASE/native/include
cd $ORBOT_BASE

echo "SETTING UP NDK CROSS COMPILER..."
# export needed variables
export NDK_TOOLCHAIN=$NDK_ROOT/my-android-toolchain

# remove the old toolchain
rm -rf $NDK_TOOLCHAIN/*

# create the toolchain
$NDK_ROOT/build/tools/make-standalone-toolchain.sh --platform=android-9 --install-dir=$NDK_TOOLCHAIN

# export needed variables for crosscompile
export PATH="$NDK_TOOLCHAIN/bin/:$PATH"

export HOST=arm-linux-androideabi

export CC=$HOST-gcc
export CXX=$HOST-g++
export AR=$HOST-ar
export LD=$HOST-ld
export AS=$HOST-as
export NM=$HOST-nm
export STRIP=$HOST-strip
export RANLIB=$HOST-ranlib
export OBJDUMP=$HOST-objdump

export CPPFLAGS="--sysroot=$NDK_TOOLCHAIN/sysroot -I$NDK_TOOLCHAIN/sysroot/usr/include -I$NDK_TOOLCHAIN/include"
export LDFLAGS="-L$NDK_TOOLCHAIN/sysroot/usr/lib -L$NDK_TOOLCHAIN/lib"

echo $BUILD

#Build libevent
echo "BUILDING LIBEVENT..."
cd external/libevent
./autogen.sh
./configure --host=arm-linux-eabi --prefix=$NDK_TOOLCHAIN
make
cp .libs/libevent.a $ORBOT_BASE/native/lib
cp *.h $ORBOT_BASE/native/include
cd $ORBOT_BASE

#Build Tor
echo "BUILDING TOR..."
cd external/tor
./autogen.sh
./configure --host=arm-linux-eabi --disable-asciidoc --prefix=$NDK_TOOLCHAIN --with-libevent-dir=$ORBOT_BASE/native --enable-static-libevent --with-openssl-dir=$ORBOT_BASE/native --enable-static-openssl
make
cd $ORBOT_BASE

#Build JTorControl
echo "BUILDING JTORCTRL..."
cd external/jtorctl
mkdir bin
javac net/freehaven/tor/control/TorControlConnection.java -d bin
cd bin
jar cvf jtorctrl.jar *
cp jtorctrl.jar $ORBOT_BASE/libs
cd $ORBOT_BASE

#BUILD privoxy
echo "BUILDING PRIVOXY..."
cd $ORBOT_BASE/external/privoxy
autoheader
autoconf
#need to disable setpgrp check in configure
export ac_cv_func_setpgrp_void=yes
./configure --host=arm-linux-eabi --prefix=$NDK_TOOLCHAIN --disable-pthread
make

cd $ORBOT_BASE

#create assets folder and put the binaries in it
echo "MOVING BINARIES TO ANDROID RESOURCES..."
mkdir res/raw
cp external/privoxy/privoxy res/raw
cp external/tor/src/or/tor res/raw

echo "Setting Android build configuration"
android update project --name Orbot --target 9 --path .

echo "READY TO BUILD ANDROID APP: run 'ant debug'"
echo "BUILD COMPLETE"
