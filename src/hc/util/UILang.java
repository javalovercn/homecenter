package hc.util;

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
	
	public static String getUILang(int id){
		return resources.get(String.valueOf(id));
	}
	
	public static Locale getUsedLocale(){
		return locale;
	}
	
	public static void setLocale(Locale loc){
		if(loc == null){
			locale = sysDefaultLocale;
		}else{
			locale = loc;
		}
		Locale.setDefault(locale);
		resources = buildResourceBundle();
	}
	
	private static Hashtable<String, String> buildResourceBundle() {
		final String userLang = locale.getLanguage() + "_" + locale.getCountry();
		
		Hashtable<String, String> table = new Hashtable<String, String>();

		final String fileName = "/uilang_";
		InputStream is = null;
		final Class<UILang> baseClass = UILang.class;
		try {
			is = baseClass.getResourceAsStream(fileName + userLang + ".properties");
		} catch (Exception e) {
		}
		if(is == null){
			try {
				is = baseClass.getResourceAsStream(fileName + userLang.substring(0, 2) + ".properties");
			} catch (Exception e) {
			}
			if(is == null){
				locale = EN_LOCALE;
				sysDefaultLocale = locale;
				Locale.setDefault(locale);
				is = baseClass.getResourceAsStream(fileName + "en_US.properties");
			}
		}
		Reader stream = null;
		try {
			stream = new InputStreamReader(is, "ISO-8859-1");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		StringUtil.load(stream, table);
		
		return table;
	}
}
