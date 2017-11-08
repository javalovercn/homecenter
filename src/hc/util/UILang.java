package hc.util;

import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LangUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Locale;

public class UILang {
	private static final String XIA_LINE = "_";
	public static final int PRODUCT_NAME = 1014;
	public static final Locale EN_LOCALE = new Locale("en", "US");
	private static Locale locale;
	private static Locale sysDefaultLocale = Locale.getDefault();
	private static Hashtable<String, String> resources;
	private static final Hashtable<String, Hashtable<String, String>> mobileRes = new Hashtable<String, Hashtable<String,String>>(60);
	
	static{
		if(PropertiesManager.isTrue(PropertiesManager.p_ForceEn)){
			locale = EN_LOCALE;
		}else{
			locale = sysDefaultLocale;
		}
		resources = buildResourceBundle();
	}
	
	private static ThreadGroup token;
	
	public static void initToken(final ThreadGroup t){
		CCoreUtil.checkAccess();
		token = t;
	}
	
	public static String getUILang(String locale, final int id){
		Hashtable<String, String> mobiR = mobileRes.get(locale);
		if(mobiR == null){
			final int lastCompareSplitIdx = locale.lastIndexOf(LangUtil.LOCALE_SPLIT_CHAR);
			if(lastCompareSplitIdx >= 0){
				final String[] cmpParts = StringUtil.splitToArray(locale, LangUtil.LOCALE_SPLIT);
				if(cmpParts.length == 3){//zh-Hans-CN => zh-CN
					locale =cmpParts[0] + LangUtil.LOCALE_SPLIT + cmpParts[2];
				}
			}
			locale = locale.replace(LangUtil.LOCALE_SPLIT, XIA_LINE);
			final String p_locale = locale;
			ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() {
					mobileRes.put(p_locale, buildMobiResourceBundle(p_locale));
					return null;
				}
			}, token);
			
			mobiR = mobileRes.get(locale);
		}
		return mobiR.get(String.valueOf(id));
	}
	
	public static String getUILang(final int id){
		final String result = resources.get(String.valueOf(id));
//		L.V = L.WShop ? false : LogManager.log("res " + id + " : " + result + ", locale : " + locale);
		return result;
	}
	
	public static Locale getUsedLocale(){
		return locale;
	}
	
	public static void setLocale(final Locale loc){
		if(loc == null){
			locale = sysDefaultLocale;
		}else{
			locale = loc;
		}
		Locale.setDefault(locale);
		resources = buildResourceBundle();
	}
	
	public static final String UI_LANG_FILE_NAME = "uilang_";
	public static final String UI_LANG_FILE_NAME_PREFIX = "/" + UI_LANG_FILE_NAME;

	private static Hashtable<String, String> buildResourceBundle() {
		final String userLang = locale.getLanguage() + XIA_LINE + locale.getCountry();
		
		return buildResourceBundle(userLang);
	}

	private static Hashtable<String, String> buildMobiResourceBundle(final String userLang) {
		final Hashtable<String, String> table = new Hashtable<String, String>();

		InputStream is = null;
		final Class<UILang> baseClass = UILang.class;
		try {
			is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + userLang + ".properties");
		} catch (final Exception e) {
		}
		if(is == null){
			try {
				is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + userLang.substring(0, 2) + ".properties");
			} catch (final Exception e) {
			}
			if(is == null){
				is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + "en_US.properties");
			}
		}
		Reader stream = null;
		try {
			stream = new InputStreamReader(is, "ISO-8859-1");
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			return null;
		}

		StringUtil.load(stream, table);
		
		return table;
	}
	
	public static Hashtable<String, String> buildResourceBundle(final String userLang) {
		final Hashtable<String, String> table = new Hashtable<String, String>();

		InputStream is = null;
		final Class<UILang> baseClass = UILang.class;
		try {
			is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + userLang + ".properties");
		} catch (final Exception e) {
		}
		if(is == null){
			try {
				is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + userLang.substring(0, 2) + ".properties");
			} catch (final Exception e) {
			}
			if(is == null){
				locale = EN_LOCALE;
				sysDefaultLocale = locale;
				Locale.setDefault(locale);
				is = baseClass.getResourceAsStream(UI_LANG_FILE_NAME_PREFIX + "en_US.properties");
			}
		}
		Reader stream = null;
		try {
			stream = new InputStreamReader(is, "ISO-8859-1");
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			return null;
		}

		StringUtil.load(stream, table);
		
		return table;
	}
}
