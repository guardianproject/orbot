/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/actionlist.h,v $
 *
 * Purpose     :  Master list of supported actions.
 *                Not really a header, since it generates code.
 *                This is included (3 times!) from actions.c
 *                Each time, the following macros are defined to
 *                suitable values beforehand:
 *                    DEFINE_ACTION_MULTI()
 *                    DEFINE_ACTION_STRING()
 *                    DEFINE_ACTION_BOOL()
 *                    DEFINE_ACTION_ALIAS
 *
 * Copyright   :  Written by and Copyright (C) 2001-2008 the SourceForge
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
 * Revisions   :
 *    $Log: actionlist.h,v $
 *    Revision 1.36  2008/09/20 10:04:33  fabiankeil
 *    Remove hide-forwarded-for-headers action which has
 *    been obsoleted by change-x-forwarded-for{block}.
 *
 *    Revision 1.35  2008/09/19 15:43:54  fabiankeil
 *    Fix sorting.
 *
 *    Revision 1.34  2008/09/19 15:26:28  fabiankeil
 *    Add change-x-forwarded-for{} action to block or add
 *    X-Forwarded-For headers. Mostly based on code removed
 *    before 3.0.7.
 *
 *    Revision 1.33  2008/03/29 12:13:45  fabiankeil
 *    Remove send-wafer and send-vanilla-wafer actions.
 *
 *    Revision 1.32  2008/03/28 15:13:42  fabiankeil
 *    Remove inspect-jpegs action.
 *
 *    Revision 1.31  2008/03/27 18:27:20  fabiankeil
 *    Remove kill-popups action.
 *
 *    Revision 1.30  2008/03/04 18:30:34  fabiankeil
 *    Remove the treat-forbidden-connects-like-blocks action. We now
 *    use the "blocked" page for forbidden CONNECT requests by default.
 *
 *    Revision 1.29  2008/03/01 14:00:43  fabiankeil
 *    Let the block action take the reason for the block
 *    as argument and show it on the "blocked" page.
 *
 *    Revision 1.28  2007/12/11 21:08:29  fabiankeil
 *    Let the CGI editor suggest a forward-override
 *    parameter whose syntax is actually valid.
 *
 *    Revision 1.27  2007/11/10 15:04:08  fabiankeil
 *    Tell the CGI editor about +hide-referrer{conditional-forge}.
 *
 *    Revision 1.26  2007/06/01 16:54:28  fabiankeil
 *    Add forward-override{} to change the forwarding settings through
 *    action sections. This is mainly interesting to forward different
 *    clients differently (for example based on User-Agent or request
 *    origin).
 *
 *    Revision 1.25  2007/04/15 16:39:20  fabiankeil
 *    Introduce tags as alternative way to specify which
 *    actions apply to a request. At the moment tags can be
 *    created based on client and server headers.
 *
 *    Revision 1.24  2007/03/20 15:16:34  fabiankeil
 *    Use dedicated header filter actions instead of abusing "filter".
 *    Replace "filter-client-headers" and "filter-client-headers"
 *    with "server-header-filter" and "client-header-filter".
 *
 *    Revision 1.23  2006/10/09 10:26:18  fabiankeil
 *    Changed the path in set-image-blocker's redirection default to
 *    "send-banner?type=pattern" instead of "show-banner?type=pattern"
 *    which isn't caught by Privoxy. Fixes BR 1573468.
 *
 *    Changed hide-user-agent's default value to "Privoxy VERSION".
 *
 *    Changed hide-referrer's default fake value to "http://www.privoxy.org/".
 *    A static referrer is obviously fake anyway, so we might as well
 *    advertise ourselves.
 *
 *    Revision 1.22  2006/09/01 17:14:18  hal9
 *    Re-ordered the actions list so that they display in the actions editor in
 *    alphabetical order. Some of the new actions were "out of order".
 *
 *    Revision 1.21  2006/08/14 08:25:19  fabiankeil
 *    Split filter-headers{} into filter-client-headers{}
 *    and filter-server-headers{}.
 *    Added parse_header_time() to share some code.
 *    Replaced timegm() with mktime().
 *
 *    Revision 1.20  2006/08/03 02:46:41  david__schmidt
 *    Incorporate Fabian Keil's patch work:http://www.fabiankeil.de/sourcecode/privoxy/
 *
 *    Revision 1.19  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.17.2.3  2004/10/03 12:53:32  david__schmidt
 *    Add the ability to check jpeg images for invalid
 *    lengths of comment blocks.  Defensive strategy
 *    against the exploit:
 *       Microsoft Security Bulletin MS04-028
 *       Buffer Overrun in JPEG Processing (GDI+) Could
 *       Allow Code Execution (833987)
 *    Enabled with +inspect-jpegs in actions files.
 *
 *    Revision 1.17.2.2  2002/09/25 15:25:25  oes
 *    Added more aliases for prehistoric action names
 *
 *    Revision 1.17.2.1  2002/08/02 12:50:47  oes
 *    Consistency with docs: Change default name for action from hide-referer to hide-referrer
 *
 *    Revision 1.17  2002/05/14 21:25:55  oes
 *    Renamed prevent-(setting/reading)-cookies to crunch-(incoming/outgoing)-cookies
 *
 *    Revision 1.16  2002/04/24 02:15:18  oes
 *    Renamed actions as discussed, Aliased old action names to new ones.
 *
 *    Revision 1.15  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.14  2002/03/24 16:32:08  jongfoster
 *    Removing logo option
 *
 *    Revision 1.13  2002/03/24 15:23:33  jongfoster
 *    Name changes
 *
 *    Revision 1.12  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.11  2002/03/12 01:42:49  oes
 *    Introduced modular filters
 *
 *    Revision 1.10  2002/03/08 18:19:14  jongfoster
 *    Adding +image-blocker{pattern} option to edit interface
 *
 *    Revision 1.9  2001/11/22 21:58:41  jongfoster
 *    Adding action +no-cookies-keep
 *
 *    Revision 1.8  2001/10/10 16:42:52  oes
 *    Fixed a bug, Added +limit-connect string action
 *
 *    Revision 1.7  2001/10/07 15:33:59  oes
 *    Introduced a +no-compression action
 *    Introduced a +downgrade action
 *
 *    Revision 1.6  2001/09/16 15:47:37  jongfoster
 *    First version of CGI-based edit interface.  This is very much a
 *    work-in-progress, and you can't actually use it to edit anything
 *    yet.  You must #define FEATURE_CGI_EDIT_ACTIONS for these changes
 *    to have any effect.
 *
 *    Revision 1.5  2001/07/18 12:27:03  oes
 *    Changed deanimate-gifs to string action
 *
 *    Revision 1.4  2001/07/13 13:52:12  oes
 *     - Formatting
 *     - Introduced new action ACTION_DEANIMATE
 *
 *    Revision 1.3  2001/06/07 23:03:56  jongfoster
 *    Added standard comment at top of file.
 *
 *
 *********************************************************************/


#if !(defined(DEFINE_ACTION_BOOL) && defined(DEFINE_ACTION_MULTI) && defined(DEFINE_ACTION_STRING))
#error Please define lots of macros before including "actionlist.h".
#endif /* !defined(all the DEFINE_ACTION_xxx macros) */

#ifndef DEFINE_CGI_PARAM_RADIO
#define DEFINE_CGI_PARAM_RADIO(name, bit, index, value, is_default)
#define DEFINE_CGI_PARAM_CUSTOM(name, bit, index, default_val)
#define DEFINE_CGI_PARAM_NO_RADIO(name, bit, index, default_val)
#endif /* ndef DEFINE_CGI_PARAM_RADIO */

DEFINE_ACTION_MULTI      ("add-header",                 ACTION_MULTI_ADD_HEADER)
DEFINE_ACTION_STRING     ("block",                      ACTION_BLOCK, ACTION_STRING_BLOCK)
DEFINE_CGI_PARAM_NO_RADIO("block",                      ACTION_BLOCK, ACTION_STRING_BLOCK, "No reason specified.")
DEFINE_ACTION_STRING     ("change-x-forwarded-for",     ACTION_CHANGE_X_FORWARDED_FOR,  ACTION_STRING_CHANGE_X_FORWARDED_FOR)
DEFINE_CGI_PARAM_RADIO   ("change-x-forwarded-for",     ACTION_CHANGE_X_FORWARDED_FOR,  ACTION_STRING_CHANGE_X_FORWARDED_FOR, "block", 0)
DEFINE_CGI_PARAM_RADIO   ("change-x-forwarded-for",     ACTION_CHANGE_X_FORWARDED_FOR,  ACTION_STRING_CHANGE_X_FORWARDED_FOR, "add", 1)
DEFINE_ACTION_MULTI      ("client-header-filter",       ACTION_MULTI_CLIENT_HEADER_FILTER)
DEFINE_ACTION_MULTI      ("client-header-tagger",       ACTION_MULTI_CLIENT_HEADER_TAGGER)
DEFINE_ACTION_STRING     ("content-type-overwrite",     ACTION_CONTENT_TYPE_OVERWRITE, ACTION_STRING_CONTENT_TYPE)
DEFINE_CGI_PARAM_NO_RADIO("content-type-overwrite",     ACTION_CONTENT_TYPE_OVERWRITE, ACTION_STRING_CONTENT_TYPE,    "text/html")
DEFINE_ACTION_STRING     ("crunch-client-header",       ACTION_CRUNCH_CLIENT_HEADER, ACTION_STRING_CLIENT_HEADER)
DEFINE_CGI_PARAM_NO_RADIO("crunch-client-header",       ACTION_CRUNCH_CLIENT_HEADER, ACTION_STRING_CLIENT_HEADER,          "X-Whatever:")
DEFINE_ACTION_BOOL       ("crunch-if-none-match",       ACTION_CRUNCH_IF_NONE_MATCH)
DEFINE_ACTION_BOOL       ("crunch-incoming-cookies",    ACTION_NO_COOKIE_SET)
DEFINE_ACTION_BOOL       ("crunch-outgoing-cookies",    ACTION_NO_COOKIE_READ)
DEFINE_ACTION_STRING     ("crunch-server-header",       ACTION_CRUNCH_SERVER_HEADER, ACTION_STRING_SERVER_HEADER)
DEFINE_CGI_PARAM_NO_RADIO("crunch-server-header",       ACTION_CRUNCH_SERVER_HEADER, ACTION_STRING_SERVER_HEADER,          "X-Whatever:")
DEFINE_ACTION_STRING     ("deanimate-gifs",             ACTION_DEANIMATE,       ACTION_STRING_DEANIMATE)
DEFINE_CGI_PARAM_RADIO   ("deanimate-gifs",             ACTION_DEANIMATE,       ACTION_STRING_DEANIMATE,     "first", 0)
DEFINE_CGI_PARAM_RADIO   ("deanimate-gifs",             ACTION_DEANIMATE,       ACTION_STRING_DEANIMATE,     "last",  1)
DEFINE_ACTION_BOOL       ("downgrade-http-version",     ACTION_DOWNGRADE)
DEFINE_ACTION_STRING     ("fast-redirects",             ACTION_FAST_REDIRECTS,  ACTION_STRING_FAST_REDIRECTS)
DEFINE_CGI_PARAM_RADIO   ("fast-redirects",             ACTION_FAST_REDIRECTS,  ACTION_STRING_FAST_REDIRECTS, "simple-check",  0)
DEFINE_CGI_PARAM_RADIO   ("fast-redirects",             ACTION_FAST_REDIRECTS,  ACTION_STRING_FAST_REDIRECTS, "check-decoded-url",  1)
DEFINE_ACTION_MULTI      ("filter",                     ACTION_MULTI_FILTER)
DEFINE_ACTION_BOOL       ("force-text-mode",            ACTION_FORCE_TEXT_MODE)
DEFINE_ACTION_STRING     ("forward-override",           ACTION_FORWARD_OVERRIDE, ACTION_STRING_FORWARD_OVERRIDE)
DEFINE_CGI_PARAM_CUSTOM  ("forward-override",           ACTION_FORWARD_OVERRIDE, ACTION_STRING_FORWARD_OVERRIDE, "forward .")
DEFINE_ACTION_BOOL       ("handle-as-empty-document",   ACTION_HANDLE_AS_EMPTY_DOCUMENT)
DEFINE_ACTION_BOOL       ("handle-as-image",            ACTION_IMAGE)
DEFINE_ACTION_STRING     ("hide-accept-language",       ACTION_HIDE_ACCEPT_LANGUAGE, ACTION_STRING_LANGUAGE)
DEFINE_CGI_PARAM_RADIO   ("hide-accept-language",       ACTION_HIDE_ACCEPT_LANGUAGE, ACTION_STRING_LANGUAGE, "block", 0)
DEFINE_CGI_PARAM_CUSTOM  ("hide-accept-language",       ACTION_HIDE_ACCEPT_LANGUAGE, ACTION_STRING_LANGUAGE, "de-de")
DEFINE_ACTION_STRING     ("hide-content-disposition",   ACTION_HIDE_CONTENT_DISPOSITION, ACTION_STRING_CONTENT_DISPOSITION)
DEFINE_CGI_PARAM_RADIO   ("hide-content-disposition",   ACTION_HIDE_CONTENT_DISPOSITION, ACTION_STRING_CONTENT_DISPOSITION,    "block", 0)
DEFINE_CGI_PARAM_CUSTOM  ("hide-content-disposition",   ACTION_HIDE_CONTENT_DISPOSITION, ACTION_STRING_CONTENT_DISPOSITION,    "attachment; filename=WHATEVER.txt")
DEFINE_ACTION_STRING     ("hide-from-header",           ACTION_HIDE_FROM,       ACTION_STRING_FROM)
DEFINE_CGI_PARAM_RADIO   ("hide-from-header",           ACTION_HIDE_FROM,       ACTION_STRING_FROM,          "block", 1)
DEFINE_CGI_PARAM_CUSTOM  ("hide-from-header",           ACTION_HIDE_FROM,       ACTION_STRING_FROM,          "spam_me_senseless@sittingduck.xyz")
DEFINE_ACTION_STRING     ("hide-if-modified-since",     ACTION_HIDE_IF_MODIFIED_SINCE, ACTION_STRING_IF_MODIFIED_SINCE)
DEFINE_CGI_PARAM_RADIO   ("hide-if-modified-since",     ACTION_HIDE_IF_MODIFIED_SINCE, ACTION_STRING_IF_MODIFIED_SINCE, "block", 0)
DEFINE_CGI_PARAM_CUSTOM  ("hide-if-modified-since",     ACTION_HIDE_IF_MODIFIED_SINCE, ACTION_STRING_IF_MODIFIED_SINCE, "-1")
DEFINE_ACTION_STRING     ("hide-referrer",              ACTION_HIDE_REFERER,    ACTION_STRING_REFERER)
DEFINE_CGI_PARAM_RADIO   ("hide-referrer",              ACTION_HIDE_REFERER,    ACTION_STRING_REFERER,       "conditional-forge", 3)
DEFINE_CGI_PARAM_RADIO   ("hide-referrer",              ACTION_HIDE_REFERER,    ACTION_STRING_REFERER,       "conditional-block", 2)
DEFINE_CGI_PARAM_RADIO   ("hide-referrer",              ACTION_HIDE_REFERER,    ACTION_STRING_REFERER,       "forge", 1)
DEFINE_CGI_PARAM_RADIO   ("hide-referrer",              ACTION_HIDE_REFERER,    ACTION_STRING_REFERER,       "block", 0)
DEFINE_CGI_PARAM_CUSTOM  ("hide-referrer",              ACTION_HIDE_REFERER,    ACTION_STRING_REFERER,       "http://www.privoxy.org/")
DEFINE_ACTION_STRING     ("hide-user-agent",            ACTION_HIDE_USER_AGENT, ACTION_STRING_USER_AGENT)
DEFINE_CGI_PARAM_NO_RADIO("hide-user-agent",            ACTION_HIDE_USER_AGENT, ACTION_STRING_USER_AGENT,    "Privoxy " VERSION)
DEFINE_ACTION_STRING     ("limit-connect",              ACTION_LIMIT_CONNECT,   ACTION_STRING_LIMIT_CONNECT)
DEFINE_CGI_PARAM_NO_RADIO("limit-connect",              ACTION_LIMIT_CONNECT,   ACTION_STRING_LIMIT_CONNECT,  "443")
DEFINE_ACTION_STRING     ("overwrite-last-modified",    ACTION_OVERWRITE_LAST_MODIFIED, ACTION_STRING_LAST_MODIFIED)
DEFINE_CGI_PARAM_RADIO   ("overwrite-last-modified",    ACTION_OVERWRITE_LAST_MODIFIED, ACTION_STRING_LAST_MODIFIED, "block", 0)
DEFINE_CGI_PARAM_RADIO   ("overwrite-last-modified",    ACTION_OVERWRITE_LAST_MODIFIED, ACTION_STRING_LAST_MODIFIED, "reset-to-request-time", 1)
DEFINE_CGI_PARAM_RADIO   ("overwrite-last-modified",    ACTION_OVERWRITE_LAST_MODIFIED, ACTION_STRING_LAST_MODIFIED, "randomize", 2)
DEFINE_ACTION_BOOL       ("prevent-compression",        ACTION_NO_COMPRESSION)
DEFINE_ACTION_STRING     ("redirect",                   ACTION_REDIRECT,        ACTION_STRING_REDIRECT)
DEFINE_CGI_PARAM_NO_RADIO("redirect",                   ACTION_REDIRECT,        ACTION_STRING_REDIRECT,  "http://localhost/")
DEFINE_ACTION_MULTI      ("server-header-filter",       ACTION_MULTI_SERVER_HEADER_FILTER)
DEFINE_ACTION_MULTI      ("server-header-tagger",       ACTION_MULTI_SERVER_HEADER_TAGGER)
DEFINE_ACTION_BOOL       ("session-cookies-only",       ACTION_NO_COOKIE_KEEP)
DEFINE_ACTION_STRING     ("set-image-blocker",          ACTION_IMAGE_BLOCKER,   ACTION_STRING_IMAGE_BLOCKER)
DEFINE_CGI_PARAM_RADIO   ("set-image-blocker",          ACTION_IMAGE_BLOCKER,   ACTION_STRING_IMAGE_BLOCKER, "pattern", 1)
DEFINE_CGI_PARAM_RADIO   ("set-image-blocker",          ACTION_IMAGE_BLOCKER,   ACTION_STRING_IMAGE_BLOCKER, "blank", 0)
DEFINE_CGI_PARAM_CUSTOM  ("set-image-blocker",          ACTION_IMAGE_BLOCKER,   ACTION_STRING_IMAGE_BLOCKER,  CGI_PREFIX "send-banner?type=pattern")

#if DEFINE_ACTION_ALIAS

/* 
 * Alternative spellings
 */
DEFINE_ACTION_STRING     ("hide-referer",   ACTION_HIDE_REFERER,    ACTION_STRING_REFERER)
DEFINE_ACTION_BOOL       ("prevent-keeping-cookies", ACTION_NO_COOKIE_KEEP)

/* 
 * Pre-3.0.7 (pseudo) compatibility
 */
DEFINE_ACTION_MULTI      ("filter-client-headers",       ACTION_MULTI_CLIENT_HEADER_FILTER)
DEFINE_ACTION_MULTI      ("filter-server-headers",       ACTION_MULTI_SERVER_HEADER_FILTER)

/* 
 * Pre-3.0 compatibility
 */
DEFINE_ACTION_BOOL       ("no-cookie-read",          ACTION_NO_COOKIE_READ)
DEFINE_ACTION_BOOL       ("no-cookie-set",           ACTION_NO_COOKIE_SET)
DEFINE_ACTION_BOOL       ("prevent-reading-cookies", ACTION_NO_COOKIE_READ)
DEFINE_ACTION_BOOL       ("prevent-setting-cookies", ACTION_NO_COOKIE_SET)
DEFINE_ACTION_BOOL       ("downgrade",               ACTION_DOWNGRADE)
DEFINE_ACTION_STRING     ("hide-from",               ACTION_HIDE_FROM,       ACTION_STRING_FROM)
DEFINE_ACTION_BOOL       ("image",                   ACTION_IMAGE)
DEFINE_ACTION_STRING     ("image-blocker",           ACTION_IMAGE_BLOCKER,   ACTION_STRING_IMAGE_BLOCKER)
DEFINE_ACTION_BOOL       ("no-compression",          ACTION_NO_COMPRESSION)
DEFINE_ACTION_BOOL       ("no-cookies-keep",         ACTION_NO_COOKIE_KEEP)
DEFINE_ACTION_BOOL       ("no-cookies-read",         ACTION_NO_COOKIE_READ)
DEFINE_ACTION_BOOL       ("no-cookies-set",          ACTION_NO_COOKIE_SET)
#endif /* if DEFINE_ACTION_ALIAS */

#undef DEFINE_ACTION_MULTI
#undef DEFINE_ACTION_STRING
#undef DEFINE_ACTION_BOOL
#undef DEFINE_ACTION_ALIAS
#undef DEFINE_CGI_PARAM_CUSTOM
#undef DEFINE_CGI_PARAM_RADIO
#undef DEFINE_CGI_PARAM_NO_RADIO

