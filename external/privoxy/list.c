const char list_rcs[] = "$Id: list.c,v 1.20 2007/05/14 16:56:07 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/list.c,v $
 *
 * Purpose     :  Declares functions to handle lists.
 *                Functions declared include:
 *                   `destroy_list', `enlist' and `list_to_text'
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
 *    $Log: list.c,v $
 *    Revision 1.20  2007/05/14 16:56:07  fabiankeil
 *    - Stop using strcpy().
 *    - enlist_unique_header() now behaves as advertised
 *      and checks for existing headers with the same name
 *      but ignores the values.
 *
 *    Revision 1.19  2007/04/17 18:14:06  fabiankeil
 *    Add list_contains_item().
 *
 *    Revision 1.18  2006/12/28 19:21:23  fabiankeil
 *    Fixed gcc43 warning and enabled list_is_valid()'s loop
 *    detection again. It was ineffective since the removal of
 *    the arbitrary list length limit two years ago.
 *
 *    Revision 1.17  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.15.2.2  2004/05/25 02:04:23  david__schmidt
 *    Removed the "arbitrary" 1000 filter limit in file.c.  See tracker #911950.
 *
 *    Revision 1.15.2.1  2002/11/28 18:14:54  oes
 *    Added unmap function that removes all items with a given
 *    name from a map.
 *
 *    Revision 1.15  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.14  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.13  2002/03/07 03:46:17  oes
 *    Fixed compiler warnings
 *
 *    Revision 1.12  2001/10/25 03:40:48  david__schmidt
 *    Change in porting tactics: OS/2's EMX porting layer doesn't allow multiple
 *    threads to call select() simultaneously.  So, it's time to do a real, live,
 *    native OS/2 port.  See defines for __EMX__ (the porting layer) vs. __OS2__
 *    (native). Both versions will work, but using __OS2__ offers multi-threading.
 *
 *    Revision 1.11  2001/10/23 21:21:03  jongfoster
 *    New error handling - error codes are now jb_errs, not ints.
 *    Changed the way map() handles out-of-memory, to dramatically
 *    reduce the amount of error-checking clutter needed.
 *
 *    Revision 1.10  2001/09/16 17:30:24  jongfoster
 *    Fixing a compiler warning.
 *
 *    Revision 1.9  2001/09/16 13:20:29  jongfoster
 *    Rewrite of list library.  Now has seperate header and list_entry
 *    structures.  Also added a large sprinking of assert()s to the list
 *    code.
 *
 *    Revision 1.8  2001/08/07 14:00:20  oes
 *    Fixed comment
 *
 *    Revision 1.7  2001/08/05 16:06:20  jongfoster
 *    Modifiying "struct map" so that there are now separate header and
 *    "map_entry" structures.  This means that functions which modify a
 *    map no longer need to return a pointer to the modified map.
 *    Also, it no longer reverses the order of the entries (which may be
 *    important with some advanced template substitutions).
 *
 *    Revision 1.6  2001/07/31 14:44:51  oes
 *    list_to_text() now appends empty line at end
 *
 *    Revision 1.5  2001/06/29 21:45:41  oes
 *    Indentation, CRLF->LF, Tab-> Space
 *
 *    Revision 1.4  2001/06/29 13:30:22  oes
 *    - Added Convenience function enlist_unique_header(),
 *      which takes the Header name and value as separate
 *      arguments and thus saves the pain of sprintf()ing
 *      and determining the Header name length to enlist_unique
 *    - Improved comments
 *    - Removed logentry from cancelled commit
 *
 *    Revision 1.3  2001/06/03 19:12:24  oes
 *    functions for new struct map, extended enlist_unique
 *
 *    Revision 1.2  2001/06/01 18:49:17  jongfoster
 *    Replaced "list_share" with "list" - the tiny memory gain was not
 *    worth the extra complexity.
 *
 *    Revision 1.1  2001/05/31 21:11:53  jongfoster
 *    - Moved linked list support to new "list.c" file.
 *      Structure definitions are still in project.h,
 *      function prototypes are now in "list.h".
 *    - Added support for "struct list_share", which is identical
 *      to "struct list" except it saves memory by not duplicating
 *      the strings.  Obviously, this only works if there is some
 *      other way of managing the memory used by the strings.
 *      (These list_share lists are used for lists which last
 *      for only 1 request, and where all the list entries are
 *      just coming directly from entries in the actionsfile.)
 *      Note that you still need to destroy list_share lists
 *      properly to free the nodes - it's only the strings
 *      which are shared.
 *
 *
 *********************************************************************/


#include "config.h"

#ifndef _WIN32
/* FIXME: The following headers are not needed for Win32.  Are they
 * needed on other platforms?
 */
#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>
#include <ctype.h>
#endif
#include <string.h>

#if !defined(_WIN32) && !defined(__OS2__)
#include <unistd.h>
#endif

#include <assert.h>

#include "project.h"
#include "list.h"
#include "miscutil.h"

const char list_h_rcs[] = LIST_H_VERSION;


static int list_is_valid (const struct list *the_list);


/*********************************************************************
 *
 * Function    :  init_list
 *
 * Description :  Create a new, empty list in user-allocated memory.
 *                Caller should allocate a "struct list" variable,
 *                then pass it to this function.
 *                (Implementation note:  Rather than calling this
 *                function, you can also just memset the memory to
 *                zero, e.g. if you have a larger structure you
 *                want to initialize quickly.  However, that isn't
 *                really good design.)
 *
 * Parameters  :
 *          1  :  the_list = pointer to list
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void init_list(struct list *the_list)
{
   memset(the_list, '\0', sizeof(*the_list));
}


/*********************************************************************
 *
 * Function    :  destroy_list
 *
 * Description :  Destroy a string list (opposite of list_init).
 *                On return, the memory used by the list entries has
 *                been freed, but not the memory used by the_list
 *                itself.  You should not re-use the_list without
 *                calling list_init().
 *
 *                (Implementation note:  You *can* reuse the_list
 *                without calling list_init(), but please don't.
 *                If you want to remove all entries from a list
 *                and still have a usable list, then use
 *                list_remove_all().)
 *
 * Parameters  :
 *          1  :  the_list = pointer to list
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void destroy_list (struct list *the_list)
{
   struct list_entry *cur_entry, *next_entry;

   assert(the_list);

   for (cur_entry = the_list->first; cur_entry ; cur_entry = next_entry)
   {
      next_entry = cur_entry->next;
      freez(cur_entry->str);
      free(cur_entry);
   }

   the_list->first = NULL;
   the_list->last = NULL;
}


/*********************************************************************
 *
 * Function    :  list_is_valid
 *
 * Description :  Check that a string list is valid.  The intended
 *                usage is "assert(list_is_valid(the_list))".
 *                Currently this checks that "the_list->last"
 *                is correct, and that the list dosn't contain
 *                circular references.  It is likely to crash if
 *                it's passed complete garbage.
 *
 * Parameters  :
 *          1  :  the_list = pointer to list.  Must be non-null.
 *
 * Returns     :  1 if list is valid, 0 otherwise.
 *
 *********************************************************************/
static int list_is_valid (const struct list *the_list)
{
   /*
    * If you don't want this check, just change the line below
    * from "#if 1" to "#if 0".
    */
#if 1
   const struct list_entry *cur_entry;
   const struct list_entry *last_entry = NULL;
   int entry = 0;

   assert(the_list);

   for (cur_entry = the_list->first; cur_entry ; cur_entry = cur_entry->next)
   {
      last_entry = cur_entry;

      if (cur_entry->str)
      {
         /*
          * Just check that this string can be accessed - i.e. it's a valid
          * pointer.
          */
         (void)strlen(cur_entry->str);
      }

      /*
       * Check for looping back to first
       */
      if ((entry++ != 0) && (cur_entry == the_list->first))
      {
         return 0;
      }

      /*
       * Arbitrarily limit list length to prevent infinite loops.
       * Note that the 1000 limit was hit by a real user in tracker 911950;
       * removing it for now.  Real circular references should eventually
       * be caught by the check above, anyway.
       */         
      /*
      if (entry > 1000)
      {           
         return 0;
      }           
      */          

      /*
       * Check this isn't marked as the last entry, unless of course it's
       * *really* the last entry.
       */
      if ((the_list->last == cur_entry) && (cur_entry->next != NULL))
      {
         /* This is the last entry, but there's data after it !!?? */
         return 0;
      }
   }

   return (the_list->last == last_entry);
#else
   return 1;
#endif
}

/*********************************************************************
 *
 * Function    :  enlist
 *
 * Description :  Append a string into a specified string list.
 *
 * Parameters  :
 *          1  :  the_list = pointer to list
 *          2  :  str = string to add to the list (maybe NULL)
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *                On error, the_list will be unchanged.
 *
 *********************************************************************/
jb_err enlist(struct list *the_list, const char *str)
{
   struct list_entry *cur;

   assert(the_list);
   assert(list_is_valid(the_list));

   if (NULL == (cur = (struct list_entry *)zalloc(sizeof(*cur))))
   {
      return JB_ERR_MEMORY;
   }

   if (str)
   {
      if (NULL == (cur->str = strdup(str)))
      {
         free(cur);
         return JB_ERR_MEMORY;
      }
   }
   /* else { cur->str = NULL; }  - implied by zalloc */

   /* cur->next = NULL;  - implied by zalloc */

   if (the_list->last)
   {
      the_list->last->next = cur;
      the_list->last = cur;
   }
   else
   {
      the_list->first = cur;
      the_list->last = cur;
   }

   assert(list_is_valid(the_list));
   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  enlist_first
 *
 * Description :  Append a string as first element into a specified
 *                string list.
 *
 * Parameters  :
 *          1  :  the_list = pointer to list
 *          2  :  str = string to add to the list (maybe NULL)
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *                On error, the_list will be unchanged.
 *
 *********************************************************************/
jb_err enlist_first(struct list *the_list, const char *str)
{
   struct list_entry *cur;

   assert(the_list);
   assert(list_is_valid(the_list));

   if (NULL == (cur = (struct list_entry *)zalloc(sizeof(*cur))))
   {
      return JB_ERR_MEMORY;
   }

   if (str)
   {
      if (NULL == (cur->str = strdup(str)))
      {
         free(cur);
         return JB_ERR_MEMORY;
      }
   }
   /* else { cur->str = NULL; }  - implied by zalloc */

   cur->next = the_list->first;

   the_list->first = cur;
   if (the_list->last == NULL)
   {
      the_list->last = cur;
   }

   assert(list_is_valid(the_list));
   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  enlist_unique
 *
 * Description :  Append a string into a specified string list,
 *                if & only if it's not there already.
 *                If the num_significant_chars argument is nonzero,
 *                only compare up to the nth character.
 *
 * Parameters  :
 *          1  :  the_list = pointer to list
 *          2  :  str = string to add to the list
 *          3  :  num_significant_chars = number of chars to use
 *                for uniqueness test, or 0 to require an exact match.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *                On error, the_list will be unchanged.
 *                "Success" does not indicate whether or not the
 *                item was already in the list.
 *
 *********************************************************************/
jb_err enlist_unique(struct list *the_list, const char *str,
                     size_t num_significant_chars)
{
   struct list_entry *cur_entry;

   assert(the_list);
   assert(list_is_valid(the_list));
   assert(str);
   assert(num_significant_chars >= 0);
   assert(num_significant_chars <= strlen(str));

   if (num_significant_chars > 0)
   {
      for (cur_entry = the_list->first; cur_entry != NULL; cur_entry = cur_entry->next)
      {
         if ( (cur_entry->str != NULL)
           && (0 == strncmp(str, cur_entry->str, num_significant_chars)))
         {
            /* Already there */
            return JB_ERR_OK;
         }
      }
   }
   else
   {
      /* Test whole string */
      for (cur_entry = the_list->first; cur_entry != NULL; cur_entry = cur_entry->next)
      {
         if ( (cur_entry->str != NULL) && (0 == strcmp(str, cur_entry->str)))
         {
            /* Already there */
            return JB_ERR_OK;
         }
      }
   }

   return enlist(the_list, str);
}


/*********************************************************************
 *
 * Function    :  enlist_unique_header
 *
 * Description :  Make a HTTP header from the two strings name and value,
 *                and append the result into a specified string list,
 *                if & only if there isn't already a header with that name.
 *
 * Parameters  :
 *          1  :  the_list = pointer to list
 *          2  :  name = HTTP header name (e.g. "Content-type")
 *          3  :  value = HTTP header value (e.g. "text/html")
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *                On error, the_list will be unchanged.
 *                "Success" does not indicate whether or not the
 *                header was already in the list.
 *
 *********************************************************************/
jb_err enlist_unique_header(struct list *the_list, const char *name,
                            const char *value)
{
   jb_err result = JB_ERR_MEMORY;
   char *header;
   size_t header_size;

   assert(the_list);
   assert(list_is_valid(the_list));
   assert(name);
   assert(value);

   /* + 2 for the ': ', + 1 for the \0 */
   header_size = strlen(name) + 2 + strlen(value) + 1;
   header = (char *)malloc(header_size);

   if (NULL != header)
   {
      const size_t bytes_to_compare = strlen(name) + 2;

      snprintf(header, header_size, "%s: %s", name, value);
      result = enlist_unique(the_list, header, bytes_to_compare);
      free(header);
      assert(list_is_valid(the_list));
   }

   return result;

}


/*********************************************************************
 *
 * Function    :  list_remove_all
 *
 * Description :  Remove all entries from a list.  On return, the_list
 *                is a valid, empty list.  Note that this is similar
 *                to destroy_list(), but the difference is that this
 *                function guarantees that the list structure is still
 *                valid after the call.
 *
 * Parameters  :
 *          1  :  the_list = pointer to list
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void list_remove_all(struct list *the_list)
{
   struct list_entry *cur_entry;
   struct list_entry *next_entry;

   assert(the_list);
   assert(list_is_valid(the_list));

   for (cur_entry = the_list->first; cur_entry ; cur_entry = next_entry)
   {
      next_entry = cur_entry->next;
      freez(cur_entry->str);
      free(cur_entry);
   }

   the_list->first = the_list->last = NULL;

   assert(list_is_valid(the_list));
}


/*********************************************************************
 *
 * Function    :  list_to_text
 *
 * Description :  "Flatten" a string list into 1 long \r\n delimited string,
 *                adding an empty line at the end.  NULL entries are ignored.
 *                This function does not change the_list.
 *
 *                XXX: Should probably be renamed as it's only
 *                useful (and used) to flatten header lists.
 *
 * Parameters  :
 *          1  :  the_list = pointer to list
 *
 * Returns     :  NULL on malloc error, else new long string.
 *                Caller must free() it.
 *
 *********************************************************************/
char *list_to_text(const struct list *the_list)
{
   struct list_entry *cur_entry;
   char *text;
   size_t text_length;
   char *cursor;
   size_t bytes_left;

   assert(the_list);
   assert(list_is_valid(the_list));

   /*
    * Calculate the lenght of the final text.
    * '2' because of the '\r\n' at the end of
    * each string and at the end of the text.
    */
   text_length = 2;
   for (cur_entry = the_list->first; cur_entry; cur_entry = cur_entry->next)
   {
      if (cur_entry->str)
      {
         text_length += strlen(cur_entry->str) + 2;
      }
   }

   bytes_left = text_length + 1;

   text = (char *)malloc(bytes_left);
   if (NULL == text)
   {
      return NULL;
   }

   cursor = text;

   for (cur_entry = the_list->first; cur_entry; cur_entry = cur_entry->next)
   {
      if (cur_entry->str)
      {
         const int written = snprintf(cursor, bytes_left, "%s\r\n", cur_entry->str);

         assert(written > 0);
         assert(written < bytes_left);

         bytes_left -= (size_t)written;
         cursor += (size_t)written;
      }
   }

   assert(bytes_left == 3);

   *cursor++ = '\r';
   *cursor++ = '\n';
   *cursor   = '\0';

   assert(text_length == cursor - text);
   assert(text[text_length] == '\0');

   return text;
}


/*********************************************************************
 *
 * Function    :  list_remove_item
 *
 * Description :  Remove a string from a specified string list.
 *
 * Parameters  :
 *          1  :  the_list = pointer to list
 *          2  :  str = string to remove from the list - non-NULL
 *
 * Returns     :  Number of times it was removed.
 *
 *********************************************************************/
int list_remove_item(struct list *the_list, const char *str)
{
   struct list_entry *prev = NULL;
   struct list_entry *cur;
   struct list_entry *next;
   int count = 0;

   assert(the_list);
   assert(list_is_valid(the_list));
   assert(str);

   cur = the_list->first;

   while (cur != NULL)
   {
      next = cur->next;

      if ((cur->str != NULL) && (0 == strcmp(str, cur->str)))
      {
         count++;

         if (prev != NULL)
         {
            prev->next = next;
         }
         else
         {
            the_list->first = next;
         }
         free((char *)cur->str);
         free(cur);
      }
      else
      {
         prev = cur;
      }
      cur = next;
   }

   the_list->last = prev;

   assert(list_is_valid(the_list));

   return count;
}


/*********************************************************************
 *
 * Function    :  list_remove_list
 *
 * Description :  Remove all strings in one list from another list.
 *                This is currently a brute-force algorithm
 *                (it compares every pair of strings).
 *
 * Parameters  :
 *          1  :  dest = list to change
 *          2  :  src = list of strings to remove
 *
 * Returns     :  Total number of strings removed.
 *
 *********************************************************************/
int list_remove_list(struct list *dest, const struct list *src)
{
   struct list_entry *cur;
   int count = 0;

   assert(src);
   assert(dest);
   assert(list_is_valid(src));
   assert(list_is_valid(dest));

   for (cur = src->first; cur != NULL; cur = cur->next)
   {
      if (cur->str != NULL)
      {
         count += list_remove_item(dest, cur->str);
      }
   }

   assert(list_is_valid(src));
   assert(list_is_valid(dest));

   return count;
}


/*********************************************************************
 *
 * Function    :  list_duplicate
 *
 * Description :  Copy a string list
 *
 * Parameters  :
 *          1  :  dest = Destination list.  Must be a valid list.
 *                       All existing entries will be removed.
 *          1  :  src = pointer to source list for copy.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *                On error, dest will be empty.
 *
 *********************************************************************/
jb_err list_duplicate(struct list *dest, const struct list *src)
{
   struct list_entry * cur_src;
   struct list_entry * cur_dest;

   assert(src);
   assert(dest);
   assert(list_is_valid(src));
   assert(list_is_valid(dest));

   list_remove_all(dest);

   /* Need to process first entry specially so we can set dest->first */
   cur_src = src->first;
   if (cur_src)
   {
      cur_dest = dest->first = (struct list_entry *)zalloc(sizeof(*cur_dest));
      if (cur_dest == NULL)
      {
         destroy_list(dest);

         assert(list_is_valid(src));
         assert(list_is_valid(dest));

         return JB_ERR_MEMORY;
      }

      if (cur_src->str)
      {
         cur_dest->str = strdup(cur_src->str);
         if (cur_dest->str == NULL)
         {
            destroy_list(dest);

            assert(list_is_valid(src));
            assert(list_is_valid(dest));

            return JB_ERR_MEMORY;
         }
      }
      /* else { cur_dest->str = NULL; }  - implied by zalloc */

      /* Now process the rest */
      for (cur_src = cur_src->next; cur_src; cur_src = cur_src->next)
      {
         cur_dest = cur_dest->next = (struct list_entry *)zalloc(sizeof(*cur_dest));
         if (cur_dest == NULL)
         {
            destroy_list(dest);

            assert(list_is_valid(src));
            assert(list_is_valid(dest));

            return JB_ERR_MEMORY;
         }
         if (cur_src->str)
         {
            cur_dest->str = strdup(cur_src->str);
            if (cur_dest->str == NULL)
            {
               destroy_list(dest);

               assert(list_is_valid(src));
               assert(list_is_valid(dest));

               return JB_ERR_MEMORY;
            }
         }
         /* else { cur_dest->str = NULL; }  - implied by zalloc */
      }

      dest->last = cur_dest;
   }

   assert(list_is_valid(src));
   assert(list_is_valid(dest));

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  list_append_list_unique
 *
 * Description :  Append a string list to another list.
 *                Duplicate items are not added.
 *
 * Parameters  :
 *          1  :  dest = pointer to destination list for merge.
 *          2  :  src = pointer to source for merge.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *                On error, some (but not all) of src might have
 *                been copied into dest.
 *
 *********************************************************************/
jb_err list_append_list_unique(struct list *dest, const struct list *src)
{
   struct list_entry * cur;

   assert(src);
   assert(dest);
   assert(list_is_valid(src));
   assert(list_is_valid(dest));

   for (cur = src->first; cur; cur = cur->next)
   {
      if (cur->str)
      {
         if (enlist_unique(dest, cur->str, 0))
         {
            assert(list_is_valid(src));
            assert(list_is_valid(dest));

            return JB_ERR_MEMORY;
         }
      }
   }

   assert(list_is_valid(src));
   assert(list_is_valid(dest));

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  list_is_empty
 *
 * Description :  Test whether a list is empty.  Does not change the list.
 *
 * Parameters  :
 *          1  :  the_list = pointer to list to test.
 *
 * Returns     :  Nonzero if the list contains no entries.
 *
 *********************************************************************/
int list_is_empty(const struct list *the_list)
{
   assert(the_list);
   assert(list_is_valid(the_list));

   return (the_list->first == NULL);
}


/*********************************************************************
 *
 * Function    :  list_contains_item
 *
 * Description :  Tests whether a list item is already set.
 *                Does not change the list.
 *
 * Parameters  :
 *          1  :  the_list = list to search in
 *          2  :  str = string to search for
 *
 * Returns     :  TRUE if the item was found,
 *                FALSE otherwise.
 *
 *********************************************************************/
int list_contains_item(const struct list *the_list, const char *str)
{
   struct list_entry *entry;

   assert(the_list);
   assert(list_is_valid(the_list));
   assert(str);

   for (entry = the_list->first; entry != NULL; entry = entry->next)
   {
      if (entry->str == NULL)
      {
         /*
          * NULL pointers are allowed in some lists. 
          * For example for csp->headers in case a
          * header was removed.
          */
         continue;
      }

      if (0 == strcmp(str, entry->str))
      {
         /* Item found */
         return TRUE;
      }
   }

   return FALSE;
}


/*********************************************************************
 *
 * Function    :  new_map
 *
 * Description :  Create a new, empty map.
 *
 * Parameters  :  N/A
 *
 * Returns     :  A new, empty map, or NULL if out of memory.
 *
 *********************************************************************/
struct map *new_map(void)
{
   return (struct map *) zalloc(sizeof(struct map));
}


/*********************************************************************
 *
 * Function    :  free_map
 *
 * Description :  Free the memory occupied by a map and its
 *                depandant strings
 *
 * Parameters  :
 *          1  :  the_map = map to be freed.  May be NULL.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void free_map(struct map *the_map)
{
   struct map_entry *cur_entry;
   struct map_entry *next_entry;

   if (the_map == NULL)
   {
      return;
   }

   for (cur_entry = the_map->first; cur_entry != NULL; cur_entry = next_entry)
   {
      freez(cur_entry->name);
      freez(cur_entry->value);

      next_entry = cur_entry->next;
      free(cur_entry);
   }

   the_map->first = the_map->last = NULL;

   free(the_map);
}


/*********************************************************************
 *
 * Function    :  map
 *
 * Description :  Add a mapping from given name to given value to a
 *                given map.
 *
 *                Note: Since all strings will be free()d in free_map()
 *                      later, set the copy flags for constants or
 *                      strings that will be independantly free()d.
 *
 *                Note2: This function allows NULL parameters - it
 *                       returns JB_ERR_MEMORY in that case.
 *
 *                Note3: If this function returns JB_ERR_MEMORY,
 *                       it will free(name) unless you specify
 *                       name_needs_copying, and similarly it will
 *                       free(value) unless you specify
 *                       value_needs_copying.
 *
 *                Due to Note2 and Note3 above, the following code
 *                is legal, and will never crash or leak memory even
 *                if the system runs out of memory:
 *
 *                    err = map(mymap, "xyz", 1, html_encode(somestring), 0);
 *
 *                err will be set to JB_ERR_MEMORY if either call runs
 *                out-of-memory.  Without these features, you would
 *                need to check the return value of html_encode in the
 *                above example for NULL, which (at least) doubles the
 *                amount of error-checking code needed.
 *
 * Parameters  :
 *          1  :  the_map = map to add to
 *          2  :  name = name to add
 *          3  :  name_needs_copying = flag set if a copy of name should be used
 *          4  :  value = value to add
 *          5  :  value_needs_copying = flag set if a copy of value should be used
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err map(struct map *the_map,
           const char *name, int name_needs_copying,
           const char *value, int value_needs_copying)
{
   struct map_entry *new_entry;

   assert(the_map);

   if ( (NULL == value)
     || (NULL == name)
     || (NULL == (new_entry = zalloc(sizeof(*new_entry)))) )
   {
      if ((name != NULL) && (!name_needs_copying))
      {
          free((char *)name);
      }
      if ((value != NULL) && (!value_needs_copying))
      {
          free((char *)value);
      }
      return JB_ERR_MEMORY;
   }

   if (name_needs_copying)
   {
      if (NULL == (name = strdup(name)))
      {
         free(new_entry);
         if (!value_needs_copying)
         {
             free((char *)value);
         }
         return JB_ERR_MEMORY;
      }
   }

   if (value_needs_copying)
   {
      if (NULL == (value = strdup(value)))
      {
         free((char *)name);
         free(new_entry);
         return JB_ERR_MEMORY;
      }
   }

   new_entry->name = name;
   new_entry->value = value;
   /* new_entry->next = NULL;  - implied by zalloc */

   if (the_map->last)
   {
      the_map->last->next = new_entry;
      the_map->last = new_entry;
   }
   else
   {
      the_map->first = new_entry;
      the_map->last = new_entry;
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  unmap
 *
 * Description :  Remove all map_entry structs with a given name from
 *                a given map.
 *
 * Parameters  :
 *          1  :  the_map = map to look in
 *          2  :  name = name to unmap
 *
 * Returns     :  JB_ERR_OK
 *
 *********************************************************************/
jb_err unmap(struct map *the_map, const char *name)
{
   struct map_entry *cur_entry, *last_entry;

   assert(the_map);
   assert(name);
   
   last_entry = the_map->first;

   for (cur_entry = the_map->first; cur_entry != NULL; cur_entry = cur_entry->next)
   {
      if (!strcmp(name, cur_entry->name))
      {
         /*
          * Update the incoming pointer
          */
         if (cur_entry == the_map->first)
         {
            the_map->first = cur_entry->next;
         }
         else
         {
            last_entry->next = cur_entry->next;
         }

         /*
          * Update the map's last pointer 
          */
         if (cur_entry == the_map->last)
         {
            the_map->last = last_entry;
         }
         
         /*
          * Free the map_entry
          */
         freez(cur_entry->name);
         freez(cur_entry->value);
         freez(cur_entry);

         cur_entry = last_entry;
      }
      else
      {
         last_entry = cur_entry;
      }
   }
   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  lookup
 *
 * Description :  Look up an item with a given name in a map, and
 *                return its value
 *
 * Parameters  :
 *          1  :  the_map = map to look in
 *          2  :  name = name parameter to look for
 *
 * Returns     :  the value if found, else the empty string.
 *                Return value is alloced as part of the map, so
 *                it is freed when the map is destroyed.  Caller
 *                must not free or modify it.
 *
 *********************************************************************/
const char *lookup(const struct map *the_map, const char *name)
{
   const struct map_entry *cur_entry;

   assert(the_map);
   assert(name);

   for (cur_entry = the_map->first; cur_entry != NULL; cur_entry = cur_entry->next)
   {
      if (!strcmp(name, cur_entry->name))
      {
         return cur_entry->value;
      }
   }
   return "";
}


/*
  Local Variables:
  tab-width: 3
  end:
*/
