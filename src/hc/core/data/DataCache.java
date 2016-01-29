package hc.core.data;

import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

public class DataCache extends HCData {
	private static final int proj_len_index = MsgBuilder.INDEX_MSG_DATA;
	private static final int proj_data_index = proj_len_index + 2;
	
	public final int setCacheInfo(final byte[] projID, final int projIdx, final int projLen,
			final byte[] urlID, final int urlIdx, final int urlLen,
			final byte[] codeID, final int codeIdx, final int codeLen){
		final int dataLen = projLen + urlLen + codeLen + 2 * 3;
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
		return eleLenIdx + 2;
	}

	public int getEleLength(final int eleLenIdx) {
		return ByteUtil.twoBytesToInteger(bs, eleLenIdx);
	}

	public int getCodeLenIndex(final int eleIdx, final int eleLen) {
		return eleIdx + eleLen;
	}
	
	public int getCodeIndex(final int codeLenIndex) {
		return codeLenIndex + 2;
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
