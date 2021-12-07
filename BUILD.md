# Building Orbot 

Orbot includes, in the external directory, git submodules of:
- BadVPN
- JSocks

Please install the following prerequisites (instructions for each follows):
	
- Android Software Development Kit or SDK (for Java code): http://developer.android.com/sdk/index.html *(Android Studio)*
- Android Native Development Kit or NDK (for C/C++ code) http://developer.android.com/sdk/ndk/index.html *(This can be installed through Android Studio's SDK Manager)*

Be sure that you have all of the git submodules up-to-date:
```bash
git submodule update --init --recursive
```

You then need to run "ndk-build" and the following commands to compile and prepare Orbot's native code:

## UNIX based 

```bash
cd orbotservice/src/main
ndk-build #(located in Android/Sdk/ndk/VERSION/)
mv libs/armeabi-v7a/pdnsd libs/armeabi-v7a/libpdnsd.so
mv libs/arm64-v8a/pdnsd libs/arm64-v8a/libpdnsd.so
mv libs/x86/pdnsd libs/x86/libpdnsd.so
mv libs/x86_64/pdnsd libs/x86_64/libpdnsd.so
```

## Windows

```bat
cd orbotservice\src\main
ndk-build.cmd (located in Android\Sdk\ndk\VERSION\) 
ren libs\armeabi-v7a\pdnsd libpdnsd.so
ren libs\arm64-v8a\pdnsd libpdnsd.so
ren libs\x86\pdnsd libpdnsd.so
ren libs\x86_64\pdnsd libpdnsd.so
```


Now build the Android app using Android Studio/gradle

This will produce an unsigned Orbot package APK.

To produce a usable package, you'll need to sign the .apk. The basics on signing can be found on the Android developer site:

http://developer.android.com/guide/publishing/app-signing.html
