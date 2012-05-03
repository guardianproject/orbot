#!/bin/sh
#
# $Id: genclspec.sh,v 1.2 2002/04/27 04:49:11 morcego Exp $
#
# Written by and Copyright (C) 2001 the SourceForge
# Privoxy team. http://www.privoxy.org/
#
# This program is free software; you can redistribute it 
# and/or modify it under the terms of the GNU General
# Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at
# your option) any later version.
#
# This program is distributed in the hope that it will
# be useful, but WITHOUT ANY WARRANTY; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A
# PARTICULAR PURPOSE.  See the GNU General Public
# License for more details.
#
# The GNU General Public License should be included with
# this file.  If not, you can view it at
# http://www.gnu.org/copyleft/gpl.html
# or write to the Free Software Foundation, Inc., 59
# Temple Place - Suite 330, Boston, MA  02111-1307, USA.
#
# $Log: genclspec.sh,v $
# Revision 1.2  2002/04/27 04:49:11  morcego
# Adding license and copyright comments.
#
#

VERSION=`cat privoxy-rh.spec | sed -n -e 's/^Version:[ ]*//p'`
RELEASE=`cat privoxy-rh.spec | sed -n -e 's/^Release:[ ]*//p'`
CLTAG=${VERSION}-${RELEASE}cl

PACKAGER=`rpm --eval "%{packager}"`
if [ "${PACKAGER}" = "%{packager}" ]; then
	PACKAGER="genclspec script <developers@privoxy.org>"
fi

export LC_ALL=
export LANG=
DATETAG=`date "+%a %b %d %Y"`

if [ -r privoxy-cl.spec ]; then
	echo Old CL specfile found. Removing it.
fi

cat privoxy-rh.spec | sed -e 's/^\(Release:[ ]*[^ ]\+\)[ ]*$/\1cl/' \
			  -e "/^%changelog/a* ${DATETAG} ${PACKAGER}" \
			  -e "/^%changelog/a+ privoxy-${CLTAG}" \
			  -e "/^%changelog/a- Packaging for Conectiva Linux (automatic genarated specfile)" \
			  -e '/^%changelog/a \
' > privoxy-cl.spec

