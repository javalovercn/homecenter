package hc;

import hc.core.ConditionWatcher;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.IEncrypter;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.core.util.Stack;
import hc.core.util.StringUtil;
import hc.res.ImageSrc;
import hc.server.ConfigPane;
import hc.server.J2SEConstant;
import hc.server.J2SEContext;
import hc.server.ServerInitor;
import hc.server.StarterManager;
import hc.server.ThirdlibManager;
import hc.server.ui.ClosableWindow;
import hc.server.ui.ServerUIUtil;
import hc.server.util.IDArrayGroup;
import hc.util.ConnectionManager;
import hc.util.HttpUtil;
import hc.util.IBiz;
import hc.util.LogServerSide;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.TokenManager;
import hc.util.UILang;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.FontUIResource;

public class App {
	public static final String TAG_INI_DEBUG_ON = "debugOn";
	public static final String TAG_SERVER_MODE = "serverOn";
	public static final String TAG_SERVER_OFF_MODE = "serverOff";
	public static final String TAG_STARTER_VER = "starterVer";

	public static boolean SERVER_ON = false;

	static FileLock lock;

	public static final String SYS_ERROR_ICON = "OptionPane.errorIcon";
	public static final String SYS_INFO_ICON = "OptionPane.informationIcon";
	public static final String SYS_QUES_ICON = "OptionPane.questionIcon";
	public static final String SYS_WARN_ICON = "OptionPane.warningIcon";

	public static Icon getSysIcon(final String key) {
		return UIManager.getIcon(key);
	}

	public static float getJREVer() {
		String ver = System.getProperty("java.version");
		Pattern pattern = Pattern.compile("^(\\d\\.\\d)");
		Matcher matcher = pattern.matcher(ver);
		if (matcher.find()) {
			return Float.parseFloat(matcher.group(1));
		} else {
			return 1.0F;
		}
	}

	public static void main(final String args[]) {
		new Thread(){
			public void run(){
				execMain(args);
			}
		}.start();
	}
	
	private static void execMain(String args[]) {
		if (getJREVer() < 1.5) {
			JOptionPane.showMessageDialog(null, "JRE 1.6 or above!", "Error",
					JOptionPane.ERROR_MESSAGE);
			CCoreUtil.globalExit();
		}

		// 创建锁文件
		try {
			// Get a file channel for the file
			File file = new File("lockme.hc");
			FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

			try {
				// lock = channel.lock();
				lock = channel.tryLock();
			} catch (OverlappingFileLockException e) {
				// File is already locked in this thread or virtual machine
			}
			if (lock == null) {
				JOptionPane.showMessageDialog(null,
						"HomeCenter is already runing!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			// Release the lock
			// lock.release();
			// Close the file
			// channel.close();
		} catch (Exception e) {
		}

		SERVER_ON = true;
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg == null) {
					break;
				}
				if (arg.equals(TAG_INI_DEBUG_ON)) {
					LogManager.INI_DEBUG_ON = true;
				} else if (arg.equals(TAG_SERVER_MODE)) {
				} else if (arg.equals(TAG_SERVER_OFF_MODE)) {
					SERVER_ON = false;
				}else if(arg.startsWith(TAG_STARTER_VER)){
					try{
						String[] values = arg.split("=");
						StarterManager.setCurrStarterVer(values[1]);
					}catch (Exception e) {
					}
				}
			}
		}
		IConstant.setServerSide(SERVER_ON);
		if (SERVER_ON == false) {
			JOptionPane.showMessageDialog(null,
					"Please use mobile client (Install hcME.jar to your mobile).\r\n"
							+ "PC client is in developing.\r\nThanks",
					"Coming soon", JOptionPane.INFORMATION_MESSAGE);
			CCoreUtil.globalExit();
		}
		IConstant.propertiesFileName = ((App.SERVER_ON) ? "hc_config.properties"
				: "hc_client_config.properties");

		PropertiesManager.emptyDelDir();

		StarterManager.startUpgradeStarter();
		
		ThirdlibManager.loadThirdLibs();

		// 选择Skin
		String selectedSkin = ConfigPane.getSystemSkin();

		if(selectedSkin != null){
			applyLookFeel(selectedSkin, ConfigPane.getDefaultSkin());
		}
		
		if (PropertiesManager.isTrue(PropertiesManager.p_IAgree) == false) {
			IBiz biz = new IBiz() {
				@Override
				public void start() {
					PropertiesManager.setValue(PropertiesManager.p_IAgree,
							IConstant.TRUE);
					PropertiesManager.saveFile();
					
					initServer();
				}

				@Override
				public void setMap(HashMap map) {
				}
			};
			// 同意使用许可
			showAgreeLicense("HomeCenter : License Agreement",
					"http://homecenter.mobi/bcl.txt", biz, true);
			return;
		}

		initServer();
	}

	public static void setNoTransCert() {
		PropertiesManager.setValue(PropertiesManager.p_NewCertIsNotTransed, IConstant.TRUE);
		PropertiesManager.saveFile();
	}

	public static void generateCert() {
		if(CUtil.CertKey == null){
			CUtil.CertKey = new byte[CCoreUtil.CERT_KEY_LEN];
		}
		CCoreUtil.generateRandomKey(CUtil.CertKey, 0, CCoreUtil.CERT_KEY_LEN);
		IConstant.getInstance().setObject(IConstant.CertKey, CUtil.CertKey);
	}
	
	private static void initServer() {
		LogManager.setLog(new LogServerSide());
		L.enable(true);

		IConstant.setInstance(new J2SEConstant());
		
		if(PropertiesManager.getValue(PropertiesManager.p_CertKey) == null){
			L.V = L.O ? false : LogManager.log("create new certification for new install.");
			generateCert();
			PropertiesManager.setValue(PropertiesManager.p_EnableTransNewCertKeyNow, IConstant.TRUE);
			
			setNoTransCert();
		}

		ServerInitor.doNothing();

		if (PropertiesManager.isTrue(PropertiesManager.p_IsSimu)) {
			L.V = L.O ? false : LogManager.log("isSimu : true");
		}
		// JRE 6 install
		// http://www.oracle.com/technetwork/java/javase/downloads/jre-6u27-download-440425.html

//		if (PropertiesManager.isTrue(PropertiesManager.p_IsSimu)) {
//			RootConfig.getInstance().setProperty(RootConfig.p_RootRelayServer,
//					RootServerConnector.SIMU_ROOT_IP);
//			RootConfig.getInstance().setProperty(
//					RootConfig.p_RootRelayServerPort, 
//					String.valueOf(RootServerConnector.SIMU_ROOT_PORT));
//		}
		// ContextManager.setSimulate(errorStunEnv != null &&
		// errorStunEnv.equals("true"));

		// 初始化字体
		GraphicsEnvironment environment = GraphicsEnvironment
				.getLocalGraphicsEnvironment();// GraphicsEnvironment是一个抽象类，不能实例化，只能用其中的静态方法获取一个实例
		String[] fontNames = environment.getAvailableFontFamilyNames();// 获取系统字体

		final JMenu l = new JMenu();
		final String valueDefaultFontSize = PropertiesManager.getValue(PropertiesManager.C_SYSTEM_DEFAULT_FONT_SIZE);
		final String defaultFontName = l.getFont().getName();//"Dialog";
		String fontName = PropertiesManager.getValue(PropertiesManager.C_FONT_NAME,
				defaultFontName);
		final String defaultFontSize = String.valueOf(l.getFont().getSize());//16;
		String fontSize = PropertiesManager
				.getValue(PropertiesManager.C_FONT_SIZE, defaultFontSize);
		if((valueDefaultFontSize == null) || (PropertiesManager.isTrue(valueDefaultFontSize) == false)){
		}else{
			for (int i = 0; i < fontNames.length; i++) {
				if (fontNames[i].equals(fontName)) {
					try{
						initGlobalFontSetting(new Font(fontName, Font.PLAIN,
							Integer.parseInt(fontSize)));
					}catch (Throwable e) {
						//由于某些皮肤包导致本错误
						LogManager.err("initGlobalFontSetting error : " + e.toString());
						e.printStackTrace();
						
						L.V = L.O ? false : LogManager.log("(set default font and LookAndFeel)");
						applyLookFeel(ConfigPane.getDefaultSkin(), null);
					}
					break;
				}
			}
		}
		// SwingUtilities.invokeLater(new Runnable() {
		// public void run() {
		// try{
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel());
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceTwilightLookAndFeel());
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel());//V
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel());
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel());//V
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel());//VVV
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel());VV
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceCremeLookAndFeel());//VVV
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel());//V
		// }catch (Exception e) {
		//
		// }
		// new App();
		// }
		// });

		// boolean succ = UDPChannel.buildChannel();
		// if (succ == false) {
		// JOptionPane.showMessageDialog(null,
		// (String) ResourceUtil.get(1000), (String) ResourceUtil
		// .get(UILang.ERROR), JOptionPane.ERROR_MESSAGE);
		// ExitManager.exit();
		// }

		boolean needNewApp = true;
		if (SERVER_ON) {
			String v = PropertiesManager
					.getValue(PropertiesManager.p_AutoStart);
			String password = PropertiesManager
					.getValue(PropertiesManager.p_password);
			String uuid = PropertiesManager.getValue(PropertiesManager.p_uuid);
			if (v != null && password != null && uuid != null
					&& v.equals(IConstant.TRUE)) {
				needNewApp = false;

				if (IConstant.checkUUID(uuid) == false) {
					JOptionPane.showMessageDialog(null, "Invalid UUID",
							"Error", JOptionPane.ERROR_MESSAGE);
					CCoreUtil.globalExit();
				}
				startAfterInfo();
			}
		}
		if (needNewApp) {
			startRegDialog();
			
//			ActionListener getUUIDAL = new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					startRegDialog();
//				}
//			};
//
//			JButton jbOK = null;
//			jbOK = new JButton((String) ResourceUtil.get(IContext.OK));
//
//			JPanel panel = new JPanel();
//			panel.add(new JLabel("Welcome to HomeCenter World!"));
//			showCenterPanel(panel, 300, 120, "HomeCenter", false, jbOK,
//					getUUIDAL, null, null, false, false);
		}
	}

	public static void startAfterInfo() {
		String password = PropertiesManager
				.getValue(PropertiesManager.p_password);
		String uuid = PropertiesManager.getValue(PropertiesManager.p_uuid);

		if (IConstant.checkUUID(uuid) == false) {
			JOptionPane.showMessageDialog(null, "Invalid ID", "Error",
					JOptionPane.ERROR_MESSAGE);
			CCoreUtil.globalExit();
		}

		IConstant.setUUID(uuid);
		String pwd = App.getFromBASE64(password);
		IConstant.setPassword(pwd);
		IConstant.setServerSide(true);

		ILog ilog = LogManager.getLogger();
		if (ilog != null && ilog instanceof LogServerSide) {
			((LogServerSide) ilog).buildOutStream();
		}

		try {
			ContextManager.setContextInstance(new J2SEContext());
			ContextManager.start();
		} catch (Throwable e) {
			e.printStackTrace();
			JOptionPane
					.showMessageDialog(
							null,
							"Error connect to server! please try again after few minutes.",
							(String) ResourceUtil.get(IContext.ERROR),
							JOptionPane.ERROR_MESSAGE);

		}
	}

	public static void showHARProjectAgreeLicense(String title, String license_url,
			final IBiz biz, boolean logoHC, Frame owner) {
		final JDialog dialog = new JDialog(owner);
		dialog.setModal(true);
		dialog.setTitle(title);
		dialog.setIconImage(App.SYS_LOGO);

		Container main = dialog.getContentPane();

		JPanel c = new JPanel();
		c.setLayout(new BorderLayout(5, 5));

		addBorderGap(main, c);

		try {
			// "Please review the license agreement before using HomeCenter. " +
			JLabel label = new JLabel(
					"If you accept all terms of the agreement, Click 'I Agree'.",
					(logoHC ? new ImageIcon(ImageIO.read(ResourceUtil
							.getResource("hc/res/hc_32.png"))) : null),
					SwingConstants.LEADING);
			c.add(label, "North");
		} catch (IOException e) {
			e.printStackTrace();
		}

		final JTextArea area = new JTextArea(30, 30);
		try {
			URL oracle = new URL(license_url);
			BufferedReader in;
			in = new BufferedReader(new InputStreamReader(oracle.openStream()));
			area.read(in, null);

			area.addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent mouseEvent) {
					area.setCursor(new Cursor(Cursor.TEXT_CURSOR)); // 鼠标进入Text区后变为文本输入指针
				}

				public void mouseExited(MouseEvent mouseEvent) {
					area.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); // 鼠标离开Text区后恢复默认形态
				}
			});
			area.getCaret().addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					area.getCaret().setVisible(true); // 使Text区的文本光标显示
				}
			});
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
		} catch (IOException e) {
			area.setText(license_url);
		}finally{
			area.setEditable(false);
		}

		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		java.awt.event.ActionListener exitActionListener = new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		};
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
			}
		});
		dialog.getRootPane().registerKeyboardAction(exitActionListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		BufferedImage ok_icon = null;
		try {
			ok_icon = ImageIO.read(ImageSrc.OK_ICON);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		final JButton ok = new JButton("I Agree", new ImageIcon(ok_icon));

		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				if (biz != null) {
					biz.start();
				}
			}
		});
		BufferedImage cancel_icon = null;
		try {
			cancel_icon = ImageIO.read(ImageSrc.CANCEL_ICON);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		JButton cancel = new JButton("Cancel", new ImageIcon(cancel_icon));
		cancel.addActionListener(exitActionListener);

		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new GridLayout(1, 2, 5, 5));
		btnPanel.add(ok);
		btnPanel.add(cancel);

		JPanel botton = new JPanel();
		botton.setLayout(new BorderLayout(5, 5));
		botton.add(btnPanel, "East");

		JScrollPane jsp = new JScrollPane(area);
		c.add(jsp, "Center");
		c.add(botton, "South");

		dialog.setSize(700, 600);
		dialog.setResizable(false);
		showCenter(dialog);
	}
	
	public static void showAgreeLicense(String title, String license_url,
			final IBiz biz, boolean logoHC) {
		final JDialog dialog = new JDialog();
		dialog.setModal(true);
		dialog.setTitle(title);
		dialog.setIconImage(App.SYS_LOGO);

		Container main = dialog.getContentPane();

		JPanel c = new JPanel();
		c.setLayout(new BorderLayout(5, 5));

		addBorderGap(main, c);

		try {
			// "Please review the license agreement before using HomeCenter. " +
			JLabel label = new JLabel(
					"If you accept all terms of the agreement, Click 'I Agree'.",
					(logoHC ? new ImageIcon(ImageIO.read(ResourceUtil
							.getResource("hc/res/hc_32.png"))) : null),
					SwingConstants.LEADING);
			c.add(label, "North");
		} catch (IOException e) {
			e.printStackTrace();
		}

		final JTextArea area = new JTextArea(30, 30);
		try {
			URL oracle = new URL(license_url);
			BufferedReader in;
			in = new BufferedReader(new InputStreamReader(oracle.openStream()));
			area.read(in, null);

			area.setEditable(false);
			area.addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent mouseEvent) {
					area.setCursor(new Cursor(Cursor.TEXT_CURSOR)); // 鼠标进入Text区后变为文本输入指针
				}

				public void mouseExited(MouseEvent mouseEvent) {
					area.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); // 鼠标离开Text区后恢复默认形态
				}
			});
			area.getCaret().addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					area.getCaret().setVisible(true); // 使Text区的文本光标显示
				}
			});
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
		} catch (IOException e) {
			String[] options = { "O K" };
			JOptionPane.showOptionDialog(null,
					"Cant connect server, please try late!", "HomeCenter",
					JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
					null, options, options[0]);
			dialog.dispose();
		}

		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		java.awt.event.ActionListener exitActionListener = new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		};
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
			}
		});
		dialog.getRootPane().registerKeyboardAction(exitActionListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		final JCheckBox check = new JCheckBox("I Agree");
		BufferedImage ok_icon = null;
		try {
			ok_icon = ImageIO.read(ImageSrc.OK_ICON);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		final JButton ok = new JButton("O K", new ImageIcon(ok_icon));

		check.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ok.setEnabled(check.isSelected());
			}
		});

		ok.setEnabled(false);
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				if (biz != null) {
					biz.start();
				}
			}
		});
		BufferedImage cancel_icon = null;
		try {
			cancel_icon = ImageIO.read(ImageSrc.CANCEL_ICON);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		JButton cancel = new JButton("Cancel", new ImageIcon(cancel_icon));
		cancel.addActionListener(exitActionListener);

		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new GridLayout(1, 2, 5, 5));
		btnPanel.add(ok);
		btnPanel.add(cancel);

		JPanel botton = new JPanel();
		botton.setLayout(new BorderLayout(5, 5));
		botton.add(check, "Center");
		botton.add(btnPanel, "East");

		JScrollPane jsp = new JScrollPane(area);
		c.add(jsp, "Center");
		c.add(botton, "South");

		dialog.setSize(700, 600);
		dialog.setResizable(false);
		showCenter(dialog);
	}

	public static void addBorderGap(Container main, JPanel c) {
		main.setLayout(new GridBagLayout());
		main.add(c, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
						10, 10, 10, 10), 0, 0));
	}

	public static void showInstallTip(boolean isCenter) {
		try {
			JPanel jp = new JPanel() {
				ImageIcon image = new ImageIcon(new URL(
						"http://homecenter.mobi/images/tuto.gif"));

				@Override
				public void paint(Graphics g) {
					g.drawImage(image.getImage(), 0, 0, this);
				}
			};
			final JFrame frame = new JFrame();
			frame.setTitle((String) ResourceUtil.get(9029));
			frame.setIconImage(App.SYS_LOGO);// new File("hc/res/hc_16.png")
			frame.getContentPane().setBackground(Color.WHITE);

			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(jp, BorderLayout.CENTER);
			JPanel bottonPanel = new JPanel();
			bottonPanel.setLayout(new BorderLayout());

			int width = 400;
			JEditorPane mEditorPane = new JEditorPane();
			mEditorPane.setEditable(false);
			try {
				mEditorPane
						.setPage("http://homecenter.mobi/msg/installtip.php?lang="
								+ System.getProperties().getProperty(
										"user.language"));
				// mEditorPane.setPage("http://localhost:8080/msg/installtip.php");
			} catch (IOException e1) {
			}
			mEditorPane.addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					HyperlinkEvent.EventType type = e.getEventType();
					if (type == HyperlinkEvent.EventType.ACTIVATED) {
						HttpUtil.browse(e.getURL().toExternalForm());
					}
				}
			});
			bottonPanel.add(mEditorPane, BorderLayout.CENTER);

			JButton close = new JButton((String) ResourceUtil.get(1010));
			close.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					frame.dispose();
				}
			});
			bottonPanel.add(close, BorderLayout.SOUTH);

			frame.getContentPane().add(bottonPanel, BorderLayout.SOUTH);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			// frame.setResizable(false);
			frame.resize(width, 550);

			if (isCenter) {
				showCenter(frame);
			}
			frame.setVisible(true);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public static BufferedImage SYS_LOGO;
	static {
		try {
			App.SYS_LOGO = ImageIO.read(App.class.getClassLoader().getResource(
					"hc/res/hc_16.png"));
		} catch (Exception e) {

		}
	}

	public static void initGlobalFontSetting(Font fnt) {
		if(defaultFontSetting == null || defaultFontSetting.keySet().size() == 0){
			defaultFontSetting = new HashMap<Object, Object>();
			saveDefaultGlobalFontSetting();
		}
		
		FontUIResource fontRes = new FontUIResource(fnt);
		for (Enumeration keys = UIManager.getDefaults().keys(); keys
				.hasMoreElements();) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof FontUIResource)
				UIManager.put(key, fontRes);
		}
	}
	
	private static HashMap<Object, Object> defaultFontSetting = null;
	
	private static void saveDefaultGlobalFontSetting() {
		for (Enumeration keys = UIManager.getDefaults().keys(); keys
				.hasMoreElements();) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof FontUIResource)
				defaultFontSetting.put(key, value);
		}
	}
	

	public static void restoreDefaultGlobalFontSetting() {
		if(defaultFontSetting == null || defaultFontSetting.keySet().size() == 0){
			return;
		}
		
		for (Enumeration keys = UIManager.getDefaults().keys(); keys
				.hasMoreElements();) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof FontUIResource)
				UIManager.put(key, defaultFontSetting.get(key));
		}
	}

	public static void showCenter(final Component frame) {
		frame.applyComponentOrientation(ComponentOrientation
				.getOrientation(UILang.getUsedLocale()));
		int width = frame.getWidth(), height = frame.getHeight();
		int w = (Toolkit.getDefaultToolkit().getScreenSize().width - width) / 2;
		int h = (Toolkit.getDefaultToolkit().getScreenSize().height - height) / 2;
		frame.setLocation(w, h);
		frame.setVisible(true);
		if(frame instanceof Window){
			((Window)frame).toFront();
		}
	}

	/**
	 * 
	 * @param panel
	 * @param width
	 *            为0，则为自适应高宽
	 * @param height
	 *            为0，则为自适应高宽
	 * @param title
	 * @param isAddCancle
	 *            如果为true，则添加一个Cancle
	 * @param jbOK
	 *            如果为null，则创建缺少的图标按钮
	 * @param cancelButText
	 * @param listener
	 * @param cancelListener
	 * @param frame
	 * @param model
	 * @param relativeTo
	 * @param isResizable
	 * @param delay 延时显示，如果当前有正在显示的CenterPanel，则当前对话关闭后，才后加载显示
	 */
	public static Window showCenterPanel(JPanel panel, int width, int height,
			String title, boolean isAddCancle, JButton jbOK,
			String cancelButText, final ActionListener listener,
			ActionListener cancelListener, JFrame frame, boolean model, boolean isNewJFrame, Component relativeTo, boolean isResizable, boolean delay) {
		return showCenterPanelOKDispose(panel, width, height, title, isAddCancle, jbOK,
				cancelButText, listener, cancelListener, true, frame, model, isNewJFrame, relativeTo, isResizable, delay);
	}
	
	public static Window showCenterMessage(final String msg){
		Window[] back = {null};
		showCenterMessageOnTop(null, false, msg, back);
		return back[0];
	}
	
	public static void showCenterMessageOnTop(final Frame parent, final boolean isModal, final String msg, final Window[] back){
		final Window waiting;
		final Container contentPane;
		
		final boolean isNewFrame = parent == null || isModal == false;
		if(isNewFrame){
			waiting = new JFrame();
			((JFrame)waiting).setUndecorated(true);
			contentPane = ((JFrame)waiting).getContentPane();
		}else{
			waiting = new JDialog(parent, isModal);
			((JDialog)waiting).setUndecorated(true);
			contentPane = ((JDialog)waiting).getContentPane();
		}
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		panel.add(new JLabel(msg, App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING),
				BorderLayout.CENTER);
		contentPane.add(panel);
		waiting.pack();
		back[0] = waiting;
		
		if(isNewFrame){
			showCenter(waiting);
		}else{
			new Thread(){
				public void run(){
					showCenter(waiting);
				}
			}.start();
			try{
				//等待上面充分绘制
				Thread.sleep(100);
			}catch (Exception e) {
			}
		}
	}

	public static Window showCenterPanelOKDispose(JPanel panel, int width,
			int height, String title, boolean isAddCancle, JButton jbOK,
			String cancelButText, final ActionListener listener,
			final ActionListener cancelListener, final boolean isOkDispose, JFrame parentframe,
			boolean model, boolean isNewJFrame, Component relativeToObj, boolean isResizable, boolean isDelay) {
		JButton jbCancle = new JButton(((cancelButText == null)?(String) ResourceUtil.get(1018):cancelButText),
				new ImageIcon(ImageSrc.CANCEL_ICON));
		final UIActionListener cancelAction = new UIActionListener() {
			@Override
			public void actionPerformed(Window window, JButton ok,
					JButton cancel) {
				synchronized (delayCenterWindow) {
					window.dispose();
					isShowCenterWindow = false;
					if (cancelListener != null) {
						cancelListener.actionPerformed(null);
					}
					loadDelayWindow();
				}
			}
		};
		if (jbOK == null) {
			try {
				JButton okButton = new JButton(
						(String) ResourceUtil.get(IContext.OK), new ImageIcon(
								ImageIO.read(ImageSrc.OK_ICON)));
				jbOK = okButton;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		final UIActionListener jbOKAction = new UIActionListener() {
			@Override
			public void actionPerformed(Window window, JButton ok,
					JButton cancel) {
				synchronized (delayCenterWindow) {
					if (isOkDispose) {
						window.dispose();
						isShowCenterWindow = false;
					}
					try {
						if (listener != null) {
							listener.actionPerformed(null);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					loadDelayWindow();
				}
			}
		};

		return showCenterPanelButtons(panel, width, height, title, isAddCancle, jbOK,
				jbCancle, jbOKAction, cancelAction, parentframe, model, isNewJFrame, relativeToObj, isResizable, isDelay);
	}

	public static Window showCenterPanelWindow(JPanel panel, int width,
			int height, boolean isAddCancle, final JButton jbOK, final JButton jbCancle,
			final UIActionListener jbOKAction, final UIActionListener cancelAction,
			final Window dialog, Component relativeTo, boolean isResizable, boolean delay) {
		dialog.setIconImage(App.SYS_LOGO);

		jbOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jbOKAction.actionPerformed(dialog, jbOK, jbCancle);
			}
		});
		final ActionListener quitAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				if (cancelAction != null) {
					cancelAction.actionPerformed(dialog, jbOK, jbCancle);
				}
			}
		};
		if(dialog instanceof ClosableWindow){
			((ClosableWindow)dialog).setCloseAction(quitAction);
		}
		JRootPane rootPane = null;
		if (dialog instanceof JFrame) {
			rootPane = ((JFrame) dialog).getRootPane();
		} else if (dialog instanceof JDialog){
			rootPane = ((JDialog) dialog).getRootPane();
		}
		rootPane.registerKeyboardAction(quitAction,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		JPanel base = new JPanel();
		base.setLayout(new GridBagLayout());

		Insets insets = new Insets(5, 5, 5, 5);
		base.add(panel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0,
				0));
		
		final Border panelBorder = panel.getBorder();
		final boolean isPanelTitledBorded = (panelBorder != null && panelBorder instanceof TitledBorder);
		if(isPanelTitledBorded == false){	
			JPanel separatorPane = new JPanel();
			separatorPane.setLayout(new BoxLayout(separatorPane, BoxLayout.PAGE_AXIS));
//			separatorPane.add(Box.createVerticalStrut(5));
			final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
			separatorPane.add(separator);
//			separatorPane.add(Box.createVerticalStrut(2));
//			separatorPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
			base.add(separatorPane, new GridBagConstraints(0, 1, 1, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				insets, 0, 0));
		}
		if (isAddCancle) {
			JPanel subPanel = new JPanel();
			subPanel.setLayout(new GridLayout(1, 2, 5, 5));
			jbCancle.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					cancelAction.actionPerformed(dialog, jbOK, jbCancle);
				}
			});
			subPanel.add(jbOK);
			subPanel.add(jbCancle);
			base.add(subPanel, new GridBagConstraints(0, isPanelTitledBorded?1:2, 1, 1, 0.0,
					0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
					insets, 0, 0));
		} else {
			base.add(jbOK, new GridBagConstraints(0, isPanelTitledBorded?1:2, 1, 1, 0.0, 0.0,
					GridBagConstraints.LINE_END, GridBagConstraints.NONE, insets,
					0, 0));
		}

		if (dialog instanceof JFrame) {
			Container c = ((JFrame) dialog).getContentPane();
			c.setLayout(new BorderLayout());
			c.add(base, BorderLayout.CENTER);
		} else if (dialog instanceof JDialog){
			Container c = ((JDialog) dialog).getContentPane();
			c.setLayout(new BorderLayout());
			c.add(base, BorderLayout.CENTER);
		}
		
		if (dialog instanceof JFrame) {
			((JFrame) dialog)
					.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		} else if (dialog instanceof JDialog){
			((JDialog) dialog)
					.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		}
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				quitAction.actionPerformed(null);
			}
		});
		jbOK.setFocusable(true);
		rootPane.setDefaultButton(jbOK);
		jbOK.requestFocus();

		if (width == 0 || height == 0) {
			dialog.pack();
		} else {
			dialog.setSize(width, height);
		}
		
		if(isResizable == false){
			if (dialog instanceof JFrame) {
				((JFrame) dialog).setResizable(false);
			} else if(dialog instanceof JDialog){
				((JDialog) dialog).setResizable(false);
			}
		}
		
		if(relativeTo != null){
			dialog.setLocationRelativeTo(relativeTo);
			dialog.applyComponentOrientation(ComponentOrientation
					.getOrientation(UILang.getUsedLocale()));
		}else{
			dialog.applyComponentOrientation(ComponentOrientation
					.getOrientation(UILang.getUsedLocale()));
			int width_d = dialog.getWidth(), height_d = dialog.getHeight();
			int w = (Toolkit.getDefaultToolkit().getScreenSize().width - width_d) / 2;
			int h = (Toolkit.getDefaultToolkit().getScreenSize().height - height_d) / 2;
			dialog.setLocation(w, h);
		}

		setVisibleCenterWindow(dialog, delay);

		return dialog;
	}
	
	private static boolean isShowCenterWindow = false;
	private static final Stack delayCenterWindow = new Stack();
	
	private static void setVisibleCenterWindow(final Window dialog, final boolean delay){
		synchronized (delayCenterWindow) {
			if(delay && isShowCenterWindow){
				delayCenterWindow.push(dialog);
			}else{
				dialog.setVisible(true);
				if(dialog instanceof Window){
					((Window)dialog).toFront();
				}
				isShowCenterWindow = true;		
			}
		}
	}
	
	private static void loadDelayWindow(){
		if(isShowCenterWindow == false && delayCenterWindow.size() > 0){
			Window dialog = (Window)delayCenterWindow.pop();
			setVisibleCenterWindow(dialog, false);
		}
	}

	public static Window showCenterPanelButtons(JPanel panel, int width,
			int height, String title, boolean isAddCancle, final JButton jbOK,
			final JButton jbCancle, final UIActionListener jbOKAction,
			final UIActionListener cancelAction, JFrame frame, boolean model,
			boolean isNewJFrame, Component relativeTo, boolean isResizable, boolean delay) {
		final Window dialog;

		dialog = App.buildCloseableWindow(isNewJFrame, frame, title, model);
		return showCenterPanelWindow(panel, width, height, isAddCancle, jbOK, jbCancle,
				jbOKAction, cancelAction, dialog, relativeTo, isResizable, delay);
	}

	public static void showCenterPanel(JPanel panel, int width, int height,
			String title) {
		showCenterPanel(panel, width, height, title, false, null, null, null,
				null, null, false, false, null, false, false);
	}

	private static JTextField jtfuuid;

	public static int showOptionDialog(Component parentComponent, Object message, String title) {
		Object[] options = {ResourceUtil.get(1032),
                ResourceUtil.get(1033),
                ResourceUtil.get(1018)};
		return JOptionPane.showOptionDialog(parentComponent,
				message,
				title,
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			options,
			options[0]);
	}

	public static void showInputPWDDialog(final String uuid, String pwd1,
			String pwd2, final boolean isToLogin) {
		final String passwdStr = (String) ResourceUtil.get(9030);

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		Insets insets = new Insets(5, 5, 5, 5);

		panel.setBorder(new TitledBorder(""));

		JLabel jluuid = new JLabel();
		jluuid.setIcon(new ImageIcon(ResourceUtil
				.getResource("hc/res/idsmile_22.png")));
		JPanel uuidPanelflow = new JPanel();
		uuidPanelflow.setLayout(new FlowLayout());
		uuidPanelflow.add(jluuid);
		uuidPanelflow.add(new JLabel((String)ResourceUtil.get(9074)));
		uuidPanelflow.add(new JLabel(":"));
		panel.add(uuidPanelflow, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));

		final int columns = 15;
		jtfuuid = new JTextField(uuid, columns);
		jtfuuid.setEditable(isToLogin);
		jtfuuid.setForeground(Color.BLUE);
		jtfuuid.setHorizontalAlignment(SwingConstants.RIGHT);
//		JPanel uuidPanel = new JPanel();
//		uuidPanel.setLayout(new FlowLayout());
//		uuidPanel.add(jtfuuid);
//		uuidPanel.add(new JLabel(new ImageIcon(ImageSrc.DONE_ICON)));//UUID OK图标
//		if (isToLogin && false) {
//			JButton payButton = new JButton("I am VIP");
//			payButton.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					showUnlock();
//				}
//			});
//			uuidPanel.add(payButton);
//		}
		panel.add(jtfuuid, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));


		JLabel jlPassword = new JLabel(passwdStr);
		jlPassword.setIcon(new ImageIcon(ImageSrc.PASSWORD_ICON));
		final String inputPwdTip = "<html>input new password for new account.<BR>password is saved locally.</html>";
		if(isToLogin){
			jlPassword.setToolTipText(inputPwdTip);
		}
		
		final JPasswordField passwd1, passwd2;
		passwd1 = new JPasswordField(pwd1, columns);
		passwd1.setEchoChar('*');
		passwd1.enableInputMethods(true);
		passwd1.setHorizontalAlignment(SwingUtilities.RIGHT);
		passwd2 = new JPasswordField(pwd2, columns);
		passwd2.setEchoChar('*');
		passwd2.enableInputMethods(true);
		passwd2.setHorizontalAlignment(SwingUtilities.RIGHT);

		JPanel pwJpanel = new JPanel();
		pwJpanel.setLayout(new FlowLayout());
		pwJpanel.add(jlPassword);
		pwJpanel.add(new JLabel(":"));
		panel.add(pwJpanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));
		Component subItem = passwd1;
		panel.add(subItem, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));

		JPanel doublepw = new JPanel();
		doublepw.setLayout(new FlowLayout());
		JLabel jlPassword2 = new JLabel(passwdStr);
		jlPassword2.setIcon(new ImageIcon(ImageSrc.PASSWORD_ICON));
		if(isToLogin){
			jlPassword2.setToolTipText(inputPwdTip);
		}
		doublepw.add(jlPassword2);

		// 两次密码图标
		// jlPassword2 = new JLabel();
		// jlPassword2.setIcon(new ImageIcon(ImageSrc.PASSWORD_ICON));
		// doublepw.add(jlPassword2);

		doublepw.add(new JLabel(":"));

		panel.add(doublepw, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));
		
		subItem = passwd2;
		panel.add(subItem, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));
//		if (isToLogin) {
//			// 增加安全说明文字
//			String url = "http://homecenter.mobi/msg/know.php?lang="
//					+ System.getProperties().getProperty("user.language");
//			String content = HttpUtil.getAjax(url);
//
//			if (content.length() > 0) {
//				JPanel know = new JPanel();
//				know.setLayout(new BorderLayout());
//				know.setBorder(new TitledBorder("Do you know?"));// <STRONG></STRONG><BR>
//				JLabel desc = new JLabel(content);
//				know.add(desc, BorderLayout.CENTER);
//				panel.add(know, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
//						GridBagConstraints.LINE_START, GridBagConstraints.NONE,
//						insets, 0, 0));
//			}
//		}

		// JPanel main = new JPanel();
		// main.setLayout(new GridBagLayout());
		// main.add(panel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
		// GridBagConstraints.CENTER, GridBagConstraints.BOTH,
		// new Insets(10, 10, 10, 10), 0, 0));
		//
		if(isToLogin == false){
			BufferedImage img = null;
			try{
				img = ImageIO.read(ResourceUtil.getResource("hc/res/tip_16.png"));
			}catch (Exception e) {
			}
			final JLabel passwordTip = new JLabel((String)ResourceUtil.get(9075), 
					new ImageIcon(img), SwingConstants.LEADING);

//			JPanel subPanel = new JPanel(new BorderLayout());
//			subPanel.add(forgetPwd, BorderLayout.CENTER);
//			subPanel.setBorder(new TitledBorder(""));
			panel.add(passwordTip, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
					GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
					0, 0));
		}
		JButton jbCancle = new JButton((String) ResourceUtil.get(1018),
				new ImageIcon(ImageSrc.CANCEL_ICON));
		final UIActionListener cancelAction = new UIActionListener() {
			@Override
			public void actionPerformed(Window window, JButton ok,
					JButton cancel) {
				window.dispose();
				if (isToLogin) {
					CCoreUtil.globalExit();
				}
			}
		};
		JButton jbOK = null;
		try {
			JButton okButton = new JButton(
					(String) ResourceUtil.get(IContext.OK), new ImageIcon(
							ImageIO.read(ImageSrc.OK_ICON)));
			jbOK = okButton;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		final UIActionListener jbOKAction = new UIActionListener() {
			@Override
			public void actionPerformed(Window window, JButton ok,
					JButton cancel) {
				try {
					final String email = jtfuuid.getText();
					if(ResourceUtil.checkEmailID(email, window) == false){
						return;
					}
					
					if ((passwd1.getText().getBytes(IConstant.UTF_8).length >= App.MIN_PWD_LEN)
							&& passwd2.getText().equals(passwd1.getText())) {
						window.dispose();
						PropertiesManager.setValue(PropertiesManager.p_uuid, email);
						IConstant.setUUID(email);
						App.storePWD(passwd2.getText());
						if (isToLogin) {
							startAfterInfo();
						}
					} else {
						JOptionPane.showMessageDialog(window,
								StringUtil.replace((String)ResourceUtil.get(9077), "{min}", "" + App.MIN_PWD_LEN),
								(String)ResourceUtil.get(9076), JOptionPane.ERROR_MESSAGE);
					}
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
			}
		};

		showCenterPanelButtons(panel, 0, 0, passwdStr, true, jbOK, jbCancle,
				jbOKAction, cancelAction, null, false, false, null, false, false);
		if(isToLogin){
			jtfuuid.requestFocus();
		}else{
			passwd1.requestFocus();
		}
	}

	public static void showImageURLWindow(final String title, final String url) {
		try {
			final ImageIcon image = new ImageIcon(new URL(url));
			if (image == null || image.getIconWidth() <= 0) {
				// 没有取到QR图片
				return;
			}
			JPanel jp = new JPanel() {
				@Override
				public void paint(Graphics g) {
					g.drawImage(image.getImage(), 68, 48, this);
				}
			};

			final JFrame frame = new JFrame();
			frame.setTitle(title);
			frame.setIconImage(SYS_LOGO);// new File("hc/res/hc_16.png")
			frame.getRootPane().setLayout(new BorderLayout());
			frame.getRootPane().add(jp, BorderLayout.CENTER);
			JButton close = new JButton((String) ResourceUtil.get(1010));
			close.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					frame.dispose();
				}
			});
			frame.getRootPane().add(close, BorderLayout.SOUTH);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setResizable(false);
			frame.setSize(300, 300);
			frame.setVisible(true);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public static final int MIN_PWD_LEN = 6;

	public static void storePWD(String pwd) {
		final String base64 = App.getBASE64(pwd);
		PropertiesManager.setValue(PropertiesManager.p_password,
				base64);
		IConstant.setPassword(pwd);
		
		reloadEncrypt();
		
		if (IConstant.serverSide) {
			String checkAutoStart = IConstant.TRUE;
			PropertiesManager.setValue(PropertiesManager.p_AutoStart,
					checkAutoStart);
		}

		PropertiesManager.saveFile();
	}

	public static void reloadEncrypt() {
		final boolean isServing = ServerUIUtil.isServing();
		if(isServing){
			ServerUIUtil.promptAndStop(true, null);
		}
		IEncrypter en = CUtil.userEncryptor;
		if(en != null){
			L.V = L.O ? false : LogManager.log("ID or password is changed, call user encryptor.notifyExit methoad.");
			en.notifyExit(IConstant.serverSide);
			L.V = L.O ? false : LogManager.log("reload user encryptor.");
		}
		CUtil.userEncryptor = CUtil.loadEncryptor();
		if(isServing){
			ServerUIUtil.restartResponsorServerDelayMode();
		}
	}

	// 将 BASE64 编码的字符串 s 进行解码
	public static String getFromBASE64(String s) {
		if (s == null)
			return null;
		try {
			byte[] b = ByteUtil.decodeBase64(s);
			return new String(b, IConstant.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}

	public static void applyLookFeel(String newSkin, String errorSkin) {
		if(newSkin == null){
			return;
		}
		try {
			if (newSkin.startsWith(ConfigPane.SYS_LOOKFEEL)) {
				newSkin = newSkin.substring(ConfigPane.SYS_LOOKFEEL.length());
				for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
					if (newSkin.equals(info.getName())) {
						UIManager.setLookAndFeel(info.getClassName());
						return;
					}
				}
			} else {
				try{
					UIManager.setLookAndFeel(newSkin);
				}catch (Throwable e) {
					JOptionPane.showConfirmDialog(null, e.getClass().getName() + " : \n" + e.getMessage(), 
							"Apply UI LookAndFeel Error", JOptionPane.CLOSED_OPTION, 
							JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
				}
				return;
			}
		} catch (Throwable e) {
			LogManager.err("Loading look and feel error : " + newSkin);
			e.printStackTrace();
		}
		if(errorSkin != null){
			PropertiesManager.setValue(PropertiesManager.C_SKIN, errorSkin);
			PropertiesManager.saveFile();
			
			applyLookFeel(errorSkin, null);
		}
	}

	// 将 s 进行 BASE64 编码
	public static String getBASE64(String s) {
		if (s == null)
			return null;
		try {
			return ByteUtil.encodeBase64(s.getBytes(IConstant.UTF_8));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void showTuto() {
		try {
			String[] urls = new String[7];
			for (int i = 0; i < urls.length;) {
				urls[i++] = "step" + i + ".png";
			}
			final HCNexter next = new HCNexter(urls);
			JPanel imgPanel = new JPanel();
			final JLabel imgLabel = new JLabel();
			imgPanel.add(imgLabel);
			JButton jbOK = null;
			final String nextStr = (String) ResourceUtil.get(1029);
			try {
				JButton okButton = new JButton(nextStr, new ImageIcon(
						ImageIO.read(ResourceUtil
								.getResource("hc/res/next_22.png"))));
				jbOK = okButton;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			final Icon nextIco = jbOK.getIcon();
			final JButton nextBtn = jbOK;
			JButton jbpre = null;
			try {
				jbpre = new JButton((String) ResourceUtil.get(1030),
						new ImageIcon(ImageIO.read(ResourceUtil
								.getResource("hc/res/prev_22.png"))));
				jbpre.setEnabled(false);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			imgLabel.setIcon(new ImageIcon(next.imgs[0]));
			UIActionListener nextal = new UIActionListener() {
				@Override
				public void actionPerformed(Window window, JButton ok,
						JButton cancel) {
					if (next.idx == 0) {
						cancel.setEnabled(true);
					}
					if (next.idx < (next.imgUrl.length - 1)) {
						final BufferedImage image = next.imgs[++next.idx];
						if (image == null) {
							next.idx--;
							return;
						}
						imgLabel.setIcon(new ImageIcon(image));
						if (next.idx == (next.imgUrl.length - 1)) {
							ok.setText((String) ResourceUtil.get(IContext.OK));
							try {
								ok.setIcon(new ImageIcon(ImageIO
										.read(ImageSrc.OK_ICON)));
							} catch (IOException e) {
							}
						}
					} else {
						window.dispose();
						
						showLockWarning();
					}
				}
			};
			UIActionListener preal = new UIActionListener() {
				@Override
				public void actionPerformed(Window window, JButton ok,
						JButton cancel) {
					if (next.idx == next.imgUrl.length - 1) {
						nextBtn.setText(nextStr);
						nextBtn.setIcon(nextIco);
					}

					if (next.idx == 0) {
						cancel.setEnabled(false);
					} else if (next.idx < next.imgUrl.length) {
						imgLabel.setIcon(new ImageIcon(next.imgs[--next.idx]));
					}
				}
			};
			showCenterPanelButtons(imgPanel, 0, 0,
					(String) ResourceUtil.get(9029), true, jbOK, jbpre, nextal,
					preal, null, false, false, null, false, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void showUnlock() {
		final JDialog showDonate = new JDialog();
		showDonate.setTitle("VIP Register");
		showDonate.setIconImage(App.SYS_LOGO);
		java.awt.event.ActionListener exitActionListener = new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showDonate.dispose();
			}
		};
		showDonate.getRootPane().registerKeyboardAction(exitActionListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		showDonate.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		showDonate.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				showDonate.dispose();
			}
		});

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2, 2, 10, 10));
		// panel.setBorder(new TitledBorder((String)ResourceUtil.get(9010)));
		JLabel label_ID = new JLabel("Email");
		label_ID.setHorizontalAlignment(SwingConstants.LEFT);
		label_ID.setIcon(new ImageIcon(ResourceUtil
				.getResource("hc/res/idsmile_22.png")));
		panel.add(label_ID);
		final JTextField donateID = new JTextField("");
		donateID.setColumns(15);
		if(ResourceUtil.validEmail(IConstant.uuid)){
			donateID.setText(IConstant.uuid);
		}
		panel.add(donateID);
		JLabel label_key = new JLabel("Token");
		label_key.setIcon(new ImageIcon(ResourceUtil
				.getResource("hc/res/vip_22.png")));
		label_key.setHorizontalAlignment(SwingConstants.LEFT);
		panel.add(label_key);
		final JTextField donateKey = new JTextField("");
		panel.add(donateKey);

		JButton jbOK = new JButton("OK", new ImageIcon(ImageSrc.OK_ICON));
		jbOK.setText((String) ResourceUtil.get(IContext.OK));
		showDonate.getRootPane().setDefaultButton(jbOK);
		JButton jbExit = new JButton("", new ImageIcon(ImageSrc.CANCEL_ICON));
		jbExit.setText((String) ResourceUtil.get(1018));

		jbOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final String donateIDStr = donateID.getText().trim();
				if(ResourceUtil.checkEmailID(donateIDStr, showDonate) == false){
					return;
				}

				final String donateKeyStr = donateKey.getText().trim();

				String result = RootServerConnector.bindDonateKey(donateIDStr, donateKeyStr);
				if (result == null) {
					JOptionPane.showMessageDialog(null,
							"Error connect to server! try again.",
							(String) ResourceUtil.get(IContext.ERROR),
							JOptionPane.ERROR_MESSAGE);
					return;
				} else if (result.equals("ok")) {
					showDonate.dispose();

					ConnectionManager.addBeforeConnectionBiz(new hc.server.AbstractDelayBiz(null){
						@Override
						public void doBiz() {
							PropertiesManager.setValue(PropertiesManager.p_uuid, donateIDStr);
							PropertiesManager.setValue(PropertiesManager.p_Token, donateKeyStr);
							PropertiesManager.saveFile();

							IConstant.setUUID(donateIDStr);
							TokenManager.refreshToken(donateKeyStr);

							((J2SEContext) ContextManager
									.getContextInstance())
									.buildMenu(UILang
											.getUsedLocale());

							reloadEncrypt();
						}
					});
					
					//强制重连
					SIPManager.startRelineonForce(false);
					
					JPanel jpanel = new JPanel();
					try {
						jpanel.add(new JLabel(
								"<html>success active VIP token. Login ID is [<STRONG>" + donateIDStr + "</STRONG>]</html>",
								new ImageIcon(ImageIO.read(ImageSrc.OK_ICON)),
								SwingConstants.LEFT));
					} catch (IOException e2) {
					}
					App.showCenterPanel(jpanel, 0, 0, "Success");

					if (jtfuuid != null) {
						jtfuuid.setText(donateIDStr);
					}
					return;
				} else {
					JPanel jpanel = new JPanel();
					jpanel.add(new JLabel(
							"<html><STRONG>Invalid Token</STRONG><BR><BR>Please check the token again, "
									+ "<BR>or email help@homecenter.mobi</html>",
							App.getSysIcon(App.SYS_ERROR_ICON),
							SwingConstants.LEFT));
					App.showCenterPanel(jpanel, 0, 0, "Unknow Status");
				}
			}
		});
		jbExit.addActionListener(exitActionListener);
		JPanel allPanel = new JPanel();
		allPanel.setLayout(new BorderLayout());
		panel.setBorder(new TitledBorder(""));
		allPanel.add(panel, BorderLayout.CENTER);
		JPanel jPanel3 = new JPanel();
		allPanel.add(jPanel3, BorderLayout.SOUTH);
		jPanel3.add(jbOK, null);
		jPanel3.add(jbExit, null);
		JButton toVIP = null;
		try {
			toVIP = new JButton("buy token to be VIP",
					new ImageIcon(ImageIO.read(ResourceUtil
							.getResource("hc/res/vip_22.png"))));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		toVIP.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String targetURL;
				try {
					targetURL = HttpUtil.buildLangURL("pc/vip.htm", null);
					HttpUtil.browseLangURL(targetURL);
				} catch (UnsupportedEncodingException e1) {
				}
			}
		});
		jPanel3.add(toVIP, null);
		showDonate.add(allPanel);
		showDonate.pack();

		showCenter(showDonate);

	}

	private static void startRegDialog() {
//		String uuid = IConstant.getInstance().getAjaxForSimu(
//				"http://homecenter.mobi/ajax/call.php?f=uuid", false);

//		if (uuid == null) {
//			JOptionPane.showMessageDialog(null,
//					(String) ResourceUtil.get(1000),
//					(String) ResourceUtil.get(IContext.ERROR),
//					JOptionPane.ERROR_MESSAGE);
//			ExitManager.exit();
//		} else {
//			uuid = UUIDUtil.addCheckCode("0" + uuid);
//
//			// http://localhost:8080/images/qr.php?txt=helloWorld%E4%B8%AD%E5%9B%BD123456780
//			// 返回一个PNG图片
//			App.showImageURLWindow(uuid,
//					"http://homecenter.mobi/images/qr.php?txt="
//							+ uuid);
//
//			PropertiesManager.setValue(PropertiesManager.p_uuid,
//					uuid);

			String pwd1 = "", pwd2 = "";

			showInputPWDDialog("", pwd1, pwd2, true);
			// storePWD("123456");
			//
			// PropertiesManager.setValue(PropertiesManager.p_IsInitPWD,
			// IConstant.TRUE);
			// PropertiesManager.saveFile();
			//
			// startAfterInfo();

			if (RootConfig.getInstance().isTrue(
					RootConfig.p_First_Reg_Tuto)) {
				ConditionWatcher.addWatcher(new IWatcher() {
					@Override
					public boolean watch() {
						if (ContextManager.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON) {
							// 服务器上线，启动引导运行手机端
							JPanel totuQuest = new JPanel();
							totuQuest.add(new JLabel(
									((String) ResourceUtil
											.get(9036)) + "?",
									App.getSysIcon(App.SYS_QUES_ICON),
									SwingConstants.LEFT));
							JButton jbOK = null;
							try {
								jbOK = new JButton(
										(String) ResourceUtil
												.get(IContext.OK),
										new ImageIcon(
												ImageIO.read(ImageSrc.OK_ICON)));
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							App.showCenterPanelOKDispose(
									totuQuest,
									300,
									120,
									((String) ResourceUtil
											.get(9029)) + "?",
									true,
									jbOK,
									null, new ActionListener() {
										@Override
										public void actionPerformed(
												ActionEvent e) {
											showTuto();
										}
									}, new ActionListener() {
										@Override
										public void actionPerformed(ActionEvent e) {
											showLockWarning();
										}
									}, true, null,
									false, false, null, false, false);
							return true;
						} else {
							return false;
						}
					}

					@Override
					public void setPara(Object p) {
					}

					@Override
					public boolean isNotCancelable() {
						return false;
					}

					@Override
					public void cancel() {
					}
				});
			}else{
				ConditionWatcher.addWatcher(new IWatcher() {
					@Override
					public boolean watch() {
						if (ContextManager.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON) {
							showLockWarning();
							return true;
						}else{
							return false;
						}
					}

					@Override
					public void setPara(Object p) {
					}

					@Override
					public void cancel() {
					}

					@Override
					public boolean isNotCancelable() {
						return false;
					}
				});
			}
//		end if uuid
//		}
	}
	
	private static void showLockWarning(){
		if(IDArrayGroup.checkAndAdd(IDArrayGroup.MSG_ID_LOCK_SCREEN) == false){
			return;
		}
		
		JPanel lockPanel = new JPanel(new BorderLayout(0, 20));
		//final String targetURL = "https://forums.oracle.com/thread/1279871";
		Icon cong_icon = null;
		try {
			cong_icon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/art_cong.png")));
		} catch (IOException e1) {
		}
		final int panelWidth = 500;
		final JButton congLabel = new JButton
				("<html><body style=\"width:" + (panelWidth - cong_icon.getIconWidth()) + "\">"
						+ StringUtil.replace((String)ResourceUtil.get(9065), "{uuid}", IConstant.uuid) +
						"</body></html>",
						cong_icon);
		
		congLabel.setDisabledIcon(cong_icon);
		congLabel.setEnabled(false);
		congLabel.setBorderPainted(false);
		final boolean isLockWarn = RootConfig.getInstance().isTrue(RootConfig.p_Lock_Warn_First_Login);

		if(isLockWarn){
			congLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		}
		
		//L.V = L.O ? false : LogManager.log("os.version : " + System.getProperty("os.name"));
		if(isLockWarn && 
				(ResourceUtil.isWindowsXP() || ResourceUtil.isWindows2003()
				|| ResourceUtil.isWindows2008() || ResourceUtil.isWindowsVista())){
			lockPanel.add(congLabel, BorderLayout.NORTH);

			final JLabel lockLabel = new JLabel
					("<html><body style=\"width:" + panelWidth + "\">" +
							"<STRONG>Important : </STRONG><BR><BR>In " + System.getProperty("os.name") + " (<STRONG>not</STRONG> Windows 7 64bit or other), when mobile is accessing desktop, " +
							"press Win + L to lock screen(or screen save is triggered), mobile phone will display full black. " +
	//						"<BR>for more, click <a href=''>" + targetURL + "</a>" + 
							"<BR><BR>Windows 7 (Fedora 19) works well in lock mode." +
							"<BR><BR>Strongly recommended to install Windows 7." +
							"</body></html>",
					SwingConstants.LEFT);
			lockLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			lockPanel.add(lockLabel, BorderLayout.CENTER);
	//			lockLabel.addMouseListener(new MouseAdapter(){
	//			 public void mouseClicked(MouseEvent e) {
	//				 HttpUtil.browseLangURL(targetURL);
	//			 }
	//		});
		}else{
			lockPanel.add(congLabel, BorderLayout.CENTER);
		}
		App.showCenterPanelOKDispose(
				lockPanel,
				0, 0,
				((String) ResourceUtil
						.get(IContext.INFO)),
				false,
				null,
				null, null, null, true, null,
					false, false, null, false, false);
	}
	
	public static Window buildCloseableWindow(final boolean newFrame, JFrame owner,
			final String title, final boolean model) {
		final String new_title = (title.indexOf("HomeCenter") >= 0)?title:title + " - HomeCenter";
		if (newFrame) {
			 class CloseableFrame extends JFrame implements ClosableWindow {
				 public CloseableFrame(String title){
					 super(title);
				 }
				 ActionListener al;
				@Override
				public void notifyClose() {
					al.actionPerformed(null);
				}
	
				@Override
				public void setCloseAction(ActionListener al) {
					this.al = al;
				}
		    }
			return new CloseableFrame(new_title) ;
		} else {
			class CloseableDialog extends JDialog implements ClosableWindow {
				 public CloseableDialog(Frame owner, String title, boolean modal){
					 super(owner, title, modal);
				 }
				 ActionListener al;
				@Override
				public void notifyClose() {
					al.actionPerformed(null);
				}
	
				@Override
				public void setCloseAction(ActionListener al) {
					this.al = al;
				}
			}
			return new CloseableDialog(owner, new_title, model);
		}
	}
}