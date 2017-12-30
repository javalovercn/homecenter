package hc.server.ui;

import hc.core.MsgBuilder;
import hc.core.util.CtrlKey;
import hc.core.util.CtrlKeySet;
import hc.core.util.LangUtil;
import hc.core.util.OutPortTranser;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;
import hc.server.util.ai.AIObjectCache;
import hc.server.util.ai.AIPersistentManager;
import hc.server.util.ai.AnalysableData;
import hc.server.util.ai.FormData;
import hc.util.ResourceUtil;
import hc.util.ThreadConfig;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <code>CtrlResponse</code> is used to design a remote controller.
 * <BR><BR>if want to design a powerful controller with complex UI, see <code>HTMLMlet</code>.
 * @since 6.98
 */
public abstract class CtrlResponse {
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public CtrlResponse(){
		context = ProjectContext.getProjectContext();
		coreSS = SimuMobile.checkSimuProjectContext(context)?SimuMobile.SIMU_NULL:(ServerUIAPIAgent.getProjResponserMaybeNull(context).getSessionContextFromCurrThread().j2seSocketSession);
		target = (String)ThreadConfig.getValue(ThreadConfig.TARGET_URL, true);
		screenID = ServerUIAPIAgent.buildScreenID(context.getProjectID(), target);
		isRTL = LangUtil.isRTL(UserThreadResourceUtil.getMobileLocaleFrom(coreSS));
	}
	
	final String target, screenID;
	final ProjectContext context;
	private final J2SESession coreSS;
	private final boolean isRTL;
	/**
	 * for example, return <code>controller://myctrl</code>
	 * @return
	 * @since 6.98
	 */
	public String getTarget(){
		return target;//支持Test Scripts
	}
	
	/**
	 * the elementID of current CtrlResponse.
	 * <BR><BR>
	 * for example, current CtrlResponse is "controller://MyCtrler", then the elementID is "MyCtrler".
	 * <BR><BR>
	 * to get the instance of menu item for current CtrlResponse in designer, invoke {@link ProjectContext#getMenuItemBy(String, String)}.
	 * @return
	 * @see ProjectContext#getMenuItemBy(String, String)
	 * @since 7.20
	 */
	public String getElementID(){
		if(elementID == null){
			elementID = ResourceUtil.getElementIDFromTarget(target);//支持Test Scripts
		}
		return elementID;
	}
	
	private String elementID;

	/**
	 * return current project context
	 * @return
	 * @since 6.98
	 */
	public ProjectContext getProjectContext(){
		return context;
	}
	
	/**
	 * it is invoked by server when enter this controller.
	 * @since 6.98
	 */
	public void onLoad(){
		
	}
	
	/**
	 * it is invoked by server when exit this controller.
	 * @since 6.98
	 */
	public void onExit(){
		
	}

	/**
	 * it is invoked when user click some key of remote controller.
	 * @param keyValue see {@link CtrlKey}
	 * @since 6.98
	 */
	public abstract void click(int keyValue);
	
	/**
	 * set initial text for some key.<BR><BR>
	 * this method is invoked by HomeCenter server to initialize the button text of controller.
	 * <BR>If you don't want to set button text, please don't override this method or return null(or empty string) for <code>keyValue</code> in the implementation.
	 * @param keyValue see {@link CtrlKey}
	 * @return the initial button text of key
	 * @since 6.98
	 */
	public String getButtonInitText(final int keyValue){
		return null;
	}
	
	/**
	 * notify controller of mobile to change text of a button.
	 * @param key
	 * @param text
	 * @since 6.98
	 */
	public void setButtonText(final int key, final String text){
		if(coreSS == SimuMobile.SIMU_NULL){
			return;
		}
		
		if(text != null && text.length() > 0){
			ServerUIAPIAgent.runInSysThread(new Runnable() {
				@Override
				public void run() {
					ServerUIAPIAgent.__sendTextOfCtrlButton(coreSS, key, text);
				}
			});
			
			if(AIPersistentManager.isEnableHCAI()){
				final String locale = UserThreadResourceUtil.getMobileLocaleFrom(coreSS);
				AIPersistentManager.processCtrl(locale, target, text, context);
			}
		}
	}
	
	/**
	 * display a message moving from right to left on mobile 
	 * <BR><BR>
	 * Note : if mobile is in background ({@link ProjectContext#isMobileInBackground()}), a
	 * notification is also created for mobile.
	 * <BR><BR>
	 * if mobile option [Message, Notification to Speech also] is on, it may be spoken.
	 * <BR>
	 * the speech or not is depends on text, TTS engine, locale and mute of mobile.
	 * @param msg the message to show.
	 * @see ProjectContext#sendMovingMsg(String)
	 * @since 6.98
	 */
	public void showTip(final String msg){
		sendMovingMsg(msg);
	}
	
	/**
	 * send a message moving from right to left.<BR>
	 * <BR>
	 * Note : if mobile is in background ({@link ProjectContext#isMobileInBackground()}), a
	 * notification is also created for mobile.<BR><BR>
	 * if mobile option [Message, Notification to Speech also] is on, it may be spoken.<BR>
	 * the speech or not is depends on text, TTS engine, locale and mute of mobile.
	 * 
	 * @param msg the message to show.
	 * @see ProjectContext#sendMovingMsg(String)
	 * @since 7.30
	 */
	public void sendMovingMsg(final String msg){
		if(AIPersistentManager.isEnableAnalyseFlow && AIPersistentManager.isEnableHCAI()){
			final FormData data = AIObjectCache.getFormData();
			data.uiType = FormData.UI_TYPE_CTRLRESP;
			data.uiObject = this;
			data.movingMsg = msg;
			data.snap(context.getProjectID(), context.getClientLocale(), AnalysableData.DIRECT_OUT);
			AIPersistentManager.processFormData(data);
		}
		context.sendMovingMsg(msg);
	}
	
	/**
	 *  send status of controller to mobile
	 * @param attribute
	 * @param status
	 * @since 6.98
	 */
	public void sendStatus(final String attribute, final String status){
		sendStatus(attribute, status, isRTL);
	}
	
	/**
	 *  send status of controller to mobile
	 * @param attribute
	 * @param status
	 * @param isRTL true if is right to left
	 * @since 6.98
	 */
	public void sendStatus(final String attribute, final String status, final boolean isRTL){
		final String[] keyArr = {attribute};
		final String[] vArr = {status};
		sendStatus(keyArr, vArr, isRTL);
	}
	
	/**
	 * send a group status of controller to mobile.
	 * @param map
	 * @see #sendStatus(String[], String[])
	 * @since 7.46
	 */
	public void sendStatus(final Map map){
		sendStatus(map, isRTL);
	}
	
	/**
	 * send a group status of controller to mobile.
	 * @param map
	 * @param isRTL
	 * @see #sendStatus(String[], String[], boolean)
	 * @since 7.46
	 */
	public void sendStatus(Map map, final boolean isRTL){
		if(map instanceof SortedMap){
		}else{
			map = new TreeMap(map);
		}
		
		final int size = map.size();
		final String[] keys = new String[size];
		final String[] values = new String[size];
		
		int i = 0;
		for (final Object key : map.keySet()) {
			keys[i] = key.toString();
			values[i] = map.get(key).toString();
			i++;
		}
		
		sendStatus(keys, values, isRTL);
	}
	
	/**
	 * send a group status of controller to mobile.
	 * @param attributes
	 * @param status
	 * @see #sendStatus(Map)
	 * @since 6.98
	 */
	public void sendStatus(final String[] attributes, final String[] status){
		sendStatus(attributes, status, isRTL);
	}
	
	/**
	 * send a group status of controller to mobile.
	 * @param attributes
	 * @param status
	 * @param isRTL true if is right to left
	 * @see #sendStatus(Map, boolean)
	 * @since 6.98
	 */
	public void sendStatus(final String[] attributes, final String[] status, final boolean isRTL){
		if(coreSS == SimuMobile.SIMU_NULL){
			return;
		}
		
		final OutPortTranser opt = new OutPortTranser();
		opt.out(screenID);
		opt.out(CtrlKeySet.SEND_STATUS);
		opt.out(attributes);
		opt.out(status);
		opt.out(isRTL);
		
		ServerUIAPIAgent.runInSysThread(new Runnable() {
			@Override
			public void run() {
				coreSS.context.send(MsgBuilder.E_CTRL_STATUS, opt.getSubmitString());
			}
		});
		
		if(AIPersistentManager.isEnableHCAI()){
			final String locale = UserThreadResourceUtil.getMobileLocaleFrom(coreSS);
			AIPersistentManager.processCtrl(locale, target, attributes, context);
		}
	}
}
