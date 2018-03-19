package hc.core.util;

public class RLEUtil {
	public static int deflate(int[] src, int len, int[] target) {
		int offset = 0;
		int targetIdx = 0;
		int next = 0;
		int count = src[offset++];// 取第一个字节
		while (offset < len) {// 当文件没有结束时执行
			next = src[offset++];
			int counter = 1;// 计重复的次数
			if (count == next) {// 如果有相同的
				counter++;
				while (offset < len && (next == (count = src[offset++]))) {// 计算重复的次数
					counter++;
				}
				while (counter >= 63) {// 重复次数大于63的情况
					target[targetIdx++] = 255;// 63个（192+63）
					target[targetIdx++] = next;
					// System.out.println("大于63的情况"+(0xc0+63)+" "+count);
					counter -= 63;// 减去处理的63个字节
				}
				if (counter > 1) {// 处理剩下的字节
					target[targetIdx++] = (0xc0 + counter);
					target[targetIdx++] = (next);
					// System.out.println("重复剩余的"+(0xc0+counter)+" "+counter);
				}
			} else {
				if (count <= 0xc0) {// 不重复小于192的情况
					// System.out.println(count);
					target[targetIdx++] = (count);
					count = next;
				} else {// 不重复大于192的情况
					target[targetIdx++] = (0xc1);
					target[targetIdx++] = (count);
					count = next;
					// System.out.println("0xc1的"+(0xc1)+count);
				}
			}
		}
		// 处理最后一个字节
		if (count <= 0xc0) {
			// System.out.println(count);
			target[targetIdx++] = (count);
		} else {
			target[targetIdx++] = (0xc1);
			target[targetIdx++] = (count);
			// System.out.println("0xc1的"+(0xc1)+count);
		}

		return targetIdx;
	}

	public static int inflate(int[] src, int len, int[] target) {
		int offset = 0;
		int targetIdx = 0;
		int count = 0;
		while (offset < len) {
			count = src[offset++];
			if (count == 0xc1) {
				target[targetIdx++] = (src[offset++]);
			} else if (count <= 0xc0) {
				target[targetIdx++] = (count);
			} else if (count > 0xc1) {
				int next = src[offset++];
				for (int i = 0; i < (count - 0xc0); i++)
					target[targetIdx++] = (next);
			}
		}

		return targetIdx - 1;
	}

	/**
	 * byte:255,[单个串长度],{int1,int2,int3..} byte:254,[一个byte，最多255个],{int}
	 * byte:253,[两个byte,j最多65535],{int}
	 * 
	 * @param rgb
	 * @param len
	 * @param bs
	 * @param offset
	 * @return
	 */
	public static int fastDeflate(int[] rgb, int len, byte[] bs,
			int bs_offset) {
		int oldbs_off = bs_offset;
		int offset = 0;
		int next = 0;
		int count = rgb[offset++];// 取第一个字节
		int storeDiffIdx = 0;
		int diffCounter = 0;
		while (offset < len) {// 当文件没有结束时执行
			next = rgb[offset++];
			int counter = 1;// 计重复的次数
			if (count == next) {// 如果有相同的
				if (diffCounter > 0) {
					diffCounter = 0;
				}
				counter++;
				while (offset < len && (next == (count = rgb[offset++]))) {// 计算重复的次数
					counter++;
					if (counter == 65535) {
						bs[bs_offset++] = (byte) 0xFD;// 253
						bs[bs_offset++] = (byte) 0xFF;
						bs[bs_offset++] = (byte) 0xFF;

						bs[bs_offset++] = (byte) ((next >>> 16) & 0xFF);
						bs[bs_offset++] = (byte) ((next >>> 8) & 0xFF);
						bs[bs_offset++] = (byte) (next & 0xFF);

						counter -= 65535;
					}
				}
				if (counter > 255) {
					bs[bs_offset++] = (byte) 0xFD;// 253
					bs[bs_offset++] = (byte) ((counter >>> 8) & 0xFF);
					bs[bs_offset++] = (byte) (counter & 0xFF);

					bs[bs_offset++] = (byte) ((next >>> 16) & 0xFF);
					bs[bs_offset++] = (byte) ((next >>> 8) & 0xFF);
					bs[bs_offset++] = (byte) (next & 0xFF);
				} else if (counter > 0) {
					bs[bs_offset++] = (byte) 0xFE;// 254
					bs[bs_offset++] = (byte) (counter & 0xFF);

					bs[bs_offset++] = (byte) ((next >>> 16) & 0xFF);
					bs[bs_offset++] = (byte) ((next >>> 8) & 0xFF);
					bs[bs_offset++] = (byte) (next & 0xFF);
				}
				counter = 0;
			} else {
				if (diffCounter++ == 0) {
					bs[bs_offset++] = (byte) 0xFF;// 255
					storeDiffIdx = bs_offset++;
					bs[storeDiffIdx] = (byte) (diffCounter & 0xFF);

					bs[bs_offset++] = (byte) ((count >>> 16) & 0xFF);
					bs[bs_offset++] = (byte) ((count >>> 8) & 0xFF);
					bs[bs_offset++] = (byte) (count & 0xFF);

				} else {
					if (diffCounter == 256) {
						diffCounter -= 255;

						bs[bs_offset++] = (byte) 0xFF;// 255
						storeDiffIdx = bs_offset++;
						bs[storeDiffIdx] = (byte) (diffCounter & 0xFF);

						bs[bs_offset++] = (byte) ((count >>> 16) & 0xFF);
						bs[bs_offset++] = (byte) ((count >>> 8) & 0xFF);
						bs[bs_offset++] = (byte) (count & 0xFF);
					} else {
						bs[storeDiffIdx] = (byte) (diffCounter & 0xFF);

						bs[bs_offset++] = (byte) ((count >>> 16) & 0xFF);
						bs[bs_offset++] = (byte) ((count >>> 8) & 0xFF);
						bs[bs_offset++] = (byte) (count & 0xFF);
					}
				}
				count = next;
			}
		}

		if (count != next) {
			bs[bs_offset++] = (byte) 0xFF;// 255
			bs[bs_offset++] = (byte) (1 & 0xFF);

			bs[bs_offset++] = (byte) ((count >>> 16) & 0xFF);
			bs[bs_offset++] = (byte) ((count >>> 8) & 0xFF);
			bs[bs_offset++] = (byte) (count & 0xFF);
		} else {
			if (diffCounter > 0) {
				if (diffCounter == 255) {
					bs[bs_offset++] = (byte) 0xFF;// 255
					bs[bs_offset++] = (byte) (1 & 0xFF);

					bs[bs_offset++] = (byte) ((count >>> 16) & 0xFF);
					bs[bs_offset++] = (byte) ((count >>> 8) & 0xFF);
					bs[bs_offset++] = (byte) (count & 0xFF);
				} else {
					bs[storeDiffIdx] = (byte) (++diffCounter & 0xFF);

					bs[bs_offset++] = (byte) ((count >>> 16) & 0xFF);
					bs[bs_offset++] = (byte) ((count >>> 8) & 0xFF);
					bs[bs_offset++] = (byte) (count & 0xFF);
				}
			}
		}
		return bs_offset - oldbs_off;
	}

	/**
	 * byte:255,[单个串长度],{int1,int2,int3..} byte:254,[一个byte，最多255个],{int}
	 * byte:253,[两个byte,j最多65535],{int}
	 * 
	 * @param bs
	 * @param offset
	 * @param len
	 * @param rgb
	 * @return
	 */
	public static int fastInflate(byte[] bs, int offset, int len, int[] rgb) {
		int rgb_idx = 0;
		int end = offset + len;
		do {
			byte tag = bs[offset++];
			if (tag == ((byte) 0xFF)) {// 255
				int count = bs[offset++] & 0xFF;
				for (int i = 0; i < count; i++) {
					int temp1 = bs[offset++] & 0xFF;
					int temp2 = bs[offset++] & 0xFF;
					int temp3 = bs[offset++] & 0xFF;
					rgb[rgb_idx++] = ((temp1 << 16) + (temp2 << 8) + temp3);
				}
			} else if (tag == ((byte) 0xFE)) {// 254
				int count = bs[offset++] & 0xFF;

				int temp1 = bs[offset++] & 0xFF;
				int temp2 = bs[offset++] & 0xFF;
				int temp3 = bs[offset++] & 0xFF;
				int samev = ((temp1 << 16) + (temp2 << 8) + temp3);

				for (int i = 0; i < count; i++) {
					rgb[rgb_idx++] = samev;
				}
			} else if (tag == ((byte) 0xFD)) {// 253
				int count = ((bs[offset++] & 0xFF) << 8)
						+ (bs[offset++] & 0xFF);

				int temp1 = bs[offset++] & 0xFF;
				int temp2 = bs[offset++] & 0xFF;
				int temp3 = bs[offset++] & 0xFF;
				int samev = ((temp1 << 16) + (temp2 << 8) + temp3);

				for (int i = 0; i < count; i++) {
					rgb[rgb_idx++] = samev;
				}
			} else {
				System.err.println("Unknow data Tag");
				return 0;
			}
		} while (offset < end);

		return rgb_idx;
	}
}