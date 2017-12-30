package hc.server.msb;

import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.MutableArray;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.SimuMobile;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.SessionContext;
import hc.server.util.ai.AIObjectCache;
import hc.server.util.ai.AIPersistentManager;
import hc.server.util.ai.RobotEventData;
import hc.util.ResourceUtil;

import java.util.ArrayList;

/**
 * <code>Robot</code> is a intelligent control unit, which manage zero or multiple <code>Device</code>(s). 
 * <BR><BR>Through <code>Message</code>, robot communicates with devices. 
 * <BR><BR>When a user want to control or operate a <code>Robot</code> from <code>Mlet</code>, <code>HTMLMlet</code> or <code>CtrlResponse</code>, 
 * please invoke {@link ProjectContext#getRobot(String)} and {@link Robot#operate(long, Object)},
 * <BR><BR><STRONG>Important : </STRONG>you can not bypass <code>Robot</code> to operate <code>Device</code> directly from <code>Mlet</code>, <code>HTMLMlet</code> or <code>CtrlResponse</code>.
 * <BR><BR>
 * <UL>
 * <LI>The {@link Message#getDevID()} returns <i>Reference Device ID</i>(not real device ID), if the <code>Message</code> is received in <code>Robot</code> or dispatched from <code>Robot</code>.</LI>
 * <LI>The {@link Message#getDevID()} returns real device ID, if the <code>Message</code> is received in <code>Device</code> or dispatched from <code>Device</code>. </LI>
 * <LI>After active and apply HAR project, <i>Reference Device ID</i> of <code>Robot</code> in HAR project must refer to real device ID, we call it binding. If there is a <i>Reference Device ID</i> which is not binded, the binding dialog will be popped up before starting up HAR project.</LI>
 * <LI>a <code>Converter</code> is required if the <code>Message</code> between <code>Robot</code> and <code>Device</code> need to be converted and adapted.</LI>
 * <LI>A real device ID may be referenced by more than one <i>Reference Device ID</i>. For example, a temperature device initiative publish current temperature to all robots by {@link Device#dispatch(Message, boolean)}.</LI>
 * <LI><code>Robot</code> is the only way to drive <code>Device</code>, no matter they are in same HAR project or not. To access <code>Robot</code> instance in <code>Mlet</code> or <code>CtrlResponse</code>, please invoke {@link ProjectContext#getRobot(String)} and {@link Robot#operate(long, Object)}. </LI>
 * <LI><code>Message</code> is intermediary between <code>Robot</code> and <code>Device</code>, not intermediary between <code>Mlet</code>(or <code>CtrlResponse</code>) and <code>Device</code>. You can't get a instance of <code>Message</code> outside of <code>Robot</code>, <code>Converter</code> and <code>Device</code>.</LI>
 * </UL>
 * @see Device
 * @see Message
 */
public abstract class Robot extends Processor{
//	 * <BR>If a <code>Robot</code> is connecting to network via WiFi and manage no real <code>Device</code>, please create a <code>Device</code> for it, because <STRONG>No Device No WiFi-Connection</STRONG>.

	final ProjResponser resp;
	RobotWrapper robotWrapper;
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public Robot(){
		super("", Workbench.TYPE_ROBOT_PROC);
		if(this instanceof RobotWrapper){
		}else{
			robotWrapper = new RobotWrapper(this);
		}
		resp = ServerUIAPIAgent.getProjResponserMaybeNull(__context);//getProjectContext()
	}
	
	/**
	 * get a <code>Message</code> from recycling pool.
	 * <br><BR>The message will be converted by <code>Converter</code> or not depends on binding.
	 * @param ref_dev_id the <i>Reference Device ID</i> which the message is dispatched to. 
	 * @return 
	 */
	protected Message getFreeMessage(final String ref_dev_id) {//不能去掉，因为生成API时，由于没有附加生成Processor
		return super.getFreeMessageInProc(ref_dev_id);
	}
	
	final void setName(final String name){
		super.name = name;
	}
	
	/**
	 * @return the name of <code>Robot</code>
	 * @since 7.0
	 */
	final String getName(){
		return name;
	}
	
	String nameLower;
	
	final String getNameLower(){
		if(nameLower == null){
			nameLower = getName().toLowerCase();
		}
		return nameLower;
	}
	
	/**
	 * operate the <code>Robot</code> to do some business from caller, 
	 * <BR><BR>
	 * for example, adjust the temperature to 28℃, then the <code>parameter</code> is integer object with value 28, and <code>functionID</code> may be 1.
	 * <br><br>the method is the only way to operate <code>Robot</code> to drive <code>Device</code> from UI, for example <code>CtrlResponse</code>, <code>HTMLMlet</code>/<code>Mlet</code>, <code>Dialog</code>, <code>Assistant</code>.
	 * <BR><BR>to get <code>Robot</code> instance, invoke {@link ProjectContext#getRobot(String)}.
	 * <br><br>
	 * <STRONG>Important</STRONG> : <BR>
	 * the implementation of this method must be able to be executed in session level and project level.<BR>
	 * @param functionID
	 * @param parameter it can NOT be a <code>Message</code>. it is recommended to use the primitive types and their corresponding object wrapper classes and <code>AnalysableRobotParameter</code> to help server for <code>Assistant</code>.<BR>
	 * <BR>NOTE : <BR>
	 * the instance of <code>AnalysableRobotParameter</code> is <STRONG>NOT</STRONG> recyclable, because server will analyst it background.
	 * @return it can NOT be a <code>Message</code>. <BR>it is recommended to use the primitive types and their corresponding object wrapper classes and <code>AnalysableRobotParameter</code>.
	 * @see ProjectContext#isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public abstract Object operate(final long functionID, final Object parameter);
	
	/**
	 * get the <code>ProjectContext</code> instance of current project.
	 * @return 
	 * @since 7.0
	 */
	public ProjectContext getProjectContext(){
		return __context;
	}
	
	/**
	 * dispatch a <code>Message</code> to a device asynchronous.
	 * <br><br>to get instance of the <code>msg</code>, invoke {@link #getFreeMessage(String)}
	 * <BR><BR>Note : 
	 * <BR>the <code>msg</code> can't be dispatched more than one time.
	 * <BR>the <code>msg</code> will be recycled and clean after consumed, please do NOT keep any reference of it.
	 * <br><br>to print log of creating/converting/transferring/recycling of message, please enable [Option/Developer/log MSB message].
	 * @param msg the <code>Message</code> will be dispatched to device(s).
	 * @param isInitiative 
	 * <br>false : answer passively, passive mode is invoked generally in method {@link Robot#response(Message)} after <code>Message</code> is received.
	 * <br>true : dispatch initiative, initiative mode is invoked generally in method {@link #operate(long, Object)}.
	 * @see #waitFor(Message, long)
	 * @see Device#dispatch(Message, boolean)
	 * @since 7.0
	 */
	protected void dispatch(final Message msg, final boolean isInitiative){
		msg.ctrl_bind_id = DeviceBindInfo.buildStandardBindID(project_id, name, msg.ctrl_dev_id);
		msg.ctrl_isInitiative = isInitiative;
		
		if(isInitiative){
			//置于dispatch之后，由于多线程，可能导致已回收
			workbench.V = workbench.O ? false : workbench.log("{" + project_id + "/" + name + "} dispatch initiatively : " + msg.toString());
			workbench.dispatch(msg, true, procType);
		}else{
			msg.ctrl_bind_id = preMsg.ctrl_bind_id;
			workbench.V = workbench.O ? false : workbench.log("{" + project_id + "/" + name + "} dispatch passively : " + msg.toString());
			super.__response(msg, true);
		}
	}
	
	/**
	 * waiting for responding a <code>Message</code>.
	 * <BR><BR>
	 * it is synchronous.
	 * <br><br>to print log of creating/converting/transferring/recycling of message, please enable [Option/Developer/log MSB message].
	 * @param msg the request Message.
	 * @param timeout the maximum time to wait in milliseconds. Zero means never time out.
	 * @return null if it is not processed or time is out.
	 * @see #dispatch(Message, boolean)
	 * @since 7.0
	 */
	protected Message waitFor(final Message msg, final long timeout){
		msg.ctrl_bind_id = DeviceBindInfo.buildStandardBindID(project_id, name, msg.ctrl_dev_id);
		return workbench.waitFor(msg, timeout, true, procType, this);
	}
	
	/**
	 * all the <i>Reference Device ID</i> names those are used in this robot, each one of them will correspond to a real device ID after binding.
	 * <BR><BR><STRONG>Note</STRONG> : it is allowed that a <code>Robot</code> without <i>Reference Device ID</i>.
	 * @return all the <i>Reference Device ID</i> names those are used in this robot.
	 * @see #getDeviceCompatibleDescription(String)
	 * @since 7.0
	 */
	public abstract String[] declareReferenceDeviceID();
	
	/**
	 * return an instance of {@link DeviceCompatibleDescription} about device compatible of <code>referenceDeviceID</code>.
	 * <BR><BR>the description is used for auto-binding to real devices.
	 * @param referenceDeviceID
	 * @return 
	 * @see #declareReferenceDeviceID()
	 * @see Converter#getUpDeviceCompatibleDescription()
	 * @see Device#getDeviceCompatibleDescription()
	 * @since 7.0
	 */
	public abstract DeviceCompatibleDescription getDeviceCompatibleDescription(final String referenceDeviceID);
	
	/**
	 * return the description of Robot.
	 * @return 
	 * @since 7.3
	 */
	@Override
	public String getIoTDesc(){
		return this.classSimpleName + super.getIoTDesc();
	}
	
//	@Override
//	public String toString() {//please use getIoTDesc
//		return this.getClass().getSimpleName() + super.getProcDesc();
//	}
	
	ArrayList<RobotListener> projectLevelRobotListeners;
	
	/**
	 * add a listener to receive {@link RobotEvent}.
	 * <BR><BR>
	 * if a <code>RobotListener</code> is added in project level, it will be executed in project level.<BR>
	 * if a <code>RobotListener</code> is added in session level, it will be executed in session level, and it will gone automatically after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}.
	 * <BR><BR>
	 * it is thread safe.
	 * @param listener
	 * @see #removeRobotListener(RobotListener)
	 * @see #dispatchRobotEvent(RobotEvent)
	 * @see ProjectContext#isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public void addRobotListener(final RobotListener listener){
		if(listener == null){
			return;
		}
		
		final ProjectContext projectContext = getProjectContext();
		if(SimuMobile.checkSimuProjectContext(projectContext)){
			return;
		}
		
		final SessionContext sessionContext = resp.getSessionContextFromCurrThread();
		if(sessionContext == null || sessionContext.j2seSocketSession == null){
			if(L.isInWorkshop){
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if(isLoggerOn == false){
				ServerUIAPIAgent.printInProjectLevelWarn("robot", "addRobotListener");
			}
			synchronized (this) {
				if(projectLevelRobotListeners == null){
					projectLevelRobotListeners = new ArrayList<RobotListener>(4);
				}
				projectLevelRobotListeners.add(listener);
			}
		}else{
			sessionContext.j2seSocketSession.addRobotListener(this, listener);
		}
	}
	
	/**
	 * remove a listener.
	 * <BR><BR>
	 * it is thread safe.
	 * <BR><BR>
	 * if a <code>RobotListener</code> is added in project level, it can be removed in session level.<BR>
	 * if a <code>RobotListener</code> is added in session level, it will gone automatically after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}, or it should be removed in session level.
	 * @param listener
	 * @return true means successful removed.
	 * @see #addRobotListener(RobotListener)
	 * @see #dispatchRobotEvent(RobotEvent)
	 * @see ProjectContext#isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public boolean removeRobotListener(final RobotListener listener){
		final ProjectContext projectContext = getProjectContext();
		if(SimuMobile.checkSimuProjectContext(projectContext)){
			return true;
		}
		
		final SessionContext sessionContext = resp.getSessionContextFromCurrThread();
		if(sessionContext == null || sessionContext.j2seSocketSession == null){
			if(L.isInWorkshop){
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if(isLoggerOn == false){
				ServerUIAPIAgent.printInProjectLevelWarn("robot", "removeRobotListener");
			}
			return removeProjectLevelRobotListener(listener);
		}else{
			final boolean isInSession = sessionContext.j2seSocketSession.removeRobotListener(this, listener);
			if(isInSession){
				return true;
			}else{
				return removeProjectLevelRobotListener(listener);
			}
		}
	}

	private final boolean removeProjectLevelRobotListener(final RobotListener listener) {
		synchronized (this) {
			if(projectLevelRobotListeners != null){
				return projectLevelRobotListeners.remove(listener);
			}
		}
		return false;
	}
	
	/**
	 * build a {@link RobotEvent} instance.
	 * <BR><BR>
	 * <STRONG>Note :</STRONG><BR>
	 * it is recyclable, please don't keep any reference of it in <code>Robot</code> or {@link RobotListener}.
	 * @param propertyName the property name of event.
	 * @param oldValue the old value of property of current event. Maybe null.
	 * @param newValue the new value of property of current event. Maybe null.
	 * @return 
	 * @see #dispatchRobotEvent(RobotEvent)
	 * @see #addRobotListener(RobotListener)
	 * @see #removeRobotListener(RobotListener)
	 * @since 7.0
	 */
	protected RobotEvent buildRobotEvent(final String propertyName, final Object oldValue, final Object newValue){
		final RobotEvent re = RobotEventPool.instance.getFreeRobotEvent();
		re.sourceWrapper = robotWrapper;
		
		re.propertyName = propertyName;
		re.oldValue = oldValue;
		re.newValue = newValue;
		
		return re;
	}
	
	/**
	 * dispatch an event to all {@link RobotListener} no matter it is in session level or in project level.
	 * <BR><BR>it is synchronized,
	 * <BR><BR>
	 * to get new instance of {@link RobotEvent}, please invoke {@link #buildRobotEvent(String, Object, Object)}.
	 * <BR><BR>
	 * <STRONG>Important : </STRONG><BR>the <code>event</code> will be recycled and clean after consumed, and you will get it again from {@link #buildRobotEvent(String, Object, Object)}.
	 * please do NOT keep any reference of it.
	 * <BR>
	 * @param event 
	 * @see #addRobotListener(RobotListener)
	 * @see #removeRobotListener(RobotListener)
	 * @see #buildRobotEvent(String, Object, Object)
	 * @since 7.0
	 */
	protected void dispatchRobotEvent(final RobotEvent event) {
		if(event == null){
			return;
		}
		
		if(projectLevelRobotListeners != null){
			try{
				final int size = projectLevelRobotListeners.size();
				for (int i = 0; i < size; i++) {
					final RobotListener robotListener = projectLevelRobotListeners.get(i);
					robotListener.action(event);//处于project level
				}
			}catch (final IndexOutOfBoundsException outOfBound) {
				//越界或不存在或已删除
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		
		J2SESessionManager.dispatchRobotEventSynchronized(resp, this, event, sessionArrayForEventDispatch);
		
		if(AIPersistentManager.isEnableAnalyseFlow && AIPersistentManager.isEnableHCAI()
				&& ResourceUtil.isAnalysableParameter(event.newValue)
				&& ResourceUtil.isAnalysableParameter(event.oldValue)){
			final RobotEventData eventData = AIObjectCache.getRobotEventData();
			eventData.newValue = event.newValue;
			eventData.oldValue = event.oldValue;
			eventData.robotWrapper = event.sourceWrapper;
			AIPersistentManager.processRobotEventAndRecycle(eventData);
		}

		MSBAgent.recycle(event);
	}
	
	final MutableArray sessionArrayForEventDispatch = new MutableArray(0);

	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	final void __startup(){
		startup();//in user thread group
	}
	
	/**
	 * response <code>Message</code> <code>msg</code>, which is dispatched from {@link Device#dispatch(Message, boolean)} (may be converted by <code>Converter</code>)
	 * @param msg dispatched from <code>Device</code> (or may be converted by <code>Converter</code>)
	 */
	@Override
	public abstract void response(Message msg);
	
	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	final void __shutdown(){
		shutdown();//in user thread group
	}
	
	/**
	 * start up and initialize the <code>Robot</code>.
	 * <br><br>this method is invoked by server <STRONG>before</STRONG> {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}
	 * @since 7.0
	 */
	public abstract void startup();
	
	/**
	 * shutdown the <code>Robot</code>
	 * <br><br>this method is invoked by server <STRONG>after</STRONG> {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}
	 * @since 7.0
	 */
	public abstract void shutdown();
}
