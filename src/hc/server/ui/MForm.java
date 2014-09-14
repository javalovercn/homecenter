package hc.server.ui;

import hc.core.util.HCURL;

public abstract class MForm extends MUIView implements ICanvas{
	public String buildJcip(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("{'" + HCURL.FORM_PROTOCAL + "' : ");
		
		sb.append('{');
			appendString(sb, getTitle(), true);
			sb.append(getItems());//, true);
			
			String init = getInit();
			if(init == null){
			}else{
				sb.append(',');
				
				appendString(sb, "Init", false);
				sb.append('=');
				appendString(sb, init, false);
			}
			
			int iomode = getIOMode();
			sb.append(',');
			
			appendString(sb, "IO", false);
			sb.append('=');
			appendInt(sb, iomode, false);
			
			String id = getID();
			if(id == null){
			}else{
				sb.append(',');
				
				appendString(sb, "ID", false);
				sb.append('=');
				appendString(sb, id, false);
			}
		
		sb.append("}}");
		return sb.toString();
	}
	
	public abstract String getTitle();
	
	public abstract String getItems();
	
	/**
	 * 如果返回null，则不进行此项
	 * @return
	 */
	public abstract String getInit();
	
	public abstract short getIOMode();
	
	public abstract String getID();
}
