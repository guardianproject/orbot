
// 2011/04/15 this document is a bit out of date. We will be updating to use
// the standalone cross-compiler that is offered by the Android NDK soon

This document explains how to properly build an Android package of Orbot from
source. It covers building on Debian Lenny (5.0.3).

Please install the following prerequisites (instructions for each follows):
	ant: http://ant.apache.org/
	Android OS SDK: http://source.android.com/download
	droid-wrapper: http://github.com/tmurakam/droid-wrapper
	libevent source (1.4.12-stable from svn)
	Tor source (most recent git master branch)
	Privoxy source (http://sourceforge.net/projects/ijbswa/)

Install and prepare the Android OS SDK ( http://source.android.com/download )
on Debian Lenny:

	sudo apt-get install git-core gnupg sun-java5-jdk flex bison gperf \
		libsdl-dev libesd0-dev libwxgtk2.6-dev build-essential zip \
		curl libncurses5-dev zlib1g-dev valgrind libtool automake \
		ruby subversion
	update-java-alternatives -s java-1.5.0-sun

	curl http://android.git.kernel.org/repo >~/bin/repo
	chmod a+x ~/bin/repo

	mkdir ~/mydroid
	cd ~/mydroid

	repo init -u git://android.git.kernel.org/platform/manifest.git
	repo sync

	# Paste in key from http://source.android.com/download next...
	gpg --import

	cd ~/mydroid

	# This takes a long while...
	make

Install droid-wrapper:

	cd /tmp
	git clone git://github.com/tmurakam/droid-wrapper.git
	cd droid-wrapper
	sudo make install

zlib and OpenSSL are included with the Android OS SDK. You'll need to build
libevent, Privoxy and finally Tor. We'll create an externals directory for this code:

	mkdir -p ~/mydroid/external/{libevent,tor,privoxy}

We need to set to environment variables for droid-gcc:
	export DROID_ROOT=~/mydroid/
	export DROID_TARGET=generic

Fetch and build Privoxy:
	cd ~/mydroid/external/privoxy
	wget http://sourceforge.net/projects/ijbswa/files/Sources/3.0.12%20%28stable%29/privoxy-3.0.12-stable-src.tar.gz/download
	tar xzvf privoxy-3.0.12-stable-src.tar.gz
	cd privoxy-3.0.12-stable
	autoheader
	autoconf
	#need to disable setpgrp check in configure
	export ac_cv_func_setpgrp_void=yes
	#replace FOO with your actual username
	CC=droid-gcc LD=droid-ld CPPFLAGS="-I/home/FOO/mydroid/external/zlib/" ./configure --host=arm-none-linux-gnueabi
	#don't mind the "unrecognized option '-pthread'" error message that you'll see when you run make
	make
	
Fetch and build libevent:

	cd ~/mydroid/external/libevent
	svn co https://levent.svn.sourceforge.net/svnroot/levent/tags/release-1.4.12-stable/libevent/ .
	export LIBEVENTDIR=`pwd`
	./autogen.sh
	# Put the contents of http://pastebin.ca/1577207 in /tmp/libevent-patch
	patch < /tmp/libevent-patch
	CC=droid-gcc LD=droid-ld ./configure --host=arm-none-linux-gnueabi
	make

Copy over the libevent library:
	cp .libs/libevent.a ~/mydroid/out/target/product/generic/obj/lib

Fetch and build Tor:

	export OPENSSLDIR=`cd ~/mydroid/external/openssl/include/ && pwd`
	export ZLIBDIR=`cd ~/mydroid/external/zlib && pwd`

	cd ~/mydroid/external/tor
	git clone git://git.torproject.org/git/tor.git
	cd tor/
	./autogen.sh
	CC=droid-gcc LD=droid-ld ./configure --host=arm-none-linux-gnueabi \
	--with-libevent-dir=$LIBEVENTDIR --with-openssl-dir=$OPENSSLDIR \
	--with-zlib-dir=$ZLIBDIR --disable-asciidoc
	make

At this point, you'll have a Tor binary that can be run on an Android handset.
This isn't enough though and we'll now sew up the binary into a small package
that will handle basic Tor controlling features.

We need to build our Java SOCKS library:

	# If you're in Orbot's directory already...
	cd ../asocks/
	ant compile
	ant jar
	cp bin/jar/asocks.jar ../Orbot/libs

We need to get the TorControl library for Java:
(see also https://svn.torproject.org/svn/torctl/trunk/doc/howto.txt)

	git clone git://git.torproject.org/git/jtorctl
	cd jtorctl
	mkdir bin
	javac net/freehaven/tor/control/TorControlConnection.java -d bin
	cd bin
	jar cvf jtorctrl.jar *
	cp jtorctrl.jar {Orbot Home}/libs
	
Finally, we'll make a proper Android package with ant and the Android App SDK:

	export APP_SDK=~/Documents/projects/android/android-sdk-linux_x86-1.5_r3/tools
	cd ../Orbot/
	cp ~/mydroid/external/privoxy/privoxy-3.0.12-stable/privoxy assets/privoxy
	cp ~/mydroid/external/tor/tor/src/or/tor assets/tor
	$APP_SDK/android update project --name Orbot --target 3 --path .
	ant release

This will produce an unsigned Tor package in ./bin/Orbot-unsigned.apk!

To produce a usable package, you'll need to sign the .apk. The basics on
signing can be found on the Android developer site:

	http://developer.android.com/guide/publishing/app-signing.html

The three steps are quite simple. First, you'll generate a key. Secondly,
you'll sign the application. Thirdly, you'll verify the the apk.

Generating a signing key:

	keytool -genkey -v -keystore my-release-key.keystore \
		-alias orbots_key -keyalg RSA -validity 10000

Sign the apk:

	jarsigner -verbose -keystore my-release-key.keystore \
		bin/Orbot-unsigned.apk orbots_key

Verify the signature for the apk:

	jarsigner -verify bin/Orbot-unsigned.apk
	mv bin/Orbot-unsigned.apk bin/Orbot-signed-alpha.apk

You can also GPG sign the apk and generate an .asc:

	gpg -ab Orbot-signed-alpha.apk

Now you should have a fully signed and production ready alpha release of Orbot!
Give bin/Orbot-signed-alpha.apk an install and send us bug reports!

