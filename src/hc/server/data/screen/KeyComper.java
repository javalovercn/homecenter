package hc.server.data.screen;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.data.KeyComperPanel;
import hc.server.ui.ProjectContext;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Vector;

public class KeyComper {
	private HashMap<Integer, Vector<Integer>> keys = new HashMap<Integer, Vector<Integer>>(6);
	private HashMap<Integer, Vector<String>> descs = new HashMap<Integer, Vector<String>>(6);
	private HashMap<Integer, String> imagepath = new HashMap<Integer, String>(6);
	
	public KeyComper() {
		
	}
	
	public int size(){
		return keys.size();
	}
	
	public String getImagePath(int idx){
		return imagepath.get(idx);
	}
	
	public Vector<String> getKeysDesc(int idx){
		return descs.get(idx);
	}
	
	public Vector<Integer> getKeys(int idx){
		return keys.get(idx);
	}
	
	public boolean addMap(Vector<String> inputs, String imagesPath){
		int idx = 1 + size();
		
		Vector<Integer> ke = convert(inputs);
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
	public static Vector splitByChar(String msg, char split, int initSize){
		Vector v = new Vector(initSize);
		char[] char_imgs = msg.toCharArray();
		int offset = 0;
		int endIdx = char_imgs.length - 1;
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
					v.addElement(String.valueOf(char_imgs, offset, j - offset).replaceAll("\\\\", ""));
					offset = j + 1;
				}
			}
		}
		
		if(offset != char_imgs.length){
			v.addElement(String.valueOf(char_imgs, offset, char_imgs.length - offset).replaceAll("\\\\", ""));
		}
		
		return v;
	}

	/**
	 * 产生键盘事件Ctrl+Alt+A
	 * @param keyDesc Ctrl+Alt+A
	 */
	public static void actionKeys(String keyDesc) {
		try {
			keyAction(new Robot(), convert(convertStr(keyDesc)));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public static void keyAction(Robot robot2, Vector<Integer> vInt) {
		for (int i = 0; i < vInt.size(); i++) {
			robot2.keyPress(vInt.elementAt(i));
		}
		
		for (int j = vInt.size() - 1; j >= 0; j--) {
			robot2.keyRelease(vInt.elementAt(j));
		}
		
		String desc = KeyComperPanel.getHCKeyText(vInt.elementAt(0));
		for (int i = 1; i < vInt.size(); i++) {
			desc += "+" + KeyComperPanel.getHCKeyText(vInt.elementAt(i));
		}
		ProjectContext.sendStaticMovingMsg("keys : " + desc);
		L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "action keys : " + desc);
	}

	/**
	 * Ctrl+Alt+A转换为串Ctrl, Alt, A
	 * @param k
	 * @return
	 */
	public static Vector convertStr(String k) {
		return KeyComper.splitByChar(k, '+', 2);
	}

	/**
	 * 如果转换失败，则返回null
	 * @param inputs
	 * @return
	 */
	public static Vector<Integer> convert(Vector<String> inputs) {
		int size = inputs.size();
		Vector<Integer> ke = new Vector<Integer>(size);
		
		for (int i = 0; i < size; i++) {
			int keyEvent = getKeyEvent(inputs.elementAt(i));
			if(keyEvent == -1){
				return null;
			}
			ke.add(i, keyEvent);
		}
		return ke;
	}
	
	private static int getKeyEvent(String c) {
	    Field f;
		try {
			f = KeyEvent.class.getField("VK_" + c.toUpperCase());
		    f.setAccessible(true);
		    return (Integer) f.get(null);
		} catch (Exception e) {
			LogManager.err("Unknow Key:" + c);
		}
		return -1;
	}
	
}
