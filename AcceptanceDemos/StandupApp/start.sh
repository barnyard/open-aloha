#! /bin/sh
####################################################
ANT_HOME=/home/ccuser/projects/pe-build/ant
JAVA_HOME=/opt/java
STANDUP_HOME=/home/woloszp/pe-sdk-rd/StandupApp
SLEEP=5s
####################################################
export JAVA_HOME=$JAVA_HOME
LOG=$STANDUP_HOME/standup.log
cd $STANDUP_HOME

/usr/local/bin/svn update

PID=`ps -ef | grep com.bt.sdk.callcontrol.demo.standup.Main | grep -v grep | awk '{ print $2 }'`

echo "PID: --"$PID"--"

if [ -n "$PID" ]; then
	echo "Killing previous instance of StandupService with PID = "$PID
	kill -9 $PID

	echo "Waiting for the process to release port:" $SLEEP
	sleep $SLEEP
else
	echo "Standup Service process has not been found."
fi
echo "Starting Standup Starting"
$ANT_HOME/bin/ant run > $LOG &
echo "Done."

