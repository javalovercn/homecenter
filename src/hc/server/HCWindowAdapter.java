package hc.server;

import hc.core.ContextManager;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class HCWindowAdapter extends WindowAdapter {
	final Runnable run;
	final ThreadGroup token;
	
	public HCWindowAdapter(){
		run = null;
		token = null;
	}
	
	public HCWindowAdapter(final Runnable run, final ThreadGroup token){
		this.run = run;
		this.token = token;
	}
	
	public void windowClosing(WindowEvent e) {
		ContextManager.getThreadPool().run(run, token);
	}
}
