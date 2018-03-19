package hc.server.ui;

import hc.App;
import hc.core.IContext;
import hc.res.ImageSrc;
import hc.server.HCActionListener;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class SingleMessageNotify {
	public static final String TYPE_ERROR_CONNECTION = "ErrCon";
	public static final String TYPE_ERROR_PASS = "ErrPass";
	public static final String TYPE_ERROR_CERT = "ErrCert";
	public static final String TYPE_LOCK_CERT = "LockCert";
	public static final String TYPE_FORBID_CERT = "ForbidCert";
	public static final String TYPE_DIALOG_CERT = "DIA_Cert";
	public static final String TYPE_DIALOG_TRANS_OFF = "DIA_TRS_Off";
	public static final String TYPE_SCR_LOCKING = "SCR_LOCKING";
	public static final String TYPE_SAME_ID = "SAME_ID";

	public static final int NEVER_AUTO_CLOSE = 0;

	private static HashMap<String, Boolean> typeMessageStatus = new HashMap<String, Boolean>();
	private static HashMap<String, JDialog> typeDialogs = new HashMap<String, JDialog>();

	/**
	 * 
	 * @param type
	 * @param msg
	 * @param title
	 * @param disposeMS
	 *            指定时间后自动关闭；如果为{@link SingleMessageNotify#NEVER_AUTO_CLOSE}，表示须手工关闭
	 * @param icon
	 */
	public static final void showOnce(final String type, final String msg, final String title, final int disposeMS, final Icon icon) {
		synchronized (SingleMessageNotify.class) {
			if (SingleMessageNotify.isShowMessage(type) == false) {
				new SingleMessageNotify(type, msg, title, disposeMS, icon);
				SingleMessageNotify.setShowToType(type, true);
			}
		}
	}

	public static final boolean isShowMessage(final String type) {
		final Boolean isShow = typeMessageStatus.get(type);
		if (isShow == null) {
			return false;
		}
		return isShow.booleanValue();
	}

	public static final void setShowToType(final String type, final boolean isShow) {
		if (isShow) {
			typeMessageStatus.put(type, Boolean.TRUE);
		} else {
			typeMessageStatus.remove(type);
		}
	}

	/**
	 * 
	 * @param type
	 * @param msg
	 * @param title
	 * @param disposeMS
	 *            指定时间后自动关闭；如果为0，表示须手工关闭
	 * @param icon
	 */
	public SingleMessageNotify(final String type, final String msg, final String title, final int disposeMS, final Icon icon) {
		if (ResourceUtil.isNonUIServer()) {// Demo时，不显示UI界面
			return;
		}

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("<html><body>" + msg + "</body></html>", icon, SwingConstants.LEADING), BorderLayout.CENTER);
		final ActionListener quitAction = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				closeDialog(type);
			}
		}, App.getThreadPoolToken());
		final JButton okButton = new JButton((String) ResourceUtil.get(IContext.OK), new ImageIcon(ImageSrc.OK_ICON));
		final JDialog dialog = (JDialog) App.showCenterPanelMain(panel, 0, 0, title, false, okButton, null, quitAction, quitAction, null,
				false, false, null, false, false);
		typeDialogs.put(type, dialog);

		if (disposeMS > NEVER_AUTO_CLOSE) {
			// 由原CondWatcher改来，采用线程
			new Thread() {
				private final long startMS = System.currentTimeMillis();

				@Override
				public void run() {
					while (isShowMessage(type)) {
						try {
							// 必须置于前，因为有可能Message尚未显示。
							Thread.sleep(1000);
						} catch (final Exception e) {
						}

						final long diff = System.currentTimeMillis() - startMS;
						if (diff > disposeMS) {
							closeDialog(type);
							break;
						}
						okButton.setText((String) ResourceUtil.get(IContext.OK) + " ["
								+ (int) ((startMS + disposeMS - System.currentTimeMillis()) / 1000) + "]");
					}
				}
			}.start();
		}
	}

	public static void closeDialog(final String type) {
		synchronized (SingleMessageNotify.class) {
			final JDialog dialog = typeDialogs.get(type);

			if (dialog != null) {
				setShowToType(type, false);
				typeDialogs.put(type, null);
				dialog.dispose();
			}
		}
	}
}
