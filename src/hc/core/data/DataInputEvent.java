package hc.core.data;

import java.io.UnsupportedEncodingException;

import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;

public class DataInputEvent extends HCData {
	public static final int TYPE_MOUSE_LEFT_CLICK = 0;
	public static final int TYPE_MOUSE_RIGHT_CLICK = 1;
	public static final int TYPE_MOUSE_MOVE = 2;
	public static final int TYPE_MOUSE_DOUBLE_CLICK = 3;
	public static final int TYPE_TRANS_TEXT = 4;
	public static final int TYPE_BACKSPACE = 5;
	public static final int TYPE_TAG_TRANS_TEXT_2_MOBI = 6;
	
	public static final int TYPE_TAG_KEY_PRESS_V_SCREEN = 7;
	public static final int TYPE_TAG_KEY_RELEASE_V_SCREEN = 8;
	public static final int TYPE_TAG_POINTER_PRESS_V_SCREEN = 9;
	public static final int TYPE_TAG_POINTER_DRAG_V_SCREEN = 10;
	public static final int TYPE_TAG_POINTER_RELEASE_V_SCREEN = 11;
	
	public static final int TYPE_TAG_INPUT_KEYBOARD_FROM_MOBI = 12;
	
	public static final int TYPE_START_USER_ICON = TYPE_TAG_TRANS_TEXT_2_MOBI;
	
	public final static int type_index = MsgBuilder.INDEX_MSG_DATA;
	private final static int x_index = type_index + 1;
	private final static int y_index = x_index + 2;
	private final static int ic_index = y_index + 2;//两位长
	private final static int screen_id_len_index = ic_index + 2;
	public final static int screen_id_index = screen_id_len_index + 2;
	
	public static final int MAX_SCREEN_ID_LEN = 100;
	//因为是char，所以为512，而不是1024
	public static final int MAX_MOBI_UI_TXT_LEN = MsgBuilder.UDP_BYTE_SIZE - screen_id_index - 2 - MAX_SCREEN_ID_LEN;

	public int getLength() {
		final int out = screen_id_index - type_index + getScreenIDLen() + 2 + getTextDataLen();
//		if(L.isInWorkshop){
//			LogManager.log("[DataInputEvent] getLength : " + out);
//		}
		return out;
	}
	
	public void setBytes(byte[] vbs){
		super.setBytes(vbs);
	}
	
	public final int getTextLenIdx(){
		final int out = screen_id_index + getScreenIDLen();
//		if(L.isInWorkshop){
//			LogManager.log("[DataInputEvent] getTextLenIdx : " + out);
//		}
		return out;
	}
	
	public final int getTextIdx(){
		final int out = getTextLenIdx() + 2;
//		if(L.isInWorkshop){
//			LogManager.log("[DataInputEvent] getTextIdx : " + out);
//		}
		return out;
	}
	
	public void resetText(){
		ByteUtil.integerToTwoBytes(0, bs, getTextLenIdx());
	}

	public int copyTextDataOut(byte[] target, int offset){
		int len = getTextDataLen();
		System.arraycopy(bs, getTextIdx(), target, offset, len);
		return len;
	}
	
	public final int copyScreenIDOut(byte[] target, int offset){
		int len = getScreenIDLen();
		System.arraycopy(bs, screen_id_index, target, offset, len);
		return len;
	}
	
	public String getTextDataAsString(){
		try {
			return new String(bs, getTextIdx(), getTextDataLen(), IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
		}		
		return "";
	}
	
	public String getScreenIDAsString(){
		try {
			return new String(bs, screen_id_index, getScreenIDLen(), IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
		}		
		return "";
	}
	
	public void setScreenID(byte[] pd, int offset, int len){
		if(len > MAX_SCREEN_ID_LEN){
			len = MAX_SCREEN_ID_LEN;
		}
		ByteUtil.integerToTwoBytes(len, bs, screen_id_len_index);
		System.arraycopy(pd, offset, bs, screen_id_index, len);
	}
	
	public void setTextData(byte[] pd, int offset, int len){
		if(len > MAX_MOBI_UI_TXT_LEN){
			len = MAX_MOBI_UI_TXT_LEN;
		}
		ByteUtil.integerToTwoBytes(len, bs, getTextLenIdx());
		System.arraycopy(pd, offset, bs, getTextIdx(), len);
	}
	
	public int getTextDataLen() {
		final int out = ByteUtil.twoBytesToInteger(bs, getTextLenIdx());
//		if(L.isInWorkshop){
//			LogManager.log("[DataInputEvent] getTextDataLen : " + out);
//		}
		return out;
	}
	
	public final int getScreenIDLen() {
		final int out = ByteUtil.twoBytesToInteger(bs, screen_id_len_index);
//		if(L.isInWorkshop){
//			LogManager.log("[DataInputEvent] getScreenIDLen : " + out);
//		}
		return out;
	}

	public int getType() {
		return ByteUtil.oneByteToInteger(bs, type_index);
	}

	public void setType(int type) {
		ByteUtil.integerToOneByte(type, bs, type_index);
	}
	
	public int getX() {
		return ByteUtil.twoBytesToInteger(bs, x_index);
	}

	public void setX(int x) {
		ByteUtil.integerToTwoBytes(x, bs, x_index);
	}

	public int getY() {
		return ByteUtil.twoBytesToInteger(bs, y_index);
	}

	public void setY(int y) {
		ByteUtil.integerToTwoBytes(y, bs, y_index);
	}

}
