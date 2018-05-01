package hc.server.ui.design;

public class LinkPanePlugSource extends PlugSource {

	@Override
	public void sendMessage(final String msg) {
	}

	@Override
	public void sendError(final String error) {
	}

	@Override
	public void sendWarn(final String warn) {
	}

	@Override
	public J2SESession getJ2SESession() {
		return J2SESession.NULL_J2SESESSION_FOR_PROJECT;
	}

	@Override
	public void sendInitCheckActivingMessaeg() {
		//do nothing
	}

	@Override
	public void enterMobileBindInSysThread(final MobiUIResponsor mobiResp, final BindRobotSource bindSource) {
		//do nothing
		final DesktopDeviceBinderWizSource desktopSource = new DesktopDeviceBinderWizSource(bindSource, bindSource.respo);
		DeviceBinderWizard.enterBindUI(desktopSource, null, null);//理论如此，但实际不会访问
	}

}
