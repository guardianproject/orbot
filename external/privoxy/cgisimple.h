#ifndef CGISIMPLE_H_INCLUDED
#define CGISIMPLE_H_INCLUDED
#define CGISIMPLE_H_VERSION "$Id: cgisimple.h,v 1.16 2008/05/26 17:30:55 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/cgisimple.h,v $
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
 *    $Log: cgisimple.h,v $
 *    Revision 1.16  2008/05/26 17:30:55  fabiankeil
 *    Provide an OpenSearch Description to access the
 *    show-url-info page through "search engine plugins".
 *
 *    Revision 1.15  2007/01/23 15:51:17  fabiankeil
 *    Add favicon delivery functions.
 *
 *    Revision 1.14  2006/09/06 18:45:03  fabiankeil
 *    Incorporate modified version of Roland Rosenfeld's patch to
 *    optionally access the user-manual via Privoxy. Closes patch 679075.
 *
 *    Formatting changed to Privoxy style, added call to
 *    cgi_error_no_template if the requested file doesn't
 *    exist and modified check whether or not Privoxy itself
 *    should serve the manual. Should work cross-platform now.
 *
 *    Revision 1.13  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.11  2002/04/05 15:50:53  oes
 *    added send-stylesheet CGI
 *
 *    Revision 1.10  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.9  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.8  2002/03/16 23:54:06  jongfoster
 *    Adding graceful termination feature, to help look for memory leaks.
 *    If you enable this (which, by design, has to be done by hand
 *    editing config.h) and then go to http://i.j.b/die, then the program
 *    will exit cleanly after the *next* request.  It should free all the
 *    memory that was used.
 *
 *    Revision 1.7  2002/03/08 16:43:59  oes
 *    Renamed cgi_transparent_png to cgi_transparent_image
 *
 *    Revision 1.6  2002/03/07 03:48:59  oes
 *     - Changed built-in images from GIF to PNG
 *       (with regard to Unisys patent issue)
 *
 *    Revision 1.5  2002/01/22 23:26:03  jongfoster
 *    Adding cgi_transparent_gif() for http://i.j.b/t
 *
 *    Revision 1.4  2001/10/23 21:48:19  jongfoster
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
 *    Revision 1.3  2001/10/14 22:00:32  jongfoster
 *    Adding support for a 404 error when an invalid CGI page is requested.
 *
 *    Revision 1.2  2001/10/02 15:31:20  oes
 *    Introduced show-request cgi
 *
 *    Revision 1.1  2001/09/16 17:08:54  jongfoster
 *    Moving simple CGI functions from cgi.c to new file cgisimple.c
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
extern jb_err cgi_default      (struct client_state *csp,
                                struct http_response *rsp,
                                const struct map *parameters);
extern jb_err cgi_error_404    (struct client_state *csp,
                                struct http_response *rsp,
                                const struct map *parameters);
extern jb_err cgi_robots_txt   (struct client_state *csp,
                                struct http_response *rsp,
                                const struct map *parameters);
extern jb_err cgi_send_banner  (struct client_state *csp,
                                struct http_response *rsp,
                                const struct map *parameters);
extern jb_err cgi_show_status  (struct client_state *csp,
                                struct http_response *rsp,
                                const struct map *parameters);
extern jb_err cgi_show_url_info(struct client_state *csp,
                                struct http_response *rsp,
                                const struct map *parameters);
extern jb_err cgi_show_version (struct client_state *csp,
                                struct http_response *rsp,
                                const struct map *parameters);
extern jb_err cgi_show_request (struct client_state *csp,
                                struct http_response *rsp,
                                const struct map *parameters);
extern jb_err cgi_transparent_image (struct client_state *csp,
                                     struct http_response *rsp,
                                     const struct map *parameters);
extern jb_err cgi_send_error_favicon (struct client_state *csp,
                                      struct http_response *rsp,
                                      const struct map *parameters);
extern jb_err cgi_send_default_favicon (struct client_state *csp,
                                        struct http_response *rsp,
                                        const struct map *parameters);
extern jb_err cgi_send_stylesheet(struct client_state *csp,
                                  struct http_response *rsp,
                                  const struct map *parameters);
extern jb_err cgi_send_url_info_osd(struct client_state *csp,
                                    struct http_response *rsp,
                                    const struct map *parameters);
extern jb_err cgi_send_user_manual(struct client_state *csp,
                                   struct http_response *rsp,
                                   const struct map *parameters);


#ifdef FEATURE_GRACEFUL_TERMINATION
extern jb_err cgi_die (struct client_state *csp,
                       struct http_response *rsp,
                       const struct map *parameters);
#endif

/* Revision control strings from this header and associated .c file */
extern const char cgisimple_rcs[];
extern const char cgisimple_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef CGISIMPLE_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
