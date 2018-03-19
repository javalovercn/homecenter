package hc.server.ui.design.hpj;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.LogManager;
import hc.server.ui.design.Designer;
import hc.server.ui.design.code.CodeHelper;

import java.awt.Point;

import javax.swing.text.AbstractDocument;

public class MouseMovingTipTimer extends HCTimer {
	int x, y, lastShowX, lastShowY;
	final HCTextPane jtaScript;
	final AbstractDocument jtaDocment;
	final int fontHeight;
	CodeHelper codeHelper;
	final ScriptEditPanel scriptPanel;
	long setLocMS, lastShowMS;

	public final void setLocation(final int x, final int y) {
		setLocMS = System.currentTimeMillis();

		this.x = x;
		this.y = y;
		resetTimerCount();
	}

	static final int interMS = 600;
	static final int interDirtyMS = interMS - 200;

	public MouseMovingTipTimer(final ScriptEditPanel scriptPanel, final HCTextPane jtaScript, final AbstractDocument jtaDocment,
			final int fontHeight) {
		super("MouseMovingTipTimer", interMS, false);// 由原来的1000=>600
		if (L.isInWorkshop) {
			LogManager.log("create MouseMovingTipTimer");
		}

		this.jtaScript = jtaScript;
		this.jtaDocment = jtaDocment;
		this.fontHeight = fontHeight;
		this.scriptPanel = scriptPanel;
	}

	@Override
	public void doBiz() {
		final Designer designer = scriptPanel.designer;

		if (designer.isNeedLoadThirdLibForDoc && designer.isLoadedThirdLibsForDoc == false) {
			L.V = L.WShop ? false : LogManager.log("waiting for load third libs...");
			return;
		}

		if (codeHelper == null) {
			codeHelper = scriptPanel.designer.codeHelper;
		}

		synchronized (ScriptEditPanel.scriptEventLock) {
			setEnable(false);

			if (System.currentTimeMillis() - interDirtyMS > setLocMS) {// 防止time已启动，但是事件又更新，导致eventPoint为脏数据
			} else {
				return;
			}

			final Point eventPoint = new Point(x, y);
			try {
				final int caretPosition = jtaScript.viewToModel(eventPoint);// 脚本正在绘制时
				if (caretPosition < 0) {
					return;
				}

				final boolean isOn = codeHelper.mouseMovOn(scriptPanel, jtaScript, jtaDocment, fontHeight, true, caretPosition);
				if (isOn) {
					lastShowX = x;
					lastShowY = y;
					lastShowMS = System.currentTimeMillis();
				}
			} catch (final Exception ex) {
				// 比如java.awt.IllegalComponentStateException: component must be
				// showing on the screen to determine its location
				// ex.printStackTrace();
			}
		}
	}

}
