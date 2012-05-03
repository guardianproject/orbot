#!/bin/sh

# begin by testing NDK_ROOT
if test -z $NDK_ROOT; then echo "NDK_ROOT is not exported, do so by something close to this: export NDK_ROOT=/path/to/android-ndk-r5"; exit; fi

#clean
rm -rf native
rm res/raw/privoxy
rm res/raw/tor
rm -rf libs

#create the native folder if it doesn't exist
mkdir native
mkdir native/lib
mkdir native/include
mkdir libs
cd native

#Build openssl using default ndk-build
echo "BUILDING OPENSSL STATIC..."
git clone git://github.com/guardianproject/android-external-openssl-ndk-static.git
cd android-external-openssl-ndk-static

ndk-build
cp obj/local/armeabi/*.a ../lib
cp -R include/openssl ../include
cd ../..

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


#Build libevent
echo "BUILDING LIBEVENT..."
mkdir native/libevent
cd native/libevent
svn co https://levent.svn.sourceforge.net/svnroot/levent/tags/release-1.4.13-stable/libevent/ .
./autogen.sh
./configure --host=arm-linux-eabi --build=$BUILD --prefix=$NDK_TOOLCHAIN
make clean
make
cp .libs/libevent.a ../lib
cp *.h ../include
cd ../..

#Build Tor
echo "BUILDING TOR..."
cd native
git clone git://git.torproject.org/git/tor.git
cd tor/
./autogen.sh
./configure --host=arm-linux-eabi --disable-asciidoc --prefix=$NDK_TOOLCHAIN --with-libevent-dir=$PWD/../ --enable-static-libevent --with-openssl-dir=$PWD/../ --enable-static-openssl
make clean
make
cd ../..

#Build JTorControl
echo "BUILDING JTORCTRL..."
cd libs
git clone git://git.torproject.org/git/jtorctl
cd jtorctl
mkdir bin
javac net/freehaven/tor/control/TorControlConnection.java -d bin
cd bin
jar cvf jtorctrl.jar *
cp jtorctrl.jar ../..
cd ../../..

#BUILD privoxy
echo "BUILDING PRIVOXY..."
mkdir native/privoxy
cd native/privoxy
rm download*
wget http://sourceforge.net/projects/ijbswa/files/Sources/3.0.12%20%28stable%29/privoxy-3.0.12-stable-src.tar.gz/download
tar xzvf download
cd privoxy-3.0.12-stable
autoheader
autoconf
#need to disable setpgrp check in configure
export ac_cv_func_setpgrp_void=yes
./configure --host=arm-linux-eabi --build=$BUILD --prefix=$NDK_TOOLCHAIN --disable-pthread
make clean
make
cd ../../..

#create assets folder and put the binaries in it
echo "MOVING BINARIES TO ANDROID RESOURCES..."
mkdir res/raw
cp native/privoxy/privoxy-3.0.12-stable/privoxy res/raw
cp native/tor/src/or/tor res/raw

echo "BUILD ANDROID APP"
android update project --name Orbot --target 9 --path .
ant debug

echo "BUILD COMPLETE"
