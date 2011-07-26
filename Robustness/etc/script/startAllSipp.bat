set SIPP=c:\work\sipp\sipp.exe
set IP=10.237.33.74
rem set IP=127.0.0.1
start "sipp1" /d ..\sipp "%SIPP%" %IP% -sf uac.xml -trace_msg -aa
rem start "sipp2" /d ..\sipp "%SIPP%" %IP% -sf uacreject.xml -p 5061 -aa
rem start "sipp3" /d ..\sipp "%SIPP%" %IP% -sf uactimeout.xml -p 5062 -aa
rem start "sipp4" /d ..\sipp "%SIPP%" %IP% -sf prompt-and-collect.xml -p 5063 -aa

