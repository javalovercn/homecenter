package hc.server.ui.design.code;

import hc.server.ui.HTMLMlet;
import hc.server.ui.ProjectContext;

public class StyleManager {
	
	//注意：如果增加，请同步更改replaceVariable
	public static final String[] variables = {"$smallFontSize$", "$normalFontSize$", "$largeFontSize$", "$buttonFontSize$", 
			"$buttonHeight$", "$mobileWidth$", "$mobileHeight$"};
	
	private static final String[] variableForPattern = convertForPattern(variables);
	
	private static final String[] convertForPattern(final String[] varray){
		final int size = varray.length;
		final String[] out = new String[size];
		
		for (int i = 0; i < size; i++) {
			out[i] = varray[i].replaceAll("\\$", "\\\\\\$");
		}
		
		return out;
	}
	
	public static String replaceVariable(String styles, final HTMLMlet htmlmlet, final ProjectContext ctx){
		if(styles.indexOf('$') > 0){
			final int size = variableForPattern.length;
			final int[] values = {htmlmlet.getFontSizeForSmall(), htmlmlet.getFontSizeForNormal(), htmlmlet.getFontSizeForLarge(),
					htmlmlet.getFontSizeForButton(), htmlmlet.getButtonHeight(), ctx.getMobileWidth(), ctx.getMobileHeight()};
			for (int i = 0; i < size; i++) {
				styles = styles.replaceAll(variableForPattern[i], String.valueOf(values[i]));
			}
			return styles;
		}else{
			return styles;
		}
	}
}
