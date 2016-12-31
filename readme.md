### Sample
![screenshot](https://homecenter.mobi/images/sc_mlet.png)

to implement above UI functionality for Android, iPhone, and J2ME mobile,

the full codes are following :
```JRuby
#encoding:utf-8

require 'testlib.jar'

import javax.swing.JButton
import javax.swing.JTextArea
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.ImageIcon
import java.net.URL
import java.awt.Dimension
import javax.imageio.ImageIO
import javax.swing.SwingConstants
import Java::hc.server.ui.ProjectContext

class MyHTMLMlet < Java::hc.server.ui.HTMLMlet
	def initialize
		super #invoke super construct method
		
		@area = JTextArea.new()
		@btn_light = JButton.new()
		@btn_switch = JButton.new()
		@icon_press_on = ImageIcon.new(ImageIO.read(URL.new("http://homecenter.mobi/images/press_on_64.png")))
		@icon_press_off = ImageIcon.new(ImageIO.read(URL.new("http://homecenter.mobi/images/press_off_64.png")))
		
		@context = getProjectContext()
				
		tClass = Java::test.TestClass#this class is in testlib.jar
		@png_url = tClass.java_class.resource("/test/light_on_64.png")#the icon is in testlib.jar
		@icon_light_on = ImageIcon.new(@png_url)#get image from testlib.jar
		@png_url = tClass.java_class.resource("/test/light_off_64.png")
		@icon_light_off = ImageIcon.new(@png_url)
		
		@isLightOn = false
		@btn_switch.setIcon(@icon_press_off)
		@btn_light.setIcon(@icon_light_off)
		
		setCSS(@btn_switch, "iconStyle", nil)#iconStyle is defined "CSS Styles" and is automatically loaded for all HTMLMlet in current project
		setCSS(@btn_light, "iconStyle", nil)
		
		cssStyle = ".areaStyle{width:100%;height:100%;font-size:" + getFontSizeForNormal().to_s() + "px;color:green}"
		loadCSS(cssStyle)
		setCSS(@area, "areaStyle", nil)#areaStyel is defined cssStyle string.
		#it equals with setCSS(@area, nil, "width:100%;height:100%;font-size:" + getFontSizeForNormal().to_s() + "px;color:green")
		@area.setEditable(false)
		
		lightPanel = JPanel.new
		lightPanel.setLayout(GridLayout.new(1, 2))
		lightPanel.add(@btn_light)
		lightPanel.add(@btn_switch)
				
		@btn_switch.addActionListener{|e|
			@area.append("click switch\n")
			@isLightOn = !@isLightOn
			if @isLightOn
				@context.sendMovingMsg("light on")
				@btn_switch.setIcon(@icon_press_on)
				@btn_light.setIcon(@icon_light_on)
			else
				@context.sendMovingMsg("light off")
				@btn_switch.setIcon(@icon_press_off)
				@btn_light.setIcon(@icon_light_off)
			end
		}
			
		buttonPanel = JPanel.new()
		buttonPanel.setLayout(GridLayout.new(1, 2))
		buttonPanel.setPreferredSize(Dimension.new(@context.getMobileWidth(), getButtonHeight()))

		button = JButton.new("Screen")
		setCSS(button, "btnStyle", nil)
		button.addActionListener{|e|
			go(Java::hc.server.ui.Mlet::URL_SCREEN)#open desktop and control remote screen.
		}
		buttonPanel.add(button)

		button = JButton.new("Back")
		setCSS(button, "btnStyle", nil)
		button.addActionListener{|e|
			back()#exit and return back
		}
		buttonPanel.add(button)

		setLayout(BorderLayout.new())
		add(lightPanel, BorderLayout::NORTH)
		add(@area, BorderLayout::CENTER)
		add(buttonPanel, BorderLayout::SOUTH)
		
		setCSS(self, nil, "background-color:white;")#override the default color styles.
	end

	#override empty method onStart
	def onStart
		@area.append("Sys call onStart\n")
	end

	#override empty method onPause
	def onPause
		@area.append("Sys call onPause\n")
	end

	#override empty method onResume
	def onResume
		@area.append("Sys call onResume\n")
	end

	#override empty method onExit
	def onExit
		@context.tipOnTray("[Mobile UI Designer] Sys call onExit on Screen-Mlet")
	end
end

return MyHTMLMlet.new
```
global CSS for current project :
```css
.iconStyle {
	text-align:center;
	vertical-align:middle;
	width:100%;
	height:100%;
}

.btnStyle {
	width:100%;
	height:100%;
	color:#fff;
	font-size:$buttonFontSize$px;
	background-color:#54a1d9;
	background-image:-webkit-gradient(linear, 0 0, 0 100%, color-stop(0, #8fc2e8), color-stop(0.5, #54a1d9), color-stop(0.5, #126aa9), color-stop(1, #2ddef2));
	-webkit-border-radius:9px;
	border:1px solid #377daf;
	-webkit-box-shadow:0 2px 4px rgba(46,185,230,0.7);
}
```
***
### License
1. please read and agree the license file "bcl.txt", "hc_license.txt" and "mpl_license.txt".

***
### Main Features

1. just double-click to run, no configuration.
2. HomeCenter server runs not only on Oracle/OpenJDK JVM, but also Android, because there is a library ["J2SE for Android"](https://github.com/javalovercn/j2se_for_android).
3. scan QRcode from mobile to download and load HAR (home archive) project on server, plug and play, not required to restart server.
4. focus your business and UI with Java J2SE technology, the rest of work are ours.
5. the component HTMLMlet is implements with JComponent+Listener+Layout+CSS on server and it will be translated to HTML5+JavaScript+CSS for Android/iPhone mobile client on air.
6. the designer on server is integrated with Java API Doc and is also an IDE for JRuby, hint of code and resource file(in jar library) will be auto completion.
7. stratify IoT, Robot+Converter+Device, provides powerful device adaptability and data format conversion.
8. HAR project is self-signed and be upgraded automatically if the newer on web.
9. exception is automatically reported to your Email or website if end user enable reports exception.
10. build-in SecurityManager lets project runs in a optional, security, limited computing and networking environment.

***
### How to use source

1. these source files are HomeCenter Application Server, which is on server side, NOT client for mobile.
2. JRE/JDK 7 or above is required.
3. main class : hc.App
4. VM arguments for main class : -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8
5. set [Compiler compliance level] of Eclipse to 1.7 or upper
6. to print server log to console, please keep [options/Developer/Logger] unchecked
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

***

![designer](https://homecenter.mobi/images/usage/pc_designer.png)