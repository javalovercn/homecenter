package hc.util;

import hc.core.util.ExceptionReporter;
import hc.core.util.StringUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Locale;

public class UILang {
	public static final int PRODUCT_NAME = 1014;
	public static final Locale EN_LOCALE = new Locale("en", "US");
	private static Locale locale;
	private static Locale sysDefaultLocale = Locale.getDefault();
	private static Hashtable<String, String> resources;

	static{
		if(PropertiesManager.isTrue(PropertiesManager.p_ForceEn)){
			locale = EN_LOCALE;
		}else{
			locale = sysDefaultLocale;
		}
		resources = buildResourceBundle();
	}
	
	public static String getUILang(final int id){
		return resources.get(String.valueOf(id));
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
		final String userLang = locale.getLanguage() + "_" + locale.getCountry();
		
		return buildResourceBundle(userLang);
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
