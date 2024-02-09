#!/usr/bin/env sh

set -e
set -x

for f in `find $(dirname $0)/../*/src/ -type f -name \*.png`; do
    echo $f | grep -Eo '\.9\.png$' && continue  # do not optimized 9-patch, it breaks them
    tmpfile=$(mktemp)
    aapt singleCrunch -v -i $f -o $tmpfile
    exiftool -all= $tmpfile
    mv $tmpfile $f
done

for f in `find $(dirname $0)/../fastlane/metadata/android/ -type f -name \*.png`; do
    exiftool -all= $f
    tmpfile=$(mktemp)
    (zopflipng --filters=01234mepb --lossy_8bit --lossy_transparent -y $f $tmpfile && mv $tmpfile $f) &
done
