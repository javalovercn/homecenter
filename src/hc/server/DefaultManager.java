package hc.server;

import hc.core.IConstant;
import hc.util.PropertiesManager;

public class DefaultManager {
	public static final int DEFAULT_DOC_FONT_SIZE = 11;
	public static final String DEFAULT_DOC_FONT_SIZE_INPUT = String.valueOf(DEFAULT_DOC_FONT_SIZE);
	public static final int DEFAULT_DIRECT_SERVER_PORT = 0;
	public static final String DEFAULT_DIRECT_SERVER_PORT_FOR_INPUT = String
			.valueOf(DEFAULT_DIRECT_SERVER_PORT);
	public static final String DEFAULT_FONT_SIZE = "16";
	public static final String INTERVAL_SECONDS_FOR_NEXT_STARTUP = "5";
	public static final int ERR_TRY_TIMES = 10;
	public static final int LOCK_MINUTES = 3;
	public static final String PRELOAD_AFTER_STARTUP_FOR_INPUT = "120";
	public static final String SNAP_MS_FOR_INPUT = "1000";
	public static final String SNAP_HEIGHT_FOR_INPUT = "20";
	public static final String SNAP_WIDTH_FOR_INPUT = "20";
	public static final String DEL_DAYS_FOR_INPUT = "3";

	public static String getDesignerDocFontSize() {
		return PropertiesManager.getValue(PropertiesManager.p_DesignerDocFontSize,
				DEFAULT_DOC_FONT_SIZE_INPUT);
	}

	public static String getDirectServerPort() {
		return PropertiesManager.getValue(PropertiesManager.p_selectedNetworkPort,
				DEFAULT_DIRECT_SERVER_PORT_FOR_INPUT);
	}

	public static boolean isHideIDForErrCert() {
		if (isEnableTransNewCertNow()) {
			return false;
		}

		final String valueHideID = PropertiesManager.getValue(PropertiesManager.p_HideIDForErrCert);
		if (valueHideID == null) {
			if (isEnableTransNewCertNow()) {
				return false;
			} else {
				return true;
			}
		}
		return valueHideID.equals(IConstant.TRUE);
	}

	public static boolean isEnableTransNewCertNow() {
		final String out = PropertiesManager.getValue(PropertiesManager.p_EnableTransNewCertKeyNow);
		if (out == null || out.equals(IConstant.TRUE)) {
			return true;
		} else {
			return false;
		}
	}

}
