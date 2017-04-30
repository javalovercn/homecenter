package hc.server.msb;

import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.MutableArray;
import hc.server.ui.CtrlResponse;
import hc.server.ui.Dialog;
import hc.server.ui.HTMLMlet;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.SimuMobile;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.SessionContext;
import hc.server.util.Assistant;
import hc.server.util.ai.AIObjectCache;
import hc.server.util.ai.AIPersistentManager;
import hc.server.util.ai.RobotEventData;
import hc.util.ResourceUtil;

import java.util.ArrayList;

/**
 * Robot is a intelligent control unit, which manage zero or multiple {@link Device}(s). 
 * <BR><BR>Through {@link Message}, robot communicates with devices. 
 * <BR><BR>When a user want to control or operate a {@link Robot} from {@link Mlet}, {@link HTMLMlet} or {@link CtrlResponse}, 
 * please invoke {@link ProjectContext#getRobot(String)} and {@link Robot#operate(long, Object)},
 * <BR><BR><STRONG>Important : </STRONG>you can not bypass {@link Robot} to operate {@link Device} directly from {@link Mlet}, {@link HTMLMlet} or {@link CtrlResponse}.
 * <BR><BR>
 * <UL>
 * <LI>The {@link Message#getDevID()} returns <i>Reference Device ID</i>(not real device ID), if the {@link Message} is received in {@link Robot} or dispatched from {@link Robot}.</LI>
 * <LI>The {@link Message#getDevID()} returns real device ID, if the {@link Message} is received in {@link Device} or dispatched from {@link Device}. </LI>
 * <LI>After active and apply HAR project, <i>Reference Device ID</i> of {@link Robot} in HAR project must refer to real device ID, we call it binding. If there is a <i>Reference Device ID</i> which is not binded, the binding dialog will be popped up before starting up HAR project.</LI>
 * <LI>a {@link Converter} is required if the {@link Message} between {@link Robot} and {@link Device} need to be converted and adapted.</LI>
 * <LI>A real device ID may be referenced by more than one <i>Reference Device ID</i>. For example, a temperature device initiative publish current temperature to all robots by {@link Device#dispatch(Message, boolean)}.</LI>
 * <LI>{@link Robot} is the only way to drive {@link Device}, no matter they are in same HAR project or not. To access {@link Robot} instance in {@link Mlet} or {@link CtrlResponse}, please invoke {@link ProjectContext#getRobot(String)} and {@link Robot#operate(long, Object)}. </LI>
 * <LI>{@link Message} is intermediary between {@link Robot} and {@link Device}, not intermediary between {@link Mlet}(or {@link CtrlResponse}) and {@link Device}. You can't get a instance of {@link Message} outside of {@link Robot}, {@link Converter} and {@link Device}.</LI>
 * </UL>
 * @see Device
 * @see Message
 */
public abstract class Robot extends Processor{
//	 * <BR>If a {@link Robot} is connecting to network via WiFi and manage no real {@link Device}, please create a {@link Device} for it, because <STRONG>No Device No WiFi-Connection</STRONG>.

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
	 * get a {@link Message} from recycling pool.
	 * <br><BR>The message will be converted by {@link Converter} or not depends on binding.
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
	 * @return the name of {@link Robot}
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
	 * operate the {@link Robot} to do some business from caller, 
	 * <BR><BR>
	 * for example, adjust the temperature to 28℃, then the <code>parameter</code> is integer object with value 28, and <code>functionID</code> may be 1.
	 * <br><br>the method is the only way to operate {@link Robot} to drive {@link Device} from UI, such as {@link CtrlResponse}, {@link HTMLMlet}/{@link Mlet}, {@link Dialog}, {@link Assistant}.
	 * <BR><BR>to get {@link Robot} instance, invoke {@link ProjectContext#getRobot(String)}.
	 * <br><br>
	 * <STRONG>Important</STRONG> : <BR>
	 * this method must be able to be executed in session level and project level.<BR>
	 * @param functionID
	 * @param parameter it can NOT be a {@link Message}. it is recommended to use the primitive types and their corresponding object wrapper classes and {@link AnalysableRobotParameter} to help server for {@link Assistant}.<BR>
	 * <BR>NOTE : <BR>
	 * the instance of {@link AnalysableRobotParameter} is <STRONG>NOT</STRONG> recyclable, because server will analyst it background.
	 * @return it can NOT be a {@link Message}. <BR>it is recommended to use the primitive types and their corresponding object wrapper classes and {@link AnalysableRobotParameter}.
	 * @see ProjectContext#isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public abstract Object operate(final long functionID, final Object parameter);
	
	/**
	 * get the {@link ProjectContext} instance of current project.
	 * @return 
	 * @since 7.0
	 */
	public ProjectContext getProjectContext(){
		return __context;
	}
	
	/**
	 * dispatch a {@link Message} to a device asynchronous.
	 * <br><br>to get instance of the <code>msg</code>, invoke {@link #getFreeMessage(String)}
	 * <BR><BR>Note : 
	 * <BR>the <code>msg</code> can't be dispatched more than one time.
	 * <BR>the <code>msg</code> will be recycled and clean after consumed, please do NOT keep any reference of it.
	 * <br><br>to print log of creating/converting/transferring/recycling of message, please enable [Option/Developer/log MSB message].
	 * @param msg the {@link Message} will be dispatched to device(s).
	 * @param isInitiative 
	 * <br>false : answer passively, passive mode is invoked generally in method {@link Robot#response(Message)} after {@link Message} is received.
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
	 * waiting for responding a {@link Message}.
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
	 * <BR><BR><STRONG>Note</STRONG> : it is allowed that a {@link Robot} without <i>Reference Device ID</i>.
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
	 * it is recyclable, please don't keep any reference of it in {@link Robot} or {@link RobotListener}.
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
	 * response {@link Message} <code>msg</code>, which is dispatched from {@link Device#dispatch(Message, boolean)} (may be converted by {@link Converter})
	 * @param msg dispatched from {@link Device} (or may be converted by {@link Converter})
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
	 * start up and initialize the {@link Robot}.
	 * <br><br>this method is invoked by server <STRONG>before</STRONG> {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}
	 * @since 7.0
	 */
	public abstract void startup();
	
	/**
	 * shutdown the {@link Robot}
	 * <br><br>this method is invoked by server <STRONG>after</STRONG> {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}
	 * @since 7.0
	 */
	public abstract void shutdown();
}
