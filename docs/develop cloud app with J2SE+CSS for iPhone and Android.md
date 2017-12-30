# Develop cloud app with J2SE+CSS for iPhone and Android

 
## HTMLMlet
HTMLMlet is extends J2SE JPanel, setting CSS for JComponent is our magical function, when initialization, the server will convert it to HTML5+JavaScript+CSS in real time for mobile, after user inputs, convert them to J2SE Events and trigger the listeners of JComponents.

Focusing on UI and Event with J2SE technology, all other work is left to the container.

let's do following three steps to delivery a cross-platform cloud app with J2SE+CSS.

![](http://homecenter.mobi/images/sc_mlet.png)

open &quot;designer&quot; on server, (if not setup server, see part &quot;**Appendix - Installation Development Environment**&quot;).

the project &quot;MyFirst&quot; is opened as default.

### 1. add HTMLMlet

click &quot;Add Item&quot; button on toolbar, it will display as following :

![](http://homecenter.mobi/images/usage/j2se_css_img2_en.png)

select &quot;form&quot;, click &quot;Next&quot; :

![](http://homecenter.mobi/images/usage/j2se_css_img3_en.png)

select &quot;HTMLMlet&quot;, click &quot;OK&quot;.

![](http://homecenter.mobi/images/usage/j2se_css_img4.png)

change &quot;Display Name&quot; as &quot;HTMLMlet&quot;, &quot;Target Locator&quot; as &quot;MyHTMLMlet&quot;.

paste following codes to &quot;JRuby Script&quot;. (Note : for demo, button image is downloaded from net, in your practices, putting images in jar is good choice. )
```ruby
#encoding:utf-8

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
		
		@icon_light_on = ImageIcon.new(ImageIO.read(URL.new("http://homecenter.mobi/images/light_on_64.png")))
		@icon_light_off = ImageIcon.new(ImageIO.read(URL.new("http://homecenter.mobi/images/light_off_64.png")))
		
		@isLightOn = false
		@btn_switch.setIcon(@icon_press_off)
		@btn_light.setIcon(@icon_light_off)
		@btn_switch.setToolTipText("light,lumière,Licht,빛,свет,灯")#speak 'light' to open this form. (opening once is required, build-in lucene will record it for voice command )
		
		setCSS(@btn_switch, "iconStyle", nil)#iconStyle is defined global (as following) and is automatically loaded for all HTMLMlet in current project
		setCSS(@btn_light, "iconStyle", nil)
		
		buttonStyle = ".btnStyle{width:100%;height:100%;border-radius: " + getButtonHeight().to_s() + "px;display: block;transition: all 0.15s ease;border: 0.1em solid #4fc08d;background-color: #fff;color: #42b983;}"
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
			go(Java::hc.server.ui.Mlet::URL_SCREEN)#open desktop of server for remote access
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

**Note :**

to download button image from net, the internet permission is required, click &quot;My Project&quot; node, click &quot;Permission&quot; tab, un-check &quot;limit socket/connect&quot;.

### 2. global CSS

click &quot;Resources/CSS Styles&quot; on the left tree, putting following CSS to&quot;Styles Edit Area&quot;.
```CSS
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
	padding:0.2em;/*default box-sizing: border-box;*/
	display: table-cell;
	text-align: center;
	vertical-align: center;
}
```

### 3. active

click &quot;Activate&quot; button on toolbar, server will query to save or not, click&quot;Yes&quot;, then server will reload current project, displaying following means OK :

![](http://homecenter.mobi/images/usage/j2se_css_img5_en.png)

congratulation, HTMLMlet is ready, access it from mobile client now!

to delivery the project, input the download URL into &quot;MyFirst/Upgrade URL&quot;, click &quot;Save As&quot; button to export project. (if you had created &quot;Developer Certificates&quot;, project will be signed also when export)

upload two files (the extension is har and had), generate QR image to download had, tell your friends to scan it from client to add the project to their servers.

## More Surprised
  1. liking JavaScript, [ScriptPanel](http://homecenter.mobi/download/javadoc/hc/server/ui/ScriptPanel.html) comes for you.
  2. through the voice assistant API, in response to the mobile voice commands, to achieve more magical and interesting application scenarios.
  3. build-in HSQLDB.
  4. click &quot;Demo&quot; to load demo project, it covers more than 80% JRuby grammar and more than 90% frequently functions.
  5. Java 8 API and CSS 2.2 documents.
  6. without any changes, it can run on Android, because there is a open source &quot;[J2SE for Android](https://github.com/javalovercn/j2se_for_android)&quot; library on the server for Android (it will transcode jar to dex when adding project).
  7. for the newer source codes of server, please click [https://github.com/javalovercn/homecenter](https://github.com/javalovercn/homecenter)

## Appendix - Installation Development Environment
1. JRE 7 (Java Runtime Environment) or JDK 7 (Java SE Development Kit) or later is required, for install, open [https://www.java.com/](https://www.java.com/).
2. install HomeCenter, open [https://github.com/javalovercn/hc_server_dist](https://github.com/javalovercn/hc_server_dist), download package for your OS, for example, the package for Windows is &quot;HC\_Server\_For\_Win.zip&quot;.
3. install client for mobile, open [https://github.com/javalovercn/client](https://github.com/javalovercn/client).
4. unzip package, double click batch or script to start server, for example, Windows is &quot;HomeCenter.bat&quot;; Mac is &quot;HomeCenter.command&quot;.
5. it will download the newer kernel of server.
6. agree license and continue.
7. input Email for account. Note : you can verify Email later.
8. when server successful line on, it display as following :

![](http://homecenter.mobi/images/usage/j2se_css_img6_en.png)

9. server will download JRuby engine background for first installation. It is a jar library, about 20M, when it is OK, then shows :

![](http://homecenter.mobi/images/usage/j2se_css_img7_en.png)

10. congratulation, all is ready for develop HAR (Home Archive) project! (if you are end user, and don't want to develop HAR, login with mobile client, scan QR code from friends or providers to install HAR project, plug is play)
11. click &quot;OK&quot; to enter designer, it shows like this :

![](http://homecenter.mobi/images/usage/j2se_css_img8.png)

12. the mechanism of server : the load unit is HAR (Home Archive), it is jar file and encapsulate scripts, icons, executable jar on which it depends. Don't worry about delivery project, just clicking &quot;Save As&quot; to export and sign project. The server is not only container, but also designer (Note : the server for Android is not a designer). HAR runs on JRuby engine. JRuby is a pure-Java implementation of the Ruby interpreter that combines the simplicity of the Ruby language with the powerful JVM execution mechanism, including full integration with the Java library.
