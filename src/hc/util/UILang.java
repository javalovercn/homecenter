package hc.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class UILang {
	public static final int PRODUCT_NAME = 1014;
	public static final Locale EN_LOCALE = new Locale("en", "US");
	private static Locale locale;
	private static Locale sysDefaultLocale = Locale.getDefault();
	
	private static ResourceBundle resources;
	static{
		if(PropertiesManager.isTrue(PropertiesManager.p_ForceEn)){
			locale = EN_LOCALE;
		}else{
			locale = sysDefaultLocale;
		}
		try{
			resources = ResourceBundle.getBundle("uilang", locale);
		}catch (Exception e) {
			locale = EN_LOCALE;
			sysDefaultLocale = locale;
			Locale.setDefault(locale);
			resources = ResourceBundle.getBundle("uilang", locale);
		}
	}
	public static String getUILang(int id){
		return resources.getString(String.valueOf(id));
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
		resources = ResourceBundle.getBundle("uilang", locale);
	}
}
