package hc.server.util;

import hc.App;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.LogManager;
import hc.server.PlatformManager;
import hc.server.ui.SingleMessageNotify;
import hc.util.ResourceUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class ServerCUtil {
	/**
	 * 
	 * @param ctx 
	 * @param data
	 * @param offset
	 * @param certKeyLen
	 * @param storeVerifyIdx
	 * @return
	 */
	public static short checkCertKeyAndPwd(final IContext ctx, final byte[] OneTimeCertKey, final byte[] data, final int offset, final byte[] pwd, final byte[] certKey, final byte[] oldData){
//		LogManager.logInTest("before : \n" + CUtil.toHexString(oldData, CUtil.VERIFY_CERT_IDX + offset, CCoreUtil.CERT_KEY_LEN));
		CUtil.buildCheckCertKeyAndPwd(ctx, OneTimeCertKey, oldData, offset, pwd, certKey, true);
//		LogManager.logInTest("after : \n" + CUtil.toHexString(oldData, CUtil.VERIFY_CERT_IDX + offset, CCoreUtil.CERT_KEY_LEN));
//		LogManager.logInTest("receive ckp : \n" + CUtil.toHexString(data, CUtil.VERIFY_CERT_IDX + offset, CCoreUtil.CERT_KEY_LEN));
		final int endIdx = CUtil.VERIFY_CERT_IDX + CCoreUtil.CERT_KEY_LEN;
		final int firstHalfEndIdx = CUtil.VERIFY_CERT_IDX + CCoreUtil.CERT_KEY_LEN / 2;
//		String str1, str2;
//		str1 = "";
//		str2 = "";
		for (int i = endIdx - 1; i >= CUtil.VERIFY_CERT_IDX; i--) {
			final int dataIdx = offset + i;
//			str1 += " " + (Integer.toHexString(0xFF & oldData[i]));
//			str2 += " " + (Integer.toHexString(0xFF & data[i]));
			
//			System.out.println("Str1 : " + str1);
//			System.out.println("Str2 : " + str2);
			
			if(i < firstHalfEndIdx){
//				System.out.println("security i:" + i);
				if(oldData[dataIdx] != data[dataIdx]){
					notifyErrPWDDialog();
					return IContext.BIZ_SERVER_AFTER_PWD_ERROR;
				}
			}else{
//				System.out.println("security i:" + i);
				if(oldData[dataIdx] != data[dataIdx]){
					return IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR;
				}
			}
		}
		L.V = L.O ? false : LogManager.log("Client pass certkey and password");
		return IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS;
	}

	public static void notifyErrPWDDialog() {
		CCoreUtil.checkAccess();
		
		final String errPwd = (String)ResourceUtil.get(9185);
		final String errPwdTitle = (String)ResourceUtil.get(9184);
		SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_ERROR_PASS, 
				errPwd, errPwdTitle, 1000 * 60, 
				App.getSysIcon(App.SYS_ERROR_ICON));
		LogManager.errToLog(errPwd);
	}

	public static void transCertKey(final IContext ic, final byte[] oneTimeCertKey, final byte msgTag, final boolean isOneTimeKeys) {
		final byte[] transCKBS = new byte[MsgBuilder.UDP_BYTE_SIZE];
		
		CUtil.generateTransCertKey(ic, ic.coreSS.OneTimeCertKey, ResourceUtil.getStartMS(), transCKBS, MsgBuilder.INDEX_MSG_DATA, oneTimeCertKey, IConstant.getPasswordBS(), isOneTimeKeys);
		
		ic.send(msgTag, transCKBS, CUtil.TRANS_CERT_KEY_LEN);
	}


	public static void transOneTimeCertKey(final IContext ctx) {
		L.V = L.O ? false : LogManager.log("transport one time certification key to client");
		
		//传输OneTimeCertKey
		byte[] oneTimeCertKey = ctx.coreSS.OneTimeCertKey;
		if(oneTimeCertKey == null){
			oneTimeCertKey = new byte[CCoreUtil.CERT_KEY_LEN];
			ctx.coreSS.setOneTimeCertKey(oneTimeCertKey);
		}
		CCoreUtil.generateRandomKey(ResourceUtil.getStartMS(), oneTimeCertKey, 0, CCoreUtil.CERT_KEY_LEN);
//		L.V = L.O ? false : LogManager.log("OneTime:" + CUtil.toHexString(CUtil.OneTimeCertKey));
		transCertKey(ctx, oneTimeCertKey, MsgBuilder.E_TRANS_ONE_TIME_CERT_KEY, true);
	}
	
	private final static String Algorithm = "DES"; // 定义 加密算法,可用DES,DESede,Blowfish
	public final static String oldCipherAlgorithm = "DES";
	public final static String CipherAlgorithm = "DES/ECB/NoPadding";
	
	static {
		try{
			PlatformManager.getService().addJCEProvider();
		}catch (final Throwable e) {
		}
	}

	public static InputStream decodeStream(final InputStream in, byte[] key, final String cipherAlgorithm)
			throws Exception {
		key = doubePWD(key);
		
		// DES算法要求有一个可信任的随机数源
		final SecureRandom sr = new SecureRandom();
		// 创建一个 DESKeySpec 对象,指定一个 DES 密钥
		final DESKeySpec ks = new DESKeySpec(key);
		// 生成指定秘密密钥算法的 SecretKeyFactory 对象。
		final SecretKeyFactory factroy = SecretKeyFactory.getInstance(Algorithm);
		// 根据提供的密钥规范（密钥材料）生成 SecretKey 对象,利用密钥工厂把DESKeySpec转换成一个SecretKey对象
		final SecretKey sk = factroy.generateSecret(ks);
		// 生成一个实现指定转换的 Cipher 对象。Cipher对象实际完成加解密操作
		final Cipher c = Cipher.getInstance(cipherAlgorithm);
		// 用密钥和随机源初始化此 cipher
		c.init(Cipher.DECRYPT_MODE, sk, sr);

		// 从 InputStream 和 Cipher 构造 CipherInputStream。
		// read() 方法在从基础 InputStream 读入已经由 Cipher 另外处理(加密或解密)
		final CipherInputStream cin = new CipherInputStream(in, c);

		return cin;
	}

	public static OutputStream encodeStream(final OutputStream out, byte[] key)
			throws Exception {
		key = doubePWD(key);
		
		// 创建一个 DESKeySpec 对象,指定一个 DES 密钥
		final DESKeySpec ks = new DESKeySpec(key);
		// 生成指定秘密密钥算法的 SecretKeyFactory 对象。
		final SecretKeyFactory factroy = SecretKeyFactory.getInstance(Algorithm);
		// 根据提供的密钥规范（密钥材料）生成 SecretKey 对象,利用密钥工厂把DESKeySpec转换成一个SecretKey对象
		final SecretKey sk = factroy.generateSecret(ks);
		
		
//		// 秘密（对称）密钥(SecretKey继承(key))
//		// 根据给定的字节数组构造一个密钥。
//		SecretKey deskey = new SecretKeySpec(key, Algorithm);
		// 生成一个实现指定转换的 Cipher 对象。Cipher对象实际完成加解密操作
		final Cipher c = Cipher.getInstance(CipherAlgorithm);
		// 用密钥初始化此 cipher
		c.init(Cipher.ENCRYPT_MODE, sk);

		// CipherOutputStream 由一个 OutputStream 和一个 Cipher 组成
		// write() 方法在将数据写出到基础 OutputStream 之前先对该数据进行处理(加密或解密)
		final CipherOutputStream cout = new CipherOutputStream(out, c);
		return cout;
	}

	private static byte[] doubePWD(final byte[] key) {
		if(key.length <= 8){
			final byte[] newKey = new byte[key.length * 2];
			for (int i = 0; i < key.length; i++) {
				newKey[i] = key[i];
			}
			for (int i = key.length, j = 0; i < newKey.length; i++, j++) {
				newKey[i] = key[j];
			}
			return newKey;
		}else{
			return key;
		}
	}
	
}
