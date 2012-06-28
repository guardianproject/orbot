#ifndef W32RES_H_INCLUDED
#define W32RES_H_INCLUDED
#define W32RES_H_VERSION "$Id: w32res.h,v 1.20 2011/09/04 11:10:56 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/w32res.h,v $
 *
 * Purpose     :  Identifiers for Windows GUI resources.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2002 members of
 *                the Privoxy team.  http://www.privoxy.org/
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

#define IDR_TRAYMENU                      101
#define IDI_IDLE                          102
#define IDR_LOGVIEW                       103
#define IDR_ACCELERATOR                   104
#define IDR_POPUP_SELECTION               105


#define IDI_MAINICON                      200
#define IDI_ANIMATED1                     201
#define IDI_ANIMATED2                     202
#define IDI_ANIMATED3                     203
#define IDI_ANIMATED4                     204
#define IDI_ANIMATED5                     205
#define IDI_ANIMATED6                     206
#define IDI_ANIMATED7                     207
#define IDI_ANIMATED8                     208
#define IDI_OFF                           209

#define ID_TOGGLE_SHOWWINDOW              4000
#define ID_HELP_ABOUT                     4001
#define ID_FILE_EXIT                      4002
#define ID_VIEW_CLEARLOG                  4003
#define ID_VIEW_LOGMESSAGES               4004
#define ID_VIEW_MESSAGEHIGHLIGHTING       4005
#define ID_VIEW_LIMITBUFFERSIZE           4006
#define ID_VIEW_ACTIVITYANIMATION         4007
#define ID_HELP_FAQ                       4008
#define ID_HELP_MANUAL                    4009
#define ID_HELP_GPL                       4010
#define ID_HELP_STATUS                    4011
#ifdef FEATURE_TOGGLE
#define ID_TOGGLE_ENABLED                 4012
#endif /* def FEATURE_TOGGLE */

/* Break these out so they are easier to extend, but keep consecutive */
#define ID_TOOLS_EDITCONFIG               5000
#define ID_TOOLS_EDITDEFAULTACTIONS       5001
#define ID_TOOLS_EDITUSERACTIONS          5002
#define ID_TOOLS_EDITDEFAULTFILTERS       5003
#define ID_TOOLS_EDITUSERFILTERS          5004

#ifdef FEATURE_TRUST
#define ID_TOOLS_EDITTRUST                5005
#endif /* def FEATURE_TRUST */

#define ID_EDIT_COPY  30000


#endif /* ndef W32RES_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
