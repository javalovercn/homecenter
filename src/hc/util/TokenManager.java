package hc.util;

import hc.App;
import hc.core.IConstant;
import hc.core.RootServerConnector;
import hc.core.util.CCoreUtil;
import hc.server.AbstractDelayBiz;
import hc.server.ui.J2SESessionManager;

import java.io.UnsupportedEncodingException;

public class TokenManager {
	//含Donate的Token
	private static String token, relayToken;//, backToken;
	private static byte[] tokenBS, relayTokenBS;//, backTokenBS;
	
	static {
		token = PropertiesManager.getValue(PropertiesManager.p_Token);
		if(token == null || token.length() == 0){
			token = ResourceUtil.buildUUID();
			PropertiesManager.setValue(PropertiesManager.p_Token, token);
			
			PropertiesManager.saveFile();
		}
		refreshTokenBS();
		
		if(relayToken == null || relayToken.length() == 0){
			relayToken = ResourceUtil.buildUUID();
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

	public static void changeTokenFromUI(final String id, final String token, final boolean isVerified) {
		ConnectionManager.addBeforeConnectionBiz(new AbstractDelayBiz(null){
			@Override
			public final void doBiz() {
				//由于loginID更改是低概率事件，为了简化JRuby开发，restart HAR。参见ProjectContext.getLoginID
				final boolean isForceRestartHAR = (id.equals(IConstant.getUUID()) == false);
				
				//HAR stop前，不能更改uuid，所以要建Runnable
				final Runnable runAfterStop = new Runnable() {
					@Override
					public void run() {
						PropertiesManager.setValue(PropertiesManager.p_uuid, id);
						PropertiesManager.setValue(PropertiesManager.p_Token, token);
						if(isVerified){
							PropertiesManager.setValue(PropertiesManager.p_IsVerifiedEmail, IConstant.TRUE);
						}
						
						PropertiesManager.saveFile();
			
						IConstant.setUUID(id);
						refreshToken(token);
			
						ResourceUtil.buildMenu();
					}
				};
	
				App.reloadEncrypt(isForceRestartHAR, runAfterStop);
			}
		});
		
		//强制重连
		J2SESessionManager.stopAllSession(false, true, true);
	}
}
