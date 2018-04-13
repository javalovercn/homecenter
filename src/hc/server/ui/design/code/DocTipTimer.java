package hc.server.ui.design.code;

import javax.swing.JFrame;

import hc.core.HCTimer;
import hc.core.L;
import hc.util.ResourceUtil;

public class DocTipTimer extends HCTimer {
	DocHelper docHelper;
	JFrame classFrame;
	CodeItem item;
	DocLayoutLimit layoutLimit;

	public DocTipTimer(final String name, final int ms, final boolean enable) {
		super(name, ms, enable);
	}

	@Override
	public final void doBiz() {
		synchronized (this) {// 与hide互斥
			if (isEnable) {// 重新检查条件，必须的
				final int type = item.type;
				
				if (type == CodeItem.TYPE_CLASS || type == CodeItem.TYPE_CLASS_IMPORT) {
					final String fieldOrMethodName = item.codeForDoc;
					final Class c = ResourceUtil.loadClass(fieldOrMethodName, L.isInWorkshop);
					if (c != null) {
						CodeHelper.buildForClass(docHelper.codeHelper, c);
					}
				}
				docHelper.popDocTipWindow(item, classFrame, layoutLimit);
				setEnable(false);
//				docHelper.codeHelper.window.scriptEditPanel.autoCodeTip.setEnable(false);//有可能mouseExit scriptEditPane，所以不能disable
			}
		}
	}
	
}
