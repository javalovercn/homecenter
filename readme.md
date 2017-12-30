### Snapshot of mobile
![screenshot](http://homecenter.mobi/images/sc6.png)
![screenshot](http://homecenter.mobi/images/sc_mlet_bg.png)
![screenshot](http://homecenter.mobi/images/sc8.png)

***
### Sample mobile UI
![screenshot](http://homecenter.mobi/images/sc_mlet.png)

to implement above UI functionality for Android and iPhone mobile, do it as follows ([details...](https://github.com/javalovercn/homecenter/blob/master/docs/develop%20cloud%20app%20with%20J2SE%2BCSS%20for%20iPhone%20and%20Android.md)).

start HomeCenter server, open designer, the project "MyFirst" is default.

click "Add Item" button, select "form", click "next",

select "HTMLMlet", click "OK", copy follow codes to "JRuby Scripts", and change "Display Name" with "FirstMlet".
```JRuby
#encoding:utf-8

import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.Dimension
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JTextArea
import javax.swing.JPanel
import javax.swing.ImageIcon
import javax.swing.SwingConstants
import Java::hc.server.ui.ProjectContext

class MyHTMLMlet < Java::hc.server.ui.HTMLMlet
	def initialize
		super #invoke super construct method
		
		@area = JTextArea.new()
		@btn_light = JButton.new()
		@btn_switch = JButton.new()
		
		@context = getProjectContext()
		@context.sendMovingMsg("loading images from network...")
		
		@icon_press_on = ImageIcon.new(ImageIO.read(URL.new("http://homecenter.mobi/images/press_on_64.png")))
		@icon_press_off = ImageIcon.new(ImageIO.read(URL.new("http://homecenter.mobi/images/press_off_64.png")))
		@icon_light_on = ImageIcon.new(ImageIO.read(URL.new("http://homecenter.mobi/images/light_on_64.png")))
		@icon_light_off = ImageIcon.new(ImageIO.read(URL.new("http://homecenter.mobi/images/light_off_64.png")))
		
		@isLightOn = false
		@btn_switch.setIcon(@icon_press_off)
		@btn_light.setIcon(@icon_light_off)
		@btn_switch.setToolTipText("light,lumière,Licht,빛,свет,灯")#for voice to open this form, open once is required.
		
		setCSS(@btn_switch, "iconStyle", nil)#iconStyle is defined global (as following) and is automatically loaded for all HTMLMlet in current project
		setCSS(@btn_light, "iconStyle", nil)
		
		buttonStyle = ".btnStyle{width:100%;height:100%;border-radius: " + getButtonHeight().to_s() + "px;display: block;transition: all 0.15s ease;border: 1px solid #4fc08d;background-color: #fff;color: #42b983;}"
		areaStyle = ".areaStyle{width:100%;height:100%;border: 1px solid #fff;font-size:" + getFontSizeForNormal().to_s() + "px;background-color:#fff;color:#42b983}"
		loadCSS(buttonStyle + areaStyle)
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
		buttonPanel.setPreferredSize(Dimension.new(getMobileWidth(), getButtonHeight()))
		setCSS(buttonPanel, nil, "background-color:white;")
		
		button = JButton.new("Screen")
		setCSSForDiv(button, "btnForDiv", nil)
		setCSS(button, "btnStyle", nil)
		button.addActionListener{|e|
			go(Java::hc.server.ui.Mlet::URL_SCREEN)#open desktop and control remote screen.
		}
		buttonPanel.add(button)
		
		button = JButton.new("Back")
		setCSSForDiv(button, "btnForDiv", nil)
		setCSS(button, "btnStyle", nil)
		button.addActionListener{|e|
			back()#exit and return back
		}
		buttonPanel.add(button)
		
		setLayout(BorderLayout.new())
		add(lightPanel, BorderLayout::NORTH)
		add(@area, BorderLayout::CENTER)
		add(buttonPanel, BorderLayout::SOUTH)
		
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
		@context.tipOnTray("Sys call onExit")
	end
end

return MyHTMLMlet.new
```

**Note : **

to enable connection permission to access image from network, select root of tree node (project node), click tab "Permission", un-check "limit socket/connect".

copy following CSS to "CSS Styles" tree node.
```css
.iconStyle {
	text-align:center;
	vertical-align:middle;
	width:100%;
	height:100%;
	transition: all 0.15s ease;
	border: 1px solid #fff;
	background-color: #fff;
	display: inline-block;
	box-sizing: border-box;
}
.btnForDiv {
	padding:0.2em;
	box-sizing: border-box;
	-webkit-box-sizing: border-box;
	-moz-box-sizing: border-box;
	display: -webkit-box;
	display: -moz-box;
	display: -ms-flexbox;
	display: -webkit-flex;
	display: flex;
	-webkit-box-align: center;
	-moz-box-align: center;
	-ms-flex-align: center;
	-webkit-align-items: center;
	align-items: center;
	justify-content: center;
	-webkit-justify-content: center;
	-webkit-box-pack: center;
	-moz-box-pack: center;
	-ms-flex-pack: center;
}
```

click "Activate" button to apply changes to project, login in from mobile, click "FirstMlet" menu item to show above form.
***
### License
1. please read and agree the license file "bcl.txt", "hc_license.txt" and "mpl_license.txt".

***
### Mobile Demo Account
1. download mobile client from https://github.com/javalovercn/client
2. click "Demo ID" in login form, or input account : 012345，password : 012345

***
### Features

1. HomeCenter server runs not only on Oracle JDK/JRE (OpenJDK), but also Android, because there is a library ["J2SE for Android"](https://github.com/javalovercn/j2se_for_android).
2. double click to run, no configuration.
3. scan QRcode from mobile to download and load HAR (home archive) project on server, plug and play, it is not required to restart server.
4. focus your business and UI with Java J2SE technology, the rest of work are ours.
5. the component HTMLMlet (the sample above) will be translated to HTML5+JavaScript+CSS for Android/iPhone mobile client on air, translate user inputs to J2SE Event and dispatch to listener.
6. built-in Lucene analysis HTMLMlet background, according to the most suitable form for voice command.
7. built-in Quartz job scheduler.
8. build-in HSQLDB database.
9. voice command API to drive IoT.
10. the designer on server is integrated with Java 8 API, Ruby 2.2.0 and CSS 2.2 (part) documents, hint of code and resource file(in user jar) will be auto completion.
11. stratify IoT, Robot+Converter+Device(device is required to open source or provide API), provides powerful device adaptability and data format conversion.
12. HAR project is self-signed and be upgraded automatically if the newer on web.
13. exception is automatically reported to your Email or website if end user enable reports exception.
14. build-in SecurityManager lets project runs in a optional, security, limited computing and networking environment.

***
### How to Use

1. these source files are HomeCenter Application Server, which is NOT client for mobile.
2. JRE/JDK 7 or above is required.
3. main class : hc.App
4. VM arguments for main class : -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8
5. set [Compiler compliance level] of Eclipse to 1.7, NOT 1.8 or above.
6. to print server log to console, please keep [options/Developer/Logger] unchecked in options of server
7. please keep the source code for the latest version, otherwise it may causes abnormal connections with mobile.
8. there is a demo HAR project (NOT "MyFirst" project) in designer on server, covers 80% of JRuby syntax and 90% of functions of HomeCenter server.
9. for API, please press alt+/ in designer or go http://homecenter.mobi/download/javadoc/index.html
10. to create server with your own account or for [host](https://github.com/javalovercn/homecenter/blob/master/docs/NoUI%20server%20on%20host.md), please run this source codes or [binary distribution](https://github.com/javalovercn/hc_server_dist).
11. any question, please open issues https://github.com/javalovercn/homecenter/issues or Email : help at homecenter.mobi

***
### Mirrors

1. https://gitee.com/javalovercn/homecenter

***
### Snapshot of server

![splash](http://homecenter.mobi/images/splash_n_txt.png)

***

![designer](http://homecenter.mobi/images/usage/pc_designer.png)