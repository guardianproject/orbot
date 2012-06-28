#ifndef ACTIONS_H_INCLUDED
#define ACTIONS_H_INCLUDED
#define ACTIONS_H_VERSION "$Id: actions.h,v 1.20 2011/09/04 11:10:56 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/actions.h,v $
 *
 * Purpose     :  Declares functions to work with actions files
 *                Functions declared include: FIXME
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
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif


struct action_spec;
struct current_action_spec;
struct client_state;



/* This structure is used to hold user-defined aliases */
struct action_alias
{
   const char * name;
   struct action_spec action[1];
   struct action_alias * next;
};


extern jb_err get_actions (char *line,
                           struct action_alias * alias_list,
                           struct action_spec *cur_action);
extern void free_alias_list(struct action_alias *alias_list);

extern void init_action(struct action_spec *dest);
extern void free_action(struct action_spec *src);
extern jb_err merge_actions (struct action_spec *dest,
                             const struct action_spec *src);
#if 0
extern int update_action_bits_for_all_tags(struct client_state *csp);
#endif
extern int update_action_bits_for_tag(struct client_state *csp, const char *tag);
extern jb_err copy_action (struct action_spec *dest,
                           const struct action_spec *src);
extern char * actions_to_text     (const struct action_spec *action);
extern char * actions_to_html     (const struct client_state *csp,
                                   const struct action_spec *action);
extern void init_current_action     (struct current_action_spec *dest);
extern void free_current_action     (struct current_action_spec *src);
extern jb_err merge_current_action  (struct current_action_spec *dest,
                                     const struct action_spec *src);
extern char * current_action_to_html(const struct client_state *csp,
                                     const struct current_action_spec *action);

extern jb_err get_action_token(char **line, char **name, char **value);
extern void unload_actions_file(void *file_data);
extern int load_action_files(struct client_state *csp);

#ifdef FEATURE_GRACEFUL_TERMINATION
void unload_current_actions_file(void);
#endif


/* Revision control strings from this header and associated .c file */
extern const char actions_rcs[];
extern const char actions_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef ACTIONS_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/

