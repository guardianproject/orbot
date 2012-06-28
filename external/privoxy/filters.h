#ifndef FILTERS_H_INCLUDED
#define FILTERS_H_INCLUDED
#define FILTERS_H_VERSION "$Id: filters.h,v 1.43 2011/09/04 11:10:56 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/filters.h,v $
 *
 * Purpose     :  Declares functions to parse/crunch headers and pages.
 *                Functions declared include:
 *                   `acl_addr', `add_stats', `block_acl', `block_imageurl',
 *                   `block_url', `url_actions', `filter_popups', `forward_url'
 *                   `ij_untrusted_url', `intercept_url', `re_process_buffer',
 *                   `show_proxy_args', and `trust_url'
 *
 * Copyright   :  Written by and Copyright (C) 2001-2010 the
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


#include "project.h"

#ifdef __cplusplus
extern "C" {
#endif


struct access_control_addr;
struct client_state;
struct http_request;
struct http_response;
struct current_action_spec;
struct url_actions;
struct url_spec;


/*
 * ACL checking
 */
#ifdef FEATURE_ACL
extern int block_acl(const struct access_control_addr *dst, const struct client_state *csp);
extern int acl_addr(const char *aspec, struct access_control_addr *aca);
#endif /* def FEATURE_ACL */

/*
 * Interceptors
 */
extern struct http_response *block_url(struct client_state *csp);
extern struct http_response *redirect_url(struct client_state *csp);
#ifdef FEATURE_TRUST
extern struct http_response *trust_url(struct client_state *csp);
#endif /* def FEATURE_TRUST */

/*
 * Request inspectors
 */
#ifdef FEATURE_TRUST
extern int is_untrusted_url(const struct client_state *csp);
#endif /* def FEATURE_TRUST */
#ifdef FEATURE_IMAGE_BLOCKING
extern int is_imageurl(const struct client_state *csp);
#endif /* def FEATURE_IMAGE_BLOCKING */
extern int connect_port_is_forbidden(const struct client_state *csp);

/*
 * Determining applicable actions
 */
extern void get_url_actions(struct client_state *csp,
                            struct http_request *http);
extern void apply_url_actions(struct current_action_spec *action,
                              struct http_request *http,
                              struct url_actions *b);
/*
 * Determining parent proxies
 */
extern const struct forward_spec *forward_url(struct client_state *csp,
                                              const struct http_request *http);

/*
 * Content modification
 */
extern char *execute_content_filters(struct client_state *csp);
extern char *execute_single_pcrs_command(char *subject, const char *pcrs_command, int *hits);
extern char *rewrite_url(char *old_url, const char *pcrs_command);
extern char *get_last_url(char *subject, const char *redirect_mode);

extern pcrs_job *compile_dynamic_pcrs_job_list(const struct client_state *csp, const struct re_filterfile_spec *b);

extern int content_requires_filtering(struct client_state *csp);
extern int content_filters_enabled(const struct current_action_spec *action);
extern int filters_available(const struct client_state *csp);

/*
 * Handling Max-Forwards:
 */
extern struct http_response *direct_response(struct client_state *csp);

/*
 * Revision control strings from this header and associated .c file
 */
extern const char filters_rcs[];
extern const char filters_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef FILTERS_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
