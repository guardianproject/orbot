# $Id: privoxy-suse.spec,v 1.33 2009/03/21 10:46:15 fabiankeil Exp $
#
# Written by and Copyright (C) 2001-2006 the SourceForge
# Privoxy team. http://www.privoxy.org/
#
# Based on the Internet Junkbuster originally written
# by and Copyright (C) 1997 Anonymous Coders and 
# Junkbusters Corporation.  http://www.junkbusters.com
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

# do not set to %{name}
%define privoxyconf %{_sysconfdir}/privoxy
%define privoxy_uid 73
%define privoxy_gid 73


Summary:      Privoxy - privacy enhancing proxy
Vendor:       Privoxy.Org
Name:         privoxy-suse
Distribution: defineme
Version: 3.0.12
Release: 1
# Needs makefile change: Source: http://prdownloads.sourceforge.net/ijbswa/privoxy-%{version}-%{status}-src.tar.gz
Source: http://prdownloads.sourceforge.net/ijbswa/privoxy-%{version}.tar.gz
# not sure if this works
BuildRoot: %{_tmppath}/%{name}-%{version}-root
Packager:     http://www.privoxy.org/
License:     GPL
Group:        Networking/Utilities
URL:          http://www.privoxy.org/
Autoreqprov:  on
BuildRequires: perl gzip libtool autoconf
Conflicts: junkbuster-raw junkbuster-blank junkbuster-suse junkbuster privoxy

#
# -----------------------------------------------------------------------------
#
%description
Privoxy is a web proxy with advanced filtering capabilities for
protecting privacy, modifying web page data, managing cookies,
controlling access, and removing ads, banners, pop-ups and other
obnoxious Internet junk. Privoxy has a very flexible configuration and
can be customized to suit individual needs and tastes. Privoxy has 
application for both stand-alone systems and multi-user networks.

Privoxy is based on the Internet Junkbuster.

Authors:
--------
    http://www.privoxy.org/

SuSE series: n

#
# -----------------------------------------------------------------------------
#
%prep
%setup -c

#
# -----------------------------------------------------------------------------
#
%build
autoheader
autoconf
./configure --disable-dynamic-pcre
make


## Explicitily stripping is not recomended.
## This is handled altomaticaly by RPM, and can couse troubles if
## anyone wants to build an unstriped version - morcego
#strip privoxy

#
# -----------------------------------------------------------------------------
#
%install
[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT
mkdir -p ${RPM_BUILD_ROOT}%{_sbindir} \
         ${RPM_BUILD_ROOT}%{_mandir}/man8 \
         ${RPM_BUILD_ROOT}/var/log/privoxy \
         ${RPM_BUILD_ROOT}%{privoxyconf}/templates \
         ${RPM_BUILD_ROOT}%{_sysconfdir}/logrotate.d \
         ${RPM_BUILD_ROOT}%{_sysconfdir}/init.d
gzip README AUTHORS ChangeLog privoxy.1 LICENSE || /bin/true
install -s -m 744 privoxy $RPM_BUILD_ROOT%{_sbindir}/privoxy
cp -f privoxy.1.gz $RPM_BUILD_ROOT%{_mandir}/man8/privoxy.8.gz
cp -f *.action $RPM_BUILD_ROOT%{privoxyconf}/
cp -f default.filter $RPM_BUILD_ROOT%{privoxyconf}/default.filter
cp -f trust $RPM_BUILD_ROOT%{privoxyconf}/trust
cp -f templates/*  $RPM_BUILD_ROOT%{privoxyconf}/templates/
cp -f privoxy.logrotate $RPM_BUILD_ROOT%{_sysconfdir}/logrotate.d/privoxy
install -m 755 privoxy.init.suse $RPM_BUILD_ROOT%{_sysconfdir}/init.d/privoxy
install -m 711 -d $RPM_BUILD_ROOT/var/log/privoxy
ln -sf /etc/init.d/privoxy $RPM_BUILD_ROOT/usr/sbin/rcprivoxy

# verify all file locations, etc. in the config file
# don't start with ^ or commented lines are not replaced
cat config | \
    sed 's/^confdir.*/confdir \/etc\/privoxy/g' | \
#    sed 's/^permissionsfile.*/permissionsfile \/etc\/privoxy\/permissionsfile/g' | \
#    sed 's/^filterfile.*/default.filter \/etc\/privoxy\/default.filter/g' | \
#    sed 's/^logfile.*/logfile \/var\/log\/privoxy\/logfile/g' | \
#    sed 's/^forward.*/forward \/etc\/privoxy\/forward/g' | \
#    sed 's/^aclfile.*/aclfile \/etc\/privoxy\/aclfile/g' > \
    sed 's/^logdir.*/logdir \/var\/log\/privoxy/g' > \
    $RPM_BUILD_ROOT%{privoxyconf}/config

#
# -----------------------------------------------------------------------------
#
%pre
# We check to see if the user privoxy exists.
# If it does, we do nothing
# If we don't, we check to see if the user junkbust exist and, in case it
# does, we change it do privoxy. If it also does not exist, we create the
# privoxy user -- morcego
id privoxy > /dev/null 2>&1 
if [ $? -eq 1 ]; then
	id junkbust > /dev/null 2>&1 
	if [ $? -eq 0 ]; then
		/usr/sbin/usermod -u %{privoxy_uid} -g %{privoxy_gid} -l privoxy -d %{_sysconfdir}/privoxy -s "" junkbust  > /dev/null 2>&1
	else
# -r does not work on suse.
		/usr/sbin/groupadd -g %{privoxy_gid} privoxy
		/usr/sbin/useradd -u %{privoxy_uid} -d %{_sysconfdir}/privoxy -g privoxy -s "" privoxy > /dev/null 2>&1 
	fi
fi

#
# -----------------------------------------------------------------------------
#
%post
[ -f /var/log/privoxy/privoxy ] &&\
 mv -f /var/log/privoxy/privoxy /var/log/privoxy/logfile || /bin/true
chown -R privoxy:privoxy /var/log/privoxy 2>/dev/null
chown -R privoxy:privoxy /etc/privoxy 2>/dev/null
# not available on suse
#if [ "$1" = "1" ]; then
#     /sbin/chkconfig --add privoxy
#	/sbin/service privoxy condrestart > /dev/null 2>&1
#fi
# 01/09/02 HB, getting rid of any user=junkbust
# Changed by morcego to use the id command.
id junkbust > /dev/null 2>&1 && /usr/sbin/userdel junkbust || /bin/true
sbin/insserv etc/init.d/privoxy

#
# -----------------------------------------------------------------------------
#
%preun
# need to stop the service on suse. swa.
#if [ "$1" = "0" ]; then
#	/sbin/service privoxy stop > /dev/null 2>&1 ||:
#fi

#
# -----------------------------------------------------------------------------
#
%postun
sbin/insserv etc/init.d/
# dont forget to remove user and group privoxy
id privoxy > /dev/null 2>&1 && /usr/sbin/userdel privoxy || /bin/true

#
# -----------------------------------------------------------------------------
#
%clean
[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

#
# -----------------------------------------------------------------------------
#
%files
%defattr(-,root,root)
%doc README.gz AUTHORS.gz ChangeLog.gz LICENSE.gz
%doc doc/webserver/developer-manual
%doc doc/webserver/user-manual
%doc doc/webserver/faq
%doc doc/webserver/p_doc.css
%doc doc/webserver/privoxy-index.html
%doc doc/webserver/images
%doc doc/webserver/man-page

%dir %{privoxyconf}
%config %{privoxyconf}/*
%attr(0740,privoxy,privoxy) %dir /var/log/privoxy
%config %{_sysconfdir}/logrotate.d/privoxy
%attr(0755,root,root)/usr/sbin/privoxy
%{_mandir}/man8/*
%config %{_sysconfdir}/init.d/privoxy
/usr/sbin/rcprivoxy

#
# -----------------------------------------------------------------------------
#
%changelog
* Wed Sep 20 2006 Hal Burgiss <hal@foobox.net>
- Bump version for 3.0.5.

* Fri Sep 08 2006 Hal Burgiss <hal@foobox.net>
- Bump version for 3.0.4.

* Wed Mar 26 2003 Andreas Oesterhelt <andreas@oesterhelt.org>
- Bump version for 3.0.2.

* Wed Mar 19 2003 Hal Burgiss <hal@foobox.net>
- Bump version for 3.0.1.

* Tue Aug 25 2002 Hal Burgiss <hal@foobox.net>
- Bump version for 3.0.0 :) 

* Tue Aug 06 2002 Hal Burgiss <hal@foobox.net>
- Reset version for 2.9.18. 

* Wed Jul 30 2002 Hal Burgiss <hal@foobox.net>
- Reset version for 2.9.17.

* Sat Jul 27 2002 Hal Burgiss <hal@foobox.net>
- Reset version and release for 2.9.16.

* Mon May 27 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.15-1
- Index.html is now privoxy-index.html for doc usage.

* Mon May 27 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.15-1
- Index.html is now privoxy-index.html for doc usage.

* Sat May 25 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.15-1
- Add html man page so index.html does not 404.

* Fri May 24 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.15-1
- Add doc/images directory.

* Fri May 03 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-suse-2.9.15-1
- Version bump

* Fri Apr 26 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-suse-2.9.14-3
- Changing Vendor to Privoxy.Org

* Mon Apr 22 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-suse-2.9.14-2
- Bumping release to reflect the new value on configure.in
- Taking the oportunity to change the header Copyright to License. The
  Copyright headers is deprecated, and after all, GPL is a license, not a
  Copyright

* Mon Apr 08 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.13-4
- Add LICENSE.gz, p_web.css, and index.html. Add autoconf
- to Buildrequires.

* Wed Mar 27 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.13-3
- Doc css has changed names.

* Tue Mar 25 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.13-3
- Minor fix to description.

* Sun Mar 24 2002 Hal Burgiss <hal@foobox.net>
- added faq to docs.

* Thu Mar 21 2002 Hal Burgiss <hal@foobox.net>
- added ijb_docs.css to docs.

* Mon Mar 11 2002 Hal Burgiss <hal@foobox.net>
- Remove --enable-no-gifs from configure.

* Sun Mar 03 2002 Hal Burgiss <hal@foobox.net>
- /bin/false for shell causes init script to fail. Reverting.

* Wed Jan 09 2002 Hal Burgiss <hal@foobox.net>
- Removed UID 73. Included user-manual and developer-manual in docs.
  Include other actions files. Default shell is now /bin/false.
  Userdel user=junkbust. ChangeLog was not zipped. Removed
  RPM_OPT_FLAGS kludge.

* Fri Dec 28 2001 Thomas Steudten <thomas@steudten.ch>
- add paranoia check for 'rm -rf $RPM_BUILD_ROOT'
- add gzip to 'BuildRequires'

* Sat Dec  1 2001 Hal Burgiss <hal@foobox.net>
- actionsfile is now ijb.action.

* Tue Nov  6 2001 Thomas Steudten <thomas@steudten.ch>
- Compress manpage
- Add more documents for installation
- Add version string to name and source

* Wed Oct 24 2001 Hal Burigss <hal@foobox.net>
- Back to user 'junkbuster' and fix configure macro.

* Wed Oct 10 2001 Hal Burigss <hal@foobox.net>
- More changes for user 'junkbust'. Init script had 'junkbuster'.

* Sun Sep 23 2001 Hal Burgiss <hal@foobox.net>
- Change of $RPM_OPT_FLAGS handling. Added new HTML doc files.
- Changed owner of /etc/junkbuster to shut up PAM/xauth log noise.

* Thu Sep 13 2001 Hal Burgiss <hal@foobox.net>
- Added $RPM_OPT_FLAGS support, renaming of old logfile, and
- made sure no default shell exists for user junkbust.

* Sun Jun  3 2001 Stefan Waldherr <stefan@waldherr.org>
- rework of RPM
* Wed Feb 14 2001 - uli@suse.de
- fixed init script
* Wed Dec 06 2000 - bjacke@suse.de
- renamed package to junkbuster
- fixed copyright tag
* Thu Nov 30 2000 - uli@suse.de
- moved init script to /etc/init.d
* Wed Feb 16 2000 - kukuk@suse.de
- Move /usr/man -> /usr/share/man
- Mark /etc/ijb as "config(noreplace)"
* Mon Sep 20 1999 - uli@suse.de
- fixed init script
* Mon Sep 13 1999 - bs@suse.de
- ran old prepare_spec on spec file to switch to new prepare_spec.
* Thu Apr 01 1999 - daniel@suse.de
- do not start ijb as root (security)
* Tue Mar 30 1999 - daniel@suse.de
- don´t use saclfile.ini
* Tue Mar 30 1999 - daniel@suse.de
- small fix to whitelist-configuration,
  version is and was 2.0.2 WITHOUT Stefan Waldherr's patches
  (http://www.waldherr.org/junkbuster/)
* Mon Mar 01 1999 - daniel@suse.de
- new package: version 2.0

# $Log: privoxy-suse.spec,v $
# Revision 1.33  2009/03/21 10:46:15  fabiankeil
# Bump version to 3.0.12.
#
# Revision 1.32  2009/02/15 17:18:14  fabiankeil
# - Bump version to 3.0.11.
#
# Revision 1.31  2008/08/30 12:46:49  fabiankeil
# The jarfile directive is gone. Update accordingly.
#
# Revision 1.30  2008/08/13 16:57:46  fabiankeil
# Change version to 3.0.10.
#
# Revision 1.29  2008/03/02 17:36:43  fabiankeil
# Set version to 3.0.9.
#
# Revision 1.28  2008/01/20 14:30:59  fabiankeil
# Set version to 3.0.8.
#
# Revision 1.27  2006/11/18 14:37:12  fabiankeil
# Bump version to 3.0.6.
#
# Revision 1.26  2006/09/20 23:51:26  hal9
# Bump versions to 3.0.5
#
# Revision 1.25  2006/09/09 00:35:10  hal9
# Bumped versions to 3.0.4. Both files should be checked further.
#
# Revision 1.24  2006/07/18 14:48:47  david__schmidt
# Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
# with what was really the latest development (the v_3_0_branch branch)
#
# Revision 1.20.2.10  2004/01/30 17:09:29  oes
# Bumped version for 3.0.3
#
# Revision 1.20.2.9  2003/03/26 00:24:58  oes
# Bump version for 3.0.2
#
# Revision 1.20.2.8  2003/03/20 03:27:11  hal9
# Bump version for 3.0.1 pending release.
#
# Revision 1.20.2.7  2002/08/25 23:36:03  hal9
# Bump version for 3.0.0.
#
# Revision 1.20.2.6  2002/08/10 11:28:50  oes
# Bumped version
#
# Revision 1.20.2.5  2002/08/07 01:08:49  hal9
# Bumped version to 2.9.18.
#
# Revision 1.20.2.4  2002/07/30 21:51:19  hal9
# Bump version to 2.9.17.
#
# Revision 1.20.2.3  2002/07/27 15:47:10  hal9
# Reset version and release for 2.9.16.
#
# Revision 1.20.2.2  2002/05/28 02:39:38  hal9
# Replace index.html with privoxy-index.html for docs.
#
# Revision 1.20.2.1  2002/05/26 17:20:23  hal9
# Add images to doc dirs.
#
# Revision 1.20  2002/05/25 02:08:23  hal9
# Add doc/images directory.
# Redhat: alphabetized list of templates (and I think added one in the process)
#
# Revision 1.19  2002/05/03 17:14:36  morcego
# *.spec: Version bump to 2.9.15
# -rh.spec: noreplace for %%{privoxyconf}/config
#           Will interrupt the build if versions from configure.in and
# 		specfile do not match
#
# Revision 1.18  2002/04/27 20:26:59  swa
# uid, gui 73 incorporated
#
# Revision 1.17  2002/04/26 15:51:05  morcego
# Changing Vendor value to Privoxy.Org
#
# Revision 1.16  2002/04/22 16:32:31  morcego
# configure.in, *.spec: Bumping release to 2 (2.9.14-2)
# -rh.spec: uid and gid are now macros
# -suse.spec: Changing the header Copyright to License (Copyright is
#             deprecable)
#
# Revision 1.15  2002/04/16 18:49:07  oes
# Build with static built-in pcre
#
# Revision 1.14  2002/04/11 17:57:40  oes
# Fixed(?) Conflicts: Provides: Obsoletes:
#
# Revision 1.13  2002/04/11 10:09:20  oes
# Version 2.9.14
#
# Revision 1.12  2002/04/09 13:29:43  swa
# build suse and gen-dist with html docs. do not generate docs while building rpm
#
# Revision 1.11  2002/04/09 03:12:37  hal9
# Add LICENSE, p_web.css and index.html. Add autoconf to buildrequires.
#
# Revision 1.10  2002/04/08 20:24:13  swa
# fixed JB spelling
#
# Revision 1.9  2002/03/30 09:01:52  swa
# new release
#
# Revision 1.8  2002/03/27 23:46:41  hal9
# ijb_docs.css to p_doc.css
#
# Revision 1.7  2002/03/27 00:49:39  hal9
# Minor fix to description.
#
# Revision 1.6  2002/03/26 22:29:55  swa
# we have a new homepage!
#
# Revision 1.5  2002/03/25 03:10:50  hal9
# Added faq to docs.
#
# Revision 1.4  2002/03/24 12:56:21  swa
# name change related issues.
#
# Revision 1.3  2002/03/24 12:44:31  swa
# new version string
#
# Revision 1.2  2002/03/24 11:40:14  swa
# name change
#
# Revision 1.1  2002/03/24 11:23:44  swa
# name change
#
# Revision 1.21  2002/03/21 16:04:33  hal9
# added ijb_docs.css to %%doc
#
# Revision 1.20  2002/03/12 13:42:14  sarantis
# remove hardcoded "ijbswa" from build phase
#
# Revision 1.19  2002/03/11 22:59:05  hal9
# Remove --enable-no-gifs
#
# Revision 1.18  2002/03/11 12:30:31  swa
# be consistent with rh spec file
#
# Revision 1.17  2002/03/08 19:30:23  swa
# remove user junkbuster after de-installation.
# synced suse with rh-specfile. installation
# and de-installation seem to work.
#
# Revision 1.16  2002/03/08 18:40:44  swa
# build requires tools. useradd and del works
# now.
#
# Revision 1.15  2002/03/07 19:23:50  swa
# i hate to scroll. suse: wrong configdir.
#
# Revision 1.14  2002/03/07 19:10:21  swa
# builds cleanly. thanks to kukuk@suse.de
# not yet tested.
#
# Revision 1.13  2002/03/07 18:25:56  swa
# synced redhat and suse build process
#
# Revision 1.12  2002/03/02 15:50:04  swa
# 2.9.11 version. more input for docs.
#
# Revision 1.11  2001/12/02 10:29:26  swa
# New version made these changes necessary.
#
# Revision 1.10  2001/10/31 19:27:27  swa
# consistent description. new name for suse since
# we had troubles with rpms of identical names
# on the webserver.
#
# Revision 1.9  2001/10/26 18:17:23  swa
# new version string
#
# Revision 1.8  2001/09/13 16:22:42  swa
# man page is legacy. suse rpm now contains html
# documentation.
#
# Revision 1.7  2001/09/10 17:44:22  swa
# integrate three pieces of documentation.
#
# Revision 1.6  2001/09/10 16:29:23  swa
# binary contained debug info.
# buildroot definition fucks up the build process under suse.
# program needs to write in varlogjunkbuster
# install all templates
# create varlogjunkbuster
#
# Revision 1.5  2001/06/09 09:13:29  swa
# description shorter
#
# Revision 1.4  2001/06/08 20:53:36  swa
# use buildroot, export init to separate file (better manageability)
#
# Revision 1.3  2001/06/07 17:28:10  swa
# cosmetics
#
# Revision 1.2  2001/06/07 17:18:44  swa
# header fixed
#
#
