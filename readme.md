### Snapshot of mobile
![screenshot](http://homecenter.mobi/images/sc6.png)
![screenshot](http://homecenter.mobi/images/sc_mlet_bg.png)
![screenshot](http://homecenter.mobi/images/sc8.png)

***
### Sample mobile UI
![screenshot](http://homecenter.mobi/images/sc_mlet.png)

to implement the cross-platform UI for Android/iPhone, write with J2SE+CSS as [here](https://github.com/javalovercn/homecenter/blob/master/docs/develop%20cloud%20app%20with%20J2SE%2BCSS%20for%20iPhone%20and%20Android.md).

***
### License
1. please read and agree the license file "bcl.txt", "hc_license.txt" and "mpl_license.txt".

***
### Demo on mobile
1. download mobile client from https://github.com/javalovercn/client
2. click "Demo" button in login form. (or input account : 012345，password : 012345)

***
### Features

1. scan QRcode from mobile and download/load HAR (home archive) project on server, plug and play.
2. the component HTMLMlet (codes above) will be translated to HTML5+JavaScript+CSS for Android/iPhone client on air, user inputs will be translated to J2SE Event and dispatch to their listeners.
3. focus business/UI with J2SE/JavaScript+CSS technology.
4. ScriptPanel is used to load and execute JavaScript on mobile client.
5. code editor supports Java 8 API, Ruby 2.2.0 and CSS 2.2 (part) documents, hint of code and resource file(in user jar) will be auto completion. 
6. built-in Lucene, Quartz and HSQLDB, see [API](https://homecenter.mobi/download/javadoc/index.html) for more.
7. mobile voice command API (let Android/iPhone recognize your voice) drives IoT and your business.
8. family members or work group not only share the same service account, but also differentiate services based on the member ID.
9. stratify IoT, Robot+Converter+Device(device is required to open source or provide API), provides powerful device adaptability and data format conversion.
10. HAR project is self-signed and be upgraded automatically if the newer on web.
11. a powerful tool for building your voice, mobility, automation, intelligence life.
12. exception is automatically reported to Email or website if end user enable reporting exception.
13. build-in SecurityManager limits project running in an optional, security computing and networking environment.
14. because of ["J2SE for Android"](https://github.com/javalovercn/j2se_for_android), HAR can also run on Android server (code editor is not built in),  and dex jar automatically.

***
### best practice for development (recommend)

1. if unfamiliar with CSS or debug JavaScript, edit it by text editor you like and test with web browser first, no code editor of Server.
2. the added jar library, must be full tested in Eclipse or Java IDE. (the jar libraries are isolated if in difference projects, and never conflict; if in same project, be careful with conflict of class name)
3. to debug JRuby scripts and HAR, please with desktop server, not Android server. (Even if the target environment is Android server)
4. if the target environment is Android server, it is required to test UI and listeners of HTMLMlet. (J2SE Swing is not full supported by Android server), edit in code editor of desktop server, and one-click to hot deploy to Android server if in local net. (code instantly, deploy instantly, test instantly)
5. there are two servers at same time on above step, be careful, please use difference account when register; if conflict with account, please change it or verify account in error message dialog.
6. to print logs, please use "ProjectContext.log()" and "ProjectContext.error()", the message will be added prefix "[YourProjectID]". For Android server, the log will be print to LogCat (the filter is regex, please add prefix "\\" before chars "[]") , enable [option/developer/LogCat] on Android server is required.
7. to see log in console of Eclipse, create Java project in Eclipse, add starter.jar from [binary distribution](https://github.com/javalovercn/hc_server_dist), set Main class "starter.Starter", set VM arguments : "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8", and disable [option/developer/Logger] of server.
***
### How to use

1. source files are HomeCenter application server, NOT client for mobile.
2. JRE/JDK 7 or above is required.
3. main class : hc.App
4. VM arguments for main class : -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8
5. if fail on building, set [Compiler compliance level] of Eclipse to 1.7, NOT 1.8 or above.
6. properties are encoded by UTF-8.
7. keep the source codes for the newest, otherwise it may causes abnormal connection with mobile.
8. the demo project (NOT "MyFirst") in designer, covers 80% of JRuby syntax and 90% of functions of HomeCenter server.
9. for [binary distribution](https://github.com/javalovercn/hc_server_dist).
10. to host server on [non-UI server](https://github.com/javalovercn/homecenter/blob/master/docs/NoUI%20server%20on%20host.md).
11. welcome any questions, open issues https://github.com/javalovercn/homecenter/issues or Email : help at homecenter.mobi

***
### Mirrors

1. https://gitee.com/javalovercn/homecenter

***
### Snapshot of server

![splash](http://homecenter.mobi/images/splash_n_txt.png)

***

![designer](http://homecenter.mobi/images/usage/pc_designer.png)