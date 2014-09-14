package hc.server.ui;

public class ServCtrlCanvas implements ICanvas {
	final CtrlResponse cr;
	public ServCtrlCanvas(CtrlResponse cr) {
		this.cr = cr;
	}
	
	@Override
	public void onStart() {
		cr.onLoad();
	}

	@Override
	public void onPause() {
	}

	@Override
	public void onResume() {
	}

	@Override
	public void onExit() {
		cr.onExit();
	}

}
