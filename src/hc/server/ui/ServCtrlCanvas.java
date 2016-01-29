package hc.server.ui;

public class ServCtrlCanvas implements ICanvas {
	final CtrlResponse cr;
	public ServCtrlCanvas(CtrlResponse cr) {
		this.cr = cr;
	}
	
	@Override
	public void onStart() {
		cr.getProjectContext().run(new Runnable() {
			@Override
			public void run() {
				cr.onLoad();
			}
		});
	}

	@Override
	public void onPause() {
	}

	@Override
	public void onResume() {
	}

	@Override
	public void onExit() {
		cr.getProjectContext().run(new Runnable() {
			@Override
			public void run() {
				cr.onExit();
			}
		});
	}

}
