package hc.server.ui;

import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.server.ScreenServer;
import hc.server.html5.syn.DiffManager;
import hc.server.html5.syn.DifferTodo;
import hc.server.html5.syn.JPanelDiff;
import hc.server.ui.design.ProjResponser;
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
 * <BR>for demo, please goto <a target="_blank" href="http://homecenter.mobi/en/pc/steps_mlet.htm">http://homecenter.mobi/en/pc/steps_mlet.htm</a>
 * <BR><BR>
 * if you want display JComponents to mobile in HTML (not snapshot) and set CSS for these JComponents, you can use {@link HTMLMlet}, which is extends {@link Mlet}.
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
	 * 
	 * @return {@link #STATUS_INIT}, {@link #STATUS_RUNNING}, {@link #STATUS_PAUSE}, {@link #STATUS_EXIT} or other.
	 */
	public final int getStatus(){
		return status;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public final void notifyStatusChanged(final int newStatus){//in user thread
		synchronized (this) {//要置于外部
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("change Mlet/HTMLMlet [" + this.toString() + "] from [" + status + "] to [" + newStatus + "].");
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
	 * @since 7.0
	 */
	public static final String URL_EXIT = HCURL.URL_CMD_EXIT;
	/**
	 * @since 7.0
	 */
	public static final String URL_SCREEN = HCURL.URL_HOME_SCREEN;
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public Mlet(){
		__context = ProjectContext.getProjectContext();
		if(__context != null){//测试用例时(TestMlet.java)，产生null
			__target = ServerUIAPIAgent.__getTargetFromInnerMethod(__context);
		}
	}
	
	private boolean isAutoReleaseAfterGo = false;
	
	/**
	 * when invoke {@link #go(String)}, this {@link Mlet} will be released by server or not.
	 * @return true : it will be released after leave and {@link #onExit()} will be executed by server before releasing;
	 * <BR>false : keep this {@link Mlet} and user will return/exit to this {@link Mlet}.
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
	private String __target;
	/**
	 * @deprecated
	 */
	@Deprecated
	ProjectContext __context;
	
	/**
	 * @return for example, screen://myMlet or cmd://playMusic
	 * @since 7.0
	 */
	public final String getTarget(){
		return __target;
	}
	
	/**
	 * @return current project context
	 * @since 7.0
	 */
	public final ProjectContext getProjectContext(){
		return __context;
	}
	
	private boolean isExited;
	
	/**
	 * you can go to resources as following:<BR>
	 * 1. open and show a screen ({@link #URL_SCREEN}), <BR>
	 * 1. open and show a form (form://myMlet, or form://myHtmlMlet), <BR>
	 * 2. open and show a controller (controller://myctrl), <BR>
	 * 3. run script commands ({@link #URL_EXIT}, cmd://myCmd)
	 * <BR><BR>
	 * <STRONG>Tip : </STRONG><BR>if you had jump to <i>form://myTwo</i> from <i>form://myOne</i>, 
	 * and ready jump to <i>form://myOne</i> from current <i>form://myTwo</i>.<BR>system will bring the target (form://myOne) to top level if it is opened.
	 * <BR><BR>
	 * <STRONG>Important : </STRONG><BR>to go to external URL (for example, http://homecenter.mobi), please invoke {@link #goExternalURL(String, boolean)}.
	 * @param url
	 * @see #goMlet(Mlet, String, boolean)
	 * @see #goExternalURL(String, boolean)
	 * @since 7.0
	 */
	public final void go(final String url){
		if(url.equals(URL_EXIT)){
			synchronized (this) {
		    	if(isExited){//cant go exit and press back key.
		    		return;
		    	}
		    	isExited = true;
	    	}
		}
		
		try {
//			不需要转码，可直接支持"screen://我的Mlet"
//			final String encodeURL = URLEncoder.encode(url, IConstant.UTF_8);
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_TRANSURL, url);
					return null;
				}
			});
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	/**
	 * go and open a {@link Mlet} or {@link HTMLMlet} (which is created by {@link ProjectContext#eval(String)} or other).
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>
	 * <BR>if the same <code>locator</code> or <code>form://locator</code> is opened, then it will be brought to font.
	 * <BR>for more, please see {@link #go(String)}.
	 * @param mlet
	 * @param mletLocator locator of {@link Mlet}. The prefix <code>form://</code> is <STRONG>NOT</STRONG> required.
	 * @param isAutoReleaseCurrentMlet true : if you are opening <code>mlet1</code> from <code>mlet0</code>, 
	 * the <code>mlet0</code> will be released and never return back. See {@link Mlet#setAutoReleaseAfterGo(boolean)}.
	 * @see ProjectContext#eval(String)
	 * @see #go(String)
	 * @since 7.7
	 */
	public final void goMlet(final Mlet mlet, final String mletLocator, final boolean isAutoReleaseCurrentMlet){
		if(mlet == null){
			throw new Error("Mlet is null when goMlet.");
		}
		
		if(mletLocator == null){
			throw new Error("Locator of Mlet is null when goMlet.");
		}
		
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				final Mlet currMlet = ScreenServer.getCurrMlet();
				if(currMlet != null){
					__context.runAndWait(new Runnable() {
						@Override
						public void run() {
							currMlet.setAutoReleaseAfterGo(isAutoReleaseCurrentMlet);
						}
					});
				}
				
				final String mletURL = (mletLocator.indexOf(HCURL.HTTP_SPLITTER) > 0)?mletLocator:HCURL.buildLocatorURL(mletLocator, mlet instanceof HTMLMlet);
				if(ProjResponser.bringMletToTop(__context, mletURL)){
					return null;
				}
				
				ProjResponser.openMlet(mletLocator, "title-" + mletLocator, __context, mlet);
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
	 * enter this {@link Mlet}, server will call this method.
	 * <BR><BR>
	 * calling {@link #getStatus()} in this method will returns {@link #STATUS_RUNNING}.
	 * <BR><BR>
	 * {@link #STATUS_INIT} is a status before {@link #STATUS_RUNNING}.
	 * @since 7.0
	 */
	@Override
	public void onStart() {
	}

	/**
	 * if user click a button to jump other screen, the server will call {@link #onPause()} method before enter next screen, see sample in HomeCenter server.
	 * <BR><BR>
	 * calling {@link #getStatus()} in this method will returns {@link #STATUS_PAUSE}.
	 * @see #onResume()
	 * @since 7.0
	 */
	@Override
	public void onPause() {
	}

	/**
	 * the server will call this method when exit from next screen, and re-enter this {@link Mlet}.
	 * <BR><BR>
	 * calling {@link #getStatus()} in this method will returns {@link #STATUS_RUNNING}.
	 * @see #onPause()
	 * @since 7.0
	 */
	@Override
	public void onResume() {
	}

	/**
	 * the server will call this method when exit this {@link Mlet}, and return/back.
	 * <BR><BR>
	 * calling {@link #getStatus()} in this method will returns {@link #STATUS_EXIT}.
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>if there is a running {@link Runnable} is started by this {@link Mlet} via {@link ProjectContext#run(Runnable)}, 
	 * it is a good practice that the running {@link Runnable} will check {@link #getStatus()} in loop or after {@link Thread#sleep(long)}.
	 * <BR>Please DON'T try to get <code>Thread</code> of it and call {@link Thread#stop()}.
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
	 * @param isUseExtBrowser true : use system web browser to open URL; false : the URL will be opened in HomeCenter client application.
	 * @since 7.7
	 */
	public final void goExternalURL(final String url, final boolean isUseExtBrowser) {
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
						ctx.runAndWait(new Runnable() {
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
		
		if(isForceExtBrowser || isUseExtBrowser || ProjResponser.isMletMobileEnv()){
			ServerUIAPIAgent.runInSysThread(new Runnable() {
				@Override
				public void run() {
					HCURLUtil.sendEClass(HCURLUtil.CLASS_GO_EXTERNAL_URL, url);
				}
			});
		}else{
			//TODO
		}
	}
}
