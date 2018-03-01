package hc.util;

/**
 * UUID结构：用户类型标识[0-f,0:免费用户,..] + php_uuiqid()(13位) + 校验位(1位，前14位16进制之和模10)
 *
 */
public class UUIDUtil {
	/**
	 * 如果失败，则返回null
	 * 
	 * @param php_uuid
	 *            用户类型标识[0-f,0:免费用户,..] + php_uuiqid()(13位)
	 * @return
	 */
	public static String addCheckCode(String php_uuid) {
		char[] c1 = php_uuid.toCharArray();

		int len = c1.length;
		int total = 0;
		for (int i = 0; i < len; i++) {
			try {
				int num1 = Integer.parseInt(String.valueOf(c1, i, 1), 16);
				total += num1;
			} catch (Exception e) {
				return null;
			}
		}

		return php_uuid + String.valueOf(total % 10);
	}

	// public static boolean checkUUID(String uuid){
	// char[] c1 = uuid.toCharArray();
	//
	// int len = c1.length - 1;
	//
	// if(len != 14){
	// return false;
	// }
	//
	// int total = 0;
	// for (int i = 0; i < len; i++) {
	// try{
	// int num1 = Integer.parseInt(String.valueOf(c1, i, 1), 16);
	// total += num1;
	// }catch (Exception e) {
	// return false;
	// }
	// }
	//
	// int check_num = Integer.parseInt(String.valueOf(c1, len, 1), 16);
	// if(check_num == (total % 10)){
	// return true;
	// }else{
	// return false;
	// }
	// }
}
