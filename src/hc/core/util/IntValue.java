package hc.core.util;

public class IntValue {
	public int value;
	
	/**
	 * 比如11/2=>6, -11/2=>-6
	 * @param intValue
	 * @param d
	 * @return
	 */
	public static int divide(final int intValue, final int d){
		final int result = intValue / d;
		final int recInt = result * d;
		if(recInt == intValue){
			return result;
		}else if(recInt < intValue){
			return result + 1;
		}else{
			if(intValue < 0){
				return result - 1;
			}else{
				return result;
			}
		}
	}
	
	public static int floatStringToInt(final String f){
		return StringUtil.floatStringToInt(f);
	}
}
