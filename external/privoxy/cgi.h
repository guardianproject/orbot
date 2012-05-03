#ifndef CGI_H_INCLUDED
#define CGI_H_INCLUDED
#define CGI_H_VERSION "$Id: cgi.h,v 1.35 2008/05/21 15:24:37 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/cgi.h,v $
 *
 * Purpose     :  Declares functions to intercept request, generate
 *                html or gif answers, and to compose HTTP resonses.
 *                
 *                Functions declared include:
 * 
 *
 * Copyright   :  Written by and Copyright (C) 2001-2007 the SourceForge
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
 *    $Log: cgi.h,v $
 *    Revision 1.35  2008/05/21 15:24:37  fabiankeil
 *    Mark csp as immutable for a bunch of functions.
 *
 *    Revision 1.34  2008/04/17 14:40:48  fabiankeil
 *    Provide get_http_time() with the buffer size so it doesn't
 *    have to blindly assume that the buffer is big enough.
 *
 *    Revision 1.33  2007/01/28 13:41:17  fabiankeil
 *    - Add HEAD support to finish_http_response.
 *    - Add error favicon to internal HTML error messages.
 *
 *    Revision 1.32  2006/12/17 17:53:39  fabiankeil
 *    Suppress the toggle link if remote toggling is disabled.
 *
 *    Revision 1.31  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.29.2.2  2004/02/17 13:30:23  oes
 *    Moved cgi_error_disabled() from cgiedit.c to
 *    cgi.c to re-enable build with --disable-editor.
 *    Fixes Bug #892744. Thanks to Matthew Fischer
 *    for spotting.
 *
 *    Revision 1.29.2.1  2003/12/17 16:33:28  oes
 *    Added prototype of new function cgi_redirect
 *
 *    Revision 1.29  2002/05/19 11:33:21  jongfoster
 *    If a CGI error was not handled, and propogated back to
 *    dispatch_known_cgi(), then it was assumed to be "out of memory".
 *    This gave a very misleading error message.
 *
 *    Now other errors will cause a simple message giving the error
 *    number and asking the user to report a bug.
 *
 *    Bug report:
 *    http://sourceforge.net/tracker/index.php?func=detail
 *    &aid=557905&group_id=11118&atid=111118
 *
 *    Revision 1.28  2002/04/26 12:54:03  oes
 *    New function add_help_link
 *
 *    Revision 1.27  2002/04/24 02:16:51  oes
 *    Moved get_char_param, get_string_param and get_number_param here from cgiedit.c
 *
 *    Revision 1.26  2002/04/10 13:38:35  oes
 *    load_template signature changed
 *
 *    Revision 1.25  2002/04/08 20:50:25  swa
 *    fixed JB spelling
 *
 *    Revision 1.24  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.23  2002/03/24 16:18:15  jongfoster
 *    Removing old logo
 *
 *    Revision 1.22  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.21  2002/03/07 03:48:38  oes
 *     - Changed built-in images from GIF to PNG
 *       (with regard to Unisys patent issue)
 *     - Added a 4x4 pattern PNG which is less intrusive
 *       than the logo but also clearly marks the deleted banners
 *
 *    Revision 1.20  2002/03/04 17:53:22  oes
 *    Fixed compiled warning
 *
 *    Revision 1.19  2002/01/21 00:33:52  jongfoster
 *    Adding map_block_keep() to save a few bytes in the edit-actions-list HTML.
 *
 *    Revision 1.18  2001/11/16 00:46:31  jongfoster
 *    Fixing compiler warnings
 *
 *    Revision 1.17  2001/10/23 21:48:19  jongfoster
 *    Cleaning up error handling in CGI functions - they now send back
 *    a HTML error page and should never cause a FATAL error.  (Fixes one
 *    potential source of "denial of service" attacks).
 *
 *    CGI actions file editor that works and is actually useful.
 *
 *    Ability to toggle Junkbuster remotely using a CGI call.
 *
 *    You can turn off both the above features in the main configuration
 *    file, e.g. if you are running a multi-user proxy.
 *
 *    Revision 1.16  2001/09/16 17:08:54  jongfoster
 *    Moving simple CGI functions from cgi.c to new file cgisimple.c
 *
 *    Revision 1.15  2001/09/16 15:02:35  jongfoster
 *    Adding i.j.b/robots.txt.
 *    Inlining add_stats() since it's only ever called from one place.
 *
 *    Revision 1.14  2001/09/16 11:38:02  jongfoster
 *    Splitting fill_template() into 2 functions:
 *    template_load() loads the file
 *    template_fill() performs the PCRS regexps.
 *    This is because the CGI edit interface has a "table row"
 *    template which is used many times in the page - this
 *    change means it's only loaded from disk once.
 *
 *    Revision 1.13  2001/09/16 11:00:10  jongfoster
 *    New function alloc_http_response, for symmetry with free_http_response
 *
 *    Revision 1.12  2001/09/13 23:31:25  jongfoster
 *    Moving image data to cgi.c rather than cgi.h.
 *
 *    Revision 1.11  2001/08/05 16:06:20  jongfoster
 *    Modifiying "struct map" so that there are now separate header and
 *    "map_entry" structures.  This means that functions which modify a
 *    map no longer need to return a pointer to the modified map.
 *    Also, it no longer reverses the order of the entries (which may be
 *    important with some advanced template substitutions).
 *
 *    Revision 1.10  2001/08/01 21:19:22  jongfoster
 *    Moving file version information to a separate CGI page.
 *
 *    Revision 1.9  2001/08/01 00:17:54  jongfoster
 *    Adding prototype for map_conditional
 *
 *    Revision 1.8  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.7  2001/07/29 18:43:08  jongfoster
 *    Changing #ifdef _FILENAME_H to FILENAME_H_INCLUDED, to conform to
 *    ANSI C rules.
 *
 *    Revision 1.6  2001/06/29 21:45:41  oes
 *    Indentation, CRLF->LF, Tab-> Space
 *
 *    Revision 1.5  2001/06/29 13:22:44  oes
 *    - Cleaned up
 *    - Added new functions: default_exports(), make_menu(),
 *      error_response() etc, ranamed others and changed
 *      param and return types.
 *    - Removed HTTP/HTML snipplets
 *    - Removed logentry from cancelled commit
 *
 *    Revision 1.4  2001/06/09 10:50:58  jongfoster
 *    Changing "show URL info" handler to new style.
 *    Adding "extern" to some function prototypes.
 *
 *    Revision 1.3  2001/06/03 19:12:16  oes
 *    introduced new cgi handling
 *
 *    No revisions before 1.3
 *
 **********************************************************************/


#include "project.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Main dispatch function
 */
extern struct http_response *dispatch_cgi(struct client_state *csp);

/* Not exactly a CGI */
extern struct http_response * error_response(struct client_state *csp,
                                             const char *templatename,
                                             int err);

/*
 * CGI support functions
 */
extern struct http_response * alloc_http_response(void);
extern void free_http_response(struct http_response *rsp);

extern struct http_response *finish_http_response(const struct client_state *csp,
                                                  struct http_response *rsp);

extern struct map * default_exports(const struct client_state *csp, const char *caller);

extern jb_err map_block_killer (struct map *exports, const char *name);
extern jb_err map_block_keep   (struct map *exports, const char *name);
extern jb_err map_conditional  (struct map *exports, const char *name, int choose_first);

extern jb_err template_load(const struct client_state *csp, char ** template_ptr, 
                            const char *templatename, int recursive);
extern jb_err template_fill(char ** template_ptr, const struct map *exports);
extern jb_err template_fill_for_cgi(const struct client_state *csp,
                                    const char *templatename,
                                    struct map *exports,
                                    struct http_response *rsp);

extern void cgi_init_error_messages(void);
extern struct http_response *cgi_error_memory(void);
extern jb_err cgi_redirect (struct http_response * rsp, const char *target);

extern jb_err cgi_error_no_template(const struct client_state *csp,
                                    struct http_response *rsp,
                                    const char *template_name);
extern jb_err cgi_error_bad_param(const struct client_state *csp,
                                  struct http_response *rsp);
extern jb_err cgi_error_disabled(const struct client_state *csp,
                                 struct http_response *rsp);
extern jb_err cgi_error_unknown(const struct client_state *csp,
                         struct http_response *rsp,
                         jb_err error_to_report);

extern jb_err get_number_param(struct client_state *csp,
                               const struct map *parameters,
                               char *name,
                               unsigned *pvalue);
extern jb_err get_string_param(const struct map *parameters,
                               const char *param_name,
                               const char **pparam);
extern char   get_char_param(const struct map *parameters,
                             const char *param_name);

/*
 * Text generators
 */
extern void get_http_time(int time_offset, char *buf, size_t buffer_size);
extern char *add_help_link(const char *item, struct configuration_spec *config);
extern char *make_menu(const char *self, const unsigned feature_flags);
extern char *dump_map(const struct map *the_map);

/*
 * Ad replacement images
 */
extern const char image_pattern_data[];
extern const size_t  image_pattern_length;
extern const char image_blank_data[];
extern const size_t  image_blank_length;

/* Revision control strings from this header and associated .c file */
extern const char cgi_rcs[];
extern const char cgi_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef CGI_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
