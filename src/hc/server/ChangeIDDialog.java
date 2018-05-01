package hc.server;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import hc.App;
import hc.UIActionListener;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.res.ImageSrc;
import hc.server.ui.J2SESessionManager;
import hc.server.util.SafeDataManager;
import hc.server.util.VerifyEmailManager;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

public class ChangeIDDialog {
	private static Window changeIDWindow;

	public static synchronized void showChangeIDWindow() {
		if (changeIDWindow != null) {
			return;
		}
		
		String msg = ResourceUtil.get(9113);//9113=<html>帐号 [<STRONG>{uuid}</STRONG>] 正被其它服务器使用。<BR>点击 '{ok}' 更改帐号。</html>
		msg = StringUtil.replace(msg, "{uuid}", IConstant.getUUID());// html tag is in it.
		final String changeEmail = ResourceUtil.get(9250);
		msg = StringUtil.replace(msg, "{change}", changeEmail);//9250=change Email
		final String verifyEmail = ResourceUtil.get(9198);
		msg = StringUtil.replace(msg, "{verify}", verifyEmail);//9198=Verify Email

		LogManager.error(ResourceUtil.get(9259));//9259=same ID is using, try another ID or verify Email!
		
		if(ResourceUtil.isNonUIServer()) {
			return;
		}

		final JPanel panel = App.buildMessagePanel(msg, App.getSysIcon(App.SYS_ERROR_ICON));
		final ActionListener verifyAction = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				changeIDWindow = null;
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						VerifyEmailManager.startVerifyProcess();
					}
				});
			}
		};
		final ActionListener changeAction = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						showInputEmailDialog(IConstant.getUUID());	
					}
				});
			}
		};
		final JButton btnVerifyEmail = new JButton(verifyEmail);
		final JButton btnChangeEmail = new JButton(changeEmail);
		changeIDWindow = App.showCenterPanelMain(panel, 0, 0, ResourceUtil.get(IConstant.ERROR), true, btnVerifyEmail, btnChangeEmail,
				verifyAction, changeAction, null, false, true, null, false, false);
	}
	
	private static void showInputEmailDialog(final String uuid) {
		final String title = ResourceUtil.get(9279);//9279=New Account

		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		final Insets insets = new Insets(5, 5, 5, 5);

		panel.setBorder(new TitledBorder(""));

		final JLabel jluuid = new JLabel();
		jluuid.setIcon(new ImageIcon(ImageSrc.ACCOUNT_ICON));
		final JPanel uuidPanelflow = new JPanel();
		uuidPanelflow.setLayout(new FlowLayout());
		uuidPanelflow.add(jluuid);
		uuidPanelflow.add(new JLabel(VerifyEmailManager.getEmailI18N()));
		uuidPanelflow.add(new JLabel(":"));
		panel.add(uuidPanelflow,
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets, 0, 0));

		final int columns = 20;
		final JTextField jtfuuid = new JTextField(uuid, columns);
		jtfuuid.setEditable(true);
		jtfuuid.setForeground(Color.BLUE);
		jtfuuid.setHorizontalAlignment(SwingConstants.RIGHT);
		jtfuuid.setToolTipText(ResourceUtil.get(9278));//9278=create new account, such as an email.
		panel.add(jtfuuid,
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets, 0, 0));

		final JButton jbCancle = new JButton(ResourceUtil.get(1018), new ImageIcon(ImageSrc.CANCEL_ICON));
		final UIActionListener cancelAction = new UIActionListener() {
			@Override
			public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
				window.dispose();
				changeIDWindow = null;
			}
		};
		final JButton jbOK = new JButton(ResourceUtil.get(IContext.OK), new ImageIcon(ImageSrc.OK_ICON));
		final UIActionListener jbOKAction = new UIActionListener() {
			@Override
			public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
				final String email = jtfuuid.getText();
				if (ResourceUtil.checkEmailID(email, window) == false) {
					return;
				}

				changeIDWindow = null;
				window.dispose();
				PropertiesManager.setValue(PropertiesManager.p_uuid, email);// 注意：强制更新，因为密码丢失时，需要调用此逻辑重写
				IConstant.setUUID(email);
				PropertiesManager.saveFile();

				ResourceUtil.buildMenu();
				SafeDataManager.startSafeBackupProcess(true, false);
				
				J2SESessionManager.stopAllSession(false, true);
				J2SESessionManager.startNewIdleSession();
			}
		};

		App.showCenter(panel, 0, 0, title, true, jbOK, jbCancle, jbOKAction, cancelAction, null, false, false, null, false);
		jtfuuid.requestFocusInWindow();
	}

}
