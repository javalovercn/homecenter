package hc.server;

import java.awt.event.ActionEvent;

public abstract class ActionListenerRun implements Runnable {
	public ActionEvent event;

	public final ActionEvent getActionEvent() {
		return event;
	}

}
