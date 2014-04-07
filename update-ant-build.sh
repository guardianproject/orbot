#!/bin/sh

# make sure your Android SDK tools path is set in SDK_BASE
android update project -t android-19 --path . --name Orbot --subprojects
android update lib-project -t android-19 --path external/appcompat
android update lib-project -t android-19 --path external/superuser-commands/RootCommands-Library/
