# HomeCenter：用J2SE+CSS开发跨手机平台的云应用

 
## 开发第一个HTMLMlet
新型交互面板HTMLMlet，继承J2SE的JPanel，遵循J2SE的界面开发方法，通过API为JComponent设置CSS，以获得CSS强大展示，实例化时，服务器将其转换为手机端所需的HTML5+JavaScript+CSS，并将用户响应转换为J2SE的Event，驱动JComponent侦听器。

因此，仅需关注业务界面构造和事件响应，其它则全部交与容器。

以下我们通过三步，用J2SE+CSS来实现如下图所示的可运行于iPhone和Android的云应用。

![](http://homecenter.mobi/images/sc_mlet.png)

打开服务器端的&quot;设计器&quot;，如果没有安装，请参阅&quot;**附录-安装开发环境**&quot;。

### 1. 添加一个HTMLMlet的菜单项

点击工具条中的&quot;Add Item&quot;按钮后，显示如下：

![](http://homecenter.mobi/images/usage/j2se_css_img2.png)

选中&quot;form&quot;，点击&quot;下页&quot;：

![](http://homecenter.mobi/images/usage/j2se_css_img3.png)

选中&quot;HTMLMlet&quot;，点击&quot;确认&quot;。

![](http://homecenter.mobi/images/usage/j2se_css_img4.png)

将&quot;Display Name&quot;改为&quot;HTMLMlet&quot;，&quot;Target Locator&quot;改为&quot;MyHTMLMlet&quot;。

粘贴如下代码到&quot;JRuby Script&quot;区。（注意：按钮图片通过网络加载，实际应用时，可存放在jar内，以免网络迟延或故障）
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
		@btn_switch.setToolTipText("light,lumière,Licht,빛,свет,灯")#说“灯”便可打开这个界面。（至少打开一次，内置lucene便会记录，供语音命令之用）
		
		setCSS(@btn_switch, "iconStyle", nil)#iconStyle is defined global (as following) and is automatically loaded for all HTMLMlet in current project
		setCSS(@btn_light, "iconStyle", nil)
		
		buttonStyle = ".btnStyle{width:100%;height:100%;border-radius: " + getButtonHeight().to_s() + "px;display: inline-block;transition: all 0.15s ease;border: 1px solid #4fc08d;background-color: #fff;color: #42b983;}"
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
			go(Java::hc.server.ui.Mlet::URL_SCREEN)#打开电脑桌面，远程访问屏幕
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

**注意：**

由于本示例使用了联网，请点击左侧“My Project”节点，点击“Permission”分页，取消“limit socket/connect”勾选。

### 2. 设置全局CSS

点击左侧树的Resources/CSS Styles，将下面CSS粘贴到&quot;Styles Edit Area&quot;。
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
	padding:0.2em;
	-webkit-box-sizing: border-box;
	-moz-box-sizing: border-box;
	box-sizing: border-box;
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

### 3. 发布应用

点击工具条中的&quot;Activate&quot;按钮，系统询问是否保存修改，点击&quot;是&quot;，成功加载工程后，显示如下：

![](http://homecenter.mobi/images/usage/j2se_css_img5.png)

祝贺您，第一个HTMLMlet已就诸，通过手机，可以访问啦！

此时工程仅供您的手机访问，您的好友并不能下载并安装到他们自己的服务器上（除非您将自己服务器的帐号和密码告诉他们），将可供下载的网址复制到&quot;MyFirst/Upgrade URL&quot;输入框中，点击工具条中的&quot;Save As&quot;，就可导出工程。（如果您已在&quot;Developer Certificates&quot;创建了证书，导出的同时进行签名）

将导出的har和had两文件上传，生成had的下载网址的二维码图片，好友即可扫描，安装到各自的服务器上了。

## 更多惊喜
  1. 如果您喜欢用JavaScript来实现手机端应用的特效，请参阅[ScriptPanel](http://homecenter.mobi/download/javadoc/hc/server/ui/ScriptPanel.html)组件。
  2. 通过语音助手API，响应手机的语音指令，实现更多神奇、有趣的应用场景。
  3. 内置HSQLDB小型数据库。
  4. 通过工具条上的&quot;Demo&quot;按钮，加载演示工程，它涵盖了80%以上的JRuby语法和90%以上的常用功能。
  5. 服务器内置Java 8 API文档和CSS 2.2文档，自动代码提示和API显示。
  6. HAR无需任何修改，便能运行于安卓平台上，因为安卓版使用了&quot;J2SE for Android&quot;开源包（如含有标准的jar库，会在安装时，转码成dex）。
  7. 关注、下载最新服务器源代码，请转至[https://github.com/javalovercn/homecenter](https://github.com/javalovercn/homecenter)

## 附录-安装开发环境
1. 确保已安装Java运行环境(Java Runtime Environment)或Java开发环境(Java SE Development Kit)，没安装，从[https://www.java.com/](https://www.java.com/)下载安装。
2. 安装HomeCenter启动器，打开[https://github.com/javalovercn/hc_server_dist](https://github.com/javalovercn/hc_server_dist)，下载相应操作系统压缩包，比如Windows系统，则是&quot;HC\_Server\_For\_Win.zip&quot;。
3. 手机端下载，请打开[https://github.com/javalovercn/client](https://github.com/javalovercn/client)。
4. 解压包，双击可执行脚本。例如：Windows系统是&quot;HomeCenter.bat&quot;；Mac系统则是&quot;HomeCenter.command&quot;。
5. 启动器在运行时，会检查、下载最新的服务器内核。
6. 下载成功后，系统显示软件使用许可协议对话框，点选&quot;我同意&quot;项，点击&quot;同意&quot;继续。
7. 输入电子邮箱作为帐号。注：账号创建完后，并不能表明你是该邮箱的拥有者，稍后可进行邮箱验证。
8. 服务器上线成功后，弹出如下信息：

![](http://homecenter.mobi/images/usage/j2se_css_img6.png)

9. 初次安装，服务器会在后台下载JRuby引擎。JRuby引擎是一个jar包，约20M，下载校验后，系统提示如下：

![](http://homecenter.mobi/images/usage/j2se_css_img7.png)

10. 祝贺您，现在可以开始编写第一个HAR工程了！（如果您是最终用户，且不需开发HAR，手机登录后，扫描好友或供应商的二维码以安装HAR工程包，即加即用。）
11. 点击&quot;确认&quot;，进入设计器，界面如下：

![](http://homecenter.mobi/images/usage/j2se_css_img8.png)

12. 服务器的基本机理：服务器加载单元是HAR（Home Archive）工程包，采用jar格式封装应用的脚本、图标或所依赖的可执行jar。无需担忧如何发布工程包，点击&quot;Save As&quot;按钮即可完成HAR包的导出和签名。服务器既是运行容器，也是设计器，即HAR的开发环境（注：Android版的服务器除外，这是因为Android并不适合脚本编写，所以Android版仅供运行，而非开发。特别说明：此处的Android版为服务器端，而非手机端的Android版本）。HAR工程运行在JRuby解释器上。JRuby是采用纯Java实现的Ruby解释器，它结合了Ruby语言的简易性和功能强大的JVM的执行机制，包括与Java库全面集成。
