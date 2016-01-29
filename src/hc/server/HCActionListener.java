package hc.server;

import hc.core.ContextManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class HCActionListener implements ActionListener, ItemListener {
	final Runnable run;
	final ThreadGroup token;
	ActionEvent event;
	
	public HCActionListener(){
		this(null, null);
	}
	
	public HCActionListener(final Runnable run, final ThreadGroup token){
		this.run = run;
		this.token = token;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		ContextManager.getThreadPool().run(run, token);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		ContextManager.getThreadPool().run(run, token);
	}
}
