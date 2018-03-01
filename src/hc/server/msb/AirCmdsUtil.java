package hc.server.msb;

import java.util.Collections;
import java.util.Vector;

public class AirCmdsUtil {

	private static final char JIN_HAO = '#';
	public static final String PREFIX_AIRCMDS_HAR_URL = "@";

	/**
	 * 如果没有，则返回null
	 * 
	 * @param ssid
	 * @return
	 */
	public static final String getAirCmdsHarUrl(final String[] ssid) {
		final Vector<String> harurl = new Vector<String>();
		// @AIRCOND56#1#
		String header = null;
		int firstSplitIdx = 0;
		{
			final int size = ssid.length;
			for (int i = 0; i < size; i++) {
				final String item = ssid[i];
				if (item.startsWith(AirCmdsUtil.PREFIX_AIRCMDS_HAR_URL)) {
					if (header == null) {
						firstSplitIdx = item.indexOf(JIN_HAO);
						if (firstSplitIdx > 0
								&& item.indexOf(JIN_HAO, firstSplitIdx + 1) > firstSplitIdx + 1) {
							header = item.substring(0, firstSplitIdx);
						} else {
							continue;
						}
					} else if (item.startsWith(header) == false) {
						continue;
					} else if (item.indexOf(JIN_HAO, firstSplitIdx + 1) > firstSplitIdx + 1) {
					} else {
						continue;
					}
					harurl.add(item);
				}
			}
		}

		final int size = harurl.size();
		if (size > 0) {
			// 按header排序
			Collections.sort(harurl);

			final StringBuffer sb = new StringBuffer();
			for (int i = 0; i < size; i++) {
				final String item = harurl.elementAt(i);
				sb.append(item.substring(item.indexOf('#', firstSplitIdx + 1) + 1));
			}

			return sb.toString();
		}

		return null;
	}
}
