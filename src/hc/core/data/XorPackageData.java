package hc.core.data;

import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

public class XorPackageData extends HCData {
	public final static int LEN_WITH_HEADER = MsgBuilder.INDEX_MSG_DATA + MsgBuilder.XOR_PACKAGE_ID_LEN;
	
	private final int connectionId_index = MsgBuilder.INDEX_MSG_DATA;
	
	public final void setConnectionPackageID(final long connectionID){
		ByteUtil.longToEightBytes(connectionID, bs, connectionId_index);
	}
	
	public final long getConnectionPackageID(){//final byte[] bs
//		System.arraycopy(this.bs, connectionId_index, bs, 0, MsgBuilder.XOR_PACKAGE_ID_LEN);
		return ByteUtil.eightBytesToLong(bs, connectionId_index);
	}
	
	public final int getLength() {
		return LEN_WITH_HEADER - MsgBuilder.INDEX_MSG_DATA;
	}

	public static XorPackageData buildEmptyPackageData(){
		final XorPackageData xpd = new XorPackageData();
		final byte[] bs = new byte[LEN_WITH_HEADER];
		xpd.setBytes(bs);
		return xpd;
	}

}
