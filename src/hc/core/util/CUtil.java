package hc.core.util;

import hc.core.ContextManager;
import hc.core.EventCenter;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IEventHCListener;
import hc.core.L;
import hc.core.Message;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import java.util.Random;

public class CUtil {
	public static int factor = CCoreUtil.resetFactor();
	
//	public static void superXor(final byte[] src, final int offset, final int len, final byte[] keys, final boolean isEncode){
//		superXor(src, offset, len, keys, 0, keys.length, isEncode);
//	}
	
	public static void superXor(final byte[] src, final int offset, final int s_len, final byte[] keys, final boolean isEncode){
		final int k_len = keys.length;
		final int endIdx = offset + s_len;

		if(isEncode){
    		if(userEncryptor != null){
    			userEncryptor.encryptData(src, offset, s_len);
    		}
    		int modeKIdx = 0;
    		for (int i = offset; i < endIdx; i++) {
    			src[i] ^= keys[modeKIdx++];
    			if(modeKIdx == k_len){
    				modeKIdx = 0;
    			}
    			src[i] ^= keys[modeKIdx++];
    			if(modeKIdx == k_len){
    				modeKIdx = 0;
    			}
//    			System.out.println("modeKIdx : " + modeKIdx);
    		}
		}
		final int factorBigOne = (factor < 2)?2:factor;
		final int factor_temp = (s_len <= 10)?factorBigOne*4:((s_len <= 20)?factorBigOne*2:factorBigOne);
		int modeK = isEncode?0:((factor_temp*s_len-1) % k_len);
		int t = isEncode?0:(factor_temp-1);
		for (; t >= 0 && t < factor_temp; ) {
			int i = isEncode?offset:(endIdx-1);
			for (; i >= offset && i < endIdx; ) {
				final byte maskKey = keys[(t + (modeK)) % k_len];
				final int maskKeyInt = maskKey & (0xFF);
				final int storeIdx = ((factor_temp + t + (t==0?i:i<<1) + maskKeyInt + (factor_temp==0?0:modeK))%s_len) + offset;
//				if(storeIdx < offset || storeIdx >= (offset + s_len)){
//					LogManager.logInTest("storeIdx : " + storeIdx + ", modeK:" + modeK + ", k_len:" + k_len + ", maskKey:" + (maskKey & 0xFF) + ", s_len:" + s_len);
//				}
				final boolean maskKModTwo = (modeK % 2) == 0;
				if(isEncode == false && maskKModTwo){
					src[storeIdx] ^= maskKey;
				}
				if(maskKey % 2 == 0){
					if(isEncode){
						src[storeIdx] += maskKey / 2;
					}else{
						src[storeIdx] -= maskKey / 2;
					}
				}else{
					if(isEncode){
						src[storeIdx] -= maskKey;
					}else{
						src[storeIdx] += maskKey;
					}
				}
//				System.out.println("storeIdx : " + storeIdx + ", modeK:" + modeK + ", i : " + i + ", t : " + t + ", maskKey : " + maskKey);
				if(isEncode && maskKModTwo){
					src[storeIdx] ^= maskKey;
				}
				if(isEncode){
					if(++modeK == k_len){
						modeK = 0;
					}
					i++;
				}else{
					if(--modeK == -1){
						modeK = k_len - 1;
					}
					i--;
				}
			}
			if(isEncode){
				t++;
			}else{
				t--;
			}
		}
		
		if(isEncode == false){
    		int modeKIdx = (s_len * 2 - 1) % k_len;
//    		System.out.println("modeKIdx : " + modeKIdx);
    		for (int i = endIdx - 1; i >= offset; i--) {
    			src[i] ^= keys[modeKIdx--];
    			if(modeKIdx == -1){
    				modeKIdx = k_len - 1;
    			}
    			src[i] ^= keys[modeKIdx--];
    			if(modeKIdx == -1){
    				modeKIdx = k_len - 1;
    			}
    		}
    		if(userEncryptor != null){
    			userEncryptor.decryptData(src, offset, s_len);
    		}
		}
	}
	
	public static void xor(final byte[] src, final int offset, final int len, final byte[] keys){
		final int k_len = keys.length;
		final int endIdx = offset + len;
		int modeK = 0;
		for (int i = offset; i < endIdx; i++) {
			src[i] ^= keys[modeK++];
			if(modeK == k_len){
				modeK = 0;
			}
		}
	}
	
	/**
	 * 将新的CertKey用母密进行加密，加密后全长为TRANS_CERT_KEY_LEN
	 * @param certKey
	 * @return
	 */
	public static void generateTransCertKey(final byte[] data, final int offset, final byte[] certKey, final byte[] password, boolean isOneTimeKey){
		final int index = TRANS_CERT_KEY_START_IDX + offset;
		CCoreUtil.generateRandomKey(data, index, TRANS_CERT_KEY_LEN);
		
		for (int i = 0, j = index; i < certKey.length; i++, j++) {
			data[j] = certKey[i];
		}
		if(isOneTimeKey){
			superXor(data, index, TRANS_CERT_KEY_LEN, CertKey, true);
			superXor(data, index, TRANS_CERT_KEY_LEN, password, true);
		}else{
			xor(data, index, TRANS_CERT_KEY_LEN, password);
			
			//注意与普通数据加解密的次序相反
			if(userEncryptor != null){
				userEncryptor.encryptCertKey(data, index, TRANS_CERT_KEY_LEN);
			}
		}
	}
	
	/**
	 * 将新收到的加密后的CertKey，进行解密还原
	 * @param ts
	 * @param offset
	 * @param length
	 * @return
	 */
	public static void decodeFromTransCertKey(final byte[] ts, final int offset, final byte[] password, final byte[] storebs, boolean isOneTimeKey){
		final int index = TRANS_CERT_KEY_START_IDX + offset;
		
		if(isOneTimeKey){
			superXor(ts, index, TRANS_CERT_KEY_LEN, password, false);
			superXor(ts, index, TRANS_CERT_KEY_LEN, CertKey, false);
		}else{
			//注意与普通数据加解密的次序相反
			if(userEncryptor != null){
				userEncryptor.decryptCertKey(ts, index, TRANS_CERT_KEY_LEN);
			}
			xor(ts, index, TRANS_CERT_KEY_LEN, password);
		}
		for (int i = 0, j = index; i < storebs.length; i++, j++) {
			storebs[i] = ts[j];
		}
	}
	
	public static final int TRANS_CERT_KEY_START_IDX = 28;
	public static final int VERIFY_CERT_IDX = 26;
	//必须满足VERIFY_CERT_IDX + CERT_KEY_LEN <= TRANS_CERT_KEY_LEN;同时在发现最优MTU前，建议其小于140的最小UDP
	//参见：buildCheckCertKeyAndPwd
	public static final int TRANS_CERT_KEY_LEN = 99;
	
	public static boolean isSecondCertKeyError = false;
	public static byte[] SERVER_READY_TO_CHECK;
	
	//INI_CERTKEY已被停用，服务器和手机端初始生成随机证书。
	public static final String INI_CERTKEY = "*49AtBU7qtD:{&DJ7ey|&2S0.,Kjd4}9^1Y!id(12YUOSwR$d01u}[49AtBU7qtW";
	public static byte[] CertKey, OneTimeCertKey;
	
	public static IEncrypter userEncryptor = loadEncryptor();
	
	public static IEncrypter loadEncryptor(){
		String encryptClass = (String)IConstant.getInstance().getObject("encryptClass");
		if(encryptClass != null){
			try {
				Class c = Class.forName(encryptClass);
				IEncrypter en = (IEncrypter)c.newInstance();
				en.setUUID(IConstant.uuidBS);
				en.setPassword(IConstant.passwordBS);
				en.initEncrypter(!IConstant.serverSide);
				
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Enable user Encryptor [" + encryptClass + "]");
				
				return en;
			} catch (Exception e) {
				LogManager.err("Error Load Encryptor [" + encryptClass + "]");
				e.printStackTrace();
			}
		}else{
//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Disable user Encryptor");
		}
		return null;
	}
	
	public static void buildCheckCertKeyAndPwd(final byte[] data, final int offset, final byte[] pwd, final byte[] certKey, final boolean isEncode){
//		LogManager.logInTest("password : " + CUtil.toHexString(pwd));
//		LogManager.logInTest("certkeys : " + CUtil.toHexString(certKey));
//		final Random r = new Random(System.currentTimeMillis());
	
//		final int endIdx = CUtil.VERIFY_CERT_IDX + CCoreUtil.CERT_KEY_LEN;
		final int halfCertKeyLen = CCoreUtil.CERT_KEY_LEN / 2;
		final int firstHalfEndIdx = CUtil.VERIFY_CERT_IDX + halfCertKeyLen;
	
		byte[] onlyHalfCert = new byte[halfCertKeyLen];
//		LogManager.logInTest("isEncode:" + isEncode + ", superXOR offset : " + CUtil.VERIFY_CERT_IDX);
		if(isEncode){
			for (int i = 0; i < onlyHalfCert.length; i++) {
				onlyHalfCert[i] = certKey[i];
			}
			superXor(data, offset + CUtil.VERIFY_CERT_IDX, halfCertKeyLen, onlyHalfCert, isEncode);
			superXor(data, offset + CUtil.VERIFY_CERT_IDX, halfCertKeyLen, pwd, isEncode);
		}else{
			superXor(data, offset + CUtil.VERIFY_CERT_IDX, halfCertKeyLen, pwd, isEncode);
			for (int i = 0; i < onlyHalfCert.length; i++) {
				onlyHalfCert[i] = certKey[i];
			}
			superXor(data, offset + CUtil.VERIFY_CERT_IDX, halfCertKeyLen, onlyHalfCert, isEncode);
		}
		
		for (int i = 0; i < onlyHalfCert.length; i++) {
			onlyHalfCert[i] = certKey[i + halfCertKeyLen];
		}
		superXor(data, offset + firstHalfEndIdx, halfCertKeyLen, onlyHalfCert, isEncode);
		
		
//		final int randEndIdx = offset + endIdx;
//		for (int i = 0; i < randEndIdx; i++) {
//			int dataIdx = offset + i;
//			if(i >= (CUtil.VERIFY_CERT_IDX) && dataIdx < (endIdx)){
//				int checkIdx = i - CUtil.VERIFY_CERT_IDX;
//				if(i < (firstHalfEndIdx)){
//					//用密码和CertKey进行双处理
//					data[dataIdx] = (byte) (data[dataIdx] ^ pwd[checkIdx%pwdLen] ^ certKey[checkIdx]);
//				}else{
//					//仅用余下的CertKey进行单处理
//					data[dataIdx] = (byte) (data[dataIdx] ^ certKey[checkIdx]);
//				}
//			}else{
//				int g = r.nextInt();
//				data[dataIdx] = (byte) (g & 0xFF);
//			}
//		}
//		L.V = L.O ? false : LogManager.log("Str1 : " + str1);
//		L.V = L.O ? false : LogManager.log("Str2 : " + str2);
	}


	static{
		
		if(IConstant.serverSide == false){
			
			//客户端环境
			EventCenter.addListener(new IEventHCListener(){
				public boolean action(final byte[] bs) {
					//并将该随机数发送给客户机，客户机用同法处理后回转给服务器
					CUtil.resetCheck();
					CUtil.SERVER_READY_TO_CHECK = ContextManager.cloneDatagram(bs);
					
//					System.out.println("before : " + CUtil.toHexString(bs, MsgBuilder.INDEX_MSG_DATA + CUtil.VERIFY_CERT_IDX, CCoreUtil.CERT_KEY_LEN));
					buildCheckCertKeyAndPwd(bs, MsgBuilder.INDEX_MSG_DATA, IConstant.passwordBS, CUtil.CertKey, true);
//					System.out.println("after : " + CUtil.toHexString(bs, MsgBuilder.INDEX_MSG_DATA + CUtil.VERIFY_CERT_IDX, CCoreUtil.CERT_KEY_LEN));
					
					byte[] sendBS = new byte[MsgBuilder.UDP_BYTE_SIZE];
					System.arraycopy(bs, MsgBuilder.INDEX_MSG_DATA, sendBS, MsgBuilder.INDEX_MSG_DATA, CUtil.TRANS_CERT_KEY_LEN);
					
					ContextManager.getContextInstance().send(MsgBuilder.E_RANDOM_FOR_CHECK_CK_PWD, sendBS, CUtil.TRANS_CERT_KEY_LEN);
					
					hc.core.L.V=hc.core.L.O?false:LogManager.log("Sended processed random data to check certification key and password back to server");
					
					return true;
				}

				public byte getEventTag() {
					return MsgBuilder.E_RANDOM_FOR_CHECK_CK_PWD;
				}});			
			
			EventCenter.addListener(new IEventHCListener(){
				public boolean action(final byte[] bs) {
					String status = Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
//					System.out.println("Status:" + status);
					if(status.equals(String.valueOf(IContext.BIZ_SERVER_AFTER_PWD_ERROR))){
						ContextManager.getContextInstance().doExtBiz(IContext.BIZ_SERVER_AFTER_PWD_ERROR, null);
					}else if(status.equals(String.valueOf(IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR))){
						ContextManager.getContextInstance().doExtBiz(IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR, null);
					}else if(status.equals(String.valueOf(IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS))){
						LogManager.info("CertKey PWD are passed");
						ContextManager.getContextInstance().doExtBiz(IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS, null);
						CUtil.resetCheck();
					}else if(status.equals(String.valueOf(IContext.BIZ_SERVER_AFTER_SERVICE_IS_FULL))){
						LogManager.info("service is full");
						ContextManager.getContextInstance().doExtBiz(IContext.BIZ_SERVER_AFTER_SERVICE_IS_FULL, null);
					}else if(status.equals(String.valueOf(IContext.BIZ_SERVER_AFTER_OLD_MOBI_VER_STATUS))){
						ContextManager.getContextInstance().doExtBiz(IContext.BIZ_SERVER_AFTER_OLD_MOBI_VER_STATUS, null);
					}else{
						LogManager.info("account locked or unknown");
						ContextManager.getContextInstance().doExtBiz(IContext.BIZ_SERVER_AFTER_UNKNOW_STATUS, null);
					}
					return true;
				}

				public byte getEventTag() {
					return MsgBuilder.E_AFTER_CERT_STATUS;
				}});
			
			EventCenter.addListener(new IEventHCListener(){
				public boolean action(final byte[] bs) {
					
					if(ContextManager.cmStatus != ContextManager.STATUS_CLIENT_SELF){
						Boolean b = (Boolean)IConstant.getInstance().getObject(IConstant.IS_FORBID_UPDATE_CERT);
						if(b.booleanValue()){
							//MsgNotifier.getInstance().notifyNewMsg(ConfigForm.FORBID_UPDATE_CERT);
							LogManager.info("[system]:" + CCoreUtil.FORBID_UPDATE_CERT);
							LogManager.info("[system]: if you are first connecting(or had created new certification), please disable it in options.");
							
							//因为传送应答需要走加密，所以关闭如下应答
							HCURLUtil.sendCmdUnXOR(HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_CERT_RECEIVED, CCoreUtil.RECEIVE_CERT_FORBID);
							
							ContextManager.getContextInstance().doExtBiz(IContext.BIZ_FORBID_UPDATE_CERT, null);
							return true;
						}
					}
					CUtil.decodeFromTransCertKey(bs, MsgBuilder.INDEX_MSG_DATA, 
							IConstant.passwordBS, CUtil.CertKey, false);
//					LogManager.logInTest("receive cert : " + CUtil.toHexString(CUtil.CertKey));
					IConstant.getInstance().setObject(IConstant.CertKey, CertKey);
					
					if(ContextManager.cmStatus == ContextManager.STATUS_CLIENT_SELF){
						//仅保存新的证书，并通知新消息事件，HCCtrlGame会获得通知，并UI提示
//						MsgNotifier.getInstance().notifyNewMsg("OK, update new cert!");
						
						//证书已送达
						HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_CERT_RECEIVED, CCoreUtil.RECEIVE_CERT_OK);
						return true;
					}
					
					CertForbidManager.receiveUncheckCert();
					
					LogManager.info("trans new CertKey");
					
					sendCheck();

					HCURLUtil.sendCmdUnXOR(HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_CERT_RECEIVED, CCoreUtil.RECEIVE_CERT_OK);
					
					hc.core.L.V=hc.core.L.O?false:LogManager.log("check certification key and password back to server AFTER New Cert Key");

					return true;
				}

				public void sendCheck() {
					//重新发送密码
					buildCheckCertKeyAndPwd(CUtil.SERVER_READY_TO_CHECK, MsgBuilder.INDEX_MSG_DATA, IConstant.passwordBS, CUtil.CertKey, true);
					
					ContextManager.getContextInstance().send(MsgBuilder.E_RANDOM_FOR_CHECK_CK_PWD, 
							CUtil.SERVER_READY_TO_CHECK, CUtil.TRANS_CERT_KEY_LEN);
					
					//注意不能用resetCheck来进行回收，因为上行发送后会进行回收
					CUtil.SERVER_READY_TO_CHECK = null;
				}

				public byte getEventTag() {
					return MsgBuilder.E_TRANS_NEW_CERT_KEY;
				}});

		
			EventCenter.addListener(new IEventHCListener(){
				public boolean action(final byte[] bs) {
					LogManager.info(RootServerConnector.unObfuscate("og tnercpy tnotemi eeky"));
					
					OneTimeCertKey = new byte[CCoreUtil.CERT_KEY_LEN];
					CUtil.decodeFromTransCertKey(bs, MsgBuilder.INDEX_MSG_DATA, 
							IConstant.passwordBS, OneTimeCertKey, true);
//					L.V = L.O ? false : LogManager.log("OneTime : " + CUtil.toHexString(OneTimeCertKey));
					SIPManager.notifyCertPwdPassAtClient();
					return true;
				}

				public byte getEventTag() {
					return MsgBuilder.E_TRANS_ONE_TIME_CERT_KEY;
				}});

		}
	}

	public static String toHexString(byte[] bs){
		return toHexString(bs, 0, bs.length);
	}
	
	public static String toHexString(byte[] bs, final int offset, final int len){
		String str1 = "";
		final int endIdx = offset + len;
		for (int i = offset; i < endIdx; i++) {
			final String v = Integer.toHexString(0xFF & bs[i]).toUpperCase();
			str1 += ((((i - offset)%8)==0)?"   ":"") + ((((i - offset)%4)==0)?" ":"") + (v.length() == 1?"0" + v : v);
		}
		return str1;
	}
	
	public static void resetCheck() {
		CertForbidManager.reset();
		if(SERVER_READY_TO_CHECK != null){
			SERVER_READY_TO_CHECK = null;
			isSecondCertKeyError = false;
		}
	}

}
