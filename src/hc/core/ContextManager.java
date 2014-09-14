package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;

public class ContextManager {
	private static IContext instanceContext;
	public static short cmStatus;
	
	public static IContext getContextInstance(){
		return instanceContext;
	}
    public static void exit(){
    	if(instanceContext != null){
	    	setStatus(STATUS_EXIT);
    	}
    }
    
//    private static boolean isSimu = false;
//    
//    public static void setSimulate(boolean s){
//    	isSimu = s;
//    }
//    
//    public static boolean isSimulate(){
//    	return isSimu;
//    }
    
    public static void setContextInstance(IContext ib){
    	instanceContext = ib;
    	setStatus(STATUS_NEED_NAT);
    }
    
    public static void notifyShutdown(){
    	instanceContext.notifyShutdown();
    }

    
    public static void displayMessage(String caption, String text, int type, int timeOut){
    	if(instanceContext != null){
    		instanceContext.displayMessage(caption, text, type, null, 0);
    	}
    }
		
	public static void start() {
		instanceContext.run();	
	}

	public static void shutDown(){
		if(instanceContext != null){
			instanceContext.shutDown();	
		}
	}
	
	public static ReceiveServer getReceiveServer() {
		return instanceContext.getReceiveServer();
	}
	
	public static boolean isServerStatus(){
		final int[] servermode = {STATUS_SERVER_SELF};
		final int m = cmStatus;
		for (int i = 0; i < servermode.length; i++) {
			if(m == servermode[i]){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isClientStatus(){
		final int mode = cmStatus;
		if(mode == STATUS_CLIENT_SELF){
			return true;
		}
		return false;
	}
	
//	public static boolean isRelayServerStatus(){
//		int m = getStatus();
//		if(m == STATUS_SERVER_AND_RELAY){
//			return true;
//		}
//		return false;
//	}
	
//	public static boolean isOnRelayStatus(){
//		int m = getStatus();
//		if(m == STATUS_CLIENT_ON_RELAY || m == STATUS_SERVER_ON_RELAY){
//			return true;
//		}
//		return false;
//	}
	
	public static boolean isNotWorkingStatus(){
		final int[] notWorking = {STATUS_READY_TO_LINE_ON, STATUS_EXIT, STATUS_NEED_NAT, STATUS_LINEOFF};
		final int m = cmStatus;
		for (int i = 0; i < notWorking.length; i++) {
			if(m == notWorking[i]){
				return true;
			}
		}
		return false;
	}
	
	public static final short STATUS_LINEOFF = 0;
	public static final short STATUS_NEED_NAT = 1;//尚未NAT
	public static final short STATUS_READY_TO_LINE_ON = 2;//空闲状态，暂无客户服务
	public static final short STATUS_READY_MTU = 3;//打通，但是尚未进行MTU
//	public static final short STATUS_READY_FOR_CLIENT = 4;//MTU后，但是尚未验证客户
	public static final short STATUS_SERVER_SELF = 5;
	public static final short STATUS_CLIENT_SELF = 6;
	public static final short STATUS_EXIT = 7;

	public static IStatusListen statusListen;
	
	public static final short MODE_CONNECTION_NONE = 0;
	public static final short MODE_CONNECTION_HOME_WIRELESS = 1;
	public static final short MODE_CONNECTION_PUBLIC_DIRECT = 2;
	public static final short MODE_CONNECTION_PUBLIC_UPNP_DIRECT = 3;
	public static final short MODE_CONNECTION_RELAY = 4;
	
	private static short modeStatus = MODE_CONNECTION_NONE;
	
	public static void setConnectionModeStatus(final short modeStat){
		modeStatus = modeStat;
	}
	
	public static short getConnectionModeStatus(){
		return modeStatus;
	}
	
	public static void setStatus(short mode){
		hc.core.L.V=hc.core.L.O?false:LogManager.log("Change Status, From [" + cmStatus + "] to [" + mode + "]");
		if(statusListen != null){
			statusListen.notify(cmStatus, mode);
		}
		
		if(mode == ContextManager.STATUS_LINEOFF){
			modeStatus = MODE_CONNECTION_NONE;
		}
		
		if(cmStatus == mode && mode == ContextManager.STATUS_EXIT){
			forceExit();
		}
		
		cmStatus = mode;
		
		if(mode == STATUS_EXIT){
			//没有置null的必要，而且在退出shutdown逻辑中，该值仍被使用，
			//参见J2MESendServer循环中break
//			instanceContext = null;
			
			return;
		}
		
		if(mode == ContextManager.STATUS_READY_MTU){
			if(IConstant.serverSide){
				try{
					//服务器稍等，提供客户初始化时间
					Thread.sleep(200);
				}catch (Exception e) {
				}
			}
			
//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Do biz after Hole");
			
			//激活KeepAlive hctimer
			ContextManager.getContextInstance().doExtBiz(IContext.BIZ_AFTER_HOLE, null);
			
//			if(IConstant.serverSide){
//			}else{
//			}
		}

//		if(mode == ContextManager.STATUS_SERVER_SELF){
//			ContextManager.getContextInstance().doExtBiz(IContext.BIZ_IS_ON_SERVICE);
//		}
		
//		if(mode == ContextManager.STATUS_READY_TO_LINE_ON){
//			hc.core.L.V=hc.core.L.O?false:LogManager.log("NO biz for status READY_TO_LINE_ON");
//		}
		
//		if(ContextManager.isClientStatus()){
//		}
		
//		if(ContextManager.isClientStatus() && ContextManager.isNotWorkingStatus() == false){
//			ScreenClientManager.init();
//		}		
	}
	
	public static void forceExit() {
		LogManager.exit();
		CCoreUtil.globalExit();
	}
	
	public static byte[] cloneDatagram(byte[] randomBS){
		final byte[] event = new byte[MsgBuilder.UDP_BYTE_SIZE];
		
		System.arraycopy(randomBS, 0, event, 0, (event.length > randomBS.length)?randomBS.length:event.length);
		return event;
	}
}
