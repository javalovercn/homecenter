package hc.util;


public class LinkPropertiesOption {

	public static final String getDispOpNextStartUp(){
		return (String)ResourceUtil.get(9135);
	}
	
	public static final String getDispOpAsk(){
		return (String)ResourceUtil.get(9136);
	}
	
	public static final String getDispOpImmediate(){
		return (String)ResourceUtil.get(9137);
	}
	
	public static final String getDispOpPermNoChange(){
		return (String)ResourceUtil.get(9228);
	}
	
	public static final String getDispOpPermAcceptIfSigned(){
		return (String)ResourceUtil.get(9229);
	}
	
	public static final String OP_NEXT_START_UP = "nextStartUp";
	public static final String OP_ASK = "ask";
	public static final String OP_IMMEDIATE = "immediate";
	public static final String OP_PERM_NO_CHANGE = "noChange";
	public static final String OP_PERM_ACCEPT_IF_SIGNED = "acceptIfSigned";

	/**
	 * 将各语言显示存储为proper的bug，修复一次
	 */
	public static final void fixDisplayToOpValue(){
		final String opDisp = PropertiesManager.getValue(PropertiesManager.p_OpNewLinkedInProjVer, OP_NEXT_START_UP);
		String opValue = null;
		
		if(opDisp.equals(getDispOpNextStartUp())){
			opValue = OP_NEXT_START_UP;
		}else if(opDisp.equals(getDispOpAsk())){
			opValue = OP_ASK;
		}else if(opDisp.equals(getDispOpImmediate())){
			opValue = OP_IMMEDIATE;
		}
		
		if(opValue != null && opDisp.equals(opValue) == false){
			PropertiesManager.setValue(PropertiesManager.p_OpNewLinkedInProjVer, opValue);
			PropertiesManager.saveFile();
		}
	}

}
