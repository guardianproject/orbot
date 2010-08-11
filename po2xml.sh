#!/bin/bash
#
# Author: Runa A. Sandvik, <runa.sandvik@gmail.com>
# For The Tor Project, Inc.
#
# This is Free Software (GPLv3)
# http://www.gnu.org/licenses/gpl-3.0.txt
#
# This script will convert translated po files back to xml. Before
# running the script, checkout the translation directory from
# https://svn.torproject.org.
#	

### Start config ###

# Location of the translated files, i.e. the path to the orbot
# directory in the translation module. Do not add the trailing slash. 
translated=""

# Location of the orbot directory, i.e. the original English xml file.
# In svn, this should be svn/projects/android/trunk/Orbot/res. Do not add the
# trailing slash.
xml=""

### End config ###

# Find po files to convert.
po=`find $translated -type f -name \*.po`

# For every po found, create and/or update the translated manpage.
for file in $po ; do

	# Get the basename of the file we are dealing with.
	pofile=`basename $file`

	# Strip the file for its original extension and add .xml.
	xmlfile="${pofile%.*}.xml"

	# Figure out which language we are dealing with.
	lang=`dirname $file | sed "s#$translated/##"`

	# The translated document is written if 80% or more of the po
	# file has been translated. Also, po4a-translate will only write
	# the translated document if 80% or more has been translated.
	# However, it will delete the translated txt if less than 80%
	# has been translated. To avoid having our current, translated
	# xml files deleted, convert the po to a temp xml first. If this
	# file was actually written, rename it to xml.

	# Convert translated po to xml.
	function convert {
		po4a-translate -f xml -m "$xml/values/$xmlfile" -p "$file"  -l "$xml/values-$lang/tmp-$xmlfile" --master-charset utf-8 -L utf-8

		# Check to see if the file was written. If yes, rename it.
		if [ -e "$xml/values-$lang/tmp-$xmlfile" ]
		then
			mv "$xml/values-$lang/tmp-$xmlfile" "$xml/values-$lang/$xmlfile"
			
			# We need to escape apostrophe's
			sed -i "s,',\\\',g" "$xml/values-$lang/$xmlfile"
		fi
	}
	
	# If the current directory is zh_CN use zh, else convert everything.
	if [ $lang = "zh_CN" ]
	then
		lang="zh"
		convert
	else
		convert
	fi
done
