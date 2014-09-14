package hc.server.ui;

import hc.core.ContextManager;
import hc.core.MsgBuilder;
import hc.core.util.CtrlKey;
import hc.core.util.OutPortTranser;

/**
 * a controller implementation in HomeCenter server
 * <br>if you want design a powerful controller with complex UI, see {@link hc.server.ui.Mlet Mlet}
 * <p>for demo of a controller, please goto <a target="_blank" href="http://homecenter.mobi/en/pc/steps_ctrl.htm">http://homecenter.mobi/en/pc/steps_ctrl.htm</a>
 */
public abstract class CtrlResponse {
	public CtrlResponse(){
		__context = ProjectContext.getProjectContext();
		if(__context != null){//生成模板代码时，产生null
			__target = __context.__getTargetFromInnerMethod();
		}
	}
	
	/**
	 * @deprecated
	 */
	public String __hide_currentCtrlID;

	/**
	 * @deprecated
	 */
	private String __target;
	/**
	 * @deprecated
	 */
	private ProjectContext __context;
	
	/**
	 * @return for example, controller://myctrl or cmd://playMusic
	 */
	public String getTarget(){
		return __target;
	}
	
	/**
	 * @return current project context
	 */
	public ProjectContext getProjectContext(){
		return __context;
	}
	
	public void onLoad(){
		
	}
	
	public void onExit(){
		
	}

	public abstract void click(int keyValue);
	
	/**
	 * this method will be called by HomeCenter server to initialize the button text of controller. If you don't want to set button text, please don't override this method in the implementation.
	 * @param keyValue
	 * @return the initial button text of key
	 */
	public String getButtonInitText(int keyValue){
		return null;
	}
	
	/**
	 * notify controller of mobile to change text of a button.
	 * @param keyValue
	 * @param text
	 */
	public void setButtonText(int keyValue, String text){
		if(text != null && text.length() > 0){
			__context.__sendTextOfCtrlButton(keyValue, text);
		}
	}
	
	/**
	 * show moving tip message on mobile.
	 * @param msg
	 */
	public void showTip(String msg){
		__context.sendStaticMovingMsg(msg);
	}
	
	/**
	 *  send key-value status of controller on mobile
	 * @param key
	 * @param value
	 */
	public void sendStatus(String key, String value){
		sendStatus(key, value, false);
	}
	
	/**
	 *  send key-value status of controller on mobile
	 * @param key
	 * @param value
	 * @param isRTL true : is right to left
	 */
	public void sendStatus(String key, String value, boolean isRTL){
		String[] keyArr = {key};
		String[] vArr = {value};
		sendStatus(keyArr, vArr, isRTL);
	}
	
	/**
	 * send status group of controller to mobile side.
	 * @param keys
	 * @param values
	 */
	public void sendStatus(String[] keys, String[] values){
		sendStatus(keys, values, false);
	}
	
	/**
	 * send status group of controller to mobile side.
	 * @param keys
	 * @param values
	 * @param isRTL
	 */
	public void sendStatus(String[] keys, String[] values, boolean isRTL){
		OutPortTranser opt = new OutPortTranser();
		opt.out(__hide_currentCtrlID);
		opt.out(CtrlKey.SEND_STATUS);
		opt.out(keys);
		opt.out(values);
		opt.out(isRTL);
		
		ContextManager.getContextInstance().send(MsgBuilder.E_JCIP_FORM_REFRESH, opt.getSubmitString());
	}
}
