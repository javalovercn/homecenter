package hc.core.util;

public class LangUtil {
	public static final int LOCALE_SPLIT_CHAR = '-';
	public static final String LOCALE_SPLIT = String.valueOf((char)LOCALE_SPLIT_CHAR);

	//注意：如果增加，请同步更改API ProjectContext.matchLocale
	public final static String[] equalLocale = {"he", "yi", "id", "cb"};
	public final static String[] equalLocaleTo = {"iw", "ji", "in", "ckb"};

	public final static String[] oneThreeEquals = {"zh-CN", "zh-CN", "zh-SG", "zh-TW"};
	public final static String[] oneTwoEquals = {"zh-SG", "zh-Hans", "zh-Hans", "zh-Hant"};

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
//		"kk",//Kazakh
//		"kaz",//Kazakh
		"ku",//Kurdish
		"kur",//Kurdish
		"khw",
		"ps",//Pashto
		"pus",//Pashto
		"ur",//乌尔都Urdu
		"urd",//乌尔都Urdu
//		"ug",//维吾尔Uighur; Uyghur	
//		"uig",//维吾尔Uighur; Uyghur	
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

	/**
	 * return true, if meet :<BR>
	 * 1. en ~ en-US<BR>
	 * 2. zh-CN ~ zh-Hans-CN<BR>
	 * @param locale
	 * @param compareLocale
	 * @param isMaybeEqualLang true表示使用可替换lang，比如旧遗留和二码和三码相等
	 * @return
	 */
	public static boolean isSameLang(final String locale, final String compareLocale, final boolean isMaybeEqualLang){
		if(locale.equals(compareLocale)){
			return true;
		}
		
		final int lastSplitIdx = locale.lastIndexOf(LOCALE_SPLIT_CHAR);
		final int lastCompareSplitIdx = compareLocale.lastIndexOf(LOCALE_SPLIT_CHAR);

		if(lastSplitIdx > lastCompareSplitIdx){
			return isSameLang(compareLocale, locale, isMaybeEqualLang);
		}
		
		if(lastSplitIdx >= 0){
			final String[] cmpParts = StringUtil.splitToArray(compareLocale, LOCALE_SPLIT);
			
			if(isMaybeEqualLang){
				final String[] parts = StringUtil.splitToArray(locale, LOCALE_SPLIT);
				final String part0 = parts[0];
				
				for (int i = 0; i < equalLocale.length; i++) {
					if(part0.equals(equalLocale[i])){
						return isSameLang(buildEqualLocale(parts, equalLocaleTo[i]), compareLocale, false);
					}
				}
				for (int i = 0; i < equalLocaleTo.length; i++) {
					if(part0.equals(equalLocaleTo[i])){
						return isSameLang(buildEqualLocale(parts, equalLocale[i]), compareLocale, false);
					}
				}
			}
			
			if(cmpParts.length == 3){//zh-Hans-CN => zh-CN
				final String oneTwo = cmpParts[0] + LangUtil.LOCALE_SPLIT + cmpParts[1];
				if(locale.equals(oneTwo)){
					return true;
				}
				
				for (int i = 0; i < oneTwoEquals.length; i++) {
					if(oneTwo.equals(oneTwoEquals[i])){
						if(locale.equals(oneThreeEquals[i])){
							return true;
						}
					}
				}
				
				for (int i = 0; i < oneTwoEquals.length; i++) {
					if(locale.equals(oneTwoEquals[i])){
						if(oneTwo.equals(oneThreeEquals[i])){
							return true;
						}
					}
				}
				
				final String oneThree = cmpParts[0] + LangUtil.LOCALE_SPLIT + cmpParts[2];
				if(locale.equals(oneThree)){
					return true;
				}
				
				for (int i = 0; i < oneThreeEquals.length; i++) {
					if(oneThree.equals(oneThreeEquals[i])){
						if(locale.equals(oneTwoEquals[i])){
							return true;
						}
					}
				}
				
				for (int i = 0; i < oneThreeEquals.length; i++) {
					if(locale.equals(oneThreeEquals[i])){
						if(oneThree.equals(oneTwoEquals[i])){
							return true;
						}
					}
				}
				
				if (isSameLang(oneTwo, compareLocale, false) == false){
					return isSameLang(oneThree, compareLocale, false);
				}
			}else if(cmpParts.length == 2){//zh-CN => zh
				for (int i = 0; i < oneTwoEquals.length; i++) {
					if(locale.equals(oneTwoEquals[i])){
						if(compareLocale.equals(oneThreeEquals[i])){
							return true;
						}
					}
				}
				
				for (int i = 0; i < oneThreeEquals.length; i++) {
					if(locale.equals(oneThreeEquals[i])){
						if(compareLocale.equals(oneTwoEquals[i])){
							return true;
						}
					}
				}
			}
			
			//不需两段转一段，因locale是两段
		}else{
			if(lastCompareSplitIdx > 0){//he ~ he-FF
				final String[] cmpParts = StringUtil.splitToArray(compareLocale, LOCALE_SPLIT);
				if(locale.equals(cmpParts[0])){
					return true;
				}
			}
			
			if(isMaybeEqualLang){
				for (int i = 0; i < equalLocale.length; i++) {
					if(locale.equals(equalLocale[i])){
						return isSameLang(equalLocaleTo[i], compareLocale, false);
					}
				}
				
				for (int i = 0; i < equalLocaleTo.length; i++) {
					if(locale.equals(equalLocaleTo[i])){
						return isSameLang(equalLocale[i], compareLocale, false);
					}
				}
			}
			
			//en ~ en-US
			if(lastCompareSplitIdx >= 0){
				final String[] cmpParts = StringUtil.splitToArray(compareLocale, LOCALE_SPLIT);
				if(cmpParts.length == 3){//zh-Hans-CN => zh-CN
					return isSameLang(locale, cmpParts[0] + LOCALE_SPLIT + cmpParts[2], false);
				}else if(cmpParts.length == 2){//zh-CN => zh
					return isSameLang(locale, cmpParts[0], false);
				}
			}
		}
	
		return false;
	}

	public static String buildEqualLocale(final String[] oldParts, final String equalLang){
		final StringBuffer sb = StringBufferCacher.getFree();
		
		for (int i = 0; i < oldParts.length; i++) {
			if(i == 0){
				sb.append(equalLang);
			}else{
				sb.append(LOCALE_SPLIT);
				sb.append(oldParts[i]);
			}
		}
		
		final String out = sb.toString();
		StringBufferCacher.cycle(sb);
		return out;
	}

	public static String toShortZh(String userLang) {
	    final String lowercase = userLang.toLowerCase();
		if (lowercase.startsWith("zh-hans")) {
			userLang = "zh-CN";
	    }else if(lowercase.startsWith("zh-hant")){
	    	userLang = "zh-TW";
	    }
	
		return userLang;
	}

}
