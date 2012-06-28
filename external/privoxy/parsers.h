#ifndef PARSERS_H_INCLUDED
#define PARSERS_H_INCLUDED
#define PARSERS_H_VERSION "$Id: parsers.h,v 1.51 2011/09/04 11:10:56 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/parsers.h,v $
 *
 * Purpose     :  Declares functions to parse/crunch headers and pages.
 *                Functions declared include:
 *                   `add_to_iob', `client_cookie_adder', `client_from',
 *                   `client_referrer', `client_send_cookie', `client_ua',
 *                   `client_uagent', `client_x_forwarded',
 *                   `client_x_forwarded_adder', `client_xtra_adder',
 *                   `content_type', `crumble', `destroy_list', `enlist',
 *                   `flush_socket', `free_http_request', `get_header',
 *                   `list_to_text', `parse_http_request', `sed',
 *                   and `server_set_cookie'.
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
 *********************************************************************/


#include "project.h"

#ifdef __cplusplus
extern "C" {
#endif

/* Used for sed()'s second argument. */
#define FILTER_CLIENT_HEADERS 0
#define FILTER_SERVER_HEADERS 1

extern long flush_socket(jb_socket fd, struct iob *iob);
extern jb_err add_to_iob(struct client_state *csp, char *buf, long n);
extern jb_err decompress_iob(struct client_state *csp);
extern char *get_header(struct iob *iob);
extern char *get_header_value(const struct list *header_list, const char *header_name);
extern jb_err sed(struct client_state *csp, int filter_server_headers);
extern jb_err update_server_headers(struct client_state *csp);
extern void get_http_time(int time_offset, char *buf, size_t buffer_size);
extern jb_err get_destination_from_headers(const struct list *headers, struct http_request *http);

#ifdef FEATURE_FORCE_LOAD
extern int strclean(char *string, const char *substring);
#endif /* def FEATURE_FORCE_LOAD */

/* Revision control strings from this header and associated .c file */
extern const char parsers_rcs[];
extern const char parsers_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef PARSERS_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
