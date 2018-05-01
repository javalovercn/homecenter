package hc.server.ui.design.hpj;

import java.awt.Point;

import javax.swing.text.AbstractDocument;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.LogManager;
import hc.server.ui.design.Designer;
import hc.server.ui.design.code.CodeHelper;

public class MouseMovingTipTimer extends HCTimer {
	int x, y;
	final HCTextPane jtaScript;
	final AbstractDocument jtaDocment;
	final int fontHeight;
	CodeHelper codeHelper;
	long setLocMS, lastShowMS;
	public boolean isClearHistroyShow;
	public final void setLocation(final int x, final int y) {
		setLocMS = System.currentTimeMillis();

		this.x = x;
		this.y = y;
		resetTimerCount();
	}

	static final int interMS = 600;
	static final int interDirtyMS = interMS - 200;

	public MouseMovingTipTimer(final HCTextPane jtaScript, final AbstractDocument jtaDocment,
			final int fontHeight) {
		super("MouseMovingTipTimer", interMS, false);// 由原来的1000=>600
		if (L.isInWorkshop) {
			LogManager.log("create MouseMovingTipTimer");
		}

		this.jtaScript = jtaScript;
		this.jtaDocment = jtaDocment;
		this.fontHeight = fontHeight;
	}
	
	@Override
	public final void setEnable(final boolean enable) {
		super.setEnable(enable);
		if(enable == false) {
			isClearHistroyShow = false;//进入DocWindow时，需关闭此
		}
	}

	@Override
	public void doBiz() {
		if (codeHelper == null) {
			codeHelper = Designer.getInstance().codeHelper;
		}

		final boolean isClearHistroyShowSnap = isClearHistroyShow;
		
		synchronized (ScriptEditPanel.scriptEventLock) {
			setEnable(false);

			if(isClearHistroyShowSnap) {
				codeHelper.window.hide();
				return;
			}
			
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

				final boolean isOn = codeHelper.mouseMovOn(jtaScript, jtaDocment, fontHeight, true, caretPosition);
				if (isOn) {
					lastShowMS = System.currentTimeMillis();
				}
			} catch (final Throwable ex) {
				// 比如java.awt.IllegalComponentStateException: component must be
				// showing on the screen to determine its location
				// ex.printStackTrace();
			}
		}
	}

}
