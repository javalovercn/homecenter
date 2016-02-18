package hc.core.util;

import hc.core.FastSender;
import hc.core.HCMessage;
import hc.core.L;
import hc.core.MsgBuilder;

public class TCPSplitTester {
	private final static byte[] code = new byte[20];
	
	public static void printBigData(final byte[] bs){
		final int len = HCMessage.getBigMsgLen(bs);
		ByteUtil.encodeFileXOR(bs, MsgBuilder.INDEX_MSG_DATA, len, code, 0, code.length);
		L.V = L.O ? false : LogManager.log("----[Big Msg]-----data len : " + len + ", code base64 : " + ByteUtil.encodeBase64(code));
	}
	
	static int i = 0;
	public static void sendBigTest(final FastSender fastSender, final int lastSendLen){
		if(lastSendLen < 1024 * 2){
			return;
		}

		if(((i++) % 2) != 0){
			return;
		}
		
		final byte[] testData = new byte[firstLen[lenIdx++]];
		if(lenIdx + 1 == firstLen.length){
			lenIdx = 0;
		}
		final int len = testData.length;
		byte fchar = firstChar;
		for (int i = 0; i < len; i++) {
			testData[i] = fchar++;
			if(fchar == 'Z'){
				fchar = 'A';
			}
		}
		ByteUtil.encodeFileXOR(testData, 0, len, code, 0, code.length);
		L.V = L.O ? false : LogManager.log("----[Big Msg]-----data len : " + len + ", code base64 : " + ByteUtil.encodeBase64(code));
		fastSender.sendWrapAction(MsgBuilder.E_BIG_MSG_JS_TO_MOBILE, testData, 0, len);
		L.V = L.O ? false : LogManager.log("----[Big Msg]-----done send");
	}
	
	private static byte firstChar = 'A';
	private static int lenIdx = 0;
	private static int[] firstLen = {1<<22, 1<<22, 1<<23};
//	private static int[] firstLen = {1<<24, 1<<25, 1<<24};
}
