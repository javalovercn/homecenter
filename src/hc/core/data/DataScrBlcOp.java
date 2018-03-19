package hc.core.data;

import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

public class DataScrBlcOp extends HCData {
	private final int x_index = MsgBuilder.INDEX_MSG_DATA;
	private final int y_index = x_index + 2;
	private final int w_index = y_index + 2;
	private final int h_index = w_index + 2;
	private final int dx_index = h_index + 2;
	private final int dy_index = dx_index + 2;

	public int getLength() {
		return 12;
	}

	public int getX() {
		return ByteUtil.twoBytesToInteger(bs, x_index);
	}

	public void setBlockPara(final int x, final int y, final int width,
			final int height, final int dx, final int dy) {
		ByteUtil.integerToTwoBytes(x, bs, x_index);
		ByteUtil.integerToTwoBytes(y, bs, y_index);
		ByteUtil.integerToTwoBytes(width, bs, w_index);
		ByteUtil.integerToTwoBytes(height, bs, h_index);
		ByteUtil.integerToTwoBytes(dx, bs, dx_index);
		ByteUtil.integerToTwoBytes(dy, bs, dy_index);
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

	public int getDX() {
		return ByteUtil.twoBytesToInteger(bs, dx_index);
	}

	public int getDY() {
		return ByteUtil.twoBytesToInteger(bs, dy_index);
	}

}
