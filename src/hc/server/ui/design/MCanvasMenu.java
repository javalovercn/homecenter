package hc.server.ui.design;

import hc.core.util.HCURL;
import hc.server.ui.JcipManager;
import hc.server.ui.MUIView;

public abstract class MCanvasMenu extends MUIView {
	
	/**
	 * 如果长度为0，或空串，表示不显示Title
	 * @return
	 */
	public abstract String getTitle();
	
	public abstract String[] getIcons();
	
	public abstract String[] getIconLabels();
	
	public abstract String[] getURLs();
	
	/**
	 * 无意义
	 * @return
	 */
	public abstract int getNumRow();
	
	/**
	 * 设定强制列数。如果为0，则表示根据图片集的最大宽度为公用宽度，最大高度为公用高度，来自动计算实际列数
	 * @return
	 */
	public abstract int getNumCol(); 
	public abstract boolean isFullMode();
	public abstract int getSize();
	
	public String buildJcip(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("{'" + HCURL.MENU_PROTOCAL + "' : ");
		
		sb.append('{');
		
			appendString(sb, getTitle(), true);
				
			JcipManager.appendArray(sb, getIcons(), true);
			
			JcipManager.appendArray(sb, getIconLabels(), true);
			
			JcipManager.appendArray(sb, getURLs(), true);
			
			appendInt(sb, getNumRow(), true);
			
			appendInt(sb, getNumCol(), true);
			
			appendBool(sb, isFullMode(), true);
			
			appendInt(sb, getSize(), false);
			
		sb.append("}}");
		return sb.toString();
	}
}
