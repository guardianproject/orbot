#!/bin/sh

rm -v *aar *jar
if [ ! -d OrbotIPtProxy ]; then
   git clone https://github.com/guardianproject/OrbotIPtProxy.git
fi
cd OrbotIPtProxy
git fetch
git rebase
bash build-orbot.sh
cd ..
