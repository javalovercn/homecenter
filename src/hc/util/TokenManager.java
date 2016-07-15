package hc.util;

import hc.App;
import hc.core.IConstant;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.CCoreUtil;
import hc.server.AbstractDelayBiz;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class TokenManager {
	//含Donate的Token
	private static String token, relayToken;//, backToken;
	private static byte[] tokenBS, relayTokenBS;//, backTokenBS;
	
	static {
		token = PropertiesManager.getValue(PropertiesManager.p_Token);
		if(token == null || token.length() == 0){
			token = buildToken();
			PropertiesManager.setValue(PropertiesManager.p_Token, token);
			
			PropertiesManager.saveFile();
		}
		refreshTokenBS();
		
		if(relayToken == null || relayToken.length() == 0){
			relayToken = buildToken();
			PropertiesManager.setValue(PropertiesManager.p_TokenRelay, relayToken);
			
			PropertiesManager.saveFile();			
		}
		try {
			relayTokenBS = relayToken.getBytes(IConstant.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			relayTokenBS = relayToken.getBytes();
		}
	}

	private static void refreshTokenBS() {
		try {
			tokenBS = token.getBytes(IConstant.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			tokenBS = token.getBytes();
		}
	}
	
	public static void refreshToken(final String t){
		CCoreUtil.checkAccess();
		
		token = t;
		refreshTokenBS();
	}

	public static String buildToken() {
		return UUID.randomUUID().toString();
	}
	
	public static String getToken(){
		CCoreUtil.checkAccess();
		
		return token;//存储donateToken或Email绑定Token
	}
	
	public static byte[] getTokenBS(){
		CCoreUtil.checkAccess();
		
		return tokenBS;
	}
	
	public static String getRelayToken(){
		CCoreUtil.checkAccess();
		
		return relayToken;
	}
	
	public static byte[] getRelayTokenBS(){
		CCoreUtil.checkAccess();
		
		return relayTokenBS;
	}
	
	public static boolean isDonateToken(){
		CCoreUtil.checkAccess();
		
		return RootServerConnector.isRegedToken(IConstant.getUUID(), token);
	}
	
	public static void clearUPnPPort(){
		//因为增加维持难度，增加了系统的复杂度
//		PropertiesManager.setValue(PropertiesManager.p_Token, "");
//		PropertiesManager.setValue(PropertiesManager.p_TokenRelay, "");
		
		PropertiesManager.setValue(PropertiesManager.p_DirectUPnPExtPort, "0");
		PropertiesManager.setValue(PropertiesManager.p_RelayServerUPnPExtPort, "0");
		
		PropertiesManager.saveFile();
	}

	public static void changeTokenFromUI(final String id, final String token) {
		ConnectionManager.addBeforeConnectionBiz(new AbstractDelayBiz(null){
			@Override
			public final void doBiz() {
				PropertiesManager.setValue(PropertiesManager.p_uuid, id);
				PropertiesManager.setValue(PropertiesManager.p_Token, token);
				PropertiesManager.setValue(PropertiesManager.p_IsVerifiedEmail, IConstant.TRUE);
				
				PropertiesManager.saveFile();
	
				IConstant.setUUID(id);
				refreshToken(token);
	
				ResourceUtil.buildMenu();
	
				App.reloadEncrypt();
			}
		});
		
		//强制重连
		SIPManager.startRelineonForce(false);
	}
}
