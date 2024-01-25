#!/bin/sh

rm -v *aar *jar
if [ ! -d OrbotIPtProxy ]; then
   git clone https://github.com/bitmold/OrbotIPtProxy
fi
cd OrbotIPtProxy
git fetch
git rebase
bash build-orbot.sh
cd ..
