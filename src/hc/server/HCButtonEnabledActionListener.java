package hc.server;

import hc.App;
import hc.core.ContextManager;
import hc.core.IWatcher;

import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;

public class HCButtonEnabledActionListener extends HCActionListener {
	final AbstractButton button;
	final IWatcher watcher;
	
	/**
	 * 
	 * @param btn 运行run前，置btn为disable，运行后，置btn为false
	 * @param run
	 * @param token
	 */
	public HCButtonEnabledActionListener(final AbstractButton btn, final Runnable run, final ThreadGroup token){
		this(btn, run, token, null);
	}
	
	/**
	 * 
	 * @param btn 运行run前，置btn为disable，运行后，根据iwatcher情况，置btn为false
	 * @param run
	 * @param token
	 * @param iwatcher 如果非null，则运行run后，执行watch方法，watch返回true，将button进行enable；返回false，停止。
	 */
	public HCButtonEnabledActionListener(final AbstractButton btn, final Runnable run, final ThreadGroup token,
			final IWatcher iwatcher){
		super(run, token);
		this.button = btn;
		this.watcher = iwatcher;
	}
	
	@Override
	public final void actionPerformed(final ActionEvent e) {
		final Runnable wrapRun = new Runnable() {
			@Override
			public void run() {
				App.invokeLaterUI(new Runnable() {
					@Override
					public void run() {
						button.setEnabled(false);
					}
				});
				
				try{
					Thread.sleep(100);
					run.run();
				}catch (final Throwable e) {
					e.printStackTrace();
				}
				
				if(watcher != null){
					if(watcher.watch() == false){
						return;
					}
				}
				
				App.invokeLaterUI(new Runnable() {
					@Override
					public void run() {
						button.setEnabled(true);
					}
				});
			}
		};
		ContextManager.getThreadPool().run(wrapRun, token);
	}

}
