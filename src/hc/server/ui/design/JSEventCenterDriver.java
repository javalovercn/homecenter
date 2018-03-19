package hc.server.ui.design;

import hc.core.ContextManager;
import hc.core.EventCenterDriver;
import hc.core.HCTimer;
import hc.core.IWatcher;
import hc.core.util.ExceptionReporter;
import hc.core.util.LinkedSet;
import hc.core.util.RecycleThread;

public class JSEventCenterDriver extends EventCenterDriver {
	private final LinkedSet jsEventSet = new LinkedSet();
	private RecycleThread processingThread;

	@Override
	public void notifyShutdown() {
		super.notifyShutdown();

		synchronized (jsEventSet) {
			jsEventSet.notifyAll();
		}
	}

	final Thread dispatcher = new Thread("JS_EVENT_DISPATCHER") {
		@Override
		public void run() {
			IWatcher item;

			while (isNotifyShutdown == false) {
				synchronized (jsEventSet) {
					item = (IWatcher) jsEventSet.getFirst();
					if (item == null) {
						try {
							jsEventSet.wait();
						} catch (final InterruptedException e) {
						}
						continue;
					}
				}

				final IWatcher finalItem = item;
				processingThread = ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						try {
							finalItem.watch();
						} catch (final Throwable e) {
							ExceptionReporter.printStackTrace(e);
						}
						synchronized (finalItem) {
							finalItem.notify();
						}
					}
				});

				if (processingThread == null) {
					break;
				}

				while (true) {
					synchronized (finalItem) {
						try {
							finalItem.wait(HCTimer.HC_INTERNAL_MS);
						} catch (final InterruptedException e) {
						}
					}

					if (processingThread.isIdle() || processingThread.getThread().getState() == State.WAITING) {
						break;
					}
				}
			}
		}
	};

	public JSEventCenterDriver() {
		dispatcher.start();
	}

	@Override
	protected final void processingItem(final IWatcher temp) {
		synchronized (jsEventSet) {
			jsEventSet.addTail(temp);
			jsEventSet.notify();
		}

		// synchronized (temp) {
		// try{
		// if(temp.watch() == false){
		// synchronized (watchers) {
		// if(isNotifyShutdown == false){//shutdown时，关闭长时间任务
		// usedRewatchers.addTail(temp);
		// isAddUnUsed = true;
		// }
		// }
		// }
		// }catch (final Throwable e) {//异常不会返回true，导致永久执行
		// LogManager.errToLog("Error IWatcher class : " + temp.getClass());
		// ExceptionReporter.printStackTrace(e);
		// }
		// }
		// if(isNotifyShutdown && isInWorkshop){
		// LogManager.log("[" + timeName + "] processed a watcher : " +
		// temp.hashCode());
		// }
	}
};
