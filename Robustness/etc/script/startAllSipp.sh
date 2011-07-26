#!/bin/bash
SIPP=/opt/sipp/sipp
set IP=132.146.185.195
$SIPP $IP -sf uac.xml -bg -aa
$SIPP $IP -sf uacreject.xml -p 5061 -bg -aa
$SIPP $IP -sf uactimeout.xml -p 5062 -bg -aa
sudo $SIPP $IP -sf prompt-and-collect.xml -p 5063 -bg -aa
$SIPP $IP -sf uac.xml -p 5064 -bg -aa

