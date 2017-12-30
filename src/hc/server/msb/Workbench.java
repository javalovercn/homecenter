package hc.server.msb;

import hc.core.HCTimer;
import hc.core.util.LogManager;
import hc.server.ui.design.LinkProjectStore;
import hc.util.ResourceUtil;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Machine Service Bus (MSB, a smart module is a abstract machine) is a infrastructure that unifies and connects machines, services, and resources within a network. It also supports intelligently directed communication and mediated relationships among loosely coupled and decoupled machines and smart modules.
 *<br><br>
 * a <code>Message</code> may be consumed by multiple <code>Device</code>/<code>Robot</code> in <b>different</b> HAR projects. <br>So a <code>Message</code> can't be modified after {@link #dispatch(Message, boolean, int)}, {@link #waitFor(Message, long, boolean, int, Processor)}, 
 * in other words, it can't be modified in {@link Device#response(Message)}, {@link Converter#downConvert(Message, Message)}, {@link Converter#upConvert(Message, Message)}}, {@link Robot#response(Message)}.
 * <br><br>
 * In a classic user case, a machine (air conditioning) <i>A</i> is substituted by other model air conditioning <i>B</i>, then a transformation <code>Converter</code> (or <b>a HAR project contains only one <code>Converter</code></b>) may be plugged-in to transform old <code>Message</code> for air conditioning <i>B</i>.
 */
public class Workbench{
	final static MessagePool messagePool = new MessagePool();
	final HashMap<String, String> mapRefer2Dev = new HashMap<String, String>();
	final HashMap<String, String> mapDev2Refer = new HashMap<String, String>();
	final NameMapper nameMapper;
	
	protected Workbench(final NameMapper mapper){
		nameMapper = mapper;
	}
	
	public final void reloadMap(){
		nameMapper.reloadMap();
	}
	
	public final void appendProjectToBindSet(final LinkProjectStore lps){
		nameMapper.appendBindToNameSet(lps);
	}
	
	public final WorkingDeviceList getWorkingDeviceList(final String projectID){
		return nameMapper.getWorkingDeviceList(projectID);
	}
	
	public boolean O = true;
	public boolean V = false;
	
	final protected void enableDebugInfo(final boolean enable){
		O = !enable;
	}

	public final int STATUS_BUILDING = 0;
	public final int STATUS_STARTED = 1;
	public final int STATUS_SHUTDOWN = 2;
	
	private int workbench_status = STATUS_BUILDING;

	final ConcurrentHashMap<String, HashMap<String, Processor>> projID_robotName_Proc = new ConcurrentHashMap<String, HashMap<String,Processor>>();
	final ConcurrentHashMap<String, HashMap<String, Processor>> projID_convName_Proc = new ConcurrentHashMap<String, HashMap<String,Processor>>();
	final ConcurrentHashMap<String, HashMap<String, Processor>> projID_devName_Proc = new ConcurrentHashMap<String, HashMap<String,Processor>>();
	
	protected static final int TYPE_DEVICE_PROC = 0;
	protected static final int TYPE_CONVERTER_PROC = 1;
	protected static final int TYPE_ROBOT_PROC = 2;
	
	final protected boolean removeProcessor(final Processor proc){
		synchronized (this) {
			if(proc.shutdownByHC() == false){
				return false;
			}
			
			final int type = proc.procType;
			final String projID = proc.project_id;
			HashMap<String, Processor> names_proc = null;
			ConcurrentHashMap<String, HashMap<String, Processor>> projID_XXX_proc = null;
			
			if(type == TYPE_CONVERTER_PROC){
				V = O ? false : log("remove Converter : " + proc.getIoTDesc());
				projID_XXX_proc = projID_convName_Proc;
			}else if(type == TYPE_ROBOT_PROC){
				V = O ? false : log("remove Robot : " + proc.getIoTDesc());
				projID_XXX_proc = projID_robotName_Proc;
			}else if(type == TYPE_DEVICE_PROC){
				V = O ? false : log("remove Device : " + proc.getIoTDesc());
				projID_XXX_proc = projID_devName_Proc;
			}
			if(projID_XXX_proc != null){
				names_proc = projID_XXX_proc.get(projID);
			}
			if(names_proc != null){
				final Processor remove = names_proc.remove(proc.name);
				if(remove != null && remove == proc){
					V = O ? false : log("success remove processor : " + proc.getIoTDesc());
					return true;
				}
			}
		}
		return false;
	}
	
	final private void shutdownProcessor(final ConcurrentHashMap<String, HashMap<String, Processor>> proj_xxx_proc){
		final Iterator<String> it = proj_xxx_proc.keySet().iterator();
		while(it.hasNext()){
			final String projID = it.next();
			final HashMap<String, Processor> names = proj_xxx_proc.get(projID);
			final Iterator<String> name_strs = names.keySet().iterator();
			while(name_strs.hasNext()){
				final String name = name_strs.next();
				names.get(name).shutdownByHC();
			}
		}
	}
	
	final private void clearProcessor(final ConcurrentHashMap<String, HashMap<String, Processor>> proj_xxx_proc){
		proj_xxx_proc.clear();
	}
	
	final private void shutdownAllProcessor(){
		synchronized (this) {
			shutdownProcessor(projID_robotName_Proc);
			shutdownProcessor(projID_convName_Proc);
			shutdownProcessor(projID_devName_Proc);
		}
		
		//可能有在途任务，需要访问map表，不能为null
		new HCTimer("", ResourceUtil.getIntervalSecondsForNextStartup() * 1000, true) {
			@Override
			public void doBiz() {
				clearProcessor(projID_robotName_Proc);
				clearProcessor(projID_convName_Proc);
				clearProcessor(projID_devName_Proc);		
				
				HCTimer.remove(this);
			}
		};
		
		System.gc();
	}
	
	final private void startAllProcesor(final ConcurrentHashMap<String, HashMap<String, Processor>> proj_xxx_proc, final boolean isFinish){
		final Iterator<String> it = proj_xxx_proc.keySet().iterator();
		while(it.hasNext()){
			final String projID = it.next();
			final HashMap<String, Processor> names = proj_xxx_proc.get(projID);
			final Iterator<String> name_strs = names.keySet().iterator();
			while(name_strs.hasNext()){
				final String name = name_strs.next();
				final Processor processor = names.get(name);
				if(isFinish == false){
					initAndStartProcessor(processor);
				}else{
					finishStartProcessor(processor);
				}
			}
		}
	}
	
	final protected void startAllProcesor(final boolean isFinishStart){
//			每个Processor内部会检查是否已started，而相应忽略
//			if(isStartedProcessor){
//				return;
//			}
		startAllProcesor(projID_robotName_Proc, isFinishStart);
		startAllProcesor(projID_convName_Proc, isFinishStart);
		startAllProcesor(projID_devName_Proc, isFinishStart);
		
		//注意：该方法可能被多次调用，所以
		if(isFinishStart && workbench_status < STATUS_STARTED){
			workbench_status = STATUS_STARTED;
		}
	}
	
	final private void initAndStartProcessor(final Processor processor) {
		processor.init(this);
	}
	
	final private void finishStartProcessor(final Processor processor) {
		if (processor.isFinishStarted == false){
			processor.isFinishStarted = true;//通知Device/Robot可以getFreeMessage
			synchronized (processor) {
				processor.notifyAll();//可能多个线程
			}
		}
	}

	public final void addRobot(final Robot robot){
		addProcessor(robot);
	}
	
	protected final void addProcessor(final Processor proc){
		final int type = proc.procType;
		final String projID = proc.project_id;
		HashMap<String, Processor> names_proc = null;
		ConcurrentHashMap<String, HashMap<String, Processor>> projID_XXX_proc = null;
		
		if(type == TYPE_CONVERTER_PROC){
			V = O ? false : log("add Converter : " + proc.getIoTDesc());
			projID_XXX_proc = projID_convName_Proc;
		}else if(type == TYPE_ROBOT_PROC){
			V = O ? false : log("add Robot : " + proc.getIoTDesc());
			projID_XXX_proc = projID_robotName_Proc;
		}else if(type == TYPE_DEVICE_PROC){
			V = O ? false : log("add Device : " + proc.getIoTDesc());
			projID_XXX_proc = projID_devName_Proc;
		}
		
		if(projID_XXX_proc != null){
			names_proc = projID_XXX_proc.get(projID);
		}
		
		if(names_proc == null){
			names_proc = new HashMap<String, Processor>();
			projID_XXX_proc.put(projID, names_proc);
		}
		final Processor old = names_proc.put(proc.name, proc);
		if(old != null){
			new MSBException(MSBException.SAME_KEY_PROC, null, proc);
		}
	}
	
	public final boolean log(final String msg){
		LogManager.log("[MSB Thread:" + Thread.currentThread().getId() + "] " + msg);
		return false;
	}
	
	public final boolean err(final String msg){
		LogManager.err("[MSB Error Thread:" + Thread.currentThread().getId() + "] " + msg);
		return false;
	}
	
	protected final void todo(final Message msg, final int fromProcType){
		final String devID = msg.ctrl_dev_id;

		if(devID == null || devID.length() == 0){
			throw new MSBException(MSBException.EMPTY_DEV_ID, msg, null);
		}

		final boolean isDown = msg.ctrl_is_downward;

//		synchronized(msg){
			if(msg.ctrl_isInWorkbench){
				throw new MSBException(MSBException.UN_RE_DISPATCH, msg, null);
			}
			
			msg.ctrl_isInWorkbench = true;
			
			if(fromProcType == TYPE_ROBOT_PROC){
				//将referName替换为真实设备名
				msg.ctrl_dev_id = nameMapper.bind2RealDeviceBindInfo.get(msg.ctrl_bind_id).dev_id;
			}
//			Workbench.V = Workbench.O ? false : log("todo workbench, header_refer_num : " + msg.header_refer_num);
//		}
		
		//分发消息
		if(msg.ctrl_level > MSBException.TOO_MUCH_FORWARD){
//			Workbench.V = Workbench.O ? false : err("too much times forwarding on Message : " + msg.toString());
			new MSBException(MSBException.TOO_MUCH_FORWARD_STR, msg, null);
		}else{
			if(fromProcType == TYPE_ROBOT_PROC){
				final ConverterInfo cbi = nameMapper.bind2ConverterBindInfo.get(msg.ctrl_bind_id);
				if(cbi != null){
					final Processor proc = projID_convName_Proc.get(cbi.proj_id).get(cbi.name);
					if(proc == null){
						LogManager.err("fail to transfer message to Converter : [" + cbi.proj_id +"]/{" + cbi.name + "}");
					}else{
						proc.pushInFromWorkbench(msg);
					}
					return;
				}
				
				final RealDeviceInfo rdbi = nameMapper.bind2RealDeviceBindInfo.get(msg.ctrl_bind_id);
				if(rdbi != null){
					final Processor proc = projID_devName_Proc.get(rdbi.proj_id).get(rdbi.dev_name);
					if(proc == null){
						LogManager.err("fail to transfer message to Device : [" + rdbi.proj_id +"]/{" + rdbi.dev_name + "}");
					}else{
						proc.pushInFromWorkbench(msg);
					}
					return;
				}
			}else if(fromProcType == TYPE_CONVERTER_PROC){
				if(isDown){
					final RealDeviceInfo rdbi = nameMapper.bind2RealDeviceBindInfo.get(msg.ctrl_bind_id);
					if(rdbi != null){
						final Processor proc = projID_devName_Proc.get(rdbi.proj_id).get(rdbi.dev_name);
						if(proc == null){
							LogManager.err("fail to transfer message to Device : [" + rdbi.proj_id +"]/{" + rdbi.dev_name + "}");
						}else{
							proc.pushInFromWorkbench(msg);
						}
						return;
					}
				}else{
					if(msg.checkWaitFor(this)){
						return;
					}
					
					final RobotReferBindInfo rrbi = nameMapper.bind2ReferID.get(msg.ctrl_bind_id);
					if(rrbi != null){
						final Processor proc = projID_robotName_Proc.get(rrbi.proj_id).get(rrbi.robot_name);
						if(proc == null){
							LogManager.err("fail to transfer message to Robot : [" + rrbi.proj_id +"]/{" + rrbi.robot_name + "}");
						}else{
							proc.pushInFromWorkbench(msg);
						}
						return;
					}
				}
			}else if(fromProcType == TYPE_DEVICE_PROC){
				if(msg.ctrl_isInitiative){
					final Vector<String> bindids = msg.ctrl_bind_ids;
					final int size = bindids.size();
					if(size > 0){
						final int sizeMinusOne = size - 1;
						for (int i = 0; i < size; i++) {
							final String bind_id = bindids.elementAt(i);
							
							Message clone;
							if(i == sizeMinusOne){
								clone = msg;
								clone.ctrl_bind_id = bind_id;
								clone.ctrl_dev_id = nameMapper.bind2ReferID.get(bind_id).refer_id;
							}else{
								clone = cloneFromMessage(msg);
								clone.ctrl_bind_id = bind_id;
								clone.ctrl_dev_id = nameMapper.bind2ReferID.get(bind_id).refer_id;
								V = O ? false : log("clone publish message : " + clone.toString() + "\n\tfrom :" + msg.toString());
							}
							
							//主动模式，复制到全部
							final ConverterInfo cbi = nameMapper.bind2ConverterBindInfo.get(bind_id);
							if(cbi != null){
								final Processor proc = projID_convName_Proc.get(cbi.proj_id).get(cbi.name);
								if(proc == null){
									LogManager.err("fail to transfer message to Converter : [" + cbi.proj_id +"]/{" + cbi.name + "}");
								}else{
									proc.pushInFromWorkbench(clone);
								}
								
								//重要：由于在循环内，要用continue
								continue;
							}
							
							final RobotReferBindInfo rrbi = nameMapper.bind2ReferID.get(bind_id);
							if(rrbi != null){
								final Processor proc = projID_robotName_Proc.get(rrbi.proj_id).get(rrbi.robot_name);
								if(proc == null){
									LogManager.err("fail to transfer message to Robot : [" + rrbi.proj_id +"]/{" + rrbi.robot_name + "}");
								}else{
									proc.pushInFromWorkbench(clone);
								}
								
								//重要：由于在循环内，要用continue
								continue;
							}
						}
						return;
					}
				}else{
					final String bind_id = msg.ctrl_bind_id;

					//被动模式
					final ConverterInfo cbi = nameMapper.bind2ConverterBindInfo.get(bind_id);
					if(cbi != null){
						final Processor proc = projID_convName_Proc.get(cbi.proj_id).get(cbi.name);
						if(proc == null){//曾经部署工程，删除一个无用Processor后，再次部署，由于取消/中止新的绑定，从而导致两者不一致
							LogManager.err("fail to transfer message to Converter : [" + cbi.proj_id +"]/{" + cbi.name + "}");
						}else{
							proc.pushInFromWorkbench(msg);
						}
						return;
					}
					
					if(msg.checkWaitFor(this)){
						return;
					}
					
					final RobotReferBindInfo rrbi = nameMapper.bind2ReferID.get(bind_id);
					if(rrbi != null){
						final Processor proc = projID_robotName_Proc.get(rrbi.proj_id).get(rrbi.robot_name);
						if(proc == null){
							LogManager.err("fail to transfer message to Robot : [" + rrbi.proj_id +"]/{" + rrbi.robot_name + "}");
						}else{
							proc.pushInFromWorkbench(msg);
						}
						return;
					}
				}
			}
			new MSBException("unused MSB message", msg, null);
			err("a unused MSB message [" + msg.toString() + "]");
			final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			for (int i = 0; i < stackTraceElements.length; ++i) {
	            err(stackTraceElements[i].toString() + "ClassName: "
	                    + stackTraceElements[i].getClassName() + "FileName: " + stackTraceElements[i].getFileName() + "LineNumber: "
	                    + stackTraceElements[i].getLineNumber() + "MethodName: " + stackTraceElements[i].getMethodName());
	        }
			messagePool.recycle(msg, this);
		}
	}
	
	final Message cloneFromMessage(final Message msg){
		final Message clone = messagePool.getFreeMessage();
		{
			final Iterator<String> it_prop = msg.getHeaderNames().iterator();
			while(it_prop.hasNext()){
				final String name = it_prop.next();
				clone.setObjectHeader(name, msg.getObjectHeader(name));
			}
		}
		
		{
			final Iterator<String> it_body = msg.getBodyNames().iterator();
			while(it_body.hasNext()){
				final String name = it_body.next();
				clone.setObjectBody(name, msg.getObjectBody(name));
			}
		}
		
		msg.cloneHeaderTo(clone);
		
		return clone;
	}
	
	protected final void stopAllProcessor() {
		if(workbench_status >= STATUS_SHUTDOWN){
			return;
		}
		
    	workbench_status = STATUS_SHUTDOWN;
    	
    	try{
    		Thread.sleep(500);
    	}catch (final Exception e) {
		}

    	shutdownAllProcessor();
    }
	
	static final ArrayDeque<WaitingForMessage> waitPool = new ArrayDeque<WaitingForMessage>();
	
	static final protected WaitingForMessage getFreeWaiting(){
		WaitingForMessage out;
		synchronized (waitPool) {
			out = waitPool.pollLast();
		}
		if(out == null){
			return new WaitingForMessage();
		}
		return out;
	}
	
	static final protected void cycleWaitObject(final WaitingForMessage w){
		synchronized (waitPool) {
			waitPool.addLast(w);
		}
	}
	
	/**
	 * get a new / unused <code>Message</code>
	 * @return
	 */
	protected final Message getFreeMessage(){
		return messagePool.getFreeMessage();
	}
	
	final Hashtable<Long, Message> lastUncycleMessage = new Hashtable<Long, Message>(40);
	final Hashtable<Integer, WaitingForMessage> waiting = new Hashtable<Integer, WaitingForMessage>(40);
	private final int SYNC_INIT_ID = 1;
	private int synch_no = SYNC_INIT_ID;
	protected final int FORWARDED_SYNC_ID = Integer.MAX_VALUE;
	private final int MAX_SYNC_ID = Integer.MAX_VALUE - 1;
	/**
	 * process a <code>Message</code> synchronous and waiting for response.
	 * @param msg the object must be got from {@link #getFreeMessage()}, and processed by this method only one time. the object will be auto recycled by server.
	 * @param isDownward <br>true : from smart module to device, or <b>control flow</b> to device.<br>false (Upward) : from device to smart module, or <b>status flow</b> from device.
	 * @param fromProcType
	 * @param processor
	 * @return null if it is not processed by all <code>Device</code>.<br>You must call {@link #recycle(Message)} to recycle the return object after finish business.
	 * @see #waitFor(Message, long, boolean, int, Processor)
	 * @see #dispatch(Message, boolean, int)
	 */
	private final Message process(final Message msg, final boolean isDownward, final int fromProcType, final Processor processor){
		return waitFor(msg, 0, isDownward, fromProcType, processor);
	}
	
	/**
	 * process a <code>Message</code> synchronous and waiting for response.
	 * @param msg the object must be got from {@link #getFreeMessage()}, and processed by this method only one time. the object will be auto recycled by server.
	 * @param timeout the maximum time to wait in milliseconds.
	 * @param downward <br>true : from smart module to device, or <b>control flow</b> to device.<br>false (upward) : from device to smart module, or <b>status flow</b> from device.
	 * @param fromProcType 
	 * @param processor
	 * @return null if it is not processed by all <code>Device</code> or the time is out.
	 * @see #process(Message, boolean, int, Processor)
	 * @see #dispatch(Message, boolean, int)
	 */
	protected final Message waitFor(final Message msg, final long timeout, final boolean downward, final int fromProcType, final Processor processor){
		final long thread_id = Thread.currentThread().getId();
		
		final Message old = lastUncycleMessage.remove(thread_id);
		if(old != null){
			V = O ? false : log("find last un-recycle wait-result message :" + old.toString());
			recycle(old, thread_id);
		}
		
		final WaitingForMessage wm = getFreeWaiting();

		msg.ctrl_dispatch_thread_id = thread_id;
		msg.ctrl_is_downward = downward;
		
		synchronized(Workbench.class){
			msg.ctrl_sync_id = synch_no++;
			if(synch_no == MAX_SYNC_ID){
				synch_no = SYNC_INIT_ID;
			}
		}
		final WaitingForMessage oldwm = waiting.put(msg.ctrl_sync_id, wm);
		if(oldwm != null){
			V = O ? false : log("override header_sync_id, force finish process for thread ID:" + oldwm.dispatch_thread_id);
			oldwm.wakeUp();
		}
		return wm.waiting(msg, timeout, thread_id, this, fromProcType, processor);
	}
	
	/**
	 * dispatch a <code>Message</code> and process it in asynchronous and NO waiting.
	 * @param msg the object must be got from {@link #getFreeMessage()}, and send by this method only one time. the object will be auto recycled by server.
	 * @param isDownward <br>true : from smart module to device, or <b>control flow</b> to device.<br>false (Upward) : from device to smart module, or <b>status flow</b> from device.
	 * @param fromProcType 
	 * @return
	 * @see #process(Message, boolean, int, Processor)
	 * @see #waitFor(Message, long, boolean, int, Processor)
	 */
	protected final void dispatch(final Message msg, final boolean isDownward, final int fromProcType){
		msg.ctrl_is_downward = isDownward;
		msg.ctrl_dispatch_thread_id = Thread.currentThread().getId();
		todo(msg, fromProcType);
	}
	
	/**
	 * recycle a <code>Message</code> instance.
	 * <br>Important : the object msg must be returned from {@link #process(Message, boolean, int, Processor)} or {@link #waitFor(Message, long, boolean, int, Processor)}.
	 * @param msg the object which is not returned from above methods should be auto recycled by Server.
	 */
	protected final void recycle(final Message msg, final long thread_id){
		if(msg == null){
			return;
		}
		
		msg.tryRecycle(this, false);
	}

}

class WaitingForMessage {
	Message result;
	long dispatch_thread_id;

	public final void wakeUp(){
		synchronized(this){
			this.notify();
		}
	}
	
	public final Message waiting(final Message msg, final long timeout, final long thread_id, final Workbench workbench, final int fromProcType, final Processor processor){
		dispatch_thread_id = thread_id;

		workbench.V = workbench.O ? false : workbench.log("waiting response on :" + msg.toString());
		
		synchronized(this){
			workbench.todo(msg, fromProcType);
			
			try{
				if(timeout <= 0){
					this.wait();
				}else{
					this.wait(timeout);
				}
			}catch (final Exception e) {
			}
		}
		
		final Message out = result;
		if(out != null){
			workbench.V = workbench.O ? false : workbench.log("{" + processor.project_id + "/" + processor.name + "} wait a result :" + out.toString());
			workbench.lastUncycleMessage.put(dispatch_thread_id, out);
		}else{
			workbench.V = workbench.O ? false : workbench.log("{" + processor.project_id + "/" + processor.name + "} wait no result!!!");
		}
		
		//准备回收
		result = null;
		dispatch_thread_id = 0;
		Workbench.cycleWaitObject(this);
		
		return out;
	}
}