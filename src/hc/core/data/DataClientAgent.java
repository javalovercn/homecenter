package hc.core.data;

import java.io.UnsupportedEncodingException;

import hc.core.IConstant;
import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

public class DataClientAgent extends HCData {
	private final int width_index = MsgBuilder.INDEX_MSG_DATA;
	private final int height_index = width_index + 2;
	private final int isDPI_index = height_index + 2;
	private final int lenClientLang_index = isDPI_index + 2;
	private final int ClientLang_index = lenClientLang_index + 1;//最多10
	private final int lenVer_index = ClientLang_index + 10;
	private final int ver_index = lenVer_index + 1;//最多10

	public static final int STRUCT_LEN = 7 + 10 + 11;//等于以上各位之和

	public void setVer(String ver){
		byte[] lang_bs = null;
		try {
			lang_bs = ver.getBytes(IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			lang_bs = ver.getBytes();
		}
		setVerLen(lang_bs.length);
		for (int i = 0, j = ver_index; i < lang_bs.length; i++, j++) {
			bs[j] = lang_bs[i];
		}
	}
	
	public String getVer(){
		int len = getVerLen();
		try {
			return new String(bs, ver_index, len, IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			return new String(bs, ver_index, len);
		}
	}

	private void setVerLen(int len) {
		ByteUtil.integerToOneByte(len, bs, lenVer_index);
	}
	
	private int getVerLen() {
		return ByteUtil.oneByteToInteger(bs, lenVer_index);
	}

	public void setClientLang(String lang){
		byte[] lang_bs = null;
		try {
			lang_bs = lang.getBytes(IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			lang_bs = lang.getBytes();
		}
		setClientLangLen(lang_bs.length);
		for (int i = 0, j = ClientLang_index; i < lang_bs.length; i++, j++) {
			bs[j] = lang_bs[i];
		}
	}
	
	public String getClientLang(){
		int len = getClientLangLen();
		try {
			return new String(bs, ClientLang_index, len, IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			return new String(bs, ClientLang_index, len);
		}
	}
	
	private int getClientLangLen() {
		return ByteUtil.oneByteToInteger(bs, lenClientLang_index);
	}

	private void setClientLangLen(int len) {
		ByteUtil.integerToOneByte(len, bs, lenClientLang_index);
	}

	public int getWidth() {
		return ByteUtil.twoBytesToInteger(bs, width_index);
	}

	public void setWidth(int width) {
		ByteUtil.integerToTwoBytes(width, bs, width_index);
	}

	public int getHeight() {
		return ByteUtil.twoBytesToInteger(bs, height_index);
	}

	public void setHeight(int height) {
		ByteUtil.integerToTwoBytes(height, bs, height_index);
	}

	public int getDPI() {
		return ByteUtil.twoBytesToInteger(bs, isDPI_index);
	}

	public void setDPI(int dpi) {
		ByteUtil.integerToTwoBytes(dpi, bs, isDPI_index);
	}

	public int getLength() {
		return STRUCT_LEN;
	}

}
