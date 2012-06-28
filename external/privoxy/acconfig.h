#ifndef CONFIG_H_INCLUDED
#define CONFIG_H_INCLUDED
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/acconfig.h,v $
 *
 * Purpose     :  This file should be the first thing included in every
 *                .c file.  (Before even system headers).  It contains
 *                #define statements for various features.  It was
 *                introduced because the compile command line started
 *                getting ludicrously long with feature defines.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2009 the
 *                Privoxy team. http://www.privoxy.org/
 *
 *                Based on the Internet Junkbuster originally written
 *                by and Copyright (C) 1997 Anonymous Coders and
 *                Junkbusters Corporation.  http://www.junkbusters.com
 *
 *                This program is free software; you can redistribute it
 *                and/or modify it under the terms of the GNU General
 *                Public License as published by the Free Software
 *                Foundation; either version 2 of the License, or (at
 *                your option) any later version.
 *
 *                This program is distributed in the hope that it will
 *                be useful, but WITHOUT ANY WARRANTY; without even the
 *                implied warranty of MERCHANTABILITY or FITNESS FOR A
 *                PARTICULAR PURPOSE.  See the GNU General Public
 *                License for more details.
 *
 *                The GNU General Public License should be included with
 *                this file.  If not, you can view it at
 *                http://www.gnu.org/copyleft/gpl.html
 *                or write to the Free Software Foundation, Inc., 59
 *                Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 *********************************************************************/

@TOP@

/*
 * Version number - Major (X._._)
 */
#undef VERSION_MAJOR

/*
 * Version number - Minor (_.X._)
 */
#undef VERSION_MINOR

/*
 * Version number - Point (_._.X)
 */
#undef VERSION_POINT

/*
 * Version number, as a string
 */
#undef VERSION

/*
 * Status of the code: "alpha", "beta" or "stable".
 */
#undef CODE_STATUS

/*
 * Should pcre be statically built in instead of linkling with libpcre?
 * (This is determined by configure depending on the availiability of
 * libpcre and user preferences). The name is ugly, but pcre needs it.
 * Don't bother to change this here! Use configure instead.
 */
#undef STATIC_PCRE

/*
 * Should pcrs be statically built in instead of linkling with libpcrs?
 * (This is determined by configure depending on the availiability of
 * libpcrs and user preferences).
 * Don't bother to change this here! Use configure instead.
 */
#undef STATIC_PCRS

/*
 * Allows the use of an ACL to control access to the proxy by IP address.
 */
#undef FEATURE_ACL

/*
 * Allow Privoxy to use accf_http(9) if supported.
 */
#undef FEATURE_ACCEPT_FILTER

/*
 * Enables the web-based configuration (actionsfile) editor.  If you
 * have a shared proxy, you might want to turn this off.
 */
#undef FEATURE_CGI_EDIT_ACTIONS

/*
 * Allows the use of jar files to capture cookies.
 */
#undef FEATURE_COOKIE_JAR

/*
 * Locally redirect remote script-redirect URLs
 */
#undef FEATURE_FAST_REDIRECTS

/*
 * Bypass filtering for 1 page only
 */
#undef FEATURE_FORCE_LOAD

/*
 * Allow blocking using images as well as HTML.
 * If you do not define this then everything is blocked as HTML.
 *
 * Note that this is required if you want to use FEATURE_IMAGE_DETECT_MSIE.
 */
#undef FEATURE_IMAGE_BLOCKING

/*
 * Detect image requests automatically for MSIE.  Will fall back to
 * other image-detection methods (i.e. "+image" permission) for other
 * browsers.
 *
 * You must also define FEATURE_IMAGE_BLOCKING to use this feature.
 *
 * It detects the following header pair as an image request:
 *
 * User-Agent: Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0)
 * Accept: * / *
 *
 * And the following as a HTML request:
 *
 * User-Agent: Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0)
 * Accept: image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, * / *
 *
 * And no, I haven't got that backwards - IE is being weird.
 *
 * Known limitations:
 * 1) If you press shift-reload on a blocked HTML page, you get
 *    the image "blocked" page, not the HTML "blocked" page.
 * 2) Once an image "blocked" page has been sent, viewing it
 *    in it's own browser window *should* bring up the HTML
 *    "blocked" page, but it doesn't.  You need to clear the
 *    browser cache to get the HTML version again.
 *
 * These limitations are due to IE making inconsistent choices
 * about which "Accept:" header to send.
 */
#undef FEATURE_IMAGE_DETECT_MSIE

/*
 * Use PNG instead of GIF for built-in images
 */
#undef FEATURE_NO_GIFS

/*
 * Allow to shutdown Privoxy through the webinterface.
 */
#undef FEATURE_GRACEFUL_TERMINATION

/*
 * Allow PCRE syntax in host patterns.
 */
#undef FEATURE_EXTENDED_HOST_PATTERNS

/*
 * Keep connections alive if possible.
 */
#undef FEATURE_CONNECTION_KEEP_ALIVE

/*
 * Allow to share outgoing connections between incoming connections.
 */
#undef FEATURE_CONNECTION_SHARING

/*
 * Use POSIX threads instead of native threads.
 */
#undef FEATURE_PTHREAD

/*
 * Enables statistics function.
 */
#undef FEATURE_STATISTICS

/*
 * Allow Privoxy to be "disabled" so it is just a normal non-blocking
 * non-anonymizing proxy.  This is useful if you're trying to access a
 * blocked or broken site - just change the setting in the config file,
 * or use the handy "Disable" menu option in the Windows GUI.
 */
#undef FEATURE_TOGGLE

/*
 * Allows the use of trust files.
 */
#undef FEATURE_TRUST

/*
 * Defined on Solaris only.  Makes the system libraries thread safe.
 */
#undef _REENTRANT

/*
 * Defined on Solaris only.  Without this, many important functions are not
 * defined in the system headers.
 */
#undef __EXTENSIONS__

/*
 * Defined always.
 * FIXME: Don't know what it does or why we need it.
 * (presumably something to do with MultiThreading?)
 */
#undef __MT__

/* If the (nonstandard and thread-safe) function gethostbyname_r
 * is available, select which signature to use
 */
#undef HAVE_GETHOSTBYNAME_R_6_ARGS
#undef HAVE_GETHOSTBYNAME_R_5_ARGS
#undef HAVE_GETHOSTBYNAME_R_3_ARGS

/* If the (nonstandard and thread-safe) function gethostbyaddr_r
 * is available, select which signature to use
 */
#undef HAVE_GETHOSTBYADDR_R_8_ARGS
#undef HAVE_GETHOSTBYADDR_R_7_ARGS
#undef HAVE_GETHOSTBYADDR_R_5_ARGS

/* Defined if you have gmtime_r and localtime_r with a signature
 * of (struct time *, struct tm *)
 */
#undef HAVE_GMTIME_R
#undef HAVE_LOCALTIME_R

/* Define to 'int' if <sys/socket.h> doesn't have it.
 */
#undef socklen_t

/* Define if pcre.h must be included as <pcre/pcre.h>
 */
#undef PCRE_H_IN_SUBDIR

/* Define if pcreposix.h must be included as <pcre/pcreposix.h>
 */
#undef PCREPOSIX_H_IN_SUBDIR

@BOTTOM@

/*
 * Defined always.
 * FIXME: Don't know what it does or why we need it.
 * (presumably something to do with ANSI Standard C?)
 */
#ifndef __STDC__
#define __STDC__ 1
#endif /* ndef __STDC__ */

/*
 * Need to set up this define only for the Pthreads library for
 * Win32, available from http://sources.redhat.com/pthreads-win32/
 */
#if defined(FEATURE_PTHREAD) && defined(_WIN32)
#define __CLEANUP_C
#endif /* defined(FEATURE_PTHREAD) && defined(_WIN32) */

/*
 * BEOS does not currently support POSIX threads.
 * This *should* be detected by ./configure, but let's be sure.
 */
#if defined(FEATURE_PTHREAD) && defined(__BEOS__)
#error BEOS does not support pthread - please run ./configure again with "--disable-pthread"

#endif /* defined(FEATURE_PTHREAD) && defined(__BEOS__) */

/*
 * On OpenBSD and maybe also FreeBSD, gcc doesn't define the cpp
 * symbol unix; it defines __unix__ and sometimes not even that:
 */
#if ( defined(__unix__) || defined(__NetBSD__) ) && !defined(unix)
#define unix 1
#endif

/*
 * It's too easy to accidentally use a Cygwin or MinGW32 version of config.h
 * under VC++, and it usually gives many weird error messages.  Let's make
 * the error messages understandable, by bailing out now.
 */
#ifdef _MSC_VER
#error For MS VC++, please use vc_config_winthreads.h or vc_config_pthreads.h.  You can usually do this by selecting the "Build", "Clean" menu option.
#endif /* def _MSC_VER */

#endif /* CONFIG_H_INCLUDED */
