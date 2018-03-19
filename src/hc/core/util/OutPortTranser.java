package hc.core.util;

import java.util.Date;

public class OutPortTranser {
	StringBuffer sb;

	public OutPortTranser() {
		sb = new StringBuffer();
	}

	public void out(Date bs) {
		if (sb.length() > 0) {
			sb.append(',');
		}

		sb.append("'" + String.valueOf(bs.getTime()) + "'");
	}

	public void out(final String str) {
		if (sb.length() > 0) {
			sb.append(',');
		}

		outStr(str);
	}

	private void outStr(final String str) {
		sb.append('\'');
		int index_tag = 0;
		if ((index_tag = str.indexOf('\'')) >= 0) {
			int startIdx = 0;
			while (index_tag >= 0) {
				sb.append(str.substring(startIdx, index_tag));
				sb.append('\\');
				sb.append('\'');
				startIdx = index_tag + 1;
				index_tag = str.indexOf('\'', startIdx);
			}
			sb.append(str.substring(startIdx));
		} else {
			sb.append(str);
		}
		sb.append('\'');
	}

	public void out(final String[] strs) {
		if (sb.length() > 0) {
			sb.append(',');
		}

		sb.append('[');
		for (int i = 0; i < strs.length; i++) {
			if (i != 0) {
				sb.append(',');
			}
			outStr(strs[i]);
		}
		sb.append(']');
	}

	public void out(final boolean b) {
		if (b) {
			out("true");
		} else {
			out("false");
		}
	}

	public void out(final boolean[] bs) {
		if (sb.length() > 0) {
			sb.append(',');
		}

		sb.append('[');
		for (int i = 0; i < bs.length; i++) {
			if (i != 0) {
				sb.append(',');
			}
			if (bs[i]) {
				sb.append("'true'");
			} else {
				sb.append("'false'");
			}
		}
		sb.append(']');
	}

	public String getSubmitString() {
		return "{" + sb.toString() + "}";
	}
}
