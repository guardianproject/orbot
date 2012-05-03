# Microsoft Developer Studio Project File - Name="vc_console" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 5.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Console Application" 0x0103

CFG=vc_console - Win32 Debug with Win32 threads
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "vc_console.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "vc_console.mak"\
 CFG="vc_console - Win32 Debug with Win32 threads"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "vc_console - Win32 Release" (based on\
 "Win32 (x86) Console Application")
!MESSAGE "vc_console - Win32 Debug" (based on\
 "Win32 (x86) Console Application")
!MESSAGE "vc_console - Win32 Debug with Win32 threads" (based on\
 "Win32 (x86) Console Application")
!MESSAGE "vc_console - Win32 Release with Win32 threads" (based on\
 "Win32 (x86) Console Application")
!MESSAGE 

# Begin Project
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
RSC=rc.exe

!IF  "$(CFG)" == "vc_console - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "console_release"
# PROP Intermediate_Dir "console_release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD CPP /nologo /MT /W3 /GX /O2 /I "pcre" /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /D "_WIN_CONSOLE" /D "STATIC" /YX /FD /c
# ADD BASE RSC /l 0x809 /d "NDEBUG"
# ADD RSC /l 0x809 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:I386
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib ws2_32.lib comctl32.lib pthreadVC.lib /nologo /subsystem:console /machine:I386

!ELSEIF  "$(CFG)" == "vc_console - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "console_debug"
# PROP Intermediate_Dir "console_debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /Gm /GX /Zi /Od /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD CPP /nologo /MTd /W3 /Gm /GX /Zi /Od /I "pcre" /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /D "_WIN_CONSOLE" /D "STATIC" /FR /YX /FD /c
# ADD BASE RSC /l 0x809 /d "_DEBUG"
# ADD RSC /l 0x809 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib ws2_32.lib comctl32.lib pthreadVC.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept

!ELSEIF  "$(CFG)" == "vc_console - Win32 Debug with Win32 threads"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "console_"
# PROP BASE Intermediate_Dir "console_"
# PROP BASE Ignore_Export_Lib 0
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "console_debug_winthr"
# PROP Intermediate_Dir "console_debug_winthr"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /Zi /Od /I "pcre" /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /D "_WIN_CONSOLE" /D "STATIC" /FR /YX /FD /c
# ADD CPP /nologo /MTd /W3 /Gm /GX /Zi /Od /I "pcre" /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /D "_WIN_CONSOLE" /D "STATIC" /FR /YX /FD /c
# ADD BASE RSC /l 0x809 /d "_DEBUG"
# ADD RSC /l 0x809 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib ws2_32.lib comctl32.lib pthreadVC.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib ws2_32.lib comctl32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept

!ELSEIF  "$(CFG)" == "vc_console - Win32 Release with Win32 threads"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "console0"
# PROP BASE Intermediate_Dir "console0"
# PROP BASE Ignore_Export_Lib 0
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "console_release_winthr"
# PROP Intermediate_Dir "console_release_winthr"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /I "pcre" /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /D "_WIN_CONSOLE" /D "STATIC" /YX /FD /c
# ADD CPP /nologo /MT /W3 /GX /O2 /I "pcre" /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /D "_WIN_CONSOLE" /D "STATIC" /YX /FD /c
# ADD BASE RSC /l 0x809 /d "NDEBUG"
# ADD RSC /l 0x809 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib ws2_32.lib comctl32.lib pthreadVC.lib /nologo /subsystem:console /machine:I386
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib ws2_32.lib comctl32.lib /nologo /subsystem:console /machine:I386

!ENDIF 

# Begin Target

# Name "vc_console - Win32 Release"
# Name "vc_console - Win32 Debug"
# Name "vc_console - Win32 Debug with Win32 threads"
# Name "vc_console - Win32 Release with Win32 threads"
# Begin Group "JunkBuster"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\actionlist.h
# End Source File
# Begin Source File

SOURCE=.\actions.c
# End Source File
# Begin Source File

SOURCE=.\actions.h
# End Source File
# Begin Source File

SOURCE=.\cgi.c
# End Source File
# Begin Source File

SOURCE=.\cgi.h
# End Source File
# Begin Source File

SOURCE=.\cgiedit.c
# End Source File
# Begin Source File

SOURCE=.\cgiedit.h
# End Source File
# Begin Source File

SOURCE=.\cgisimple.c
# End Source File
# Begin Source File

SOURCE=.\cgisimple.h
# End Source File
# Begin Source File

SOURCE=.\config.h
# End Source File
# Begin Source File

SOURCE=.\deanimate.c
# End Source File
# Begin Source File

SOURCE=.\deanimate.h
# End Source File
# Begin Source File

SOURCE=.\errlog.c
# End Source File
# Begin Source File

SOURCE=.\errlog.h
# End Source File
# Begin Source File

SOURCE=.\filters.c
# End Source File
# Begin Source File

SOURCE=.\filters.h
# End Source File
# Begin Source File

SOURCE=.\jcc.c
# End Source File
# Begin Source File

SOURCE=.\jcc.h
# End Source File
# Begin Source File

SOURCE=.\loadcfg.c
# End Source File
# Begin Source File

SOURCE=.\loadcfg.h
# End Source File
# Begin Source File

SOURCE=.\loaders.c
# End Source File
# Begin Source File

SOURCE=.\loaders.h
# End Source File
# Begin Source File

SOURCE=.\parsers.c
# End Source File
# Begin Source File

SOURCE=.\parsers.h
# End Source File
# Begin Source File

SOURCE=.\project.h
# End Source File
# Begin Source File

SOURCE=.\urlmatch.c
# End Source File
# Begin Source File

SOURCE=.\urlmatch.h
# End Source File
# End Group
# Begin Group "Win32"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\cygwin.h
# End Source File
# Begin Source File

SOURCE=.\win32.c
# End Source File
# Begin Source File

SOURCE=.\win32.h
# End Source File
# End Group
# Begin Group "PCRE"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\pcre\chartables.c

!IF  "$(CFG)" == "vc_console - Win32 Release"

# PROP Exclude_From_Build 1

!ELSEIF  "$(CFG)" == "vc_console - Win32 Debug"

# PROP Exclude_From_Build 1

!ELSEIF  "$(CFG)" == "vc_console - Win32 Debug with Win32 threads"

# PROP BASE Exclude_From_Build 1
# PROP Exclude_From_Build 1

!ELSEIF  "$(CFG)" == "vc_console - Win32 Release with Win32 threads"

# PROP BASE Exclude_From_Build 1
# PROP Exclude_From_Build 1

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\pcre\config.h
# End Source File
# Begin Source File

SOURCE=.\pcre\get.c
# End Source File
# Begin Source File

SOURCE=.\pcre\internal.h
# End Source File
# Begin Source File

SOURCE=.\pcre\maketables.c
# End Source File
# Begin Source File

SOURCE=.\pcre\pcre.c
# End Source File
# Begin Source File

SOURCE=.\pcre\pcre.h
# End Source File
# Begin Source File

SOURCE=.\pcre\pcreposix.c
# End Source File
# Begin Source File

SOURCE=.\pcre\pcreposix.h
# End Source File
# Begin Source File

SOURCE=.\pcre\study.c
# End Source File
# End Group
# Begin Group "PCRS"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\pcrs.c
# End Source File
# Begin Source File

SOURCE=.\pcrs.h
# End Source File
# End Group
# Begin Group "Sockets"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\gateway.c
# End Source File
# Begin Source File

SOURCE=.\gateway.h
# End Source File
# Begin Source File

SOURCE=.\jbsockets.c
# End Source File
# Begin Source File

SOURCE=.\jbsockets.h
# End Source File
# End Group
# Begin Group "Utilities"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\encode.c
# End Source File
# Begin Source File

SOURCE=.\encode.h
# End Source File
# Begin Source File

SOURCE=.\list.c
# End Source File
# Begin Source File

SOURCE=.\list.h
# End Source File
# Begin Source File

SOURCE=.\miscutil.c
# End Source File
# Begin Source File

SOURCE=.\miscutil.h
# End Source File
# Begin Source File

SOURCE=.\ssplit.c
# End Source File
# Begin Source File

SOURCE=.\ssplit.h
# End Source File
# End Group
# End Target
# End Project
