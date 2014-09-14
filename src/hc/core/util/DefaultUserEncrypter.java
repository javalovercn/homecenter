package hc.core.util;

import java.io.UnsupportedEncodingException;

import hc.core.IConstant;

public class DefaultUserEncrypter implements IEncrypter {
	final String encryptData = (String)IConstant.getInstance().getObject("encryptData");
	final byte[] encryptBS = toByteArr(encryptData);
	final int encryptLen = encryptBS.length;
	
	private static byte[] toByteArr(String encryptData){
		try {
			if(encryptData != null){
				return encryptData.getBytes("UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890zyxwvutsrqponmlkjihgfedcba".getBytes();
	}
	public void decryptCertKey(byte[] data, int offset, int len) {
		int modeK = 0;
		final int endIdx = offset + len;
		for (int i = offset; i < endIdx; i++) {
			data[i] -= encryptBS[modeK++];
			if(modeK == encryptLen){
				modeK = 0;
			}
		}
	}

	public void encryptCertKey(byte[] data, int offset, int len) {
		int modeK = 0;
		final int endIdx = offset + len;
		for (int i = offset; i < endIdx; i++) {
			data[i] += encryptBS[modeK++];
			if(modeK == encryptLen){
				modeK = 0;
			}
		}
	}

	public void decryptData(byte[] data, int offset, int len) {
		int modeK = 0;
		final int endIdx = offset + len;
		for (int i = offset; i < endIdx; i++) {
			data[i] -= encryptBS[modeK++];
			if(modeK == encryptLen){
				modeK = 0;
			}
		}
	}

	public void encryptData(byte[] data, int offset, int len) {
		int modeK = 0;
		final int endIdx = offset + len;
		for (int i = offset; i < endIdx; i++) {
			data[i] += encryptBS[modeK++];
			if(modeK == encryptLen){
				modeK = 0;
			}
		}
	}

	public void initEncrypter(boolean isMobileSide) {
	}

	public void setPassword(byte[] password) {
		int modeK = 0;
		for (int i = 0; i < encryptBS.length; i++) {
			encryptBS[i] = (byte)(encryptBS[i] ^ password[modeK++]);
			if(modeK == password.length){
				modeK = 0;
			}
		}
	}

	public void setUUID(byte[] uuid) {
		int modeK = 0;
		for (int i = 0; i < encryptBS.length; i++) {
			encryptBS[i] = (byte)(encryptBS[i] ^ uuid[modeK++]);
			if(modeK == uuid.length){
				modeK = 0;
			}
		}
	}
	
	public void notifyExit(boolean isMobileSide) {
	}
}
