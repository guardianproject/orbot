#ifndef CGI_H_INCLUDED
#define CGI_H_INCLUDED
#define CGI_H_VERSION "$Id: cgi.h,v 1.41 2011/09/04 11:10:56 fabiankeil Exp $"
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
extern struct http_response *error_response(struct client_state *csp,
                                            const char *templatename);

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
#ifdef FEATURE_COMPRESSION
/*
 * Minimum length which a buffer has to reach before
 * we bother to (re-)compress it. Completely arbitrary.
 */
extern const size_t LOWER_LENGTH_LIMIT_FOR_COMPRESSION;
extern char *compress_buffer(char *buffer, size_t *buffer_length, int compression_level);
#endif

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
