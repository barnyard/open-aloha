set LOCAL_IP=132.146.253.12
rem set SSS_IP=132.146.185.193
rem sipp -rsa %SSS_IP%:6073 -s test %LOCAL_IP% -sf uacreject.xml -p 5061 -trace_msg
sipp %LOCAL_IP% -sf uacreject.xml -p 5061 -aa
