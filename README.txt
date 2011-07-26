Directories
===========

AcceptanceDemos     - Some simple programs that demonstrate various Aloha features.
ConvediaMediaBeans  - The API extension to utilise the Convedia Media Server from RadiSys.
CruiseControl       - CruiseControl configuration and scripts for continuous integration.
Fitnesse            - Acceptance tests using the FitNesse framework.
Main                - Top level build and zip file distribution build.
Mockphones          - Mockphones project that uses Aloha, mainly used for testing.
Robustness          - Multi-threaded testing framework.
Samples             - Simple code samples that are included in the zip file.
ScratchPad          - Some work in progress.
SimpleSipStack      - The main Aloha API.
SipStone            - Test framework for the SipStone industry standard.
distribution        - Zip files from the builds are copied here for users to download.
etc                 - Some common Ant scripts used for the build metrics.


Building instructions
=====================

The project should be built in the following order:

1. cd SimpleSipStack
   ant -lib ../etc/ant/reporting/lib/jdepend -lib ../etc/ant/reporting/lib/checkstyle

2. cd ../ConvediaMediaBeans
   ant -lib ../etc/ant/reporting/lib/jdepend -lib ../etc/ant/reporting/lib/checkstyle

3. cd cd ../Fitnesse
   ant


A shorter build without tests and metrics can be performed thus:

cd Main
ant

