# Microsoft Developer Studio Project File - Name="vc_dftables" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 5.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Console Application" 0x0103

CFG=vc_dftables - Win32 Debug with Win32 threads
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "vc_dftables.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "vc_dftables.mak"\
 CFG="vc_dftables - Win32 Debug with Win32 threads"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "vc_dftables - Win32 Release" (based on\
 "Win32 (x86) Console Application")
!MESSAGE "vc_dftables - Win32 Debug" (based on\
 "Win32 (x86) Console Application")
!MESSAGE "vc_dftables - Win32 Debug with Win32 threads" (based on\
 "Win32 (x86) Console Application")
!MESSAGE "vc_dftables - Win32 Release with Win32 threads" (based on\
 "Win32 (x86) Console Application")
!MESSAGE 

# Begin Project
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
RSC=rc.exe

!IF  "$(CFG)" == "vc_dftables - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "vc_dftables"
# PROP Intermediate_Dir "vc_dftables"
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD CPP /nologo /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD BASE RSC /l 0x809 /d "NDEBUG"
# ADD RSC /l 0x809 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:I386
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:I386
# Begin Special Build Tool
OutDir=.\vc_dftables
SOURCE=$(InputPath)
PostBuild_Desc=Running program to generate chartables.c
PostBuild_Cmds=$(OutDir)\vc_dftables.exe >$(OutDir)\..\chartables.c
# End Special Build Tool

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "vc_dftables_dbg"
# PROP Intermediate_Dir "vc_dftables_dbg"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /Gm /GX /Zi /Od /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD CPP /nologo /W3 /Gm /GX /Zi /Od /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD BASE RSC /l 0x809 /d "_DEBUG"
# ADD RSC /l 0x809 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# Begin Special Build Tool
OutDir=.\vc_dftables_dbg
SOURCE=$(InputPath)
PostBuild_Desc=Running program to generate chartables.c
PostBuild_Cmds=$(OutDir)\vc_dftables.exe >$(OutDir)\..\chartables.c
# End Special Build Tool

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Debug with Win32 threads"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "vc_dftab"
# PROP BASE Intermediate_Dir "vc_dftab"
# PROP BASE Ignore_Export_Lib 0
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "vc_dftables_dbg"
# PROP Intermediate_Dir "vc_dftables_dbg"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /Gm /GX /Zi /Od /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD CPP /nologo /W3 /Gm /GX /Zi /Od /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD BASE RSC /l 0x809 /d "_DEBUG"
# ADD RSC /l 0x809 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# Begin Special Build Tool
OutDir=.\vc_dftables_dbg
SOURCE=$(InputPath)
PostBuild_Desc=Running program to generate chartables.c
PostBuild_Cmds=$(OutDir)\vc_dftables.exe >$(OutDir)\..\chartables.c
# End Special Build Tool

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Release with Win32 threads"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "vc_dfta0"
# PROP BASE Intermediate_Dir "vc_dfta0"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "vc_dftables"
# PROP Intermediate_Dir "vc_dftables"
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD CPP /nologo /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD BASE RSC /l 0x809 /d "NDEBUG"
# ADD RSC /l 0x809 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:I386
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:I386
# Begin Special Build Tool
OutDir=.\vc_dftables
SOURCE=$(InputPath)
PostBuild_Desc=Running program to generate chartables.c
PostBuild_Cmds=$(OutDir)\vc_dftables.exe >$(OutDir)\..\chartables.c
# End Special Build Tool

!ENDIF 

# Begin Target

# Name "vc_dftables - Win32 Release"
# Name "vc_dftables - Win32 Debug"
# Name "vc_dftables - Win32 Debug with Win32 threads"
# Name "vc_dftables - Win32 Release with Win32 threads"
# Begin Group "File Copy"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\vc_config_pthreads.h

!IF  "$(CFG)" == "vc_dftables - Win32 Release"

# PROP Ignore_Default_Tool 1
# Begin Custom Build - Copying vc_config_pthreads.h
WkspDir=.
InputPath=..\vc_config_pthreads.h

"$(WkspDir)\..\config.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy "$(InputPath)" "$(WkspDir)\..\config.h"

# End Custom Build

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Debug"

# PROP Ignore_Default_Tool 1
# Begin Custom Build - Copying vc_config_pthreads.h
WkspDir=.
InputPath=..\vc_config_pthreads.h

"$(WkspDir)\..\config.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy "$(InputPath)" "$(WkspDir)\..\config.h"

# End Custom Build

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Debug with Win32 threads"

# PROP Exclude_From_Build 1
# PROP Ignore_Default_Tool 1

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Release with Win32 threads"

# PROP Exclude_From_Build 1
# PROP Ignore_Default_Tool 1

!ENDIF 

# End Source File
# Begin Source File

SOURCE=..\vc_config_winthreads.h

!IF  "$(CFG)" == "vc_dftables - Win32 Release"

# PROP Exclude_From_Build 1
# PROP Ignore_Default_Tool 1

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Debug"

# PROP Exclude_From_Build 1
# PROP Ignore_Default_Tool 1

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Debug with Win32 threads"

# PROP Ignore_Default_Tool 1
# Begin Custom Build - Copying vc_config_winthreads.h
WkspDir=.
InputPath=..\vc_config_winthreads.h

"$(WkspDir)\..\config.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy "$(InputPath)" "$(WkspDir)\..\config.h"

# End Custom Build

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Release with Win32 threads"

# PROP Ignore_Default_Tool 1
# Begin Custom Build - Copying vc_config_winthreads.h
WkspDir=.
InputPath=..\vc_config_winthreads.h

"$(WkspDir)\..\config.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy "$(InputPath)" "$(WkspDir)\..\config.h"

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Source File

SOURCE=..\config.h
# End Source File
# Begin Source File

SOURCE=.\config.h
# End Source File
# Begin Source File

SOURCE=.\dftables.c
# End Source File
# Begin Source File

SOURCE=.\internal.h
# End Source File
# Begin Source File

SOURCE=.\maketables.c

!IF  "$(CFG)" == "vc_dftables - Win32 Release"

# PROP Exclude_From_Build 1

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Debug"

# PROP Exclude_From_Build 1

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Debug with Win32 threads"

# PROP BASE Exclude_From_Build 1
# PROP Exclude_From_Build 1

!ELSEIF  "$(CFG)" == "vc_dftables - Win32 Release with Win32 threads"

# PROP BASE Exclude_From_Build 1
# PROP Exclude_From_Build 1

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\pcre.h
# End Source File
# End Target
# End Project
