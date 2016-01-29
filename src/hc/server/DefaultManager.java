package hc.server;

import hc.util.PropertiesManager;

public class DefaultManager {
	public static final int DEFAULT_DOC_FONT_SIZE = 12;
	
	public static String getDesignerDocFontSize(){
		return PropertiesManager.getValue(PropertiesManager.p_DesignerDocFontSize, String.valueOf(DEFAULT_DOC_FONT_SIZE));
	}

}
