package hc.core;

public class L {
	public static boolean V = false;
	public static boolean WShop = true;//缺省是关闭
	
	/**
	 * 平台开发环境，而非应用工程开发环境<BR>
	 * it is equal with isSimu
	 */
	public static boolean isInWorkshop = false;
	
	public static void setInWorkshop(final boolean isWorkshop){
		isInWorkshop = isWorkshop;
		WShop = !isWorkshop;
	}
}
