package hc.server.ui.design.hpj;

import hc.App;
import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.util.ExceptionChecker;
import hc.core.util.ExceptionJSON;
import hc.core.util.HarHelper;
import hc.core.util.HarInfoForJSON;
import hc.core.util.RootBuilder;
import hc.server.HCActionListener;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.HCPermissionConstant;
import hc.server.ui.design.I18nTitlesEditor;
import hc.server.util.ContextSecurityConfig;
import hc.util.ResourceUtil;
import hc.util.SocketEditPanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ProjectNodeEditPanel extends NameEditPanel {
	final JPanel idPanel = new JPanel();
	final JTextField idField = new JTextField();
	final VerTextPanel verPanel = new VerTextPanel("project");
	final JTextField urlField = new JTextField();
	final JTextField exceptionField = new JTextField();
	final JButton testExceptionBtn = new JButton("Test Exception Post");
	final JButton testHADBtn = new JButton("Test HAD");

	final JTextField contact = new JTextField();
	final JTextField copyright = new JTextField();
	final JTextField desc = new JTextField();
	final JTextField license = new JTextField();
	
	final JCheckBox perm_write = new JCheckBox("write, exclude private file");
	final JCheckBox perm_exec = new JCheckBox("execute");
	final JCheckBox perm_del = new JCheckBox("delete, exclude private file");
	final JCheckBox perm_exit = new JCheckBox("exit");
	
	private final JCheckBox checkReadProperty = new JCheckBox(HCPermissionConstant.READ_SYSTEM_PROPERTIES);
	private final JCheckBox checkWriteProperty = new JCheckBox(HCPermissionConstant.WRITE_SYSTEM_PROPERTIES);
	final JCheckBox perm_memAccessSystem = new JCheckBox(HCPermissionConstant.MEMBER_ACCESS_SYSTEM);
	
	private final JCheckBox checkLoadLib = new JCheckBox("load native lib");
	private final JCheckBox checkRobot = new JCheckBox("create java.awt.Robot");
//	private final JCheckBox checkListenAllAWTEvents = new JCheckBox("listen all AWT events");
//	private final JCheckBox checkAccessClipboard = new JCheckBox("access clipboard");
	private final JCheckBox checkShutdownHooks = new JCheckBox("access shutdown hooks");
	private final JCheckBox checkSetIO = new JCheckBox("set system IO");
	private final JCheckBox checkSetFactory = new JCheckBox("set Factory");
	
	SocketEditPanel perm_sock_panel;
	
	private final JPanel buildExceptionPanel(){
		final JPanel exceptionPanel = new JPanel(new BorderLayout());
		{
			final JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
			exceptionField.setColumns(30);
			exceptionField.getDocument().addDocumentListener(new DocumentListener() {
				private void modify(){
					final String newInputURL = getExceptionURLFromEdit();
					testExceptionBtn.setEnabled(newInputURL.length() > 0);
					
					((HPProject)currItem).exceptionURL = newInputURL;
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
			
			final String reURL = "Report Exception URL";//注意：请与ProjectContext.printAndReportStackTrace使用描述字段保持一致
			
			fieldPanel.add(new JLabel("Receive URL / Email address : "));
			fieldPanel.add(exceptionField);
			testExceptionBtn.setToolTipText("<html>" +
					"post a exception to current URL or email address," +
					"<BR><BR><STRONG>URL</STRONG> : refresh log to view response (UTF-8 is required). it is very useful to debug the receiving codes." +
					"<BR><STRONG>Email</STRONG> : no attachment in Email, if it is not received, change it and try again!" +
					"</html>");
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
				
				final ExceptionChecker checker = new ExceptionChecker(){
					@Override
					public final boolean isPosted(final String projectID, final String stackTrace){
						return false;
					}
				};
				@Override
				public void run() {
					testExceptionBtn.setEnabled(false);
					
					Throwable t = null;
					try{
						Integer.parseInt("Hello");
					}catch (final Throwable e) {
						t = e;
					}
					final ExceptionJSON json = RootBuilder.getInstance().getExceptionJSONBuilder().buildJSON(harhelper, checker, t, ExceptionJSON.HC_EXCEPTION_URL, "puts \"This is test script\\n\";\nputs \"this is second line.\"", "Hello, 你好, Bonjour");
					final String urlOrEmail = getExceptionURLFromEdit();
					if(ResourceUtil.validEmail(urlOrEmail)){
						json.setReceiveExceptionForHC(false);
						json.setAttToEmail(urlOrEmail);
					}else{
//						json.setReceiveExceptionForHC(false);
						json.setToURL(urlOrEmail);
					}
					json.isForTest = true;
					ContextManager.getContextInstance().doExtBiz(IContext.BIZ_REPORT_EXCEPTION, json);
					
					testExceptionBtn.setEnabled(true);
				}
			}, threadPoolToken));
			fieldPanel.add(testExceptionBtn);
			
			exceptionPanel.add(fieldPanel, BorderLayout.NORTH);
			exceptionPanel.add(new JLabel("<html>report exception to HAR provider via URL or Email, NOT both. if blank then disable report." +
					"<BR><BR>for more, please reference API ProjectContext.<STRONG>printAndReportStackTrace</STRONG>(throwable, isCopyToHomeCenter).</html>"),
					BorderLayout.CENTER);
			exceptionPanel.setBorder(new TitledBorder(reURL));
		}
		return exceptionPanel;
	}
	
	public ProjectNodeEditPanel(){
		super();
		
		testExceptionBtn.setEnabled(false);
		testHADBtn.setEnabled(false);
		
		{
			final JButton i18nBtn = new JButton(BaseMenuItemNodeEditPanel.I18N_BTN_TEXT);
			i18nBtn.setToolTipText(BaseMenuItemNodeEditPanel.buildI18nButtonTip(nameLabel));
			i18nBtn.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					I18nTitlesEditor.showEditor(currItem.i18nMap, new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							if(currItem.i18nMap.isModified()){
								notifyModified(true);
								currItem.i18nMap.clearModifyTag();
							}
						}
					}, i18nBtn, designer);
					
				}
			}, threadPoolToken));
			namePanel.add(i18nBtn);
		}
		
		idPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		final JLabel idLabel = new JLabel("ID : ");
		final JLabel tipLabel = new JLabel("<html>it is used to install and upgrade to identify this project to different from other." +
				"<BR>'root' is system reserved ID." +
				"<BR>valid char : 0-9, a-z, A-Z, _</html>");
//		tipLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		idPanel.add(idLabel);
		idField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
				final char keyCh = e.getKeyChar();
		        if ((keyCh >= '0' && keyCh <= '9') 
		        		|| (keyCh >= 'a' && keyCh <= 'z') 
		        		|| (keyCh >= 'A' && keyCh <= 'Z') 
		        		|| keyCh == '_'){
		        }else{
		        	e.setKeyChar('\0');
		        }
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
			}
			
			@Override
			public void keyPressed(final KeyEvent e) {
			}
		});	
		idField.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				((HPProject)currItem).id = getHarIDFromEdit();
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
				((HPProject)currItem).id = idField.getText();
				App.invokeLaterUI(updateTreeRunnable);
				notifyModified(true);
			}
		}, threadPoolToken));
		idField.setColumns(20);
		idPanel.add(idField);
		
		final JPanel center = new JPanel();
		center.setLayout(new FlowLayout(FlowLayout.LEADING));
		{
			final JPanel compose = new JPanel(new GridLayout(2, 1));
			compose.add(idPanel);
			compose.add(tipLabel);
			
			compose.setBorder(new TitledBorder("Project ID"));
			int idxGridY = 1;
			{
				final JPanel composeAndVer = new JPanel(new GridBagLayout());
				final GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0,
						GridBagConstraints.WEST, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 0);
				
				composeAndVer.add(compose, c);
				c.gridy = idxGridY++;
				composeAndVer.add(verPanel, c);
				final JPanel upgradePanel = new JPanel(new BorderLayout());
				{
					final JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
					urlField.setColumns(30);
					urlField.getDocument().addDocumentListener(new DocumentListener() {
						private void modify(){
							final String newURL = urlField.getText().trim();
							testHADBtn.setEnabled(newURL.length() > 0);
							((HPProject)currItem).upgradeURL = newURL;
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
					urlPanel.add(new JLabel("URL : "));
					urlPanel.add(urlField);
					testHADBtn.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							final Properties had = new Properties();
							try{
								final String url = urlField.getText();
								ResourceUtil.loadFromURL(had, url);
								
								final JPanel jpanel = new JPanel(new BorderLayout());
								final StringBuffer sb = new StringBuffer();
								sb.append("<html>");
								final Enumeration<Object> en = had.keys();
								boolean isHead = true;
								
								while(en.hasMoreElements()){
									final Object key = en.nextElement();
									if(isHead){
										isHead = false;
									}else{
										sb.append("<br>");
									}
									sb.append("<strong>" + key + "</strong>: " + had.getProperty((String)key));
								}
								sb.append("</html>");
								
								jpanel.add(new JLabel(sb.toString(), App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING), 
										BorderLayout.CENTER);
								App.showCenterPanelMain(jpanel, 0, 0, ResourceUtil.getInfoI18N(), 
										false, null, null, null, null, designer, true, false, null, false, false);
							}catch (final Exception ex) {
								final JPanel jpanel = new JPanel(new BorderLayout());
								jpanel.add(new JLabel("fail to connect server file.", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), 
										BorderLayout.CENTER);
								App.showCenterPanelMain(jpanel, 0, 0, (String) ResourceUtil.get(IContext.ERROR), 
										false, null, null, null, null, designer, true, false, null, false, false);
							}
						}
					}, threadPoolToken));
					urlPanel.add(testHADBtn);
					
					upgradePanel.add(urlPanel, BorderLayout.NORTH);
					upgradePanel.add(new JLabel("<html>the auto upgrade URL of new version of current project. if no upgrade, please keep blank." +
							"<br><br>for example, <strong>http://example.com/dir_or_virtual/tv.had</strong> , <strong>NOTE:</strong> it is <strong>had</strong> file, not <strong>har</strong> file." +
							"<br>please put both <strong>tv.har</strong>, <strong>tv.had</strong> in directory <strong>dir_or_virtual</strong> for download." +
							"<br><br><strong>had</strong> file provides version information which is used to determine upgrade or not." +
							"<br>click <strong>Save as</strong> button, <strong>had</strong> file is automatically created with <strong>har</strong> file if url is not blank.</html>"),
							BorderLayout.CENTER);
					upgradePanel.setBorder(new TitledBorder("Upgrade URL"));
				}
				c.gridy = idxGridY++;
				composeAndVer.add(upgradePanel, c);
				
				c.gridy = idxGridY++;
				composeAndVer.add(buildExceptionPanel(), c);
				
				c.gridy = idxGridY++;
				desc.setColumns(30);
				desc.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)currItem).desc = desc.getText();
						notifyModified(true);
					}
				});
				composeAndVer.add(buildItemPanel(desc, "Description : ", 30), c);
				c.gridy = idxGridY++;
				license.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)currItem).license = license.getText();
						notifyModified(true);
					}
				});
				composeAndVer.add(buildItemPanel(license, "Text license URL for current project : ", 30), c);
				c.gridy = idxGridY++;
				contact.setColumns(30);
				contact.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)currItem).contact = contact.getText();
						notifyModified(true);
					}
				});
				composeAndVer.add(buildItemPanel(contact, "Contact : ", 30), c);
				c.gridy = idxGridY++;
				copyright.setColumns(30);
				copyright.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)currItem).copyright = copyright.getText();
						notifyModified(true);
					}
				});
				composeAndVer.add(buildItemPanel(copyright, "Copyright : ", 30), c);
				
				final VerTextField verField = verPanel.verTextField;
				verField.getDocument().addDocumentListener(new DocumentListener() {
					private void modify(){
						((HPProject)currItem).ver = getHarVersionFromEdit();
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
						((HPProject)currItem).ver = verField.getText();
						App.invokeLaterUI(updateTreeRunnable);
						notifyModified(true);
					}
				}, threadPoolToken));

				center.add(composeAndVer);
			}
		}
		
		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab((String)ResourceUtil.get(9095), new JScrollPane(center));
		{
			final JLabel noteLabel = new JLabel("<html><BR><font color='red'>Note : </font><BR>" +
					"1. these permissions are NOT for running-time, they are for designing-time.<BR>" +
					"2. for running-time, please click /Shift Project/{project}/Modify|Permission.<BR>" +
					"3. these permissions for running-time will hold its value even if the project is upgraded/re-activated/restarted.<BR>" +
//					"3. these permissions works on standard J2SE JVM, NOT for server on Android or other.<BR>" +
					"&nbsp;</html>");
//			noteLabel.setHorizontalAlignment(SwingConstants.CENTER);
			//new JSeparator(SwingConstants.HORIZONTAL), 
			final JComponent[] components = {noteLabel, new JScrollPane(buildPermissionPanel())};
			final JPanel buildNorthPanel = ServerUIUtil.buildNorthPanel(components, 0, BorderLayout.CENTER);
			tabbedPane.addTab((String)ResourceUtil.get(9094), buildNorthPanel);
		}
		
		add(tabbedPane, BorderLayout.CENTER);		
	}
	
	private final JPanel buildPermissionPanel(){
//		JPanel permissionPanel = new JPanel(new FlowLayout());
		perm_write.setToolTipText(HCPermissionConstant.WRITE_TIP);
		perm_exec.setToolTipText(HCPermissionConstant.EXECUTE_TIP);
		perm_del.setToolTipText(HCPermissionConstant.DELETE_TIP);
		perm_exit.setToolTipText(HCPermissionConstant.EXIT_TIP);
		
		checkReadProperty.setToolTipText(HCPermissionConstant.READ_PROP_TIP);
		checkWriteProperty.setToolTipText(HCPermissionConstant.WRITE_PROP_TIP);
		perm_memAccessSystem.setToolTipText(HCPermissionConstant.MEMBER_ACCESS_SYSTEM_TIP);
		
		checkLoadLib.setToolTipText(HCPermissionConstant.LOAD_LIB_TIP);
		checkRobot.setToolTipText(HCPermissionConstant.ROBOT_TIP);
//		checkListenAllAWTEvents.setToolTipText(HCPermissionConstant.LISTEN_ALL_AWT_EVENTS_TIP);
//		checkAccessClipboard.setToolTipText(HCPermissionConstant.ACCESS_CLIPBOARD_TIP);
		checkShutdownHooks.setToolTipText(HCPermissionConstant.SHUTDOWN_HOOKS_TIP);
		checkSetIO.setToolTipText(HCPermissionConstant.SETIO_TIP);
		checkSetFactory.setToolTipText(HCPermissionConstant.SET_FACTORY_TIP);
		
		perm_sock_panel = new SocketEditPanel(){
			@Override
			public void notifyModify(){
				notifyModified(true);
			}
			
			@Override
			public void notifyLostEditPanelFocus(){
				((HPProject)currItem).csc.saveToMap();
			}
			
			@Override
			public final ContextSecurityConfig getCSCSource(){
				return ((HPProject)currItem).csc;
			}
		};
		
		checkReadProperty.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setSysPropRead(checkReadProperty.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));		
		checkWriteProperty.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setSysPropWrite(checkWriteProperty.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
		checkLoadLib.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setLoadLib(checkLoadLib.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
		checkRobot.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setRobot(checkRobot.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
//		checkListenAllAWTEvents.addItemListener(new HCActionListener(new Runnable() {
//			@Override
//			public void run() {
//				((HPProject)currItem).csc.setListenAllAWTEvents(checkListenAllAWTEvents.isSelected());
//				notifyModified(true);	
//			}
//		}, threadPoolToken));	
//		checkAccessClipboard.addItemListener(new HCActionListener(new Runnable() {
//			@Override
//			public void run() {
//				((HPProject)currItem).csc.setAccessClipboard(checkAccessClipboard.isSelected());
//				notifyModified(true);	
//			}
//		}, threadPoolToken));	
		checkShutdownHooks.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setShutdownHooks(checkShutdownHooks.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
		checkSetIO.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setSetIO(checkSetIO.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
		checkSetFactory.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setSetFactory(checkSetFactory.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
		perm_write.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setWrite(perm_write.isSelected());
				notifyModified(true);				
			}
		}, threadPoolToken));	
		perm_exec.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setExecute(perm_exec.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
		perm_del.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setDelete(perm_del.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
		perm_exit.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setExit(perm_exit.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
		perm_memAccessSystem.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)currItem).csc.setMemberAccessSystem(perm_memAccessSystem.isSelected());
				notifyModified(true);	
			}
		}, threadPoolToken));	
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
//		sysOtherPropPanel.add(checkListenAllAWTEvents);
//		sysOtherPropPanel.add(checkAccessClipboard);
		final JComponent[] components = {osPermPanel, new JSeparator(SwingConstants.HORIZONTAL), 
				sysPropPanel, new JSeparator(SwingConstants.HORIZONTAL), 
				perm_sock_panel, new JSeparator(SwingConstants.HORIZONTAL),
				sysOtherPropPanel
				};		
		return ServerUIUtil.buildNorthPanel(components, 0, BorderLayout.CENTER);
//		return permissionPanel;
	}
	
	@Override
	public void notifyLostEditPanelFocus(){
		perm_sock_panel.notifyLostEditPanelFocus();
		super.notifyLostEditPanelFocus();
	}

	@Override
	public void extendInit(){
		final HPProject hpProject = (HPProject)currItem;
		
		idField.setText(hpProject.id);
		verPanel.verTextField.setText(hpProject.ver);
		urlField.setText(hpProject.upgradeURL);
		exceptionField.setText(hpProject.exceptionURL);
		license.setText(hpProject.license);
		desc.setText(hpProject.desc);
		copyright.setText(hpProject.copyright);
		contact.setText(hpProject.contact);
		
		final ContextSecurityConfig csc = hpProject.csc;
		
		perm_write.setSelected(csc.isWrite());
		perm_exec.setSelected(csc.isExecute());
		perm_del.setSelected(csc.isDelete());
		perm_exit.setSelected(csc.isExit());
		
		checkReadProperty.setSelected(csc.isSysPropRead());
		checkWriteProperty.setSelected(csc.isSysPropWrite());
		perm_memAccessSystem.setSelected(csc.isMemberAccessSystem());
		
		checkLoadLib.setSelected(csc.isLoadLib());
		checkRobot.setSelected(csc.isRobot());
//		checkListenAllAWTEvents.setSelected(csc.isListenAllAWTEvents());
//		checkAccessClipboard.setSelected(csc.isAccessClipboard());
		checkShutdownHooks.setSelected(csc.isShutdownHooks());
		checkSetIO.setSelected(csc.isSetIO());
		checkSetFactory.setSelected(csc.isSetFactory());
		
		perm_sock_panel.refresh(csc);
	}
	
	private JPanel buildItemPanel(final JTextField field, final String label, final int colNum){
		field.setColumns(colNum);
		
		final JPanel tmpPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		tmpPanel.setBorder(new TitledBorder(label));
//		tmpPanel.add(new JLabel(label));
		tmpPanel.add(field);
		
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
