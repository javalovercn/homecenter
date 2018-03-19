package hc.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Properties;

import hc.core.ContextManager;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LangUtil;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;

public class UILang {
	private static final String XIA_LINE = "_";
	public static final int PRODUCT_NAME = 1014;
	public static final Locale EN_LOCALE = new Locale("en", "US");
	private static Locale locale;
	private static Locale sysDefaultLocale = Locale.getDefault();
	private static Properties resources;
	private static final Hashtable<String, Properties> mobileRes = new Hashtable<String, Properties>(60);

	static {
		if (PropertiesManager.isTrue(PropertiesManager.p_ForceEn)) {
			locale = EN_LOCALE;
		} else {
			locale = sysDefaultLocale;
		}
		resources = buildResourceBundle();
	}

	private static ThreadGroup token;

	public static void initToken(final ThreadGroup t) {
		CCoreUtil.checkAccess();
		token = t;
	}

	public static String getUILang(String locale, final int id) {
		Properties mobiR = mobileRes.get(locale);
		if (mobiR == null) {
			final int lastCompareSplitIdx = locale.lastIndexOf(LangUtil.LOCALE_SPLIT_CHAR);
			if (lastCompareSplitIdx >= 0) {
				final String[] cmpParts = StringUtil.splitToArray(locale, LangUtil.LOCALE_SPLIT);
				if (cmpParts.length == 3) {// zh-Hans-CN => zh-CN
					locale = cmpParts[0] + LangUtil.LOCALE_SPLIT + cmpParts[2];
				}
			}
			locale = locale.replace(LangUtil.LOCALE_SPLIT, XIA_LINE);
			final String p_locale = locale;
			ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() throws Throwable {
					mobileRes.put(p_locale, buildMobiResourceBundle(p_locale));
					return null;
				}
			}, token);

			mobiR = mobileRes.get(locale);
		}
		return (String) mobiR.get(String.valueOf(id));
	}

	public static String getUILang(final int id) {
		final String result = (String) resources.get(String.valueOf(id));
		// L.V = L.WShop ? false : LogManager.log("res " + id + " : " + result +
		// ", locale : " + locale);
		return result;
	}

	public static Locale getUsedLocale() {
		return locale;
	}

	public static void setLocale(final Locale loc) {
		if (loc == null) {
			locale = sysDefaultLocale;
		} else {
			locale = loc;
		}
		Locale.setDefault(locale);
		resources = buildResourceBundle();
	}

	public static final String UI_LANG_FILE_NAME = "uilang_";
	public static final String UI_LANG_FILE_NAME_PREFIX = "/" + UI_LANG_FILE_NAME;

	private static Properties buildResourceBundle() {
		final String userLang = locale.getLanguage() + XIA_LINE + locale.getCountry();

		return buildResourceBundle(userLang);
	}

	private static Properties buildMobiResourceBundle(final String userLang) {
		InputStream is = null;
		final Class<UILang> baseClass = UILang.class;
		try {
			is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + userLang + ".properties");
		} catch (final Exception e) {
		}
		if (is == null) {
			try {
				is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + userLang.substring(0, 2) + ".properties");
			} catch (final Exception e) {
			}
			if (is == null) {
				is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + "en_US.properties");
			}
		}

		final Properties p = new Properties();
		try {
			p.load(new InputStreamReader(is, "UTF-8"));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return p;
	}

	public static Properties buildResourceBundle(final String userLang) {
		InputStream is = null;
		final Class<UILang> baseClass = UILang.class;
		try {
			is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + userLang + ".properties");
		} catch (final Exception e) {
		}
		if (is == null) {
			try {
				is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + userLang.substring(0, 2) + ".properties");
			} catch (final Exception e) {
			}
			if (is == null) {
				locale = EN_LOCALE;
				sysDefaultLocale = locale;
				Locale.setDefault(locale);
				is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + "en_US.properties");
			}
		}
		final Properties p = new Properties();
		try {
			p.load(new InputStreamReader(is, "UTF-8"));
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return p;
	}
}
