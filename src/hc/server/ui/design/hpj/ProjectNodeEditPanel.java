package hc.server.ui.design.hpj;

import hc.App;
import hc.core.IContext;
import hc.server.HCActionListener;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.HCPermissionConstant;
import hc.server.util.ContextSecurityConfig;
import hc.util.ResourceUtil;
import hc.util.SocketEditPanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
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
	
	SocketEditPanel perm_sock_panel;
	
	public ProjectNodeEditPanel(){
		super();
		
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
				((HPProject)item).id = idField.getText();
				notifyModified();
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
				((HPProject)item).id = idField.getText();
				App.invokeLaterUI(updateTreeRunnable);
				item.getContext().modified.setModified(true);
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
			
			{
				final JPanel composeAndVer = new JPanel(new GridBagLayout());
				final GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0,
						GridBagConstraints.WEST, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 0);
				
				composeAndVer.add(compose, c);
				c.gridy = 1;
				composeAndVer.add(verPanel, c);
				final JPanel upgradePanel = new JPanel(new BorderLayout());
				{
					final JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
					urlField.setColumns(30);
					urlField.getDocument().addDocumentListener(new DocumentListener() {
						private void modify(){
							((HPProject)item).upgradeURL = urlField.getText();
							notifyModified();
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
					final JButton test = new JButton("Test HAD");
					test.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							final Properties had = new Properties();
							try{
								had.load(new URL(urlField.getText()).openStream());
								
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
								App.showCenterPanel(jpanel, 0, 0, (String) ResourceUtil.get(IContext.INFO), 
										false, null, null, null, null, designer, true, false, null, false, false);
							}catch (final Exception ex) {
								final JPanel jpanel = new JPanel(new BorderLayout());
								jpanel.add(new JLabel("fail to connect server file.", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), 
										BorderLayout.CENTER);
								App.showCenterPanel(jpanel, 0, 0, (String) ResourceUtil.get(IContext.ERROR), 
										false, null, null, null, null, designer, true, false, null, false, false);
							}
						}
					}, threadPoolToken));
					urlPanel.add(test);
					
					upgradePanel.add(urlPanel, BorderLayout.NORTH);
					upgradePanel.add(new JLabel("<html>the auto upgrade URL of new version of current project. if no upgrade, please keep blank." +
							"<br><br>for example, <strong>http://example.com/dir_or_virtual/tv.had</strong> , <strong>NOTE:</strong> it is <strong>had</strong> file, not <strong>har</strong> file." +
							"<br>please put both <strong>tv.har</strong>, <strong>tv.had</strong> in directory <strong>dir_or_virtual</strong> for download." +
							"<br><br><strong>had</strong> file provides version information which is used to determine upgrade or not." +
							"<br>click <strong>Save as</strong> button, <strong>had</strong> file is automatically created with <strong>har</strong> file if url is not blank.</html>"),
							BorderLayout.CENTER);
					upgradePanel.setBorder(new TitledBorder("Upgrade URL"));
				}
				c.gridy = 2;
				composeAndVer.add(upgradePanel, c);
				
				c.gridy = 3;
				desc.setColumns(30);
				desc.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)item).desc = desc.getText();
						notifyModified();
					}
				});
				composeAndVer.add(buildItemPanel(desc, "Description : ", 30), c);
				c.gridy = 4;
				license.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)item).license = license.getText();
						notifyModified();
					}
				});
				composeAndVer.add(buildItemPanel(license, "Text license URL for current project : ", 30), c);
				c.gridy = 5;
				contact.setColumns(30);
				contact.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)item).contact = contact.getText();
						notifyModified();
					}
				});
				composeAndVer.add(buildItemPanel(contact, "Contact : ", 30), c);
				c.gridy = 6;
				copyright.setColumns(30);
				copyright.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)item).copyright = copyright.getText();
						notifyModified();
					}
				});
				composeAndVer.add(buildItemPanel(copyright, "Copyright : ", 30), c);
				
				final VerTextField verField = verPanel.verTextField;
				verField.getDocument().addDocumentListener(new DocumentListener() {
					private void modify(){
						((HPProject)item).ver = verField.getText();
						notifyModified();
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
						((HPProject)item).ver = verField.getText();
						App.invokeLaterUI(updateTreeRunnable);
						item.getContext().modified.setModified(true);
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
					"3. these permissions works on standard J2SE JVM, NOT for server on Android or other.<BR>" +
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
		
		perm_sock_panel = new SocketEditPanel(){
			@Override
			public void notifyModify(){
				notifyModified();
			}

			@Override
			public void notifySocketLimitOn(final boolean isOn) {
				ContextSecurityConfig.setSocketLimitOn(((HPProject)item).csc, isOn);
			}

			@Override
			public boolean isSocketLimitOn() {
				return ContextSecurityConfig.isSocketLimitOn(((HPProject)item).csc);
			}
			
			@Override
			public void notifyLostEditPanelFocus(){
				((HPProject)item).csc.saveToMap();
			}
		};
		
		checkReadProperty.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setSysPropRead(checkReadProperty.isSelected());
				notifyModified();	
			}
		}, threadPoolToken));		
		checkWriteProperty.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setSysPropWrite(checkWriteProperty.isSelected());
				notifyModified();	
			}
		}, threadPoolToken));	
		checkLoadLib.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setLoadLib(checkLoadLib.isSelected());
				notifyModified();	
			}
		}, threadPoolToken));	
		checkRobot.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setRobot(checkRobot.isSelected());
				notifyModified();	
			}
		}, threadPoolToken));	
//		checkListenAllAWTEvents.addItemListener(new HCActionListener(new Runnable() {
//			@Override
//			public void run() {
//				((HPProject)item).csc.setListenAllAWTEvents(checkListenAllAWTEvents.isSelected());
//				notifyModified();	
//			}
//		}, threadPoolToken));	
//		checkAccessClipboard.addItemListener(new HCActionListener(new Runnable() {
//			@Override
//			public void run() {
//				((HPProject)item).csc.setAccessClipboard(checkAccessClipboard.isSelected());
//				notifyModified();	
//			}
//		}, threadPoolToken));	
		checkShutdownHooks.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setShutdownHooks(checkShutdownHooks.isSelected());
				notifyModified();	
			}
		}, threadPoolToken));	
		checkSetIO.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setSetIO(checkSetIO.isSelected());
				notifyModified();	
			}
		}, threadPoolToken));	
		perm_write.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setWrite(perm_write.isSelected());
				notifyModified();				
			}
		}, threadPoolToken));	
		perm_exec.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setExecute(perm_exec.isSelected());
				notifyModified();	
			}
		}, threadPoolToken));	
		perm_del.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setDelete(perm_del.isSelected());
				notifyModified();	
			}
		}, threadPoolToken));	
		perm_exit.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setExit(perm_exit.isSelected());
				notifyModified();	
			}
		}, threadPoolToken));	
		perm_memAccessSystem.addItemListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HPProject)item).csc.setMemberAccessSystem(perm_memAccessSystem.isSelected());
				notifyModified();	
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
		
		final JPanel sysOtherPropPanel = new JPanel(new GridLayout(1, 4));
		sysOtherPropPanel.add(checkLoadLib);
		sysOtherPropPanel.add(checkRobot);
		sysOtherPropPanel.add(checkSetIO);
		sysOtherPropPanel.add(checkShutdownHooks);
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
		final HPProject hpProject = (HPProject)item;
		
		idField.setText(hpProject.id);
		verPanel.verTextField.setText(hpProject.ver);
		urlField.setText(hpProject.upgradeURL);
		license.setText(hpProject.license);
		desc.setText(hpProject.desc);
		copyright.setText(hpProject.copyright);
		contact.setText(hpProject.contact);
		
		perm_write.setSelected(hpProject.csc.isWrite());
		perm_exec.setSelected(hpProject.csc.isExecute());
		perm_del.setSelected(hpProject.csc.isDelete());
		perm_exit.setSelected(hpProject.csc.isExit());
		
		checkReadProperty.setSelected(hpProject.csc.isSysPropRead());
		checkWriteProperty.setSelected(hpProject.csc.isSysPropWrite());
		perm_memAccessSystem.setSelected(hpProject.csc.isMemberAccessSystem());
		
		checkLoadLib.setSelected(hpProject.csc.isLoadLib());
		checkRobot.setSelected(hpProject.csc.isRobot());
//		checkListenAllAWTEvents.setSelected(hpProject.csc.isListenAllAWTEvents());
//		checkAccessClipboard.setSelected(hpProject.csc.isAccessClipboard());
		checkShutdownHooks.setSelected(hpProject.csc.isShutdownHooks());
		checkSetIO.setSelected(hpProject.csc.isSetIO());
		
		perm_sock_panel.refresh(hpProject.csc);
	}
	
	private JPanel buildItemPanel(final JTextField field, final String label, final int colNum){
		field.setColumns(colNum);
		
		final JPanel tmpPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		tmpPanel.setBorder(new TitledBorder(label));
//		tmpPanel.add(new JLabel(label));
		tmpPanel.add(field);
		
		return tmpPanel;
	}
}
