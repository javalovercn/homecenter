package hc.server.ui;

import hc.server.MultiUsingManager;
import hc.server.ui.design.J2SESession;

public class ServCtrlCanvas implements ICanvas {
	final CtrlResponse cr;
	final J2SESession coreSS;
	String screenID;
	
	public ServCtrlCanvas(final J2SESession coreSS, final CtrlResponse cr) {
		this.coreSS = coreSS;
		this.cr = cr;
	}
	
	public final String getScreenID(){
		if(screenID == null){
			final ProjectContext ctx = ServerUIAPIAgent.getProjectContextFromCtrl(cr);
			screenID = ServerUIAPIAgent.buildScreenID(ctx.getProjectID(), cr.target);
		}
		return screenID;
	}
	
	@Override
	public void onStart() {
		final ProjectContext ctx = ServerUIAPIAgent.getProjectContextFromCtrl(cr);
		ServerUIAPIAgent.runInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(ctx), new Runnable() {
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
		final ProjectContext ctx = ServerUIAPIAgent.getProjectContextFromCtrl(cr);
		ServerUIAPIAgent.runInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(ctx), new Runnable() {
			@Override
			public void run() {
				cr.onExit();
			}
		});
		MultiUsingManager.exit(coreSS, ServerUIAPIAgent.buildScreenID(ctx.getProjectID(), cr.target));
	}

	@Override
	public final String toString(){
		return cr.target;
	}
}
