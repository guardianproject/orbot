#ifndef LOADERS_H_INCLUDED
#define LOADERS_H_INCLUDED
#define LOADERS_H_VERSION "$Id: loaders.h,v 1.29 2011/09/04 11:10:56 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/loaders.h,v $
 *
 * Purpose     :  Functions to load and unload the various
 *                configuration files.  Also contains code to manage
 *                the list of active loaders, and to automatically
 *                unload files that are no longer in use.
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


#ifdef __cplusplus
extern "C" {
#endif

/* Structures taken from project.h */
struct client_state;
struct file_list;
struct configuration_spec;
struct url_spec;

extern unsigned int sweep(void);
extern char *read_config_line(FILE *fp, unsigned long *linenum, char **buf);
extern int check_file_changed(const struct file_list * current,
                              const char * filename,
                              struct file_list ** newfl);

extern jb_err edit_read_line(FILE *fp,
                             char **raw_out,
                             char **prefix_out,
                             char **data_out,
                             int *newline,
                             unsigned long *line_number);

extern jb_err simple_read_line(FILE *fp, char **dest, int *newline);

/*
 * Various types of newlines that a file may contain.
 */
#define NEWLINE_UNKNOWN 0  /* Newline convention in file is unknown */
#define NEWLINE_UNIX    1  /* Newline convention in file is '\n'   (ASCII 10) */
#define NEWLINE_DOS     2  /* Newline convention in file is '\r\n' (ASCII 13,10) */
#define NEWLINE_MAC     3  /* Newline convention in file is '\r'   (ASCII 13) */

/*
 * Types of newlines that a file may contain, as strings.  If you have an
 * extremely weird compiler that does not have '\r' == CR == ASCII 13 and
 * '\n' == LF == ASCII 10), then fix CHAR_CR and CHAR_LF in loaders.c as
 * well as these definitions.
 */
#define NEWLINE(style) ((style)==NEWLINE_DOS ? "\r\n" : \
                        ((style)==NEWLINE_MAC ? "\r" : "\n"))


extern short int MustReload;
extern int load_action_files(struct client_state *csp);
extern int load_re_filterfiles(struct client_state *csp);

#ifdef FEATURE_TRUST
extern int load_trustfile(struct client_state *csp);
#endif /* def FEATURE_TRUST */

#ifdef FEATURE_GRACEFUL_TERMINATION
#ifdef FEATURE_TRUST
void unload_current_trust_file(void);
#endif
void unload_current_re_filterfile(void);
#endif /* FEATURE_GRACEFUL_TERMINATION */

void unload_forward_spec(struct forward_spec *fwd);

extern void add_loader(int (*loader)(struct client_state *),
                       struct configuration_spec * config);
extern int run_loader(struct client_state *csp);

extern int any_loaded_file_changed(const struct file_list *files_to_check);

/* Revision control strings from this header and associated .c file */
extern const char loaders_rcs[];
extern const char loaders_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef LOADERS_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
