package hc.core;

import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;

public class EventCenter {
	private final int event_listener_max_size = (IConstant.serverSide?1000:200);
	private final CoreSession coreSS;
	final private IEventHCListener[] listens = new IEventHCListener[event_listener_max_size];
	final private byte[] listen_types = new byte[event_listener_max_size];
	private int size = 0;
	
	public EventCenter(final CoreSession coreSS){
		this.coreSS = coreSS;
		coreSS.eventCenter = this;
	}
	
	public final void removeListener(IEventHCListener listener){
		throw new Error("remove Listener is disable, to simple the arch of event center.");
	}
	
	public final void addListener(IEventHCListener listener){
		boolean enableSameEventTag = listener.enableSameEventTag;
		
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
		if (nestAction != null){
			nestAction.action(ctrlTag, event);
			return;
		}
		
		int i = 0;
		final int searchSize = size;//注意：没有remove(listener)
		while(i < searchSize){
			IEventHCListener listener = null;
			for (; i < searchSize; i++) {
				if(listen_types[i] == ctrlTag){
					listener = listens[i];
					break;
				}
			}
			if(listener != null){
				try{
					if(listener.action(event, coreSS)){
						return;
					}
				}catch (Throwable e) {
					ExceptionReporter.printStackTrace(e);
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
	
	public final void notifyLineOff(final byte[] bs) {
		action(MsgBuilder.E_LINE_OFF_EXCEPTION, bs, nestAction);
	}

	public final NestAction nestAction = (NestAction)ConfigManager.get(ConfigManager.BUILD_NESTACTION, null);

}
