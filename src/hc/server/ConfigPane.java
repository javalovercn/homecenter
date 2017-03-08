package hc.server;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.util.ExceptionReporter;
import hc.core.util.ThreadPriorityManager;
import hc.res.ImageSrc;
import hc.server.data.KeyComperPanel;
import hc.server.ui.ClientDesc;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.NumberFormatTextField;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.Designer;
import hc.server.ui.design.code.CodeHelper;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.server.util.ExceptionViewer;
import hc.util.BaseResponsor;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.table.AbstractTableModel;

public class ConfigPane extends SingleJFrame {
	public static final int OPTION_APPLY = 1;
	public static final int OPTION_OK = 2;
	public static final int OPTION_CANCEL = 3;
	
	final String third_lib_title = (String)ResourceUtil.get(9046);
	Vector<ThirdLibValue> deledLibs = new Vector<ThirdLibValue>();
	boolean addLib = false;
	boolean isMovedLibIdx = false;
	Vector<String> cancelAddedSkinLibNames = new Vector<String>();
	boolean isNeedShutdownAndRestart = false;
	final JPanel thirdLibsPane = new JPanel();
	final JPanel securityPane = new JPanel();
	final JPanel developerPane = new JPanel();
	final JPanel otherPane = new JPanel();
	
	final HCTablePanel tablePanel;
	private final PropertiesSet extLookFeel = new PropertiesSet(PropertiesManager.S_USER_LOOKANDFEEL);
	private Vector<String> addLookFeel;
	private String newNameLookFeelLib;
	private File CurrLookFeelJar;
	private final int vgap = 5, hgap = 5;
	private final ConfigValueGroup group;
	
	private JPanel buildAppearancePane(final JFrame self){
		final JPanel appearancePane = new JPanel();
		
		{
			final String[] fontlist =
					   GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			final JComboBox fontNames = new JComboBox(fontlist);
			final NumberFormatTextField fontSize = new NumberFormatTextField(DefaultManager.DEFAULT_FONT_SIZE);
			final String oldName = PropertiesManager.getValue(PropertiesManager.C_FONT_NAME, "Dialog");
			final String oldFontSize = PropertiesManager.getValue(PropertiesManager.C_FONT_SIZE, DefaultManager.DEFAULT_FONT_SIZE);
			final ConfigValue cvSize = new ConfigValue(PropertiesManager.C_FONT_SIZE, oldFontSize, group) {
				@Override
				public String getNewValue() {
					return fontSize.getText();
				}
				
				@Override
				public void applyBiz(final int option) {
				}
			};
			fontSize.setColumns(5);
			fontSize.setText(cvSize.getOldValue());

			final JCheckBox useSysFont = new JCheckBox((String)ResourceUtil.get(9057));

			new ConfigValue(PropertiesManager.C_FONT_NAME, oldName, group) {
				@Override
				public void applyBiz(final int option) {
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
			
			final JPanel fontTotalPanel = new JPanel(new BorderLayout());
			fontTotalPanel.setBorder(new TitledBorder(""));
			useSysFont.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent e) {
					final boolean selected = useSysFont.isSelected();
					fontNames.setEnabled(!selected);
					fontSize.setEnabled(!selected);
				}
			});
			final String isSysFont = PropertiesManager.getValue(PropertiesManager.C_SYSTEM_DEFAULT_FONT_SIZE, IConstant.TRUE);
			useSysFont.setSelected(IConstant.TRUE.equals(isSysFont));
			new ConfigValue(PropertiesManager.C_SYSTEM_DEFAULT_FONT_SIZE, isSysFont, group) {
				@Override
				public void applyBiz(final int option) {
					if(option == OPTION_CANCEL){
						if(IConstant.TRUE.equals(isSysFont)){
							App.restoreDefaultGlobalFontSetting();
							self.repaint();
						}
					}else{
						if(useSysFont.isSelected()){
							App.restoreDefaultGlobalFontSetting();
							self.repaint();
						}
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
			new ConfigValue(PropertiesManager.C_SKIN, (String)skins.getSelectedItem(), group) {
				
				@Override
				public String getNewValue() {
					return (String)skins.getSelectedItem();
				}
				
				@Override
				public void applyBiz(final int option) {
					final String newSkin = group.getValueForApply(PropertiesManager.C_SKIN);
					
					App.applyLookFeel(newSkin, "");
				};
			};
			
			{
//				appearancePene.add(new JSeparator(JSeparator.HORIZONTAL));
				
				final JPanel skinPanel = new JPanel();
				skinPanel.setBorder(new TitledBorder(""));
				skinPanel.setLayout(new FlowLayout(FlowLayout.LEADING, hgap, vgap));
				skinPanel.add(new JLabel((String)ResourceUtil.get(9042)));
				skinPanel.add(skins);
				
				{
					final JButton addSkin = new JButton((String)ResourceUtil.get(9043));
					skinPanel.add(addSkin);
					final String dependMsg = "<BR>if it depends on a core jar, please import the core jar first in tab of <STRONG>" + third_lib_title + "</STRONG>.";
					addSkin.setToolTipText("<html>add and copy skin lib jar to current system." +
							dependMsg +
							"</html>");
					addSkin.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							final JPanel wPanel = new JPanel();
							wPanel.add(new JLabel(
									"<HTML>Some LookAndFeel skin jar lib may cause error. " +
									"Recommend to use build-in system skin." +
									"<BR>" + dependMsg + "<BR>" +
									"<BR>click OK to continue</HTML>", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEFT));
							App.showCenterPanelMain(wPanel, 0, 0, "Skin Warning", true, null, 
									null, new HCActionListener(new Runnable() {
@Override
public void run() {
							final File file = FileSelector.selectImageFile(addSkin, FileSelector.JAR_FILTER, true);
							if(file == null){
								return;
							}
							
							Vector<String> list = null;
							try {
								PlatformManager.getService().addSystemLib(file, false);
								list = getSubClasses(new JarFile(file, false), BasicLookAndFeel.class);
								
								if(list.size() == 0){
									App.showConfirmDialog(null, 
											"<html>No LookAndFeel resource found in library!" +
											"<BR><BR>please make sure the common or third lib which is depended is added in third-libs first.</html>", 
											"No LookAndFeel", JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE, App.getSysIcon(App.SYS_WARN_ICON));
									return;
								}
								
								final String libName = ThirdlibManager.createLibName(self,
										file);
								if(libName == null){
									return;
								}
								
								newNameLookFeelLib = libName;
								CurrLookFeelJar = file;
								addLookFeel = list;
								
								cancelAddedSkinLibNames.add(newNameLookFeelLib);
							} catch (final Exception e) {
								ExceptionReporter.printStackTrace(e);
								return;
							}
							
							
							skins.setModel(new DefaultComboBoxModel(getInstalledLookAndFeelNames(list)));
							skins.setSelectedItem(list.elementAt(0));
							
							self.pack();
							App.showCenter(self);
}
}, threadPoolToken), null, self, true, false, null, false, false);
						}
					}, threadPoolToken));
					
					{
//						JButton downloadLib = new JButton((String)ResourceUtil.get(9044));
//						downloadLib.addActionListener(new HCActionListener(new Runnable() {
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
		
		return appearancePane;
	}
	
	private final int[] buildComboKeyCodes(){
		if(ResourceUtil.isMacOSX()){
			final int[] out = {KeyEvent.VK_META, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT};
			return out;
		}else{
			final int[] out = {KeyEvent.VK_CONTROL, KeyEvent.VK_ALT};
			return out;
		}
	}
	
	public ConfigPane(){
		this.group = new ConfigValueGroup(this);
		final String OK_BTN_TXT = (String) ResourceUtil.get(IContext.OK);

		setIconImage(App.SYS_LOGO);
		setTitle((String)ResourceUtil.get(9040));

		final JFrame self = this;
		
		{
			final int[] wordShiftKeycodes = buildComboKeyCodes();
			final String[] wordShiftKeys = KeyComperPanel.buildKeyEvent(wordShiftKeycodes);
			final JComboBox wordShift = new JComboBox(wordShiftKeys);//JRE 1.6 not gerneric
			final JTextField wordKey = new JTextField(1);

			wordKey.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(final FocusEvent arg0) {
				}
				
				@Override
				public void focusGained(final FocusEvent arg0) {
					wordKey.selectAll();
				}
			});
			
			final char[] keyCharListener = new char[1];
			final boolean[] isTesting = new boolean[1];
			final KeyListener keyListener = new KeyListener() {
				@Override
				public void keyTyped(final KeyEvent e) {
					keyCharListener[0] = e.getKeyChar();
					if(isTesting[0]){
						ScriptEditPanel.consumeEvent(e);
						isTesting[0] = false;
					}else{
						ContextManager.getThreadPool().run(new Runnable() {
							@Override
							public void run() {
								try{
									Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);
								}catch (final Exception e) {
								}
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										try{
											isTesting[0] = true;
											final Robot robot = new Robot();
											final int mask = wordShiftKeycodes[wordShift.getSelectedIndex()];
											wordKey.selectAll();
											final String text = wordKey.getText();
											if(text.length() > 0){
												final int key = KeyComperPanel.getCharKeyCode(text.toCharArray()[0]);
												robot.keyPress(mask);
												robot.keyPress(key);
												robot.keyRelease(key);
												robot.keyRelease(mask);
											}else{
												isTesting[0] = false;
											}
										}catch (final Exception e) {
										}
									}
								});
							}
						});
					}
				}
				
				@Override
				public void keyReleased(final KeyEvent e) {
				}
				
				@Override
				public void keyPressed(final KeyEvent e) {
				}
			};
			
			final JCheckBox enableLoggerOn = new JCheckBox((String)ResourceUtil.get(9206));
			final JCheckBox enableMSBLog = new JCheckBox((String)ResourceUtil.get(8014));
			final JCheckBox enableMSBDialog = new JCheckBox((String)ResourceUtil.get(8015));
			final JCheckBox enableReportException = App.buildReportExceptionCheckBox(false);

			wordShift.addKeyListener(keyListener);
			final ActionListener actionListener = new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							try{
								Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);
							}catch (final Exception e) {
							}
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									try{
										isTesting[0] = true;
										final Robot robot = new Robot();
										final int mask = wordShiftKeycodes[wordShift.getSelectedIndex()];
										final String text = wordKey.getText();
										if(text.length() > 0){
											final int key = KeyComperPanel.getCharKeyCode(text.toCharArray()[0]);
											robot.keyPress(mask);
											robot.keyPress(key);
											robot.keyRelease(key);
											robot.keyRelease(mask);
										}
									}catch (final Exception e) {
									}
								}
							});
						}
					});
				}
			};
			
			if(ResourceUtil.isEnableDesigner()){
				wordKey.setText(CodeHelper.getWordCompletionKeyText());
				wordShift.setSelectedItem(CodeHelper.getWordCompletionModifierText());
				
				//注意：要后添加事件，先设置状态，否则会触发一次。导致其它控件被输入一个字符
				wordShift.addActionListener(actionListener);
				wordKey.addKeyListener(keyListener);

				new ConfigValue(PropertiesManager.p_wordCompletionModifierCode, String.valueOf(wordShiftKeycodes[wordShift.getSelectedIndex()]), group) {
					@Override
					public void applyBiz(final int option) {
						final Designer designer = Designer.getInstance();
						
						if(option == OPTION_CANCEL){
							if(designer != null){
								designer.codeHelper.initShortCutKeys();
							}
						}if(option == OPTION_APPLY){
							final char inputChar = keyCharListener[0];
							final String keyValue = String.valueOf(inputChar);
							applyKeyChar(designer, inputChar, keyValue, wordKey);
						}else if(option == OPTION_OK){
							final char inputChar = keyCharListener[0];
							final String keyValue = String.valueOf(inputChar);
							
							if(inputChar != 0){//触发修改WordCompletionKeyChar
								PropertiesManager.setValue(PropertiesManager.p_wordCompletionKeyChar, keyValue);
							}
							applyKeyChar(designer, inputChar, keyValue, wordKey);
						}
					}

					private final void applyKeyChar(final Designer designer,
							final char inputChar, String keyValue, final JTextField wordKey) {
						if(inputChar==0){//没有触发修改WordCompletionKeyChar
							keyValue = CodeHelper.getWordCompletionKeyChar();
						}
						if(designer != null){
							designer.codeHelper.refreshShortCutKeys(keyValue, wordKey.getText(), getSelectedModify());
						}
					}

					@Override
					public String getNewValue() {
						return String.valueOf(getSelectedModify());
					}

					private int getSelectedModify() {
						return wordShiftKeycodes[wordShift.getSelectedIndex()];
					}
				};
				
				new ConfigValue(PropertiesManager.p_wordCompletionKeyCode, wordKey.getText(), group) {
					@Override
					public void applyBiz(final int option) {
					}

					@Override
					public String getNewValue() {
						return wordKey.getText();
					}
				};
			}//end word key
			
			enableLoggerOn.setToolTipText("<html>log all information, event, and exception. If unselected, print to console for debug." +
					"<BR>it will take effect after restart this server.</html>");
			enableMSBLog.setToolTipText("<html>log MSB messages between Robot, Converter and Device." +
					"<br>it is very useful to debug modules in HAR project." +
					"</html>");
			enableMSBDialog.setToolTipText("When MSBException/HCSecurityException is thrown, system pop up Exception-Browse window automaticly.");
			
			final String isOldLogger = PropertiesManager.getValue(PropertiesManager.p_IsLoggerOn, IConstant.TRUE);
			final String isOldMSBLog = PropertiesManager.getValue(PropertiesManager.p_isEnableMSBLog, IConstant.FALSE);
			final String isOldMSBDialog = PropertiesManager.getValue(PropertiesManager.p_isEnableMSBExceptionDialog, IConstant.FALSE);
			final String isOldReportException = PropertiesManager.getValue(PropertiesManager.p_isReportException, IConstant.FALSE);
			
			enableLoggerOn.setSelected(ResourceUtil.isLoggerOn());
			enableMSBLog.setSelected(isOldMSBLog.equals(IConstant.TRUE));
			enableMSBDialog.setSelected(isOldMSBDialog.equals(IConstant.TRUE));
			
			final ConfigValue cvIsEnableReportException = new ConfigValue(PropertiesManager.p_isReportException, isOldReportException, group) {
				@Override
				public String getNewValue() {
					return enableReportException.isSelected()?IConstant.TRUE:IConstant.FALSE;
				}
				
				@Override
				public void applyBiz(final int option) {
					final boolean isReportException = ((option == OPTION_CANCEL)?isOldReportException.equals(IConstant.TRUE):enableReportException.isSelected());
					if(isReportException){
						ExceptionReporter.start();
					}else{
						ExceptionReporter.stop();
					}
				}
			};
			final ConfigValue cvIsEnableMSBLog = new ConfigValue(PropertiesManager.p_isEnableMSBLog, isOldMSBLog, group) {
				@Override
				public void applyBiz(final int option) {
					final boolean isToNewLog = ((option == OPTION_CANCEL)?isOldMSBLog.equals(IConstant.TRUE):enableMSBLog.isSelected());
					final BaseResponsor br = ServerUIUtil.getResponsor();
					if(br != null){
						br.enableLog(isToNewLog);
					}
				}

				@Override
				public String getNewValue() {
					return enableMSBLog.isSelected()?IConstant.TRUE:IConstant.FALSE;
				}
			};
			new ConfigValue(PropertiesManager.p_IsLoggerOn, isOldLogger, group) {
				
				@Override
				public String getNewValue() {
					return enableLoggerOn.isSelected()?IConstant.TRUE:IConstant.FALSE;
				}
				
				@Override
				public void applyBiz(final int option) {
				}
			};
			final ConfigValue cvIsEnableMSBDialog = new ConfigValue(PropertiesManager.p_isEnableMSBExceptionDialog, isOldMSBDialog, group) {
				@Override
				public void applyBiz(final int option) {
					final boolean isToNewDialog = ((option == OPTION_CANCEL)?isOldMSBDialog.equals(IConstant.TRUE):enableMSBDialog.isSelected());
					ExceptionViewer.notifyPopup(isToNewDialog);
				}

				@Override
				public String getNewValue() {
					return enableMSBDialog.isSelected()?IConstant.TRUE:IConstant.FALSE;
				}
			};
			
			developerPane.setLayout(new BorderLayout());
			{
				final JPanel panel = new JPanel();
				final BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
				panel.setLayout(boxLayout);
				
				panel.add(enableReportException);
				panel.add(enableLoggerOn);
				panel.add(enableMSBLog);
				panel.add(enableMSBDialog);
				if(ResourceUtil.isEnableDesigner()){
					panel.add(new JSeparator(SwingConstants.HORIZONTAL));

					final String tip = "set shortcut keys for word completion of Designer.";
					
					wordShift.setToolTipText(tip);
					
					final JPanel wordComp = new JPanel(new FlowLayout(FlowLayout.LEADING));
					final JLabel label = new JLabel("word completion :");
					label.setToolTipText(tip);
					
					wordComp.add(label);
					wordComp.add(wordShift);
					wordComp.add(new JLabel("+"));
					wordComp.add(wordKey);
					
					wordComp.setAlignmentX(Component.LEFT_ALIGNMENT);
					panel.add(wordComp);
				}
				if(ResourceUtil.isEnableDesigner()){
					final String oldDocFontSize = DefaultManager.getDesignerDocFontSize();
					
					final JPanel docFontSize = new JPanel(new FlowLayout(FlowLayout.LEADING));
					final JLabel label = new JLabel("API docs font size :");
					label.setToolTipText("the font size of API docs in designer.");
					
					final NumberFormatTextField sizeField = new NumberFormatTextField(DefaultManager.DEFAULT_DOC_FONT_SIZE_INPUT);
					sizeField.setColumns(4);
					sizeField.setText(oldDocFontSize);
					
					final ConfigValue cvDocFontSize = new ConfigValue(PropertiesManager.p_DesignerDocFontSize, oldDocFontSize, group) {
						@Override
						public void applyBiz(final int option) {
						}

						@Override
						public String getNewValue() {
							return sizeField.getText();
						}
					};
					
					docFontSize.add(label);
					docFontSize.add(sizeField);
					
					docFontSize.setAlignmentX(Component.LEFT_ALIGNMENT);
					panel.add(docFontSize);
				}//end docFontSize

				developerPane.add(panel, BorderLayout.NORTH);
			}
			
			otherPane.setLayout(new BorderLayout());
			{
				final JPanel panel = new JPanel();
				final BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
				panel.setLayout(boxLayout);
				
				{
					if(ResourceUtil.isAndroidServerPlatform()){//Android环境下，开机启动
						final JCheckBox cbAutoStart = new JCheckBox((String)ResourceUtil.get(6001));
						cbAutoStart.setToolTipText("<html>" + (String)ResourceUtil.get(9195) + "</html>");
						final String isAutoStart = PropertiesManager.getValue(PropertiesManager.p_autoStart, IConstant.FALSE);
						cbAutoStart.setSelected(IConstant.TRUE.equals(isAutoStart));
						new ConfigValue(PropertiesManager.p_autoStart, isAutoStart, group) {
							@Override
							public void applyBiz(final int option) {
								if(option == OPTION_OK){
									final String newAutoStart = getNewValue();
									if(isAutoStart.equals(newAutoStart) == false){
										PlatformManager.getService().setAutoStart(cbAutoStart.isSelected());
									}
								}
							}
							
							@Override
							public String getNewValue() {
								return cbAutoStart.isSelected()?IConstant.TRUE:IConstant.FALSE;
							}
						};
						final JPanel line = new JPanel();
						line.setLayout(new FlowLayout(FlowLayout.LEADING, ClientDesc.hgap, ClientDesc.vgap));
						line.add(cbAutoStart);
						panel.add(line);
						
						final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
						panel.add(separator);
					}
				}
				
				{
					final JCheckBox cbEnableClientAddHAR = new JCheckBox((String)ResourceUtil.get(9243));
					cbEnableClientAddHAR.setToolTipText("<html>" + (String)ResourceUtil.get(9156) + "</html>");
					final String isAcceptHARLicense = PropertiesManager.getValue(PropertiesManager.p_isEnableClientAddHAR, IConstant.TRUE);
					cbEnableClientAddHAR.setSelected(ResourceUtil.isEnableClientAddHAR());
					new ConfigValue(PropertiesManager.p_isEnableClientAddHAR, isAcceptHARLicense, group) {
						@Override
						public void applyBiz(final int option) {
							//由于menu是依赖于手机参数，所以不需要考虑重启或getPreparedSocketSession
						}
						
						@Override
						public String getNewValue() {
							return cbEnableClientAddHAR.isSelected()?IConstant.TRUE:IConstant.FALSE;
						}
					};
					final JPanel line = new JPanel();
					line.setLayout(new FlowLayout(FlowLayout.LEADING, ClientDesc.hgap, ClientDesc.vgap));
					line.add(cbEnableClientAddHAR);
					panel.add(line);
					
					final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
					panel.add(separator);
				}
				
				{
					final JCheckBox cbAcceptAllHARLicense = new JCheckBox((String)ResourceUtil.get(9241));
//					cbAcceptAllHARLicense.setToolTipText("<html>" + (String)ResourceUtil.get(0) + "</html>");
					final String isAcceptHARLicense = PropertiesManager.getValue(PropertiesManager.p_isAcceptAllHARLicenses, IConstant.FALSE);
					cbAcceptAllHARLicense.setSelected(IConstant.TRUE.equals(isAcceptHARLicense));
					new ConfigValue(PropertiesManager.p_isAcceptAllHARLicenses, isAcceptHARLicense, group) {
						@Override
						public void applyBiz(final int option) {
						}
						
						@Override
						public String getNewValue() {
							return cbAcceptAllHARLicense.isSelected()?IConstant.TRUE:IConstant.FALSE;
						}
					};
					final JPanel line = new JPanel();
					line.setLayout(new FlowLayout(FlowLayout.LEADING, ClientDesc.hgap, ClientDesc.vgap));
					line.add(cbAcceptAllHARLicense);
					panel.add(line);
					
					final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
					panel.add(separator);
				}
				
				{
					final JLabel intervalSecondsNextStartLabel = new JLabel((String)ResourceUtil.get(9175) + " : ");
					final NumberFormatTextField intervalSecondsNextStartField = new NumberFormatTextField(DefaultManager.INTERVAL_SECONDS_FOR_NEXT_STARTUP);
					intervalSecondsNextStartField.setColumns(5);
					final String tooltip = "<html>" + (String)ResourceUtil.get(9176) + "</html>";
					intervalSecondsNextStartLabel.setToolTipText(tooltip);
					
					final String oldIntervalSeconds = String.valueOf(ResourceUtil.getIntervalSecondsForNextStartup());
					intervalSecondsNextStartField.setText(oldIntervalSeconds);
					intervalSecondsNextStartField.setToolTipText(tooltip);

					new ConfigValue(PropertiesManager.p_intervalSecondsNextStartup, oldIntervalSeconds, group) {
						@Override
						public void applyBiz(final int option) {
						}
						@Override
						public String getNewValue() {
							return intervalSecondsNextStartField.getText();
						}
					};
					
					final JPanel line = new JPanel();
					line.setLayout(new FlowLayout(FlowLayout.LEADING, ClientDesc.hgap, ClientDesc.vgap));
					line.add(intervalSecondsNextStartLabel);
					line.add(intervalSecondsNextStartField);
					panel.add(line);
					
					final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
					panel.add(separator);
				}
				
				{
					final String split = "{split}";
					final String preload = (String)ResourceUtil.get(9177);
					final int splitIdx = preload.indexOf(split);
					final String p1 = preload.substring(0, splitIdx);
					final String p2 = preload.substring(splitIdx + split.length());
					
					final JLabel preloadAfterStartup = new JLabel(p1);
					final JLabel back = new JLabel(p2);
					final NumberFormatTextField seconds = new NumberFormatTextField(true, DefaultManager.PRELOAD_AFTER_STARTUP_FOR_INPUT);
					seconds.setColumns(5);
					final String tooltip = "<html>" + (String)ResourceUtil.get(9178) + "</html>";
					preloadAfterStartup.setToolTipText(tooltip);
					
					final String oldIntervalSeconds = String.valueOf(ResourceUtil.getSecondsForPreloadJRuby());
					seconds.setText(oldIntervalSeconds);
					seconds.setToolTipText(tooltip);

					new ConfigValue(PropertiesManager.p_preloadAfterStartup, oldIntervalSeconds, group) {
						@Override
						public void applyBiz(final int option) {
						}
						@Override
						public String getNewValue() {
							return seconds.getText();
						}
					};
					
					final JPanel line = new JPanel();
					line.setLayout(new FlowLayout(FlowLayout.LEADING, ClientDesc.hgap, ClientDesc.vgap));
					line.add(preloadAfterStartup);
					line.add(seconds);
					line.add(back);
					panel.add(line);
					
//					final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
//					panel.add(separator);
				}
				
				otherPane.add(panel, BorderLayout.NORTH);
			}
		}
		
		{
			securityPane.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
			
			final String tooltip = (String)ResourceUtil.get(9174);
			
			final JPanel panel = new JPanel();
			panel.setLayout(new FlowLayout(FlowLayout.LEADING, ClientDesc.hgap, ClientDesc.vgap));
			JLabel label = new JLabel(new ImageIcon(ResourceUtil.getImage(ResourceUtil.getResource("hc/res/errpwd_22.png"))));
			panel.add(label);
			label.setToolTipText(tooltip);
			label = new JLabel(" + ");
			panel.add(label);
			label.setToolTipText(tooltip);
			label = new JLabel(new ImageIcon(ResourceUtil.getImage(ResourceUtil.getResource("hc/res/errck_22.png"))));
			panel.add(label);
			panel.add(new JLabel(" >= "));
			label.setToolTipText(tooltip);
			final NumberFormatTextField jtfMaxTimers = new NumberFormatTextField(DefaultManager.ERR_TRY_TIMES);
			jtfMaxTimers.setToolTipText(tooltip);
			jtfMaxTimers.setText(String.valueOf(SystemLockManager.getConfigErrTry()));
			jtfMaxTimers.setColumns(5);
			panel.add(jtfMaxTimers);
			
			panel.add(new JLabel(", "));
			final JLabel lockLabel = new JLabel(new ImageIcon(ImageSrc.LOCK_ICON));
			lockLabel.setToolTipText(tooltip);
			panel.add(lockLabel);
			final NumberFormatTextField jtfLockMinutes = new NumberFormatTextField(DefaultManager.LOCK_MINUTES);
			jtfLockMinutes.setToolTipText(tooltip);
			jtfLockMinutes.setText(String.valueOf(SystemLockManager.getConfigLockMinutes()));
			jtfLockMinutes.setColumns(5);
			panel.add(jtfLockMinutes);
			final JLabel timeLabel = new JLabel(new ImageIcon(ResourceUtil.getImage(ResourceUtil.getResource("hc/res/timer_22.png"))));
			timeLabel.setToolTipText(tooltip);
			panel.add(timeLabel);
			final JLabel minuteLabel = new JLabel((String)ResourceUtil.get(9047));
			minuteLabel.setToolTipText(tooltip);
			panel.add(minuteLabel);
			
			final ConfigValue cvErrTry = new ConfigValue(PropertiesManager.PWD_ERR_TRY, jtfMaxTimers.getText(), group){
				@Override
				public String getNewValue() {
					return jtfMaxTimers.getText();
				}

				@Override
				public void applyBiz(final int option) {
					PropertiesManager.setValue(PropertiesManager.PWD_ERR_TRY, 
							group.getValueForApply(PropertiesManager.PWD_ERR_TRY));
					SystemLockManager.updateMaxLock();
				}
			};
			
			final ConfigValue cvLockMinutes = new ConfigValue(PropertiesManager.PWD_ERR_LOCK_MINUTES, jtfLockMinutes.getText(), group){
				@Override
				public String getNewValue() {
					return jtfLockMinutes.getText();
				}

				@Override
				public void applyBiz(final int option) {
					PropertiesManager.setValue(PropertiesManager.PWD_ERR_LOCK_MINUTES, 
							group.getValueForApply(PropertiesManager.PWD_ERR_LOCK_MINUTES));
					SystemLockManager.updateMaxLock();
				}
			};
			
			
			final JComboBox network = new JComboBox();
			final Object[] array = HttpUtil.getAllNetworkInterfaces().toArray();
			network.setModel(new DefaultComboBoxModel(array));
			final String selectedName = PropertiesManager.getValue(PropertiesManager.p_selectedNetwork);
			if(selectedName == null){
				network.setSelectedItem(array[0]);
			}else{
				network.setSelectedItem(selectedName);
			}

			final ConfigValueNetwork cvNetwork = new ConfigValueNetwork(network, self, PropertiesManager.p_selectedNetwork, (String)network.getSelectedItem(), group);
			
			final NumberFormatTextField networkPort = new NumberFormatTextField(DefaultManager.DEFAULT_DIRECT_SERVER_PORT_FOR_INPUT);
			networkPort.setText(DefaultManager.getDirectServerPort());
			new ConfigValue(PropertiesManager.p_selectedNetworkPort, networkPort.getText(), group){
				String newOldValue = getOldValue();
				String realWorkingValue = getOldValue();
				@Override
				public String getNewValue() {
					return networkPort.getText();
				}

				@Override
				public void applyBiz(final int option) {
					final boolean isCancel = (option == OPTION_CANCEL);
					if(isCancel){
						newOldValue = getOldValue();
					}
					if(option == OPTION_OK){
						PropertiesManager.setValue(PropertiesManager.p_selectedNetworkPort, 
								group.getValueForApply(PropertiesManager.p_selectedNetworkPort));
						if(isCancel && realWorkingValue.equals(getOldValue())){
						}else if((getNewValue().equals(newOldValue) == false)
								||(getNewValue().equals(realWorkingValue) == false)){
							J2SESessionManager.notifyRestartDirect();
							isNeedShutdownAndRestart = true;
						}
					}
					newOldValue = getNewValue();
				}
			};
			final JPanel networkPane = new JPanel(new BorderLayout(0, vgap));
			panel.setBorder(new TitledBorder((String)ResourceUtil.get(9045) + " : "));
			networkPane.add(panel, BorderLayout.NORTH);
			final JPanel borderPane = new JPanel(new BorderLayout());
			borderPane.setBorder(new TitledBorder((String)ResourceUtil.get(9170) + " : "));
			final JPanel descPane = ServerUIUtil.buildDescPanel((String)ResourceUtil.get(9171));
			borderPane.add(descPane, 
					BorderLayout.CENTER);
			{
				final JPanel networkPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
				networkPanel.add(network);
				final JLabel port = new JLabel((String)ResourceUtil.get(9172) + " : ");
				final String porttooltip = "<html>" +
						(String)ResourceUtil.get(9173) +
						"</html>";
				port.setToolTipText(porttooltip);
				networkPort.setToolTipText(porttooltip);
				networkPanel.add(port);
				networkPort.setColumns(6);
				networkPanel.add(networkPort);
				
				borderPane.add(networkPanel, BorderLayout.NORTH);
			}
			networkPane.add(borderPane, BorderLayout.CENTER);
			securityPane.add(networkPane, BorderLayout.NORTH);
		}

		{
			thirdLibsPane.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
			
			final int MAX_MENUITEM = 30;
			final Vector<Object[]> libs = new Vector<Object[]>();
			
			final JButton upBut = new JButton((String)ResourceUtil.get(9019), new ImageIcon(ImageSrc.UP_SMALL_ICON));
			final JButton downBut = new JButton((String)ResourceUtil.get(9020), new ImageIcon(ImageSrc.DOWN_SMALL_ICON));
			final JButton removeBut = new JButton((String)ResourceUtil.get(9018), new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
			final JButton importBut = new JButton((String)ResourceUtil.get(9016), new ImageIcon(ImageSrc.ADD_SMALL_ICON));

			importBut.setToolTipText("the imported jar will works after click '" + OK_BTN_TXT+ "' button.");
			removeBut.setToolTipText("the removed jar is still working before click '" + OK_BTN_TXT + "' button. Restart HomeCenter Server is NOT required!");
//			upBut.setToolTipText("you need to restart HomeCenter server to make it effect.");
			downBut.setToolTipText(upBut.getToolTipText());
			
			final Object[] colNames = {(String)ResourceUtil.get(9046)};
			final AbstractTableModel tableModel = new AbstractTableModel() {
				@Override
				public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
					((ThirdLibValue)libs.elementAt(rowIndex)[columnIndex]).fileNameWithoutPath = (String)aValue;
				}
				
				@Override
				public void removeTableModelListener(final TableModelListener l) {
				}
				
				@Override
				public boolean isCellEditable(final int rowIndex, final int columnIndex) {
					return false;
				}
				
				@Override
				public Object getValueAt(final int rowIndex, final int columnIndex) {
					return ((ThirdLibValue)libs.elementAt(rowIndex)[columnIndex]).fileNameWithoutPath;
				}
				
				@Override
				public int getRowCount() {
					return libs.size();
				}
				
				@Override
				public String getColumnName(final int columnIndex) {
					return colNames[columnIndex].toString();
				}
				
				@Override
				public int getColumnCount() {
					return colNames.length;
				}
				
				@Override
				public Class<?> getColumnClass(final int columnIndex) {
					return String.class;
				}
				
				@Override
				public void addTableModelListener(final TableModelListener l) {
				}
			};

			new ConfigValue(null, null, group) {
				@Override
				public String getNewValue() {
					return null;
				}
				@Override
				public void applyBiz(final int option) {
					if(option == OPTION_OK){
						if(deledLibs.size() > 0 || addLib || isMovedLibIdx){
							afterModi3rdLib();
						}
					}else if(option == OPTION_CANCEL){
						final int size = cancelAddedSkinLibNames.size();
						for (int i = 0; i < size; i++) {
							ThirdlibManager.removeLib(cancelAddedSkinLibNames.elementAt(i), false);
						}
					}
				}
			};
			tablePanel = new HCTablePanel(tableModel, libs, colNames, 0, 
					upBut, downBut, removeBut, importBut, null,
					//upOrDown
					new AbstractDelayBiz(null) {
						@Override
						public final void doBiz() {
							isMovedLibIdx = true;
//							HCTablePanel currTablePane = (HCTablePanel)getPara();
//							final Object[] objs = currTablePane.getColumnAt(0);
//							for (int i = 0; i < objs.length; i++) {
//								objs[i] = ThirdlibManager.buildPath(ThirdlibManager.removePath(objs[i].toString(), false));
//							}
//							ThirdlibManager.refill(objs);
						}
					},
					//remove
					new AbstractDelayBiz(null) {
						@Override
						public final void doBiz() {
							final AbstractDelayBiz selfBiz = this;
							final Object[] rows = (Object[])getPara();
							final JPanel askPanel = new JPanel();
							askPanel.add(new JLabel("Are you sure to remove jar lib?", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEFT));
							App.showCenterPanelMain(askPanel, 0, 0, "Confirm Delete?", true, null, null, new HCActionListener(new Runnable() {
								@Override
								public void run() {
									deledLibs.add((ThirdLibValue)rows[0]);
//									ThirdlibManager.removeLib((String)rows[0], true);
									final boolean[] back = {true};
									selfBiz.setPara(back);
								}
							}, threadPoolToken), new ActionListener() {
								@Override
								public void actionPerformed(final ActionEvent e) {
									final boolean[] back = {false};
									selfBiz.setPara(back);
								}
							}, self, true, false, null, false, false);
						}
					}, 
					//import
					new AbstractDelayBiz(null) {
						@Override
						public final void doBiz() {
							final File newJarFile = FileSelector.selectImageFile(importBut, FileSelector.JAR_FILTER, true);
							if(newJarFile != null){
								final String newLibName = ThirdlibManager.createLibName(self, newJarFile);
								if(newLibName != null){
//									ThirdlibManager.addLib(newJarFile, newLibName);
									addLib = true;
									
									final ThirdLibValue v = new ThirdLibValue();
									v.fileNameWithoutPath = newLibName + ThirdlibManager.EXT_JAR;
									v.status = ThirdLibValue.NEW;
									v.newSrc = newJarFile;
									
									final Object[] libName = {v};
									
									setPara(libName);
									return;
								}
							}
							setPara(null);
						}
					}, false, colNames.length);
			
			final JPanel buttonsList = new JPanel();
			buttonsList.setLayout(new GridLayout(1, 4, ClientDesc.hgap, ClientDesc.vgap));
			buttonsList.add(upBut);
			buttonsList.add(downBut);
			buttonsList.add(removeBut);
			buttonsList.add(importBut);

			final JScrollPane scrollpane = new JScrollPane(tablePanel.table);
			scrollpane.setPreferredSize(new Dimension(600, 100));
			
			final JPanel descPane = ServerUIUtil.buildDescPanel(
					"1. these libraries are available to all HAR projects.<BR>" +
					"2. it may conflict with jar libraries in HAR projects. it is better to put it in HAR project.<BR>" +
					"3. the added jar libraries are J2SE standard, no matter the server is Android, Window or other.");
			
			thirdLibsPane.add(scrollpane, BorderLayout.CENTER);
			thirdLibsPane.add(buttonsList, BorderLayout.NORTH);
			thirdLibsPane.add(descPane, BorderLayout.SOUTH);
		}
		
		final JTabbedPane centerPanel = new JTabbedPane();

		if(ResourceUtil.isJ2SELimitFunction()){
			centerPanel.add((String)ResourceUtil.get(9039), buildAppearancePane(self));
		}
		centerPanel.add((String)ResourceUtil.get(9045), securityPane);
		centerPanel.add(third_lib_title, thirdLibsPane);
		centerPanel.add((String)ResourceUtil.get(9112), otherPane);
		centerPanel.add("Developer", developerPane);
		
		final JButton ok = new JButton(OK_BTN_TXT, new ImageIcon(ImageSrc.OK_ICON));

		setLayout(new BorderLayout(hgap, vgap));
		add(centerPanel, BorderLayout.CENTER);
		
		final JPanel bottonButtons = new JPanel();
		final JButton cancel = new JButton((String)ResourceUtil.get(1018), new ImageIcon(ImageSrc.CANCEL_ICON));
		cancel.addActionListener(new HCButtonEnabledActionListener(cancel, new Runnable() {
			@Override
			public void run() {
				cancelConfig();
			}
		}, threadPoolToken));
		ok.addActionListener(new HCButtonEnabledActionListener(ok, new Runnable() {
			@Override
			public void run() {
				group.doSaveUI();//可能弹出对话框，所以不能置于SwingUtilities

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(CurrLookFeelJar != null && newNameLookFeelLib != null 
								&& addLookFeel.contains(UIManager.getLookAndFeel().getClass().getName())){
							final int size = addLookFeel.size();
							for (int i = 0; i < size; i++) {
								extLookFeel.appendItemIfNotContains(addLookFeel.elementAt(i));
							}
							extLookFeel.save();
						}

						try{
							ResourceUtil.buildMenu();
							SingleJFrame.updateAllJFrame();
						}catch (final Throwable e) {
							ExceptionReporter.printStackTrace(e);
						}
						exit();
					}
				});
			}
		}, threadPoolToken));
		final JButton apply = new JButton((String) ResourceUtil.get(9041), new ImageIcon(ResourceUtil.getImage(ResourceUtil.getResource("hc/res/test_22.png"))));
		apply.addActionListener(new HCButtonEnabledActionListener(apply, new Runnable() {
			@Override
			public void run() {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						group.applyAll(OPTION_APPLY);
						updateFrameUI();
					}
				});
			}
		}, threadPoolToken));
		bottonButtons.setLayout(new FlowLayout(FlowLayout.RIGHT, hgap, vgap));
		bottonButtons.add(ok);
		bottonButtons.add(cancel);
		bottonButtons.add(apply);

		centerPanel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				if(e.getSource() instanceof JTabbedPane){
					final JTabbedPane sourceTabbedPane = (JTabbedPane)e.getSource();
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							final String titleAt = sourceTabbedPane.getTitleAt(sourceTabbedPane.getSelectedIndex());
							final boolean isSeleThirdLib = titleAt.equals(third_lib_title);
							
							apply.setEnabled(!isSeleThirdLib);
							if(isSeleThirdLib){
								final int size = refreshLibs();
								if(size > 0){
									tablePanel.refresh(size);
								}
							}
						}
					}, threadPoolToken);
				}
			}
		});

		add(bottonButtons, BorderLayout.SOUTH);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new HCWindowAdapter(new Runnable() {
			@Override
			public void run() {
				cancelConfig();						
			}
		}, threadPoolToken));

		getRootPane().registerKeyboardAction(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				cancelConfig();
			}}, threadPoolToken),
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		pack();
		App.showCenter(this);
	}

	private boolean isNewSkinLib(final String fileName){
		final Vector<Object[]> libs = tablePanel.body;
		for (int i = 0; i < libs.size(); i++) {
			if(((ThirdLibValue)libs.elementAt(i)[0]).fileNameWithoutPath.equals(fileName)){
				return false;
			}
		}
		
		for (int j = 0; j < deledLibs.size(); j++) {
			if(deledLibs.elementAt(j).fileNameWithoutPath.equals(fileName)){
				return false;
			}
		}
		return true;
	}
	
	private int refreshLibs(){
		//刷新，因为添加皮肤，可能增加了新的lib
		final Vector<Object[]> libs = tablePanel.body;
//		libs.clear();
		final int size = ThirdlibManager.libsSet.size();
		boolean isNeedRefresh = false;
		for (int i = 0; i < size; i++) {
			final String newFile = ThirdlibManager.removePath(ThirdlibManager.libsSet.getItem(i), true);
			if(isNewSkinLib(newFile)){
				final Object[] values = new Object[1];
				final ThirdLibValue v = new ThirdLibValue();
				v.fileNameWithoutPath = newFile;
				v.status = ThirdLibValue.ADDED;
				
				values[0] = v;
				libs.add(values);
				
				isNeedRefresh = true;
			}
		}
		return isNeedRefresh?size:0;
	}

	private void afterModi3rdLib(){
		isNeedShutdownAndRestart = true;
		
		ServerUIUtil.notifyModiThridLibs();
		
		for (int i = 0; i < deledLibs.size(); i++) {
			final ThirdLibValue v = deledLibs.elementAt(i);
//			deledLibs.add((ThirdLibValue)rows[0]);
//			ThirdlibManager.removeLib((String)rows[0], true);
			if(v.status == ThirdLibValue.ADDED){
				ThirdlibManager.removeLib(v.fileNameWithoutPath, true);
			}
		}
		
//		refreshLibs();
		{
			final int size = refreshLibs();
			if(size > 0){
				tablePanel.refresh(size);//否则，下行getColumnAt(0);将返回旧值
			}
		}
		
		final Object[] objs = tablePanel.getColumnAt(0);
		final String[] storePaths = new String[objs.length];
		for (int i = 0; i < objs.length; i++) {
			final ThirdLibValue v = (ThirdLibValue)objs[i];
			if(v.status == ThirdLibValue.NEW){
				ThirdlibManager.addLib(v.newSrc, v.fileNameWithoutPath.substring(0, v.fileNameWithoutPath.length() - 4));//-4 去掉ThirdlibManager.EXT_JAR
			}
			storePaths[i] = ThirdlibManager.buildPath(ThirdlibManager.removePath(v.fileNameWithoutPath, false));
		}
		ThirdlibManager.refill(storePaths);

//File newJarFile = FileSelector.selectImageFile(importBut, FileSelector.JAR_FILTER, true);
//if(newJarFile != null){
//	String newLibName = ThirdlibManager.createLibName(self, newJarFile);
//	if(newLibName != null){
////ThirdlibManager.addLib(newJarFile, newLibName);
//
//ThirdLibValue v = new ThirdLibValue();
//v.fileNameWithoutPath = newLibName + ThirdlibManager.EXT_JAR;

		//restart server
	}

	public static final String SYS_LOOKFEEL = "System - ";

	private Object[] getInstalledLookAndFeelNames(final Vector<String> tryList) {
		final Vector<String> list = new Vector<String>();
		
		final LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels();
		for (int i = 0; i < looks.length; i++) {
			list.add(SYS_LOOKFEEL + looks[i].getName());
		}
		
		final int size = extLookFeel.size();
		boolean isModi = false;
		for (int i = size - 1; i >= 0; i--) {
			final String item = extLookFeel.getItem(i);
			final Class c = ResourceUtil.loadClass(item, true);
			if(c != null){
				if(list.contains(item)){
					isModi = true;
					continue;
				}
				list.add(item);
			}else{
				extLookFeel.delItem(item);
				isModi = true;
			}
		}
		if(isModi){
			extLookFeel.save();
		}
		
		if(tryList != null){
			final int total = tryList.size();
			for (int i = 0; i < total; i++) {
				final String item = tryList.elementAt(i);
				if(list.contains(item) == false){
					list.add(item);
				}
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				group.doCancel();
				SingleJFrame.updateAllJFrame();
				exit();
			}
		});
	}

	public static void rebuildConnection(final JFrame self) {
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("<html>setting is changed or canceled, need rebuild connection," +
				"<BR><BR>click '" + (String) ResourceUtil.get(IContext.OK) + "' to rebuild now!</html>", 
				App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING));
		App.showCenterPanelMain(panel, 0, 0, "rebuild connection now?", true, null, null, new HCActionListener(new Runnable() {
			@Override
			public void run() {
				HttpUtil.notifyStopServer(true, self);		

				J2SESessionManager.stopAllSession(true, true, false);
			}
		}, App.getThreadPoolToken()), null, self, true, false, null, false, false);
	}

	private static Vector<String> getSubClasses(final JarFile jarFile, final Class parentClass){
		final Vector<String> list = new Vector<String>();
        final String classPre = ".class";
        final int cutLen = classPre.length();
        
		final Enumeration<JarEntry> entrys = jarFile.entries();  
	    while (entrys.hasMoreElements()) {  
	        final JarEntry jarEntry = entrys.nextElement();  
	        final String entryName = jarEntry.getName();
			if(entryName.endsWith(classPre)){
	        	final String className = entryName.substring(0, entryName.length() - cutLen).replace('/', '.');
				final Class testClass = ResourceUtil.loadClass(className, true);
				if(testClass != null && testClass.asSubclass(parentClass) != null){
					if(testClass.isInterface() || testClass.isLocalClass() || testClass.isMemberClass()){
						
					}else{
						list.add(className);
					}
				}
	        }
	    }
	    
	    return list;
	}

	public static void updateComponentUI(final Container container) {
		try{
			final int size = container.getComponentCount();
			for (int i = 0; i < size; i++) {
				final Object component = container.getComponent(i);
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
		}catch (final Exception e) {
//			ExceptionReporter.printStackTrace(e);
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
		return "";
	}

	class ConfigValueNetwork extends ConfigValue {
		final JComboBox network;
		final JFrame frame;
		
		public ConfigValueNetwork(final JComboBox network, final JFrame frame, final String configKey, final String old, final ConfigValueGroup group){
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
		public void applyBiz(final int option) {
			final boolean isCancel = (option == ConfigPane.OPTION_CANCEL);
			if(isCancel){
				newOldValue = getOldValue();
			}
			if(option == ConfigPane.OPTION_OK){
				PropertiesManager.setValue(PropertiesManager.p_selectedNetwork, 
						group.getValueForApply(PropertiesManager.p_selectedNetwork));
				if(isCancel && realWorkingValue.equals(getOldValue())){
				}else if((getNewValue().equals(newOldValue) == false)
						||(getNewValue().equals(realWorkingValue) == false)){
					J2SESessionManager.notifyRestartDirect();
					ConfigPane.this.isNeedShutdownAndRestart = true;
					update();
				}
			}
			newOldValue = getNewValue();
		}

		public void update() {
			realWorkingValue = getNewValue();
		}
	}
}

class ThirdLibValue {
	public static final int ADDED = 1;
	public static final int NEW = 2;
	
	String fileNameWithoutPath;
	int status;
	File newSrc;
}

