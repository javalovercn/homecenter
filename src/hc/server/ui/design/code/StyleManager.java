package hc.server.ui.design.code;

import hc.server.ui.HTMLMlet;
import hc.server.ui.ProjectContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyleManager {
	
	//注意：如果增加，请同步更改replaceVariable
	public static final String[] variables = {"$smallFontSize$", "$normalFontSize$", "$largeFontSize$", "$buttonFontSize$", 
			"$buttonHeight$", "$mobileWidth$", "$mobileHeight$"};
	
	private static final Pattern variableForPattern = Pattern.compile("\\$\\w+\\$");
	
	public static String replaceVariable(final String styles, final HTMLMlet htmlmlet, final ProjectContext ctx){
		if(styles.indexOf('$', 0) > 0){
			final int[] values = {htmlmlet.getFontSizeForSmall(), htmlmlet.getFontSizeForNormal(), htmlmlet.getFontSizeForLarge(),
					htmlmlet.getFontSizeForButton(), htmlmlet.getButtonHeight(), ctx.getMobileWidth(), ctx.getMobileHeight()};
			
			final Matcher matcher = variableForPattern.matcher(styles);
			int appendIdx = 0;
			StringBuilder sb = null;
			while(matcher.find()){
				final int startIdx = matcher.start();
				final int endIdx = matcher.end();
				
				if(sb == null){
					sb = new StringBuilder(styles.length());
				}
				
				sb.append(styles.substring(appendIdx, startIdx));

				final String v = styles.substring(startIdx, endIdx);
				final int size = variables.length;
				
				boolean isReplaced = false;
				for (int i = 0; i < size; i++) {
					if(v.equals(variables[i])){
						sb.append(values[i]);
						isReplaced = true;
						break;
					}
				}
				
				if(isReplaced == false){
					sb.append(v);//没有找到的变量
				}
				
				appendIdx = endIdx;
			}
			
			if(appendIdx == 0){//没有变量
				return styles;
			}
			
			sb.append(styles.substring(appendIdx));
			
			return sb.toString();
		}else{
			return styles;
		}
	}
}
