package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;

public class EventCenter {
//	private final static DatagramPacketCacher packetCacher = DatagramPacketCacher.getInstance();
	private static final int EVENT_LISTENER_MAX_SIZE = (IConstant.serverSide?1000:100);
	
	final private static IEventHCListener[] listens = new IEventHCListener[EVENT_LISTENER_MAX_SIZE];
	final private static byte[] listen_types = new byte[EVENT_LISTENER_MAX_SIZE];
	private static int size = 0;
	
	public static void addListener(IEventHCListener listener){
		CCoreUtil.checkAccess();
		
		boolean enableSameEventTag = listener.enableSameEventTag;
		
		synchronized (listens) {
			if(size == EVENT_LISTENER_MAX_SIZE){
				LogManager.err("EventCenter over size listers : " + EVENT_LISTENER_MAX_SIZE);
				return;
			}
			byte eventTag = listener.getEventTag();
			for (int i = 0; i < size; i++) {
				if(listen_types[i] == eventTag){
					if(enableSameEventTag){
						hc.core.L.V=hc.core.L.O?false:LogManager.log("EventTag:" + eventTag + ", register twice or more!");
					}else{
						//因为旧的可能含有不正确的数据，所以要以新的覆盖旧的。
						listens[i] = listener;
						hc.core.L.V=hc.core.L.O?false:LogManager.log("Rewrite EventTag:" + eventTag + " EventHCListener!");
						return;
					}
				}
			}
			listens[size] = listener;
			listen_types[size++] = eventTag;
		}
	}
	
//	由于remove导致不可预知的问题，比如在action返回false时，导致消息被重复执行。所以关闭
//	public static void remove(IEventHCListener listener){
//		if(listener == null){
//			return;
//		}
//		
//		synchronized (listens) {
//			for (int i = 0; i < size; i++) {
//				if(listens[i] == listener){
//					for (int j = i, endIdx = size - 1; j < endIdx; ) {
//						listens[j] = listens[j + 1];
//						listen_types[j] = listen_types[++j];
//					}
//					size--;
//					return;
//				}
//			}
//		}
//	}
	
	//不回收
	static final void action(final byte ctrlTag, final byte[] event, final NestAction nestAction){
		if (nestAction != null){
			nestAction.action(ctrlTag, event);
			return;
		}
		
		int i = 0;
		final int searchSize = size;//注意：没有remove(listener)
		while(i < searchSize){
			IEventHCListener listener = null;
			for (; true; i++) {
				if(listen_types[i] == ctrlTag){
					listener = listens[i];
					break;
				}
			}
			if(listener != null){
				try{
					if(listener.action(event)){
						return;
					}
				}catch (Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
		}
		L.V = L.O ? false : LogManager.log("Unused HCEvent, [BizType:" + ctrlTag + "]");
		
//		if(event.isUseNormalBS == false){
//			event.releaseUseBlobBs();
//		}

//		packetCacher.cycle(event.data_bs);
//		eventCacher.cycle(event);
	}
	
	public static void notifyLineOff(final byte[] bs) {
		CCoreUtil.checkAccess();
		
		action(MsgBuilder.E_LINE_OFF_EXCEPTION, bs, nestAction);
	}

	public static final NestAction nestAction = (NestAction)ConfigManager.get(ConfigManager.BUILD_NESTACTION, null);

}
