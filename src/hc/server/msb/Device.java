package hc.server.msb;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.ui.CtrlResponse;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.SessionContext;

import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

/**
 * <UL>
 * <LI>In most cases, a real device provides one function. 
 * <BR>For example, a air condition provides temperature control function, if a air condition also provides lighting (equal a smart bulb),
 * you should design two {@link Device} to provide each one of functions.</LI>
 * <LI>A {@link Device} may be a agent to a virtual service.</LI>
 * <LI>A {@link Device} is means not only a real device. After {@link #connect()}, there are maybe two or more same model real devices managed by a instance of {@link Device}.</LI>
 * <LI>To drive {@link Device}, invoke {@link Robot#dispatch(Message, boolean)} or {@link Robot#waitFor(Message, long)}</LI>
 * <LI>The {@link Message#getDevID()} returns real device ID, if {@link Message} is received in {@link Device} or dispatched from {@link Device}. </LI>
 * <LI>The {@link Message#getDevID()} returns <i>Reference Device ID</i> (not real device ID), if the {@link Message} is received in {@link Robot} or dispatched from {@link Robot}.</LI>
 * <LI>After active and apply HAR project, <i>Reference Device ID</i> of {@link Robot} must refer to real device ID (a {@link Robot} can works well without any {@link Device}). If fail on auto-bind ({@link Robot#getDeviceCompatibleDescription(String)} for more), the binding dialog will be shown.</LI>
 * <LI>A real device ID may be referenced by more than one <i>Reference Device ID</i>. <BR>For example, a temperature device initiative publish current temperature to all robots by {@link #dispatch(Message, boolean)}, and the Message will be cloned (not deep).</LI>
 * <LI>{@link Robot} is the only way to drive {@link Device}, even if the {@link Device} is not in same HAR project. <BR>To access {@link Robot} instance from {@link Mlet} or {@link CtrlResponse}, please invoke {@link ProjectContext#getRobot(String)} and drive {@link Device} via {@link Robot#operate(long, Object)}. </li>
 * <LI>{@link Message} is an intermediary between {@link Robot} and {@link Device} (maybe {@link Converter}). <BR>{@link Message} is never used outside of {@link Robot}, {@link Converter} and {@link Device}. In other words, you can't get a instance of {@link Message} from {@link Mlet} or {@link CtrlResponse}.</LI>
 * <LI>Because the {@link Device} can communicate with the {@link Robot} which is not in the same HAR project. It may be a good practice to put only one {@link Device} in a HAR project. If the device need to be substituted or upgraded, what you should do is upgrade HAR project with the newer. Re-binding will not be executed in this case if real device ID is same. For more, see {@link Converter}.</LI>
 * </UL>
 * @see Robot
 */
public abstract class Device extends Processor{
//	 * <LI>If a {@link Robot} is connecting to network via WiFi and manage no real {@link Device}, please create a {@link Device} for it, because <STRONG>No Device No WiFi-Connection</STRONG>.</LI>

	/**
	 * @deprecated
	 */
	@Deprecated
	public Device(){
		this("");
	}
	
	/**
	 * @deprecated
	 * @param n
	 */
	@Deprecated
	public Device(final String n){
		super(n, Workbench.TYPE_DEVICE_PROC);
	}
	
	/**
	 * response a message from robot.
	 * <BR><BR>
	 * the general procedure to response a {@link Message} passively is as following:
	 * <br>1. <code>{@link Message} newMsg = this.getFreeMessage(msg.getDevID());</code>
	 * <br>2. set header or body of <code>newMsg</code>
	 * <br>3. dispatch(newMsg, <strong>false</strong>);//false is required, because it is passively.
	 * <br>4. it is allowed to dispatch an initiative {@link Message} in this method.
	 * @param msg dispatched from {@link Robot#dispatch(Message, boolean)} or {@link Robot#waitFor(Message, long)} (it is also may be converted by a {@link Converter})
	 * @see Device#dispatch(Message, boolean)
	 */
	@Override
	public abstract void response(final Message msg);
	
	/**
	 * get the {@link ProjectContext} instance of current project.
	 * @return
	 * @since 7.0
	 */
	public ProjectContext getProjectContext(){
		return __context;
	}
	
	/**
	 * @param name the name of device
	 */
	final void setName(final String name){
		super.name = name;
	}
	
	String[] connectedDevices;
	
	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	final void __startup(){
		try{
			connectedDevices = connect();//in user thread group
		}finally{
			if(isUseCmdGroup){
				final SessionContext sessionContext = ServerUIAPIAgent.getProjResponserMaybeNull(__context).getSessionContextFromCurrThread();
				if(sessionContext == null || sessionContext.j2seSocketSession == null){
					if(L.isInWorkshop){
						LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
					}
					return;
				}
				final J2SESession coreSS = sessionContext.j2seSocketSession;
				
				MSBAgent.clearWiFiAccountGroup(coreSS, this, getCmdGroup());
				isUseCmdGroup = false;
			}
		}
	}
	
	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	final void __shutdown(){
		disconnect();//in user thread group
		synchronized (this) {
			this.notifyAll();
		}
	}
	
	/**
	 * get a free {@link Message} from recycling message pool.
	 * @param from_dev_id the real device ID that the message is dispatched <STRONG>from</STRONG>. 
	 * <br>The message may be converted by {@link Converter} or not depends on binding (after active current project).
	 * @return a free {@link Message}
	 */
	protected Message getFreeMessage(final String from_dev_id) {//不能去掉，因为生成API时，由于没有附加生成Processor
		return super.getFreeMessageInProc(from_dev_id);
	}

	/**
	 * dispatch {@link Message} to robot(s).
	 * <br><br><strong>the <code>msg</code> can't be dispatched more than one time.</strong>
	 * <br><br>to print log of creating/converting/transferring/recycling of message, please enable [Option/Developer/log MSB message].
	 * @param msg the {@link Message} will be dispatched to robot(s). To get instance of the <code>msg</code>, call {@link #getFreeMessage(String)}.
	 * @param isInitiative 
	 * <br>if true, initiative to report status of {@link Device} to robot(s). If the device ID is binded to multiple robots, the <code>msg</code> will be cloned and dispatched to all these robots (NOT deep clone. copy keys of property/body and the corresponding values).
	 * <br>if false, answer passively, passive mode is normally used in method {@link Device#response(Message)}, the <code>msg</code> will be dispatched ONLY to the request robot, even if the real device ID is binded to multiple <i>Reference Device ID</i>.
	 * @see Robot#dispatch(Message, boolean)
	 * @since 7.0
	 */
	protected void dispatch(final Message msg, final boolean isInitiative){
		msg.ctrl_isInitiative = isInitiative;
		
		if(isInitiative){
			final HashMap<String, HashMap<String, Vector<String>>> hashMap = workbench//for debug
					.nameMapper
					.searchBindIDFromDevice
					.get(project_id);
			if(hashMap == null){
				//注意：以下不能用workbench.log
				LogManager.warning("{" + project_id + "/" + name + "} is never used, skip dispatch initiative message.");
				return;
			}
			msg.ctrl_bind_ids = hashMap
					.get(name)
					.get(msg.ctrl_dev_id);
			//注意：要置于dispatch之前
			workbench.V = workbench.O ? false : workbench.log("{" + project_id + "/" + name + "} dispatch initiatively : " + msg.toString());
			workbench.dispatch(msg, false, procType);
		}else{
			msg.ctrl_bind_id = preMsg.ctrl_bind_id;
			msg.ctrl_dev_id = workbench.nameMapper.bind2ReferID.get(msg.ctrl_bind_id).refer_id;
			//注意：要置于__response之前
			workbench.V = workbench.O ? false : workbench.log("{" + project_id + "/" + name + "} dispatch passively : " + msg.toString());
			super.__response(msg, false);
		}
	}
	
	/**
	 * return an instance of {@link DeviceCompatibleDescription} about this device.
	 * @return
	 */
	public abstract DeviceCompatibleDescription getDeviceCompatibleDescription();

//	/**
//	 * <BR>this method is invoked in {@link #connect()} to broadcast encrypted WiFi account for device(s) ready to added to network.
//	 * <BR>if this device will not connect via WiFi, please ignore this method.
//	 * <BR><BR><STRONG>important : </STRONG>you must override this method for encrypt the WiFi account and broadcast to air.
//	 * <BR><BR>there are two cases to broadcast encrypt data :
//	 * <BR>1. {@link #broadcastWiFiAccountAsSSID(String, String)}
//	 * <BR>2. {@link #createWiFiMulticastStream(String, int)}
//	 * <BR><BR>For more about configuration WiFi account automatically from server, see {@link WiFiAccount}.
//	 * @see #broadcastWiFiAccountAsSSID(String, String)
//	 * @see #createWiFiMulticastStream(String, int)
//	 * @see #getWiFiAccount()
//	 * @see {@link WiFiAccount}
//	 * @since 7.0
//	 */
//	public void broadcastWiFiAccount(){
//		//方法名被模板使用，如重构改名，请同步
//	}
	
//	/**
//	 * broadcast encrypted WiFi account via SSID to air for device ready to added to network.
//	 * <BR><BR>for example : <STRONG>prefix</STRONG> is <i>AIRCOND</i>, <STRONG>encryptedCommand</STRONG> is <i>ssid:abc#efg;pwd:1234567890;security:none</i><BR>
//	 * after invoke, following SSID will be scanned on air : <BR><BR>
//	 * <i>AIRCOND56#1#ssid:abc#efg;pwd:123</i><BR>
//	 * <i>AIRCOND56#2#4567890;security:WPA</i><BR>
//	 * <i>AIRCOND56#3#2_PSK</i><BR>
//	 * <STRONG>Important : </STRONG>it is split to three parts, because of the length of an SSID should be a maximum of 32.
//	 * <BR><i>56</i> is random integer (<100) for prevent conflicts.
//	 * <BR>to decode from commands, user data is stored from the <STRONG>second</STRONG> '#', 
//	 * if same prefix "AIRCOND" with other random integer is displayed also, please choose stronger signal.
//	 * <BR><BR>these SSID(s) will be removed automatically when caller exits from {@link #connect()}.
//	 * <BR><BR>if you want to broadcast special format commands, please invoke {@link #broadcastWiFiAccountAsSSID(String[])}.
//	 * <BR><BR>the WiFi equipment of server or mobile is driven to broadcast these data.
//	 * <BR><BR>For more about AirCmds to configuration automatically from server, see {@link WiFiAccount}.
//	 * @param prefix char '#' is not allowed in prefix, because it is split char for index.
//	 * @param encryptedCommand
//	 * @see #broadcastWiFiAccountAsSSID(String[])
//	 * @see #getWiFiSSIDListOnAir()
//	 * @see {@link WiFiAccount}
//	 * @since 7.0
//	 */
//	public final void broadcastWiFiAccountAsSSID(final String prefix, final String encryptedCommand){
//		//方法名被模板使用，如重构改名，请同步
//		if(prefix.indexOf("#") > 0){
//			throw new Error("char # is deprecated in AirCmds, error prefix : " + prefix);
//		}
//		if(prefix.startsWith(AirCmdsUtil.PREFIX_AIRCMDS_HAR_URL)){
//			throw new Error("char @ is deprecated in AirCmds, error prefix : " + prefix);
//		}
//		final String[] existsCmds = MSBAgent.getWiFiSSIDListOnAir(this);
//		final String[] cmds = splitCommands(prefix, encryptedCommand, existsCmds);
//		broadcastWiFiAccountAsSSID(cmds);
//	}
	
//	/**
//	 * broadcast WiFi account as SSID using exclusive and special format.
//	 * <BR><BR>for example : commands are <code>{"WiFi_SSID:abc", "WiFi_PWD:123321", "WiFi_SECURITY:WEP"}</code> 
//	 * <BR><BR>after invoke, there are three SSIDs like above is on air.
//	 * <BR><BR>to broadcast standard format WiFi account, please invoke {@link #broadcastWiFiAccountAsSSID(String, String)}.
//	 * <BR><BR>these SSID(s) will be removed automatically when caller exits from {@link #connect()}.
//	 * @param cmds each item will be a WiFi SSID, please keep the length of each item no more than 32.
//	 * @see #broadcastWiFiAccountAsSSID(String, String)
//	 * @see #getWiFiSSIDListOnAir()
//	 * @since 7.0
//	 */
//	private final void broadcastWiFiAccountAsSSID(final String[] cmds){
//		MSBAgent.broadcastWiFiAccountAsSSID(project_id, this, cmds, getCmdGroup());
//	}
	
//	/**
//	 * when new {@link WiFiAccount} is created, the old WiFI account is still valid for keep connection.
//	 * <BR>the manager of server inputs new WiFI account from dialog, then this method of all {@link Device} of active HAR project will be invoked.
//	 * <BR><BR>what you should do is just like following:
//	 * <BR>1. overrides this empty method.
//	 * <BR>2. notify the real device to change WiFi account via the old alive connection.
//	 * <BR>3. close and release old connection.
//	 * <BR>4. make new connect based on new WiFi account.
//	 * @param newAccount
//	 */
//	public void notifyNewWiFiAccount(final WiFiAccount newAccount){
//		如果开启此功能，请务必打开调用此此逻辑的处
//	}
	
	boolean isUseCmdGroup;
	
	private final String getCmdGroup(){
		isUseCmdGroup = true;
		return project_id + "/" + getName();
	}
	
	private final boolean checkExist(final String pretype, final String[] existsCmds){
		if(existsCmds != null){
			for (int i = 0; i < existsCmds.length; i++) {
				if(pretype.startsWith(existsCmds[i])){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 返回如"pretype{random}#{index}#cmds"
	 * @param pretype
	 * @param commands
	 * @param existsCmds
	 * @return such as "pretype{random}#{index}#cmds"
	 */
	private final String[] splitCommands(final String pretype, final String commands, final String[] existsCmds){
		final Random r = new Random();
		String newpretype = pretype + r.nextInt(100) + "#";
		int count = 0;
		while(checkExist(newpretype, existsCmds)){
			count++;
			newpretype = pretype + r.nextInt(count<100?100:(count<1000?1000:10000)) + "#";
		}
		
		final char[] chars = commands.toCharArray();
		final Vector<String> out = new Vector<String>();
		
		int offset = 0;
		int charLen = chars.length;
		char indx_char = '1';
		while(offset < chars.length){
			final String tryOneCommand = newpretype + indx_char + "#" + new String(chars, offset, charLen);
			try{
				final byte[] utfBS = tryOneCommand.getBytes("UTF-8");
				if(utfBS.length > 32){
					charLen--;
				}else{
					out.add(tryOneCommand);
					offset += charLen;
					charLen = chars.length - offset;
					indx_char++;
				}
			}catch (final Exception e) {
			}
		}
		
		final String[] outtype = {};
		return out.toArray(outtype);
	}
	
//	/**
//	 * create OutputStream to multicast encrypted WiFi account to air for device ready to added to network.
//	 * 	<BR><BR>if there is no WiFi module on server (but there is a WiFi router), 
//	 * please connect to server from mobile via WiFi first, 
//	 * server will broadcast WiFi account via your mobile.
//	 * <BR>it is a good choice that you add HAR project from mobile by QRcode, not from server.
//	 * <BR>because the WiFi module of server or mobile is driven to broadcast.
//	 * @param multicastIP for example, 224.X.X.X or 239.X.X.X
//	 * @param port
//	 * @see #broadcastWiFiAccountAsSSID(String[], int)
//	 * @see #listenFromWiFiMulticast(String, int)
//	 * @see {@link WiFiAccount}
//	 * @since 7.0
//	 */
//	public final OutputStream createWiFiMulticastStream(final String multicastIP, final int port){
//		//方法名被模板使用，如重构改名，请同步
//		return MSBAgent.createWiFiMulticastStream(this, multicastIP, port);
//	}
	
//	/**
//	 * before a new device connect to server, invoke this method to get WiFi account.
//	 * <BR>WiFi account may be set in Android Server or J2SE server with WiFi router which your mobile is connecting.
//	 * <BR>For more about configuration automatically from server, see {@link WiFiAccount}.
//	 * <BR><BR><STRONG>Important : </STRONG>if there is no WiFi module to broadcast WiFi account or the client is connect to server via browser (which can not drive WiFi module), it will returns null.
//	 * @return return null, if there is no WiFi module to drive.
//	 * @see {@link WiFiAccount}
//	 * @since 7.0
//	 */
//	public final WiFiAccount getWiFiAccount(){
//		//方法名被模板使用，如重构改名，请同步
//		return MSBAgent.getWiFiAccount(this, __context);
//	}
	
//	/**
//	 * return SSID on air as string array.
//	 * <BR><BR><STRONG>Important : </STRONG>it is sorted by signal level from strong to weak.
//	 * @return
//	 * @since 7.0
//	 * @see #broadcastWiFiAccountAsSSID(String, String)
//	 */
//	public final String[] getWiFiSSIDListOnAir(){
//		return MSBAgent.getWiFiSSIDListOnAir(this);
//	}
	
	/**
	 * log an error message to log system.
	 * @param logMessage
	 * @see #log(String)
	 * @see #reportStatus(boolean, int, String)
	 * @since 7.0
	 */
	public void error(final String logMessage){
		__context.error(logMessage);
	}
	
	/**
	 * log message to log system.
	 * @param msg
	 * @see #error(String)
	 * @see #reportStatus(boolean, int, String)
	 * @since 7.0
	 */
	public void log(final String msg){
		__context.log(msg);
	}
	
//	/**
//	 * listen from WiFi multicast.
//	 * <BR><BR>if there is no WiFi module on server (but there is a WiFi router), 
//	 * please connect to server from mobile via WiFi first, 
//	 * server will broadcast WiFi account via your mobile.
//	 * <BR>it is a good choice that you add HAR project from mobile by QRcode, not from server.
//	 * <BR>because the WiFi module of server or mobile is driven to listen.
//	 * @param multicastIP
//	 * @param port
//	 * @return if fail return null
//	 * @see #createWiFiMulticastStream(String, int)
//	 * @since 7.0
//	 */
//	public final InputStream listenFromWiFiMulticast(final String multicastIP, final int port){
//		return MSBAgent.listenFromWiFiMulticast(this, multicastIP, port);
//	}
	
	/**
	 * report status of this device to monitor of server (not to client). 
	 * <BR><BR>the message will be logged.
	 * <BR><BR>this method is invoked when an error is raised or device is turned to OK from error.
	 * <BR>it is NOT required if there is no error on device.
	 * @param isOK true : device turn to OK from error; false : device turn to error from OK.
	 * @param statusCode the error/status code, 0 means OK. if unknown, set it to -1.
	 * @param message the description of status message, the project name and device name is not required.
	 * @since 7.0
	 */
	public void reportStatus(final boolean isOK, final int statusCode, final String message){
		if(isOK){
			if(message == null || message.length() == 0){
				final String okmessage = "device [" + project_id + "/" + name + "] is OK";
				LogManager.info(okmessage);
				LogManager.log(okmessage);
			}else{
				final String okmessage = "[device] " + message + " [" + project_id + "/" + name + "]";
				LogManager.info(okmessage);
				LogManager.log(okmessage);
			}
		}else{
			final String fullMessage = "[device] " + message + " [" + project_id + "/" + name + "/" + statusCode + "]";
			LogManager.err(fullMessage);
		}
	}
	
	/**
	 * connect to the real device(s). One or more real devices may be managed by this instance.
	 * <br><br>this method is invoked by server <STRONG>before</STRONG> {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}.
	 * <br><br>if this device will initiative publish status, please put the task in {@link ProjectContext#run(Runnable)} and the task should be finished when {@link #disconnect()}.
	 * <br><BR><STRONG>Warning : </STRONG>this method will <STRONG>block</STRONG> thread of HomeCenter server. 
	 * <BR><BR>
	 * About binding
	 * <BR>1. before binding, please turn on this/these same model real devices first.
	 * <BR>2. this method is invoked by server before binding, and it returns an array of IDs of real devices of this same model.
	 * <BR>3. user may be required to input token of device, see {@link ProjectContext#showInputDialog(String, String[], String[])},
	 * <BR>4. to save the token of device, see {@link ProjectContext#saveProperties()}.
	 * @return the string array of real devices ID of the same model of device.
	 * @see #disconnect()
	 * @since 7.0
	 */
	public abstract String[] connect();
	
	//注意：如果开启，请将下注释添加到最后一行
//	 * <br>to auto set WiFi account for first connection of new devices, please use {@link #broadcastWiFiAccountAsSSID(String, String)}
	
	/**
	 * disconnect the real device(s).
	 * <br><br>this method is invoked by server <STRONG>after</STRONG> {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}
	 * @see #connect()
	 * @since 7.0
	 */
	public abstract void disconnect();
	
	/**
	 * return the name of device.
	 * @return
	 * @since 7.0
	 */
	final String getName(){
		return name;
	}
	
	/**
	 * return the description of this device.
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
	
}
