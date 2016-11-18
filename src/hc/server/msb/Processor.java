package hc.server.msb;

import hc.core.L;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;

import java.util.ArrayDeque;

public abstract class Processor{
	String name;
	final ProjectContext __context;
	final int procType;
	final String project_id;
	boolean isFinishStarted = false;
	final String classSimpleName;

	final ArrayDeque<Message> todo = new ArrayDeque<Message>();
	boolean isShutdown = false;
	boolean isStarted = false;
	Workbench workbench;

	final void init(final Workbench workbench) {
		synchronized (this) {
			if(isStarted){
				return;
			}
			isStarted = true;	
		}
		
		this.workbench = workbench;
		
		ServerUIAPIAgent.runAndWaitInProjContext(__context, new ReturnableRunnable() {
			@Override
			public Object run() {
				startableRunnable.start();
				return null;
			}
		});
		ServerUIAPIAgent.runInProjContext(__context, startableRunnable);
	}

	final void __response(final Message msg, final boolean isDownward){
		if(msg == preMsg){
			throw new MSBException(this.getClass().getSimpleName() + " response/forward " + Message.class.getSimpleName() + " should be a instance from getFreeMessage", msg, this);
		}
		
		final int header_sync_id = preMsg.ctrl_sync_id;
		if(header_sync_id != 0){
			msg.ctrl_sync_id = header_sync_id;
		}

//		workbench.V = workbench.O ? false : workbench.log(this.getClass().getSimpleName() + " todo new Message " + msg.toString() + "\n from Message " + preMsg.toString());

		msg.ctrl_level = preMsg.ctrl_level + 1;

		workbench.dispatch(msg, isDownward, procType);
	}
	
	/**
	 * @param dev_id the device ID of new free {@link Message}, which will be dispatched to. It is <i>Reference Device ID</i>, if it is dispatched from {@link Robot}; it is real device ID, if it is dispatched from {@link Device}.
	 * @return
	 */
	final Message getFreeMessageInProc(final String dev_id) {
		if(isFinishStarted == false){
			try{
				Thread.sleep(500);//稍等，以免产生不需的下行warning
			}catch (final Exception e) {
			}
			synchronized (this) {
				if(isFinishStarted == false){
					L.V = L.O ? false : LogManager.warning("waiting for project [" + project_id +"] to ACTIVE in Device/Robot [" + name + "]...");
					L.V = L.O ? false : LogManager.warning("if your programe is blocked, please invoke getFreeMessage in ProjectContext.run().");

					try{
						this.wait();
					}catch (final Exception e) {
					}
				}
			}
		}
		
		if(dev_id == null || dev_id.length() == 0){
			throw new MSBException(MSBException.EMPTY_DEV_ID, null, null);
		}
		final Message msg = Workbench.messagePool.getFreeMessage();
		msg.ctrl_dev_id = dev_id;
		
		workbench.V = workbench.O ? false : workbench.log("{" + project_id + "/" + name + "} get free message : " + msg.toString());
		
		return msg;
	}
	
	StartableRunnable startableRunnable;
	
	Message preMsg;

	/**
	 * if it was shutdown, then return false; else return true;
	 * @return
	 */
	final boolean shutdownByHC() {
		if(isStarted == false){
			return false;
		}
		
		if(isShutdown){
			return false;
		}
		
		synchronized (todo) {
			isShutdown = true;
			todo.notifyAll();
		}
		
		if(this instanceof Device){
			MSBAgent.notifyDeviceShutdownForSuperRight((Device)this);
		}
		
		startableRunnable = null;
		return true;
	}

	final void pushInFromWorkbench(final Message msg) {
//		workbench.V = workbench.O ? false : workbench.log("push message " + msg.toString() + "\n to processor " + toString());
	
		synchronized (todo) {
			if(isShutdown){
				msg.tryRecycle(workbench, true);
				return;
			}
			
			todo.addLast(msg);
			todo.notify();
		}
	}
	
	public Processor(final String n, final int procType) {
		this(n, procType, ProjectContext.getProjectContext());
	}
	
	final boolean isLoggerOn;
	
	public Processor(final String n, final int procType, final ProjectContext ctx) {
		super();
		isLoggerOn = UserThreadResourceUtil.isLoggerOn();
		this.classSimpleName = this.getClass().getSimpleName();
		__context = ctx;
		if(__context != null){
			project_id = __context.getProjectID();
		}else{
			LogManager.errToLog("No project context.");
			project_id = "unknow project";
		}
		this.procType = procType;
		
		String propName = "";
		if(procType == Workbench.TYPE_DEVICE_PROC){
			propName = ServerUIAPIAgent.DEVICE_NAME_PROP;
		}else if(procType == Workbench.TYPE_ROBOT_PROC){
			propName = ServerUIAPIAgent.ROBOT_NAME_PROP;
		}else if(procType == Workbench.TYPE_CONVERTER_PROC){
			propName = ServerUIAPIAgent.CONVERT_NAME_PROP;
		}
		name = ServerUIAPIAgent.getProcessorNameFromCtx(__context, n, propName);
		
		startableRunnable = new ProcessorRunnable(this);
	}
	
	final void preprocess(final Message msg) {
		preMsg = msg;
		
		if(procType == Workbench.TYPE_ROBOT_PROC || procType == Workbench.TYPE_DEVICE_PROC){
			workbench.V = workbench.O ? false : workbench.log("enter {" + project_id + "/" + name + "} method response(Message), message : " + msg.toString());
		}
		
		response(msg);
	}

	/**
	 * start up the device or smart module.
	 * <br><br>the start-up is in a thread owned by this {@link Device}, so it is no necessary to create new thread.
	 * <br><br>this start-up process may be never finished (if some error on hardware), and HomeCenter Server will shutdown, the method <i>interrupt</i> of this initial thread is invoked by Server.
	 * <br><br>this method is invoked by server after {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}, so it is no necessary to execute in JRuby on event {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}
	 */
	abstract void __startup();

	/**
	 * shut down the device or smart module.
	 * <br><br>the shut-down is in a thread owned by this {@link Device}, so it is no necessary to create new thread.
	 * <br><br>this method is invoked by server after {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}, so it is no necessary to execute in JRuby on event {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}
	 */
	abstract void __shutdown();

	/**
	 * call {@link #getFreeMessage()} to get new instance.
	 * <br> call {@link #__response(Message, boolean)} to dispatch a {@link Message} to server.
	 * <br><br><Strong>Note : </Strong>it is <Strong>not</Strong> allowed to keep any references of {@link Message} in the instance of Processor.
	 * @param msg it is NOT allowed to modified any parts, because it may be consumed by other object.<br>the message will be auto recycled by HomeCenter server.
	 */
	public abstract void response(Message msg);
	
	String getIoTDesc(){
		return buildDesc(name, __context);// + super.toString();
	}

	static final String buildDesc(final String name, final ProjectContext ctx) {
		return " [" + name + "] in project [" + ctx.getProjectID() + "]";
	}
	
//	@Override
//	public String toString(){//please use getIoTDesc
//		return " [" + name + "] in project [" + __context.getProjectID() + "]";// + super.toString();
//	}
	
}