const char actions_rcs[] = "$Id: actions.c,v 1.56 2009/03/08 14:19:21 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/actions.c,v $
 *
 * Purpose     :  Declares functions to work with actions files
 *                Functions declared include: FIXME
 *
 * Copyright   :  Written by and Copyright (C) 2001-2008 the SourceForge
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
 *    $Log: actions.c,v $
 *    Revision 1.56  2009/03/08 14:19:21  fabiankeil
 *    Fix justified (but harmless) compiler warnings
 *    on platforms where sizeof(int) < sizeof(long).
 *
 *    Revision 1.55  2008/12/04 18:18:56  fabiankeil
 *    Fix some cparser warnings.
 *
 *    Revision 1.54  2008/09/20 10:04:33  fabiankeil
 *    Remove hide-forwarded-for-headers action which has
 *    been obsoleted by change-x-forwarded-for{block}.
 *
 *    Revision 1.53  2008/05/26 16:04:04  fabiankeil
 *    s@memorey@memory@
 *
 *    Revision 1.52  2008/04/27 16:26:59  fabiankeil
 *    White space fix for the last commit.
 *
 *    Revision 1.51  2008/04/27 16:20:19  fabiankeil
 *    Complain about every block action without reason found.
 *
 *    Revision 1.50  2008/03/30 14:52:00  fabiankeil
 *    Rename load_actions_file() and load_re_filterfile()
 *    as they load multiple files "now".
 *
 *    Revision 1.49  2008/03/29 12:13:45  fabiankeil
 *    Remove send-wafer and send-vanilla-wafer actions.
 *
 *    Revision 1.48  2008/03/28 18:17:14  fabiankeil
 *    In action_used_to_be_valid(), loop through an array of formerly
 *    valid actions instead of using an OR-chain of strcmpic() calls.
 *
 *    Revision 1.47  2008/03/28 15:13:37  fabiankeil
 *    Remove inspect-jpegs action.
 *
 *    Revision 1.46  2008/03/27 18:27:20  fabiankeil
 *    Remove kill-popups action.
 *
 *    Revision 1.45  2008/03/24 11:21:02  fabiankeil
 *    Share the action settings for multiple patterns in the same
 *    section so we waste less memory for gigantic block lists
 *    (and load them slightly faster). Reported by Franz Schwartau.
 *
 *    Revision 1.44  2008/03/04 18:30:34  fabiankeil
 *    Remove the treat-forbidden-connects-like-blocks action. We now
 *    use the "blocked" page for forbidden CONNECT requests by default.
 *
 *    Revision 1.43  2008/03/01 14:00:43  fabiankeil
 *    Let the block action take the reason for the block
 *    as argument and show it on the "blocked" page.
 *
 *    Revision 1.42  2008/02/09 15:15:38  fabiankeil
 *    List active and inactive actions in the show-url-info's
 *    "Final results" section separately. Patch submitted by Lee
 *    in #1830056, modified to list active actions first.
 *
 *    Revision 1.41  2008/01/28 20:17:40  fabiankeil
 *    - Mark some parameters as immutable.
 *    - Hide update_action_bits_for_all_tags() while it's unused.
 *
 *    Revision 1.40  2007/05/21 10:26:50  fabiankeil
 *    - Use strlcpy() instead of strcpy().
 *    - Provide a reason why loading the actions
 *      file might have failed.
 *
 *    Revision 1.39  2007/04/17 18:21:45  fabiankeil
 *    Split update_action_bits() into
 *    update_action_bits_for_all_tags()
 *    and update_action_bits_for_tag().
 *
 *    Revision 1.38  2007/04/15 16:39:20  fabiankeil
 *    Introduce tags as alternative way to specify which
 *    actions apply to a request. At the moment tags can be
 *    created based on client and server headers.
 *
 *    Revision 1.37  2007/03/11 15:56:12  fabiankeil
 *    Add kludge to log unknown aliases and actions before exiting.
 *
 *    Revision 1.36  2006/12/28 17:15:42  fabiankeil
 *    Fix gcc43 conversion warning.
 *
 *    Revision 1.35  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.32.2.6  2006/01/29 23:10:56  david__schmidt
 *    Multiple filter file support
 *
 *    Revision 1.32.2.5  2005/06/09 01:18:41  david__schmidt
 *    Tweaks to conditionally include pthread.h if FEATURE_PTHREAD is enabled -
 *    this becomes important when jcc.h gets included later down the line.
 *
 *    Revision 1.32.2.4  2003/12/03 10:33:11  oes
 *    - Implemented Privoxy version requirement through
 *      for-privoxy-version= statement in {{settings}}
 *      block
 *    - Fix for unchecked out-of-memory condition
 *
 *    Revision 1.32.2.3  2003/02/28 12:52:10  oes
 *    Fixed memory leak reported by Dan Price in Bug #694713
 *
 *    Revision 1.32.2.2  2002/11/20 14:36:55  oes
 *    Extended unload_current_actions_file() to multiple AFs.
 *    Thanks to Oliver Stoeneberg for the hint.
 *
 *    Revision 1.32.2.1  2002/05/26 12:13:16  roro
 *    Change unsigned to unsigned long in actions_name struct.  This closes
 *    SourceForge Bug #539284.
 *
 *    Revision 1.32  2002/05/12 21:36:29  jongfoster
 *    Correcting function comments
 *
 *    Revision 1.31  2002/05/06 07:56:50  oes
 *    Made actions_to_html independent of FEATURE_CGI_EDIT_ACTIONS
 *
 *    Revision 1.30  2002/04/30 11:14:52  oes
 *    Made csp the first parameter in *action_to_html
 *
 *    Revision 1.29  2002/04/26 19:30:54  jongfoster
 *    - current_action_to_html(): Adding help link for the "-" form of
 *      one-string actions.
 *    - Some actions had "<br>-", some "<br> -" (note the space).
 *      Standardizing on no space.
 *    - Greatly simplifying some of the code by using string_join()
 *      where appropriate.
 *
 *    Revision 1.28  2002/04/26 12:53:15  oes
 *     - CGI AF editor now writes action lines split into
 *       single lines with line continuation
 *     - actions_to_html now embeds each action name in
 *       link to chapter
 *     - current_action_to_text is now called current_action_to_html
 *       and acts like actions_to_html
 *
 *    Revision 1.27  2002/04/24 02:10:31  oes
 *     - Jon's patch for multiple AFs:
 *       - split load_actions_file, add load_one_actions_file
 *       - make csp->actions_list files an array
 *       - remember file id with each action
 *     - Copy_action now frees dest action before copying
 *
 *    Revision 1.26  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.25  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.24  2002/03/16 23:54:06  jongfoster
 *    Adding graceful termination feature, to help look for memory leaks.
 *    If you enable this (which, by design, has to be done by hand
 *    editing config.h) and then go to http://i.j.b/die, then the program
 *    will exit cleanly after the *next* request.  It should free all the
 *    memory that was used.
 *
 *    Revision 1.23  2002/03/07 03:46:16  oes
 *    Fixed compiler warnings
 *
 *    Revision 1.22  2002/01/21 00:27:02  jongfoster
 *    Allowing free_action(NULL).
 *    Moving the functions that #include actionlist.h to the end of the file,
 *    because the Visual C++ 97 debugger gets extremely confused if you try
 *    to debug any code that comes after them in the file.
 *
 *    Revision 1.21  2002/01/17 20:54:44  jongfoster
 *    Renaming free_url to free_url_spec, since it frees a struct url_spec.
 *
 *    Revision 1.20  2001/11/22 21:56:49  jongfoster
 *    Making action_spec->flags into an unsigned long rather than just an
 *    unsigned int.
 *    Fixing a bug in the display of -add-header and -wafer
 *
 *    Revision 1.19  2001/11/13 00:14:07  jongfoster
 *    Fixing stupid bug now I've figured out what || means.
 *    (It always returns 0 or 1, not one of it's paramaters.)
 *
 *    Revision 1.18  2001/11/07 00:06:06  steudten
 *    Add line number in error output for lineparsing for
 *    actionsfile.
 *
 *    Revision 1.17  2001/10/25 03:40:47  david__schmidt
 *    Change in porting tactics: OS/2's EMX porting layer doesn't allow multiple
 *    threads to call select() simultaneously.  So, it's time to do a real, live,
 *    native OS/2 port.  See defines for __EMX__ (the porting layer) vs. __OS2__
 *    (native). Both versions will work, but using __OS2__ offers multi-threading.
 *
 *    Revision 1.16  2001/10/23 21:30:30  jongfoster
 *    Adding error-checking to selected functions.
 *
 *    Revision 1.15  2001/10/14 21:58:22  jongfoster
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
 *    Revision 1.14  2001/09/22 16:36:59  jongfoster
 *    Removing unused parameter fs from read_config_line()
 *
 *    Revision 1.13  2001/09/16 15:47:37  jongfoster
 *    First version of CGI-based edit interface.  This is very much a
 *    work-in-progress, and you can't actually use it to edit anything
 *    yet.  You must #define FEATURE_CGI_EDIT_ACTIONS for these changes
 *    to have any effect.
 *
 *    Revision 1.12  2001/09/16 13:21:27  jongfoster
 *    Changes to use new list functions.
 *
 *    Revision 1.11  2001/09/14 00:17:32  jongfoster
 *    Tidying up memory allocation. New function init_action().
 *
 *    Revision 1.10  2001/09/10 10:14:34  oes
 *    Removing unused variable
 *
 *    Revision 1.9  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.8  2001/06/29 13:19:52  oes
 *    Removed logentry from cancelled commit
 *
 *    Revision 1.7  2001/06/09 10:55:28  jongfoster
 *    Changing BUFSIZ ==> BUFFER_SIZE
 *
 *    Revision 1.6  2001/06/07 23:04:34  jongfoster
 *    Made get_actions() static.
 *
 *    Revision 1.5  2001/06/03 19:11:48  oes
 *    adapted to new enlist_unique arg format
 *
 *    Revision 1.4  2001/06/01 20:03:42  jongfoster
 *    Better memory management - current_action->strings[] now
 *    contains copies of the strings, not the original.
 *
 *    Revision 1.3  2001/06/01 18:49:17  jongfoster
 *    Replaced "list_share" with "list" - the tiny memory gain was not
 *    worth the extra complexity.
 *
 *    Revision 1.2  2001/05/31 21:40:00  jongfoster
 *    Removing some commented out, obsolete blocks of code.
 *
 *    Revision 1.1  2001/05/31 21:16:46  jongfoster
 *    Moved functions to process the action list into this new file.
 *
 *
 *********************************************************************/


#include "config.h"

#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <stdlib.h>

#ifdef FEATURE_PTHREAD
#include <pthread.h>
#endif

#include "project.h"
#include "jcc.h"
#include "list.h"
#include "actions.h"
#include "miscutil.h"
#include "errlog.h"
#include "loaders.h"
#include "encode.h"
#include "urlmatch.h"
#include "cgi.h"
#include "ssplit.h"

const char actions_h_rcs[] = ACTIONS_H_VERSION;


/*
 * We need the main list of options.
 *
 * First, we need a way to tell between boolean, string, and multi-string
 * options.  For string and multistring options, we also need to be
 * able to tell the difference between a "+" and a "-".  (For bools,
 * the "+"/"-" information is encoded in "add" and "mask").  So we use
 * an enumerated type (well, the preprocessor equivalent).  Here are
 * the values:
 */
#define AV_NONE       0 /* +opt -opt */
#define AV_ADD_STRING 1 /* +stropt{string} */
#define AV_REM_STRING 2 /* -stropt */
#define AV_ADD_MULTI  3 /* +multiopt{string} +multiopt{string2} */
#define AV_REM_MULTI  4 /* -multiopt{string} -multiopt          */

/*
 * We need a structure to hold the name, flag changes,
 * type, and string index.
 */
struct action_name
{
   const char * name;
   unsigned long mask;   /* a bit set to "0" = remove action */
   unsigned long add;    /* a bit set to "1" = add action */
   int takes_value;      /* an AV_... constant */
   int index;            /* index into strings[] or multi[] */
};

/*
 * And with those building blocks in place, here's the array.
 */
static const struct action_name action_names[] =
{
   /*
    * Well actually there's no data here - it's in actionlist.h
    * This keeps it together to make it easy to change.
    *
    * Here's the macros used to format it:
    */
#define DEFINE_ACTION_MULTI(name,index)                   \
   { "+" name, ACTION_MASK_ALL, 0, AV_ADD_MULTI, index }, \
   { "-" name, ACTION_MASK_ALL, 0, AV_REM_MULTI, index },
#define DEFINE_ACTION_STRING(name,flag,index)                 \
   { "+" name, ACTION_MASK_ALL, flag, AV_ADD_STRING, index }, \
   { "-" name, ~flag, 0, AV_REM_STRING, index },
#define DEFINE_ACTION_BOOL(name,flag)   \
   { "+" name, ACTION_MASK_ALL, flag }, \
   { "-" name, ~flag, 0 },
#define DEFINE_ACTION_ALIAS 1 /* Want aliases please */

#include "actionlist.h"

#undef DEFINE_ACTION_MULTI
#undef DEFINE_ACTION_STRING
#undef DEFINE_ACTION_BOOL
#undef DEFINE_ACTION_ALIAS

   { NULL, 0, 0 } /* End marker */
};


static int load_one_actions_file(struct client_state *csp, int fileid);


/*********************************************************************
 *
 * Function    :  merge_actions
 *
 * Description :  Merge two actions together.
 *                Similar to "dest += src".
 *
 * Parameters  :
 *          1  :  dest = Actions to modify.
 *          2  :  src = Action to add.
 *
 * Returns     :  JB_ERR_OK or JB_ERR_MEMORY
 *
 *********************************************************************/
jb_err merge_actions (struct action_spec *dest,
                      const struct action_spec *src)
{
   int i;
   jb_err err;

   dest->mask &= src->mask;
   dest->add  &= src->mask;
   dest->add  |= src->add;

   for (i = 0; i < ACTION_STRING_COUNT; i++)
   {
      char * str = src->string[i];
      if (str)
      {
         freez(dest->string[i]);
         dest->string[i] = strdup(str);
         if (NULL == dest->string[i])
         {
            return JB_ERR_MEMORY;
         }
      }
   }

   for (i = 0; i < ACTION_MULTI_COUNT; i++)
   {
      if (src->multi_remove_all[i])
      {
         /* Remove everything from dest */
         list_remove_all(dest->multi_remove[i]);
         dest->multi_remove_all[i] = 1;

         err = list_duplicate(dest->multi_add[i], src->multi_add[i]);
      }
      else if (dest->multi_remove_all[i])
      {
         /*
          * dest already removes everything, so we only need to worry
          * about what we add.
          */
         list_remove_list(dest->multi_add[i], src->multi_remove[i]);
         err = list_append_list_unique(dest->multi_add[i], src->multi_add[i]);
      }
      else
      {
         /* No "remove all"s to worry about. */
         list_remove_list(dest->multi_add[i], src->multi_remove[i]);
         err = list_append_list_unique(dest->multi_remove[i], src->multi_remove[i]);
         if (!err) err = list_append_list_unique(dest->multi_add[i], src->multi_add[i]);
      }

      if (err)
      {
         return err;
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  copy_action
 *
 * Description :  Copy an action_specs.
 *                Similar to "dest = src".
 *
 * Parameters  :
 *          1  :  dest = Destination of copy.
 *          2  :  src = Source for copy.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
jb_err copy_action (struct action_spec *dest,
                    const struct action_spec *src)
{
   int i;
   jb_err err = JB_ERR_OK;

   free_action(dest);
   memset(dest, '\0', sizeof(*dest));

   dest->mask = src->mask;
   dest->add  = src->add;

   for (i = 0; i < ACTION_STRING_COUNT; i++)
   {
      char * str = src->string[i];
      if (str)
      {
         str = strdup(str);
         if (!str)
         {
            return JB_ERR_MEMORY;
         }
         dest->string[i] = str;
      }
   }

   for (i = 0; i < ACTION_MULTI_COUNT; i++)
   {
      dest->multi_remove_all[i] = src->multi_remove_all[i];
      err = list_duplicate(dest->multi_remove[i], src->multi_remove[i]);
      if (err)
      {
         return err;
      }
      err = list_duplicate(dest->multi_add[i],    src->multi_add[i]);
      if (err)
      {
         return err;
      }
   }
   return err;
}

/*********************************************************************
 *
 * Function    :  free_action_spec
 *
 * Description :  Frees an action_spec and the memory used by it.
 *
 * Parameters  :
 *          1  :  src = Source to free.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void free_action_spec(struct action_spec *src)
{
   free_action(src);
   freez(src);
}


/*********************************************************************
 *
 * Function    :  free_action
 *
 * Description :  Destroy an action_spec.  Frees memory used by it,
 *                except for the memory used by the struct action_spec
 *                itself.
 *
 * Parameters  :
 *          1  :  src = Source to free.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void free_action (struct action_spec *src)
{
   int i;

   if (src == NULL)
   {
      return;
   }

   for (i = 0; i < ACTION_STRING_COUNT; i++)
   {
      freez(src->string[i]);
   }

   for (i = 0; i < ACTION_MULTI_COUNT; i++)
   {
      destroy_list(src->multi_remove[i]);
      destroy_list(src->multi_add[i]);
   }

   memset(src, '\0', sizeof(*src));
}


/*********************************************************************
 *
 * Function    :  get_action_token
 *
 * Description :  Parses a line for the first action.
 *                Modifies it's input array, doesn't allocate memory.
 *                e.g. given:
 *                *line="  +abc{def}  -ghi "
 *                Returns:
 *                *line="  -ghi "
 *                *name="+abc"
 *                *value="def"
 *
 * Parameters  :
 *          1  :  line = [in] The line containing the action.
 *                       [out] Start of next action on line, or
 *                       NULL if we reached the end of line before
 *                       we found an action.
 *          2  :  name = [out] Start of action name, null
 *                       terminated.  NULL on EOL
 *          3  :  value = [out] Start of action value, null
 *                        terminated.  NULL if none or EOL.
 *
 * Returns     :  JB_ERR_OK => Ok
 *                JB_ERR_PARSE => Mismatched {} (line was trashed anyway)
 *
 *********************************************************************/
jb_err get_action_token(char **line, char **name, char **value)
{
   char * str = *line;
   char ch;

   /* set default returns */
   *line = NULL;
   *name = NULL;
   *value = NULL;

   /* Eat any leading whitespace */
   while ((*str == ' ') || (*str == '\t'))
   {
      str++;
   }

   if (*str == '\0')
   {
      return 0;
   }

   if (*str == '{')
   {
      /* null name, just value is prohibited */
      return JB_ERR_PARSE;
   }

   *name = str;

   /* parse option */
   while (((ch = *str) != '\0') &&
          (ch != ' ') && (ch != '\t') && (ch != '{'))
   {
      if (ch == '}')
      {
         /* error, '}' without '{' */
         return JB_ERR_PARSE;
      }
      str++;
   }
   *str = '\0';

   if (ch != '{')
   {
      /* no value */
      if (ch == '\0')
      {
         /* EOL - be careful not to run off buffer */
         *line = str;
      }
      else
      {
         /* More to parse next time. */
         *line = str + 1;
      }
      return JB_ERR_OK;
   }

   str++;
   *value = str;

   str = strchr(str, '}');
   if (str == NULL)
   {
      /* error */
      *value = NULL;
      return JB_ERR_PARSE;
   }

   /* got value */
   *str = '\0';
   *line = str + 1;

   chomp(*value);

   return JB_ERR_OK;
}

/*********************************************************************
 *
 * Function    :  action_used_to_be_valid
 *
 * Description :  Checks if unrecognized actions were valid in earlier
 *                releases.
 *
 * Parameters  :
 *          1  :  action = The string containing the action to check.
 *
 * Returns     :  True if yes, otherwise false.
 *
 *********************************************************************/
static int action_used_to_be_valid(const char *action)
{
   static const char *formerly_valid_actions[] = {
      "inspect-jpegs",
      "kill-popups",
      "send-vanilla-wafer",
      "send-wafer",
      "treat-forbidden-connects-like-blocks",
      "vanilla-wafer",
      "wafer"
   };
   unsigned int i;

   for (i = 0; i < SZ(formerly_valid_actions); i++)
   {
      if (0 == strcmpic(action, formerly_valid_actions[i]))
      {
         return TRUE;
      }
   }

   return FALSE;
}

/*********************************************************************
 *
 * Function    :  get_actions
 *
 * Description :  Parses a list of actions.
 *
 * Parameters  :
 *          1  :  line = The string containing the actions.
 *                       Will be written to by this function.
 *          2  :  alias_list = Custom alias list, or NULL for none.
 *          3  :  cur_action = Where to store the action.  Caller
 *                             allocates memory.
 *
 * Returns     :  JB_ERR_OK => Ok
 *                JB_ERR_PARSE => Parse error (line was trashed anyway)
 *                nonzero => Out of memory (line was trashed anyway)
 *
 *********************************************************************/
jb_err get_actions(char *line,
                   struct action_alias * alias_list,
                   struct action_spec *cur_action)
{
   jb_err err;
   init_action(cur_action);
   cur_action->mask = ACTION_MASK_ALL;

   while (line)
   {
      char * option = NULL;
      char * value = NULL;

      err = get_action_token(&line, &option, &value);
      if (err)
      {
         return err;
      }

      if (option)
      {
         /* handle option in 'option' */

         /* Check for standard action name */
         const struct action_name * action = action_names;

         while ( (action->name != NULL) && (0 != strcmpic(action->name, option)) )
         {
            action++;
         }
         if (action->name != NULL)
         {
            /* Found it */
            cur_action->mask &= action->mask;
            cur_action->add  &= action->mask;
            cur_action->add  |= action->add;

            switch (action->takes_value)
            {
            case AV_NONE:
               /* ignore any option. */
               break;
            case AV_ADD_STRING:
               {
                  /* add single string. */

                  if ((value == NULL) || (*value == '\0'))
                  {
                     if (0 != strcmpic(action->name, "block"))
                     {
                        /*
                         * XXX: Temporary backwards compatibility hack.
                         * XXX: should include line number.
                         */
                        value = "No reason specified.";
                        log_error(LOG_LEVEL_ERROR,
                           "block action without reason found. This may "
                           "become a fatal error in future versions.");
                     }
                     else
                     {
                        return JB_ERR_PARSE;
                     }
                  }
                  /* FIXME: should validate option string here */
                  freez (cur_action->string[action->index]);
                  cur_action->string[action->index] = strdup(value);
                  if (NULL == cur_action->string[action->index])
                  {
                     return JB_ERR_MEMORY;
                  }
                  break;
               }
            case AV_REM_STRING:
               {
                  /* remove single string. */

                  freez (cur_action->string[action->index]);
                  break;
               }
            case AV_ADD_MULTI:
               {
                  /* append multi string. */

                  struct list * remove_p = cur_action->multi_remove[action->index];
                  struct list * add_p    = cur_action->multi_add[action->index];

                  if ((value == NULL) || (*value == '\0'))
                  {
                     return JB_ERR_PARSE;
                  }

                  list_remove_item(remove_p, value);
                  err = enlist_unique(add_p, value, 0);
                  if (err)
                  {
                     return err;
                  }
                  break;
               }
            case AV_REM_MULTI:
               {
                  /* remove multi string. */

                  struct list * remove_p = cur_action->multi_remove[action->index];
                  struct list * add_p    = cur_action->multi_add[action->index];

                  if ( (value == NULL) || (*value == '\0')
                     || ((*value == '*') && (value[1] == '\0')) )
                  {
                     /*
                      * no option, or option == "*".
                      *
                      * Remove *ALL*.
                      */
                     list_remove_all(remove_p);
                     list_remove_all(add_p);
                     cur_action->multi_remove_all[action->index] = 1;
                  }
                  else
                  {
                     /* Valid option - remove only 1 option */

                     if ( !cur_action->multi_remove_all[action->index] )
                     {
                        /* there isn't a catch-all in the remove list already */
                        err = enlist_unique(remove_p, value, 0);
                        if (err)
                        {
                           return err;
                        }
                     }
                     list_remove_item(add_p, value);
                  }
                  break;
               }
            default:
               /* Shouldn't get here unless there's memory corruption. */
               assert(0);
               return JB_ERR_PARSE;
            }
         }
         else
         {
            /* try user aliases. */
            const struct action_alias * alias = alias_list;

            while ( (alias != NULL) && (0 != strcmpic(alias->name, option)) )
            {
               alias = alias->next;
            }
            if (alias != NULL)
            {
               /* Found it */
               merge_actions(cur_action, alias->action);
            }
            else if (((size_t)2 < strlen(option)) && action_used_to_be_valid(option+1))
            {
               log_error(LOG_LEVEL_ERROR, "Action '%s' is no longer valid "
                  "in this Privoxy release. Ignored.", option+1);
            }
            else if (((size_t)2 < strlen(option)) && 0 == strcmpic(option+1, "hide-forwarded-for-headers"))
            {
               log_error(LOG_LEVEL_FATAL, "The action 'hide-forwarded-for-headers' "
                  "is no longer valid in this Privoxy release. "
                  "Use 'change-x-forwarded-for' instead.");
            }
            else
            {
               /* Bad action name */
               /*
                * XXX: This is a fatal error and Privoxy will later on exit
                * in load_one_actions_file() because of an "invalid line".
                *
                * It would be preferable to name the offending option in that
                * error message, but currently there is no way to do that and
                * we have to live with two error messages for basically the
                * same reason.
                */
               log_error(LOG_LEVEL_ERROR, "Unknown action or alias: %s", option);
               return JB_ERR_PARSE;
            }
         }
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  init_current_action
 *
 * Description :  Zero out an action.
 *
 * Parameters  :
 *          1  :  dest = An uninitialized current_action_spec.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void init_current_action (struct current_action_spec *dest)
{
   memset(dest, '\0', sizeof(*dest));

   dest->flags = ACTION_MOST_COMPATIBLE;
}


/*********************************************************************
 *
 * Function    :  init_action
 *
 * Description :  Zero out an action.
 *
 * Parameters  :
 *          1  :  dest = An uninitialized action_spec.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void init_action (struct action_spec *dest)
{
   memset(dest, '\0', sizeof(*dest));
}


/*********************************************************************
 *
 * Function    :  merge_current_action
 *
 * Description :  Merge two actions together.
 *                Similar to "dest += src".
 *                Differences between this and merge_actions()
 *                is that this one doesn't allocate memory for
 *                strings (so "src" better be in memory for at least
 *                as long as "dest" is, and you'd better free
 *                "dest" using "free_current_action").
 *                Also, there is no  mask or remove lists in dest.
 *                (If we're applying it to a URL, we don't need them)
 *
 * Parameters  :
 *          1  :  dest = Current actions, to modify.
 *          2  :  src = Action to add.
 *
 * Returns  0  :  no error
 *        !=0  :  error, probably JB_ERR_MEMORY.
 *
 *********************************************************************/
jb_err merge_current_action (struct current_action_spec *dest,
                             const struct action_spec *src)
{
   int i;
   jb_err err = JB_ERR_OK;

   dest->flags  &= src->mask;
   dest->flags  |= src->add;

   for (i = 0; i < ACTION_STRING_COUNT; i++)
   {
      char * str = src->string[i];
      if (str)
      {
         str = strdup(str);
         if (!str)
         {
            return JB_ERR_MEMORY;
         }
         freez(dest->string[i]);
         dest->string[i] = str;
      }
   }

   for (i = 0; i < ACTION_MULTI_COUNT; i++)
   {
      if (src->multi_remove_all[i])
      {
         /* Remove everything from dest, then add src->multi_add */
         err = list_duplicate(dest->multi[i], src->multi_add[i]);
         if (err)
         {
            return err;
         }
      }
      else
      {
         list_remove_list(dest->multi[i], src->multi_remove[i]);
         err = list_append_list_unique(dest->multi[i], src->multi_add[i]);
         if (err)
         {
            return err;
         }
      }
   }
   return err;
}

#if 0
/*********************************************************************
 *
 * Function    :  update_action_bits_for_all_tags
 *
 * Description :  Updates the action bits based on all matching tags.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  0 if no tag matched, or
 *                1 otherwise
 *
 *********************************************************************/
int update_action_bits_for_all_tags(struct client_state *csp)
{
   struct list_entry *tag;
   int updated = 0;

   for (tag = csp->tags->first; tag != NULL; tag = tag->next)
   {
      if (update_action_bits_for_tag(csp, tag->str))
      {
         updated = 1;
      }
   }

   return updated;
}
#endif

/*********************************************************************
 *
 * Function    :  update_action_bits_for_tag
 *
 * Description :  Updates the action bits based on the action sections
 *                whose tag patterns match a provided tag.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  tag = The tag on which the update should be based on
 *
 * Returns     :  0 if no tag matched, or
 *                1 otherwise
 *
 *********************************************************************/
int update_action_bits_for_tag(struct client_state *csp, const char *tag)
{
   struct file_list *fl;
   struct url_actions *b;

   int updated = 0;
   int i;

   assert(tag);
   assert(list_contains_item(csp->tags, tag));

   /* Run through all action files, */
   for (i = 0; i < MAX_AF_FILES; i++)
   {
      if (((fl = csp->actions_list[i]) == NULL) || ((b = fl->f) == NULL))
      {
         /* Skip empty files */
         continue;
      }

      /* and through all the action patterns, */
      for (b = b->next; NULL != b; b = b->next)
      {
         /* skip the URL patterns, */
         if (NULL == b->url->tag_regex)
         {
            continue;
         }

         /* and check if one of the tag patterns matches the tag, */
         if (0 == regexec(b->url->tag_regex, tag, 0, NULL, 0))
         {
            /* if it does, update the action bit map, */
            if (merge_current_action(csp->action, b->action))
            {
               log_error(LOG_LEVEL_ERROR,
                  "Out of memory while changing action bits");
            }
            /* and signal the change. */
            updated = 1;
         }
      }
   }

   return updated;
}


/*********************************************************************
 *
 * Function    :  free_current_action
 *
 * Description :  Free memory used by a current_action_spec.
 *                Does not free the current_action_spec itself.
 *
 * Parameters  :
 *          1  :  src = Source to free.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void free_current_action (struct current_action_spec *src)
{
   int i;

   for (i = 0; i < ACTION_STRING_COUNT; i++)
   {
      freez(src->string[i]);
   }

   for (i = 0; i < ACTION_MULTI_COUNT; i++)
   {
      destroy_list(src->multi[i]);
   }

   memset(src, '\0', sizeof(*src));
}


static struct file_list *current_actions_file[MAX_AF_FILES]  = {
   NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL
};


#ifdef FEATURE_GRACEFUL_TERMINATION
/*********************************************************************
 *
 * Function    :  unload_current_actions_file
 *
 * Description :  Unloads current actions file - reset to state at
 *                beginning of program.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void unload_current_actions_file(void)
{
   int i;

   for (i = 0; i < MAX_AF_FILES; i++)
   {
      if (current_actions_file[i])
      {
         current_actions_file[i]->unloader = unload_actions_file;
         current_actions_file[i] = NULL;
      }
   }
}
#endif /* FEATURE_GRACEFUL_TERMINATION */


/*********************************************************************
 *
 * Function    :  unload_actions_file
 *
 * Description :  Unloads an actions module.
 *
 * Parameters  :
 *          1  :  file_data = the data structure associated with the
 *                            actions file.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void unload_actions_file(void *file_data)
{
   struct url_actions * next;
   struct url_actions * cur = (struct url_actions *)file_data;
   while (cur != NULL)
   {
      next = cur->next;
      free_url_spec(cur->url);
      if ((next == NULL) || (next->action != cur->action))
      {
         /*
          * As the action settings might be shared,
          * we can only free them if the current
          * url pattern is the last one, or if the
          * next one is using different settings.
          */
         free_action_spec(cur->action);
      }
      freez(cur);
      cur = next;
   }
}


/*********************************************************************
 *
 * Function    :  free_alias_list
 *
 * Description :  Free memory used by a list of aliases.
 *
 * Parameters  :
 *          1  :  alias_list = Linked list to free.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void free_alias_list(struct action_alias *alias_list)
{
   while (alias_list != NULL)
   {
      struct action_alias * next = alias_list->next;
      alias_list->next = NULL;
      freez(alias_list->name);
      free_action(alias_list->action);
      free(alias_list);
      alias_list = next;
   }
}


/*********************************************************************
 *
 * Function    :  load_action_files
 *
 * Description :  Read and parse all the action files and add to files
 *                list.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  0 => Ok, everything else is an error.
 *
 *********************************************************************/
int load_action_files(struct client_state *csp)
{
   int i;
   int result;

   for (i = 0; i < MAX_AF_FILES; i++)
   {
      if (csp->config->actions_file[i])
      {
         result = load_one_actions_file(csp, i);
         if (result)
         {
            return result;
         }
      }
      else if (current_actions_file[i])
      {
         current_actions_file[i]->unloader = unload_actions_file;
         current_actions_file[i] = NULL;
      }
   }

   return 0;
}

/*********************************************************************
 *
 * Function    :  load_one_actions_file
 *
 * Description :  Read and parse a action file and add to files
 *                list.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  fileid = File index to load.
 *
 * Returns     :  0 => Ok, everything else is an error.
 *
 *********************************************************************/
static int load_one_actions_file(struct client_state *csp, int fileid)
{

   /*
    * Parser mode.
    * Note: Keep these in the order they occur in the file, they are
    * sometimes tested with <=
    */
#define MODE_START_OF_FILE 1
#define MODE_SETTINGS      2
#define MODE_DESCRIPTION   3
#define MODE_ALIAS         4
#define MODE_ACTIONS       5

   int mode = MODE_START_OF_FILE;

   FILE *fp;
   struct url_actions *last_perm;
   struct url_actions *perm;
   char  buf[BUFFER_SIZE];
   struct file_list *fs;
   struct action_spec * cur_action = NULL;
   int cur_action_used = 0;
   struct action_alias * alias_list = NULL;
   unsigned long linenum = 0;

   if (!check_file_changed(current_actions_file[fileid], csp->config->actions_file[fileid], &fs))
   {
      /* No need to load */
      csp->actions_list[fileid] = current_actions_file[fileid];
      return 0;
   }
   if (!fs)
   {
      log_error(LOG_LEVEL_FATAL, "can't load actions file '%s': %E. "
         "Note that beginning with Privoxy 3.0.7, actions files have to be specified "
         "with their complete file names.", csp->config->actions_file[fileid]);
      return 1; /* never get here */
   }

   fs->f = last_perm = (struct url_actions *)zalloc(sizeof(*last_perm));
   if (last_perm == NULL)
   {
      log_error(LOG_LEVEL_FATAL, "can't load actions file '%s': out of memory!",
                csp->config->actions_file[fileid]);
      return 1; /* never get here */
   }

   if ((fp = fopen(csp->config->actions_file[fileid], "r")) == NULL)
   {
      log_error(LOG_LEVEL_FATAL, "can't load actions file '%s': error opening file: %E",
                csp->config->actions_file[fileid]);
      return 1; /* never get here */
   }

   while (read_config_line(buf, sizeof(buf), fp, &linenum) != NULL)
   {
      if (*buf == '{')
      {
         /* It's a header block */
         if (buf[1] == '{')
         {
            /* It's {{settings}} or {{alias}} */
            size_t len = strlen(buf);
            char * start = buf + 2;
            char * end = buf + len - 1;
            if ((len < (size_t)5) || (*end-- != '}') || (*end-- != '}'))
            {
               /* too short */
               fclose(fp);
               log_error(LOG_LEVEL_FATAL,
                  "can't load actions file '%s': invalid line (%lu): %s", 
                  csp->config->actions_file[fileid], linenum, buf);
               return 1; /* never get here */
            }

            /* Trim leading and trailing whitespace. */
            end[1] = '\0';
            chomp(start);

            if (*start == '\0')
            {
               /* too short */
               fclose(fp);
               log_error(LOG_LEVEL_FATAL,
                  "can't load actions file '%s': invalid line (%lu): {{ }}",
                  csp->config->actions_file[fileid], linenum);
               return 1; /* never get here */
            }

            /*
             * An actionsfile can optionally contain the following blocks.
             * They *MUST* be in this order, to simplify processing:
             *
             * {{settings}}
             * name=value...
             *
             * {{description}}
             * ...free text, format TBD, but no line may start with a '{'...
             *
             * {{alias}}
             * name=actions...
             *
             * The actual actions must be *after* these special blocks.
             * None of these special blocks may be repeated.
             *
             */
            if (0 == strcmpic(start, "settings"))
            {
               /* it's a {{settings}} block */
               if (mode >= MODE_SETTINGS)
               {
                  /* {{settings}} must be first thing in file and must only
                   * appear once.
                   */
                  fclose(fp);
                  log_error(LOG_LEVEL_FATAL,
                     "can't load actions file '%s': line %lu: {{settings}} must only appear once, and it must be before anything else.",
                     csp->config->actions_file[fileid], linenum);
               }
               mode = MODE_SETTINGS;
            }
            else if (0 == strcmpic(start, "description"))
            {
               /* it's a {{description}} block */
               if (mode >= MODE_DESCRIPTION)
               {
                  /* {{description}} is a singleton and only {{settings}} may proceed it
                   */
                  fclose(fp);
                  log_error(LOG_LEVEL_FATAL,
                     "can't load actions file '%s': line %lu: {{description}} must only appear once, and only a {{settings}} block may be above it.",
                     csp->config->actions_file[fileid], linenum);
               }
               mode = MODE_DESCRIPTION;
            }
            else if (0 == strcmpic(start, "alias"))
            {
               /* it's an {{alias}} block */
               if (mode >= MODE_ALIAS)
               {
                  /* {{alias}} must be first thing in file, possibly after
                   * {{settings}} and {{description}}
                   *
                   * {{alias}} must only appear once.
                   *
                   * Note that these are new restrictions introduced in
                   * v2.9.10 in order to make actionsfile editing simpler.
                   * (Otherwise, reordering actionsfile entries without
                   * completely rewriting the file becomes non-trivial)
                   */
                  fclose(fp);
                  log_error(LOG_LEVEL_FATAL,
                     "can't load actions file '%s': line %lu: {{alias}} must only appear once, and it must be before all actions.",
                     csp->config->actions_file[fileid], linenum);
               }
               mode = MODE_ALIAS;
            }
            else
            {
               /* invalid {{something}} block */
               fclose(fp);
               log_error(LOG_LEVEL_FATAL,
                  "can't load actions file '%s': invalid line (%lu): {{%s}}",
                  csp->config->actions_file[fileid], linenum, start);
               return 1; /* never get here */
            }
         }
         else
         {
            /* It's an actions block */

            char  actions_buf[BUFFER_SIZE];
            char * end;

            /* set mode */
            mode    = MODE_ACTIONS;

            /* free old action */
            if (cur_action)
            {
               if (!cur_action_used)
               {
                  free_action_spec(cur_action);
               }
               cur_action = NULL;
            }
            cur_action_used = 0;
            cur_action = (struct action_spec *)zalloc(sizeof(*cur_action));
            if (cur_action == NULL)
            {
               fclose(fp);
               log_error(LOG_LEVEL_FATAL,
                  "can't load actions file '%s': out of memory",
                  csp->config->actions_file[fileid]);
               return 1; /* never get here */
            }
            init_action(cur_action);

            /* trim { */
            strlcpy(actions_buf, buf + 1, sizeof(actions_buf));

            /* check we have a trailing } and then trim it */
            end = actions_buf + strlen(actions_buf) - 1;
            if (*end != '}')
            {
               /* No closing } */
               fclose(fp);
               log_error(LOG_LEVEL_FATAL,
                  "can't load actions file '%s': invalid line (%lu): %s",
                  csp->config->actions_file[fileid], linenum, buf);
               return 1; /* never get here */
            }
            *end = '\0';

            /* trim any whitespace immediately inside {} */
            chomp(actions_buf);

            if (get_actions(actions_buf, alias_list, cur_action))
            {
               /* error */
               fclose(fp);
               log_error(LOG_LEVEL_FATAL,
                  "can't load actions file '%s': invalid line (%lu): %s",
                  csp->config->actions_file[fileid], linenum, buf);
               return 1; /* never get here */
            }
         }
      }
      else if (mode == MODE_SETTINGS)
      {
         /*
          * Part of the {{settings}} block.
          * For now only serves to check if the file's minimum Privoxy
          * version requirement is met, but we may want to read & check
          * permissions when we go multi-user.
          */
         if (!strncmp(buf, "for-privoxy-version=", 20))
         {
            char *version_string, *fields[3];
            int num_fields;

            if ((version_string = strdup(buf + 20)) == NULL)
            {
               fclose(fp);
               log_error(LOG_LEVEL_FATAL,
                         "can't load actions file '%s': out of memory!",
                         csp->config->actions_file[fileid]);
               return 1; /* never get here */
            }
            
            num_fields = ssplit(version_string, ".", fields, 3, TRUE, FALSE);

            if (num_fields < 1 || atoi(fields[0]) == 0)
            {
               log_error(LOG_LEVEL_ERROR,
                 "While loading actions file '%s': invalid line (%lu): %s",
                  csp->config->actions_file[fileid], linenum, buf);
            }
            else if (                      atoi(fields[0]) > VERSION_MAJOR
                     || (num_fields > 1 && atoi(fields[1]) > VERSION_MINOR)
                     || (num_fields > 2 && atoi(fields[2]) > VERSION_POINT))
            {
               fclose(fp);
               log_error(LOG_LEVEL_FATAL,
                         "Actions file '%s', line %lu requires newer Privoxy version: %s",
                         csp->config->actions_file[fileid], linenum, buf );
               return 1; /* never get here */
            }
            free(version_string);
         }
      }
      else if (mode == MODE_DESCRIPTION)
      {
         /*
          * Part of the {{description}} block.
          * Ignore for now.
          */
      }
      else if (mode == MODE_ALIAS)
      {
         /*
          * define an alias
          */
         char  actions_buf[BUFFER_SIZE];
         struct action_alias * new_alias;

         char * start = strchr(buf, '=');
         char * end = start;

         if ((start == NULL) || (start == buf))
         {
            log_error(LOG_LEVEL_FATAL,
               "can't load actions file '%s': invalid alias line (%lu): %s",
               csp->config->actions_file[fileid], linenum, buf);
            return 1; /* never get here */
         }

         if ((new_alias = zalloc(sizeof(*new_alias))) == NULL)
         {
            fclose(fp);
            log_error(LOG_LEVEL_FATAL,
               "can't load actions file '%s': out of memory!",
               csp->config->actions_file[fileid]);
            return 1; /* never get here */
         }

         /* Eat any the whitespace before the '=' */
         end--;
         while ((*end == ' ') || (*end == '\t'))
         {
            /*
             * we already know we must have at least 1 non-ws char
             * at start of buf - no need to check
             */
            end--;
         }
         end[1] = '\0';

         /* Eat any the whitespace after the '=' */
         start++;
         while ((*start == ' ') || (*start == '\t'))
         {
            start++;
         }
         if (*start == '\0')
         {
            log_error(LOG_LEVEL_FATAL,
               "can't load actions file '%s': invalid alias line (%lu): %s",
               csp->config->actions_file[fileid], linenum, buf);
            return 1; /* never get here */
         }

         if ((new_alias->name = strdup(buf)) == NULL)
         {
            fclose(fp);
            log_error(LOG_LEVEL_FATAL,
               "can't load actions file '%s': out of memory!",
               csp->config->actions_file[fileid]);
            return 1; /* never get here */
         }

         strlcpy(actions_buf, start, sizeof(actions_buf));

         if (get_actions(actions_buf, alias_list, new_alias->action))
         {
            /* error */
            fclose(fp);
            log_error(LOG_LEVEL_FATAL,
               "can't load actions file '%s': invalid alias line (%lu): %s = %s",
               csp->config->actions_file[fileid], linenum, buf, start);
            return 1; /* never get here */
         }

         /* add to list */
         new_alias->next = alias_list;
         alias_list = new_alias;
      }
      else if (mode == MODE_ACTIONS)
      {
         /* it's a URL pattern */

         /* allocate a new node */
         if ((perm = zalloc(sizeof(*perm))) == NULL)
         {
            fclose(fp);
            log_error(LOG_LEVEL_FATAL,
               "can't load actions file '%s': out of memory!",
               csp->config->actions_file[fileid]);
            return 1; /* never get here */
         }

         perm->action = cur_action;
         cur_action_used = 1;

         /* Save the URL pattern */
         if (create_url_spec(perm->url, buf))
         {
            fclose(fp);
            log_error(LOG_LEVEL_FATAL,
               "can't load actions file '%s': line %lu: cannot create URL pattern from: %s",
               csp->config->actions_file[fileid], linenum, buf);
            return 1; /* never get here */
         }

         /* add it to the list */
         last_perm->next = perm;
         last_perm = perm;
      }
      else if (mode == MODE_START_OF_FILE)
      {
         /* oops - please have a {} line as 1st line in file. */
         fclose(fp);
         log_error(LOG_LEVEL_FATAL,
            "can't load actions file '%s': first needed line (%lu) is invalid: %s",
            csp->config->actions_file[fileid], linenum, buf);
         return 1; /* never get here */
      }
      else
      {
         /* How did we get here? This is impossible! */
         fclose(fp);
         log_error(LOG_LEVEL_FATAL,
            "can't load actions file '%s': INTERNAL ERROR - mode = %d",
            csp->config->actions_file[fileid], mode);
         return 1; /* never get here */
      }
   }

   fclose(fp);

   if (!cur_action_used)
   {
      free_action_spec(cur_action);
   }
   free_alias_list(alias_list);

   /* the old one is now obsolete */
   if (current_actions_file[fileid])
   {
      current_actions_file[fileid]->unloader = unload_actions_file;
   }

   fs->next    = files->next;
   files->next = fs;
   current_actions_file[fileid] = fs;

   csp->actions_list[fileid] = fs;

   return(0);

}


/*********************************************************************
 *
 * Function    :  actions_to_text
 *
 * Description :  Converts a actionsfile entry from the internal
 *                structure into a text line.  The output is split
 *                into one line for each action with line continuation. 
 *
 * Parameters  :
 *          1  :  action = The action to format.
 *
 * Returns     :  A string.  Caller must free it.
 *                NULL on out-of-memory error.
 *
 *********************************************************************/
char * actions_to_text(const struct action_spec *action)
{
   unsigned long mask = action->mask;
   unsigned long add  = action->add;
   char *result = strdup("");
   struct list_entry * lst;

   /* sanity - prevents "-feature +feature" */
   mask |= add;


#define DEFINE_ACTION_BOOL(__name, __bit)          \
   if (!(mask & __bit))                            \
   {                                               \
      string_append(&result, " -" __name " \\\n"); \
   }                                               \
   else if (add & __bit)                           \
   {                                               \
      string_append(&result, " +" __name " \\\n"); \
   }

#define DEFINE_ACTION_STRING(__name, __bit, __index)   \
   if (!(mask & __bit))                                \
   {                                                   \
      string_append(&result, " -" __name " \\\n");     \
   }                                                   \
   else if (add & __bit)                               \
   {                                                   \
      string_append(&result, " +" __name "{");         \
      string_append(&result, action->string[__index]); \
      string_append(&result, "} \\\n");                \
   }

#define DEFINE_ACTION_MULTI(__name, __index)         \
   if (action->multi_remove_all[__index])            \
   {                                                 \
      string_append(&result, " -" __name " \\\n");   \
   }                                                 \
   else                                              \
   {                                                 \
      lst = action->multi_remove[__index]->first;    \
      while (lst)                                    \
      {                                              \
         string_append(&result, " -" __name "{");    \
         string_append(&result, lst->str);           \
         string_append(&result, "} \\\n");           \
         lst = lst->next;                            \
      }                                              \
   }                                                 \
   lst = action->multi_add[__index]->first;          \
   while (lst)                                       \
   {                                                 \
      string_append(&result, " +" __name "{");       \
      string_append(&result, lst->str);              \
      string_append(&result, "} \\\n");              \
      lst = lst->next;                               \
   }

#define DEFINE_ACTION_ALIAS 0 /* No aliases for output */

#include "actionlist.h"

#undef DEFINE_ACTION_MULTI
#undef DEFINE_ACTION_STRING
#undef DEFINE_ACTION_BOOL
#undef DEFINE_ACTION_ALIAS

   return result;
}


/*********************************************************************
 *
 * Function    :  actions_to_html
 *
 * Description :  Converts a actionsfile entry from numeric form
 *                ("mask" and "add") to a <br>-seperated HTML string
 *                in which each action is linked to its chapter in
 *                the user manual.
 *
 * Parameters  :
 *          1  :  csp    = Client state (for config)
 *          2  :  action = Action spec to be converted
 *
 * Returns     :  A string.  Caller must free it.
 *                NULL on out-of-memory error.
 *
 *********************************************************************/
char * actions_to_html(const struct client_state *csp,
                       const struct action_spec *action)
{
   unsigned long mask = action->mask;
   unsigned long add  = action->add;
   char *result = strdup("");
   struct list_entry * lst;

   /* sanity - prevents "-feature +feature" */
   mask |= add;


#define DEFINE_ACTION_BOOL(__name, __bit)       \
   if (!(mask & __bit))                         \
   {                                            \
      string_append(&result, "\n<br>-");        \
      string_join(&result, add_help_link(__name, csp->config)); \
   }                                            \
   else if (add & __bit)                        \
   {                                            \
      string_append(&result, "\n<br>+");        \
      string_join(&result, add_help_link(__name, csp->config)); \
   }

#define DEFINE_ACTION_STRING(__name, __bit, __index) \
   if (!(mask & __bit))                              \
   {                                                 \
      string_append(&result, "\n<br>-");             \
      string_join(&result, add_help_link(__name, csp->config)); \
   }                                                 \
   else if (add & __bit)                             \
   {                                                 \
      string_append(&result, "\n<br>+");             \
      string_join(&result, add_help_link(__name, csp->config)); \
      string_append(&result, "{");                   \
      string_join(&result, html_encode(action->string[__index])); \
      string_append(&result, "}");                   \
   }

#define DEFINE_ACTION_MULTI(__name, __index)          \
   if (action->multi_remove_all[__index])             \
   {                                                  \
      string_append(&result, "\n<br>-");              \
      string_join(&result, add_help_link(__name, csp->config)); \
   }                                                  \
   else                                               \
   {                                                  \
      lst = action->multi_remove[__index]->first;     \
      while (lst)                                     \
      {                                               \
         string_append(&result, "\n<br>-");           \
         string_join(&result, add_help_link(__name, csp->config)); \
         string_append(&result, "{");                 \
         string_join(&result, html_encode(lst->str)); \
         string_append(&result, "}");                 \
         lst = lst->next;                             \
      }                                               \
   }                                                  \
   lst = action->multi_add[__index]->first;           \
   while (lst)                                        \
   {                                                  \
      string_append(&result, "\n<br>+");              \
      string_join(&result, add_help_link(__name, csp->config)); \
      string_append(&result, "{");                    \
      string_join(&result, html_encode(lst->str));    \
      string_append(&result, "}");                    \
      lst = lst->next;                                \
   }

#define DEFINE_ACTION_ALIAS 0 /* No aliases for output */

#include "actionlist.h"

#undef DEFINE_ACTION_MULTI
#undef DEFINE_ACTION_STRING
#undef DEFINE_ACTION_BOOL
#undef DEFINE_ACTION_ALIAS

   /* trim leading <br> */
   if (result && *result)
   {
      char * s = result;
      result = strdup(result + 5);
      free(s);
   }

   return result;
}


/*********************************************************************
 *
 * Function    :  current_actions_to_html
 *
 * Description :  Converts a curren action spec to a <br> seperated HTML
 *                text in which each action is linked to its chapter in
 *                the user manual.
 *
 * Parameters  :
 *          1  :  csp    = Client state (for config) 
 *          2  :  action = Current action spec to be converted
 *
 * Returns     :  A string.  Caller must free it.
 *                NULL on out-of-memory error.
 *
 *********************************************************************/
char *current_action_to_html(const struct client_state *csp,
                             const struct current_action_spec *action)
{
   unsigned long flags  = action->flags;
   struct list_entry * lst;
   char *result   = strdup("");
   char *active   = strdup("");
   char *inactive = strdup("");

#define DEFINE_ACTION_BOOL(__name, __bit)  \
   if (flags & __bit)                      \
   {                                       \
      string_append(&active, "\n<br>+");   \
      string_join(&active, add_help_link(__name, csp->config)); \
   }                                       \
   else                                    \
   {                                       \
      string_append(&inactive, "\n<br>-"); \
      string_join(&inactive, add_help_link(__name, csp->config)); \
   }

#define DEFINE_ACTION_STRING(__name, __bit, __index)   \
   if (flags & __bit)                                  \
   {                                                   \
      string_append(&active, "\n<br>+");               \
      string_join(&active, add_help_link(__name, csp->config)); \
      string_append(&active, "{");                     \
      string_join(&active, html_encode(action->string[__index])); \
      string_append(&active, "}");                     \
   }                                                   \
   else                                                \
   {                                                   \
      string_append(&inactive, "\n<br>-");             \
      string_join(&inactive, add_help_link(__name, csp->config)); \
   }

#define DEFINE_ACTION_MULTI(__name, __index)           \
   lst = action->multi[__index]->first;                \
   if (lst == NULL)                                    \
   {                                                   \
      string_append(&inactive, "\n<br>-");             \
      string_join(&inactive, add_help_link(__name, csp->config)); \
   }                                                   \
   else                                                \
   {                                                   \
      while (lst)                                      \
      {                                                \
         string_append(&active, "\n<br>+");            \
         string_join(&active, add_help_link(__name, csp->config)); \
         string_append(&active, "{");                  \
         string_join(&active, html_encode(lst->str));  \
         string_append(&active, "}");                  \
         lst = lst->next;                              \
      }                                                \
   }

#define DEFINE_ACTION_ALIAS 0 /* No aliases for output */

#include "actionlist.h"

#undef DEFINE_ACTION_MULTI
#undef DEFINE_ACTION_STRING
#undef DEFINE_ACTION_BOOL
#undef DEFINE_ACTION_ALIAS

   if (active != NULL)
   {
      string_append(&result, active);
      freez(active);
   }
   string_append(&result, "\n<br>");
   if (inactive != NULL)
   {
      string_append(&result, inactive);
      freez(inactive);
   }
   return result;
}
