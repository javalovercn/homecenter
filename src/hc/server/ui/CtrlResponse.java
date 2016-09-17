package hc.server.ui;

import hc.core.ContextManager;
import hc.core.MsgBuilder;
import hc.core.util.CtrlKey;
import hc.core.util.CtrlKeySet;
import hc.core.util.OutPortTranser;

/**
 * {@link CtrlResponse} is used for designing a remote controller.
 * <br>if you want design a powerful controller with complex UI, see {@link HTMLMlet} or {@link Mlet}
 * <p>for demo of a controller, please goto <a target="_blank" href="http://homecenter.mobi/en/pc/steps_ctrl.htm">http://homecenter.mobi/en/pc/steps_ctrl.htm</a>
 * @since 6.98
 */
public abstract class CtrlResponse {
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public CtrlResponse(){
		context = ProjectContext.getProjectContext();
		if(context != null){//生成模板代码时，产生null
			target = ServerUIAPIAgent.__getTargetFromInnerMethod(context);
		}
	}
	
	/**
	 * @deprecated
	 */
	@Deprecated
	String __hide_currentCtrlID;

	private String target;
	private final ProjectContext context;
	
	/**
	 * @return for example, controller://myctrl or cmd://playMusic
	 * @since 6.98
	 */
	public final String getTarget(){
		return target;
	}
	
	/**
	 * @return current project context
	 * @since 6.98
	 */
	public final ProjectContext getProjectContext(){
		return context;
	}
	
	/**
	 * @since 6.98
	 */
	public void onLoad(){
		
	}
	
	/**
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
	 * @param keyValue
	 * @param text
	 * @since 6.98
	 */
	public final void setButtonText(final int keyValue, final String text){
		if(text != null && text.length() > 0){
			ServerUIAPIAgent.runInSysThread(new Runnable() {
				@Override
				public void run() {
					ServerUIAPIAgent.__sendTextOfCtrlButton(keyValue, text);
				}
			});
		}
	}
	
	/**
	 * display a message moving from right to left on mobile <BR>
	 * <BR>
	 * Note : if mobile is in background ({@link ProjectContext#isMobileInBackground()}), a
	 * notification is also created for mobile.<BR><BR>
	 * if mobile option [Message, Notification to Speech also] is on, it may be spoken.<BR>
	 * the speech or not is depends on text, TTS engine, locale and mute of mobile.
	 * @param msg the message to show.
	 * @since 6.98
	 * @see {@link ProjectContext#sendMovingMsg(String)}
	 */
	public final void showTip(final String msg){
		getProjectContext().sendMovingMsg(msg);
	}
	
	/**
	 *  send key-value status of controller to mobile
	 * @param key
	 * @param value
	 * @since 6.98
	 */
	public final void sendStatus(final String key, final String value){
		sendStatus(key, value, false);
	}
	
	/**
	 *  send key-value status of controller to mobile
	 * @param key
	 * @param value
	 * @param isRTL true : is right to left
	 * @since 6.98
	 */
	public final void sendStatus(final String key, final String value, final boolean isRTL){
		final String[] keyArr = {key};
		final String[] vArr = {value};
		sendStatus(keyArr, vArr, isRTL);
	}
	
	/**
	 * send status group of controller to mobile side.
	 * @param keys
	 * @param values
	 * @since 6.98
	 */
	public final void sendStatus(final String[] keys, final String[] values){
		sendStatus(keys, values, false);
	}
	
	/**
	 * send status group of controller to mobile side.
	 * @param keys
	 * @param values
	 * @param isRTL
	 * @since 6.98
	 */
	public final void sendStatus(final String[] keys, final String[] values, final boolean isRTL){
		final OutPortTranser opt = new OutPortTranser();
		opt.out(__hide_currentCtrlID);
		opt.out(CtrlKeySet.SEND_STATUS);
		opt.out(keys);
		opt.out(values);
		opt.out(isRTL);
		
		ServerUIAPIAgent.runInSysThread(new Runnable() {
			@Override
			public void run() {
				ContextManager.getContextInstance().send(MsgBuilder.E_CTRL_STATUS, opt.getSubmitString());
			}
		});
		
	}
}
