package hc.core;

import hc.core.util.HCURL;
import hc.core.util.LogManager;

import java.io.UnsupportedEncodingException;

/**
 * 处理公共部分
 *
 */
public class ClientCmdExector {
	/**
	 * 如果已响应处理，则返回true；否则返回false
	 * @param coreSS
	 * @param url
	 * @return
	 */
	public static boolean process(final CoreSession coreSS, HCURL url) {
		IContext ctx = coreSS.context;
		
		if(url.protocal == HCURL.CMD_PROTOCAL){
			final String elementID = url.elementID;
			if(elementID.equals(HCURL.DATA_CMD_ALERT)){
				ctx.doExtBiz(IContext.BIZ_NEW_NOTIFICATION, url.getValueofPara("status"));
				return true;
			}else if(elementID.equals(HCURL.DATA_CMD_MSG)){
//				L.V = L.O ? false : LogManager.log("Receive Cmd Msg");

				final String caption = url.getValueofPara("caption");
				final String text = url.getValueofPara("text");
				
				//仅处理服务器端过来的，不能置于displayMessage之中，因为本地异常也可能displayMessage
				ctx.doExtBiz(IContext.BIZ_ASSISTANT_SPEAK, caption);
				ctx.doExtBiz(IContext.BIZ_ASSISTANT_SPEAK, text);
				
				ctx.displayMessage(
						caption, 
						text, 
						Integer.parseInt(url.getValueofPara("type")), 
						url.getValueofPara("image"), 
						Integer.parseInt(url.getValueofPara("timeOut")));
				
				return true;
			}else if(elementID.equals(HCURL.DATA_CMD_MOVING_MSG)){
				ctx.doExtBiz(IContext.BIZ_MOVING_SCREEN_TIP, url.getValueofPara("value"));
				return true;
			}else if(elementID.equals(HCURL.DATA_CMD_CTRL_BTN_TXT)){
				ctx.doExtBiz(IContext.BIZ_CTRL_BTN_TXT, url);
				return true;
			}
		}else if(url.protocal == HCURL.FORM_PROTOCAL){
//			if(url.elementID.equals("/form1")){}
		}else if(url.protocal == HCURL.MENU_PROTOCAL){
//			if(url.elementID.equals("/root")){}
		}else if(url.protocal == HCURL.CONTROLLER_PROTOCAL){
//			if(url.elementID.equals("/root")){}
		}
		return false;
	}

	public static String decodeURL(String s, String enc)
			throws UnsupportedEncodingException {

		boolean needToChange = false;
		int numChars = s.length();
		StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2
				: numChars);
		int i = 0;

		if (enc.length() == 0) {
			return s;
		}

		char c;
		byte[] bytes = null;
		while (i < numChars) {
			c = s.charAt(i);
			switch (c) {
			case '+':
				sb.append(' ');
				i++;
				needToChange = true;
				break;
			case '%':
				try {
					// (numChars-i)/3 is an upper bound for the number
					// of remaining bytes
					if (bytes == null)
						bytes = new byte[(numChars - i) / 3];
					int pos = 0;

					while (((i + 2) < numChars) && (c == '%')) {
						bytes[pos++] = (byte) Integer.parseInt(
								s.substring(i + 1, i + 3), 16);
						i += 3;
						if (i < numChars)
							c = s.charAt(i);
					}

					// A trailing, incomplete byte encoding such as
					// "%x" will cause an exception to be thrown

					if ((i < numChars) && (c == '%'))
						return s;

					sb.append(new String(bytes, 0, pos, enc));
				} catch (NumberFormatException e) {
					return s;
				}
				needToChange = true;
				break;
			default:
				sb.append(c);
				i++;
				break;
			}
		}

		return (needToChange ? sb.toString() : s);
	}

}
