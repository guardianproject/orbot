#!/bin/sh

# make sure your Android SDK tools path is set in SDK_BASE
android update project -t android-21 --path . --name Orbot --subprojects
android update lib-project -t android-21 --path external/appcompat
android update lib-project -t android-21 --path external/superuser-commands/RootCommands-Library/
android update lib-project -t android-21 --path external/jsocks/jsockslib
