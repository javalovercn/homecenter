package hc.core.util;

public class LangUtil {
	public static final int LOCALE_SPLIT_CHAR = '-';
	public static final String LOCALE_SPLIT = String.valueOf((char)LOCALE_SPLIT_CHAR);

	//注意：如果增加，请同步更改API ProjectContext.matchLocale
	public final static String[] equalLocale = {"he", "yi", "id"};
	public final static String[] equalLocaleTo = {"iw", "ji", "in"};

	public static final String EN_US = "en-US";
	public static final String EN = "en";
	
	//https://www.loc.gov/standards/iso639-2/php/code_list.php
	private static final String[] rtl = {//https://meta.wikimedia.org/wiki/Template:List_of_language_names_ordered_by_code
		"ar", 
		"arc",
		"dv",//Divehi
		"div",//Divehi
		"fa",//波斯语
		"fas",//波斯语
		"far",
		"ha",//Hausa
		"hau",//Hausa
		"he",//Hebrew
		"heb",//Hebrew
		"hw",
		"iw",
		"ks",//Kashmiri
		"kas",//Kashmiri
		"kk",//Kazakh
		"kaz",//Kazakh
		"ku",//Kurdish
		"kur",//Kurdish
		"khw",
		"ps",//Pushto
		"pus",//Pushto
		"ur",//乌尔都
		"urd",//乌尔都
		"ug",//维吾尔
		"uig",//维吾尔
		"yi",
		"yid",
		};
	
	private static final String[] rtlWithSplit = buildWithSplit(rtl);
	
	private static String[] buildWithSplit(final String[] rtl){
		final int len = rtl.length;
		final String[] out = new String[len];
		
		for (int i = 0; i < len; i++) {
			out[i] = rtl[i] + LOCALE_SPLIT;
		}
		
		return out;
	}
	
	public static boolean isRTL(String isoLang){
		if(isoLang == null){
			return false;
		}
		
		final boolean isWithSplit = isoLang.indexOf(LOCALE_SPLIT, 0) > 0;
		
		if(isWithSplit){
			for (int i = 0; i < rtlWithSplit.length; i++) {
				if(isoLang.startsWith(rtlWithSplit[i], 0)){
					return true;
				}
			}
		}else{
			for (int i = 0; i < rtl.length; i++) {
				if(isoLang.equals(rtl[i])){
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * true : if locale like "zh-Hants-GD-CN"
	 * @param locale
	 * @return
	 */
	public static final boolean isTourPartOrMore(final String locale){
		int count = 0;
		int startIdx = 0;
		while(true){
			startIdx = locale.indexOf(LOCALE_SPLIT, startIdx);
			if(startIdx >= 0){
				count++;
				startIdx++;
			}else{
				break;
			}
		}
		return count >= 3;
	}
}
