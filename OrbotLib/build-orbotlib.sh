#!/bin/sh

rm *aar *jar -v
if [ ! -d OrbotIPtProxy ]; then
   git clone https://github.com/bitmold/OrbotIPtProxy
fi
cd OrbotIPtProxy
git fetch
git rebase
bash build-orbot.sh
mv OrbotLib.aar .. -v
mv OrbotLib-sources.jar .. -v
cd ..
