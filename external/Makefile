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
ANDROID_NDK_HOME ?= /opt/android-ndk
NDK_PLATFORM_LEVEL ?= 16
NDK_TOOLCHAIN_VERSION=4.8
APP_ABI ?= armeabi
NDK_ABI ?= $(APP_ABI)
ifneq ($(filter arm%, $(APP_ABI)),)
  NDK_ABI := arm
endif
ifneq ($(filter arm64%, $(APP_ABI)),)
 NDK_ABI := arm64
endif
NDK_SYSROOT=$(ANDROID_NDK_HOME)/platforms/android-$(NDK_PLATFORM_LEVEL)/arch-$(NDK_ABI)
NDK_UNAME := $(shell uname -s | tr '[A-Z]' '[a-z]')
ifneq ($(filter mips%, $(NDK_ABI)),)
 HOST := $(NDK_ABI)el-linux-android
endif
ifneq ($(filter arm64, $(NDK_ABI)),)
 HOST := aarch64-linux-android
endif
ifneq ($(filter arm, $(NDK_ABI)),)
 HOST := arm-linux-androideabi
endif
ifneq ($(filter x86, $(NDK_ABI)),)
 HOST := i686-linux-android
endif
ifneq ($(filter x86_64, $(NDK_ABI)),)
 HOST := x86_64-linux-android
endif

NDK_TOOLCHAIN := $(HOST)-$(NDK_TOOLCHAIN_VERSION)
ifneq ($(filter x86%, $(NDK_ABI)),)
 NDK_TOOLCHAIN := $(NDK_ABI)-$(NDK_TOOLCHAIN_VERSION)
endif
NDK_TOOLCHAIN_BASE=$(ANDROID_NDK_HOME)/toolchains/$(NDK_TOOLCHAIN)/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)

# include Android's build flags
TARGET_ARCH_ABI = $(APP_ABI)
include $(ANDROID_NDK_HOME)/toolchains/$(NDK_TOOLCHAIN)/setup.mk

CC := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-gcc --sysroot=$(NDK_SYSROOT)
CXX := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-g++ --sysroot=$(NDK_SYSROOT)
CPP := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-cpp --sysroot=$(NDK_SYSROOT)
LD := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-ld
AR := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-ar
RANLIB := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-ranlib
STRIP := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-strip \
	--strip-unneeded -R .note -R .comment --strip-debug

# PIEFLAGS for SDK 16/Android L must be set to -fPIE -pie, but can override for earlier targets 
PIEFLAGS ?= -fPIE -pie
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
	polipo polipo-clean

all: assets

#------------------------------------------------------------------------------#
# openssl

# Disable rc4 cipher for 64-bit archs, to avoid this link error for tor:
# external/lib/libcrypto.a(e_rc4_hmac_md5.o):e_rc4_hmac_md5.c:function rc4_hmac_md5_cipher: error: undefined reference to 'rc4_md5_enc'
OPENSSL_CONF_FLAG=
ifneq ($(findstring 64, $(NDK_ABI)),)
 OPENSSL_CONF_FLAG+=no-rc4
endif

# OpenSSL's 'make depend' fails if _MIPS_SZLONG is not set:
ifneq ($(filter mips%, $(NDK_ABI)),)
 ifneq ($(findstring 64, $(NDK_ABI)),)
  OPENSSL_CONF_FLAG+=-D_MIPS_SZLONG=64
 else
  OPENSSL_CONF_FLAG+=-D_MIPS_SZLONG=32 -D__MIPSEL__
 endif
endif

lib/libcrypto.a:
	cd openssl && \
		./Configure android -DL_ENDIAN $(OPENSSL_CONF_FLAG) && \
		make CC="$(CC)" ANDROID_DEV=$(NDK_SYSROOT)/usr depend && \
		make CC="$(CC)" ANDROID_DEV=$(NDK_SYSROOT)/usr build_libs

lib/libssl.a:
	cp config.sub openssl
	cp config.guess openssl
	cd openssl && \
		./Configure android -DL_ENDIAN $(OPENSSL_CONF_FLAG) && \
		make CC="$(CC)" ANDROID_DEV=$(NDK_SYSROOT)/usr depend && \
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
	cp libevent-patch-1 libevent
	-cd libevent && \
	 	patch -N -p1 --reject-file=- < libevent-patch-1
	##sed -i 's@\(AC_OUTPUT(Makefile include/Makefile\) test/Makefile sample/Makefile)@\1)@' libevent/configure.in
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
	cp iptables-patch-4 iptables
	-cd iptables && \
	 	patch -N -p1 --reject-file=- < iptables-patch-1
	-cd iptables && \
	 	patch -N -p1 --reject-file=- < iptables-patch-2
	-cd iptables && \
	 	patch -N -p1 --reject-file=- < iptables-patch-3
	-cd iptables && \
	 	patch -N -p0 --reject-file=- < iptables-patch-4
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
	cp fix_android_0.2.6.4rc_build.patch tor
	cd tor && \
		git apply fix_android_0.2.6.4rc_build.patch
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
	-cd tor && \
		git reset HEAD --hard

#------------------------------------------------------------------------------#
# polipo

polipo-build-stamp: 
	CC="$(CC)" CFLAGS="$(PIEFLAGS)" LDFLAGS="$(PIEFLAGS)" $(MAKE) -C polipo
	touch polipo-build-stamp

polipo: polipo-build-stamp
	test -d bin || mkdir bin
	cp polipo/polipo bin

polipo-clean:
	$(MAKE) -C polipo clean
	-rm -f polipo/polipo
	-rm -f bin/polipo
	-rm -f polipo-build-stamp

#------------------------------------------------------------------------------#
# PLUTO Pluggable Transports

pluto/bin/android_arm/obfs4proxy:
#don't build for now until we sort out Go SDK 
#	cd pluto && \
#		./build.sh

pluto-build-stamp: pluto/bin/android_arm/obfs4proxy
	touch pluto-build-stamp

pluto: pluto-build-stamp
	test -d bin || mkdir bin
	cp pluto/bin/android_arm/obfs4proxy bin

pluto-clean:
#	-rm -rf pluto/bin

#------------------------------------------------------------------------------#
#create and clean assets: FYI - tor is stored as a ZIP file with an mp3 extension
#in order to stop Android OS (older devices) from trying to compress/decompress it
#this is related to a bug in compression of assets and resources > 1MB

assets: tor polipo iptables pluto
	install -d ../libs
	install -d ../assets/$(APP_ABI)
	-$(STRIP) bin/polipo
	-zip ../assets/$(APP_ABI)/polipo.mp3 bin/polipo
	-$(STRIP) bin/tor
	-zip ../assets/$(APP_ABI)/tor.mp3 bin/tor
	-$(STRIP) bin/xtables
	-zip ../assets/$(APP_ABI)/xtables.mp3 bin/xtables
	-$(STRIP) bin/obfs4proxy
	-zip ../assets/$(APP_ABI)/obfs4proxy.mp3 bin/obfs4proxy
	-$(STRIP) ../libs/$(APP_ABI)/pdnsd
	-zip ../assets/$(APP_ABI)/pdnsd.mp3 ../libs/$(APP_ABI)/pdnsd
	-rm ../libs/$(APP_ABI)/pdnsd

assets-clean:
	-rm ../assets/$(APP_ABI)/polipo.mp3
	-rm ../assets/$(APP_ABI)/tor.mp3
	-rm ../assets/$(APP_ABI)/xtables.mp3
	-rm ../assets/$(APP_ABI)/obfs4proxy.mp3
	-rm ../assets/$(APP_ABI)/pdnsd.mp3

#------------------------------------------------------------------------------#
# cleanup, cleanup, put the toys away

##clean: openssl-clean libevent-clean tor-clean polipo-clean assets-clean
clean: openssl-clean libevent-clean tor-clean polipo-clean iptables-clean pluto-clean

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
