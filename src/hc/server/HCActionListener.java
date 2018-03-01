package hc.server;

import hc.core.ContextManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class HCActionListener implements ActionListener, ItemListener {
	final Runnable run;
	final ThreadGroup token;

	/**
	 * 缺省token为null，表示系统级应用
	 * 
	 * @param run
	 */
	public HCActionListener(final Runnable run) {
		this(run, null);
	}

	public HCActionListener(final Runnable run, final ThreadGroup token) {
		this.run = run;
		this.token = token;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (run instanceof ActionListenerRun) {
			((ActionListenerRun) run).event = e;
		}
		ContextManager.getThreadPool().run(run, token);
	}

	@Override
	public void itemStateChanged(final ItemEvent e) {
		ContextManager.getThreadPool().run(run, token);
	}

}
