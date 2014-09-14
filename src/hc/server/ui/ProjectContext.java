package hc.server.ui;

import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.data.DataPNG;
import hc.core.util.ByteUtil;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.StarterManager;
import hc.server.data.screen.KeyComper;
import hc.util.PropertiesManager;
import hc.util.PropertiesMap;
import hc.util.ResourceUtil;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.imageio.ImageIO;

/**
 * there are three way to get instance of ProjectContext
 * <br>1. call {@link hc.server.ui.Mlet#getProjectContext()}
 * <br>2. call {@link hc.server.ui.CtrlResponse#getProjectContext()}
 * <br>3. call {@link hc.server.ui.ProjectContext#getProjectContext()}
 */
public class ProjectContext {
	private final String projectID;
	private final String projectVer;
	final Hashtable<String, Object> obj_map = new Hashtable<String, Object>();
	private static ProjectContext staticContext;
	/**
	 * @deprecated
	 * @param id
	 * @param ver
	 */
	public ProjectContext(final String id, final String ver){
		projectID = id;
		projectVer =  ver;
		staticContext = this;
	}
	
	/**
	 * @return the version of HomeCenter app server.
	 */
	public String getHomeCenterVersion(){
		return StarterManager.getHCVersion();
	}
	
	/**
	 * @return the version of current HAR project.
	 */
	public String getProjectVersion(){
		return projectVer;
	}
	
	public String getProjectID(){
		return projectID;
	}
	
	/**
	 * @return the width pixel of login mobile
	 */
	public int getMobileWidth(){
		return ClientDesc.clientWidth;
	}
	
	/**
	 * Binds an object to a given attribute name in this context.
	 * @see #removeAttribute(String)
	 * @see #getAttribute(String)
	 * @see #getAttributeNames()
	 * @param name
	 * @param obj
	 */
	public void setAttribute(String name, Object obj){
		obj_map.put(name, obj);
	}
	
	/**
	 * Removes the attribute with the given name from the context.
	 * @see #setAttribute(String, Object)
	 * @see #getAttribute(String)
	 * @see #getAttributeNames()
	 * @param name
	 */
	public void removeAttribute(String name){
		obj_map.remove(name);
	}
	
	/**
	 * Returns the attribute with the given name, or null if there is no attribute by that name.
	 * @see #getAttributeNames()
	 * @see #setAttribute(String, Object)
	 * @see #removeAttribute(String)
	 * @param name
	 * @return
	 */
	public Object getAttribute(String name){
		return obj_map.get(name);
	}
	
	/**
	 * Returns an Enumeration containing the attribute names available within this context.
	 * @see #getAttribute(String)
	 * @see #setAttribute(String, Object)
	 * @see #removeAttribute(String)
	 * @return
	 */
	public Enumeration getAttributeNames(){
		return obj_map.keys();
	}
	
	/**
	 * @return the height pixel of login mobile
	 */
	public int getMobileHeight(){
		return ClientDesc.clientHeight;
	}
	
	/**
	 * in Android, it means densityDpi;
	 * @return the DPI of login mobile, if 0 means unknown.
	 */
	public int getMobileDPI(){
		return ClientDesc.dpi;
	}
	
	/**
	 * set a property with new value. Call saveProperties() method is required to save to persistent system.
	 * @see #getProperty(String)
	 * @see #removeProperty(String)
	 * @see #saveProperties()
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value){
		if(prop_map == null){
			init();
		}
		prop_map.put(key, value);
	}

	private void init() {
		prop_map = new PropertiesMap(PropertiesManager.p_PROJ_RECORD + getProjectID());
	}
	
	/**
	 * @return null if not save or set before.
	 * @see #setProperty(String, String)
	 * @see #removeProperty(String)
	 * @see #saveProperties()
	 */
	public String getProperty(String key){
		if(prop_map == null){
			init();
		}
		return prop_map.get(key);
	}
	
	/**
	 * @see #getProperty(String)
	 * @see #setProperty(String, String)
	 * @see #saveProperties()
	 * @param key
	 */
	public void removeProperty(String key){
		if(prop_map == null){
			init();
		}
		prop_map.remove(key);
	}
	
	/**
	 * save properties of current project to persistent system.
	 * <br><br>it is a good practice to save small data in properties system and save big data in cloud system.
	 * <br>creating local files for HAR project is not recommended, and it may be forbidden in future.
	 * <br>data will be deleted if the project is removed.
	 * @see #setProperty(String, String)
	 * @see #getProperty(String)
	 * @see #removeProperty(String)
	 */
	public void saveProperties(){
		if(prop_map == null){
			return;
		}
		
		prop_map.save();
	}
	
	PropertiesMap prop_map;
	
	/**
	 * three legal values:
	 * <br>1. null
	 * <br>2. language ("en", "fr", "ro", "ru", etc.)
	 * <br>3. language-region ("en-GB", "en-CA", "en-IE", "en-US", etc.)
	 * @return mobile locale
	 */
	public String getMobileLocale(){
		return ClientDesc.clientLang;
	}
	
	/**
	 * @deprecated
	 * @return
	 */
	public String __getTargetFromInnerMethod(){
		return __tmp_target;
	}
	
	/**
	 * action keyboard keys, such as Control+Shift+Escape or Tab only. <p>
	 * Control : KeyEvent.VK_CONTROL<br>
	 * Shift : KeyEvent.VK_SHIFT<br>
	 * Escape : KeyEvent.VK_ESCAPE<br>
	 * Meta : KeyEvent.VK_META (Max OS X : Command)<p>
	 * more key string , please refer <a target="_blank" href="http://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html">http://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html</a><br>
	 * NOTE : NOT all keys are supported
	 * @param keys for example, "Control+Shift+Escape" or "Tab"
	 */
	public void actionKeys(String keys){
		KeyComper.actionKeys(keys);
	}

	/**
	 * log error message to HomeCenter server
	 * @param msg
	 */
	public void err(String msg){
		LogManager.errToLog(msg);
	}

	/**
	 * log message to HomeCenter server
	 * @param msg
	 */
	public void log(String msg){
		L.V = L.O ? false : LogManager.log(msg);
	}

	private static String[] buildParaForClass(final int paraNum){
		String[] p = new String[paraNum + 1];
		p[0] = HCURL.DATA_PARA_CLASS;
		for (int i = 1; i <= paraNum; i++) {
			p[i] = String.valueOf(i);
		}
		return p;
	}

	private static void sendCmd(String cmdType, String para, String value){
		if(isToMobile()){
			HCURLUtil.sendCmd(cmdType, para, value);
		}
	}

	private static void sendCmd(String cmdType, String[] para, String[] value){
		if(isToMobile()){
			HCURLUtil.sendCmd(cmdType, para, value);
		}
	}

	private static void sendClass(String[] para){
		ProjectContext.sendCmd(HCURL.DATA_CMD_SendPara, ProjectContext.buildParaForClass(para.length - 1), para);
	}

	/**
	 * play tone at mobile, please disable mute on mobile first.
	 * @param note A note is given in the range of 0 to 127 inclusive. Defines the tone of the note as specified by the above formula.
	 * @param duration The duration of the tone in milli-seconds. Duration must be positive.
	 * @param volume Audio volume range from 0 to 100. 100 represents the maximum
	 */
	public void playTone(int note, int duration, int volume){
		String[] v = {"hc.j2me.load.Tone", String.valueOf(note), String.valueOf(duration), String.valueOf(volume)};
		ProjectContext.sendClass(v);
	}

	/**
	 * display notification on android/IOS
	 * @param title
	 * @param text
	 * @param flags FLAG_NOTIFICATION_SOUND : notification with sound, need disable mute option in mobile configuration; FLAG_NOTIFICATION_VIBRATE : notification with vibrate, need enable vibrate of Android
	 */
	public void sendNotification(String title, String text, int flags){
		String[] v = {"hc.j2me.load.Notification", title, text, String.valueOf(flags)};
		ProjectContext.sendClass(v);
	}

	/**
	 * Requests operation of the mobile device's vibrator, some mobile will do nothing.
	 * @param duration the number of milliseconds the vibrator should be run
	 */
	public void vibrate(int duration){
		String[] v = {"hc.j2me.load.Vibrate", String.valueOf(duration)};
		ProjectContext.sendClass(v);
	}

	public void alertOff(){
		ProjectContext.sendCmd(HCURL.DATA_CMD_ALERT, "status", "off");
	}

	public void alertOn(){
		ProjectContext.sendCmd(HCURL.DATA_CMD_ALERT, "status", "on");
	}

	/**
	 * @see sendMessage
	 * @param caption
	 * @param text
	 * @param type
	 */
	public void send(String caption, String text, int type){
		try{
			sendMessage(caption, text, type, null, 0);
		}catch (Throwable e) {
			//设计时，手机非在线时，
		}
	}

	/**
	 * because the HomeCenter server is open source, login ID may be a fake, so authentication is required for HAR project.
	 * @return HomeCenter server login ID.
	 */
	public String getLoginID(){
		return IConstant.uuid;
	}

	/**
	 * important : this method is available in JRuby-executing thread, it will return null in new thread or event thread.
	 * @return
	 */
	public static ProjectContext getProjectContext(){
		return ProjectContextManager.getThreadObject();
	}

	/**
	 * @deprecated
	 * @param caption
	 * @param text
	 * @param type
	 * @param image
	 * @param timeOut
	 */
	public static void sendStaticMessage(String caption, String text, int type, BufferedImage image, int timeOut){
		staticContext.sendMessage(caption, text, type, image, timeOut);
	}
	
	/**
	 * send a alert dialog to mobile, 
	 * @param caption
	 * @param text
	 * @param type 	1 = ERROR, 2 = WARN, 3 = INFO, 4 = ALARM, 5 = CONFIRMATION;
	 * @param image
	 * @param timeOut 0:forever;a positive time value in milliseconds
	 */
	public void sendMessage(String caption, String text, int type, BufferedImage image, int timeOut){
		if(isToMobile()){
			String imageData = null;
			if(image != null){
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				try {
					ImageIO.write(image, "png", byteArrayOutputStream);
					byte[] out = byteArrayOutputStream.toByteArray();
					imageData = "&image=" + ByteUtil.encodeBase64(out);
					byteArrayOutputStream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
			}
			String url = HCURL.CMD_PROTOCAL + "://" + HCURL.DATA_CMD_MSG + 
				"?caption=" + StringUtil.replace(caption, "&", "\\&") + 
				"&text=" + StringUtil.replace(text, "&", "\\&") +
				"&timeOut=" + timeOut +			
				"&type=" + String.valueOf(type) + ((imageData==null)?"":imageData);
			ContextManager.getContextInstance().send(MsgBuilder.E_GOTO_URL, url);
		}
	}

	public void sendAUSound(byte[] bs) {
		try{
	        final long length = bs.length;
	    
	        final int HEAD = DataPNG.HEAD_LENGTH + MsgBuilder.INDEX_MSG_DATA;
			final byte[] bytes = ByteUtil.byteArrayCacher.getFree((int)length + HEAD);
	        System.arraycopy(bs, 0, bytes, HEAD, bs.length);
	
	        DataPNG blob = new DataPNG();
	        blob.bs = bytes;
	        
	        blob.setPNGDataLen((int)length, 0, 0 , 0, 0);
	        ContextManager.getContextInstance().sendWrap(MsgBuilder.E_SOUND, bytes, MsgBuilder.INDEX_MSG_DATA, 
	        		(int)length + DataPNG.HEAD_LENGTH);
	        
	        ByteUtil.byteArrayCacher.cycle(bytes);
	        
	        LogManager.log("AU length:" + length);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @deprecated
	 * @param msg
	 */
	public static void tipStaticOnTray(final String msg){
		staticContext.tipOnTray(msg);
	}
	
	/**
	 * display a tip message on HomeCenter server tray
	 * @param msg
	 */
	public void tipOnTray(final String msg){
		ContextManager.displayMessage((String) ResourceUtil.get(IContext.INFO), 
				msg, IContext.INFO, 0);
	}

	private static boolean isToMobile() {
		return ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF;
	}

	/**
	 * @deprecated
	 * @param msg
	 */
	public static void sendStaticMovingMsg(String msg) {
		staticContext.sendMovingMsg(msg);
	}
	
	/**
	 * display a message moving from right to left on mobile
	 * @param msg
	 */
	public void sendMovingMsg(String msg) {
		if(ProjectContext.isToMobile()){
			HCURLUtil.sendCmd(HCURL.DATA_CMD_MOVING_MSG, "value", msg);
		}
	}

	/**
	 * @deprecated
	 * @param keyValue
	 * @param text
	 */
	public static void __sendTextOfCtrlButton(int keyValue, String text){
		if(ProjectContext.isToMobile()){
			String[] keys = {"key", "text"};
			String[] values = {String.valueOf(keyValue), text};
			HCURLUtil.sendCmd(HCURL.DATA_CMD_CTRL_BTN_TXT, keys, values);
		}
	}

	/**
	 * @deprecated
	 */
	public String __tmp_target;
	public static final int FLAG_NOTIFICATION_VIBRATE = ConfigManager.FLAG_NOTIFICATION_VIBRATE;
	public static final int FLAG_NOTIFICATION_SOUND = ConfigManager.FLAG_NOTIFICATION_SOUND;
	public static final String EVENT_SYS_PROJ_SHUTDOWN = "SYS_PROJ_SHUTDOWN";
	public static final String EVENT_SYS_PROJ_STARTUP = "SYS_PROJ_STARTUP";
	public static final String EVENT_SYS_MOBILE_LOGIN = "SYS_MOBILE_LOGIN";
	public static final String EVENT_SYS_MOBILE_LOGOUT = "SYS_MOBILE_LOGOUT";
}
