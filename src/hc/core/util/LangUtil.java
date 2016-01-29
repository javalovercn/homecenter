package hc.core.util;

public class LangUtil {
	private static final String[] rtl = {
		"ar", 
		"hw",//希伯来
		"fa",//波斯语
		"ur",//乌尔都
		"ug",//维吾尔
		"kk"//哈萨克
		};//泰语、印地语、泰米尔语或马拉雅拉姆语
	
	public static boolean isRTL(String isoLang){
		if(isoLang == null){
			return false;
		}
		
		for (int i = 0; i < rtl.length; i++) {
			if(isoLang.startsWith(rtl[i])){
				return true;
			}
		}
		
		return false;
	}
}
