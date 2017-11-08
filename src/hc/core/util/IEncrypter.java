package hc.core.util;

/**
 * It is not required to implement this interface to enhance the encryption.
 * <br>
 * Powerful and efficient encryption system is built in HomeCenter system.
 * <br><br>
 * How to develop a encryptor, please see <a target="_blank" href="http://homecenter.mobi/en/pc/faq.htm#item7">http://homecenter.mobi/en/pc/faq.htm#item7</a>
 */
public interface IEncrypter {
	/**
	 * 
	 * @param uuid UTF-8 of uuid String
	 */
	public void setUUID(byte[] uuid);
	
	/**
	 * 
	 * @param password UTF-8 of password String
	 */
	public void setPassword(byte[] password);
	
	/**
	 * system will call initEncrypter method before encrypt or decrypt;
	 * System will call setUUID and setPassword before initEncrypter method.
	 */
	public void initEncrypter(boolean isMobileSide);
	
	/**
	 * Note: Process data ONLY inside [offset, offset + len - 1].
	 * @param data
	 * @param offset
	 * @param len
	 */
	public void encryptCertKey(byte[] data, int offset, int len);
	
	/**
	 * Note: Process data ONLY inside [offset, offset + len - 1].
	 * @param data
	 * @param offset
	 * @param len
	 */
	public void decryptCertKey(byte[] data, int offset, int len);
	
	/**
	 * Note: Process data ONLY inside [offset, offset + len - 1].
	 * @param data
	 * @param offset
	 * @param len
	 */
	public void encryptData(byte[] data, int offset, int len);
	
	/**
	 * Note: Process data ONLY inside [offset, offset + len - 1].
	 * @param data
	 * @param offset
	 * @param len
	 */
	public void decryptData(byte[] data, int offset, int len);
	
	/**
	 * When mobile or PC exit, It will be called.
	 */
	public void notifyExit(boolean isMobileSide);
}
