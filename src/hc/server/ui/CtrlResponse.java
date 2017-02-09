package hc.server.ui;

import hc.core.MsgBuilder;
import hc.core.util.CtrlKey;
import hc.core.util.CtrlKeySet;
import hc.core.util.LangUtil;
import hc.core.util.OutPortTranser;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;
import hc.util.ResourceUtil;
import hc.util.ThreadConfig;

/**
 * {@link CtrlResponse} is used to design a remote controller.
 * <BR><BR>if you want design a powerful controller with complex UI, see {@link HTMLMlet} or {@link Mlet}
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
	private final ProjectContext context;
	private final J2SESession coreSS;
	private final boolean isRTL;
	/**
	 * for example, return <code>controller://myctrl</code>
	 * @return
	 * @since 6.98
	 */
	public final String getTarget(){
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
	public final String getElementID(){
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
	public final ProjectContext getProjectContext(){
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
	 * @param key see {@link CtrlKey}
	 * @since 6.98
	 */
	public abstract void click(int key);
	
	/**
	 * set initial text for some key.<BR><BR>
	 * this method is invoked by HomeCenter server to initialize the button text of controller.
	 * <BR>If you don't want to set button text, please don't override this method or return null(or empty string) for <code>keyValue</code> in the implementation.
	 * @param key see {@link CtrlKey}
	 * @return the initial button text of key
	 * @since 6.98
	 */
	public String getButtonInitText(final int key){
		return null;
	}
	
	/**
	 * notify controller of mobile to change text of a button.
	 * @param key
	 * @param text
	 * @since 6.98
	 */
	public final void setButtonText(final int key, final String text){
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
	public final void showTip(final String msg){
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
	public final void sendMovingMsg(final String msg){
		getProjectContext().sendMovingMsg(msg);
	}
	
	/**
	 *  send status of controller to mobile
	 * @param attribute
	 * @param status
	 * @since 6.98
	 */
	public final void sendStatus(final String attribute, final String status){
		sendStatus(attribute, status, isRTL);
	}
	
	/**
	 *  send status of controller to mobile
	 * @param attribute
	 * @param status
	 * @param isRTL true if is right to left
	 * @since 6.98
	 */
	public final void sendStatus(final String attribute, final String status, final boolean isRTL){
		final String[] keyArr = {attribute};
		final String[] vArr = {status};
		sendStatus(keyArr, vArr, isRTL);
	}
	
	/**
	 * send a group status of controller to mobile.
	 * @param attributes
	 * @param status
	 * @since 6.98
	 */
	public final void sendStatus(final String[] attributes, final String[] status){
		sendStatus(attributes, status, isRTL);
	}
	
	/**
	 * send a group status of controller to mobile.
	 * @param attributes
	 * @param status
	 * @param isRTL true if is right to left
	 * @since 6.98
	 */
	public final void sendStatus(final String[] attributes, final String[] status, final boolean isRTL){
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
		
	}
}
