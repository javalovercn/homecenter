package hc.server.msb;

import hc.core.util.ExceptionReporter;
import hc.server.ui.CtrlResponse;
import hc.server.ui.HTMLMlet;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;

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

	/**
	 * @deprecated
	 */
	@Deprecated
	public Robot(){
		super("", Workbench.TYPE_ROBOT_PROC);
	}
	
	/**
	 * get a free {@link Message} from recycling pool.
	 * @param ref_dev_id the <i>Reference Device ID</i> that the message is dispatched <STRONG>to</STRONG>. 
	 * <br>The message may be converted by {@link Converter} or not depends on binding (after active current project).
	 * @return a free {@link Message}
	 */
	protected final Message getFreeMessage(final String ref_dev_id) {//不能去掉，因为生成API时，由于没有附加生成Processor
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
	
	/**
	 * operate the {@link Robot} to do some business, for example, adjust the temperature to 28℃, then the <code>parameter</code> is integer object with value 28, and <code>functionID</code> may be customized to be 1.
	 * <br><br>the method is the only way to operate {@link Robot} to drive {@link Device} for {@link CtrlResponse} and {@link Mlet}, to get {@link Robot} instance, call {@link ProjectContext#getRobot(String)}
	 * <br>
	 * @param functionID
	 * @param parameter any object except {@link Message}. Because a instance of {@link Message} can't be outside of {@link Robot}, {@link Converter}, or {@link Device}.
	 * @return any object except {@link Message}
	 * @since 7.0
	 */
	public abstract Object operate(final long functionID, final Object parameter);
	
	/**
	 * get the {@link ProjectContext} instance of current project.
	 * @return the {@link ProjectContext} instance of current project.
	 * @since 7.0
	 */
	public final ProjectContext getProjectContext(){
		return __context;
	}
	
	/**
	 * dispatch a {@link Message} to a device asynchronous.
	 * <br><br><strong>the <code>msg</code> can't be dispatched more than one time.</strong>
	 * <br><br>to print log of creating/converting/transferring/recycling of message, please enable [Option/Developer/log MSB message].
	 * @param msg the {@link Message} will be dispatched to device(s). To get instance of the <code>msg</code>, call {@link #getFreeMessage(String)}.
	 * @param isInitiative 
	 * <br>false : answer passively, passive mode is invoked generally in method {@link Robot#response(Message)} after {@link Message} is received.
	 * <br>true : dispatch initiative, initiative mode is invoked generally in method {@link #operate(long, Object)}.
	 * @see #waitFor(Message, long)
	 * @see Device#dispatch(Message, boolean)
	 * @since 7.0
	 */
	protected final void dispatch(final Message msg, final boolean isInitiative){
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
	 * process a {@link Message} synchronous and waiting for response.
	 * <br><br>to print log of creating/converting/transferring/recycling of message, please enable [Option/Developer/log MSB message].
	 * @param msg the object is got through {@link #getFreeMessage(String)}, and called by this method only one time, in other words, the instance of {@link Message} can't be as parameter more than one time.
	 * @param timeout the maximum time to wait in milliseconds. Zero means never time out.
	 * @return null if it is not processed or the time is out.
	 * @see #dispatch(Message, boolean)
	 * @since 7.0
	 */
	protected final Message waitFor(final Message msg, final long timeout){
		msg.ctrl_bind_id = DeviceBindInfo.buildStandardBindID(project_id, name, msg.ctrl_dev_id);
		return workbench.waitFor(msg, timeout, true, procType, this);
	}
	
	/**
	 * all the <i>Reference Device ID</i> names those are used in this robot, each one of them will correspond to a real device ID after binding.
	 * <BR><BR><STRONG>Important</STRONG> : A {@link Robot} without <i>Reference Device ID</i> is allowed.
	 * @return all the <i>Reference Device ID</i> names those are used in this robot.
	 * @see #getDeviceCompatibleDescription(String)
	 * @since 7.0
	 */
	public abstract String[] declareReferenceDeviceID();
	
	/**
	 * return an instance of {@link DeviceCompatibleDescription} about device compatible of <code>referenceDeviceID</code>.
	 * <BR>these description is useful for binding to real devices.
	 * @param referenceDeviceID
	 * @return an instance of {@link DeviceCompatibleDescription} about <code>referenceDeviceID</code>.
	 * @see #declareReferenceDeviceID()
	 * @since 7.0
	 */
	public abstract DeviceCompatibleDescription getDeviceCompatibleDescription(final String referenceDeviceID);
	
	/**
	 * @return the description of Robot.
	 * @since 7.3
	 */
	@Override
	public final String getIoTDesc(){
		return this.classSimpleName + super.getIoTDesc();
	}
	
//	@Override
//	public String toString() {//please use getIoTDesc
//		return this.getClass().getSimpleName() + super.getProcDesc();
//	}
	
	ArrayList<RobotListener> robotListeners;
	
	/**
	 * add a listener to receive {@link RobotEvent} about status of {@link Robot}.
	 * <BR>
	 * it is thread safe.
	 * @param listener
	 * @see #removeRobotListener(RobotListener)
	 * @see #dispatchRobotEvent(RobotEvent)
	 * @since 7.0
	 */
	public final void addRobotListener(final RobotListener listener){
		if(listener == null){
			return;
		}
		
		synchronized (this) {
			if(robotListeners == null){
				robotListeners = new ArrayList<RobotListener>(4);
			}
			robotListeners.add(listener);
		}
	}
	
	/**
	 * remove a listener.
	 * <BR>
	 * it is thread safe.
	 * @param listener
	 * @see #addRobotListener(RobotListener)
	 * @see #dispatchRobotEvent(RobotEvent)
	 * @since 7.0
	 */
	public final void removeRobotListener(final RobotListener listener){
		synchronized (this) {
			if(robotListeners != null){
				robotListeners.remove(listener);
			}
		}
	}
	
	/**
	 * build a {@link RobotEvent} instance.
	 * @param propertyName the property name of event.
	 * @param oldValue the old value of property of current event. Maybe null.
	 * @param newValue the new value of property of current event. Maybe null.
	 * @return a {@link RobotEvent} instance which {@link RobotEvent#getSource()} is self. If there are multiple {@link Robot}(s) in current project, {@link RobotEvent#getSource()} is used to distinguish between different sources.
	 * @see #dispatchRobotEvent(RobotEvent)
	 * @see #addRobotListener(RobotListener)
	 * @see #removeRobotListener(RobotListener)
	 * @since 7.0
	 */
	protected final RobotEvent buildRobotEvent(final String propertyName, final Object oldValue, final Object newValue){
		final RobotEvent re = RobotEventPool.instance.getFreeRobotEvent();
		re.source = this;
		
		re.propertyName = propertyName;
		re.oldValue = oldValue;
		re.newValue = newValue;
		
		return re;
	}
	
	/**
	 * dispatch a event to all {@link RobotListener}.
	 * <BR><BR>
	 * to get a instance of {@link RobotEvent}, please invoke {@link #buildRobotEvent(String, Object, Object)}.
	 * <BR><BR>
	 * <STRONG>Important : </STRONG>the event will be recycled and clean after dispatched, and you can get it from {@link #buildRobotEvent(String, Object, Object)}.
	 * <BR>please do NOT keep any reference of it.
	 * @param event 
	 * @see #addRobotListener(RobotListener)
	 * @see #removeRobotListener(RobotListener)
	 * @see #buildRobotEvent(String, Object, Object)
	 * @since 7.0
	 */
	protected final void dispatchRobotEvent(final RobotEvent event) {
		if(event == null){
			return;
		}
		
		if(robotListeners != null){
			try{
				final int size = robotListeners.size();
				for (int i = 0; i < size; i++) {
					final RobotListener robotListener = robotListeners.get(i);
					robotListener.action(event);//无需强制使用用户线程组，因为由用户触发
				}
			}catch (final IndexOutOfBoundsException outOfBound) {
				//越界或不存在或已删除
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		
		RobotEventPool.instance.recycle(event);
	}
	
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
