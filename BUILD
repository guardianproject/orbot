
This document explains how to properly build an Android package of Orbot from
source.

Orbot includes, in the external directory, git repo submodules of:
	- Tor
	- OpenSSL (statically built and patched for Android)
	- LibEvent
	- JTorControl: The Tor Control Library for Java

The Orbot repo also includes the Polipo source code of a recent stable release.

Please install the following prerequisites (instructions for each follows):
	ant: http://ant.apache.org/
	Android Native Dev Kit or NDK (for C/C++ code):
        http://developer.android.com/sdk/ndk/index.html
	Android Software Dev Kit or SDK (for Java code):
        http://developer.android.com/sdk/index.html

You will need to run the 'android' command in the SDK to install the necessary
Android platform supports (ICS 4.x or android-15)

Be sure that you have all of the git submodules up-to-date:

	git submodule update --init --recursive

To begin building, from the Orbot root directory, you first need to build all
external C/native dependencies:

	export ANDROID_NDK_HOME={PATH TO YOUR NDK INSTALL}
	make -C external

At this point, you'll have Tor and Polipo binaries that can be run on an
Android handset.  You can verify the ARM binary was properly built using the
following command:

	file external/bin/tor external/bin/polipo
	
You should see something like:
    external/bin/tor: ELF 32-bit LSB executable, ARM, version 1 (SYSV),
        dynamically linked (uses shared libs), not stripped
    external/bin/polipo: ELF 32-bit LSB executable, ARM, version 1 (SYSV),
        dynamically linked (uses shared libs), not stripped

This isn't enough though and we'll now sew up the binary into a small package
that will handle basic Tor controlling features.

	android update project --name Orbot --target android-15 --path .

Now build the Android app

(gradle / android studio instructions here)

This will produce an unsigned Tor package APK.

To produce a usable package, you'll need to sign the .apk. The basics on
signing can be found on the Android developer site:

	http://developer.android.com/guide/publishing/app-signing.html


