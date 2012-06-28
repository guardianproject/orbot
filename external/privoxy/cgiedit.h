#ifndef CGIEDIT_H_INCLUDED
#define CGIEDIT_H_INCLUDED
#define CGIEDIT_H_VERSION "$Id: cgiedit.h,v 1.10 2008/08/31 15:59:03 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/cgiedit.h,v $
 *
 * Purpose     :  CGI-based actionsfile editor.
 *                
 *                Functions declared include:
 * 
 *
 * Copyright   :  Written by and Copyright (C) 2001 the SourceForge
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
 *    $Log: cgiedit.h,v $
 *    Revision 1.10  2008/08/31 15:59:03  fabiankeil
 *    There's no reason to let remote toggling support depend
 *    on FEATURE_CGI_EDIT_ACTIONS, so make sure it doesn't.
 *
 *    Revision 1.9  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.7.2.2  2004/02/17 13:30:23  oes
 *    Moved cgi_error_disabled() from cgiedit.c to
 *    cgi.c to re-enable build with --disable-editor.
 *    Fixes Bug #892744. Thanks to Matthew Fischer
 *    for spotting.
 *
 *    Revision 1.7.2.1  2002/11/28 18:15:17  oes
 *    Added cgi_error_disabled
 *
 *    Revision 1.7  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.6  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.5  2002/01/22 23:24:48  jongfoster
 *    Adding edit-actions-section-swap
 *
 *    Revision 1.4  2001/11/13 00:28:51  jongfoster
 *    Adding new CGIs for use by non-JavaScript browsers:
 *      edit-actions-url-form
 *      edit-actions-add-url-form
 *      edit-actions-remove-url-form
 *
 *    Revision 1.3  2001/10/23 21:48:19  jongfoster
 *    Cleaning up error handling in CGI functions - they now send back
 *    a HTML error page and should never cause a FATAL error.  (Fixes one
 *    potential source of "denial of service" attacks).
 *
 *    CGI actions file editor that works and is actually useful.
 *
 *    Ability to toggle JunkBuster remotely using a CGI call.
 *
 *    You can turn off both the above features in the main configuration
 *    file, e.g. if you are running a multi-user proxy.
 *
 *    Revision 1.2  2001/10/14 22:12:49  jongfoster
 *    New version of CGI-based actionsfile editor.
 *    Major changes, including:
 *    - Completely new file parser and file output routines
 *    - edit-actions CGI renamed edit-actions-for-url
 *    - All CGIs now need a filename parameter, except for...
 *    - New CGI edit-actions which doesn't need a filename,
 *      to allow you to start the editor up.
 *    - edit-actions-submit now works, and now automatically
 *      redirects you back to the main edit-actions-list handler.
 *
 *    Revision 1.1  2001/09/16 15:47:37  jongfoster
 *    First version of CGI-based edit interface.  This is very much a
 *    work-in-progress, and you can't actually use it to edit anything
 *    yet.  You must #define FEATURE_CGI_EDIT_ACTIONS for these changes
 *    to have any effect.
 *
 *
 **********************************************************************/


#include "project.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * CGI functions
 */
#ifdef FEATURE_CGI_EDIT_ACTIONS
extern jb_err cgi_edit_actions        (struct client_state *csp,
                                       struct http_response *rsp,
                                       const struct map *parameters);
extern jb_err cgi_edit_actions_for_url(struct client_state *csp,
                                       struct http_response *rsp,
                                       const struct map *parameters);
extern jb_err cgi_edit_actions_list   (struct client_state *csp,
                                       struct http_response *rsp,
                                       const struct map *parameters);
extern jb_err cgi_edit_actions_submit (struct client_state *csp,
                                       struct http_response *rsp,
                                       const struct map *parameters);
extern jb_err cgi_edit_actions_url    (struct client_state *csp,
                                       struct http_response *rsp,
                                       const struct map *parameters);
extern jb_err cgi_edit_actions_url_form(struct client_state *csp,
                                        struct http_response *rsp,
                                        const struct map *parameters);
extern jb_err cgi_edit_actions_add_url(struct client_state *csp,
                                       struct http_response *rsp,
                                       const struct map *parameters);
extern jb_err cgi_edit_actions_add_url_form(struct client_state *csp,
                                            struct http_response *rsp,
                                            const struct map *parameters);
extern jb_err cgi_edit_actions_remove_url    (struct client_state *csp,
                                              struct http_response *rsp,
                                              const struct map *parameters);
extern jb_err cgi_edit_actions_remove_url_form(struct client_state *csp,
                                            struct http_response *rsp,
                                            const struct map *parameters);
extern jb_err cgi_edit_actions_section_remove(struct client_state *csp,
                                              struct http_response *rsp,
                                              const struct map *parameters);
extern jb_err cgi_edit_actions_section_add   (struct client_state *csp,
                                              struct http_response *rsp,
                                              const struct map *parameters);
extern jb_err cgi_edit_actions_section_swap  (struct client_state *csp,
                                              struct http_response *rsp,
                                              const struct map *parameters);
#endif /* def FEATURE_CGI_EDIT_ACTIONS */
#ifdef FEATURE_TOGGLE
extern jb_err cgi_toggle(struct client_state *csp,
                         struct http_response *rsp,
                         const struct map *parameters);
#endif /* def FEATURE_TOGGLE */

/* Revision control strings from this header and associated .c file */
extern const char cgiedit_rcs[];
extern const char cgiedit_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef CGI_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
