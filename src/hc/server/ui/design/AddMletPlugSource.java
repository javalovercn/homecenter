package hc.server.ui.design;

import java.util.Map;

import hc.core.IConstant;
import hc.server.ui.HTMLMlet;
import hc.server.ui.design.hpj.HCjar;
import hc.util.ResourceUtil;

public class AddMletPlugSource extends PlugSource {
	final AddHarHTMLMlet addHarMlet;
	final HTMLMlet mlet;
	final J2SESession coreSS;
	final Map<String, Object> map;
	
	public AddMletPlugSource(final AddHarHTMLMlet addHarMlet, final HTMLMlet mlet, final J2SESession coreSS, final Map<String, Object> map) {
		this.addHarMlet = addHarMlet;
		this.mlet = mlet;
		this.coreSS = coreSS;
		this.map = map;
	}
	
	@Override
	public void sendMessage(final String msg) {
		addHarMlet.appendMessage(msg);
	}

	@Override
	public void sendError(final String error) {
	}

	@Override
	public void sendWarn(final String warn) {
		final int msgType = IConstant.WARN;
		AddHarHTMLMlet.showMsgForAddHar(msgType, warn);
	}

	@Override
	public J2SESession getJ2SESession() {
		return coreSS;
	}

	@Override
	public void sendInitCheckActivingMessaeg() {
		AddHarHTMLMlet.showMsgForAddHar(IConstant.INFO, ResourceUtil.get(coreSS, 9092));//9092=check, initialize, start up HAR projects...
	}

	@Override
	public void enterMobileBindInSysThread(final MobiUIResponsor mobiResp, final BindRobotSource bindSource) {
		//进入手机的人工绑定界面
		if (addHarMlet.enterMobileBindInSysThread(bindSource, coreSS, mlet) == false) {
			final String errMsg = "NOT binded for robots for project [" + (String) map.get(HCjar.PROJ_ID) + "].";
			throw new Error(errMsg);
		}
	}

}
