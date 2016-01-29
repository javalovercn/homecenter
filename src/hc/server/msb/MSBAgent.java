package hc.server.msb;

import hc.core.ContextManager;
import hc.core.util.CCoreUtil;
import hc.core.util.ReturnableRunnable;
import hc.core.util.WiFiDeviceManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.design.AddHarHTMLMlet;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;


/**
 * Important : this class may be removed, changed , renamed...
 * please don't use any class and method outside API doc files.
 */
public final class MSBAgent {
	
	public final static String[] getCompatibleItem(final DeviceCompatibleDescription compDesc){
		return compDesc.getCompatibleItem();
	}
	
	public final static Workbench buildWorkbench(final NameMapper mapper){
		return new Workbench(mapper);
	}

	public final static String getName(final Converter converter){
		return converter.getName();
	}
	
	public final static String getName(final Device device){
		return device.getName();
	}
	
	public final static String getName(final Robot robot){
		return robot.getName();
	}
	
//	public final static void setName(final Converter converter, final String name){
//		converter.setName(name);
//	}
	
	private static HashSet<Device> deviceSet = new HashSet<Device>(20);
	private static Object sysThreadPoolToken = ContextManager.getThreadPoolToken();
	
	
	public final static void clearWiFiAccountGroup(final Device device, final String cmdGroup){
		final boolean hasToken = hasToken(device);
		if(hasToken){
			ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() {
					WiFiDeviceManager.getInstance().clearWiFiAccountGroup(cmdGroup);		
					return null;
				}
			}, sysThreadPoolToken);
		}else{
			throw new Error("no permission to clearWiFiAccountGroup!");
		}
	}
	
	public final static void broadcastWiFiAccountAsSSID(final String projectID, final Device device, final String[] encryptedCommand, final String cmdGroup){
		final boolean hasToken = hasToken(device);
		if(hasToken){
			ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() {
					WiFiDeviceManager.getInstance().broadcastWiFiAccountAsSSID(encryptedCommand, cmdGroup);		
					if(ContextManager.isMobileLogin()){
						final AddHarHTMLMlet mlet = AddHarHTMLMlet.getCurrAddHarHTMLMlet();
						if(mlet != null){
							mlet.notifyBroadcastWifiAccout(projectID, device.getName());
						}
					}
					return null;
				}
			}, sysThreadPoolToken);
		}else{
			throw new Error("no permission to broadcastWiFiAccountAsSSID!");
		}
	}
	
	public final static OutputStream createWiFiMulticastStream(final Device device, final String multicastIP, final int port){
		final boolean hasToken = hasToken(device);
		if(hasToken){
			return (OutputStream)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() {
					return WiFiDeviceManager.getInstance().createWiFiMulticastStream(multicastIP, port);
				}
			}, sysThreadPoolToken);
		}else{
			throw new Error("no permission to createWiFiMulticastStream!");
		}
	}
	
//	public final static void startDevice(final Device device){
//		device.__startup();
//	}
	
	public final static String[] getRegisterDeviceID(final Device device){
		if(device.isStarted == false){
			device.init();
		}
		return device.connectedDevices;
	}
	
	public final static boolean isStartingProcessor(final Processor processor){
		return processor.isStarted && processor.isShutdown == false;
	}
	
	public final static WiFiAccount getWiFiAccount(final Device device, final ProjectContext ctx){
		final boolean hasToken = hasToken(device);
		if(hasToken){
			return (WiFiAccount)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() {
					return WiFiHelper.getWiFiAccount(ctx, (ThreadGroup)sysThreadPoolToken);
				}
			}, sysThreadPoolToken);
		}else{
			throw new Error("no permission to getWiFiAccount!");
		}
	}
	
	public final static String[] getWiFiSSIDListOnAir(final Device device){
		final boolean hasToken = hasToken(device);
		if(hasToken){
			final String[] out =  (String[])ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() {
					return WiFiDeviceManager.getInstance().getSSIDListOnAir();
				}
			}, sysThreadPoolToken);
//			for (int i = 0; i < out.length; i++) {
//				System.out.println("getWiFiSSIDListOnAir : " + out[i]);
//			}
			return out;
		}else{
			throw new Error("no permission to getWiFiSSIDListOnAir!");
		}
	}
	
	public final static InputStream listenFromWiFiMulticast(final Device device, final String multicastIP, final int port){
		final boolean hasToken = hasToken(device);
		if(hasToken){
			return (InputStream)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
				@Override
				public Object run() {
					return WiFiDeviceManager.getInstance().listenFromWiFiMulticast(multicastIP, port);
				}
			}, sysThreadPoolToken);
		}else{
			throw new Error("no permission to listenFromWiFiMulticast!");
		}
	}
	
	private final static boolean hasToken(final Device device){
		synchronized (MSBAgent.class) {
			return deviceSet.contains(device);
		}
	}
	
	public final synchronized static void notifyDeviceShutdownForSuperRight(final Device device){
		synchronized (MSBAgent.class) {
			deviceSet.remove(device);
		}
	}
	
	public final synchronized static void addSuperRightSet(final Device device){
		CCoreUtil.checkAccess();
		
		synchronized (MSBAgent.class) {
			deviceSet.add(device);
		}
	}
	
	public static void resetDeviceSet(){
		CCoreUtil.checkAccess();
		
		deviceSet.clear();
	}
	
	public final Workbench workbench;
	public final void dispatch(final Robot robot, final Message msg, final boolean isInitiative){
		robot.dispatch(msg, isInitiative);
	}
	
	public final Message waitFor(final Robot robot, final Message msg, final long timeout){
		return robot.waitFor(msg, timeout);
	}
	
	public final Message getFreeMessage(final String dev_id){
		final Message msg = workbench.getFreeMessage();
		msg.ctrl_dev_id = dev_id;
		return msg;
	}
	
	public MSBAgent(final Workbench workbench) {
		this.workbench = workbench;
	}
	
	public final void shutDown(final Device p){
		workbench.removeProcessor(p);
	}
	
	/**
	 * 此处是初始启动时，运行一次。
	 * 运行时，增加新工程时，可能被再次调用一次。
	 */
	public final void startAllProcessor(){
		synchronized (workbench) {
			workbench.startAllProcesor(false);
			workbench.startAllProcesor(true);
		}
	}

	public final void stopAllProcessor(){
		synchronized (workbench) {
			workbench.stopAllProcessor();
		}
	}
	
	public final void enableDebugInfo(final boolean enable){
		workbench.enableDebugInfo(enable);
	}
	
	public final void addDevice(final Device proc){
		workbench.addProcessor(proc);
	}
	
	public final void addRobot(final Robot robot){
		workbench.addProcessor(robot);
	}
	
	public final void addConverter(final Converter fproc){
		fproc.__workbench = workbench;
		
		if(fproc instanceof Converter){
			final Converter tfv = fproc;
			if(tfv.__fpwrapper != null){
				throw new MSBException(fproc.getClass().getSimpleName() + " [" + fproc.getName() + "] was added to server twice.", null, null);
			}
			final ConverterWrapper wrapper = new ConverterWrapper(tfv);
			tfv.__fpwrapper = wrapper;
			workbench.addProcessor(wrapper);
		}
		
	}

	public final boolean removeProcessor(final Device proc){
		return workbench.removeProcessor(proc);
	}
	
	public final boolean removeProcessor(final Converter proc){
		if(proc instanceof Converter){
			return workbench.removeProcessor(proc.__fpwrapper);
		}
		return false;
	}
	
	/**
	 * stop and remove this {@link Device}, and <b>NO</b> {@link Message} will be processed by it in the future.
	 * @param proc
	 * @see #addProcessor(Device)
	 */
	public final void stopProcessor(final Device proc){
		workbench.removeProcessor(proc);
	}
	
}
