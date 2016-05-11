!define MULTIUSER_EXECUTIONLEVEL Highest
!define MULTIUSER_MUI
!define MULTIUSER_INSTALLMODE_INSTDIR Phaedra

!include MultiUser.nsh
!include MUI2.nsh
!include LogicLib.nsh
!include x64.nsh

;--------------------------------
; Before installation, remove existing Phaedra if needed

Function .onInit
  !insertmacro MULTIUSER_INIT
  
  ReadRegStr $R0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Phaedra" "UninstallString"
  StrCmp $R0 "" done

  MessageBox MB_OKCANCEL|MB_ICONINFORMATION \
  "A version of Phaedra is already installed. $\n$\nClick `OK` to remove the previous version or `Cancel` to cancel this upgrade." IDOK uninst
  Abort

  ;Run the uninstaller
  uninst:
    ClearErrors
    Push $R0
    Call getParentDir
    Pop $R1
    ExecWait '$R0 _?=$R1'
  done:

FunctionEnd

;--------------------------------
Function un.onInit
  !insertmacro MULTIUSER_UNINIT
FunctionEnd

;--------------------------------
Function un.isEmptyDir
  # Stack ->                    # Stack: <directory>
  Exch $0                       # Stack: $0
  Push $1                       # Stack: $1, $0
  FindFirst $0 $1 "$0\*.*"
  strcmp $1 "." 0 _notempty
    FindNext $0 $1
    strcmp $1 ".." 0 _notempty
      ClearErrors
      FindNext $0 $1
      IfErrors 0 _notempty
        FindClose $0
        Pop $1                  # Stack: $0
        StrCpy $0 1
        Exch $0                 # Stack: 1 (true)
        goto _end
     _notempty:
       FindClose $0
       ClearErrors
       Pop $1                   # Stack: $0
       StrCpy $0 0
       Exch $0                  # Stack: 0 (false)
  _end:
FunctionEnd

;--------------------------------
Function setDirX64
  ${If} $INSTDIR == "$PROGRAMFILES\${MULTIUSER_INSTALLMODE_INSTDIR}"
	StrCpy $INSTDIR "$PROGRAMFILES64\${MULTIUSER_INSTALLMODE_INSTDIR}"
  ${EndIf}
FunctionEnd

;--------------------------------
Function getParentDir
  Exch $R0
  Push $R1
  Push $R2
  Push $R3
  StrCpy $R1 0
  StrLen $R2 $R0
 
  loop:
    IntOp $R1 $R1 + 1
    IntCmp $R1 $R2 get 0 get
    StrCpy $R3 $R0 1 -$R1
    StrCmp $R3 "\" get
  Goto loop
 
  get:
    StrCpy $R0 $R0 -$R1
    Pop $R3
    Pop $R2
    Pop $R1
    Exch $R0
FunctionEnd

;--------------------------------
Name "Phaedra"
OutFile "setup.exe"
InstallDir "Phaedra"

;--------------------------------
!define MUI_ICON "phaedra.ico"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP "phaedra.bmp"
!define MUI_ABORTWARNING

;--------------------------------
;Pages

!insertmacro MUI_PAGE_WELCOME
!insertmacro MULTIUSER_PAGE_INSTALLMODE
!define MUI_PAGE_CUSTOMFUNCTION_PRE setDirX64
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

!insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Sections

Section "Phaedra" SecInstall

  SetOutPath $INSTDIR
  
  ; Unzip program files
  File /r phaedra\*.*

  ; Create Desktop & Start Menu shortcuts
  CreateShortCut "$DESKTOP\Phaedra.lnk" "$INSTDIR\phaedra.exe" ""
  CreateDirectory "$SMPROGRAMS\Phaedra"
  CreateShortCut "$SMPROGRAMS\Phaedra\Phaedra.lnk" "$INSTDIR\phaedra.exe"
  CreateShortCut "$SMPROGRAMS\Phaedra\Uninstall Phaedra.lnk" "$INSTDIR\uninstall.exe"

  ; Give full control to everyone. Required for updates and temp folder usage.
  AccessControl::GrantOnFile "$INSTDIR" "(BU)" "FullAccess"
  
  ; Write registry keys for phaedra:// protocol association
  WriteRegStr HKCR "Phaedra" "" "URL:Phaedra Protocol"
  WriteRegStr HKCR "Phaedra" "URL Protocol" ""
  WriteRegStr HKCR "Phaedra\DefaultIcon" "" '"$INSTDIR\phaedra.exe"'
  WriteRegStr HKCR "Phaedra\shell\open\command" "" '"$INSTDIR\phaedra.exe" "%1"'
  
  ; Write registry keys for uninstallation
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Phaedra" "DisplayName" "Phaedra (remove only)"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Phaedra" "UninstallString" "$INSTDIR\uninstall.exe"
  WriteUninstaller "$INSTDIR\uninstall.exe"

SectionEnd

;--------------------------------
;Descriptions

LangString DESC_SecInstall ${LANG_ENGLISH} "The Phaedra Application"

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SecInstall} $(DESC_SecInstall)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  RMDir /r "$INSTDIR\configuration"
  RMDir /r "$INSTDIR\features"
  RMDir /r "$INSTDIR\p2"
  RMDir /r "$INSTDIR\plugins"
  RMDir /r "$INSTDIR\readme"
  RMDir /r "$INSTDIR\workspace"
  RMDir /r "$INSTDIR\jre"
  RMDir /r "$INSTDIR\temp"
  RMDir /r "$INSTDIR\embedded.env"
  delete "$INSTDIR\phaedra.exe"
  delete "$INSTDIR\phaedra.ini"
  delete "$INSTDIR\phaedra.log"
  delete "$INSTDIR\artifacts.xml"
  delete "$INSTDIR\epl-v10.html"
  delete "$INSTDIR\notice.html"
  delete "$INSTDIR\.eclipseproduct"
  delete "$INSTDIR\uninstall.exe"
  delete "$INSTDIR\eclipsec.exe"
   
  RMDir /r "$SMPROGRAMS\Phaedra"
  delete "$DESKTOP\Phaedra.lnk"

  DeleteRegKey HKCR "Phaedra"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Phaedra"

  Push "$INSTDIR"
	Call un.isEmptyDir
  Pop $0
  StrCmp $0 1 0 +2
    RMDir "$INSTDIR"
    
SectionEnd