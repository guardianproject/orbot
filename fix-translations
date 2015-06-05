#!/bin/bash -x

# Fix TypographyEllipsis programmatically
sed -i 's/\.\.\./…/g' res/values*/*.xml

# Replace "--" with an "em dash" character
sed -i 's,\(\>[^\<]*\)--\([^\>]\),\1—\2,g' res/values*/*.xml

# make sure apostrophes in strings are escaped
sed -i "s,\(>[^<]*[^\\]\)',\1\\\\',gp" res/values*/*.xml
