package hc.server.ui;

import hc.core.util.UIUtil;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class DialogHTMLMlet extends HTMLMlet {
	private static final long serialVersionUID = 1270832156284865606L;

	public DialogGlobalLock resLock;
	public DialogProcessedChecker checker;
	public Dialog dialog;

	public final void setDialogGlobalLock(final DialogGlobalLock resLock) {
		this.resLock = resLock;
		checker = new DialogProcessedChecker(resLock);
	}

	public final void addDialog(final Dialog dialog) {
		this.dialog = dialog;
		setLayout(new GridBagLayout());

		// final JPanel dialogContainer = new JPanel(new GridBagLayout());
		// {
		// final GridBagConstraints c = new GridBagConstraints();
		// c.anchor = GridBagConstraints.CENTER;
		// c.fill = GridBagConstraints.NONE;
		// c.insets = new Insets(10, 10, 10, 10);
		// add(dialogContainer, c);
		// }

		{
			final GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.NONE;

			enableApplyOrientationWhenRTL(dialog.enableApplyOrientationWhenRTL);

			add(dialog, c);
		}
		// dialog.setVisible(false);
		final int r = (UIUtil.DEFAULT_COLOR_BACKGROUND & 0xff0000) >> 16;
		final int g = (UIUtil.DEFAULT_COLOR_BACKGROUND & 0xff00) >> 8;
		final int b = UIUtil.DEFAULT_COLOR_BACKGROUND & 0xff;
		setCSS(this, null, "background-color:rgba(" + r + ", " + g + ", " + b + ", .8); filter : Alpha(opacity=80);");// 仍占据空间，background-color
																														// : transparent;
																														// setCSS(this, null, "background-color : # " + colorTrans + "; filter :
																														// Alpha(opacity=80);");
		final int borderRadius = dialog.getDialogBorderRadius();
		final int maxWH = Math.max(dialog.getMobileWidth(), dialog.getMobileHeight());
		final int shadowHV = Math.round(maxWH / 400f);
		final int blur = Math.round(maxWH / 80f);
		setCSS(dialog, null, "background-color : #" + HTMLMlet.getColorForBodyByHexString() + ";" + "overflow: hidden;" + // 部分早期手机不支持
				"-moz-border-radius: " + borderRadius + "px; -webkit-border-radius: " + borderRadius + "px; border-radius: " + borderRadius
				+ "px;" + // border: 1pt
																																						// solid
																																						// #000000;
				"filter:progid:DXImageTransform.Microsoft.Shadow(color=#000000, Direction=0, Strength=" + (shadowHV * 2) + ");" + /// *ie*/direction 阴影角度 0°为从下往上 顺时针方向,
																																	/// Strength Sets or retrieves the
																																	/// distance, in pixels, that a
																																	/// filter effect extends.
				"-moz-box-shadow: " + shadowHV + "px " + shadowHV + "px " + blur + "px #000000;" + /// *firefox*/
				"-webkit-box-shadow: " + shadowHV + "px " + shadowHV + "px " + blur + "px #000000;" + /// *safari或chrome*/
				"box-shadow:" + shadowHV + "px " + shadowHV + "px " + blur + "px #000000;");/// *opera或ie9*/
		// setCSS(dialog, null, "border: 2px solid #000000; -moz-border-radius:
		// 15px; -webkit-border-radius: 15px; border-radius:15px;");
	}

	public final boolean isContinueProcess() {
		return checker.isContinueProcess(coreSS);
	}
}
