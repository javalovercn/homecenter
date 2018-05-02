Setup HomeCenter server on a host, the end user is only required to downalod mobile app and login with account.

## Key Point
1. enable parameter *isNonUIServer*
2. intall Xvfb

### 1. create account
do following on your PC, not on host :
download, unzip HomeCenter server, run it and setup account and configure, shutdown server.

### 2. upload
upload all files to host. Note : please add permission of write and execute for folder.

### 3. enable parameter
add "isNonUIServer=true" to the end of hc_config.properties.

### 4. install Xvfb
run "yum install -y Xvfb" on CentOS to install Xvfb.

### 5. create start script
create script file "hcNoUI.sh", add following content to it and add executable permission for it.
```bash
#!/bin/bash

BASE_PATH=`dirname $0`
cd $BASE_PATH
xvfb-run --auto-servernum --server-args="-screen 0 1280x760x24" java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -cp hc.jar:hc_thirds.jar hc.App &
```

### 6. start server when power on
open "/etc/rc.d/rc.local", add path of "hcNoUI.sh" to execute it when power on.

### 7. upgrade HomeCenter
open "starter.properties" to see current version of server.

open "[http://homecenter.mobi/starter/go.php](http://homecenter.mobi/starter/go.php)" to see the new version or not.

if there is a new version, kill hcNoUI.sh(Note : please use "kill -15", not "kill -9"), upload new version jar and execute hcNoUI.sh.

## Copyright
Attribution-NonCommercial 4.0
for more, http://creativecommons.org/licenses/by-nc/4.0/
