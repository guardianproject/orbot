#ifndef ERRLOG_H_INCLUDED
#define ERRLOG_H_INCLUDED
#define ERRLOG_H_VERSION "$Id: errlog.h,v 1.27 2011/09/04 11:10:56 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/errlog.h,v $
 *
 * Purpose     :  Log errors to a designated destination in an elegant,
 *                printf-like fashion.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2009 the SourceForge
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


#ifdef __cplusplus
extern "C" {
#endif

/* Debug level for errors */

/* XXX: Should be renamed. */
#define LOG_LEVEL_GPC        0x0001
#define LOG_LEVEL_CONNECT    0x0002
#define LOG_LEVEL_IO         0x0004
#define LOG_LEVEL_HEADER     0x0008
#define LOG_LEVEL_WRITING    0x0010
#ifdef FEATURE_FORCE_LOAD
#define LOG_LEVEL_FORCE      0x0020
#endif /* def FEATURE_FORCE_LOAD */
#define LOG_LEVEL_RE_FILTER  0x0040
#define LOG_LEVEL_REDIRECTS  0x0080
#define LOG_LEVEL_DEANIMATE  0x0100
#define LOG_LEVEL_CLF        0x0200 /* Common Log File format */
#define LOG_LEVEL_CRUNCH     0x0400
#define LOG_LEVEL_CGI        0x0800 /* CGI / templates */
#define LOG_LEVEL_RECEIVED   0x8000

/* Following are always on: */
#define LOG_LEVEL_INFO    0x1000
#define LOG_LEVEL_ERROR   0x2000
#define LOG_LEVEL_FATAL   0x4000 /* Exits after writing log */

extern void init_error_log(const char *prog_name, const char *logfname);
extern void set_debug_level(int debuglevel);
extern void disable_logging(void);
extern void init_log_module(void);
extern void show_version(const char *prog_name);
extern void log_error(int loglevel, const char *fmt, ...);
extern const char *jb_err_to_string(int jb_error);

/* Revision control strings from this header and associated .c file */
extern const char errlog_rcs[];
extern const char errlog_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef ERRLOG_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/

