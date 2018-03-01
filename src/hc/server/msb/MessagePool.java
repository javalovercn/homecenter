package hc.server.msb;

import hc.core.util.Stack;

public final class MessagePool {
	private final Stack vector = new Stack(128);

	public final Message getFreeMessage() {
		Message out;

		synchronized (vector) {
			out = (Message) vector.pop();
		}

		if (out == null) {
			out = new Message();
			out.clear();
		}

		out.ctrl_cycle_get_thread_id = Thread.currentThread().getId();
		return out;
	}

	public final void recycle(final Message msg, final Workbench workbench) {
		workbench.V = workbench.O ? false
				: workbench.log("success recycle message :" + msg.toString());

		msg.clear();

		// if(vector.contains(msg)){
		// App.showCenterMessage("error recycle object, which is exists in
		// pool!");
		// }

		synchronized (vector) {
			vector.push(msg);
		}
	}

}