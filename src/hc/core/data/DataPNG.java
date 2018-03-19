package hc.core.data;

import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

/**
 * 该数据结构同时被使用于LineOn, lineOFf等服务器通讯
 * 
 * @author
 *
 */
public class DataPNG extends HCData {
	private static final int x_index = MsgBuilder.INDEX_MSG_DATA;
	private static final int y_index = x_index + 2;
	private static final int w_index = y_index + 2;
	private static final int h_index = w_index + 2;
	// private final int isrefresh_index = inflateIntLen_index + 3;
	// private final int refresh_id_index = isrefresh_index + 1;
	// private final int png_block_id_index= refresh_id_index + 2;

	// 该数据结构同时被使用于LineOn, lineOFf等服务器通讯
	private static final int png_length_index = h_index + 2;
	public static final int png_index = png_length_index + 4;

	// 尾部结构如下
	// PNG_DATA[********************************[one byte to store
	// screenID.len][??????(ScreenID bytes)]]

	// private final byte DATA_IS_REFRESH_TRUE = 't';
	// private final byte DATA_IS_REFRESH_FALSE = 'f';

	public static final int HEAD_LENGTH = png_index - MsgBuilder.INDEX_MSG_DATA;// (6
																				// +
																				// 4
																				// +
																				// 3);//1
																				// +
																				// 2
																				// +
																				// 2
																				// +

	public static final int MAX_KEEP_TAIL_LENGTH = 127; // 尾部增加ScreenID,所以保留计算所需长度

	// public int getTargetIDLen(int dataPngLen){
	// return bs[png_index + dataPngLen];
	// }

	// public int getTargetIDIdx(int dataPngLen){
	// return png_index + dataPngLen + 1;//+1表示要跳过计位TargetIDLen
	// }

	public void setTargetID(final int dataPngLen, final byte[] id,
			final int offset, final int len) {
		int storeIdx = png_index + dataPngLen;// Len_Idx
		bs[storeIdx++] = (byte) len;
		for (int i = storeIdx, j = offset; j < len;) {
			bs[i++] = id[j++];
		}
	}
	// public boolean inRefresh() {
	// return (bs[isrefresh_index] == DATA_IS_REFRESH_TRUE);
	// }
	//
	// public void setIsRefresh(boolean refresh) {
	// bs[isrefresh_index] =
	// (refresh?DATA_IS_REFRESH_TRUE:DATA_IS_REFRESH_FALSE);
	// }

	// public int getRefreshID() {
	// return ByteUtil.twoBytesToInteger(bs, refresh_id_index);
	// }

	// public void setRefreshID(int refreshID, int blockID) {
	// bs[isrefresh_index] = DATA_IS_REFRESH_TRUE;
	// ByteUtil.integerToTwoBytes(refreshID, bs, refresh_id_index);
	// ByteUtil.integerToTwoBytes(blockID, bs, png_block_id_index);
	// }

	// public int getBlockID() {
	// return ByteUtil.twoBytesToInteger(bs, png_block_id_index);
	// }

	public int getX() {
		return ByteUtil.twoBytesToInteger(bs, x_index);
	}

	public int getY() {
		return ByteUtil.twoBytesToInteger(bs, y_index);
	}

	public int getWidth() {
		return ByteUtil.twoBytesToInteger(bs, w_index);
	}

	public int getHeight() {
		return ByteUtil.twoBytesToInteger(bs, h_index);
	}

	public void setPNGDataLen(int len, final int x, final int y,
			final int width, final int height) {
		ByteUtil.integerToFourBytes(len, bs, png_length_index);

		ByteUtil.integerToTwoBytes(x, bs, x_index);
		ByteUtil.integerToTwoBytes(y, bs, y_index);
		ByteUtil.integerToTwoBytes(width, bs, w_index);
		ByteUtil.integerToTwoBytes(height, bs, h_index);
	}

	public void copyPNGDataOut(int pngDatalength, byte[] target, int offset) {
		System.arraycopy(bs, png_index, target, offset, pngDatalength);
	}

	public int getLength() {
		final int pngDataLen = getPNGDataLen();
		return HEAD_LENGTH + pngDataLen + bs[png_index + pngDataLen]// getTargetIDLen(pngDataLen)
				+ 1;
	}

	public int getPNGDataLen() {
		return (int) ByteUtil.fourBytesToLong(bs, png_length_index);
	}

}
