#ifndef CGIEDIT_H_INCLUDED
#define CGIEDIT_H_INCLUDED
#define CGIEDIT_H_VERSION "$Id: cgiedit.h,v 1.12 2011/09/04 11:10:56 fabiankeil Exp $"
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
