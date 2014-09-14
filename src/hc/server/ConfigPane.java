package hc.server;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.res.ImageSrc;
import hc.server.ui.ClientDesc;
import hc.server.ui.NumberFormatTextField;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;
import hc.util.UILang;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.table.AbstractTableModel;

public class ConfigPane extends SingleJFrame {
	final JPanel appearancePane = new JPanel();
	final JPanel thirdLibsPane = new JPanel();
	final JPanel securityPane = new JPanel();
	
	private PropertiesSet extLookFeel = new PropertiesSet(PropertiesManager.S_USER_LOOKANDFEEL);
	private Vector<String> addLookFeel;
	private String newNameLookFeelLib;
	private File CurrLookFeelJar;
	private final int vgap = 5, hgap = 5;
	private ConfigValueGroup group = new ConfigValueGroup();
	
	public ConfigPane(){
		
		setIconImage(App.SYS_LOGO);
		setTitle((String)ResourceUtil.get(9040));

		final JFrame self = this;
		
		{
			final String[] fontlist =
					   GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			final JComboBox fontNames = new JComboBox(fontlist);
			final NumberFormatTextField fontSize = new NumberFormatTextField();
			final String oldName = PropertiesManager.getValue(PropertiesManager.C_FONT_NAME, "Dialog");
			final String oldFontSize = PropertiesManager.getValue(PropertiesManager.C_FONT_SIZE, "16");
			final ConfigValue cvSize = new ConfigValue(PropertiesManager.C_FONT_SIZE, oldFontSize, group) {
				@Override
				public String getNewValue() {
					return fontSize.getText();
				}
				
				@Override
				public void applyBiz(boolean isCancel) {
				}
			};
			fontSize.setColumns(5);
			fontSize.setText((String)cvSize.getOldValue());

			final JCheckBox useSysFont = new JCheckBox((String)ResourceUtil.get(9057));

			ConfigValue cvName = new ConfigValue(PropertiesManager.C_FONT_NAME, oldName, group) {
				@Override
				public void applyBiz(boolean isCancel) {
					if(useSysFont.isSelected() == false){
						final String size = group.getValueForApply(PropertiesManager.C_FONT_SIZE);
						App.initGlobalFontSetting(new Font(group.getValueForApply(PropertiesManager.C_FONT_NAME), 
								Font.PLAIN, Integer.parseInt(size)));
						self.repaint();
					}
				}
				
				@Override
				public boolean isChanged(){
					return super.isChanged() || cvSize.isChanged();
				}

				@Override
				public String getNewValue() {
					return (String)fontNames.getSelectedItem();
				}
			};
			fontNames.setSelectedItem(oldName);
			
			final JPanel flowPane = new JPanel();
			flowPane.setLayout(new FlowLayout(FlowLayout.LEADING, hgap, vgap));
			flowPane.add(new JLabel((String)ResourceUtil.get(9037)));
			flowPane.add(fontNames);
			flowPane.add(new JLabel((String)ResourceUtil.get(9038)));
			flowPane.add(fontSize);
			
			JPanel fontTotalPanel = new JPanel(new BorderLayout());
			fontTotalPanel.setBorder(new TitledBorder(""));
			useSysFont.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					final boolean selected = useSysFont.isSelected();
					fontNames.setEnabled(!selected);
					fontSize.setEnabled(!selected);
				}
			});
			final String isSysFont = PropertiesManager.getValue(PropertiesManager.C_SYSTEM_DEFAULT_FONT_SIZE);
			if(isSysFont == null){
				useSysFont.setSelected(true);
			}else{
				useSysFont.setSelected(IConstant.TRUE.equals(isSysFont));
			}
			ConfigValue cvSysDefaultFont = new ConfigValue(PropertiesManager.C_SYSTEM_DEFAULT_FONT_SIZE, isSysFont, group) {
				@Override
				public void applyBiz(boolean isCancel) {
					if(useSysFont.isSelected()){
						App.restoreDefaultGlobalFontSetting();
						self.repaint();
					}
				}
				

				@Override
				public String getNewValue() {
					return useSysFont.isSelected()?IConstant.TRUE:IConstant.FALSE;
				}
			};
			fontTotalPanel.add(useSysFont, BorderLayout.NORTH);
			fontTotalPanel.add(flowPane, BorderLayout.CENTER);
			appearancePane.setLayout(new BoxLayout(appearancePane, BoxLayout.Y_AXIS));//new FlowLayout(FlowLayout.LEADING, hgap, vgap));
			appearancePane.add(fontTotalPanel);
			
			
			final JComboBox skins = new JComboBox();
			skins.setModel(new DefaultComboBoxModel(getInstalledLookAndFeelNames(null)));
			skins.setSelectedItem(getSystemSkin());
			final ConfigValue skincf = new ConfigValue(PropertiesManager.C_SKIN, (String)skins.getSelectedItem(), group) {
				
				@Override
				public String getNewValue() {
					return (String)skins.getSelectedItem();
				}
				
				@Override
				public void applyBiz(boolean isCancel) {
					String newSkin = group.getValueForApply(PropertiesManager.C_SKIN);
					
					App.applyLookFeel(newSkin, null);
				};
			};
			
			{
//				appearancePene.add(new JSeparator(JSeparator.HORIZONTAL));
				
				JPanel skinPanel = new JPanel();
				skinPanel.setBorder(new TitledBorder(""));
				skinPanel.setLayout(new FlowLayout(FlowLayout.LEADING, hgap, vgap));
				skinPanel.add(new JLabel((String)ResourceUtil.get(9042)));
				skinPanel.add(skins);
				
				{
					final JButton addSkin = new JButton((String)ResourceUtil.get(9043));
					skinPanel.add(addSkin);
					final String dependMsg = "<BR>if it depends on a core jar, please import the core jar first in tab of 3rd libs.";
					addSkin.setToolTipText("<html>add and copy skin lib jar to current system." +
							dependMsg +
							"</html>");
					addSkin.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							JPanel wPanel = new JPanel();
							wPanel.add(new JLabel(
									"<HTML>Some LookAndFeel skin jar lib may cause error. " +
									"Recommend to use build-in system skin." +
									"<BR>" + dependMsg + "<BR>" +
									"<BR>click OK to continue</HTML>", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEFT));
							App.showCenterPanel(wPanel, 0, 0, "Skin Warning", true, null, 
									null, new ActionListener() {
@Override
public void actionPerformed(ActionEvent e) {
							File file = FileSelector.selectImageFile(addSkin, FileSelector.JAR_FILTER, true);
							if(file == null){
								return;
							}
							
							Vector<String> list = null;
							try {
								ResourceUtil.loadJar(file);
								list = getSubClasses(new JarFile(file), BasicLookAndFeel.class);
								
								if(list.size() == 0){
									JOptionPane.showConfirmDialog(null, 
											"<html>No LookAndFeel resource found in library!" +
											"<BR><BR>please make sure the common or third lib which is depended is added before.</html>", 
											"No LookAndFeel", JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE, App.getSysIcon(App.SYS_WARN_ICON));
									return;
								}
								
								String libName = ThirdlibManager.createLibName(self,
										file);
								if(libName == null){
									return;
								}
								
								newNameLookFeelLib = libName;
								CurrLookFeelJar = file;
								addLookFeel = list;
							} catch (Exception e1) {
								return;
							}
							
							
							skins.setModel(new DefaultComboBoxModel(getInstalledLookAndFeelNames(list)));
							skins.setSelectedItem(list.elementAt(0));
							
							self.pack();
							App.showCenter(self);
}
}, null, self, true, false, null, false, false);
						}
					});
					
					{
//						JButton downloadLib = new JButton((String)ResourceUtil.get(9044));
//						downloadLib.addActionListener(new ActionListener() {
//							@Override
//							public void actionPerformed(ActionEvent e) {
//								try {
//									HttpUtil.browseLangURL(HttpUtil.buildLangURL("pc/skin.htm", null));
//								} catch (UnsupportedEncodingException e1) {
//								}
//							}
//						});
//						downloadLib.setToolTipText("goto download skin jar lib website");
//						skinPanel.add(downloadLib);
					}
				}
				
				appearancePane.add(skinPanel);
			}
		}
		
		{
			securityPane.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
			
			String tooltip = "If err password or certification login some times, then lock ID some minutes.";
			
			JPanel panel = new JPanel();
			panel.setLayout(new FlowLayout(FlowLayout.LEADING, ClientDesc.hgap, ClientDesc.vgap));
			JLabel label = new JLabel(new ImageIcon(ResourceUtil.getResource("hc/res/errpwd_22.png")));
			panel.add(label);
			label.setToolTipText(tooltip);
			label = new JLabel(" + ");
			panel.add(label);
			label.setToolTipText(tooltip);
			label = new JLabel(new ImageIcon(ResourceUtil.getResource("hc/res/errck_22.png")));
			panel.add(label);
			panel.add(new JLabel(" >= "));
			label.setToolTipText(tooltip);
			final NumberFormatTextField jtfMaxTimers = new NumberFormatTextField();
			jtfMaxTimers.setText(String.valueOf(SystemLockManager.getConfigErrTry()));
			jtfMaxTimers.setColumns(5);
			panel.add(jtfMaxTimers);
			
			panel.add(new JLabel(", "));
			panel.add(new JLabel(new ImageIcon(ImageSrc.LOCK_ICON)));
			final NumberFormatTextField jtfLockMinutes = new NumberFormatTextField();
			jtfLockMinutes.setText(String.valueOf(SystemLockManager.getConfigLockMinutes()));
			jtfLockMinutes.setColumns(5);
			panel.add(jtfLockMinutes);
			panel.add(new JLabel(new ImageIcon(ResourceUtil.getResource("hc/res/timer_22.png"))));
			panel.add(new JLabel((String)ResourceUtil.get(9047)));
			
			ConfigValue cvErrTry = new ConfigValue(PropertiesManager.PWD_ERR_TRY, jtfMaxTimers.getText(), group){
				@Override
				public String getNewValue() {
					return jtfMaxTimers.getText();
				}

				@Override
				public void applyBiz(boolean isCancel) {
					PropertiesManager.setValue(PropertiesManager.PWD_ERR_TRY, 
							group.getValueForApply(PropertiesManager.PWD_ERR_TRY));
					SystemLockManager.updateMaxLock();
				}
			};
			
			ConfigValue cvLockMinutes = new ConfigValue(PropertiesManager.PWD_ERR_LOCK_MINUTES, jtfLockMinutes.getText(), group){
				@Override
				public String getNewValue() {
					return jtfLockMinutes.getText();
				}

				@Override
				public void applyBiz(boolean isCancel) {
					PropertiesManager.setValue(PropertiesManager.PWD_ERR_LOCK_MINUTES, 
							group.getValueForApply(PropertiesManager.PWD_ERR_LOCK_MINUTES));
					SystemLockManager.updateMaxLock();
				}
			};
			
			
			final JComboBox network = new JComboBox();
			final Object[] array = HttpUtil.getAllNetworkInterfaces().toArray();
			network.setModel(new DefaultComboBoxModel(array));
			String selectedName = PropertiesManager.getValue(PropertiesManager.p_selectedNetwork);
			if(selectedName == null){
				network.setSelectedItem(array[0]);
			}else{
				network.setSelectedItem(selectedName);
			}

			final ConfigValueNetwork cvNetwork = new ConfigValueNetwork(network, self, PropertiesManager.p_selectedNetwork, (String)network.getSelectedItem(), group);
			
			final NumberFormatTextField networkPort = new NumberFormatTextField();
			networkPort.setText(PropertiesManager.getValue(PropertiesManager.p_selectedNetworkPort, "0"));
			ConfigValue cvNetworkPort = new ConfigValue(PropertiesManager.p_selectedNetworkPort, (String)networkPort.getText(), group){
				String newOldValue = getOldValue();
				String realWorkingValue = getOldValue();
				@Override
				public String getNewValue() {
					return (String)networkPort.getText();
				}

				@Override
				public void applyBiz(boolean isCancel) {
					if(isCancel){
						newOldValue = getOldValue();
					}
					PropertiesManager.setValue(PropertiesManager.p_selectedNetworkPort, 
							group.getValueForApply(PropertiesManager.p_selectedNetworkPort));
					if(isCancel && realWorkingValue.equals(getOldValue())){
					}else if((getNewValue().equals(newOldValue) == false)
							||(getNewValue().equals(realWorkingValue) == false)){
						ConfigPane.rebuildConnection(self, cvNetwork);
					}
					newOldValue = getNewValue();
				}
			};
			JPanel networkPane = new JPanel(new BorderLayout(0, vgap));
			panel.setBorder(new TitledBorder("Security : "));
			networkPane.add(panel, BorderLayout.NORTH);
			JPanel borderPane = new JPanel(new BorderLayout());
			borderPane.setBorder(new TitledBorder("Network Interface : "));
			JPanel descPane = new JPanel(new BorderLayout());
			descPane.add(new JLabel("Description:"), BorderLayout.NORTH);
			final JLabel descLabel = new JLabel("<html>Mobile phone try connect to network interface directly via 3G/WiFi." +
					"<BR>If fail, relay server (HomeCenter.MOBI exchange data between them) is used." +
					"<BR>Rebuilding connection is required after network interface is changed!" +
					"</html>");
			descLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			//descLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			descPane.add(descLabel, BorderLayout.CENTER);
			borderPane.add(descPane, 
					BorderLayout.CENTER);
			{
				JPanel networkPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
				networkPanel.add(network);
				JLabel port = new JLabel("local port : ");
				port.setToolTipText("<html>" +
						"0-65535, 0:select random port" +
						"<br>if selected port is used, then system will select random port for direct server." +
						"</html>");
				networkPanel.add(port);
				networkPort.setColumns(6);
				networkPanel.add(networkPort);
				
				borderPane.add(networkPanel, BorderLayout.NORTH);
			}
			networkPane.add(borderPane, BorderLayout.CENTER);
			securityPane.add(networkPane, BorderLayout.NORTH);
		}

		final HCTablePanel tablePanel;
		
		{
			thirdLibsPane.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
			
			final int MAX_MENUITEM = 30;
			final Object libs[][] = new Object[MAX_MENUITEM][1];
			for (int i = 0; i < MAX_MENUITEM; i++) {
				libs[i][0] = "";
			}
			
			final JButton upBut = new JButton((String)ResourceUtil.get(9019), new ImageIcon(ImageSrc.UP_SMALL_ICON));
			final JButton downBut = new JButton((String)ResourceUtil.get(9020), new ImageIcon(ImageSrc.DOWN_SMALL_ICON));
			final JButton removeBut = new JButton((String)ResourceUtil.get(9018), new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
			final JButton importBut = new JButton((String)ResourceUtil.get(9016), new ImageIcon(ImageSrc.ADD_SMALL_ICON));

			importBut.setToolTipText("the imported jar library is working after click '" + importBut.getText()+ "' button.");
			removeBut.setToolTipText("the removed jar library is still working after click '" + removeBut.getText() + "' button, restart HomeCenter Server is needed!");
			upBut.setToolTipText("you need to restart HomeCenter server to make it effect.");
			downBut.setToolTipText(upBut.getToolTipText());
			
			final Object[] colNames = {(String)ResourceUtil.get(9046)};
			Object[] defaultRow = {""};
			final AbstractTableModel tableModel = new AbstractTableModel() {
				@Override
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
					libs[rowIndex][columnIndex] = aValue;
				}
				
				@Override
				public void removeTableModelListener(TableModelListener l) {
				}
				
				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				
				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					return libs[rowIndex][columnIndex];
				}
				
				@Override
				public int getRowCount() {
					return libs.length;
				}
				
				@Override
				public String getColumnName(int columnIndex) {
					return colNames[columnIndex].toString();
				}
				
				@Override
				public int getColumnCount() {
					return colNames.length;
				}
				
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					return String.class;
				}
				
				@Override
				public void addTableModelListener(TableModelListener l) {
				}
			};

			tablePanel = new HCTablePanel(tableModel, libs, colNames, defaultRow, 0, 
					upBut, downBut, removeBut, importBut, null,
					//upOrDown
					new AbstractDelayBiz(null) {
						@Override
						public void doBiz() {
							HCTablePanel currTablePane = (HCTablePanel)getPara();
							final Object[] objs = currTablePane.getColumnAt(0);
							for (int i = 0; i < objs.length; i++) {
								objs[i] = ThirdlibManager.buildPath(ThirdlibManager.removePath(objs[i].toString(), false));
							}
							ThirdlibManager.refill(objs);
						}
					},
					//remove
					new AbstractDelayBiz(null) {
						@Override
						public void doBiz() {
							final AbstractDelayBiz selfBiz = this;
							final Object[] rows = (Object[])getPara();
							JPanel askPanel = new JPanel();
							askPanel.add(new JLabel("Are you sure to remove jar lib?", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEFT));
							App.showCenterPanel(askPanel, 0, 0, "Confirm Delete?", true, null, null, new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									ThirdlibManager.removeLib((String)rows[0], true);
									boolean[] back = {true};
									selfBiz.setPara(back);
								}
							}, null, self, true, false, null, false, false);
						}
					}, 
					//import
					new AbstractDelayBiz(null) {
						@Override
						public void doBiz() {
							File newJarFile = FileSelector.selectImageFile(importBut, FileSelector.JAR_FILTER, true);
							if(newJarFile != null){
								String newLibName = ThirdlibManager.createLibName(self, newJarFile);
								if(newLibName != null){
									ThirdlibManager.addLib(newJarFile, newLibName);
									
									Object[] libName = {newLibName + ThirdlibManager.EXT_JAR};
									setPara(libName);
									return;
								}
							}
							setPara(null);
						}
					}, false);
			
			JPanel buttonsList = new JPanel();
			buttonsList.setLayout(new GridLayout(1, 4, ClientDesc.hgap, ClientDesc.vgap));
			buttonsList.add(upBut);
			buttonsList.add(downBut);
			buttonsList.add(removeBut);
			buttonsList.add(importBut);

			final JScrollPane scrollpane = new JScrollPane(tablePanel.table);
			scrollpane.setPreferredSize(new Dimension(300, 100));
			thirdLibsPane.add(scrollpane, BorderLayout.CENTER);
			thirdLibsPane.add(buttonsList, BorderLayout.NORTH);
		}
		
		final String third_lib_title = (String)ResourceUtil.get(9046);
		JTabbedPane centerPanel = new JTabbedPane();

		centerPanel.add((String)ResourceUtil.get(9039), appearancePane);
		centerPanel.add((String)ResourceUtil.get(9045), securityPane);
		centerPanel.add(third_lib_title, thirdLibsPane);
		
		final JButton ok = new JButton((String) ResourceUtil.get(IContext.OK), new ImageIcon(ImageSrc.OK_ICON));

		centerPanel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
		        int index = sourceTabbedPane.getSelectedIndex();
		        
		        //进入3方库添加面板时，关闭确认按钮
		        if(sourceTabbedPane.getTitleAt(index).equals(third_lib_title)){
		        	ok.setEnabled(false);
		        }else{
		        	ok.setEnabled(true);
		        }
			}
		});
		
		setLayout(new BorderLayout(hgap, vgap));
		add(centerPanel, BorderLayout.CENTER);
		
		final JPanel bottonButtons = new JPanel();
		final JButton cancel = new JButton((String)ResourceUtil.get(1018), new ImageIcon(ImageSrc.CANCEL_ICON));
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				group.doCancel();
				exit();
			}
		});
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				group.doSave();
				
				if(CurrLookFeelJar != null && newNameLookFeelLib != null 
						&& addLookFeel.contains(UIManager.getLookAndFeel().getClass().getName())){
					ThirdlibManager.addLib(CurrLookFeelJar, newNameLookFeelLib);
					final int size = addLookFeel.size();
					for (int i = 0; i < size; i++) {
						extLookFeel.appendItem(addLookFeel.elementAt(i));
					}
					extLookFeel.save();
				}

				try{
					((J2SEContext)ContextManager.getContextInstance()).buildMenu(UILang.getUsedLocale());
					SingleJFrame.updateAllJFrame();
				}catch (Throwable t) {
				}
				exit();
			}
		});
		final JButton apply = new JButton((String) ResourceUtil.get(9041), new ImageIcon(ResourceUtil.getResource("hc/res/test_22.png")));
		apply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				group.applyAll();
				updateFrameUI();
			}
		});
		bottonButtons.setLayout(new FlowLayout(FlowLayout.RIGHT, hgap, vgap));
		bottonButtons.add(ok);
		bottonButtons.add(cancel);
		bottonButtons.add(apply);

		centerPanel.addChangeListener(new ChangeListener() {
			boolean isEnterThirdLib = false;
			@Override
			public void stateChanged(ChangeEvent e) {
				if(e.getSource() instanceof JTabbedPane){
					JTabbedPane sourceTabbedPane = (JTabbedPane)e.getSource();
					final String titleAt = sourceTabbedPane.getTitleAt(sourceTabbedPane.getSelectedIndex());
					if(titleAt.equals(third_lib_title)){
						initButtons(true);
						
						//刷新
						Object[][] libs = tablePanel.body;
						final int size = ThirdlibManager.libsSet.size();
						for (int i = 0; i < size; i++) {
							libs[i][0] = ThirdlibManager.removePath(ThirdlibManager.libsSet.getItem(i), true);
						}
						
						tablePanel.refresh(size);
					}else{
						initButtons(false);
					}
				}
			}
			private void initButtons(boolean isEnter) {
				if(isEnter != isEnterThirdLib){
					isEnterThirdLib = isEnter;
	
					cancel.setEnabled(!isEnter);
					apply.setEnabled(!isEnter);
				}
			}
		});

		add(bottonButtons, BorderLayout.SOUTH);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				cancelConfig();
			}
		});

		getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancelConfig();
			}},
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		pack();
		App.showCenter(this);
	}

	public static final String SYS_LOOKFEEL = "System - ";

	private Object[] getInstalledLookAndFeelNames(Vector<String> tryList) {
		Vector<String> list = new Vector<String>();
		
		LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels();
		for (int i = 0; i < looks.length; i++) {
			list.add(SYS_LOOKFEEL + looks[i].getName());
		}
		
		final int size = extLookFeel.size();
		boolean isLost = false;
		for (int i = size - 1; i >= 0; i--) {
			final String item = extLookFeel.getItem(i);
			try{
				Class c = Class.forName(item);
				list.add(item);
			}catch (Exception e) {
				extLookFeel.delItem(item);
				isLost = true;
			}
		}
		if(isLost){
			extLookFeel.save();
		}
		
		if(tryList != null){
			final int total = tryList.size();
			for (int i = 0; i < total; i++) {
				list.add(tryList.elementAt(i));
			}
		}
		
		return list.toArray();
	}

	private void exit() {
		dispose();
	}

	private void updateFrameUI() {
		SingleJFrame.updateAllJFrame();
		pack();
		
//		如果用户移动了窗口，会产生跳动情形，故关闭showCenter
//		App.showCenter(this);
	}
	
	private void cancelConfig() {
		group.doCancel();
		SingleJFrame.updateAllJFrame();
		exit();
	}

	public static void rebuildConnection(final JFrame self, final ConfigValueNetwork cvNetwork) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("<html>setting is changed or canceled, need rebuild connection," +
				"<BR><BR>click '" + (String) ResourceUtil.get(IContext.OK) + "' to rebuild now!</html>"));
		App.showCenterPanel(panel, 0, 0, "rebuild connection now?", true, null, null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				HttpUtil.notifyStopServer(true, self);		

				RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_ServerReq_STR);

				SIPManager.notifyRelineon(false);
				
				cvNetwork.update();
			}
		}, null, self, true, false, null, false, false);
	}

	private static Vector<String> getSubClasses(JarFile jarFile, Class parentClass){
		Vector<String> list = new Vector<String>();
        final String classPre = ".class";
        final int cutLen = classPre.length();
        
		Enumeration<JarEntry> entrys = jarFile.entries();  
	    while (entrys.hasMoreElements()) {  
	        JarEntry jarEntry = entrys.nextElement();  
	        String entryName = jarEntry.getName();
			if(entryName.endsWith(classPre)){
	        	String className = entryName.substring(0, entryName.length() - cutLen).replace('/', '.');
	        	try {
					Class testClass = Class.forName(className);
					if(testClass.asSubclass(parentClass) != null){
						if(testClass.isInterface() || testClass.isLocalClass() || testClass.isMemberClass()){
							
						}else{
							list.add(className);
						}
					}
				} catch (Throwable e) {
				}
	        	
	        }
	    }
	    
	    return list;
	}

	public static void updateComponentUI(final Container container) {
		try{
			final int size = container.getComponentCount();
			for (int i = 0; i < size; i++) {
				Object component = container.getComponent(i);
				if (component instanceof Container) {
					updateComponentUI((Container) component);
				}
	
				if (component instanceof JComponent) {
					((JComponent) component).updateUI();
				}
			}
			
			if (container instanceof JComponent) {
				((JComponent) container).updateUI();
			}
		}catch (Exception e) {
//			e.printStackTrace();
		}
	}

	public static String getSystemSkin() {
		String configSkin = PropertiesManager.getValue(PropertiesManager.C_SKIN);
		if(configSkin == null){
			configSkin = getDefaultSkin();
			if(configSkin != null){
				PropertiesManager.setValue(PropertiesManager.C_SKIN, configSkin);
			}
		}
		return configSkin;
	}

	public static String getDefaultSkin() {
		if(ResourceUtil.isWindowsOS()){
			return SYS_LOOKFEEL + "Nimbus";//"Windows Classic";
		}
		return null;
	}

}

class ConfigValueNetwork extends ConfigValue {
	final JComboBox network;
	final JFrame frame;
	
	public ConfigValueNetwork(final JComboBox network, final JFrame frame, String configKey, String old, ConfigValueGroup group){
		super(configKey, old, group);
		this.network = network;
		this.frame = frame;
	}
	
	String newOldValue = getOldValue();
	String realWorkingValue = getOldValue();
	@Override
	public String getNewValue() {
		return (String)network.getSelectedItem();
	}

	@Override
	public void applyBiz(boolean isCancel) {
		if(isCancel){
			newOldValue = getOldValue();
		}
		PropertiesManager.setValue(PropertiesManager.p_selectedNetwork, 
				group.getValueForApply(PropertiesManager.p_selectedNetwork));
		if(isCancel && realWorkingValue.equals(getOldValue())){
		}else if((getNewValue().equals(newOldValue) == false)
				||(getNewValue().equals(realWorkingValue) == false)){
			ConfigPane.rebuildConnection(frame, this);
			update();
		}
		newOldValue = getNewValue();
	}

	public void update() {
		realWorkingValue = getNewValue();
	}
};