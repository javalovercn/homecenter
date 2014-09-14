package hc.core.data;

import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;

public class DataMTUTest extends HCData {
	public final int LEN_INDEX = MsgBuilder.INDEX_MSG_DATA;
	private final int data_index = LEN_INDEX + 2;

	public int getLength() {
		return ByteUtil.twoBytesToInteger(bs, LEN_INDEX) + 2;
	}

	public void setDataLen(int dataLen){
		hc.core.L.V=hc.core.L.O?false:LogManager.log("try MTU : " + dataLen);
		int realLen = dataLen - 2 - MsgBuilder.INDEX_MSG_DATA;
		ByteUtil.integerToTwoBytes(realLen, bs, LEN_INDEX);
		for (int i = 0; i < realLen; i++) {
			bs[data_index + i] = 'a';
		}
	}
	
	/**
	 * 数据区完整传送返回true；
	 * @return
	 */
	public boolean passData(){
		int realLen = ByteUtil.twoBytesToInteger(bs, LEN_INDEX);
		for (int i = 0; i < realLen; i++) {
			if(bs[data_index + i] != 'a'){
				return false;
			}
		}
		return true;
	}
	
}
