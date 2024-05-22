#!/bin/bash

# Fix TypographyEllipsis programmatically

sed -i 's/\.\.\./…/g' app/src/main/res/values*/*.xml
if git diff | grep -Eo '^\+.*…'; then
    echo Fix TypographyEllipsis
    exit 1
fi
