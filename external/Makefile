# Please install the following prerequisites (instructions for each follows):
# 	Android OS SDK: http://source.android.com/download
#
# Install and prepare the Android OS SDK ( http://source.android.com/download )
# on Debian or Ubuntu

### these modify the calling shell
# point pkg-config to the .pc files generated from these builds
export PKG_CONFIG_PATH=$(LOCAL)/lib/pkgconfig
# workaround for cross-compiling bug in autoconf
export ac_cv_func_malloc_0_nonnull=yes
export ac_cv_func_setpgrp_void=yes

CWD = $(shell pwd)
PROJECT_ROOT = $(CWD)/..
EXTERNAL_ROOT = $(CWD)

# Android NDK setup:
NDK_BASE ?= /usr/local/android-ndk
NDK_PLATFORM_LEVEL ?= 9
NDK_TOOLCHAIN_VERSION=4.6
NDK_SYSROOT=$(NDK_BASE)/platforms/android-$(NDK_PLATFORM_LEVEL)/arch-arm
NDK_UNAME:=`uname -s | tr '[A-Z]' '[a-z]'`
NDK_PROCESSOR:=`uname -m`

NDK_TOOLCHAIN=$(NDK_BASE)/toolchains/arm-linux-androideabi-4.6/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)

# to use the real HOST tag, you need the latest libtool files:
# http://stackoverflow.com/questions/4594736/configure-does-not-recognize-androideabi
#HOST := arm-none-linux-gnueabi
HOST := arm-linux-androideabi

# install root for built files
DESTDIR = $(EXTERNAL_ROOT)
# TODO try adding the Android-style /data/app.name here
prefix = 
LOCAL := $(DESTDIR)$(prefix)

export PATH := ${PATH}:$(NDK_TOOLCHAIN)/bin:$(LOCAL)/bin

CC := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-gcc --sysroot=$(NDK_SYSROOT)
CXX := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-g++
CPP := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-cpp
LD := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ld
AR := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ar
RANLIB := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ranlib
STRIP := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-strip \
	--strip-unneeded -R .note -R .comment

CFLAGS = -DANDROID -I$(LOCAL) -I$(LOCAL)/include -I$(NDK_SYSROOT)/usr/include
LDFLAGS = -L$(LOCAL) -L$(LOCAL)/lib -L$(NDK_SYSROOT)/usr/lib

# build as small as possible, mostly useful for static binaries
CFLAGS += -fdata-sections -ffunction-sections -Os
LDFLAGS += -Wl,--gc-sections

all: assets

#------------------------------------------------------------------------------#
# openssl

lib/libcrypto.a:
	cd openssl && \
		CC="$(CC)" \
		./Configure android-armv7 && \
		ANDROID_DEV=$(NDK_BASE)/platforms/android-8/arch-arm/usr make build_libs

lib/libssl.a:
	cp config.sub openssl
	cp config.guess openssl
	cd openssl && \
		CC="$(CC)"\
		./Configure android-armv7 && \
		ANDROID_DEV=$(NDK_BASE)/platforms/android-8/arch-arm/usr make build_libs

openssl-build-stamp: lib/libcrypto.a lib/libssl.a
	touch openssl-build-stamp

openssl: openssl-build-stamp
	test -d lib || mkdir lib
	test -d include || mkdir include
	test -d include/openssl || mkdir include/openssl
	cp openssl/libcrypto.a lib/libcrypto.a
	cp openssl/libssl.a lib/libssl.a
	cp openssl/include/openssl/* include/openssl

openssl-clean:
	-rm openssl-build-stamp
	-rm lib/libcrypto.a
	-rm lib/libssl.a
	-cd openssl && \
		git clean -fdx

#------------------------------------------------------------------------------#
# libevent

libevent/Makefile:
	cd libevent && ./autogen.sh
	cp config.sub libevent
	cp config.guess libevent
	cd libevent && \
 		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS) -I$(EXTERNAL_ROOT)/include" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--host=$(HOST) \
				--disable-shared

libevent-build-stamp: libevent/Makefile
	$(MAKE) -C libevent
	touch libevent-build-stamp

libevent: openssl libevent-build-stamp
	test -d lib || mkdir lib
	test -d include || mkdir include
	cp libevent/.libs/libevent.a lib
	cp -R libevent/include/event2 include

libevent-clean:
	-rm -f include/event2
	-rm -f lib/libevent.a
	-rm -f libevent-build-stamp
	-cd libevent && \
		git clean -fdx

#------------------------------------------------------------------------------#
# iptables

iptables/Makefile:
	cp iptables-patch-1 iptables
	cp iptables-patch-2 iptables
	cp iptables-patch-3 iptables
	-cd iptables && \
	 	patch -N -p1 --reject-file=- < iptables-patch-1
	-cd iptables && \
	 	patch -N -p1 --reject-file=- < iptables-patch-2
	-cd iptables && \
	 	patch -N -p1 --reject-file=- < iptables-patch-3
	cd iptables && ./autogen.sh
	cp config.sub iptables
	cp config.guess iptables
	cd iptables && \
 		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS) -I$(EXTERNAL_ROOT)/include" LDFLAGS="$(LDFLAGS)" \
			./configure --host=$(HOST) --disable-shared --enable-static

iptables-build-stamp: iptables/Makefile
	$(MAKE) -C iptables 
	touch iptables-build-stamp
	cp iptables/iptables/xtables-multi bin/xtables

iptables: iptables-build-stamp

iptables-clean:
	-rm -f iptables-build-stamp
	-rm bin/xtables
	-cd iptables && \
		git clean -fdx

#------------------------------------------------------------------------------#
# tor

tor/configure: 
	cd tor && \
	 ./autogen.sh

tor/Makefile: tor/configure
	cp config.sub tor
	cp config.guess tor
	cd tor && \
                CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS) -D_FORTIFY_SOURCE=2 -fPIE -fwrapv -fno-strict-aliasing -fno-strict-overflow" LDFLAGS="$(LDFLAGS)" \
                        LIBS="-L$(EXTERNAL_ROOT)/lib" CFLAGS="-I$(EXTERNAL_ROOT)/include -I$(EXTERNAL_ROOT)/include/event2" \
                        ./configure \
                                --host=$(HOST) \
                                --prefix=$(NDK_TOOLCHAIN) \
				--disable-asciidoc \
				--enable-static-libevent --with-libevent-dir=$(EXTERNAL_ROOT) \
				--enable-static-openssl --with-openssl-dir=$(EXTERNAL_ROOT) \
				--disable-linker-hardening --disable-gcc-hardening 

tor-build-stamp: tor/Makefile
	$(MAKE) -C tor 
	touch tor-build-stamp

tor: libevent openssl tor-build-stamp
	test -d bin || mkdir bin
	cp tor/src/or/tor bin

tor-clean:
	-rm -f bin/tor
	-rm -f tor-build-stamp
	-cd tor && \
		git clean -fdx


#------------------------------------------------------------------------------#
# obfsproxy


obfsproxy/Makefile: 
	cp obfsproxy_android_no_hardening.patch obfsproxy
	-cd obfsproxy && \
	 	patch -N -p1 --reject-file=- < obfsproxy_android_no_hardening.patch
	cd obfsproxy && \
	 	./autogen.sh
	cp config.sub obfsproxy
	cp config.guess obfsproxy
	cd obfsproxy && \
                CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS) -D_FORTIFY_SOURCE=2 -fPIE -fwrapv -fno-strict-aliasing -fno-strict-overflow" LDFLAGS="$(LDFLAGS)" \
			libevent_LIBS="-L$(EXTERNAL_ROOT)/lib -levent" libevent_CFLAGS="-I$(EXTERNAL_ROOT)/include" \
			libcrypto_LIBS="-L$(EXTERNAL_ROOT)/lib -lcrypto" libcrypto_CFLAGS="-I$(EXTERNAL_ROOT)/include" \
                        ./configure \
                                --host=$(HOST) \
                                --prefix=$(NDK_TOOLCHAIN) \

obfsproxy-build-stamp: obfsproxy/Makefile
	$(MAKE) -C obfsproxy
	touch obfsproxy-build-stamp

obfsproxy: openssl libevent obfsproxy-build-stamp
	test -d bin || mkdir bin
	cp obfsproxy/obfsproxy bin

obfsproxy-clean:
	-rm -f bin/obfsproxy
	-rm -f obfsproxy-build-stamp
	-cd obfsproxy && \
		git clean -fdx

#------------------------------------------------------------------------------#
# privoxy


privoxy/config.log:
	tar xzvf privoxy.tar.gz
	mv privoxy*stable privoxy
	cp privoxy.configure.in privoxy/configure.in
	cd privoxy && \
	  autoheader
	cd privoxy && \
	  autoconf
	cp config.sub privoxy
	cp config.guess privoxy	
	cd privoxy && \
                CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
                        ./configure \
                                --host=arm-linux-eabi --enable-pthread
## --disable-pthread 

privoxy-build-stamp: privoxy/config.log
	$(MAKE) -C privoxy
	touch privoxy-build-stamp

privoxy: privoxy-build-stamp
	test -d bin || mkdir bin
	cp privoxy/privoxy bin

privoxy-clean:
	-rm -f bin/privoxy
	-rm -f privoxy-build-stamp
	-rm -rf privoxy

#------------------------------------------------------------------------------#
# JTorControl library

jtorctl/bin/jtorctl.jar:
	test -d jtorctl/bin || mkdir jtorctl/bin
	cd jtorctl && \
		javac -source 1.6 -target 1.6 net/freehaven/tor/control/TorControlConnection.java -d bin
	cd jtorctl/bin && \
		jar cvf jtorctl.jar *

jtorctl-build-stamp: jtorctl/bin/jtorctl.jar
	touch jtorctl-build-stamp

jtorctl: jtorctl-build-stamp
	test -d bin || mkdir bin
	cp jtorctl/bin/jtorctl.jar bin
	
jtorctl-clean:
	-rm -rf jtorctl/bin
	-rm jtorctl-build-stamp

#------------------------------------------------------------------------------#
#create and clean assets: FYI - tor is stored as a ZIP file with an mp3 extension
#in order to stop Android OS (older devices) from trying to compress/decompress it
#this is related to a bug in compression of assets and resources > 1MB

assets: tor privoxy jtorctl obfsproxy iptables
	install -d ../libs/armeabi
	install -d ../libs
	install bin/privoxy ../libs/armeabi/libprivoxy.so
	install bin/obfsproxy ../libs/armeabi/libobfsproxy.so
	install bin/jtorctl.jar ../libs
	install bin/tor ../libs/armeabi/libtor.so
	install bin/xtables ../libs/armeabi/libxtables.so

assets-clean:
	-rm ../libs/armeabi/libxtables.so
	-rm ../libs/armeabi/libtor.so
	-rm ../libs/armeabi/libprivoxy.so
	-rm ../libs/armeabi/libobfsproxy.so
	-rm ../libs/jtorctl.jar

#------------------------------------------------------------------------------#
# cleanup, cleanup, put the toys away

clean: openssl-clean libevent-clean tor-clean privoxy-clean jtorctl-clean assets-clean
