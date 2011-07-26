
"%SIPP%" title "sipp" %LOCAL_IP% -sf ..\..\inbound2.xml -inf ..\..\callees_allHappy.csv -rsa %REMOTE_IP%:6060 -r 1 -rate_increase 1 -fd 5s -rate_max 1 -trace_stat

