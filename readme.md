### License
1. please read and agree the license file "bcl.txt", "hc_license.txt" and "mpl_license.txt".

***
### Main Features

1. just double-click to run, no configuration.
2. HomeCenter server runs not only on Oracle/OpenJDK JVM, but also Android, because there is a library ["J2SE for Android"](https://github.com/javalovercn/j2se_for_android).
3. scan QRcode from mobile to download and load HAR (home archive) project on server, plug and play, not required to restart server.
4. focus your business and UI with Java J2SE technology for your design HAR, the rest of work are ours.
5. HTMLMlet is implements with JComponent+Listener+Layout+CSS on server and it will be translated to HTML5+JavaScript+CSS for your Android/iPhone mobile client on-air.
6. the designer on server is integrated with Java API Doc and is also an IDE for JRuby, hint of code and resource file(in jar library) will be auto completion.
7. stratify IoT, Robot+Converter+Device, provides powerful device adaptability and data format conversion.
8. your HAR project is self-signed and be upgraded automatically if upload the newer HAR on web.
9. exception is automatically reported to your Email or website if you are HAR project provider.
10. build-in SecurityManager, HAR project runs in a optional, security, limited computing and networking environment.

***
### How to use source

1. these source files are HomeCenter Application Server, which is on server side, NOT client for mobile.
2. JRE/JDK 7 or above is required.
3. main class : hc.App
4. VM arguments for main class : -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8
5. set [Compiler compliance level] of Eclipse to 1.7 or upper
6. to print server log to console, please keep server [options/Developer/Logger] unchecked
7. please keep the source code for the latest version, otherwise it may causes abnormal connections with mobile.
8. there is a sample HAR project (NOT "MyFirst" project) in designer on server, covers 80% of JRuby syntax and 90% of functions of HomeCenter server.
9. for API, press alt+/ in designer or go https://homecenter.mobi/download/javadoc/index.html
10. any question, please open issues https://github.com/javalovercn/homecenter/issues or Email : help at homecenter.mobi
11. for the latest version mobile App (client side), download from https://github.com/javalovercn/client
12. for binary server of Android or other, download from https://homecenter.mobi/en/pc/downloads.htm
13. these two files (starter.jar and hc.pem) in binary distributed zip file (NOT in source files), are used to check new version, download binary jar of these source files, verify and start up HomeCenter server.

***

![splash](https://homecenter.mobi/images/splash_n_txt.png)

***

![screenshot](https://homecenter.mobi/images/sc6.png)
![screenshot](https://homecenter.mobi/images/sc8.png)
![screenshot](https://homecenter.mobi/images/sc7.png)
![screenshot](https://homecenter.mobi/images/sc9.png)

***

![designer](https://homecenter.mobi/images/usage/pc_designer.png)