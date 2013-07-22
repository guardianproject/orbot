#!/bin/sh

# make sure your Android SDK tools path is set in SDK_BASE
android update project --path . --name Orbot --subprojects
android update project --path external/ActionBarSherlock/actionbarsherlock -t android-17

