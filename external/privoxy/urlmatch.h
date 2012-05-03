#ifndef URLMATCH_H_INCLUDED
#define URLMATCH_H_INCLUDED
#define URLMATCH_H_VERSION "$Id: urlmatch.h,v 1.13 2009/03/02 19:18:11 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/urlmatch.h,v $
 *
 * Purpose     :  Declares functions to match URLs against URL
 *                patterns.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2002, 2006 the SourceForge
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
 *    $Log: urlmatch.h,v $
 *    Revision 1.13  2009/03/02 19:18:11  fabiankeil
 *    Streamline parse_http_request()'s prototype. As
 *    cparser pointed out it doesn't actually use csp.
 *
 *    Revision 1.12  2008/05/04 16:18:32  fabiankeil
 *    Provide parse_http_url() with a third parameter to specify
 *    whether or not URLs without protocol are acceptable.
 *
 *    Revision 1.11  2008/05/04 13:30:55  fabiankeil
 *    Streamline parse_http_url()'s prototype.
 *
 *    Revision 1.10  2008/04/14 18:11:21  fabiankeil
 *    The compiler might not notice it, but the buffer passed to
 *    create_url_spec() is modified later on and thus shouldn't
 *    be declared immutable.
 *
 *    Revision 1.9  2008/04/08 16:07:39  fabiankeil
 *    Make it harder to mistake url_match()'s
 *    second parameter for an url_spec.
 *
 *    Revision 1.8  2007/09/02 15:31:20  fabiankeil
 *    Move match_portlist() from filter.c to urlmatch.c.
 *    It's used for url matching, not for filtering.
 *
 *    Revision 1.7  2007/01/06 14:24:38  fabiankeil
 *    Mark *csp as immutable for parse_http_url()
 *    and url_match().
 *
 *    Revision 1.6  2006/12/06 19:12:43  fabiankeil
 *    Added prototype for init_domain_components().
 *
 *    Revision 1.5  2006/07/18 14:48:47  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.3  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.2  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.1  2002/01/17 20:53:46  jongfoster
 *    Moving all our URL and URL pattern parsing code to the same file - it
 *    was scattered around in filters.c, loaders.c and parsers.c.
 *
 *    Providing a single, simple url_match(pattern,url) function - rather than
 *    the 3-line match routine which was repeated all over the place.
 *
 *    Renaming free_url to free_url_spec, since it frees a struct url_spec.
 *
 *    Providing parse_http_url() so that URLs can be parsed without faking a
 *    HTTP request line for parse_http_request() or repeating the parsing
 *    code (both of which were techniques that were actually in use).
 *
 *    Standardizing that struct http_request is used to represent a URL, and
 *    struct url_spec is used to represent a URL pattern.  (Before, URLs were
 *    represented as seperate variables and a partially-filled-in url_spec).
 *
 *
 *********************************************************************/


#include "project.h"

#ifdef __cplusplus
extern "C" {
#endif

extern void free_http_request(struct http_request *http);
extern jb_err init_domain_components(struct http_request *http);
extern jb_err parse_http_request(const char *req, struct http_request *http);
extern jb_err parse_http_url(const char *url,
                             struct http_request *http,
                             int require_protocol);
#define REQUIRE_PROTOCOL 1

extern int url_match(const struct url_spec *pattern,
                     const struct http_request *http);

extern jb_err create_url_spec(struct url_spec *url, char *buf);
extern void free_url_spec(struct url_spec *url);
extern int match_portlist(const char *portlist, int port);


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
