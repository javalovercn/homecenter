package hc.server.ui;

import hc.App;
import hc.core.ContextManager;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.ReturnableRunnable;
import hc.core.util.Stack;
import hc.core.util.ThreadPool;
import hc.server.ScreenServer;
import hc.server.html5.syn.DifferTodo;
import hc.server.msb.Workbench;
import hc.server.ui.design.ProjResponser;
import hc.server.util.HCLimitSecurityManager;

import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.JToggleButton;

public class ServerUIAPIAgent {
	public final static String CONVERT_NAME_PROP = Workbench.SYS_RESERVED_KEYS_START + "convert_name_prop";
	public final static String DEVICE_NAME_PROP = Workbench.SYS_RESERVED_KEYS_START + "device_name_prop";
	public final static String ROBOT_NAME_PROP = Workbench.SYS_RESERVED_KEYS_START + "robot_name_prop";
	
	public final static String ATTRIBUTE_IS_TRANSED_MLET_BODY = Workbench.SYS_RESERVED_KEYS_START + "mlet_html_body";
	
	final static Object threadToken = App.getThreadPoolToken();
	
	public final static String ATTRIBUTE_PEND_CACHE = "pend_cache_att";
	private final static HashMap<Integer, QuestionParameter> questionMap = new HashMap<Integer, QuestionParameter>();
	private static int questionID = 1;
	
	public static QuestionParameter buildQuestionID(final ProjectContext ctx, final Runnable yesRunnable, final Runnable noRunnable, final Runnable cancelRunnable){
		final QuestionParameter qp = new QuestionParameter();
		qp.ctx = ctx;
		qp.yesRunnable = yesRunnable;
		qp.noRunnable = noRunnable;
		qp.cancelRunnable = cancelRunnable;
		
		synchronized (questionMap) {
			qp.questionID = questionID++;
			questionMap.put(qp.questionID, qp);
			if(questionID == Integer.MAX_VALUE){
				questionID = 1;
			}
		}
		
		return qp;
	}
	
	
	public final static void setProjectContext(final Mlet mlet, final ProjectContext ctx){
		mlet.__context = ctx;
	}
	
	public static void resetQuestion(){
		synchronized (questionMap) {
			questionMap.clear();
		}
	}
	
	public static void execQuestionResult(final String ques_id, final String result){
		try{
			final int int_id = Integer.parseInt(ques_id);
			QuestionParameter qp;
			synchronized (questionMap) {
				qp = questionMap.remove(int_id);
			}
			if(qp != null){
				final ProjectContext ctx = qp.ctx;
				if(result.equals("yes")){
					if(qp.yesRunnable != null){
						ctx.run(qp.yesRunnable);
					}
				}else if(result.equals("no")){
					if(qp.noRunnable != null){
						ctx.run(qp.noRunnable);
					}
				}else if(result.equals("cancel")){
					if(qp.cancelRunnable != null){
						ctx.run(qp.cancelRunnable);
					}
				}
			}
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public final static String getMobileUID(){
		return ClientDesc.getAgent().getUID();
	}
	
	public final static void setHCSysProperties(final ProjectContext ctx, final String key, final String value){
		ctx.__setPropertySuper(key, value);
	}
	
	public final static String getHCSysProperties(final ProjectContext ctx, final String key){
		return ctx.__getPropertySuper(key);
	}
	
	public final static void loadStyles(final HTMLMlet mlet) {
		if(mlet.stylesToDeliver != null){
			final int count = mlet.stylesToDeliver.size();
			for (int i = 0; i < count; i++) {
				final String styles = mlet.stylesToDeliver.elementAt(i);
				mlet.loadCSS(styles);
			}
			mlet.stylesToDeliver.clear();
		}
	}
	
	public final static void flushCSS(final HTMLMlet mlet, final DifferTodo diffTodo) {//in user thread
		if(mlet.styleItemToDeliver != null){
			final int count = mlet.styleItemToDeliver.size();
			for (int i = 0; i < count; i++) {
				final StyleItem item = mlet.styleItemToDeliver.elementAt(i);
				if(item.forType == StyleItem.FOR_DIV){
					mlet.setCSSForDiv(item.component, item.className, item.styles);//in user thread
				}else if(item.forType == StyleItem.FOR_JCOMPONENT){
					mlet.setCSS(item.component, item.className, item.styles);//in user thread
				}else if(item.forType == StyleItem.FOR_JTOGGLEBUTTON){
					mlet.setCSSForToggle((JToggleButton)item.component, item.className, item.styles);//in user thread
				}
			}
			mlet.styleItemToDeliver.clear();
		}
		
		//由于与MletSnapCanvas共用，所以增加null检查
		if(diffTodo != null){
			diffTodo.notifyInitDone();
		}
	}
	
	public final static Object getMobileAttribute(final Object key){
		return ClientDesc.sys_attribute.get(key);
	}
	
	public final static void setMobileAttribute(final String key, final Object value){
		ClientDesc.sys_attribute.put(key, value);
	}
	
	public final static boolean isOnTopHistory(final ProjectContext ctx, final String url){
		synchronized(ctx){
			if(ctx.mletHistoryUrl != null){
				final int size = ctx.mletHistoryUrl.size();
				if(size > 0){
					return ctx.mletHistoryUrl.elementAt(size - 1).equals(url);
				}
			}
			return false;
		}
	}
	
	public final static boolean pushMletURLToHistory(final ProjectContext ctx, final String url) {
		synchronized(ctx){
			if(ctx.mletHistoryUrl == null){
				ctx.mletHistoryUrl = new Stack();
			}
			
			final int size = ctx.mletHistoryUrl.size();
			if(size > 0){
				final int idx = ctx.mletHistoryUrl.search(url);
				if(idx >= 0){
					ctx.mletHistoryUrl.removeAt(idx);//从队列中删除
					ctx.mletHistoryUrl.push(url);//置于顶
					
					ScreenServer.pushToTopForMlet(url);
					
					return false;
				}
			}
	
	//		System.out.println("----------pushMletURLToHistory : " + url);
			ctx.mletHistoryUrl.push(url);
			return true;
		}
	}
	
	public final static void popMletURLHistory(final ProjectContext ctx){
		synchronized(ctx){
			if(ctx.mletHistoryUrl != null){
				final String url = (String)ctx.mletHistoryUrl.pop();
	//			if(url != null){
	//				System.out.println("popMletURLHistory : " + url);
	//			}
			}
		}
	}
	
	public static final ThreadPool getThreadPoolFromProjectContext(final ProjectContext ctx){
		return ctx.threadPool;
	}
	
	public static final void set__projResponserMaybeNull(final ProjectContext ctx, final ProjResponser resp){
		ctx.__projResponserMaybeNull = resp;
	}
	
	public static ProjResponser get__projResponserMaybeNull(final ProjectContext ctx){
		return ctx.__projResponserMaybeNull;
	}
	
	public static final String __getTargetFromInnerMethod(final ProjectContext ctx){
		return ctx.__tmp_url;
	}
	
	public static final void setDiffTodo(final HTMLMlet mlet, final DifferTodo diff){
		mlet.diffTodo = diff;
	}
	
	public static final String __getTargetElementIDFromInnerMethod(final ProjectContext ctx){
		return ctx.__tmp_elementID;
	}
	
	public static final void setTMPTarget(final ProjectContext ctx, final String url, final String elementID){
		ctx.__tmp_url = url;
		ctx.__tmp_elementID = elementID;
	}
	
	public static final Enumeration getSystemEventListener(final ProjectContext ctx){
		synchronized (ctx.systemEventStack) {
			//返回一个Vector新实例的
			return ctx.systemEventStack.elements();
		}
	}
	
	public static void setCurrentCtrlID(final CtrlResponse respon, final String url){
		respon.__hide_currentCtrlID = url;
	}

	/**
	 * @deprecated
	 * @param keyValue
	 * @param text
	 */
	@Deprecated
	public final static void __sendTextOfCtrlButton(final int keyValue, final String text){
		if(ServerUIAPIAgent.isToMobile()){
			final String[] keys = {"key", "text"};
			final String[] values = {String.valueOf(keyValue), text};
			HCURLUtil.sendCmd(HCURL.DATA_CMD_CTRL_BTN_TXT, keys, values);
		}
	}

	final static boolean isToMobile() {
		return ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF;
	}
	
	final static ProjectContext staticContext = new ProjectContext("static", "1.0", 
			HCLimitSecurityManager.getTempLimitThreadPool(), null, (ProjClassLoaderFinder)null);

	/**
	 * @deprecated
	 * @param caption
	 * @param text
	 * @param type
	 * @param image
	 * @param timeOut
	 */
	@Deprecated
	public final static void __sendStaticMessage(final String caption, final String text, final int type, final BufferedImage image, final int timeOut){
		staticContext.sendMessage(caption, text, type, image, timeOut);
	}

	/**
	 * @deprecated
	 * @param msg
	 */
	@Deprecated
	public final static void tipStaticOnTray(final String msg){
		staticContext.tipOnTray(msg);
	}

	/**
	 * @deprecated
	 * @param msg
	 */
	@Deprecated
	public final static void __sendStaticMovingMsg(final String msg) {
		staticContext.sendMovingMsg(msg);
	}

	public final static void setSuperProp(final ProjectContext ctx, final String propName, final String value){
		ctx.__setPropertySuper(propName, value);
	}

	public final static void removeSuperProp(final ProjectContext ctx, final String propName){
		ctx.__removePropertySuper(propName);
	}

	public final static String getSuperProp(final ProjectContext ctx, final String propName){
		return ctx.__getPropertySuper(propName);
	}

	public final static void setSuperAttribute(final ProjectContext ctx, final String attributeName, final Object value){
		ctx.__setAttributeSuper(attributeName, value);
	}

	public final static void removeSuperAttribute(final ProjectContext ctx, final String attributeName){
		ctx.__removeAttributeSuper(attributeName);
	}

	public final static Object getSuperAttribute(final ProjectContext ctx, final String attributeName){
		return ctx.__getAttributeSuper(attributeName);
	}

	public static String getProcessorNameFromCtx(final ProjectContext ctx, String name, final String prop) {
		if(name != null && name.length() > 0){
		}else{
			name = getSuperProp(ctx, prop);
			removeSuperProp(ctx, prop);
		}
		if(name == null){
			name = "";
		}
		return name;
	}


	static final Object runAndWaitInSysThread(final ReturnableRunnable returnRun){
		return ContextManager.getThreadPool().runAndWait(returnRun, threadToken);
	}


	static final void runInSysThread(final Runnable run){
		ContextManager.getThreadPool().run(run, threadToken);
	}
}
