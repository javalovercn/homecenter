package hc.server.ui.design.hpj;

import hc.core.util.CNCtrlKey;
import hc.core.util.CtrlKey;
import hc.core.util.HCURL;
import hc.server.msb.Converter;
import hc.server.msb.Device;
import hc.server.msb.Robot;
import hc.server.ui.CtrlResponse;
import hc.server.ui.HTMLMlet;
import hc.server.ui.Mlet;

import java.lang.reflect.Field;
import java.util.Vector;

public class ScriptModelManager {
	static final String SUPER = "super";

	public static String buildDefaultScript(final int type, final HCURL url){
		if(type == HPNode.TYPE_MENU_ITEM_CONTROLLER){
			final String[] imports = {"import Java::hc.core.util.CtrlKey\n\n"};
			final String instanceName = "MyController";
			final String superClassName = CtrlResponse.class.getName();
			final String[] methods = {"click(keyValue)", "getButtonInitText(keyValue)", "onLoad", "onExit"};
			final Vector<String>[] codes = new Vector[4];
			codes[0] = buildAllKeyExample();
			codes[1] = buildInitText();
			codes[2] = buildOnLoad();
			final boolean[] isEmpty = {false, true, true, true};			
			final String[] superCodes = {SUPER};
			return buildScript(imports, instanceName, superClassName, superCodes, isEmpty, methods, codes);
		}else if(type == HPNode.TYPE_MENU_ITEM_SCREEN || type == HPNode.TYPE_MENU_ITEM_FORM){
			if(url.elementID.equals(HCURL.REMOTE_HOME_SCREEN)){
			}else if(type == HPNode.TYPE_MENU_ITEM_FORM){
				if(url.elementID.indexOf(TypeWizard.htmlmlet) >= 0){
					final String instanceName = "MyHTMLMlet";
					final String superClassName = HTMLMlet.class.getName();
					final String[] methods = {"onStart", "onPause", "onResume", "onExit"};
					final boolean[] isEmpty = {true, true, true, true};
					final String[] superCodes = {SUPER};
					return buildScript(null, instanceName, superClassName, superCodes, isEmpty, methods, null);
				}else{
					final String instanceName = "MyMlet";
					final String superClassName = Mlet.class.getName();
					final String[] methods = {"onStart", "onPause", "onResume", "onExit"};
					final boolean[] isEmpty = {true, true, true, true};
					final String[] superCodes = {SUPER};
					return buildScript(null, instanceName, superClassName, superCodes, isEmpty, methods, null);
				}
			}
		}else if(type == HPNode.TYPE_MENU_ITEM_CMD){
			if(url.elementID.equals(HCURL.DATA_CMD_EXIT) 
					|| url.elementID.equals(HCURL.DATA_CMD_CONFIG)){
			}else{
				//my-command
				return buildMyCommand();
			}
		}else if(type == HPNode.TYPE_MENU_ITEM_IOT){
			if(url.url.indexOf(HCURL.DATA_IOT_ROBOT.toLowerCase()) >= 0){
				final String[] imports = {
//						"# for Robot API, http://homecenter.mobi/download/javadoc/hc/server/msb/Robot.html\n\n"
//						,"# for demo, http://homecenter.mobi/en/pc/steps_iot.htm\n\n"
						"import Java::hc.server.msb.Message\n\n"
						};
				final String instanceName = "MyRobot";
				final String superClassName = Robot.class.getName();
				final String[] methods = {"operate(functionID, parameter)", "declareReferenceDeviceID", 
						"getDeviceCompatibleDescription(referenceDeviceID)", "response(msg)", "startup", "shutdown"};
				final Vector<String>[] codes = new Vector[methods.length];
				{
					final Vector<String> returnNil = new Vector<String>();
					returnNil.add("return nil");
					codes[0] = returnNil;
					
					final Vector<String> declareReferenceDeviceID = new Vector<String>();
					declareReferenceDeviceID.add("return [@refDev]");
					codes[1] = declareReferenceDeviceID;
					
					final Vector<String> getDeviceCompatibleDesc = new Vector<String>();
					appendDeviceCompatibleDesc(getDeviceCompatibleDesc);
					codes[2] = getDeviceCompatibleDesc;
					
					final Vector<String> conn = new Vector<String>();
					conn.add("#this method will be invoked by server to startup this robot before EVENT_SYS_PROJ_STARTUP");
					conn.add("puts \"Robot startup successful!\"");
					codes[4] = conn;
					final Vector<String> disConn = new Vector<String>();
					disConn.add("#this method will be invoked by server to shutdown this robot after EVENT_SYS_PROJ_SHUTDOWN");
					disConn.add("puts \"Robot shutdown successful!\"");
					codes[5] = disConn;
				}
				final boolean[] isEmpty = {false, false, false, false, false, false};
				final String[] superCodes = {SUPER, "@refDev = \"DemoRefDevID\""};
				return buildScript(imports, instanceName, superClassName, superCodes, isEmpty, methods, codes);
			}else if(url.url.indexOf(HCURL.DATA_IOT_CONVERTER.toLowerCase()) >= 0){
				final String[] imports = {
//						"# for Converter API, http://homecenter.mobi/download/javadoc/hc/server/msb/Converter.html\n\n"
//						,"# for demo, http://homecenter.mobi/en/pc/steps_iot.htm\n\n"
						"import Java::hc.server.msb.Message\n\n"
						};
				final String instanceName = "MyConverter";
				final String superClassName = Converter.class.getName();
				final String[] methods = {"upConvert(fromDevice, toRobot)", "downConvert(fromRobot, toDevice)", 
						"getUpDeviceCompatibleDescription", "getDownDeviceCompatibleDescription"};
				final Vector<String>[] codes = new Vector[methods.length];
				{
					final Vector<String> getUpDeviceCompatibleDesc = new Vector<String>();
					getUpDeviceCompatibleDesc.add("#the compatible description to upside(Robot).");
					appendDeviceCompatibleDesc(getUpDeviceCompatibleDesc);
					codes[2] = getUpDeviceCompatibleDesc;
					final Vector<String> getDownDeviceCompatibleDesc = new Vector<String>();
					getDownDeviceCompatibleDesc.add("#the compatible description to downside(Device).");
					appendDeviceCompatibleDesc(getDownDeviceCompatibleDesc);
					codes[3] = getDownDeviceCompatibleDesc;
				}
				final boolean[] isEmpty = {false, false, false, false};
				final String[] superCodes = {SUPER};
				return buildScript(imports, instanceName, superClassName, superCodes, isEmpty, methods, codes);
			}else if(url.url.indexOf(HCURL.DATA_IOT_DEVICE.toLowerCase()) >= 0){
				final String[] imports = {
//						"# for Device API , http://homecenter.mobi/download/javadoc/hc/server/msb/Device.html\n\n"
//						,"# for demo, http://homecenter.mobi/en/pc/steps_iot.htm\n\n"
						"import Java::hc.server.msb.Message\n\n"
						};
				final String instanceName = "MyDevice";
				final String superClassName = Device.class.getName();
				final String[] methods = {"response(msg)", "connect", "disconnect", "getDeviceCompatibleDescription"	};//"notifyNewWiFiAccount(newAccount)"
				final Vector<String>[] codes = new Vector[methods.length];
				{
					final Vector<String> conn = new Vector<String>();
					
//					conn.add("#----if you device need set WiFi account, please remove comments of the following codes.----");
//					conn.add("#ctx = getProjectContext()");
//					conn.add("#isWiFiSetted = \"isWiFiSettedOf\" + getName()#name is required, because there may be multiple types of device in same HAR");
//					conn.add("#if ctx.getProperty(isWiFiSetted, \"false\").equals(\"false\")");
//					conn.add("#\taccount = getWiFiAccount()");
//					conn.add("#\tif account == nil");
//					conn.add("#\t\t#if there is no WiFi module on server (but there is a WiFi router), ");
//					conn.add("#\t\t#please connect to server from mobile via WiFi first, ");
//					conn.add("#\t\t#server will broadcast WiFi account via your mobile.");
//					conn.add("#\t\t#you can also add HAR project from mobile by QRcode.");
//					conn.add("#\t\tctx.tipOnTray(\"No WiFi account or WiFi module to broadcast\")");
//					conn.add("#\treturn");
//					conn.add("#\tend");
//					conn.add("#");
//					conn.add("#\tcommands = \"ssid:\" + account.getSSID() + \";pwd:\" + account.getPassword() + \";security:\" + account.getSecurityOption()");
//					conn.add("#\t----you must encrypt the WiFi account here before broadcasting to air, here is just for demo----");
//					conn.add("#\tbroadcastWiFiAccountAsSSID(\"AIRCOND\", commands)");
//					conn.add("#\t----scan WiFi now, you will find following SSIDs on air, decrypt them in your real device for first connection.");
//					conn.add("#\t----AIRCOND56#1#ssid:abc#efg;pwd:123");
//					conn.add("#\t----AIRCOND56#2#4567890;security:WPA");
//					conn.add("#\t----AIRCOND56#3#2_PSK");
//					conn.add("#\t----Note : the length of an SSID should be a maximum of 32, so it is split to three parts.");
//					conn.add("#");
//					conn.add("#\t----start initial task and wait for device connecting");
//					conn.add("#\t----...");
//					conn.add("#\t----successful receive device connection");
//					conn.add("#\tctx.setProperty(isWiFiSetted, \"true\")");
//					conn.add("#\tctx.saveProperties()");
//					conn.add("#else");
//					conn.add("#\t----Note : WiFi account may be changed when server is shutdown.");
//					conn.add("#\t----start initial task and wait for device connecting");
//					conn.add("#end");
//					
//					conn.add("");//空行，不需\n
					
					conn.add("log(\"Device [\"+@refDev+\"] connect successful!\")");
					conn.add("return [@refDev]");
					codes[1] = conn;
					
					final Vector<String> disConn = new Vector<String>();
					disConn.add("log(\"Device [\"+@refDev+\"] disconnect successful!\")");
					codes[2] = disConn;

					final Vector<String> getCompatibleDesc = new Vector<String>();
					appendDeviceCompatibleDesc(getCompatibleDesc);
					
					codes[3] = getCompatibleDesc;
					
//					final Vector<String> newWiFiAccount = new Vector<String>();
//					codes[4] = newWiFiAccount;
				}
				final boolean[] isEmptyMethod = {false, false, false, false};//true
				final String[] superCodes = {SUPER, "@refDev = \"DemoDevID\""};
				return buildScript(imports, instanceName, superClassName, superCodes, isEmptyMethod, methods, codes);
			}
		}
		return "";
	}

	public static void appendDeviceCompatibleDesc(
			final Vector<String> getCompatibleDesc) {
		getCompatibleDesc.add("return Class.new(Java::hc.server.msb.DeviceCompatibleDescription) {");
		getCompatibleDesc.add("\t# override");
		getCompatibleDesc.add("\tdef getVersion");
		getCompatibleDesc.add("\t\treturn \"1.0\"");
		getCompatibleDesc.add("\tend");
		getCompatibleDesc.add("\t# override");
		getCompatibleDesc.add("\tdef getDescription");
		getCompatibleDesc.add("\t\treturn \"\"");
		getCompatibleDesc.add("\tend");
		getCompatibleDesc.add("\t# override");
		getCompatibleDesc.add("\tdef getCompatibleStringList");
		getCompatibleDesc.add("\t\treturn \"\"");
		getCompatibleDesc.add("\tend");
		getCompatibleDesc.add("}.new");
	}
	
	private static String buildMyCommand(){
		final StringBuffer sb = new StringBuffer();
		
		sb.append("#encoding:utf-8\n\n");
		
		return sb.toString();
	}

	/**
	 * 
	 * @param imports 
	 * @param instanceName
	 * @param superClassName
	 * @param superCodes 如"super", "super('para1')"
	 * @param isEmptyOrAbstract
	 * @param methods
	 * @return
	 */
	private static String buildScript(final String[] imports,
			final String instanceName, final String superClassName, final String[] superCodes, final boolean[] isEmptyOrAbstract,
			final String[] methods, final Vector<String>[] codeExamples) {
		final StringBuffer sb = new StringBuffer();
		sb.append("#encoding:utf-8\n\n");
		sb.append("#more JRuby, http://github.com/jruby/jruby/wiki\n\n");
		if(imports != null){
			for (int i = 0; i < imports.length; i++) {
				sb.append(imports[i]);
			}
		}
		sb.append("class " + instanceName + " < Java::" + superClassName + "\n");
		
		sb.append("\tdef initialize\n");
		if(superCodes != null && superCodes.length > 0){
			for (int i = 0; i < superCodes.length; i++) {
				final String superCode = superCodes[i];
				if(superCode != null && superCode.length() > 0){
					sb.append("\t\t" + superCode + "\n");
				}
			}
		}
		sb.append("\t\t#init constructor code here\n");
		sb.append("\tend\n\n");
		
		for (int i = 0; i < methods.length; i++) {
			sb.append("\t#override " + (isEmptyOrAbstract[i]?"empty":"abstract") + " method " + methods[i] + "\n");
			sb.append("\tdef " + methods[i] + "\n");
			if(codeExamples != null && codeExamples[i] != null){
				final Vector<String> codes = codeExamples[i];
				final int size = codes.size();
				for (int j = 0; j < size; j++) {
					sb.append("\t\t" + codes.elementAt(j) + "\n");
				}
			}
			sb.append("\tend\n\n");
		}

		sb.append("end\n\n");
		
		sb.append("return " + instanceName + ".new");
		
		return sb.toString();
	}
	
	private static Vector<String> buildOnLoad(){
		final Vector<String> codes = new Vector<String>();
		codes.add("#uncomment follow codes to send status key-value to mobile");
		codes.add("#sendStatus \"key1\", \"value1\"");
		codes.add("#sendStatus \"key2\", \"value2\", true#true:isRightToLeft");
		codes.add("#sendStatus [\"k1\",\"k2\"].to_java(:string), [\"v1\",\"v2\"].to_java(:string)");
		codes.add("#sendStatus [\"k1\",\"k2\"].to_java(:string), [\"v1\",\"v2\"].to_java(:string), true#true:isRightToLeft");
//		codes.add("");
		return codes;
	}
	
	private static Vector<String> buildInitText(){
		final Vector<String> codes = new Vector<String>();

//		codes.add("#----------------------------------------------");
//		codes.add("#uncomment follow code to show a tip on mobile");
//		codes.add("#showTip \"press #{keyValue}\"");
//		codes.add("");
//		codes.add("#uncomment follow code to change text of some button, if the button is visiable");
//		codes.add("#setButtonText CtrlKey::KEY_STANDBY, \"Power On\"");
		
		buildAllKeyCase(codes);
		codes.add("return \"\"");
		return codes;
	}
	
	private static Vector<String> buildAllKeyExample(){
		final Vector<String> codes = new Vector<String>();

		codes.add("#----------------------------------------------");
		codes.add("#uncomment follow code to show a tip on mobile");
		codes.add("#showTip \"press #{keyValue}\"");
		codes.add("");
		codes.add("#uncomment follow code to change text of some button, if the button is visiable");
		codes.add("#setButtonText CtrlKey::KEY_STANDBY, \"Power On\"");
		
		buildAllKeyCase(codes);
		return codes;
	}

	private static void buildAllKeyCase(final Vector<String> codes) {
		final CNCtrlKey ctrlKey = new CNCtrlKey();
		final int[] keyValues = ctrlKey.getDispKeys();
		final CtrlResponse instance = new CtrlResponse() {
			@Override
			public void click(final int keyValue) {
			}
		};
			
		final Field[] fs = CtrlKey.class.getDeclaredFields();
		for (int j = 0; j < fs.length; j++) {
			final Field f = fs[j];
			try{
				final String keyJavaStaticProp = f.getName();
				if (keyJavaStaticProp.startsWith("KEY_", 0) 
						&& java.lang.reflect.Modifier.isStatic(f.getModifiers())){
					final int propStaticValue = f.getInt(instance);
					for (int i = 0; i < keyValues.length; i++) {
						final int keyValue = keyValues[i];
						if(propStaticValue == keyValue) {
							if(i == 0){
								codes.add("if keyValue == CtrlKey::" + keyJavaStaticProp);
							}else{
								codes.add("elsif keyValue == CtrlKey::" + keyJavaStaticProp);
							}
							break;
						}
					}
				}
			}catch (final Exception e) {
			}
		}
		codes.add("else");
		codes.add("end");
	}
}
