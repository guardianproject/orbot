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
 * Copyright   :  Written by and Copyright (C) 2001-2008 the
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
#ifdef FEATURE_FAST_REDIRECTS
DEFINE_ACTION_STRING     ("fast-redirects",             ACTION_FAST_REDIRECTS,  ACTION_STRING_FAST_REDIRECTS)
DEFINE_CGI_PARAM_RADIO   ("fast-redirects",             ACTION_FAST_REDIRECTS,  ACTION_STRING_FAST_REDIRECTS, "simple-check",  0)
DEFINE_CGI_PARAM_RADIO   ("fast-redirects",             ACTION_FAST_REDIRECTS,  ACTION_STRING_FAST_REDIRECTS, "check-decoded-url",  1)
#endif /* def FEATURE_FAST_REDIRECTS */
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

