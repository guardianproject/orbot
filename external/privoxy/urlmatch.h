#ifndef URLMATCH_H_INCLUDED
#define URLMATCH_H_INCLUDED
#define URLMATCH_H_VERSION "$Id: urlmatch.h,v 1.18 2011/11/06 11:41:05 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/urlmatch.h,v $
 *
 * Purpose     :  Declares functions to match URLs against URL
 *                patterns.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2002, 2006 the
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

extern void free_http_request(struct http_request *http);
#ifndef FEATURE_EXTENDED_HOST_PATTERNS
extern jb_err init_domain_components(struct http_request *http);
#endif
extern jb_err parse_http_request(const char *req, struct http_request *http);
extern jb_err parse_http_url(const char *url,
                             struct http_request *http,
                             int require_protocol);
extern int url_requires_percent_encoding(const char *url);

#define REQUIRE_PROTOCOL 1

extern int url_match(const struct url_spec *pattern,
                     const struct http_request *http);

extern jb_err create_url_spec(struct url_spec *url, char *buf);
extern void free_url_spec(struct url_spec *url);
extern int match_portlist(const char *portlist, int port);
extern jb_err parse_forwarder_address(char *address, char **hostname, int *port);


/* Revision control strings from this header and associated .c file */
extern const char urlmatch_rcs[];
extern const char urlmatch_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef URLMATCH_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
