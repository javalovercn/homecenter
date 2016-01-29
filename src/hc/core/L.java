package hc.core;

public class L {
	public static boolean O = false;
	public static boolean V = false;
	
	//平台开发环境，而非应用工程开发环境
	public static boolean isInWorkshop = false;
	
	public static void enable(final boolean enable){
		O = !enable;
	}
	
	public static void setInWorkshop(final boolean isWorkshop){
		isInWorkshop = isWorkshop;
	}
}
