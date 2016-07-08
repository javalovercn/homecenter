### License
1. please read and agree the license file "bcl.txt", "hc_license.txt" and "mpl_license.txt".

***
### Main Features

1. My machine, My cloud. It runs on the most popular computing environments JVM and is cloud for your mobile, just double-click to setup, no configuration.
2. HomeCenter server runs not only on Oracle/OpenJDK JVM, but also Android, because there is a library ["J2SE for Android"](https://github.com/javalovercn/j2se_for_android), now you can "write once, run anywhere".
3. scan QRcode from mobile to download and setup HAR project on server, plug and play, not required to restart server.
4. build-in SecurityManager, each HAR project runs in a optional, security, limited computing and networking environment.
5. HTMLMlet is implements with JComponent+Listener+Layout+CSS on server and it will be translated to HTML5+JavaScript+CSS for your mobile client.
6. integrated with Java API Doc in designer, IDE for JRuby, code and resource file(in jar library) will be auto completion.
7. stratify IoT, Robot+Converter+Device, provides powerful device adaptability and data format conversion.
8. exception is automatically reported to Email or website of HAR project provider.
9. each HAR project can be upgraded automatically if there is a newer HAR on web.

***
### How to use

1. these source files are HomeCenter Application Server, which is on PC side. they are NOT client for mobile.
2. JRE/JDK 7.0 or above is required.
3. main class : hc.App
4. arguments for main method : debugOn serverOn verify
5. VM arguments for main class : -Dsun.jnu.encoding=UTF-8
6. set [Compiler compliance level] of Eclipse to 1.7 or upper
7. please keep the source code for the latest version, otherwise it may causes abnormal connections.
8. to install client-side (mobile) application for Android, iPhone, J2ME, please download them from http://homecenter.mobi/en/pc/downloads.htm
9. these two files (starter.jar and hc.pem) in distributed zip file (NOT source files), are used to check new version, download, verify and start up this application server
10. there is a sample HAR project in designer on server, please read source code and annotation carefully, it will be simple and joyful to develop your HAR project for mobile.
11. for help on usage, steps, guide, developing, please go http://homecenter.mobi/en/pc/steps.htm
12. for API, please go http://homecenter.mobi/download/javadoc/index.html or press alt+/ for code hints and java docs in designer.
13. any question, please contact : help at homecenter.mobi

***

![splash](http://homecenter.mobi/images/splash_n_txt.png)

***

![screenshot](http://homecenter.mobi/images/sc6.png)
![screenshot](http://homecenter.mobi/images/sc8.png)
![screenshot](http://homecenter.mobi/images/sc7.png)
![screenshot](http://homecenter.mobi/images/sc9.png)

***

![designer](http://homecenter.mobi/images/usage/pc_designer.png)