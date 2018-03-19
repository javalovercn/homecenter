package hc.server.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.RootServerConnector;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.res.ImageSrc;
import hc.server.DisposeListener;
import hc.server.HCActionListener;
import hc.server.ui.ClientDesc;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.TokenManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class VerifyEmailManager {
	public static JButton buildVerifyEmailButton() {
		final String verifiedEmail = getVerifyEmailButtonText();
		final JButton jbVerifyEmail = new JButton(verifiedEmail, new ImageIcon(ImageSrc.ACCOUNT_LOCK_ICON));
		return jbVerifyEmail;
	}

	public static String getVerifyEmailButtonText() {
		return (String) ResourceUtil.get(9198);
	}

	public static String getEmailI18N() {
		return (String) ResourceUtil.get(9074);
	}

	private static final String V_TIMEOUT = "timeout";
	private static final String V_VERIFY_PASS = "verifyPass";
	private static final String V_NOT_REQUEST = "NotRequestVerify";

	/**
	 * 
	 * @param id
	 * @param tokenNotVerifyToken
	 * @return null ：exception, "timeout", "verifyPass", "NotRequestVerify"
	 */
	public static String waitForCheckEmail(final String id, final String tokenNotVerifyToken) {
		String url = RootServerConnector.buildWaitForCheckURL(id, tokenNotVerifyToken);
		final boolean isSimu = PropertiesManager.isSimu();
		url = HttpUtil.replaceSimuURL(url, isSimu);
		try {
			return getAjaxForLongConnection(url);
		} catch (final Throwable e) {
		}
		return null;
	}

	private static boolean isShowed = false;

	private static HCJFrame sendEmailFrame;

	private synchronized static void startVerifyMailSendProcess(final String emailID) {
		if (toFront()) {
			return;
		}

		sendEmailFrame = new HCJFrame();

		final String title = VerifyEmailManager.getVerifyEmailButtonText();
		sendEmailFrame.setTitle(title);
		sendEmailFrame.setIconImage(App.SYS_LOGO);
		final java.awt.event.ActionListener exitActionListener = new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				sendEmailFrame.dispose();
				sendEmailFrame = null;
			}
		};
		sendEmailFrame.getRootPane().registerKeyboardAction(exitActionListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		sendEmailFrame.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		sendEmailFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				sendEmailFrame.dispose();
				sendEmailFrame = null;
			}
		});

		final String send = (String) ResourceUtil.get(9199);// send

		final JPanel panel = new JPanel(new BorderLayout());
		final String vEmailAddress = title + " : " + emailID;
		panel.setBorder(new TitledBorder(vEmailAddress));
		final JButton sendBtn = new JButton(send);
		final JPanel sendPanel = new JPanel(new GridLayout(1, 2));
		sendPanel.add(sendBtn);
		final String notSend = (String) ResourceUtil.get(9201);
		final JLabel verifyStatus = new JLabel(notSend, SwingConstants.CENTER);
		sendPanel.add(verifyStatus);

		String clickSendDesc = (String) ResourceUtil.get(9202);
		final String sendMatch = "{send}";
		clickSendDesc = StringUtil.replace(clickSendDesc, sendMatch, send);// 出现了两次，故执行两次
		clickSendDesc = StringUtil.replace(clickSendDesc, sendMatch, send);
		final JLabel desc = new JLabel("<html>" + clickSendDesc + "</html>");
		panel.add(sendPanel, BorderLayout.CENTER);
		{
			final JPanel descPanel = new JPanel(new BorderLayout());
			descPanel.setBorder(new TitledBorder((String) ResourceUtil.get(9095)));
			descPanel.add(desc, BorderLayout.CENTER);
			panel.add(descPanel, BorderLayout.SOUTH);
		}

		final JButton jbClose = App.buildDefaultCloseButton();
		// sendEmailFrame.getRootPane().setDefaultButton(sendBtn);

		final int[] verifiedStatus = new int[1];// 0 : 未send, 1 : send
												// one/two/..., 2 : 通过

		sendEmailFrame.setDisposeListener(new DisposeListener() {
			@Override
			public void dispose() {
				verifiedStatus[0] = 0;
			}
		});

		sendBtn.addActionListener(new HCActionListener(new Runnable() {
			final String tokenForVerifyEmailHideRealToken = ResourceUtil.buildUUID();

			@Override
			public void run() {
				if (verifiedStatus[0] == 2) {
					return;
				}

				final String token = PropertiesManager.getValue(PropertiesManager.p_Token);
				if (RootServerConnector.submitEmail(emailID, token, tokenForVerifyEmailHideRealToken) == null) {
					App.showMessageDialog(sendEmailFrame, ResourceUtil.get(9205));
					return;
				} else {
					App.showMessageDialog(sendEmailFrame, ResourceUtil.get(9204));
				}

				if (verifiedStatus[0] == 0) {
					verifiedStatus[0] = 1;

					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							final String waiting = (String) ResourceUtil.get(9203);
							final String[] working = { waiting, "." + waiting + ".", ".." + waiting + "..", "..." + waiting + "...",
									".." + waiting + "..", "." + waiting + "." };
							final int length = working.length;
							int idx = 0;
							HCJFrame sendEmailFrameSnap;
							while (verifiedStatus[0] == 1 && (sendEmailFrameSnap = sendEmailFrame) != null
									&& sendEmailFrameSnap.isVisible()) {
								synchronized (verifiedStatus) {
									if (verifiedStatus[0] == 1) {
										verifyStatus.setText(working[(idx++) % length]);
									}
								}
								try {
									Thread.sleep(400);
								} catch (final Exception e) {
								}
							}
						}
					});

					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							HCJFrame sendEmailFrameSnap;
							while ((sendEmailFrameSnap = sendEmailFrame) != null && sendEmailFrameSnap.isVisible()) {
								final String result = waitForCheckEmail(emailID, token);
								if (result == null || result.equals(V_NOT_REQUEST)) {
									try {
										Thread.sleep(1000);
									} catch (final Exception e) {
									}
								} else if (result.equals(V_TIMEOUT)) {
								} else if (result.equals(V_VERIFY_PASS)) {
									synchronized (verifyStatus) {
										verifiedStatus[0] = 2;
									}

									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											setVerifyOKStatus(sendBtn, verifyStatus);
										}
									});

									TokenManager.changeTokenFromUI(PropertiesManager.isTrue(PropertiesManager.p_isDonateOrVIPNowOrEver),
											emailID, token, true);

									break;
								}
							}
						}
					});
				}
			}
		}, App.getThreadPoolToken()));

		if (isVerifiedEmail()) {
			setVerifyOKStatus(sendBtn, verifyStatus);
		}

		jbClose.addActionListener(exitActionListener);

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(panel, BorderLayout.CENTER);

		final JPanel changeEmailPanel = new JPanel(new BorderLayout());
		final String chEmail = (String) ResourceUtil.get(9250);
		final JButton changeEmailBtn = new JButton(chEmail);// "change Email"
		changeEmailBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				sendEmailFrame.dispose();
				sendEmailFrame = null;
				VerifyEmailManager.showInputEmail("");
			}
		}));
		changeEmailPanel.add(changeEmailBtn, BorderLayout.CENTER);
		changeEmailPanel.setBorder(new TitledBorder(chEmail));
		mainPanel.add(changeEmailPanel, BorderLayout.SOUTH);

		final JPanel allPanel = new JPanel(new BorderLayout());
		allPanel.add(mainPanel, BorderLayout.CENTER);

		final JPanel bottomPanel = new JPanel(new GridBagLayout());
		bottomPanel.add(jbClose, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 5), 0, 0));
		allPanel.add(bottomPanel, BorderLayout.SOUTH);

		sendEmailFrame.add(allPanel);
		sendEmailFrame.pack();

		App.showCenter(sendEmailFrame);
	}

	private static boolean toFront() {
		HCJFrame sendEmailFrameSnap;
		if ((sendEmailFrameSnap = sendEmailFrame) != null && sendEmailFrameSnap.isVisible()) {
			sendEmailFrameSnap.toFront();
			return true;
		}

		return false;
	}

	public static void startVerifyProcess() {
		if (toFront()) {
			return;
		}

		final String emailStr = IConstant.getUUID();
		if (ResourceUtil.validEmail(emailStr) == false) {
			VerifyEmailManager.showInputEmail(emailStr);// 替换旧的帐号为email
		} else {
			VerifyEmailManager.startVerifyMailSendProcess(emailStr);
		}
	}

	private static void showInputEmail(final String initEmail) {
		CCoreUtil.checkAccess();

		final HCJFrame showEmail = new HCJFrame();
		showEmail.setTitle(VerifyEmailManager.getEmailI18N());
		showEmail.setIconImage(App.SYS_LOGO);
		final java.awt.event.ActionListener exitActionListener = new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				showEmail.dispose();
			}
		};
		showEmail.getRootPane().registerKeyboardAction(exitActionListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		showEmail.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		showEmail.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				showEmail.dispose();
			}
		});

		final JPanel panel = new JPanel(new BorderLayout());
		// panel.setBorder(new TitledBorder(""));
		final JLabel jluuid = new JLabel();
		jluuid.setIcon(new ImageIcon(ImageSrc.ACCOUNT_ICON));
		final JPanel uuidPanelflow = new JPanel();
		uuidPanelflow.setLayout(new FlowLayout());
		uuidPanelflow.add(jluuid);
		uuidPanelflow.add(new JLabel(VerifyEmailManager.getEmailI18N()));
		uuidPanelflow.add(new JLabel(":"));

		final int columns = 22;// 15在android环境下，不够
		final JTextField jtfuuid = new JTextField("", columns);
		jtfuuid.setForeground(Color.BLUE);
		jtfuuid.setHorizontalAlignment(SwingConstants.RIGHT);
		uuidPanelflow.add(jtfuuid);
		if (ResourceUtil.isAndroidServerPlatform()) {// 不能删除原字符
		} else {
			jtfuuid.setText(initEmail);
		}
		// jtfuuid.requestFocus();//注释此代码，供Focus测试之用

		panel.add(uuidPanelflow, BorderLayout.CENTER);

		final JButton jbOK = App.buildDefaultOKButton();
		showEmail.getRootPane().setDefaultButton(jbOK);
		final JButton jbExit = App.buildDefaultCancelButton();

		jbOK.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final String emailID = jtfuuid.getText().trim();
				if (ResourceUtil.validEmail(emailID) == false) {
					App.showMessageDialog(null, (String) ResourceUtil.get(9073), (String) ResourceUtil.get(IConstant.ERROR),
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				showEmail.dispose();
				showConfirmEmailDialog(emailID);
			}
		}, App.getThreadPoolToken()));

		jbExit.addActionListener(exitActionListener);

		final JPanel allPanel = new JPanel();
		allPanel.setLayout(new BorderLayout());
		// panel.setBorder(new TitledBorder(""));
		allPanel.add(panel, BorderLayout.CENTER);

		final JPanel jPanel3 = new JPanel(new GridLayout(1, 2, ClientDesc.hgap, ClientDesc.vgap));
		jPanel3.add(jbExit);
		jPanel3.add(jbOK);
		final JPanel lineEndPanel = new JPanel(new GridBagLayout());
		lineEndPanel.add(jPanel3, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
				new Insets(ClientDesc.hgap, ClientDesc.hgap, ClientDesc.hgap, ClientDesc.hgap), 0, 0));
		allPanel.add(lineEndPanel, BorderLayout.SOUTH);

		showEmail.add(allPanel);
		showEmail.pack();

		App.showCenter(showEmail);
	}

	public static void showVerifyEmailWarning(final ThreadGroup threadPoolToken) {
		CCoreUtil.checkAccess();

		if (isShowed) {
			return;
		}
		isShowed = true;

		PropertiesManager.setValue(PropertiesManager.p_IsVerifiedEmail, IConstant.FALSE);
		try {
			ResourceUtil.buildMenu();
		} catch (final Throwable e) {
			// 不重要
		}

		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				LogManager.warning(
						"===current email is verified on this server before, but there is an other server is verified by the same email later!===");
				LogManager.warning("===you can't use same email on multiple server!===");
				final String msg = "<html></html>";
				final JPanel panel = new JPanel(new BorderLayout());
				final String emailOnOther = (String) ResourceUtil.get(9196);// "[{email}]
																			// should
																			// be
																			// verified
																			// on
																			// this
																			// server
																			// again!";
				final JLabel label = new JLabel(StringUtil.replace(emailOnOther, "{email}", IConstant.getUUID()));
				final StringBuffer sb = new StringBuffer();
				sb.append("<html>");
				sb.append("<BR/>");
				final String descStr = (String) ResourceUtil.get(9197);// this
																		// Email
																		// was
																		// verified
																		// on
																		// this
																		// server,
				sb.append(StringUtil.replace(descStr, "{verify}", getVerifyEmailButtonText()));
				sb.append("</html>");
				final JLabel desc = new JLabel(sb.toString());
				panel.add(label, BorderLayout.NORTH);
				panel.add(desc, BorderLayout.CENTER);
				final JButton jbVerifyEmail = buildVerifyEmailButton();
				final ActionListener verifyListener = new HCActionListener(new Runnable() {
					@Override
					public void run() {
						startVerifyProcess();
					}
				}, threadPoolToken);
				App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getErrorI18N(), true, jbVerifyEmail, null, verifyListener, null,
						(JFrame) null, false, true, null, false, true);
			}
		});
	}

	public static boolean isVerifiedEmail() {
		return PropertiesManager.isTrue(PropertiesManager.p_IsVerifiedEmail, false);
	}

	public static String getAjaxForLongConnection(final String urlStr) throws Exception {
		try {
			final URL url = new URL(urlStr);
			final URLConnection conn = url.openConnection();
			conn.setConnectTimeout(0);
			conn.setReadTimeout(0);
			return HttpUtil.getAjax(url, conn);
		} catch (final Throwable e) {
		}
		return null;
	}

	private static void showConfirmEmailDialog(final String emailID) {
		final JPanel panel = new JPanel(new BorderLayout());
		final JLabel label = new JLabel(
				"" + "<html>" + StringUtil.replace((String) ResourceUtil.get(9200), "{email}", emailID) + "</html>");
		panel.add(label, BorderLayout.CENTER);

		final ActionListener okListener = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final String token = PropertiesManager.getValue(PropertiesManager.p_Token);
				TokenManager.changeTokenFromUI(PropertiesManager.isTrue(PropertiesManager.p_isDonateOrVIPNowOrEver), emailID, token, false);
				VerifyEmailManager.startVerifyMailSendProcess(emailID);// 重启服务时，会检查并通知重新验证
			}
		}, App.getThreadPoolToken());
		final ActionListener cancelListener = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				showInputEmail(emailID);
			}
		}, App.getThreadPoolToken());

		App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getInfoI18N(), true, null, null, okListener, cancelListener, null, false, true,
				null, false, true);
	}

	private static void setVerifyOKStatus(final JButton sendBtn, final JLabel verifyStatus) {
		sendBtn.setEnabled(false);
		final ImageIcon icon = new ImageIcon(ImageSrc.loadImageFromPath("hc/res/lock_ok_16.png"));
		verifyStatus.setIcon(icon);
		verifyStatus.setText("");
	}
}
