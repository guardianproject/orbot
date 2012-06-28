# $Id: privoxy-rh.spec,v 1.63 2009/03/21 10:46:15 fabiankeil Exp $
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

# Defines should happen in the begining of the file
%define veryoldname junkbust
%define oldname junkbuster
%define privoxyconf %{_sysconfdir}/%{name}
%define privoxy_uid 73
%define privoxy_gid 73

Name: privoxy
# ATTENTION
# Version and release should be updated accordingly on configure.in and
# configure. Otherwise, the package can be build with the wrong value
Version: 3.0.12
Release: 1
Summary: Privoxy - privacy enhancing proxy
License: GPL
Source0: http://dl.sf.net/ijbswa/%{name}-%{version}-stable-src.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-root
Group: System Environment/Daemons
URL: http://www.privoxy.org/
Obsoletes: junkbuster-raw junkbuster-blank junkbuster
# Prereq: /usr/sbin/useradd , /sbin/chkconfig , /sbin/service 
Prereq: shadow-utils, chkconfig, initscripts, sh-utils
BuildRequires: perl gzip sed libtool autoconf 
Conflicts: junkbuster-raw junkbuster-blank junkbuster

%description 
Privoxy is a web proxy with advanced filtering capabilities for
protecting privacy, filtering web page data, managing cookies,
controlling access, and removing ads, banners, pop-ups and other
obnoxious Internet junk. Privoxy has a very flexible configuration and
can be customized to suit individual needs and tastes. Privoxy has application
for both stand-alone systems and multi-user networks.

Privoxy is based on the Internet Junkbuster.

%prep
#%setup -q -c
%setup -q -n "%{name}-%{version}-stable"

%build

# We check to see if versions match
VERSION_MAJOR=3
VERSION_MINOR=0
VERSION_POINT=12

# find CVS files and remove it.
find -name CVS | xargs rm -rf

CONFIG_VERSION=`cat configure.in | sed -n -e 's/^VERSION_MAJOR=\([0-9]*\)/\1./p' -e 's/^VERSION_MINOR=\([0-9]*\)/\1./p' -e 's/^VERSION_POINT=\([0-9]*\)/\1/p' | awk '{printf $1}'`
if [ "%{version}" != "${CONFIG_VERSION}" ]; then
	echo "The version declared on the specfile does not match the version"
	echo "declared on configure.in. This should not happen. The build will"
	echo "be interrupted now, so you can fix it."
	exit 1
fi
autoheader
autoconf
%configure --disable-dynamic-pcre
make 
# Docs are in CVS and tarball now.
#%%make dok

## Explicitily stripping is not recomended.
## This is handled altomaticaly by RPM, and can couse troubles if
## anyone wants to build an unstriped version - morcego
#strip %{name}

%install
[ "%{buildroot}" != "/" ] && rm -rf %{buildroot}
mkdir -p %{buildroot}%{_sbindir} \
         %{buildroot}%{_mandir}/man1 \
         %{buildroot}%{_localstatedir}/log/%{name} \
         %{buildroot}%{privoxyconf}/templates \
         %{buildroot}%{_sysconfdir}/logrotate.d \
         %{buildroot}%{_sysconfdir}/rc.d/init.d 

## Manual gziping of manpages should not be done, once it can
## break the building on some distributions. Anyway, rpm does it
## automagicaly these days
## Gziping the documentation files is not recomended - morcego
#gzip README AUTHORS ChangeLog %{name}.1 || /bin/true

install -s -m 744 %{name} %{buildroot}%{_sbindir}/%{name}

# Using sed to "convert" from DOS format to UNIX
# This is important behaviour, and should not be removed without some
# other assurance that these files don't get packed in the the
# wrong format
for i in `ls *.action`
do
       cat $i | sed -e 's/[[:cntrl:]]*$//' > %{buildroot}%{privoxyconf}/$i
done
cat default.filter | sed -e 's/[[:cntrl:]]*$//' > %{buildroot}%{privoxyconf}/default.filter
cat user.filter | sed -e 's/[[:cntrl:]]*$//' > %{buildroot}%{privoxyconf}/user.filter
cat trust | sed -e 's/[[:cntrl:]]*$//' > %{buildroot}%{privoxyconf}/trust
(
cd templates
for i in `ls`
do
	cat $i | sed -e 's/[[:cntrl:]]*$//' > %{buildroot}%{privoxyconf}/templates/$i
done
)

cp -f %{name}.1 %{buildroot}%{_mandir}/man1/%{name}.1
cp -f %{name}.logrotate %{buildroot}%{_sysconfdir}/logrotate.d/%{name}
install -m 755 %{name}.init %{buildroot}%{_sysconfdir}/rc.d/init.d/%{name}
install -m 711 -d %{buildroot}%{_localstatedir}/log/%{name}

# verify all file locations, etc. in the config file
# don't start with ^ or commented lines are not replaced
## Changing the sed paramter delimiter to @, so we don't have to
## escape the slashes
cat config | \
    sed 's@^confdir.*@confdir %{privoxyconf}@g' | \
#    sed 's/^permissionsfile.*/permissionsfile \/etc\/%{name}\/permissionsfile/g' | \
#    sed 's/^filterfile.*/default.filter \/etc\/%{name}\/default.filter/g' | \
#    sed 's/^logfile.*/logfile \%{_localstatedir}\/log\/%{name}\/logfile/g' | \
#    sed 's/^forward.*/forward \/etc\/%{name}\/forward/g' | \
#    sed 's/^aclfile.*/aclfile \/etc\/%{name}\/aclfile/g' > \
    sed 's@^logdir.*@logdir %{_localstatedir}/log/%{name}@g' | \
    sed 's@#user-manual http://www.privoxy.org/user-manual/@user-manual %{_docdir}/%{name}-%{version}/user-manual/@g' | \
    sed -e 's/[[:cntrl:]]*$//' > \
    %{buildroot}%{privoxyconf}/config
perl -pe 's/{-no-cookies}/{-no-cookies}\n\.redhat.com/' default.action >\
    %{buildroot}%{privoxyconf}/default.action


## Macros are expanded even on commentaries. So, we have to use %%
## -- morcego
#%%makeinstall

%pre
# This is where we handle old usernames (junkbust and junkbuster)
# I'm not sure we should do that, but this is the way we have been
# doing it for some time now -- morcego
# We should do it for the group as well -- morcego
# Doing it by brute force. Much cleaner (no more Mr. Nice Guy) -- morcego

# Same for username
usermod -u %{privoxy_uid} -g %{privoxy_gid} -l %{name} -d %{_sysconfdir}/%{name} -s "" %{oldname} > /dev/null 2>&1 || :
usermod -u %{privoxy_uid} -g %{privoxy_gid} -l %{name} -d %{_sysconfdir}/%{name} -s "" %{veryoldname} > /dev/null 2>&1 || :
userdel %{oldname} > /dev/null 2>&1 ||:
userdel %{veryoldname} > /dev/null 2>&1 ||:

# Change the group name. Remove anything left behind.
groupmod -g %{privoxy_gid} -n %{name} %{oldname} > /dev/null 2>&1 ||:
groupmod -g %{privoxy_gid} -n %{name} %{veryoldname} > /dev/null 2>&1 ||:
groupdel %{oldname} > /dev/null 2>&1 ||:
groupdel %{veryoldname} > /dev/null 2>&1 ||:

# Doublecheck to see if the group exist, and that it has the correct gid
/bin/grep -E '^%{name}:' %{_sysconfdir}/group > /dev/null 2>&1
if [ $? -eq 1 ]; then
	# Looks like it does not exist. Create it
	groupadd -g %{privoxy_gid} %{name} > /dev/null 2>&1
else
	/bin/grep -E '^%{name}:[^:]*:%{privoxy_gid}:' %{_sysconfdir}/group > /dev/null 2>&1
	if [ $? -eq 1 ]; then
		# The group exists, but does not have the correct gid
		groupmod -g %{privoxy_gid} %{name} > /dev/null 2>&1
	fi
fi

# Check to see if everything is okey. Create user if it still does not
# exist
id %{name} > /dev/null 2>&1
if [ $? -eq 1 ]; then
	%{_sbindir}/useradd -u %{privoxy_uid} -g %{privoxy_gid} -d %{_sysconfdir}/%{name} -r -s "" %{name} > /dev/null 2>&1 
fi

# Double check that the group has the correct uid
P_UID=`id -u %{name} 2>/dev/null`
if [ $P_UID -ne %{privoxy_uid} ]; then
	%{_sbindir}/usermod -u %{privoxy_uid} %{name}
fi

# The same for the gid
P_GID=`id -g %{name} 2>/dev/null`
if [ $P_GID -ne %{privoxy_gid} ]; then
	%{_sbindir}/usermod -g %{privoxy_gid} %{name}
fi

%post
# for upgrade from 2.0.x
[ -f %{_localstatedir}/log/%{oldname}/logfile ] && {
  mv -f %{_localstatedir}/log/%{oldname}/logfile %{_localstatedir}/log/%{name}/logfile ||: ;
  chown -R %{name}:%{name} %{_localstatedir}/log/%{name} 2>/dev/null ||: ;
}
[ -f %{_localstatedir}/log/%{name}/%{name} ] && {
  mv -f %{_localstatedir}/log/%{name}/%{name} %{_localstatedir}/log/%{name}/logfile ||: ;
  chown -R %{name}:%{name} %{_sysconfdir}/%{name} 2>/dev/null ||: ;
}
/sbin/chkconfig --add privoxy
if [ "$1" = "1" ]; then
	/sbin/service %{name} condrestart > /dev/null 2>&1 ||:
fi

%preun
/sbin/service %{veryoldname} stop > /dev/null 2>&1 ||:
/sbin/service %{oldname} stop > /dev/null 2>&1 ||:

if [ "$1" = "0" ]; then
	/sbin/service %{name} stop > /dev/null 2>&1 ||:
	/sbin/chkconfig --del privoxy
fi

%postun
#if [ "$1" -ge "1" ]; then
#	/sbin/service %{name} condrestart > /dev/null 2>&1
#fi
# We only remove it we this is not an upgrade
if [ "$1" = "0" ]; then
	id privoxy > /dev/null 2>&1 && %{_sbindir}/userdel privoxy || /bin/true
	/bin/grep -E '^%{name}:' %{_sysconfdir}/group > /dev/null && %{_sbindir}/groupdel %{name} || /bin/true
fi

%clean
[ "%{buildroot}" != "/" ] && rm -rf %{buildroot}

%files
%defattr(0644,root,root,0755)
%doc README AUTHORS ChangeLog LICENSE 
#%doc doc/text/developer-manual.txt doc/text/user-manual.txt doc/text/faq.txt
%doc doc/webserver/developer-manual
%doc doc/webserver/user-manual
%doc doc/webserver/faq
%doc doc/webserver/p_doc.css doc/webserver/privoxy-index.html
%doc doc/webserver/images
%doc doc/webserver/man-page

# ATTENTION FOR defattr change here !
%defattr(0644,%{name},%{name},0755)

%dir %{privoxyconf}
%dir %{privoxyconf}/templates
%dir %{_localstatedir}/log/%{name}

%attr(0744,%{name},%{name})%{_sbindir}/%{name}

# WARNING ! WARNING ! WARNING ! WARNING ! WARNING ! WARNING ! WARNING !
# We should not use wildchars here. This could mask missing files problems
# -- morcego
# WARNING ! WARNING ! WARNING ! WARNING ! WARNING ! WARNING ! WARNING !
%config(noreplace) %{privoxyconf}/config
%config(noreplace) %{privoxyconf}/user.action
%config %{privoxyconf}/match-all.action
%config %{privoxyconf}/default.action
%config %{privoxyconf}/default.filter
%config %{privoxyconf}/regression-tests.action
%config(noreplace) %{privoxyconf}/user.filter
%config(noreplace) %{privoxyconf}/trust

# Please keep these alphabetized so its easier to find one that 
# is not included.
%config %{privoxyconf}/templates/blocked
%config %{privoxyconf}/templates/cgi-error-404
%config %{privoxyconf}/templates/cgi-error-bad-param
%config %{privoxyconf}/templates/cgi-error-disabled
%config %{privoxyconf}/templates/cgi-error-file
%config %{privoxyconf}/templates/cgi-error-file-read-only
%config %{privoxyconf}/templates/cgi-error-modified
%config %{privoxyconf}/templates/cgi-error-parse
%config %{privoxyconf}/templates/cgi-style.css
%config %{privoxyconf}/templates/connect-failed
%config %{privoxyconf}/templates/default
%config %{privoxyconf}/templates/forwarding-failed
%config %{privoxyconf}/templates/edit-actions-add-url-form
%config %{privoxyconf}/templates/edit-actions-for-url
%config %{privoxyconf}/templates/edit-actions-for-url-filter
%config %{privoxyconf}/templates/edit-actions-list
%config %{privoxyconf}/templates/edit-actions-list-button
%config %{privoxyconf}/templates/edit-actions-list-section
%config %{privoxyconf}/templates/edit-actions-list-url
%config %{privoxyconf}/templates/edit-actions-remove-url-form
%config %{privoxyconf}/templates/edit-actions-url-form
%config %{privoxyconf}/templates/mod-local-help
%config %{privoxyconf}/templates/mod-support-and-service
%config %{privoxyconf}/templates/mod-title
%config %{privoxyconf}/templates/mod-unstable-warning
%config %{privoxyconf}/templates/no-such-domain
%config %{privoxyconf}/templates/show-request
%config %{privoxyconf}/templates/show-status
%config %{privoxyconf}/templates/show-status-file
%config %{privoxyconf}/templates/show-url-info
%config %{privoxyconf}/templates/show-version
%config %{privoxyconf}/templates/toggle
%config %{privoxyconf}/templates/toggle-mini
%config %{privoxyconf}/templates/untrusted
%config %{privoxyconf}/templates/url-info-osd.xml

# Attention, new defattr change here !
%defattr(0644,root,root,0755)

%config(noreplace) %{_sysconfdir}/logrotate.d/%{name}
%config(noreplace) %attr(0755,root,root) %{_sysconfdir}/rc.d/init.d/%{name}

%{_mandir}/man1/%{name}.*

%changelog
* Sat Jun 18 2008 Hal Burgiss <hal@foobox.net>
- Remove reference to txt docs.

* Sat Oct 18 2006 Hal Burgiss <hal@foobox.net>
- Bump version to 3.0.6

* Sat Sep 23 2006 Jochen Schlick <j.schlick_at_decagon_de> 3.0.5-1
- let user-manual point to local documentation

* Thu Sep 21 2006 Hal Burgiss <hal@foobox.net>
- Fix user.filter install section and clean up CVS cruft in tarball.

* Wed Sep 20 2006 Hal Burgiss <hal@foobox.net>
- Bump version to 3.0.5

* Fri Sep 08 2006 Hal Burgiss <hal@foobox.net>
- Bump version to 3.0.4

* Sat Sep 02 2006 Hal Burgiss <hal@foobox.net>
- Include new file, user.filter. Do not overwrite "trust" file 
  (does anyone use this?).

* Wed Mar 26 2003 Andreas Oesterhelt <andreas@oesterhelt.org>
- Bump version for 3.0.2.

* Wed Mar 19 2003 Hal Burgiss <hal@foobox.net>
- Bump version for 3.0.1.

* Tue Aug 25 2002 Hal Burgiss <hal@foobox.net>
- Bump version for 3.0.0 :)

* Tue Aug 06 2002 Hal Burgiss <hal@foobox.net>
- Reset version for 2.9.20.

* Tue Jul 30 2002 Hal Burgiss <hal@foobox.net>
- Reset version for 2.9.18.

* Sat Jul 27 2002 Hal Burgiss <hal@foobox.net>
- Reset version and release for 2.9.16.

* Fri Jul 12 2002 Karsten Hopp <karsten@redhat.de>
- don't use ghost files for rcX.d/*, using chkconfig is the 
  correct way to do this job (#68619)

* Fri Jul 05 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.15-8
- Changing delete order for groups and users (users should be first)

* Wed Jul 03 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.15-7
- Changing sed expression that removed CR from the end of the lines. This
  new one removes any control caracter, and should work with older versions
  of sed

* Tue Jul 02 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.15-6
- Fixing defattr values. File and directory modes where swapped

* Tue Jul 02 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.15-5
- Bumping Release number (which should be changed every time the specfile
  is)

* Tue Jul 02 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.15-4
- Fix typo in templates creation.

* Wed Jun 26 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.15-4
- Fixing issues created by specfile sync between branches
  - Correcting the release number (WARNING)
  - Reintroducing text file conversion (dos -> unix)
  - Reconverting hardcoded directories to macros
  - Refixing ownership of privoxy files (now using multiple defattr
    definitions)

* Thu Jun 20 2002 Karsten Hopp <karsten@redhat.de>
- fix several .spec file issues to shut up rpmlint
  - non-standard-dir-perm /var/log/privoxy 0744
  - invalid-vendor Privoxy.Org (This is ok for binaries compiled by privoxy
    members, but not for packages from Red Hat)
  - non-standard-group Networking/Utilities
  - logrotate and init scripts should be noreplace

* Mon May 27 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.15-1
- Index.html is now privoxy-index.html for doc usage.

* Sat May 25 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.15-1
- Add html man page so index.html does not 404.

* Fri May 24 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.15-1
- Add another template and alphabetize these for easier tracking.
- Add doc/images directory.

* Wed May 15 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.15-1
- Add templates/edit-actions-list-button

* Fri May 03 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.15-1
- Version bump
- Adding noreplace for %%{privoxyconf}/config
- Included a method to verify if the versions declared on the specfile and
  configure.in match. Interrupt the build if they don't.

* Fri Apr 26 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.14-3
- Changing Vendor to Privoxy.Org

* Tue Apr 23 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.14-2
- Adjust for new *actions files.

* Mon Apr 22 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.14-2
- Removed the redhat hack that prevented the user and group from
  being dealocated. That was a misundestanding of my part regarding
  redhat policy.

* Mon Apr 22 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.14-2
- Using macros to define uid and gid values
- Bumping release

* Mon Apr 22 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.14-1
- Changes to fixate the uid and gid values as (both) 73. This is a 
  value we hope to standarize for all distributions. RedHat already
  uses it, and Conectiva should start as soon as I find where the heck
  I left my cluebat :-)
- Only remove the user and group on uninstall if this is not redhat, once
  redhat likes to have the values allocated even if the package is not 
  installed

* Tue Apr 16 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.13-6
- Add --disable-dynamic-pcre to configure.

* Wed Apr 10 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.13-5
- Relisting template files on the %%files section

* Tue Apr 09 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.13-4
- Removed 'make dok'. Docs are all maintained in CVS (and tarball) now.

* Mon Apr 08 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.13-4
- Add templates/cgi-style.css, faq.txt, p_web.css, LICENSE
- Remove templates/blocked-compact.
- Add more docbook stuff to Builderquires.

* Thu Mar 28 2002 Sarantis Paskalis <sarantis@cnl.di.uoa.gr>
+ privoxy-2.9.13-3
- Include correct documentation file.

* Tue Mar 26 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.13-3
- Fix typo in Description.

* Tue Mar 26 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.13-3
- Added commentary asking to update the release value on the configure
  script

* Tue Mar 25 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.13-3
- Added the missing edit-actions-for-url-filter to templates.

* Mon Mar 25 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ privoxy-2.9.13-2
- Fixing Release number

* Sun Mar 24 2002 Hal Burgiss <hal@foobox.net>
+ privoxy-2.9.13-2
- Added faq to docs.

* Sun Mar 24 2002 Rodrigo Barbosa <rodrigob@suespammers.org>
+ privoxy-2.9.13-2
- Fixed the init files entries. Now we use %%ghost
- improved username (and groupname) handling on the %%pre section. By improved
  I mean: we do it by brute force now. Much easier to maintain. Yeah, you
  got it right. No more Mr. Nice Guy.
- Removed the userdel call on %%post. No need, once it's complety handled on
  the %%pre section

* Sun Mar 24 2002 Hal Burgiss <hal@foobox.net>
+ junkbusterng-2.9.13-1
  Added autoheader. Added autoconf to buildrequires.

* Sun Mar 24 2002 Hal Burgiss <hal@foobox.net>
+ junkbusterng-2.9.13-1
- Fixed build problems re: name conflicts with man page and logrotate.
- Commented out rc?d/* configs for time being, which are causing a build 
- failure. /etc/junkbuster is now /etc/privoxy. Stefan did other name 
- changes. Fixed typo ';' should be ':' causing 'rpm -e' to fail.

* Fri Mar 22 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ junkbusterng-2.9.13-1
- References to the expression ijb where changed where possible
- New package name: junkbusterng (all in lower case, acording to
  the LSB recomendation)
- Version changed to: 2.9.13
- Release: 1
- Added: junkbuster to obsoletes and conflicts (Not sure this is
  right. If it obsoletes, why conflict ? Have to check it later)
- Summary changed: Stefan, please check and aprove it
- Changes description to use the new name
- Sed string was NOT changed. Have to wait to the manpage to
  change first
- Keeping the user junkbuster for now. It will require some aditional
  changes on the script (scheduled for the next specfile release)
- Added post entry to move the old logfile to the new log directory
- Removing "chkconfig --add" entry (not good to have it automaticaly
  added to the startup list).
- Added preun section to stop the service with the old name, as well
  as remove it from the startup list
- Removed the chkconfig --del entry from the conditional block on
  the preun scriptlet (now handled on the %files section)

* Thu Mar 21 2002 Hal Burgiss <hal@foobox.net>
- added ijb_docs.css to docs.

* Mon Mar 11 2002 Hal Burgiss <hal@foobox.net>
+ junkbuster-2.9.11-8 
- Take out --enable-no-gifs, breaks some browsers.

* Sun Mar 10 2002 Hal Burgiss <hal@foobox.net>
+ junkbuster-2.9.11-8 
- Add --enable-no-gifs to configure.

* Fri Mar 08 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ junkbuster-2.9.11-7
- Added BuildRequires to libtool.

* Tue Mar 06 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ junkbuster-2.9.11-6
- Changed the routined that handle the junkbust and junkbuster users on
  %%pre and %%post to work in a smoother manner
- %%files now uses hardcoded usernames, to avoid problems with package
  name changes in the future

* Tue Mar 05 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ junkbuster-2.9.11-5
- Added "make redhat-dok" to the build process
- Added docbook-utils to BuildRequires

* Tue Mar 05 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ junkbuster-2.9.11-4
- Changing man section in the manpage from 1 to 8
- We now require packages, not files, to avoid issues with apt

* Mon Mar 04 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ junkbuster-2.9.11-3
- Fixing permissions of the init script

* Mon Mar 04 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
+ junkbuster-2.9.11-2
- General specfile fixup, using the best recomended practices, including:
	- Adding -q to %%setup
	- Using macros whereever possible
	- Not using wildchars on %%files section
	- Doubling the percentage char on changelog and comments, to
	  avoid rpm expanding them

* Sun Mar 03 2002 Hal Burgiss <hal@foobox.net>
- /bin/false for shell causes init script to fail. Reverting.

* Wed Jan 09 2002 Hal Burgiss <hal@foobox.net>
- Removed UID 73. Included user-manual and developer-manual in docs.
  Include other actions files. Default shell is now /bin/false.
  Userdel user=junkbust. ChangeLog was not zipped. Removed 
  RPM_OPT_FLAGS kludge.

* Fri Dec 28 2001 Thomas Steudten <thomas@steudten.ch>
- add paranoia check for 'rm -rf %%{buildroot}'
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

* Mon Sep 25 2000 Stefan Waldherr <stefan@waldherr.org>
- CLF Logging patch by davep@cyw.uklinux.net
- Hal DeVore <haldevore@earthling.net> fix akamaitech in blocklist

* Sun Sep 17 2000 Stefan Waldherr <stefan@waldherr.org>
- Steve Kemp skx@tardis.ed.ac.uk's javascript popup patch.
- Markus Breitenbach breitenb@rbg.informatik.tu-darmstadt.de supplied
  numerous fixes and enhancements for Steve's patch.
- adamlock@netscape.com (Adam Lock) in the windows version:
  - Taskbar activity spinner always spins even when logging is
  turned off (which is the default) - people who don't
  like the spinner can turn it off from a menu option.
  - Taskbar popup menu has a options submenu - people can now
  open the settings files for cookies, blockers etc.
  without opening the JB window.
  - Logging functionality works again
  - Buffer overflow is fixed - new code uses a bigger buffer
  and snprintf so it shouldn't overflow anymore.
- Fixed userid swa, group learning problem while installing.
  root must build RPM.
- Added patch by Benjamin Low <ben@snrc.uow.edu.au> that prevents JB to
  core dump when there is no log file.
- Tweaked SuSE startup with the help of mohataj@gmx.net and Doc.B@gmx.de.
- Fixed man page to include imagefile and popupfile.
- Sanity check for the statistics function added.
- "Patrick D'Cruze" <pdcruze@orac.iinet.net.au>: It seems Microsoft
 are transitioning Hotmail from FreeBSD/Apache to Windows 2000/IIS.
 With IIS/5, it appears to omit the trailing \r\n from http header
 only messages.  eg, when I visit http://www.hotmail.com, IIS/5
 responds with a HTTP 302 redirect header.  However, this header
 message is missing the trailing \r\n.  IIS/5 then closes the
 connection.  Junkbuster, unfortunately, discards the header becomes
 it thinks it is incomplete - and it is.  MS have transmitted an
 incomplete header!
- Added bug reports and patch submission forms in the docs.

* Mon Mar 20 2000 Stefan Waldherr <stefan@waldherr.org>
       Andrew <anw@tirana.freewire.co.uk> extended the JB:
       Display of statistics of the total number of requests and the number
       of requests filtered by junkbuster, also the percentage of requests
       filtered. Suppression of the listing of files on the proxy-args page.
       All stuff optional and configurable.

* Sun Sep 12 1999 Stefan Waldherr <stefan@waldherr.org>
       Jan Willamowius (jan@janhh.shnet.org) fixed a bug in the 
       code which prevented the JB from handling URLs of the form
       user:password@www.foo.com. Fixed.

* Mon Aug  2 1999 Stefan Waldherr <stefan@waldherr.org>
	Blank images are no longer cached, thanks to a hint from Markus 
        Breitenbach <breitenb@rbg.informatik.tu-darmstadt.de>. The user 
        agent is NO longer set by the Junkbuster. Sadly, many sites depend 
        on the correct browser version nowadays. Incorporated many 
	suggestions from Jan "Yenya" Kasprzak <kas@fi.muni.cz> for the
        spec file. Fixed logging problem and since runlevel 2 does not 
        use networking, I replaced /etc/rc.d/rc2.d/S84junkbuster with
        /etc/rc.d/rc2.d/K09junkbuster thanks to Shaw Walker 
        <walker@netgate.net>. You should now be able to build this RPM as 
        a non-root user (mathias@weidner.sem.lipsia.de).

* Sun Jan 31 1999 Stefan Waldherr <stefan@waldherr.org>
	%%{_localstatedir}/log/junkbuster set to nobody. Added /etc/junkbuster/imagelist
	to allow more sophisticated matching of blocked images. Logrotate
	logfile. Added files for auto-updating the blocklist et al.

* Wed Dec 16 1998 Stefan Waldherr <stefan@waldherr.org>
	Configure blank version via config file. No separate blank
	version anymore. Added Roland's <roland@spinnaker.rhein.de>
	patch to show a logo instead of a blank area. Added a suggestion
	from Alex <alex@cocoa.demon.co.uk>: %%{_localstatedir}/lock/subsys/junkbuster.
	More regexps in the blocklist. Prepared the forwardfile for
	squid. Extended image regexp with help from gabriel 
	<somlo@CS.ColoState.EDU>.

* Thu Nov 19 1998 Stefan Waldherr <stefan@waldherr.org>
	All RPMs now identify themselves in the show-proxy-args page.
	Released Windoze version. Run junkbuster as nobody instead of
	root. 

* Fri Oct 30 1998 Stefan Waldherr <stefan@waldherr.org>
	Newest version. First release (hence the little version number
	mixture -- 2.0.2-0 instead of 2.0-7). This version tightens 
	security over 2.0.1; some multi-user sites will need to change 
	the listen-address in the configuration file. The blank version of
        the Internet Junkbuster has a more sophisticated way of replacing
	images. All RPMs identify themselves in the show-proxy-args page.

* Thu Sep 23 1998 Stefan Waldherr <stefan@waldherr.org>
	Modified the blocking feature, so that only GIFs and JPEGs are
	blocked and replaced but not HTML pages. Thanks to 
	"Gerd Flender" <plgerd@informatik.uni-siegen.de> for this nice
	idea. Added numerous stuff to the blocklist. Keep patches in
        seperate files and no longer in diffs (easier to maintain).

* Tue Jun 16 1998 Stefan Waldherr <swa@cs.cmu.edu>
        Moved config files to /etc/junkbuster directory, moved man page,
	added BuildRoot directive (Thanks to Alexey Nogin <ayn2@cornell.edu>)
        Made new version junkbuster-raw (which is only a stripped version of 
        the junkuster rpm, i.e. without my blocklist, etc.)

* Tue Jun 16 1998 (2.0-1)
	Uhm, not that much. Just a new junkbuster version that
	fixes a couple of bugs ... and of course a bigger 
	blocklist with the unique Now-less-ads-than-ever(SM)
	feature.
	Oh, one thing: I changed the default user agent to Linux -- no 
	need anymore to support Apple.

* Tue Jun 16 1998 (2.0-0)
	Now-less-ads-than-ever (SM)
	compiled with gcc instead of cc
	compiled with -O3, thus it should be a little faster
	show-proxy-args now works
	/etc/junkbuster.init wasn't necessary

* Tue Jun 16 1998 (1.4)
	some more config files were put into /etc
	The junkbuster-blank rpm returns a 1x1 pixel image, that gets 
	displayed by Netscape instead of the blocked image.
	Read http://www.waldherr.org/junkbuster/ for
	further info.

* Tue Jun 16 1998 (1.3)
	The program has been moved to /usr/sbin (from /usr/local/bin)
	Init- and stopscripts (/etc/rc.d/rc*) have been added so
	that the junkbuster starts automatically during bootup.
	The /etc/blocklist file is much more sophisticated. Theoretically
	one should e.g. browse all major US and German newspapers without
	seeing one annoying ad.
	junkbuster.init was modified. It now starts junkbuster with an
	additional "-r @" flag.

# $Log: privoxy-rh.spec,v $
# Revision 1.63  2009/03/21 10:46:15  fabiankeil
# Bump version to 3.0.12.
#
# Revision 1.62  2009/02/15 17:17:23  fabiankeil
# - Bump version to 3.0.11.
# - List match-all.action as %config file.
#
# Revision 1.61  2009/01/13 16:47:34  fabiankeil
# The standard.action file is gone.
#
# Revision 1.60  2008/08/30 12:46:49  fabiankeil
# The jarfile directive is gone. Update accordingly.
#
# Revision 1.59  2008/08/13 16:57:46  fabiankeil
# Change version to 3.0.10.
#
# Revision 1.58  2008/06/19 01:52:17  hal9
# Remove txt docs from spec file.
#
# Revision 1.57  2008/05/30 15:06:42  fabiankeil
# - Add %config directive for url-info-osd.xml.
#   As usual, this hasn't been tested.
# - Fix comment typo.
#
# Revision 1.56  2008/03/16 14:17:25  fabiankeil
# Add %config lines for regression-tests.action and forwarding-failed.
# This might or might not help with #1915185, reported by Bernardo Bacic.
#
# Revision 1.55  2008/03/02 17:36:43  fabiankeil
# Set version to 3.0.9.
#
# Revision 1.54  2008/01/20 14:30:59  fabiankeil
# Set version to 3.0.8.
#
# Revision 1.53  2006/11/28 11:34:35  hal9
# Fix the prep section per Support request so it actually builds.
#
# Revision 1.52  2006/11/18 17:36:53  hal9
# Ooops, bumping version to 3.0.6
#
# Revision 1.51  2006/11/18 14:37:12  fabiankeil
# Bump version to 3.0.6.
#
# Revision 1.50  2006/09/24 01:19:03  hal9
# Add changes for user-manual directive by nfopd submitted via SF.
#
# Revision 1.49  2006/09/22 01:02:08  hal9
# Fix user.filter installation and CVS files cruft per support request.
#
# Revision 1.48  2006/09/20 23:51:26  hal9
# Bump versions to 3.0.5
#
# Revision 1.47  2006/09/09 00:35:10  hal9
# Bumped versions to 3.0.4. Both files should be checked further.
#
# Revision 1.46  2006/09/02 22:22:59  hal9
# Include user.filter, and do not overwrite trust file on updates.
#
# Revision 1.45  2006/07/18 14:48:47  david__schmidt
# Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
# with what was really the latest development (the v_3_0_branch branch)
#
# Revision 1.33.2.22  2004/01/30 17:09:29  oes
# Bumped version for 3.0.3
#
# Revision 1.33.2.21  2003/03/26 00:25:00  oes
# Bump version for 3.0.2
#
# Revision 1.33.2.20  2003/03/20 03:27:11  hal9
# Bump version for 3.0.1 pending release.
#
# Revision 1.33.2.19  2002/08/25 23:36:03  hal9
# Bump version for 3.0.0.
#
# Revision 1.33.2.18  2002/08/10 11:28:50  oes
# Bumped version
#
# Revision 1.33.2.17  2002/08/07 01:08:49  hal9
# Bumped version to 2.9.18.
#
# Revision 1.33.2.16  2002/08/05 08:42:13  kick_
# same permissions, same runlevels as all the other initscripts
#
# Revision 1.33.2.15  2002/07/30 21:51:19  hal9
# Bump version to 2.9.17.
#
# Revision 1.33.2.14  2002/07/27 21:58:16  kick_
# bump version
#
# Revision 1.33.2.13  2002/07/27 21:39:41  kick_
# condrestart raised an error during an fresh install when privoxy wasn't already running
#
# Revision 1.33.2.12  2002/07/27 15:47:10  hal9
# Reset version and release for 2.9.16.
#
# Revision 1.33.2.11  2002/07/25 09:47:57  kick_
# this caused some errors during a fresh installation. It's unnecessary to call an extra program (/bin/true) to set the error code to 0
#
# Revision 1.33.2.10  2002/07/12 09:14:26  kick_
# don't use ghost files for rcX.d/*, chkconfig is available to do this job. Enable translation of error messge
#
# Revision 1.33.2.9  2002/07/05 17:16:19  morcego
# - Changing delete order for groups and users (users should be first)
#
# Revision 1.33.2.8  2002/07/03 20:46:24  morcego
# - Changing sed expression that removed CR from the end of the lines. This
#   new one removes any control caracter, and should work with older versions
#   of sed
#
# Revision 1.33.2.7  2002/07/02 18:16:48  morcego
# - Fixing defattr values. File and directory modes where swapped
#
# Revision 1.33.2.6  2002/07/02 17:38:10  morcego
# Bumping Release number
#
# Revision 1.33.2.5  2002/07/02 11:43:20  hal9
# Fix typo in templates creation.
#
# Revision 1.33.2.4  2002/06/26 17:32:45  morcego
# Integrating fixed from the main branch.
#
# Revision 1.33.2.3  2002/06/24 12:13:34  kick_
# shut up rpmlint. btw: The vendor tag should be set in you .rpmmacros file, not in the spec file!
#
# Revision 1.33.2.2  2002/05/28 02:39:38  hal9
# Replace index.html with privoxy-index.html for docs.
#
# Revision 1.33.2.1  2002/05/26 17:20:23  hal9
# Add images to doc dirs.
#
# Revision 1.33  2002/05/25 02:08:23  hal9
# Add doc/images directory.
# Redhat: alphabetized list of templates (and I think added one in the process)
#
# Revision 1.32  2002/05/16 01:37:29  hal9
# Add new template file so CGI stuff works :)
#
# Revision 1.31  2002/05/03 17:14:35  morcego
# *.spec: Version bump to 2.9.15
# -rh.spec: noreplace for %%{privoxyconf}/config
#           Will interrupt the build if versions from configure.in and
# 		specfile do not match
#
# Revision 1.30  2002/04/26 15:51:05  morcego
# Changing Vendor value to Privoxy.Org
#
# Revision 1.29  2002/04/24 03:13:51  hal9
# New actions files changes.
#
# Revision 1.28  2002/04/22 18:51:33  morcego
# user and group now get removed on rh too.
#
# Revision 1.27  2002/04/22 16:32:31  morcego
# configure.in, *.spec: Bumping release to 2 (2.9.14-2)
# -rh.spec: uid and gid are now macros
# -suse.spec: Changing the header Copyright to License (Copyright is
#             deprecable)
#
# Revision 1.26  2002/04/22 16:24:36  morcego
# - Changes to fixate the uid and gid values as (both) 73. This is a
#   value we hope to standarize for all distributions. RedHat already
#   uses it, and Conectiva should start as soon as I find where the heck
#   I left my cluebat :-)
# - Only remove the user and group on uninstall if this is not redhat, once
#   redhat likes to have the values allocated even if the package is not
#   installed
#
# Revision 1.25  2002/04/17 01:59:12  hal9
# Add --disable-dynamic-pcre.
#
# Revision 1.24  2002/04/11 10:09:20  oes
# Version 2.9.14
#
# Revision 1.23  2002/04/10 18:14:45  morcego
# - (privoxy-rh.spec only) Relisting template files on the %%files section
# - (configure.in, privoxy-rh.spec) Bumped package release to 5
#
# Revision 1.22  2002/04/09 22:06:12  hal9
# Remove 'make dok'.
#
# Revision 1.21  2002/04/09 02:52:26  hal9
# - Add templates/cgi-style.css, faq.txt, p_web.css, LICENSE
# - Remove templates/blocked-compact.
# - Add more docbook stuff to Buildrequires.
#
# Revision 1.20  2002/04/08 20:27:45  swa
# fixed JB spelling
#
# Revision 1.19  2002/03/27 22:44:59  sarantis
# Include correct documentation file.
#
# Revision 1.18  2002/03/27 22:10:14  sarantis
# bumped Hal's last commit 1 day to the future to make rpm build again.
#
# Revision 1.17  2002/03/27 00:48:23  hal9
# Fix up descrition.
#
# Revision 1.16  2002/03/26 22:29:55  swa
# we have a new homepage!
#
# Revision 1.15  2002/03/26 17:39:54  morcego
# Adding comment on the specfile to remember the packager to update
# the release number on the configure script
#
# Revision 1.14  2002/03/26 14:25:15  hal9
# Added edit-actions-for-url-filter to templates in %%config
#
# Revision 1.13  2002/03/25 13:31:04  morcego
# Bumping Release tag.
#
# Revision 1.12  2002/03/25 03:11:40  hal9
# Do it right way this time :/
#
# Revision 1.11  2002/03/25 03:09:51  hal9
# Added faq to docs.
#
# Revision 1.10  2002/03/24 22:16:14  morcego
# Just removing some old commentaries.
#
# Revision 1.9  2002/03/24 22:03:22  morcego
# Should be working now. See %changelog for details
#
# Revision 1.8  2002/03/24 21:13:01  morcego
# Tis broken.
#
# Revision 1.7  2002/03/24 21:07:18  hal9
# Add autoheader, etc.
#
# Revision 1.6  2002/03/24 19:56:40  hal9
# /etc/junkbuster is now /etc/privoxy. Fixed ';' typo.
#
# Revision 1.4  2002/03/24 13:32:42  swa
# name change related issues
#
# Revision 1.3  2002/03/24 12:56:21  swa
# name change related issues.
#
# Revision 1.2  2002/03/24 11:40:14  swa
# name change
#
# Revision 1.1  2002/03/24 11:23:44  swa
# name change
#
# Revision 1.1  2002/03/22 20:53:03  morcego
# - Ongoing process to change name to JunkbusterNG
# - configure/configure.in: no change needed
# - GNUmakefile.in:
#         - TAR_ARCH = /tmp/JunkbusterNG-$(RPM_VERSION).tar.gz
#         - PROGRAM    = jbng@EXEEXT@
#         - rh-spec now references as junkbusterng-rh.spec
#         - redhat-upload: references changed to junkbusterng-* (package names)
#         - tarball-dist: references changed to JunkbusterNG-distribution-*
#         - tarball-src: now JunkbusterNG-*
#         - install: initscript now junkbusterng.init and junkbusterng (when
#                    installed)
# - junkbuster-rh.spec: renamed to junkbusterng-rh.spec
# - junkbusterng.spec:
#         - References to the expression ijb where changed where possible
#         - New package name: junkbusterng (all in lower case, acording to
#           the LSB recomendation)
#         - Version changed to: 2.9.13
#         - Release: 1
#         - Added: junkbuster to obsoletes and conflicts (Not sure this is
#           right. If it obsoletes, why conflict ? Have to check it later)
#         - Summary changed: Stefan, please check and aprove it
#         - Changes description to use the new name
#         - Sed string was NOT changed. Have to wait to the manpage to
#           change first
#         - Keeping the user junkbuster for now. It will require some aditional
#           changes on the script (scheduled for the next specfile release)
#         - Added post entry to move the old logfile to the new log directory
#         - Removing "chkconfig --add" entry (not good to have it automaticaly
#           added to the startup list).
#         - Added preun section to stop the service with the old name, as well
#           as remove it from the startup list
#         - Removed the chkconfig --del entry from the conditional block on
#           the preun scriptlet (now handled on the %files section)
# - junkbuster.init: renamed to junkbusterng.init
# - junkbusterng.init:
#         - Changed JB_BIN to jbng
#         - Created JB_OBIN with the old value of JB_BIN (junkbuster), to
#           be used where necessary (config dir)
#
# Aditional notes:
# - The config directory is /etc/junkbuster yet. Have to change it on the
# specfile, after it is changes on the code
# - The only files that got renamed on the cvs tree were the rh specfile and
# the init file. Some file references got changes on the makefile and on the
# rh-spec (as listed above)
#
# Revision 1.43  2002/03/21 16:04:10  hal9
# added ijb_docs.css to %doc
#
# Revision 1.42  2002/03/12 13:41:18  sarantis
# remove hard-coded "ijbswa" string in build phase
#
# Revision 1.41  2002/03/11 22:58:32  hal9
# Remove --enable-no-gifs
#
# Revision 1.39  2002/03/08 18:57:29  swa
# remove user junkbuster after de-installation.
#
# Revision 1.38  2002/03/08 13:45:27  morcego
# Adding libtool to Buildrequires
#
# Revision 1.37  2002/03/07 19:23:49  swa
# i hate to scroll. suse: wrong configdir.
#
# Revision 1.36  2002/03/07 05:06:54  morcego
# Fixed %pre scriptlet. And, as a bonus, you can even understand it now. :-)
#
# Revision 1.34  2002/03/07 00:11:57  morcego
# Few changes on the %pre and %post sections of the rh specfile to handle
# usernames more cleanly
#
# Revision 1.33  2002/03/05 13:13:57  morcego
# - Added "make redhat-dok" to the build phase
# - Added docbook-utils to BuildRequires
#
# Revision 1.32  2002/03/05 12:34:24  morcego
# - Changing section internaly on the manpage from 1 to 8
# - We now require packages, not files, to avoid issues with apt
#
# Revision 1.31  2002/03/04 18:06:09  morcego
# SPECFILE: fixing permissing of the init script (broken by the last change)
#
# Revision 1.30  2002/03/04 16:18:03  morcego
# General cleanup of the rh specfile.
#
# %changelog
# * Mon Mar 04 2002 Rodrigo Barbosa <rodrigob@tisbrasil.com.br>
# + junkbuster-2.9.11-2
# - General specfile fixup, using the best recomended practices, including:
#         - Adding -q to %%setup
#         - Using macros whereever possible
#         - Not using wildchars on %%files section
#         - Doubling the percentage char on changelog and comments, to
#           avoid rpm expanding them
#
# Revision 1.29  2002/03/03 19:21:22  hal9
# Init script fails if shell is /bin/false.
#
# Revision 1.28  2002/01/09 18:34:03  hal9
# nit.
#
# Revision 1.27  2002/01/09 18:32:02  hal9
# Removed RPM_OPT_FLAGS kludge.
#
# Revision 1.26  2002/01/09 18:21:10  hal9
# A few minor updates.
#
# Revision 1.25  2001/12/28 01:45:36  steudten
# Add paranoia check and BuildReq: gzip
#
# Revision 1.24  2001/12/01 21:43:14  hal9
# Allowed for new ijb.action file.
#
# Revision 1.23  2001/11/06 12:09:03  steudten
# Compress doc files. Install README and AUTHORS at last as document.
#
# Revision 1.22  2001/11/05 21:37:34  steudten
# Fix to include the actual version for name.
# Let the 'real' packager be included - sorry stefan.
#
# Revision 1.21  2001/10/31 19:27:27  swa
# consistent description. new name for suse since
# we had troubles with rpms of identical names
# on the webserver.
#
# Revision 1.20  2001/10/24 15:45:49  hal9
# To keep Thomas happy (aka correcting my  mistakes)
#
# Revision 1.19  2001/10/15 03:23:59  hal9
# Nits.
#
# Revision 1.17  2001/10/10 18:59:28  hal9
# Minor change for init script.
#
# Revision 1.16  2001/09/24 20:56:23  hal9
# Minor changes.
#
# Revision 1.13  2001/09/10 17:44:43  swa
# integrate three pieces of documentation. needs work.
# will not build cleanly under redhat.
#
# Revision 1.12  2001/09/10 16:25:04  swa
# copy all templates. version updated.
#
# Revision 1.11  2001/07/03 11:00:25  sarantis
# replaced permissionsfile with actionsfile
#
# Revision 1.10  2001/07/03 09:34:44  sarantis
# bumped up version number.
#
# Revision 1.9  2001/06/12 18:15:29  swa
# the %% in front of configure (see tag below) confused
# the rpm build process on 7.1.
#
# Revision 1.8  2001/06/12 17:15:56  swa
# fixes, because a clean build on rh6.1 was impossible.
# GZIP confuses make, %% configure confuses rpm, etc.
#
# Revision 1.7  2001/06/11 12:17:26  sarantis
# fix typo in %%post
#
# Revision 1.6  2001/06/11 11:28:25  sarantis
# Further optimizations and adaptations in the spec file.
#
# Revision 1.5  2001/06/09 09:14:11  swa
# shamelessly adapted RPM stuff from the newest rpm that
# RedHat provided for the JB.
#
# Revision 1.4  2001/06/08 20:54:18  swa
# type with status file. remove forward et. al from file list.
#
# Revision 1.3  2001/06/07 17:28:10  swa
# cosmetics
#
# Revision 1.2  2001/06/04 18:31:58  swa
# files are now prefixed with either `confdir' or `logdir'.
# `make redhat-dist' replaces both entries confdir and logdir
# with redhat values
#
# Revision 1.1  2001/06/04 10:44:57  swa
# `make redhatr-dist' now works. Except for the paths
# in the config file.
#
#
#
