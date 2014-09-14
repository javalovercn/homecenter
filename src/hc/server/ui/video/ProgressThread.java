package hc.server.ui.video;

import javax.media.Processor;

public class ProgressThread extends Thread {
	public static final String ACTION_STOP = "stop";
	public static final String ACTION_PAUSE = "pause";
	public static final String ACTION_RESUME = "resume";
	public static final String ACTION_RECORD = "record";

	private Processor processor;
	private boolean boolTerminate = false;
	private boolean boolSuspended = false;

	public ProgressThread(Processor processor) {
		this.processor = processor;
	}

	public synchronized void terminateNormaly() {
		this.boolTerminate = true;
		try {
			super.interrupt();
		} catch (Exception ex) {
		}
	}

	public synchronized void pauseThread() {
		this.boolSuspended = true;
	}

	public synchronized void resumeThread() {
		this.boolSuspended = false;
		notify();
	}

	public void run() {
		this.boolTerminate = false;
		while ((!(this.boolTerminate)) && (!(super.isInterrupted())))
			try {
				Thread.sleep(200L);
				if (this.boolSuspended == true)
					synchronized (this) {
						while (this.boolSuspended)
							wait();
					}

				int nPos = (int) this.processor.getMediaTime().getSeconds();
			} catch (Exception exception) {
				this.boolTerminate = true;
				return;
			}
	}
}