set IP=10.237.33.12
rem set IP=127.0.0.1

"c:/Program Files/SIPp/sipp" -i %IP% -s test %IP% -sf ../sipp/uac.xml -aa
