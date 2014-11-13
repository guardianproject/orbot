# Please install the following prerequisites (instructions for each follows):
# 	Android OS SDK: http://source.android.com/download
#
# Install and prepare the Android OS SDK ( http://source.android.com/download )
# on Debian or Ubuntu

### these modify the calling shell
# workaround for cross-compiling bug in autoconf
export ac_cv_func_malloc_0_nonnull=yes
export ac_cv_func_setpgrp_void=yes

EXTERNAL_ROOT := $(shell pwd)

# Android now has 64-bit and 32-bit versions of the NDK for GNU/Linux.  We
# assume that the build platform uses the appropriate version, otherwise the
# user building this will have to manually set NDK_PROCESSOR or NDK_TOOLCHAIN.
CPU := $(shell uname -m)
ifeq ($(CPU),x86_64)
 NDK_PROCESSOR=x86_64
else
 NDK_PROCESSOR=x86
endif

# Android NDK setup
NDK_BASE ?= /opt/android-ndk
NDK_PLATFORM_LEVEL ?= 9
NDK_ABI ?= arm
NDK_TOOLCHAIN_VERSION=4.8
NDK_SYSROOT=$(NDK_BASE)/platforms/android-$(NDK_PLATFORM_LEVEL)/arch-$(NDK_ABI)
APP_ABI ?= armeabi
NDK_UNAME := $(shell uname -s | tr '[A-Z]' '[a-z]')
ifeq ($(NDK_ABI),x86)
 HOST = i686-linux-android
 NDK_TOOLCHAIN = $(NDK_ABI)-$(NDK_TOOLCHAIN_VERSION)
else
 HOST = $(NDK_ABI)-linux-androideabi
 NDK_TOOLCHAIN = $(HOST)-$(NDK_TOOLCHAIN_VERSION)
endif
NDK_TOOLCHAIN_BASE=$(NDK_BASE)/toolchains/$(NDK_TOOLCHAIN)/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)

# include Android's build flags
TARGET_ARCH_ABI = $(APP_ABI)
include $(NDK_BASE)/toolchains/$(NDK_TOOLCHAIN)/setup.mk

CC := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-gcc --sysroot=$(NDK_SYSROOT)
CXX := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-g++ --sysroot=$(NDK_SYSROOT)
CPP := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-cpp --sysroot=$(NDK_SYSROOT)
LD := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-ld
AR := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-ar
RANLIB := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-ranlib
STRIP := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-strip \
	--strip-unneeded -R .note -R .comment

# PIEFLAGS for SDK 16/Android L must be set to -fPIE -pie
PIEFLAGS? = 
CFLAGS = -DANDROID $(TARGET_CFLAGS) $(PIEFLAGS)
LDFLAGS = -llog $(TARGET_LDFLAGS) $(PIEFLAGS)

# change 'release' to 'debug' for unoptimized debug builds
ifeq ($(APP_ABI),armeabi-v7a)
	CFLAGS += $(TARGET_arm_release_CFLAGS)
endif
ifeq ($(APP_ABI),armeabi)
	CFLAGS += $(TARGET_thumb_release_CFLAGS)
endif

.PHONY = clean showsetup \
	assets assets-clean \
	openssl-static openssl-static-clean \
	libevent libevent-clean \
	iptables iptables-clean \
	tor tor-clean \
	liballium liballium-clean \
	obfsclient obfsclient-clean \
	polipo polipo-clean

all: assets

#------------------------------------------------------------------------------#
# openssl

lib/libcrypto.a:
	cd openssl && \
		./Configure android -DL_ENDIAN && \
		make CC="$(CC)" ANDROID_DEV=$(NDK_SYSROOT)/usr build_libs

lib/libssl.a:
	cp config.sub openssl
	cp config.guess openssl
	cd openssl && \
		./Configure android -DL_ENDIAN && \
		make CC="$(CC)" ANDROID_DEV=$(NDK_SYSROOT)/usr build_libs

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
	sed -i 's@\(SUBDIRS = . include\) sample test@\1@' libevent/Makefile.am
	sed -i 's@\(AC_OUTPUT(Makefile include/Makefile\) test/Makefile sample/Makefile)@\1)@' libevent/configure.in
	cd libevent && ./autogen.sh
	cp config.sub libevent
	cp config.guess libevent
	cd libevent && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS) -I$(EXTERNAL_ROOT)/include" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--host=$(HOST) \
				--disable-shared

libevent-build-stamp: libevent/Makefile
	$(MAKE) -C libevent ./include/event2/event-config.h all-am
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
                CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS) -D_FORTIFY_SOURCE=2 -fwrapv -fno-strict-aliasing -fno-strict-overflow" LDFLAGS="$(LDFLAGS)" \
                        LIBS="-L$(EXTERNAL_ROOT)/lib" CFLAGS="-I$(EXTERNAL_ROOT)/include -I$(EXTERNAL_ROOT)/include/event2" \
                        ./configure \
                                --host=$(HOST) \
				--disable-asciidoc \
				--enable-static-libevent --with-libevent-dir=$(EXTERNAL_ROOT) \
				--enable-static-openssl --with-openssl-dir=$(EXTERNAL_ROOT) \
				--disable-linker-hardening --disable-gcc-hardening 

tor-build-stamp: tor/Makefile
	$(MAKE) -C tor all-am
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
# liballium

liballium/Makefile:
	cd liballium && mkdir m4
	cd liballium && ./autogen.sh
	cp config.sub liballium
	cp config.guess liballium
	cd liballium && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS) -I$(EXTERNAL_ROOT)/include" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--host=$(HOST) \
				--disable-shared

liballium-build-stamp: liballium/Makefile
	$(MAKE) -C liballium all-am
	touch liballium-build-stamp

liballium: liballium-build-stamp
	test -d lib || mkdir lib
	test -d include || mkdir include
	test -d include/allium || mkdir include/allium
	cp liballium/.libs/liballium-1.0.a lib
	cp liballium/include/* include/allium/

liballium-clean:
	-rm -f include/allium
	-rm -f lib/liballium-1.0.a
	-rm -f liballium-build-stamp
	-cd liballium && \
		git clean -fdx

#------------------------------------------------------------------------------#
# obfsclient


obfsclient/Makefile: 
	cd obfsclient && \
	 	./autogen.sh
	cp config.sub obfsclient
	cp config.guess obfsclient
	cd obfsclient && \
                CC="$(CC)" CPP="$(CPP)" CXX="$(CXX)" AR="$(AR)" RANLIB=$(RANLIB) \
 CFLAGS="$(CFLAGS) -I$(EXTERNAL_ROOT)/include -I$(EXTERNAL_ROOT)/include/event2" \
 CPPFLAGS="$(CFLAGS) -D_FORTIFY_SOURCE=2 -fwrapv -fno-strict-aliasing -fno-strict-overflow" \
 CXXFLAGS="$(CFLAGS) -D_FORTIFY_SOURCE=2 -fwrapv -fno-strict-aliasing -fno-strict-overflow -I$(NDK_BASE)/sources/cxx-stl/gnu-libstdc++/$(NDK_TOOLCHAIN_VERSION)/include -I$(NDK_BASE)/sources/cxx-stl/gnu-libstdc++/$(NDK_TOOLCHAIN_VERSION)/libs/$(APP_ABI)/include" \
LDFLAGS="$(LDFLAGS)" \
 libevent_CFLAGS="-I$(EXTERNAL_ROOT)/include" libevent_LIBS="-L$(EXTERNAL_ROOT)/lib" \
 liballium_CFLAGS="-I$(EXTERNAL_ROOT)/include" liballium_LIBS="-L$(EXTERNAL_ROOT)/lib" \
 LIBS="-L$(EXTERNAL_ROOT)/lib -levent -lallium-1.0 -L$(NDK_BASE)/sources/cxx-stl/gnu-libstdc++/$(NDK_TOOLCHAIN_VERSION)/libs/$(APP_ABI)/ -lgnustl_static" \
                        ./configure \
                                --host=$(HOST) \
                                --with-openssl=$(EXTERNAL_ROOT)

obfsclient-build-stamp: obfsclient/Makefile
	$(MAKE) -C obfsclient
	touch obfsclient-build-stamp

obfsclient: openssl libevent liballium obfsclient-build-stamp
	test -d bin || mkdir bin
	cp obfsclient/obfsclient bin

obfsclient-clean:
	-rm -f bin/obfsclient
	-rm -f obfsclient-build-stamp
	-cd obfsclient && \
		git clean -fdx

#------------------------------------------------------------------------------#
# polipo

polipo-build-stamp: 
	CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" $(MAKE) -C polipo
	touch polipo-build-stamp

polipo: polipo-build-stamp
	test -d bin || mkdir bin
	cp polipo/polipo bin

polipo-clean:
	-rm -f bin/polipo
	-rm -f polipo-build-stamp

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

assets: tor polipo jtorctl iptables obfsclient
	install bin/jtorctl.jar ../libs
	install -d ../res/raw
	-$(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-strip --strip-debug bin/polipo
	-zip ../res/raw/polipo.mp3 bin/polipo
	-$(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-strip --strip-debug bin/obfsclient
	-zip ../res/raw/obfsclient.mp3 bin/obfsclient
	-$(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-strip --strip-debug bin/tor
	-zip ../res/raw/tor.mp3 bin/tor
	-$(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-strip --strip-debug bin/xtables
	-zip ../res/raw/xtables.mp3 bin/xtables

assets-clean:
	-rm ../res/raw/polipo.mp3
	-rm ../res/raw/obfsclient.mp3
	-rm ../res/raw/tor.mp3
	-rm ../res/raw/xtables.mp3
	-rm ../libs/jtorctl.jar

#------------------------------------------------------------------------------#
# cleanup, cleanup, put the toys away

clean: openssl-clean libevent-clean tor-clean polipo-clean jtorctl-clean liballium-clean obfsclient-clean assets-clean

#------------------------------------------------------------------------------#
# debugging stuff

showsetup:
	@echo "NDK_TOOLCHAIN_VERSION: $(NDK_TOOLCHAIN_VERSION)"
	@echo "NDK_TOOLCHAIN: $(NDK_TOOLCHAIN)"
	@echo "NDK_PLATFORM_LEVEL: $(NDK_PLATFORM_LEVEL)"
	@echo "NDK_SYSROOT: $(NDK_SYSROOT)"
	@echo "APP_ABI: $(APP_ABI)"
	@echo "HOST: $(HOST)"
	@echo "CC: $(CC)"
	@echo "LD: $(LD)"
	@echo "CFLAGS: $(CFLAGS)"
	@echo "LDFLAGS: $(LDFLAGS)"
