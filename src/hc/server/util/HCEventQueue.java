package hc.server.util;

import hc.App;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.Stack;
import hc.server.HCSecurityException;
import hc.util.ClassUtil;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class HCEventQueue extends EventQueue {
//	private static final String JAVA_AWT_EVENT_DISPATCH_THREAD = "java.awt.EventDispatchThread";
	public ContextSecurityConfig currentConfig;
	final Stack list = new Stack(1024);
	final Thread eventDispatchThread;
	boolean isShutdown = false;
	final Field parentGroupField;
	final ThreadGroup rootThreadGroup = App.getRootThreadGroup();
	
	final Thread antiAutoShutdown = new Thread(){
		@Override
		public void run(){
			final Runnable runnable = new Runnable() {
				@Override
				public void run() {
				}
			};
			try{
				while(!isShutdown){
					EventQueue.invokeLater(runnable);
					Thread.sleep(900);
				}
				
				while(true){
					final Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
					final Iterator<Thread> it = map.keySet().iterator();
					int aliveCount = map.size();
					int remainCount = 0;
					while(it.hasNext()){
						final Thread t = it.next();
						if(t.isDaemon() == false){
							if(ContextSecurityManager.getConfig(t.getThreadGroup()) != null){
								aliveCount = -1;
								break;
							}
							
							final String threadName = t.getName();
							if(threadName.startsWith("AWT-Shutdown") 
									|| threadName.startsWith("AWT-EventQueue-") 
									|| threadName.startsWith("DestroyJavaVM")){
								remainCount++;
							}else{
								aliveCount--;
							}
						}else{
							aliveCount--;
						}
					}
					
					if(aliveCount == remainCount){
						CCoreUtil.globalExit();
					}else{
						Thread.sleep(100);
					}
				}
			}catch (final Exception e) {
			}
		}
	};
	
	public final void shutdown(){
		final EventQueue old = Toolkit.getDefaultToolkit().getSystemEventQueue();
		if(old != null){
			if(old instanceof HCEventQueue){
				((HCEventQueue)old).pop();
			}
		}

		isShutdown = true;
	}
	
	@Override
	public void pop() throws EmptyStackException {
		super.pop();
	}

	public HCEventQueue() throws Exception{
		parentGroupField = Thread.class.getDeclaredField("group");
		parentGroupField.setAccessible(true);

		antiAutoShutdown.setDaemon(true);
		antiAutoShutdown.setPriority(Thread.MAX_PRIORITY);
		antiAutoShutdown.start();
		
		try{
			Thread.sleep(50);
		}catch (final Exception e) {
		}
		
		eventDispatchThread = buildToGetEventQueueThread();

		Toolkit.getDefaultToolkit().getSystemEventQueue().push(this);

//			ClassUtil.changeField(Thread.class, eventDispatchThread, "group", null);
	}

	private static Thread buildToGetEventQueueThread(){
		final Thread[] eventQueueThreadArray = new Thread[1];
		final boolean[] failModiGroup = new boolean[1];
		App.invokeAndWaitUI(new Runnable() {
			@Override
			public void run() {
				final Thread currentThread = Thread.currentThread();
				try{
					ClassUtil.changeField(Thread.class, currentThread, "group", null);
				}catch (final Exception e) {
					failModiGroup[0] = true;
				}
				eventQueueThreadArray[0] = currentThread;
			}
		});
		if(failModiGroup[0]){
				final JPanel panel = App.buildMessagePanel("Fail to modify Thread.group = null in " + eventQueueThreadArray[0].getName(), App.getSysIcon(App.SYS_ERROR_ICON));
				App.showCenterPanelMain(panel, 0, 0, "JVM Error", false, null, null, null, null, null, false, true, null, false, false);
		}
		final Thread thread = eventQueueThreadArray[0];
		if(thread != null){
			thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(final Thread t, final Throwable e) {
					LogManager.log("This is UncaughtExceptionHandler for thread : " + t.getName() + ", Message : " + e.getMessage());
				    ExceptionReporter.printStackTrace(e);
				}
			});
		}
		return thread;
	}
	
//	private final void stopPreviousDispatch(Object startEQ, Class eventDispThread) {
//		Object oldEQ = ClassUtil.getField(EventQueue.class, startEQ, "previousQueue");
//		if(oldEQ != null){
//			Object oldEDT = ClassUtil.getField(EventQueue.class, oldEQ, "dispatchThread");
//			if(oldEDT != null){
//				ClassUtil.invoke(eventDispThread, oldEDT, "stopDispatching", ClassUtil.NULL_PARA_TYPES, ClassUtil.NULL_PARAS);
//			}
//			stopPreviousDispatch(oldEQ, eventDispThread);
//		}
//	}
	
//	private final Object buildLimitEventDispatchThread(ThreadGroup group){
//		try{
//			Class eventDispThread = Class.forName(JAVA_AWT_EVENT_DISPATCH_THREAD);
//			Class[] construPara = {ThreadGroup.class, String.class, EventQueue.class};
//			Constructor cons = eventDispThread.getDeclaredConstructor(construPara);
//			cons.setAccessible(true);
//			Object[] para = {group, "limitEventDispatchThread", this};
//			Object eventDispatchThread = cons.newInstance(para);
//			cons.setAccessible(false);
//			Thread thread = (Thread)eventDispatchThread;
//			thread.setDaemon(true);
//			thread.start();
//			
//			return eventDispatchThread;
//		}catch (Throwable e) {
//			App.showMessageDialog(null, "fail to create EventDispathThread!", "Error", JOptionPane.ERROR_MESSAGE);
//			ExceptionReporter.printStackTrace(e);
//		}
//		return null;
//	}
//	
	@Override
	public final void postEvent(final AWTEvent theEvent) {
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
//			if(csc == null){
//				ClassUtil.printCurrentThreadStack("-------------------Non ContextSecurityConfig to postEvent-------------------");
//				return;
//			}
			boolean isOpenWindow = false;
			if(theEvent.getID() == WindowEvent.WINDOW_OPENED && theEvent instanceof WindowEvent){
				final Object src = theEvent.getSource();
				if(src instanceof HCJFrame || src instanceof HCJDialog){
				}else{
					isOpenWindow = true;
				}
			}
			
			HCAWTEvent hcawtEvent;
			synchronized (list) {
				hcawtEvent = (HCAWTEvent)list.pop();
			}
			if(hcawtEvent == null){
				hcawtEvent = new HCAWTEvent();
			}else{
				hcawtEvent.reset();
			}
			hcawtEvent.relaEvent = theEvent;
			hcawtEvent.csc = csc;
			
			super.postEvent(hcawtEvent);
			
			if(isOpenWindow){
				final WindowEvent we = (WindowEvent)theEvent;
				App.invokeLaterUI(new Runnable() {
					@Override
					public void run() {
						we.getWindow().setVisible(false);
						we.getWindow().dispose();
					}
				});
				App.showHARMessageDialog("<html>" +
						"Block open JFrame/JDialog in HAR project since 7.0," +
						"<br>please use API ProjectContext.showInputDialog/showMessage.</html>", "Error", JOptionPane.ERROR_MESSAGE);
				throw new HCSecurityException("block open JFrame/JDialog in HAR project since 7.0," 
//				+ HCLimitSecurityManager.buildPermissionOnDesc(HCjar.PERMISSION_JFRAME_JDIALOG) 
				+ " please use API ProjectContext.showInputDialog/showMessage.");
			}
//			System.out.println("xxxxxxxxxxxxpostEvent hcAWTEvent:" + theEvent);// + theEvent);
			return;
		}
//		System.out.println("mainThread postEvent" + theEvent);
		super.postEvent(theEvent);
	}
	
	@Override
	protected final void dispatchEvent(AWTEvent event) {
		if(event instanceof HCAWTEvent){
			final HCAWTEvent hcawtEvent = (HCAWTEvent)event;
			event = hcawtEvent.relaEvent;
			currentConfig = hcawtEvent.csc;
			
			try{
				parentGroupField.set(eventDispatchThread, currentConfig.threadGroup);
			}catch (final Exception e) {
				throw new HCSecurityException(e.toString());
			}
			synchronized (list) {
				list.push(hcawtEvent);
			}
		}else{
			currentConfig = null;
			try{
				parentGroupField.set(eventDispatchThread, rootThreadGroup);
			}catch (final Exception e) {
				throw new HCSecurityException(e.toString());
			}
		}
		
		super.dispatchEvent(event);
	}
	
	@Override
	public final void push(final EventQueue newEventQueue) {
//		CCoreUtil.checkAccess();
		throw new HCSecurityException("block push EventQueue over HCEventQueue");
//		super.push(newEventQueue);
	}
}

final class HCAWTEvent extends AWTEvent{
	final static JLabel source = new JLabel("hcAWTEvent");
	HCAWTEvent(){
		super(source, ActionEvent.ACTION_PERFORMED);
	}
	
	AWTEvent relaEvent;
	ContextSecurityConfig csc;
	
	public final void reset(){
//		isPosted = false;
		consumed = false;
//		focusManagerIsDispatching = false;
	}
}
