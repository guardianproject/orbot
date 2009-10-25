This document explains how to properly build an Android package of Orbot from
source.

Please install the following prerequisites:
	Android OS SDK
	droid-wrapper: http://github.com/tmurakam/droid-wrapper
	libevent source
	Tor source (most recent git master branch)

Install and prepare the Android OS SDK ( http://source.android.com/download )
on Debian Lenny:

	sudo apt-get install git-core gnupg sun-java5-jdk flex bison gperf \
		libsdl-dev libesd0-dev libwxgtk2.6-dev build-essential zip \
		curl libncurses5-dev zlib1g-dev valgrind
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

XXX TODO: Explain build process for making a static Tor with our libevent, etc.
zlib and OpenSSL are included with the Android OS SDK.

Build libevent:

Build Tor:

XXX TODO: Explain build process for making a .apk file for install.
