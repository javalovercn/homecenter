package hc.core.data;

import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

public class DataCache extends HCData {
	private static final int proj_len_index = MsgBuilder.INDEX_MSG_DATA;
	public static final int LEN_STORE_LEN = 2;
	private static final int proj_data_index = proj_len_index + LEN_STORE_LEN;
	
	public final int setCacheInfo(final byte[] projID, final int projIdx, final int projLen,
			final byte[] urlID, final int urlIdx, final int urlLen,
			final byte[] codeID){
		final int codeIdx = 0;
		final int codeLen = codeID.length;
		final int dataLen = projLen + urlLen + codeLen + LEN_STORE_LEN * 3;
		final int maxLen = dataLen + MsgBuilder.INDEX_MSG_DATA;
		if(maxLen > bs.length){
			byte[] newbs = new byte[maxLen];
			System.arraycopy(bs, 0, newbs, 0, bs.length);
			bs = newbs;
		}
		
		ByteUtil.integerToTwoBytes(projLen, bs, proj_len_index);
		System.arraycopy(projID, projIdx, bs, proj_data_index, projLen);
		
		final int eleLenIndex = getEleLenIndex(projLen);
		final int eleIndex = getEleIndex(eleLenIndex);
		ByteUtil.integerToTwoBytes(urlLen, bs, eleLenIndex);
		System.arraycopy(urlID, urlIdx, bs, eleIndex, urlLen);
		
		final int codeLenIndex = getCodeLenIndex(eleIndex, urlLen);
		ByteUtil.integerToTwoBytes(codeLen, bs, codeLenIndex);
		System.arraycopy(codeID, codeIdx, bs, getCodeIndex(codeLenIndex), codeLen);
		
		return dataLen;
	}
	
	public int getProjDataIdx(){
		return proj_data_index;
	}
	
	public int getProjLength() {
		return ByteUtil.twoBytesToInteger(bs, proj_len_index);
	}

	public int getEleLenIndex(final int projLen) {
		return proj_data_index + projLen;
	}
	
	public int getEleIndex(final int eleLenIdx) {
		return eleLenIdx + LEN_STORE_LEN;
	}

	public int getEleLength(final int eleLenIdx) {
		return ByteUtil.twoBytesToInteger(bs, eleLenIdx);
	}

	public int getCodeLenIndex(final int eleIdx, final int eleLen) {
		return eleIdx + eleLen;
	}
	
	public int getCodeIndex(final int codeLenIndex) {
		return codeLenIndex + LEN_STORE_LEN;
	}
	
	public int getCodeLength(final int codeLenIndex) {
		return ByteUtil.twoBytesToInteger(bs, codeLenIndex);
	}

	public int getLength() {
		final int eleLenIndex = getEleLenIndex(getProjLength());
		final int eleIndex = getEleIndex(eleLenIndex);
		final int codeLenIndex = getCodeLenIndex(eleIndex, getEleLength(eleLenIndex));
		return getCodeIndex(codeLenIndex) + getCodeLength(codeLenIndex) - MsgBuilder.INDEX_MSG_DATA;
	}
}
