# Building Orbot 

Please install the following prerequisites (instructions for each follows):
	
- Android Software Development Kit or SDK (for Java code): http://developer.android.com/sdk/index.html *(Android Studio)*
- Android Native Development Kit or NDK (for C/C++ code) http://developer.android.com/sdk/ndk/index.html *(This can be installed through Android Studio's SDK Manager)*

Now build the Android app using Android Studio/gradle

This will produce an unsigned Orbot package APK.

## IptProxy.aar

This project includes a pre-built version of the IPtProxy.aar file using the source code from: https://github.com/guardianproject/Orbot-IPtProxy

Given that code requires a complex process and toolchain of GoMobile, it was decided to prebuild this outside of the Android toolchain process required to work on the Orbot app itself.

We are working to publich Orbot-IPtProxy as a proper gradle dependency and/or merge the functionality into the core public IPtProxy releases.

## Signing

To produce a usable package, you'll need to sign the .apk. The basics on signing can be found on the Android developer site:

http://developer.android.com/guide/publishing/app-signing.html
