package hc.core.data;

import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

public class DataSelectTxt extends HCData {
	private final int start_x_index = MsgBuilder.INDEX_MSG_DATA;
	private final int start_y_index = start_x_index + 2;
	private final int end_x_index = start_y_index + 2;
	private final int end_y_index = end_x_index + 2;

	public static final int STRUCT_LEN = 8;

	public int getStartX() {
		return ByteUtil.twoBytesToInteger(bs, start_x_index);
	}

	public void setStartX(int x) {
		ByteUtil.integerToTwoBytes(x, bs, start_x_index);
	}

	public int getStartY() {
		return ByteUtil.twoBytesToInteger(bs, start_y_index);
	}

	public void setStartY(int y) {
		ByteUtil.integerToTwoBytes(y, bs, start_y_index);
	}

	public int getEndX() {
		return ByteUtil.twoBytesToInteger(bs, end_x_index);
	}

	public void setEndX(int x) {
		ByteUtil.integerToTwoBytes(x, bs, end_x_index);
	}

	public int getEndY() {
		return ByteUtil.twoBytesToInteger(bs, end_y_index);
	}

	public void setEndY(int y) {
		ByteUtil.integerToTwoBytes(y, bs, end_y_index);
	}

	public int getLength() {
		return STRUCT_LEN;
	}

}
