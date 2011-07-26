rem set IP=10.73.80.19
rem set IP=10.237.33.57
rem set IP=10.73.80.194
rem set IP=10.73.164.185
rem set IP=10.73.164.58
rem set IP=10.114.184.208
rem set IP=10.73.16.197
rem set IP=10.73.17.82
rem set IP=10.114.85.100
rem set IP=10.73.130.42

rem set IP=10.237.33.202
set IP=10.237.33.160
rem set IP=127.0.0.1

rem -rsa %IP%:6073

rem "D:/Program Files/SIPp/sipp" -rsa %IP%:6073 -i %IP% -s test %IP% -sf ../sipp/uac.xml
"D:/Program Files/SIPp/sipp" -bind_local -s test %IP% -sf ../sipp/uac.xml -aa
