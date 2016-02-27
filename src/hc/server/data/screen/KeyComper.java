package hc.server.data.screen;

import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.server.PlatformManager;
import hc.server.PlatformService;
import hc.server.ui.ServerUIAPIAgent;
import hc.util.ResourceUtil;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Vector;

public class KeyComper {
	public static final String ANDROID_KEYEVENT_PREFIX = "KEYCODE_";
	public static final String J2SE_KEYEVENT_PREFIX = "VK_";
	private static final Robot robot = buildDefaultRobot();
	
	private static Robot buildDefaultRobot(){
		try {
			return new Robot();
		} catch (final AWTException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private final HashMap<Integer, Vector<Integer>> keys = new HashMap<Integer, Vector<Integer>>(6);
	private final HashMap<Integer, Vector<String>> descs = new HashMap<Integer, Vector<String>>(6);
	private final HashMap<Integer, String> imagepath = new HashMap<Integer, String>(6);
	
	public KeyComper() {
		
	}
	
	public int size(){
		return keys.size();
	}
	
	public String getImagePath(final int idx){
		return imagepath.get(idx);
	}
	
	public Vector<String> getKeysDesc(final int idx){
		return descs.get(idx);
	}
	
	/**
	 * 
	 * @param idx
	 * @param keysDesc 输出组合键的描述
	 * @return
	 */
	public Vector<Integer> getKeys(final int idx, final String[] keysDesc){
		if(keysDesc != null && keysDesc.length > 0){
			String out = "";
			final Vector<String> keys = getKeysDesc(idx);
			for (int i = 0; i < keys.size(); i++) {
				if(out.length() > 0){
					out += "+";
				}
				out += keys.elementAt(i);
			}
			keysDesc[0] = out;
		}
		return keys.get(idx);
	}
	
	public boolean addMap(final Vector<String> inputs, final String imagesPath){
		final int idx = 1 + size();
		
		final Vector<Integer> ke = convert(inputs);
		if(ke == null){
			return false;
		}
		keys.put(idx, ke);
		descs.put(idx, inputs);
		imagepath.put(idx, imagesPath);

		return true;
	}

	/**
	 * 
	 * @param msg
	 * @param split 如果msg中的数据部分可能含有split，如"ctrl+\+"，则如上表示。
	 * @param initSize
	 * @return 如果没有找到匹配字符，则返回长度为0
	 */
	public static Vector splitByChar(final String msg, final char split, final int initSize){
		final Vector v = new Vector(initSize);
		final char[] char_imgs = msg.toCharArray();
		int offset = 0;
		final int endIdx = char_imgs.length - 1;
		for (int j = 0; j < char_imgs.length; j++) {
			if(j == 0 && char_imgs[j] == split){
				return v;
			}
			if(char_imgs[j] == split){
				if(j == endIdx){
					v.removeAllElements();
					return v;
				}
				
				if(char_imgs[j - 1] == '\\'){
					continue;
				}else{
					v.addElement(String.valueOf(char_imgs, offset, j - offset).replaceAll("\\\\", "").trim());//去掉+间可能的空格
					offset = j + 1;
				}
			}
		}
		
		if(offset != char_imgs.length){
			v.addElement(String.valueOf(char_imgs, offset, char_imgs.length - offset).replaceAll("\\\\", "").trim());//去掉+间可能的空格
		}
		
		return v;
	}

	/**
	 * 产生键盘事件Ctrl+Alt+A
	 * @param keyDesc Ctrl+Alt+A
	 */
	public static void actionKeys(final String keyDesc) {
//		CCoreUtil.checkAccess();
		keyAction(robot, convert(convertStr(keyDesc)), keyDesc);
	}

	/**
	 * 
	 * @param robot2
	 * @param vInt
	 * @param keysDesc 描述如：Shift+A
	 */
	public static void keyAction(final Robot robot2, final Vector<Integer> vInt, final String keysDesc) {
		synchronized (robot2) {
			if(isAndroidServer){
				PlatformManager.getService().doExtBiz(PlatformService.BIZ_BIND_FORCE_ANDROID_KEYCODE, Boolean.valueOf(isAndroidServer));
			}

			for (int i = 0; i < vInt.size(); i++) {
				robot2.keyPress(vInt.elementAt(i));
			}
			
			for (int j = vInt.size() - 1; j >= 0; j--) {
				robot2.keyRelease(vInt.elementAt(j));
			}

			if(isAndroidServer){
				PlatformManager.getService().doExtBiz(PlatformService.BIZ_BIND_FORCE_ANDROID_KEYCODE, Boolean.FALSE);
			}
		}
		
//		String desc = KeyComperPanel.getHCKeyText(vInt.elementAt(0));
//		for (int i = 1; i < vInt.size(); i++) {
//			desc += "+" + KeyComperPanel.getHCKeyText(vInt.elementAt(i));
//		}
		ServerUIAPIAgent.__sendStaticMovingMsg("keys : " + keysDesc);
		L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "action keys : " + keysDesc);
	}

	/**
	 * Ctrl+Alt+A转换为串Ctrl, Alt, A
	 * @param k
	 * @return
	 */
	public static Vector convertStr(final String k) {
		return KeyComper.splitByChar(k, '+', 2);
	}

	/**
	 * 如果转换失败，则返回null
	 * @param inputs
	 * @return
	 */
	public static Vector<Integer> convert(final Vector<String> inputs) {
		final int size = inputs.size();
		final Vector<Integer> ke = new Vector<Integer>(size);
		
		for (int i = 0; i < size; i++) {
			final int keyEvent = getKeyEvent(inputs.elementAt(i));
			if(keyEvent == -1){
				return null;
			}
			ke.add(i, keyEvent);
		}
		return ke;
	}
	
	final static boolean isAndroidServer = ResourceUtil.isAndroidServerPlatform();
	
	private static int getKeyEvent(final String c) {
	    Field f;
		final String upperCase = c.toUpperCase();
		boolean isError = false;
		try {
			if(isAndroidServer && (upperCase.startsWith(ANDROID_KEYEVENT_PREFIX))){
				final int keycode = getAndroidKeyCode(upperCase);
				if(keycode == -1){
					isError = true;
				}
				return keycode;
			}
			
			final boolean startWithJ2SE = upperCase.startsWith(J2SE_KEYEVENT_PREFIX);
			f = KeyEvent.class.getField(startWithJ2SE?upperCase:(J2SE_KEYEVENT_PREFIX+upperCase));
		    f.setAccessible(true);
		    int keyCode = (Integer) f.get(null);
		    if(keyCode != -1 && isAndroidServer && (startWithJ2SE==false)){
			    final int androidKeyCode = (Integer)PlatformManager.getService().doExtBiz(PlatformService.BIZ_CONVERT_J2SE_KE_TO_ANDROID_KEY, keyCode);
			    if(androidKeyCode == -1){
			    	isError = true;
			    }else{
			    	keyCode = androidKeyCode;
			    }
		    }
		    return keyCode;
		} catch (final Exception e) {
			if(isAndroidServer){
				final int keycode = getAndroidKeyCode(upperCase);
				if(keycode == -1){
					isError = true;
				}
				return keycode;
			}else{
				isError = true;
			}
		}finally{
			if(isError){
				LogManager.err("Unknow Key:" + c);
			}
		}
		return -1;
	}

	private static int getAndroidKeyCode(final String upperCase) {
		return (Integer)PlatformManager.getService().doExtBiz(PlatformService.BIZ_GET_ANDROID_KEYCODE, upperCase);
	}
}
