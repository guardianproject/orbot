
This document explains how to properly build an Android package of Orbot from source.

Orbot includes, in the external directory, git repo submodules of:
	- Tor
	- OpenSSL (statically built and patched for Android)
	- LibEvent
	- JTorControl: The Tor Control Library for Java

The Orbot repo also includes the Privoxy source code of a recent stable release.

Please install the following prerequisites (instructions for each follows):
	ant: http://ant.apache.org/
	Android Native Dev Kit or NDK (for C/C++ code): http://developer.android.com/sdk/ndk/index.html
	Android Software Dev Kit or SDK (for Java code): http://developer.android.com/sdk/index.html

You will need to run the 'android' command in the SDK to install the necessary Android platform supports (ICS 4.x or android-15)

To begin building, from the Orbot root directory, you first need to build all external C/native dependencies:

	export NDK_BASE={PATH TO YOUR NDK INSTALL}
	make -C external

At this point, you'll have Tor and Privoxy binaries that can be run on an Android handset.
You can verify the ARM binary was properly built using the following command:

	file res/raw/tor
	file res/raw/privoxy
	
You should see something like:
src/or/tor: ELF 32-bit LSB executable, ARM, version 1 (SYSV), dynamically linked (uses shared libs), not stripped

This isn't enough though and we'll now sew up the binary into a small package
that will handle basic Tor controlling features.

	android update project --name Orbot --target android-15 --path .

#Now you need to split and copy the tor binary into res/raw. We split it into < 1M chunks
#because some Android devices don't like resources larger than 1M.
#split --bytes=1m $DROID_ROOT/external/tor/tor/src/or/tor res/raw/tor

Now build the Android app

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

