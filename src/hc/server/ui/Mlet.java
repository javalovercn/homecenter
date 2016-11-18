package hc.server.ui;

import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.server.html5.syn.DiffManager;
import hc.server.html5.syn.DifferTodo;
import hc.server.html5.syn.JPanelDiff;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.SessionContext;
import hc.server.util.HCLimitSecurityManager;
import hc.util.ResourceUtil;
import hc.util.ThreadConfig;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Permission;

import javax.swing.JPanel;

/**
 * {@link Mlet} is an instance running in HomeCenter server, and the <STRONG>snapshot</STRONG> of {@link Mlet} is presented on mobile.
 * It looks more like a form, dialog, control panel or game canvas running in mobile.
 * <BR><BR>
 * {@link Mlet} is extends {@link javax.swing.JPanel JPanel}, so you can bring all JPanel features to mobile UI, no matter whether your mobile is Android, iPhone or other.
 * <BR><BR>
 * To present JComponents to mobile in HTML (not snapshot) and set CSS for these JComponents, please use {@link HTMLMlet}, which is extends {@link Mlet}.
 * @see HTMLMlet
 * @since 7.0
 */
public class Mlet extends JPanel implements ICanvas {
	private static final long serialVersionUID = 7;
	
	/**
	 * construct this instance.
	 */
	public static final int STATUS_INIT = 0;
	
	/**
	 * when {@link #onStart()} or {@link #onResume()}, {@link Mlet} is changed to this status.
	 */
	public static final int STATUS_RUNNING = 1;
	
	/**
	 * when {@link #onPause()}, {@link Mlet} is changed to this status.
	 */
	public static final int STATUS_PAUSE = 2;
	
	/**
	 * when {@link #onExit()}, {@link Mlet} is changed to this status.
	 */
	public static final int STATUS_EXIT = 3;
	
	int status = STATUS_INIT;
	
	/**
	 * return {@link #STATUS_INIT}, {@link #STATUS_RUNNING}, {@link #STATUS_PAUSE}, {@link #STATUS_EXIT} or other.
	 * @return 
	 */
	public final int getStatus(){
		return status;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	final void notifyStatusChanged(final int newStatus){//in user thread
		synchronized (this) {//要置于外部
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("change Mlet/HTMLMlet [" + __target + "] from [" + status + "] to [" + newStatus + "].");
			}
			
			status = newStatus;
			
			if(this instanceof HTMLMlet){
				final HTMLMlet htmlMlet = (HTMLMlet)this;
				final DifferTodo diffTodo = htmlMlet.diffTodo;
				if(newStatus == STATUS_RUNNING && diffTodo != null){
					if(htmlMlet.isFlushCSS){
						return;
					}else{
						htmlMlet.isFlushCSS = true;
					}
					
					//有可能为MletSnapCanvas模式调用本方法
					ServerUIAPIAgent.loadStyles(htmlMlet);
					
					JPanelDiff.addContainerEvent(this, diffTodo);//in user thread
					
					//in user thread
					DiffManager.getDiff(JPanelDiff.class).diff(0, this, diffTodo);//必须置于onStart之前，因为要初始手机端对象结构树
					
					ServerUIAPIAgent.flushCSS(htmlMlet, diffTodo);

					ServerUIAPIAgent.loadJS(htmlMlet);//加载JSPanel的动作脚本
				}
			}
		}
	}

	/**
	 * exit current {@link Mlet} or {@link HTMLMlet} and return back.
	 * @since 7.0
	 * @see #go(String)
	 */
	public static final String URL_EXIT = HCURL.URL_CMD_EXIT;
	
	/**
	 * enter the desktop screen of server.
	 * @since 7.0
	 * @see #go(String)
	 */
	public static final String URL_SCREEN = HCURL.URL_HOME_SCREEN;
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public Mlet(){
		__context = ProjectContext.getProjectContext();
		
		if(SimuMobile.checkSimuProjectContext(__context)){
			coreSS = SimuMobile.SIMU_NULL;
		}else{
			final ProjResponser resp = __context.__projResponserMaybeNull;
			SessionContext sc;
			if(resp != null && (sc = resp.getSessionContextFromCurrThread()) != null){
				coreSS = sc.j2seSocketSession;
			}else{
				if(L.isInWorkshop){
					LogManager.errToLog("[workshop] fail to get J2SESocketSession, use a instance in designer.");
				}
				coreSS = SimuMobile.SIMU_NULL;
			}
		}
		__target = (String)ThreadConfig.getValue(ThreadConfig.TARGET_URL);
	}
	
	private boolean isAutoReleaseAfterGo = false;
	
	/**
	 * when invoke {@link #go(String)}, this {@link Mlet} will be released by server or not.
	 * @return true : it will be released after leave and {@link #onExit()} will be executed by server before releasing;
	 * <BR>false : keep this {@link Mlet} alive and user will return back to this {@link Mlet}.
	 * <BR><BR>default is false.
	 * @see #setAutoReleaseAfterGo(boolean)
	 * @since 7.7
	 */
	public final boolean isAutoReleaseAfterGo(){
		return isAutoReleaseAfterGo;
	}
	
	/**
	 * when invoke {@link #go(String)}, this {@link Mlet} will be released by server or not.
	 * <BR><BR>
	 * default is false.
	 * <BR><BR>
	 * <STRONG>Tip : </STRONG>it is no effect when the URL is script command (for example, cmd://myCmd).
	 * @param isAutoRelease true : it will be released after leave and {@link #onExit()} will be executed by server before releasing;
	 * <BR>false : keep this {@link Mlet} alive and user will return/back to this {@link Mlet}.
	 * @see #isAutoReleaseAfterGo()
	 * @see #goMlet(Mlet, String, boolean)
	 * @since 7.7
	 */
	public final void setAutoReleaseAfterGo(final boolean isAutoRelease){
		isAutoReleaseAfterGo = isAutoRelease;
	}
	
	/**
	 * @deprecated
	 */
	@Deprecated 
	String __target;

	/**
	 * @deprecated
	 */
	@Deprecated
	ProjectContext __context;
	
	/**
	 * @deprecated
	 */
	@Deprecated
	final J2SESession coreSS;
	
	/**
	 * for example, form://myMlet.<BR>
	 * 
	 * @return 
	 * @since 7.0
	 */
	public final String getTarget(){
		return __target;//支持Test Scripts
	}
	
	/**
	 * the elementID of current Mlet.
	 * <BR><BR>
	 * for example, current Mlet is "form://MyMlet", then the elementID is "MyMlet".
	 * <BR><BR>
	 * to get the instance of menu item for current Mlet in designer, invoke {@link ProjectContext#getMenuItemBy(String, String)}.
	 * @return
	 * @see ProjectContext#getMenuItemBy(String, String)
	 * @since 7.20
	 */
	public final String getElementID(){
		if(elementID == null){
			elementID = ResourceUtil.getElementIDFromTarget(__target);//支持Test Scripts
		}
		return elementID;
	}
	
	private String elementID;

	/**
	 * return current project context
	 * @return 
	 * @since 7.0
	 */
	public final ProjectContext getProjectContext(){
		return __context;
	}
	
	private boolean isExited;
	
	/**
	 * it is equals with {@link #go(String)}.
	 * @param scheme one of {@link MenuItem#CMD_SCHEME}, 
	 * {@link MenuItem#CONTROLLER_SCHEME}, {@link MenuItem#FORM_SCHEME} or {@link MenuItem#SCREEN_SCHEME}.
	 * @param elementID for example, run scripts of menu item "cmd://myCommand", the scheme is {@linkplain MenuItem#CMD_SCHEME}, and element ID is "myCommand",
	 */
	public final void go(final String scheme, final String elementID){
		if(coreSS == SimuMobile.SIMU_NULL){
			return;
		}
		
		final String target = HCURL.buildStandardURL(scheme, elementID);
		go(target);
	}
	
	/**
	 * jump mobile to following targets:<BR>
	 * 1. <i>{@link #URL_SCREEN}</i> : enter the desktop screen of server from mobile, <BR>
	 * 2. <i>form://myMlet</i> : open and show a form, <BR>
	 * 3. <i>controller://myctrl</i> : open and show a controller, <BR>
	 * 4. <i>{@link #URL_EXIT}</i> : exit current Mlet,<BR>
	 * 5. <i>cmd://myCmd</i> : run script commands only,
	 * <BR><BR>
	 * bring to top : <BR>
	 * 1. jump to <i>form://B</i> from <i>form://A</i>, <BR>
	 * 2. ready jump to <i>form://A</i> again from <i>form://B</i>.<BR>
	 * 3. system will bring the target (form://A) to top if it is opened.
	 * <BR><BR>
	 * <STRONG>Note</STRONG> :<BR>
	 * go to external URL (for example, http://homecenter.mobi), invoke {@link #goExternalURL(String, boolean)}.
	 * @param url
	 * @see #go(String, String)
	 * @see #goMlet(Mlet, String, boolean)
	 * @see #setAutoReleaseAfterGo(boolean)
	 * @see #goExternalURL(String, boolean)
	 * @since 7.0
	 */
	public final void go(final String url){
		if(coreSS == SimuMobile.SIMU_NULL){
			return;
		}
		
		if(url.equals(URL_EXIT)){
			synchronized (this) {
		    	if(isExited){//cant go exit and press back key.
		    		return;
		    	}
		    	isExited = true;
	    	}
		}
		
		HCURL.checkSuperCmd(url);
		
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log("====>go : " + url);
		}
		try {
//			不需要转码，可直接支持"screen://我的Mlet"
//			final String encodeURL = URLEncoder.encode(url, IConstant.UTF_8);
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_TRANSURL, url);
					return null;
				}
			});
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * go and open a {@link Mlet} or {@link HTMLMlet} (which is probably created by {@link ProjectContext#eval(String)}).
	 * <BR><BR>
	 * the target of <i>toMlet</i> will be set as <i>targetOfMlet</i>.<BR><BR>
	 * <STRONG>Important : </STRONG>
	 * <BR>if the same name <i>target</i> or <i>form://target</i> is opened, then it will be brought to top.
	 * <BR>for more, see {@link #go(String)}.
	 * @param toMlet
	 * @param targetOfMlet target of {@link Mlet}. The prefix <i>form://</i> is <STRONG>NOT</STRONG> required.
	 * @param isAutoReleaseCurrentMlet true means the called Mlet will be released after go successfully.<BR>for more, see {@link Mlet#setAutoReleaseAfterGo(boolean)}.
	 * @see ProjectContext#eval(String)
	 * @see #go(String)
	 * @since 7.7
	 */
	public final void goMlet(final Mlet toMlet, final String targetOfMlet, final boolean isAutoReleaseCurrentMlet){
		if(toMlet == null){
			throw new Error("Mlet is null when goMlet.");
		}
		
		if(targetOfMlet == null){
			throw new Error("target of Mlet is null when goMlet.");
		}
		
		if(coreSS == SimuMobile.SIMU_NULL){
			return;
		}
		
		final Mlet fromMlet = this;//不用ScreenServer.getCurrMlet(coreSS);
		
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				ServerUIAPIAgent.openMlet(coreSS, __context, toMlet, targetOfMlet, isAutoReleaseCurrentMlet,
						fromMlet);
				return null;
			}
		});
	}
	
	/**
	 * resize a BufferedImage to the target size.
	 * @param src
	 * @param to_width
	 * @param to_height
	 * @return the resized image.
	 */
	public final static BufferedImage resizeImage(final BufferedImage src, final int to_width, final int to_height){
		return ResourceUtil.resizeImage(src, to_width, to_height);//注意：不能进入runAndWaitInSysThread
	}
	
	
	/**
	 * enter this {@link Mlet}, server will invoke this method.
	 * <BR><BR>
	 * invoke {@link #getStatus()} in this method will returns {@link #STATUS_RUNNING}.
	 * <BR><BR>
	 * {@link #STATUS_INIT} is a status before {@link #STATUS_RUNNING}.
	 * @since 7.0
	 */
	@Override
	public void onStart() {
	}

	/**
	 * when jump to other form/screen, the server will call {@link #onPause()} method before enter next form/screen.
	 * <BR><BR>
	 * invoke {@link #getStatus()} in this method will returns {@link #STATUS_PAUSE}.
	 * @see #onResume()
	 * @since 7.0
	 */
	@Override
	public void onPause() {
	}

	/**
	 * the server will invoke this method when exit from next form/screen, and enter this {@link Mlet} again.
	 * <BR><BR>
	 * invoke {@link #getStatus()} in this method will returns {@link #STATUS_RUNNING}.
	 * @see #onPause()
	 * @since 7.0
	 */
	@Override
	public void onResume() {
	}

	/**
	 * the server will invoke this method when exit this {@link Mlet}.
	 * <BR><BR>
	 * invoke {@link #getStatus()} in this method will returns {@link #STATUS_EXIT}.
	 * <BR><BR>
	 * <STRONG>Important : </STRONG><BR>
	 * if there is a running {@link Runnable} is started by this {@link Mlet} via {@link ProjectContext#run(Runnable)}, 
	 * it is a good practice that the running {@link Runnable} check {@link #getStatus()} in loop and finish task when exit.
	 * <BR>Please DON'T to get <code>Thread</code> of it and invoke {@link Thread#stop()}.
	 * @since 7.0
	 */
	@Override
	public void onExit() {
	}

	/**
	 * go to external URL (for example, http://homecenter.mobi).
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>
	 * <BR>socket/connect permissions is required even if the domain of external URL is the same with the domain of upgrade HAR project URL.
	 * <BR><BR>
	 * <STRONG>Warning : </STRONG>
	 * <BR>the external URL may be sniffed when in moving (exclude HTTPS).
	 * <BR>In iOS (not Android), when go external URL and leave application, the application will be released (in background). In future, it maybe keep alive in background.
	 * @param url
	 * @param isUseExtBrowser true : use system web browser to open URL; false : the URL will be opened in client application.
	 * @since 7.7
	 */
	public final void goExternalURL(final String url, final boolean isUseExtBrowser) {
		if(coreSS == SimuMobile.SIMU_NULL){
			return;
		}
		
		if(url.endsWith(StringUtil.JAD_EXT)){
			throw new Error("external URL can NOT end with : " + StringUtil.JAD_EXT);
		}
		
		if(url.startsWith(StringUtil.URL_EXTERNAL_PREFIX) == false){
			throw new Error("external URL must start with : " + StringUtil.URL_EXTERNAL_PREFIX);
		}
		
		//检查权限
		if(HCLimitSecurityManager.isSecurityManagerOn()){
			final Object[] exception = new Object[1];
			try{
				final HttpURLConnection urlConn = new HttpURLConnection(new URL(url)) {
					@Override
					public void connect() throws IOException {
					}
					
					@Override
					public boolean usingProxy() {
						return false;
					}
					
					@Override
					public void disconnect() {
					}
				};
				final Permission perm = urlConn.getPermission();
				final ProjectContext ctx = getProjectContext();
				ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() {
						final HCLimitSecurityManager manager = HCLimitSecurityManager.getHCSecurityManager();
						ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(ctx), new Runnable() {
							@Override
							public void run() {
								try{
									ThreadConfig.putValue(ThreadConfig.AUTO_PUSH_EXCEPTION, false);//关闭push exception
									manager.checkPermission(perm);
								}catch (final Throwable e) {
									exception[0] = e;
								}finally{
									ThreadConfig.putValue(ThreadConfig.AUTO_PUSH_EXCEPTION, true);
								}
							}
						});
						return null;
					}
				});
			}catch (final Throwable e) {
				throw new Error("invalid external URL : " + url);
			}
			if(exception[0] != null){
				throw new SecurityException(exception[0].toString());
			}
		}
		
		final boolean isForceExtBrowser = true;
		
		if(isForceExtBrowser && isUseExtBrowser == false){
			LogManager.warning("function [goExternalURL] use inner browser is NOT implemented now, force use external web browser.");
		}
		
		if(isForceExtBrowser || isUseExtBrowser || ProjResponser.isMletMobileEnv(coreSS)){
			ServerUIAPIAgent.runInSysThread(new Runnable() {
				@Override
				public void run() {
					HCURLUtil.sendEClass(coreSS, HCURLUtil.CLASS_GO_EXTERNAL_URL, url);
				}
			});
		}else{
			//TODO
		}
	}

}
