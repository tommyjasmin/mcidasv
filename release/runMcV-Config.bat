@ECHO OFF

SET MCV_USERPATH=%USERPROFILE%\McIDAS-V
SET MCV_PARAMS=%*

REM Check for -userpath parameter
:checkparameters
IF '%1' == '' GOTO endparameters
IF '%1' == '-userpath' GOTO setuserpath
SHIFT
GOTO checkparameters

:setuserpath
SET MCV_USERPATH=%2
SHIFT
SHIFT
GOTO checkparameters

:endparameters

REM Initialize new userpath
IF EXIST "%MCV_USERPATH%" GOTO hasuserpath

:inituserpath
IF EXIST "%USERPROFILE%\.mcidasv" (
	echo Moving files to new user path: %MCV_USERPATH%
	move "%USERPROFILE%\.mcidasv" "%MCV_USERPATH%"
)

:hasuserpath



REM Get the amount of system memory
echo Reading system configuration...
SET SYS_MEM=0
FOR /F %%i IN ('jre\bin\java.exe -cp mcidasv.jar edu.wisc.ssec.mcidasv.util.GetMem 2^>NUL') DO SET SYS_MEM=%%i

jre\bin\java -jar startupmanager.jar -Didv.sysmem=%SYS_MEM% -userpath "%MCV_USERPATH%" %MCV_PARAMS%
