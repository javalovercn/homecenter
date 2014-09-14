package hc.core.data;

import java.io.UnsupportedEncodingException;

import hc.core.IConstant;
import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

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
	private final static int text_len_index = ic_index + 2;
	public final static int text_index = text_len_index + 2;
	
	//因为是char，所以为512，而不是1024
	public static final int MAX_MOBI_UI_TXT_LEN = MsgBuilder.UDP_BYTE_SIZE - text_index;

	public int getLength() {
		return text_index - type_index + getTextDataLen();
	}
	
	public void setBytes(byte[] vbs){
		super.setBytes(vbs);
	}
	
	public void resetText(){
		ByteUtil.integerToTwoBytes(0, bs, text_len_index);
	}

	public void copyTextDataOut(byte[] target, int offset){
		int len = getTextDataLen();
		System.arraycopy(bs, text_index, target, offset, len);
	}
	
	public String getTextDataAsString(){
		try {
			return new String(bs, DataInputEvent.text_index, getTextDataLen(), IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
		return "";
	}
	
	public void setTextData(byte[] pd, int offset, int len){
		if(len > MAX_MOBI_UI_TXT_LEN){
			len = MAX_MOBI_UI_TXT_LEN;
		}
		ByteUtil.integerToTwoBytes(len, bs, text_len_index);
		System.arraycopy(pd, offset, bs, text_index, len);
	}
	
	public int getTextDataLen() {
		return ByteUtil.twoBytesToInteger(bs, text_len_index);
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
