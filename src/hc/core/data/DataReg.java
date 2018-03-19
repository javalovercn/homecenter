package hc.core.data;

import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

public class DataReg extends HCData {
	private final static int isFROMServer_index = MsgBuilder.INDEX_MSG_DATA;
	private final static int tokenLen_index = isFROMServer_index + 1;
	public final static int token_index = tokenLen_index + 1;// 60
	private final static int uuidLen_index = token_index + 60;
	public final static int uuid_index = uuidLen_index + 1;// 120

	public final static int LEN_DATA_REG = 2 + 1 + 60 + 120;

	public int getLength() {
		return LEN_DATA_REG;
	}

	public void setFromServer(byte b) {
		bs[isFROMServer_index] = b;
	}

	public byte getFromServer() {
		return bs[isFROMServer_index];
	}

	private void setUUIDLen(int len) {
		ByteUtil.integerToOneByte(len, bs, uuidLen_index);
	}

	public int getUUIDLen() {
		return ByteUtil.oneByteToInteger(bs, uuidLen_index);
	}

	private void setTokenLen(int len) {
		ByteUtil.integerToOneByte(len, bs, tokenLen_index);
	}

	public int getTokenLen() {
		return ByteUtil.oneByteToInteger(bs, tokenLen_index);
	}

	public void setUUIDDataIn(byte[] pd, int offset, int len) {
		setUUIDLen(len);
		System.arraycopy(pd, offset, bs, uuid_index, len);
	}

	public int copyUUIDDataOut(byte[] target, int offset) {
		int len = getUUIDLen();
		System.arraycopy(bs, uuid_index, target, offset, len);
		return len;
	}

	public void setTokenDataIn(byte[] pd, int offset, int len) {
		setTokenLen(len);
		System.arraycopy(pd, offset, bs, token_index, len);
	}

	public String getTokenDataOut() {
		int len = getTokenLen();
		return new String(bs, token_index, len);
	}

}
