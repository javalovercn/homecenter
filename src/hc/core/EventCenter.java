package hc.core;

import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.RootBuilder;

public class EventCenter {
	private final int event_listener_max_size = (IConstant.serverSide?1000:200);
	private final CoreSession coreSS;
	private final HCConnection hcConnection;
	final private IEventHCListener[] listens = new IEventHCListener[event_listener_max_size];
	final private byte[] listen_types = new byte[event_listener_max_size];
	private int size = 0;
	private boolean isStop;
	
	public EventCenter(final CoreSession coreSS){
		this.coreSS = coreSS;
		this.hcConnection = coreSS.hcConnection;
		coreSS.eventCenter = this;
	}
	
	public final void removeListener(IEventHCListener listener){
		RootBuilder.getInstance().doBiz(RootBuilder.ROOT_BIZ_CHECK_STACK_TRACE, null);
		
		boolean isRemoved = false;
		
		synchronized (listens) {
			for (int i = 0; i < size; i++) {
				if(isRemoved == false && listens[i] == listener){
					L.V = L.WShop ? false : LogManager.log("sucessful remove EventHCListener [" + listener.getEventTag() + "].");
					isRemoved = true;
				}else if(isRemoved){
					final int backStep = i - 1;
					
					listens[backStep] = listens[i];
					listen_types[backStep] = listen_types[i];
				}
			}
			if(isRemoved){
				size--;
			}
		}
		
		if(isRemoved == false){
			L.V = L.WShop ? false : LogManager.log("fail to remove EventHCListener [" + listener.getEventTag() + "], NOT found.");
		}
	}
	
	public final void addListener(IEventHCListener listener){
		boolean enableSameEventTag = listener.enableSameEventTag;
		
		RootBuilder.getInstance().doBiz(RootBuilder.ROOT_BIZ_CHECK_STACK_TRACE, null);
		
		synchronized (listens) {
			if(size == event_listener_max_size){
				LogManager.err("EventCenter over size listers : " + event_listener_max_size);
				return;
			}
			byte eventTag = listener.getEventTag();
			for (int i = 0; i < size; i++) {
				if(listen_types[i] == eventTag){
					if(enableSameEventTag){
						LogManager.log("EventTag:" + eventTag + ", register twice or more!");
					}else{
						//因为旧的可能含有不正确的数据，所以要以新的覆盖旧的。
						listens[i] = listener;
						LogManager.log("Rewrite EventTag:" + eventTag + " EventHCListener!");
						return;
					}
				}
			}
			listens[size] = listener;
			listen_types[size++] = eventTag;
		}
	}
	
	public final void stop(){
		isStop = true;
	}
	
//	由于remove导致不可预知的问题，比如在action返回false时，导致消息被重复执行。所以关闭
//	public void remove(IEventHCListener listener){
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
	final void action(final byte ctrlTag, final byte[] event, final NestAction nestAction){
		if(isStop && ctrlTag != MsgBuilder.E_LINE_OFF_EXCEPTION){
			return;
		}
		
		if (nestAction != null){
			nestAction.action(ctrlTag, event);
			return;
		}
		
		int i = 0;
		synchronized (listens) {
			while(i < size){
				IEventHCListener listener = null;
				for (; i < size; i++) {
					if(listen_types[i] == ctrlTag){
						listener = listens[i];
						break;
					}
				}
				if(listener != null){
					try{
						if(listener.action(event, coreSS, hcConnection)){
							return;
						}
					}catch (Throwable e) {
						ExceptionReporter.printStackTrace(e);
					}
				}
			}
		}
		LogManager.log("Unused HCEvent, [BizType:" + ctrlTag + "]");
		
//		if(event.isUseNormalBS == false){
//			event.releaseUseBlobBs();
//		}

//		packetCacher.cycle(event.data_bs);
//		eventCacher.cycle(event);
	}
	
//	public final void notifyLineOff(final byte[] bs) {
//		action(MsgBuilder.E_LINE_OFF_EXCEPTION, bs, nestAction);
//	}

	public final NestAction nestAction = (NestAction)ConfigManager.get(ConfigManager.BUILD_NESTACTION, null);

}
