#!/usr/bin/env python3

import glob
import os

for f in glob.glob('fastlane/metadata/android/*/*.txt') +  glob.glob('fastlane/metadata/android/*/*/*.txt'):
    if os.path.getsize(f) == 0:
        os.remove(f)
        continue

    with open(f) as fp:
        data = fp.read()
    with open(f, 'w') as fp:
        fp.write(data.rstrip())
        fp.write('\n')
