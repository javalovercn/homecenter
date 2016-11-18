package hc.server.ui;

import hc.server.MultiUsingManager;
import hc.server.ui.design.J2SESession;

public class ServCtrlCanvas implements ICanvas {
	final CtrlResponse cr;
	final J2SESession coreSS;
	
	public ServCtrlCanvas(final J2SESession coreSS, final CtrlResponse cr) {
		this.coreSS = coreSS;
		this.cr = cr;
	}
	
	@Override
	public void onStart() {
		ServerUIAPIAgent.runInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(cr.getProjectContext()), new Runnable() {
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
		ServerUIAPIAgent.runInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(cr.getProjectContext()), new Runnable() {
			@Override
			public void run() {
				cr.onExit();
			}
		});
		MultiUsingManager.exit(coreSS, cr.getProjectContext().getProjectID(), cr.target);
	}

}
