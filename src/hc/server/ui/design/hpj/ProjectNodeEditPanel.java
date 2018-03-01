package hc.server.ui.design.hpj;

import hc.App;
import hc.core.IConstant;
import hc.core.util.ExceptionChecker;
import hc.core.util.ExceptionJSON;
import hc.core.util.HarHelper;
import hc.core.util.HarInfoForJSON;
import hc.core.util.RootBuilder;
import hc.res.ImageSrc;
import hc.server.HCActionListener;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.Designer;
import hc.server.ui.design.HCPermissionConstant;
import hc.server.ui.design.I18nTitlesEditor;
import hc.server.util.ContextSecurityConfig;
import hc.util.IntTextField;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.SocketEditPanel;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ProjectNodeEditPanel extends NameEditPanel {
	final JTextField idField = new JTextField();
	final VerTextPanel verPanel = new VerTextPanel("project", false, true, true);
	final JTextField urlField = new JTextField();
	final JTextField exceptionField = new JTextField();
	final JButton testExceptionBtn = new JButton("Test Exception Post");
	final JButton testHADBtn = new JButton("Test HAD");

	final IntTextField compactDays = new IntTextField(1, Integer.MAX_VALUE);
	final JTextField contact = new JTextField();
	final JTextField copyright = new JTextField();
	final JTextField desc = new JTextField();
	final JTextField license = new JTextField();

	final JCheckBox perm_write = new JCheckBox("write, exclude private file");
	final JCheckBox perm_exec = new JCheckBox("execute");
	final JCheckBox perm_del = new JCheckBox("delete, exclude private file");
	final JCheckBox perm_exit = new JCheckBox("exit");

	private final JCheckBox checkReadProperty = new JCheckBox(
			HCPermissionConstant.READ_SYSTEM_PROPERTIES);
	private final JCheckBox checkWriteProperty = new JCheckBox(
			HCPermissionConstant.WRITE_SYSTEM_PROPERTIES);
	final JCheckBox perm_memAccessSystem = new JCheckBox(HCPermissionConstant.MEMBER_ACCESS_SYSTEM);

	private final JCheckBox checkLoadLib = new JCheckBox(ResourceUtil.LOAD_NATIVE_LIB);
	private final JCheckBox checkLocation = new JCheckBox(ResourceUtil.LOCATION_OF_MOBILE);
	private final JCheckBox checkScriptPanel = new JCheckBox(ResourceUtil.SCRIPT_PANEL);
	private final JCheckBox checkRobot = new JCheckBox("create java.awt.Robot");
	// private final JCheckBox checkListenAllAWTEvents = new JCheckBox("listen
	// all AWT events");
	// private final JCheckBox checkAccessClipboard = new JCheckBox("access
	// clipboard");
	private final JCheckBox checkShutdownHooks = new JCheckBox("access shutdown hooks");
	private final JCheckBox checkSetIO = new JCheckBox("set system IO");
	private final JCheckBox checkSetFactory = new JCheckBox("set Factory");

	SocketEditPanel perm_sock_panel;

	private final JPanel buildExceptionPanel() {
		final JPanel exceptionPanel = new JPanel(new BorderLayout());
		{
			exceptionField.setColumns(30);
			exceptionField.getDocument().addDocumentListener(new DocumentListener() {
				private void modify() {
					final String newInputURL = getExceptionURLFromEdit();
					testExceptionBtn.setEnabled(newInputURL.length() > 0);

					((HPProject) currItem).exceptionURL = newInputURL;
					notifyModified(true);
				}

				@Override
				public void removeUpdate(final DocumentEvent e) {
					modify();
				}

				@Override
				public void insertUpdate(final DocumentEvent e) {
					modify();
				}

				@Override
				public void changedUpdate(final DocumentEvent e) {
					modify();
				}
			});

			testExceptionBtn.setToolTipText("<html>"
					+ "post a exception to current URL or email address,"
					+ "<BR><BR><STRONG>URL</STRONG> : refresh log to view response (UTF-8 is required). it is very useful to debug the receiving codes."
					+ "<BR><STRONG>Email</STRONG> : no attachment in Email, if it is not received, change it and try again!"
					+ "</html>");
			testExceptionBtn.addActionListener(new HCActionListener(new Runnable() {
				final HarHelper harhelper = new HarHelper() {
					@Override
					public final String getExceptionReportURL() {
						return getExceptionURLFromEdit();
					}

					@Override
					public final HarInfoForJSON getHarInfoForJSON() {
						final HarInfoForJSON harInfo = new HarInfoForJSON();
						harInfo.projectID = getHarIDFromEdit();
						harInfo.projectVersion = getHarVersionFromEdit();
						return harInfo;
					}
				};

				final ExceptionChecker checker = new ExceptionChecker() {
					@Override
					public final boolean isPosted(final String projectID, final String errMsg,
							final String stackTrace) {
						return false;
					}
				};

				@Override
				public void run() {
					if (PropertiesManager.isSimu()) {
						App.showConfirmDialog(designer, "it can not work in simu mode!",
								ResourceUtil.getErrorI18N(), JOptionPane.OK_OPTION,
								JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
						return;
					}

					testExceptionBtn.setEnabled(false);

					Throwable t = null;
					try {
						Integer.parseInt("Hello");
					} catch (final Throwable e) {
						t = e;
					}
					final RootBuilder rootBuilder = RootBuilder.getInstance();
					final ExceptionJSON json = rootBuilder.getExceptionJSONBuilder().buildJSON(
							harhelper, checker, t, ExceptionJSON.HC_EXCEPTION_URL,
							"puts \"This is test script\\n\";\nputs \"this is second line.\"",
							"Hello, 你好, Bonjour");
					final String urlOrEmail = getExceptionURLFromEdit();
					if (ResourceUtil.validEmail(urlOrEmail)) {
						json.setReceiveExceptionForHC(false);
						json.setAttToEmail(urlOrEmail);
					} else {
						// json.setReceiveExceptionForHC(false);
						json.setToURL(urlOrEmail);
					}
					json.isForTest = true;
					rootBuilder.reportException(json);

					testExceptionBtn.setEnabled(true);
				}
			}, threadPoolToken));

			final JPanel tmpPanel = new JPanel(new GridLayout(2, 1));
			tmpPanel.add(new JLabel(
					"<html><STRONG>Report exception URL / Email address</STRONG> : </html>"));// 注意：请与ProjectContext.printAndReportStackTrace使用描述字段保持一致
			final JPanel flowPanel = new JPanel(new GridBagLayout());
			final GridBagConstraints c = new GridBagConstraints();
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			flowPanel.add(new JLabel("   "), c);
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			flowPanel.add(exceptionField, c);
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			flowPanel.add(testExceptionBtn, c);
			tmpPanel.add(flowPanel);

			exceptionPanel.add(tmpPanel, BorderLayout.NORTH);
			exceptionPanel.add(ResourceUtil.addSpaceBeforePanel(new JLabel(
					"<html>report exception to HAR provider via URL or Email, NOT both. if blank then disable report."
							+ "<BR>for more, please reference API ProjectContext.<STRONG>printAndReportStackTrace</STRONG>(throwable, isCopyToHomeCenter).</html>")),
					BorderLayout.CENTER);
		}
		return exceptionPanel;
	}

	public ProjectNodeEditPanel() {
		super();

		testExceptionBtn.setEnabled(false);
		testHADBtn.setEnabled(false);

		{
			final JButton i18nBtn = new JButton(BaseMenuItemNodeEditPanel.I18N_BTN_TEXT);
			i18nBtn.setIcon(new ImageIcon(ImageSrc.loadImageFromPath("hc/res/global_16.png")));
			i18nBtn.setToolTipText(BaseMenuItemNodeEditPanel.buildI18nButtonTip(nameLabel));
			i18nBtn.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					I18nTitlesEditor.showEditor(currItem.i18nMap, new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							if (currItem.i18nMap.isModified()) {
								notifyModified(true);
								currItem.i18nMap.clearModifyTag();
							}
						}
					}, i18nBtn, designer);

				}
			}, threadPoolToken));
			namePanel.add(i18nBtn);
		}

		final JPanel idPanle = new JPanel(new GridLayout(2, 1));
		final JLabel idLabel = new JLabel("<html><STRONG>Project ID</STRONG> : </html>");
		final JLabel tipLabel = ProjectIDDialog.buildIDTipLabel();
		// tipLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		idPanle.add(idLabel);
		ProjectIDDialog.buildIDFieldKeyListener(idField);
		idField.getDocument().addDocumentListener(new DocumentListener() {
			private void modify() {
				((HPProject) currItem).id = getHarIDFromEdit();
				notifyModified(true);
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				modify();
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				modify();
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {
				modify();
			}
		});
		idField.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).id = idField.getText();
				App.invokeLaterUI(updateTreeRunnable);
				notifyModified(true);
			}
		}, threadPoolToken));
		idField.setColumns(20);
		{
			final JPanel grid = new JPanel(new GridBagLayout());
			final GridBagConstraints c = new GridBagConstraints();
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			grid.add(new JLabel("   "), c);

			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			grid.add(idField, c);

			idPanle.add(grid);
		}

		final JPanel center = new JPanel(new BorderLayout());
		{
			final JPanel compose = new JPanel(new BorderLayout());
			compose.add(idPanle, BorderLayout.NORTH);
			compose.add(ResourceUtil.addSpaceBeforePanel(tipLabel), BorderLayout.CENTER);

			{
				final JPanel upgradePanel = new JPanel(new BorderLayout());
				{
					urlField.setColumns(30);
					urlField.getDocument().addDocumentListener(new DocumentListener() {
						private void modify() {
							final String newURL = urlField.getText().trim();
							testHADBtn.setEnabled(newURL.length() > 0);
							((HPProject) currItem).upgradeURL = newURL;
							notifyModified(true);
						}

						@Override
						public void removeUpdate(final DocumentEvent e) {
							modify();
						}

						@Override
						public void insertUpdate(final DocumentEvent e) {
							modify();
						}

						@Override
						public void changedUpdate(final DocumentEvent e) {
							modify();
						}
					});
					final String upgradeURL = "Upgrade URL";
					testHADBtn.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							final Properties had = new Properties();
							try {
								final String url = urlField.getText();
								ResourceUtil.loadFromURL(had, url,
										ResourceUtil.getUserAgentForHAD());

								final JPanel jpanel = new JPanel(new BorderLayout());
								final StringBuffer sb = new StringBuffer();
								sb.append("<html>");
								final Enumeration<Object> en = had.keys();
								boolean isHead = true;

								while (en.hasMoreElements()) {
									final Object key = en.nextElement();
									if (isHead) {
										isHead = false;
									} else {
										sb.append("<br>");
									}
									sb.append("<strong>" + key + "</strong>: "
											+ had.getProperty((String) key));
								}
								sb.append("</html>");

								jpanel.add(new JLabel(sb.toString(),
										App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING),
										BorderLayout.CENTER);
								App.showCenterPanelMain(jpanel, 0, 0, ResourceUtil.getInfoI18N(),
										false, null, null, null, null, designer, true, false, null,
										false, false);
							} catch (final Exception ex) {
								final JPanel jpanel = new JPanel(new BorderLayout());
								jpanel.add(new JLabel("fail to connect server file.",
										App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING),
										BorderLayout.CENTER);
								App.showCenterPanelMain(jpanel, 0, 0,
										(String) ResourceUtil.get(IConstant.ERROR), false, null,
										null, null, null, designer, true, false, null, false,
										false);
							}
						}
					}, threadPoolToken));

					final JPanel panel = new JPanel(new GridLayout(2, 1));
					panel.add(new JLabel("<html><STRONG>" + upgradeURL + "</STRONG> : </html>"));

					final JPanel grid = new JPanel(new GridBagLayout());
					final GridBagConstraints c = new GridBagConstraints();
					c.weightx = 0;
					c.fill = GridBagConstraints.NONE;
					grid.add(new JLabel("   "), c);
					c.weightx = 1;
					c.fill = GridBagConstraints.HORIZONTAL;
					grid.add(urlField, c);
					c.weightx = 0;
					c.fill = GridBagConstraints.NONE;
					grid.add(testHADBtn, c);
					panel.add(grid);

					upgradePanel.add(panel, BorderLayout.NORTH);
					upgradePanel.add(ResourceUtil.addSpaceBeforePanel(new JLabel(
							"<html>the auto upgrade URL of new version of current project, to disable upgrade, please keep blank."
									+ "<br><br>for example, http://example.com/dir_or_virtual/tv.had , NOTE: it is <strong>had</strong> file, not <strong>har</strong> file."
									+ "<br>please put both tv.har, tv.had in directory dir_or_virtual for download."
									+ "<br><br><strong>had</strong> file provides version information which is used to determine upgrade or not."
									+ "<br>click <strong>" + Designer.SAVE_AS_TEXT
									+ "</strong> button, <strong>had</strong> file is automatically created with <strong>har</strong> if <strong>"
									+ upgradeURL + "</strong> is not blank."
									+ "<br><br>for more about server, see user-agent of HTTP/HTTPS request log."
									+ "</html>")),
							BorderLayout.CENTER);
				}

				desc.setColumns(30);
				desc.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject) currItem).desc = desc.getText();
						notifyModified(true);
					}
				});

				license.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject) currItem).license = license.getText();
						notifyModified(true);
					}
				});

				contact.setColumns(30);
				contact.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject) currItem).contact = contact.getText();
						notifyModified(true);
					}
				});

				compactDays.setColumns(30);
				compactDays.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						final String days = compactDays.getText();
						try {
							final int int_day = Integer.parseInt(days);
							if (int_day > 0) {
								((HPProject) currItem).compactDays = days;
								notifyModified(true);
							}
						} catch (final Exception e) {
						}
					}
				});

				copyright.setColumns(30);
				copyright.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject) currItem).copyright = copyright.getText();
						notifyModified(true);
					}
				});

				final VerTextField verField = verPanel.verTextField;
				verField.getDocument().addDocumentListener(new DocumentListener() {
					private void modify() {
						((HPProject) currItem).ver = getHarVersionFromEdit();
						notifyModified(true);
					}

					@Override
					public void removeUpdate(final DocumentEvent e) {
						modify();
					}

					@Override
					public void insertUpdate(final DocumentEvent e) {
						modify();
					}

					@Override
					public void changedUpdate(final DocumentEvent e) {
						modify();
					}
				});
				verField.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						((HPProject) currItem).ver = verField.getText();
						App.invokeLaterUI(updateTreeRunnable);
						notifyModified(true);
					}
				}, threadPoolToken));

				final JComponent[] listPanels = { compose,
						new JSeparator(SwingConstants.HORIZONTAL), verPanel,
						new JSeparator(SwingConstants.HORIZONTAL),
						buildItemPanel(compactDays,
								"<html><STRONG>Compact DB Days</STRONG> : </html>", 30),
						new JSeparator(SwingConstants.HORIZONTAL), upgradePanel,
						new JSeparator(SwingConstants.HORIZONTAL), buildExceptionPanel(),
						new JSeparator(SwingConstants.HORIZONTAL),
						buildItemPanel(desc, "<html><STRONG>Description</STRONG> : </html>", 30),
						new JSeparator(SwingConstants.HORIZONTAL),
						buildItemPanel(license, "<html><STRONG>License URL</STRONG> : </html>", 30),
						new JSeparator(SwingConstants.HORIZONTAL),
						buildItemPanel(contact, "<html><STRONG>Contact</STRONG> : </html>", 30),
						new JSeparator(SwingConstants.HORIZONTAL), buildItemPanel(copyright,
								"<html><STRONG>Copyright</STRONG> : </html>", 30) };
				final JPanel buildNorthPanel = ServerUIUtil.buildNorthPanel(listPanels, 0,
						BorderLayout.CENTER);

				center.add(buildNorthPanel, BorderLayout.CENTER);
			}
		}

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab((String) ResourceUtil.get(9095), new JScrollPane(center));
		{
			final JLabel noteLabel = new JLabel("<html><BR><font color='red'>Note : </font><BR>"
					+ "1. these permissions are NOT run-time, they are for design period.<BR>"
					+ "2. to set run-time permissions, click /Shift Project/{project}/Modify|Permission.<BR>"
					+
					// "3. run-time permissions will keep their values even if
					// the project is upgraded/re-activated/restarted.<BR>" +
					// "3. these permissions works on standard J2SE JVM, NOT for
					// server on Android or other.<BR>" +
					"&nbsp;</html>");
			// noteLabel.setHorizontalAlignment(SwingConstants.CENTER);
			// new JSeparator(SwingConstants.HORIZONTAL),
			final JComponent[] components = { noteLabel, buildPermissionPanel() };
			final JPanel buildNorthPanel = ServerUIUtil.buildNorthPanel(components, 0,
					BorderLayout.CENTER);
			tabbedPane.addTab((String) ResourceUtil.get(9094), new JScrollPane(buildNorthPanel));
		}

		add(tabbedPane, BorderLayout.CENTER);
	}

	private final JPanel buildPermissionPanel() {
		// JPanel permissionPanel = new JPanel(new FlowLayout());
		perm_write.setToolTipText(HCPermissionConstant.WRITE_TIP);
		perm_exec.setToolTipText(HCPermissionConstant.EXECUTE_TIP);
		perm_del.setToolTipText(HCPermissionConstant.DELETE_TIP);
		perm_exit.setToolTipText(HCPermissionConstant.EXIT_TIP);

		checkReadProperty.setToolTipText(HCPermissionConstant.READ_PROP_TIP);
		checkWriteProperty.setToolTipText(HCPermissionConstant.WRITE_PROP_TIP);
		perm_memAccessSystem.setToolTipText(HCPermissionConstant.MEMBER_ACCESS_SYSTEM_TIP);

		checkLocation.setToolTipText(HCPermissionConstant.LOCATION_OF_MOBILE);
		checkScriptPanel.setToolTipText(HCPermissionConstant.SCRIPT_PANEL);

		checkLoadLib.setToolTipText(HCPermissionConstant.LOAD_LIB_TIP);
		checkRobot.setToolTipText(HCPermissionConstant.ROBOT_TIP);
		// checkListenAllAWTEvents.setToolTipText(HCPermissionConstant.LISTEN_ALL_AWT_EVENTS_TIP);
		// checkAccessClipboard.setToolTipText(HCPermissionConstant.ACCESS_CLIPBOARD_TIP);
		checkShutdownHooks.setToolTipText(HCPermissionConstant.SHUTDOWN_HOOKS_TIP);
		checkSetIO.setToolTipText(HCPermissionConstant.SETIO_TIP);
		checkSetFactory.setToolTipText(HCPermissionConstant.SET_FACTORY_TIP);

		perm_sock_panel = new SocketEditPanel() {
			@Override
			public void notifyModify() {
				notifyModiPermissions();
			}

			@Override
			public void notifyLostEditPanelFocus() {
				((HPProject) currItem).csc.saveToMap();
			}

			@Override
			public final ContextSecurityConfig getCSCSource() {
				return ((HPProject) currItem).csc;
			}
		};

		checkReadProperty.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setSysPropRead(checkReadProperty.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		checkWriteProperty.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setSysPropWrite(checkWriteProperty.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		checkLoadLib.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setLoadLib(checkLoadLib.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		checkLocation.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setLocation(checkLocation.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		checkScriptPanel.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setScriptPanel(checkScriptPanel.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		checkRobot.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setRobot(checkRobot.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		// checkListenAllAWTEvents.addItemListener(new HCActionListener(new
		// Runnable() {
		// @Override
		// public void run() {
		// ((HPProject)currItem).csc.setListenAllAWTEvents(checkListenAllAWTEvents.isSelected());
		// notifyModiPermissions();
		// }
		// }, threadPoolToken));
		// checkAccessClipboard.addItemListener(new HCActionListener(new
		// Runnable() {
		// @Override
		// public void run() {
		// ((HPProject)currItem).csc.setAccessClipboard(checkAccessClipboard.isSelected());
		// notifyModiPermissions();
		// }
		// }, threadPoolToken));
		checkShutdownHooks.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setShutdownHooks(checkShutdownHooks.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		checkSetIO.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setSetIO(checkSetIO.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		checkSetFactory.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setSetFactory(checkSetFactory.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		perm_write.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setWrite(perm_write.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		perm_exec.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setExecute(perm_exec.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		perm_del.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setDelete(perm_del.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		perm_exit.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setExit(perm_exit.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));
		perm_memAccessSystem.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject) currItem).csc.setMemberAccessSystem(perm_memAccessSystem.isSelected());
				notifyModiPermissions();
			}
		}, threadPoolToken));

		final JPanel mobilePermPanel = new JPanel(new GridLayout(1, 4));
		mobilePermPanel.add(checkLocation);
		mobilePermPanel.add(checkScriptPanel);

		final JPanel osPermPanel = new JPanel(new GridLayout(1, 4));
		osPermPanel.add(perm_write);
		osPermPanel.add(perm_exec);
		osPermPanel.add(perm_del);
		osPermPanel.add(perm_exit);
		final JPanel sysPropPanel = new JPanel(new GridLayout(1, 4));
		sysPropPanel.add(checkReadProperty);
		sysPropPanel.add(checkWriteProperty);

		sysPropPanel.add(perm_memAccessSystem);

		final JPanel sysOtherPropPanel = new JPanel(new GridLayout(2, 4));
		sysOtherPropPanel.add(checkLoadLib);
		sysOtherPropPanel.add(checkRobot);
		sysOtherPropPanel.add(checkSetIO);
		sysOtherPropPanel.add(checkShutdownHooks);
		sysOtherPropPanel.add(checkSetFactory);
		// sysOtherPropPanel.add(checkListenAllAWTEvents);
		// sysOtherPropPanel.add(checkAccessClipboard);
		final JComponent[] components = { new JSeparator(SwingConstants.HORIZONTAL),
				mobilePermPanel, new JSeparator(SwingConstants.HORIZONTAL), perm_sock_panel,
				new JSeparator(SwingConstants.HORIZONTAL), osPermPanel,
				new JSeparator(SwingConstants.HORIZONTAL), sysPropPanel,
				new JSeparator(SwingConstants.HORIZONTAL), sysOtherPropPanel };
		return ServerUIUtil.buildNorthPanel(components, 0, BorderLayout.CENTER);
		// return permissionPanel;
	}

	@Override
	public void notifyLostEditPanelFocus() {
		perm_sock_panel.notifyLostEditPanelFocus();
		super.notifyLostEditPanelFocus();
	}

	public final void notifyModiPermissions() {
		if (isInited) {
			// System.out.println(System.currentTimeMillis() +
			// "=======>notifyModiPermissions");
			designer.isModiPermissions = true;
		}
		notifyModified(true);
	}

	@Override
	public void extendInit() {
		final HPProject hpProject = (HPProject) currItem;

		idField.setText(hpProject.id);
		verPanel.verTextField.setText(hpProject.ver);
		urlField.setText(hpProject.upgradeURL);
		exceptionField.setText(hpProject.exceptionURL);
		license.setText(hpProject.license);
		desc.setText(hpProject.desc);
		copyright.setText(hpProject.copyright);
		contact.setText(hpProject.contact);
		compactDays.setText(hpProject.compactDays);

		final ContextSecurityConfig csc = hpProject.csc;

		perm_write.setSelected(csc.isWrite());
		perm_exec.setSelected(csc.isExecute());
		perm_del.setSelected(csc.isDelete());
		perm_exit.setSelected(csc.isExit());

		checkReadProperty.setSelected(csc.isSysPropRead());
		checkWriteProperty.setSelected(csc.isSysPropWrite());
		perm_memAccessSystem.setSelected(csc.isMemberAccessSystem());

		checkLocation.setSelected(csc.isLocation());
		checkScriptPanel.setSelected(csc.isScriptPanel());

		checkLoadLib.setSelected(csc.isLoadLib());
		checkRobot.setSelected(csc.isRobot());
		// checkListenAllAWTEvents.setSelected(csc.isListenAllAWTEvents());
		// checkAccessClipboard.setSelected(csc.isAccessClipboard());
		checkShutdownHooks.setSelected(csc.isShutdownHooks());
		checkSetIO.setSelected(csc.isSetIO());
		checkSetFactory.setSelected(csc.isSetFactory());

		perm_sock_panel.refresh(csc);
	}

	private final JPanel buildItemPanel(final JTextField field, final String label,
			final int colNum) {
		field.setColumns(colNum);

		final JPanel tmpPanel = new JPanel(new GridLayout(2, 1));
		tmpPanel.add(new JLabel(label));

		final JPanel grid = new JPanel(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		grid.add(new JLabel("   "), c);

		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		grid.add(field, c);

		tmpPanel.add(grid);

		return tmpPanel;
	}

	private final String getHarVersionFromEdit() {
		return verPanel.verTextField.getText().trim();
	}

	private final String getHarIDFromEdit() {
		return idField.getText().trim();
	}

	private final String getExceptionURLFromEdit() {
		return exceptionField.getText().trim();
	}
}
