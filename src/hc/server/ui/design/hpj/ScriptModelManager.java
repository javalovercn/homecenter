package hc.server.ui.design.hpj;

import java.lang.reflect.Field;
import java.util.Vector;

import hc.core.util.CNCtrlKey;
import hc.core.util.CtrlKey;
import hc.core.util.HCURL;
import hc.server.ui.CtrlResponse;
import hc.server.ui.Mlet;

public class ScriptModelManager {
	public static String buildDefaultScript(final int type, final HCURL url){
		if(type == HPNode.TYPE_MENU_ITEM_CONTROLLER){
			String[] imports = {"import Java::hc.core.util.CtrlKey\n\n"};
			String instanceName = "MyController";
			final String superClassName = CtrlResponse.class.getName();
			String[] methods = {"click(keyValue)", "getButtonInitText(keyValue)", "onLoad", "onExit"};
			Vector<String>[] codes = (Vector<String>[])new Vector[4];
			codes[0] = buildAllKeyExample();
			codes[1] = buildInitText();
			codes[2] = buildOnLoad();
			boolean[] isEmpty = {false, true, true, true};			
			return buildScript(imports, instanceName, superClassName, null, isEmpty, methods, codes);
		}else if(type == HPNode.TYPE_MENU_ITEM_SCREEN){
			if(url.elementID.equals(HCURL.REMOTE_HOME_SCREEN)){
			}else{
				String instanceName = "MyMlet";
				final String superClassName = Mlet.class.getName();
				String[] methods = {"onStart", "onPause", "onResume", "onExit"};
				boolean[] isEmpty = {true, true, true, true};
				return buildScript(null, instanceName, superClassName, "super", isEmpty, methods, null);
			}
		}else if(type == HPNode.TYPE_MENU_ITEM_CMD){
			if(url.elementID.equals(HCURL.DATA_CMD_EXIT) 
					|| url.elementID.equals(HCURL.DATA_CMD_CONFIG)){
			}else{
				//my-command
				return buildMyCommand();
			}
		}
		return "";
	}
	
	private static String buildMyCommand(){
		StringBuffer sb = new StringBuffer();
		
		sb.append("#encoding:utf-8\n\n");
		
		sb.append("require 'java'\n\n");
		
		final String str_line = "#----------------------------------------------\n";
		
		sb.append("# for the new API , see http://homecenter.mobi/download/javadoc/hc/server/ui/ProjectContext.html\n\n");
		
		sb.append("#HOW TO execute system command\n");
		sb.append(str_line);
		sb.append("#uncomment follow three to execute cmd\n");
		sb.append("#import java.lang.Runtime\n");
		sb.append("#rt = Runtime.getRuntime\n");
		sb.append("#rt.exec \"C://Program Files/Windows Media Player/wmplayer.exe\"\n\n");
		
		sb.append("#HOW TO show tip on system tray and log in HomeCenter server\n");
		sb.append(str_line);
		sb.append("#uncomment the following three codes to show message on tray, and log\n");
		sb.append("#server_class = Java::hc.server.ui.ProjectContext.getProjectContext\n");
		sb.append("#server_class.tipOnTray \"Hello, system tray!\"\n");
		sb.append("#server_class.log \"This is a test log\"\n\n");
		
		sb.append("#HOW TO action keyboard events\n");
		sb.append("#for example, Control+Shift+Escape, Control : KeyEvent.VK_CONTROL, Shift : KeyEvent.VK_SHIFT, Escape : KeyEvent.VK_ESCAPE, Meta (Mac OS X, command) : KeyEvent.VK_META)\n");
		sb.append("#more key string , please refer http://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html\n");
		sb.append("#NOTE : NOT all keys are supported\n");
		sb.append(str_line);
		sb.append("#uncomment the following two codes to actionKeys Control+Shift+Escape\n");
		sb.append("#server_class = Java::hc.server.ui.ProjectContext.getProjectContext\n");
		sb.append("#server_class.actionKeys \"Control+Shift+Escape\"\n\n");
		
		sb.append("#HOW TO display message moving from right to left on mobile\n");
		sb.append(str_line);
		sb.append("#uncomment the following codes to display moving message on mobile.\n");
		sb.append("#server_class = Java::hc.server.ui.ProjectContext.getProjectContext\n");
		sb.append("#server_class.sendMovingMsg \"I am moving...\"\n\n");
		
		sb.append("#HOW TO send alert dialog to mobile, send(String caption, String text, int type)\n");
		sb.append("# type = 1,	ERROR\n");
		sb.append("# type = 2,	WARN\n");
		sb.append("# type = 3, INFO\n");
		sb.append("# type = 4, ALARM\n");
		sb.append("# type = 5,	CONFIRMATION\n");
		sb.append(str_line);
		sb.append("#uncomment the follow codes to send message and log info.\n");
		sb.append("#server_class = Java::hc.server.ui.ProjectContext.getProjectContext\n");
		sb.append("#server_class.send \"CaptionText\", \"Hello, this is a message from server.\", 3\n\n");
		
		sb.append("#HOW TO send notification to mobile, sendNotification(String title, String body, int flags)\n");
		sb.append("# flags = FLAG_NOTIFICATION_SOUND, please disable mute option in mobile app config\n");
		sb.append("# flags = FLAG_NOTIFICATION_VIBRATE, please enable vibrate of Android\n");
		sb.append(str_line);
		sb.append("#uncomment the following codes to send notification to mobile.\n");
		sb.append("#server_class = Java::hc.server.ui.ProjectContext.getProjectContext\n");
		sb.append("#server_class.sendNotification \"Title\", \"this is a notification\", Java::hc.server.ui.ProjectContext::FLAG_NOTIFICATION_SOUND | Java::hc.server.ui.ProjectContext::FLAG_NOTIFICATION_VIBRATE\n\n");

		sb.append("#HOW TO play tone on mobile, please disable mute on mobile first\n");
		sb.append("#playTone(int note, int duration, int volume)\n");
		sb.append("#param note A note is given in the range of 0 to 127 inclusive, Defines the tone of the note as specified by the above formula.\n");
		sb.append("#param duration The duration of the tone in milli-seconds. Duration must be positive.\n");
		sb.append("#param volume Audio volume range from 0 to 100. 100 represents the maximum\n");
		sb.append(str_line);
		sb.append("#uncomment follow two codes to play a tone.\n");
		sb.append("#server_class = Java::hc.server.ui.ProjectContext.getProjectContext\n");
		sb.append("#server_class.playTone 100, 400, 75\n\n");
		
		sb.append("#HOW TO vibrate on mobile\n");
		sb.append("#vibrate(int duration)\n");
		sb.append("#duration - the number of milliseconds the vibrator should be run\n");
		sb.append(str_line);
		sb.append("#uncomment follow two codes to vibrate on mobile.\n");
		sb.append("#server_class = Java::hc.server.ui.ProjectContext.getProjectContext\n");
		sb.append("#server_class.vibrate 300\n\n");
		
//		sb.append(str_line);
//		sb.append("#uncomment follow code to use the file is locate in tree path /resources/Share JRuby File/sharefunc.rb\n");
//		sb.append("#require 'sharefunc.rb'\n\n");
		
		sb.append("#HOW TO use java classes in share jar\n");
		sb.append("#for example, testlib.jar is in tree path /Sample Project/resources/Share Jar Files/testlib.jar\n");
		sb.append("#class test.TestClass is in testlib.jar\n");
		sb.append(str_line);
		sb.append("#uncomment the following three codes to call method test.TestClass.minus(int a, int b), to a - b\n");
		sb.append("#require 'testlib.jar'\n");
		sb.append("#test = Java::test.TestClass\n");
		sb.append("#_minusResult = test.minus 100, 30\n\n");
		
		//sb.append("\n");

		
		return sb.toString();
	}

	/**
	 * 
	 * @param imports 
	 * @param instanceName
	 * @param superClassName
	 * @param superMethod 如"super", "super('para1')"
	 * @param isEmptyOrAbstract
	 * @param methods
	 * @return
	 */
	private static String buildScript(String[] imports,
			String instanceName, final String superClassName, String superMethod, boolean[] isEmptyOrAbstract,
			String[] methods, Vector<String>[] codeExamples) {
		StringBuffer sb = new StringBuffer();
		sb.append("#encoding:utf-8\n\n");
		
		sb.append("require 'java'\n\n");
		if(imports != null){
			for (int i = 0; i < imports.length; i++) {
				sb.append(imports[i]);
			}
		}
		sb.append("class " + instanceName + " < Java::" + superClassName + "\n");
		sb.append("\tdef initialize\n");
		if(superMethod != null && superMethod.length() > 0){
			sb.append("\t\t" + superMethod + "\n");
		}
		sb.append("\t\t#init constructor code here\n");
		sb.append("\tend\n\n");
		for (int i = 0; i < methods.length; i++) {
			sb.append("\t#override " + (isEmptyOrAbstract[i]?"empty":"abstract") + " method " + methods[i] + "\n");
			sb.append("\tdef " + methods[i] + "\n");
			if(codeExamples != null && codeExamples[i] != null){
				Vector<String> codes = codeExamples[i];
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
		Vector<String> codes = new Vector<String>();
		codes.add("#uncomment follow codes to send status key-value to mobile");
		codes.add("#sendStatus \"key1\", \"value1\"");
		codes.add("#sendStatus \"key2\", \"value2\", true#true:isRightToLeft");
		codes.add("#sendStatus [\"k1\",\"k2\"].to_java(:string), [\"v1\",\"v2\"].to_java(:string)");
		codes.add("#sendStatus [\"k1\",\"k2\"].to_java(:string), [\"v1\",\"v2\"].to_java(:string), true#true:isRightToLeft");
//		codes.add("");
		return codes;
	}
	
	private static Vector<String> buildInitText(){
		Vector<String> codes = new Vector<String>();

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
		Vector<String> codes = new Vector<String>();

		codes.add("#----------------------------------------------");
		codes.add("#uncomment follow code to show a tip on mobile");
		codes.add("#showTip \"press #{keyValue}\"");
		codes.add("");
		codes.add("#uncomment follow code to change text of some button, if the button is visiable");
		codes.add("#setButtonText CtrlKey::KEY_STANDBY, \"Power On\"");
		
		buildAllKeyCase(codes);
		return codes;
	}

	private static void buildAllKeyCase(Vector<String> codes) {
		CNCtrlKey ctrlKey = new CNCtrlKey();
		int[] keyValues = ctrlKey.getDispKeys();
		CtrlResponse instance = new CtrlResponse() {
			@Override
			public void click(int keyValue) {
			}
		};
			
		Field[] fs = CtrlKey.class.getDeclaredFields();
		for (int j = 0; j < fs.length; j++) {
			final Field f = fs[j];
			try{
				final String keyJavaStaticProp = f.getName();
				if (keyJavaStaticProp.startsWith("KEY_") 
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
			}catch (Exception e) {
			}
		}
		codes.add("else");
		codes.add("end");
	}
}
