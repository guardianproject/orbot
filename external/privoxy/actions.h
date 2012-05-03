#ifndef ACTIONS_H_INCLUDED
#define ACTIONS_H_INCLUDED
#define ACTIONS_H_VERSION "$Id: actions.h,v 1.18 2008/03/30 14:52:00 fabiankeil Exp $"
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
 * Revisions   :
 *    $Log: actions.h,v $
 *    Revision 1.18  2008/03/30 14:52:00  fabiankeil
 *    Rename load_actions_file() and load_re_filterfile()
 *    as they load multiple files "now".
 *
 *    Revision 1.17  2008/01/28 20:17:40  fabiankeil
 *    - Mark some parameters as immutable.
 *    - Hide update_action_bits_for_all_tags() while it's unused.
 *
 *    Revision 1.16  2007/04/17 18:21:45  fabiankeil
 *    Split update_action_bits() into
 *    update_action_bits_for_all_tags()
 *    and update_action_bits_for_tag().
 *
 *    Revision 1.15  2007/04/15 16:39:20  fabiankeil
 *    Introduce tags as alternative way to specify which
 *    actions apply to a request. At the moment tags can be
 *    created based on client and server headers.
 *
 *    Revision 1.14  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.12  2002/05/06 07:56:50  oes
 *    Made actions_to_html independent of FEATURE_CGI_EDIT_ACTIONS
 *
 *    Revision 1.11  2002/04/30 11:14:52  oes
 *    Made csp the first parameter in *action_to_html
 *
 *    Revision 1.10  2002/04/26 12:53:33  oes
 *     -  actions_to_html signature change
 *     -  current_action_to_text: renamed to current_action_to_html
 *        and signature change
 *
 *    Revision 1.9  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.8  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.7  2002/03/16 23:54:06  jongfoster
 *    Adding graceful termination feature, to help look for memory leaks.
 *    If you enable this (which, by design, has to be done by hand
 *    editing config.h) and then go to http://i.j.b/die, then the program
 *    will exit cleanly after the *next* request.  It should free all the
 *    memory that was used.
 *
 *    Revision 1.6  2001/10/23 21:30:30  jongfoster
 *    Adding error-checking to selected functions.
 *
 *    Revision 1.5  2001/10/14 21:58:22  jongfoster
 *    Adding support for the CGI-based editor:
 *    - Exported get_actions()
 *    - Added new function free_alias_list()
 *    - Added support for {{settings}} and {{description}} blocks
 *      in the actions file.  They are currently ignored.
 *    - Added restriction to only one {{alias}} block which must appear
 *      first in the file, to simplify the editor's rewriting rules.
 *    - Note that load_actions_file() is no longer used by the CGI-based
 *      editor, but some of the other routines in this file are.
 *
 *    Revision 1.4  2001/09/16 15:47:37  jongfoster
 *    First version of CGI-based edit interface.  This is very much a
 *    work-in-progress, and you can't actually use it to edit anything
 *    yet.  You must #define FEATURE_CGI_EDIT_ACTIONS for these changes
 *    to have any effect.
 *
 *    Revision 1.3  2001/09/14 00:17:32  jongfoster
 *    Tidying up memory allocation. New function init_action().
 *
 *    Revision 1.2  2001/07/29 19:01:11  jongfoster
 *    Changed _FILENAME_H to FILENAME_H_INCLUDED.
 *    Added forward declarations for needed structures.
 *
 *    Revision 1.1  2001/05/31 21:16:46  jongfoster
 *    Moved functions to process the action list into this new file.
 *
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

