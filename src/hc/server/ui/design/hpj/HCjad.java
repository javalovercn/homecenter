package hc.server.ui.design.hpj;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import hc.App;
import hc.core.util.ExceptionReporter;
import hc.server.ui.design.Designer;
import hc.util.ResourceUtil;

public class HCjad {

	public static final String HAD_ID = "HAR-ID";
	public static final String HAD_VERSION = "HAR-Version";
	public static final String HAD_JRUBY_MIN_VERSION = "JRuby-Min-Version";
	public static final String HAD_JRE_MIN_VERSION = "JRE-Min-Version";
	public static final String HAD_HC_MIN_VERSION = "HomeCenter-Min-Version";
	public static final String HAD_HAR_MD5 = "HAR-MD5";
	public static final String HAD_HAR_SIZE = "HAR-Size";
	public static final String HAD_HAR_URL = "HAR-URL";

	public static final String convertToExtHad(final String harURL) {
		return harURL.substring(0, harURL.length() - Designer.HAR_EXT.length()) + Designer.HAD_EXT;
	}

	public static final String convertToExtHar(final String hadURL) {
		return hadURL.substring(0, hadURL.length() - Designer.HAD_EXT.length()) + Designer.HAR_EXT;
	}

	private static final void writeHadLine(final Writer out, final String key, final Object value) throws Exception {
		out.write(key + ": " + value + "\r\n");
	}

	public static final void toHad(final Map<String, Object> map, final File hadfile, final String harmd5, final int length) {
		if (hadfile.exists()) {
			hadfile.delete();
		}
		try {
			final FileOutputStream fos = new FileOutputStream(hadfile);
			final Writer out = new OutputStreamWriter(fos, "UTF-8");

			writeHadLine(out, HAD_ID, map.get(HCjar.PROJ_ID));
			writeHadLine(out, HAD_VERSION, map.get(HCjar.PROJ_VER));
			final Object jruby_ver = map.get(HCjar.JRUBY_VER);
			if (jruby_ver != null) {
				writeHadLine(out, HAD_JRUBY_MIN_VERSION, jruby_ver);
			}
			writeHadLine(out, HAD_JRE_MIN_VERSION, map.get(HCjar.JRE_VER));
			writeHadLine(out, HAD_HC_MIN_VERSION, map.get(HCjar.HOMECENTER_VER));
			writeHadLine(out, HAD_HAR_MD5, harmd5);

			writeHadLine(out, HAD_HAR_SIZE, length);

			writeHadLine(out, "#" + HAD_HAR_URL, "keep # if in same directory");

			out.close();
			fos.close();
		} catch (final Exception e) {
			App.showErrorMessageDialog(null, "fail to save HAD : " + e.getMessage(), ResourceUtil.getErrorI18N());
			ExceptionReporter.printStackTrace(e);
		}
	}

}
