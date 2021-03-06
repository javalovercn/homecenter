package hc.server.data;

import hc.App;
import hc.core.CoreSession;
import hc.core.HCTimer;
import hc.core.MsgBuilder;
import hc.core.SessionManager;
import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.res.ImageSrc;
import hc.server.HCActionListener;
import hc.server.SingleJFrame;
import hc.server.data.screen.KeyComper;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.JcipManager;
import hc.server.ui.design.J2SESession;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class KeyComperPanel extends DataEditorPanel implements ActionListener {
	protected final ThreadGroup threadPoolToken = App.getThreadPoolToken();

	String body[][] = new String[DAOKeyComper.MAX_KEYCOMP_NUM][3];
	ImageIcon[] icons = new ImageIcon[DAOKeyComper.MAX_KEYCOMP_NUM];
	String colTitle[] = { (String) ResourceUtil.get(9109), // No, 序号
			(String) ResourceUtil.get(9022), (String) ResourceUtil.get(9025) };

	int[] combKeyCodes = buildComboKeyCodes();
	String[] combKeys = buildKeyEvent(combKeyCodes);

	// 可供用户使用的keycode列表
	static final int[] keyCodes = buildKeyCodes();
	static final String[] keyTexts = buildKeyEvent(keyCodes);

	private int[] buildComboKeyCodes() {
		if (ResourceUtil.isMacOSX()) {
			// 增加Mac command键
			final int[] out = { KeyEvent.VK_UNDEFINED, KeyEvent.VK_META, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT };
			return out;
		} else {
			final int[] out = { KeyEvent.VK_UNDEFINED, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT };
			return out;
		}
	}

	public static String[] buildKeyEvent(final int[] kcs) {
		final String[] out = new String[kcs.length];

		for (int i = 0; i < kcs.length; i++) {
			out[i] = getHCKeyText(kcs[i]);
		}
		return out;
	}

	private static int[] buildKeyCodes() {
		// 重要：如果以下添加，请同步添加到AndroidPlatformService.convertToAndroidKeyCodeAdAPI
		final int[] kc = { KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6,
				KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9,

				KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_C, KeyEvent.VK_D, KeyEvent.VK_E, KeyEvent.VK_F, KeyEvent.VK_G, KeyEvent.VK_H,
				KeyEvent.VK_I, KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_L, KeyEvent.VK_M, KeyEvent.VK_N, KeyEvent.VK_O, KeyEvent.VK_P,
				KeyEvent.VK_Q, KeyEvent.VK_R, KeyEvent.VK_S, KeyEvent.VK_T, KeyEvent.VK_U, KeyEvent.VK_V, KeyEvent.VK_W, KeyEvent.VK_X,
				KeyEvent.VK_Y, KeyEvent.VK_Z,

				KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4, KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7,
				KeyEvent.VK_F8, KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12,

				KeyEvent.VK_WINDOWS, KeyEvent.VK_CONTEXT_MENU, KeyEvent.VK_SPACE, KeyEvent.VK_CAPS_LOCK, KeyEvent.VK_TAB,
				KeyEvent.VK_ESCAPE, KeyEvent.VK_PRINTSCREEN, KeyEvent.VK_NUM_LOCK, KeyEvent.VK_PAUSE,

				KeyEvent.VK_INSERT, KeyEvent.VK_HOME, KeyEvent.VK_PAGE_UP, KeyEvent.VK_DELETE, KeyEvent.VK_END, KeyEvent.VK_PAGE_DOWN,
				KeyEvent.VK_BACK_SPACE, KeyEvent.VK_ENTER,

				KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT,

				KeyEvent.VK_MINUS, KeyEvent.VK_EQUALS, // 因为+是个特殊字符，所以关闭
				// KeyEvent.VK_PLUS,
				KeyEvent.VK_OPEN_BRACKET, KeyEvent.VK_CLOSE_BRACKET, KeyEvent.VK_BACK_SLASH, KeyEvent.VK_SEMICOLON, KeyEvent.VK_COMMA,
				KeyEvent.VK_PERIOD, KeyEvent.VK_SLASH
				// 无 %, &, *, ', ", ?, `, ~, <, >, {, }, |
		};
		return kc;
	}

	public static String getHCKeyText(final int keyCode) {
		final String finalBestText = KeyEvent.getKeyText(keyCode);
		if ((keyCode != KeyEvent.VK_META || keyCode != KeyEvent.VK_ALT) && finalBestText.length() == 1) {
			//  因为后期的版本可能支持最优表意字符，如Mac下的option
			return finalBestText;
		}

		switch (keyCode) {
		case KeyEvent.VK_COMMA:
			return ",";
		case KeyEvent.VK_PERIOD:
			return ".";
		case KeyEvent.VK_SLASH:
			return "/";
		case KeyEvent.VK_SEMICOLON:
			return ";";
		case KeyEvent.VK_EQUALS:
			return "=";
		case KeyEvent.VK_OPEN_BRACKET:
			return "[";
		case KeyEvent.VK_BACK_SLASH:
			return "\\";
		case KeyEvent.VK_CLOSE_BRACKET:
			return "]";

		case KeyEvent.VK_AT:
			return "@";
		case KeyEvent.VK_COLON:
			return ":";
		case KeyEvent.VK_CIRCUMFLEX:
			return "^";
		case KeyEvent.VK_DOLLAR:
			return "$";
		case KeyEvent.VK_EXCLAMATION_MARK:
			return "!";
		case KeyEvent.VK_LEFT_PARENTHESIS:
			return "(";
		case KeyEvent.VK_NUMBER_SIGN:
			return "#";
		case KeyEvent.VK_MINUS:
			return "-";
		case KeyEvent.VK_PLUS:
			return "+";
		case KeyEvent.VK_RIGHT_PARENTHESIS:
			return ")";
		case KeyEvent.VK_UNDERSCORE:
			return "_";

		case KeyEvent.VK_CONTEXT_MENU:
			return "WMenu";

		case KeyEvent.VK_UNDEFINED:
			return "";

		case KeyEvent.VK_META:
			return (ResourceUtil.isMacOSX()) ? ResourceUtil.getMacOSCommandKeyText() : finalBestText;// 8984就是Mac的command键

		case KeyEvent.VK_ALT:
			return (ResourceUtil.isMacOSX()) ? ResourceUtil.getMacOSOptionKeyText() : finalBestText;// 8984就是Mac的command键
		}
		return finalBestText;
	}

	private final static int STANDARD_ICON_SIZE = 32;

	private Icon[] convert(final String[] urls) {
		final Icon[] icos = new Icon[urls.length];
		for (int i = 0; i < icos.length; i++) {
			try {
				BufferedImage bi = ImageIO.read(ResourceUtil.getAbsPathInputStream(urls[i]));
				final int imgSize = bi.getWidth();
				if (imgSize == STANDARD_ICON_SIZE) {
				} else if (imgSize == 16) {
				} else {
					bi = ResourceUtil.resizeImage(bi, STANDARD_ICON_SIZE, STANDARD_ICON_SIZE);
				}
				icos[i] = new ImageIcon(bi);
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		return icos;
	}

	public static void showKeyComperPanel() {
		SingleJFrame.showJFrame(KeyComperJFrame.class);
	}

	private JTable table;

	ImageIcon addIcon = new ImageIcon(ImageSrc.ADD_SMALL_ICON);
	ImageIcon modifyIcon = new ImageIcon(ImageSrc.MODIFY_SMALL_ICON);

	// JPanel jPanel2 = new JPanel();
	JPanel jPanel3 = new JPanel();
	Border border1;
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	// TitledBorder titledBorder2;
	JButton jbRemove = new JButton("", new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
	JButton jbTest = new JButton("", new ImageIcon(ImageSrc.TEST_SMALL_ICON));
	JButton jbDown = new JButton("", new ImageIcon(ImageSrc.DOWN_SMALL_ICON));
	JButton jbUp = new JButton("", new ImageIcon(ImageSrc.UP_SMALL_ICON));
	JButton jbSave = new JButton("", new ImageIcon(ImageSrc.OK_ICON));
	JButton jbCancel = new JButton("", new ImageIcon(ImageSrc.CANCEL_ICON));
	JButton jbAdd = new JButton(ADD_TAG, addIcon);
	JButton jbBuildImage = new JButton((String) ResourceUtil.get(9021), new ImageIcon(ImageSrc.BUILD_SMALL_ICON));

	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JComboBox jcb1 = new JComboBox(combKeys);
	JComboBox jcb2 = new JComboBox(combKeys);
	JComboBox jcb3 = new JComboBox(keyTexts);

	String[] imagesURL = DAOKeyComper.getImagesURL();
	Icon[] imagesIco = convert(imagesURL);

	JComboBox jcbImage = new JComboBox(imagesIco);
	JLabel jlKey2 = new JLabel();
	JLabel jlKey3 = new JLabel();
	// JLabel jlTip = new JLabel((String)ResourceUtil.get(9027));//停止使用
	boolean needSave;

	public KeyComperPanel(final boolean needSave) {
		this.needSave = needSave;

		table = new JTable();

		table.setModel(new AbstractTableModel() {
			@Override
			public String getColumnName(final int columnIndex) {
				return colTitle[columnIndex];
			}

			@Override
			public int getRowCount() {
				return body.length;
			}

			@Override
			public int getColumnCount() {
				return colTitle.length;
			}

			@Override
			public Object getValueAt(final int rowIndex, final int columnIndex) {
				return body[rowIndex][columnIndex];
			}
		});
		table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
					final boolean hasFocus, final int row, final int column) {
				if (isSelected || row == table.getSelectedRow()) {
					this.setForeground(table.getSelectionForeground());
					this.setBackground(table.getSelectionBackground());
				} else {
					this.setForeground(table.getForeground());
					this.setBackground(table.getBackground()); // 设置偶数行底色
				}
				setBorder(null);
				final String cell = body[row][column];
				if (cell != null && cell.length() > 0) {
					if (icons[row] == null) {
						try {
							BufferedImage bImageFromConvert = ImageIO.read(ResourceUtil.getAbsPathInputStream(cell));
							if (bImageFromConvert.getWidth() != 16) {
								bImageFromConvert = ResourceUtil.resizeImage(bImageFromConvert, 16, 16);
							}
							icons[row] = new ImageIcon(bImageFromConvert);
							setHorizontalAlignment(CENTER);
						} catch (final Exception e) {

						}
					}
				} else {
					icons[row] = null;
				}
				setIcon(icons[row]);
				return this;
				// return super.getTableCellRendererComponent(table, value,
				// isSelected, hasFocus, row, column);
			}
		});
		table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
					final boolean hasFocus, final int row, final int column) {
				setHorizontalAlignment(CENTER);
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});
		final ListSelectionModel rowSM = table.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				refreshButton();
			}
		});
		table.getColumnModel().getColumn(0).setPreferredWidth(20);
		table.getColumnModel().getColumn(1).setPreferredWidth(180);
		table.getColumnModel().getColumn(2).setPreferredWidth(60);
		// table.setPreferredScrollableViewportSize(new Dimension(1200, 500));
		table.setRowSelectionAllowed(true);
		border1 = BorderFactory.createEmptyBorder();

		// try {
		// jlTip.setIcon(new ImageIcon(ImageIO.read(ResourceUtil
		// .getResource("hc/res/tip_16.png"))));
		// } catch (IOException e1) {
		// }

		jbRemove.setText((String) ResourceUtil.get(9018));
		jbTest.setText((String) ResourceUtil.get(9026));
		jbDown.setText((String) ResourceUtil.get(9020));
		jbUp.setText((String) ResourceUtil.get(9019));
		// titledBorder2.setTitle("GooDMor");
		// setBorder(titledBorder2);
		setLayout(borderLayout1);
		jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.LINE_AXIS));
		jlKey2.setText(" + ");
		jlKey3.setText(" + ");
		jbSave.setText((String) ResourceUtil.get(1017));
		jbSave.setSelected(true);
		jbCancel.setText((String) ResourceUtil.get(1011));// 取消改为退出

		jPanel1.add(jcb1, null);
		jPanel1.add(jlKey2, null);
		jPanel1.add(jcb2, null);
		jPanel1.add(jlKey3, null);
		jPanel1.add(jcb3, null);
		jPanel1.setBorder(new TitledBorder((String) ResourceUtil.get(9022)));

		final JPanel jpImage = new JPanel();
		jpImage.setLayout(new BoxLayout(jpImage, BoxLayout.LINE_AXIS));

		jpImage.add(jcbImage, null);
		jpImage.add(new JLabel(" "));
		jbBuildImage.setToolTipText("create new icon from the selected key(s), the icon will display in mobile for clicking");
		jpImage.add(jbBuildImage, null);
		jpImage.add(new JLabel(" "));
		jpImage.setBorder(new TitledBorder((String) ResourceUtil.get(9025)));
		final JPanel jpTop = new JPanel();
		jpTop.setBorder(new TitledBorder((String) ResourceUtil.get(9023)));
		jpTop.setLayout(new GridBagLayout());
		jpTop.add(jPanel1, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		{
			final JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
			panel.add(jpImage, null);
			final JPanel addPanel = new JPanel();
			addPanel.setBorder(new TitledBorder(MODIFY_TAG));
			addPanel.add(jbAdd);

			jpTop.add(panel, new GridBagConstraints(0, 1, 1, 1, 0.9, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(2, 2, 2, 2), 0, 0));
			jpTop.add(addPanel, new GridBagConstraints(1, 1, 1, 1, 0.1, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(2, 2, 2, 2), 0, 0));
		}

		final JPanel jpCenter = new JPanel();
		jpCenter.setLayout(new BorderLayout());

		jpCenter.add(new JScrollPane(table), BorderLayout.CENTER);
		jpCenter.setBorder(new TitledBorder((String) ResourceUtil.get(9024)));
		this.add(jpCenter, BorderLayout.CENTER);
		this.add(jpTop, BorderLayout.NORTH);
		final JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());
		bottom.add(jPanel3, BorderLayout.CENTER);
		// bottom.add(jlTip, BorderLayout.SOUTH);

		final JPanel bottomSet = new JPanel();
		bottomSet.setLayout(new GridBagLayout());
		bottomSet.add(bottom, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		this.add(bottomSet, BorderLayout.SOUTH);

		final int addButtonSize = (needSave) ? 5 : 3;

		jPanel3.setLayout(new GridLayout(1, addButtonSize, 5, 20));
		jPanel3.add(jbRemove);
		jPanel3.add(jbTest);
		jPanel3.add(jbUp);
		jPanel3.add(jbDown);
		if (needSave) {
			jPanel3.add(jbSave);
			jPanel3.add(jbCancel);
		}

		jbRemove.addActionListener(this);
		jbTest.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final int out = App.showConfirmDialog(KeyComperPanel.this, ResourceUtil.get(9167), "Test Key Input?",
						JOptionPane.OK_CANCEL_OPTION);
				if (out == JOptionPane.OK_OPTION) {
					final String keyDesc = body[table.getSelectedRow()][1];
					if (keyDesc == null) {
						App.showMessageDialog(KeyComperPanel.this, "It can't be null!", "Error", JOptionPane.ERROR_MESSAGE,
								App.getSysIcon(App.SYS_ERROR_ICON));
						return;
					}
					KeyComper.actionKeys(keyDesc);
				}
			}
		}, threadPoolToken));
		jbDown.addActionListener(this);
		jbUp.addActionListener(this);
		jbSave.addActionListener(this);
		jbCancel.addActionListener(this);
		jbAdd.addActionListener(this);
		jbBuildImage.addActionListener(this);

		// for (int i = 0; i < keyCodes.length; i++) {
		// System.out.println(KeyEvent.getKeyText(keyCodes[i]));
		// }

		for (int i = 0; i < MAX_SIZE;) {
			body[i][0] = String.valueOf(++i);
		}

		final KeyComper kc = DAOKeyComper.getInstance(null).getKeyComper();
		size = kc.size();
		for (int i = 1; i <= size; i++) {
			final Vector<String> vs = kc.getKeysDesc(i);
			String desc = "";
			for (int j = 0; j < vs.size(); j++) {
				if (desc.length() > 0) {
					desc += "+";
				}
				desc += vs.elementAt(j);
			}
			body[i - 1][1] = desc;
			try {
				body[i - 1][2] = kc.getImagePath(i);
				appendImgToCombox(body[i - 1][2]);
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
		}

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowSelectionInterval(0, 0);

		refreshButton();
	}

	private void appendImgToCombox(final String url) {
		for (int i = 0; i < imagesURL.length; i++) {
			if (imagesURL[i].equals(url)) {
				return;
			}
		}

		final String[] newImgURL = new String[imagesURL.length + 1];
		for (int i = 0; i < imagesURL.length; i++) {
			newImgURL[i] = imagesURL[i];
		}
		newImgURL[imagesURL.length] = url;
		imagesURL = newImgURL;

		final Icon[] newIco = new Icon[imagesURL.length];
		for (int i = 0; i < imagesIco.length; i++) {
			newIco[i] = imagesIco[i];
		}
		try {
			BufferedImage bi = ImageIO.read(ResourceUtil.getAbsPathInputStream(url));
			final int imgSize = bi.getWidth();
			if (imgSize == STANDARD_ICON_SIZE) {
			} else if (imgSize == 16) {
			} else {
				bi = ResourceUtil.resizeImage(bi, STANDARD_ICON_SIZE, STANDARD_ICON_SIZE);
			}
			newIco[imagesIco.length] = new ImageIcon(bi);//

		} catch (final Exception e) {
			newIco[imagesIco.length] = new ImageIcon(App.SYS_LOGO);
			ExceptionReporter.printStackTrace(e);
		}
		imagesIco = newIco;
		jcbImage.setModel(new DefaultComboBoxModel(imagesIco));
		jcbImage.updateUI();
	}

	int size;

	JFrame inFrame;
	public static final char[] DIRECT_KEYS = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
			's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
			'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',

			'`', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '=', '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_',
			'+', '\t', '\n', '[', ']', '\\', '{', '}', '|', ';', ':', '\'', '"', ',', '<', '.', '>', '/', '?', ' ' };

	public static final int getCharKeyCode(final char ch) {
		switch (ch) {
		case 'a':
			return KeyEvent.VK_A;
		case 'b':
			return KeyEvent.VK_B;
		case 'c':
			return KeyEvent.VK_C;
		case 'd':
			return KeyEvent.VK_D;
		case 'e':
			return KeyEvent.VK_E;
		case 'f':
			return KeyEvent.VK_F;
		case 'g':
			return KeyEvent.VK_G;
		case 'h':
			return KeyEvent.VK_H;
		case 'i':
			return KeyEvent.VK_I;
		case 'j':
			return KeyEvent.VK_J;
		case 'k':
			return KeyEvent.VK_K;
		case 'l':
			return KeyEvent.VK_L;
		case 'm':
			return KeyEvent.VK_M;
		case 'n':
			return KeyEvent.VK_N;
		case 'o':
			return KeyEvent.VK_O;
		case 'p':
			return KeyEvent.VK_P;
		case 'q':
			return KeyEvent.VK_Q;
		case 'r':
			return KeyEvent.VK_R;
		case 's':
			return KeyEvent.VK_S;
		case 't':
			return KeyEvent.VK_T;
		case 'u':
			return KeyEvent.VK_U;
		case 'v':
			return KeyEvent.VK_V;
		case 'w':
			return KeyEvent.VK_W;
		case 'x':
			return KeyEvent.VK_X;
		case 'y':
			return KeyEvent.VK_Y;
		case 'z':
			return KeyEvent.VK_Z;
		case '`':
			return KeyEvent.VK_BACK_QUOTE;
		case '0':
			return KeyEvent.VK_0;
		case '1':
			return KeyEvent.VK_1;
		case '2':
			return KeyEvent.VK_2;
		case '3':
			return KeyEvent.VK_3;
		case '4':
			return KeyEvent.VK_4;
		case '5':
			return KeyEvent.VK_5;
		case '6':
			return KeyEvent.VK_6;
		case '7':
			return KeyEvent.VK_7;
		case '8':
			return KeyEvent.VK_8;
		case '9':
			return KeyEvent.VK_9;
		case '-':
			return KeyEvent.VK_MINUS;
		case '=':
			return KeyEvent.VK_EQUALS;
		case '\t':
			return KeyEvent.VK_TAB;
		case '\n':
			return KeyEvent.VK_ENTER;
		case '[':
			return KeyEvent.VK_OPEN_BRACKET;
		case ']':
			return KeyEvent.VK_CLOSE_BRACKET;
		case '\\':
			return KeyEvent.VK_BACK_SLASH;
		case ';':
			return KeyEvent.VK_SEMICOLON;
		case '\'':
			return KeyEvent.VK_QUOTE;
		case ',':
			return KeyEvent.VK_COMMA;
		case '.':
			return KeyEvent.VK_PERIOD;
		case '/':
			return KeyEvent.VK_SLASH;
		case ' ':
			return KeyEvent.VK_SPACE;
		}
		return KeyEvent.VK_UNDEFINED;
	}

	public void setInFrame(final JFrame frame) {
		inFrame = frame;
	}

	public static final int MAX_SIZE = 6;

	public static final String MODIFY_TAG = (String) ResourceUtil.get(9017);
	public static final String ADD_TAG = (String) ResourceUtil.get(9016);

	private String[] reserve(final String[] src) {
		final String[] out = new String[src.length];

		for (int i = src.length - 1, j = 0; i >= 0; i--, j++) {
			out[j] = src[i];
		}
		return out;
	}

	private void refreshButton() {
		final int selectedRow = table.getSelectedRow();
		final int editRowNum = selectedRow + 1;

		jcb1.setSelectedIndex(0);
		jcb2.setSelectedIndex(0);
		jcb3.setSelectedIndex(0);
		jcbImage.setSelectedIndex(0);

		if (editRowNum <= size) {
			jbAdd.setText(MODIFY_TAG);
			jbAdd.setIcon(modifyIcon);

			final String keys = body[selectedRow][1];
			final String[] keyArrs = reserve(keys.split("\\+"));
			final JComboBox[] jcbs = { jcb1, jcb2, jcb3 };
			final int[][] mapKey = { combKeyCodes, combKeyCodes, keyCodes };
			for (int i = 0, j = 2; i < keyArrs.length; i++, j--) {
				final int[] tmpMap = mapKey[j];
				for (int k = 0; k < tmpMap.length; k++) {
					// 由于用户手写，可能使用小写，所以要转为大写
					if (keyArrs[i].toUpperCase().equals(fromKeyCodeToDesc(tmpMap[k]).toUpperCase())) {
						jcbs[j].setSelectedIndex(k);
						break;
					}
				}
			}

			final String imagebdURL = body[selectedRow][2];
			for (int i = 0; i < imagesURL.length; i++) {
				if (imagebdURL.equals(imagesURL[i])) {
					jcbImage.setSelectedIndex(i);
					break;
				}
			}
		} else {
			jbAdd.setText(ADD_TAG);
			jbAdd.setIcon(addIcon);
		}

		if (size == MAX_SIZE) {
			if (jbAdd.getText().equals(ADD_TAG)) {
				jbAdd.setEnabled(false);
			}
		} else {
			jbAdd.setEnabled(true);
		}

		if (editRowNum > size) {
			jbRemove.setEnabled(false);
			jbTest.setEnabled(false);
			jbDown.setEnabled(false);
			jbUp.setEnabled(false);
		} else {
			jbRemove.setEnabled(true);
			jbTest.setEnabled(true);

			if (editRowNum == size) {
				jbDown.setEnabled(false);
			} else {
				jbDown.setEnabled(true);
			}

			if (editRowNum == 1) {
				jbUp.setEnabled(false);
			} else {
				jbUp.setEnabled(true);
			}
		}
	}

	@Override
	public void notifySave() {
		super.notifySave();

		final DAOKeyComper kc = DAOKeyComper.getInstance(null);
		kc.getProperties().clear();

		for (int i = 0; i < size; i++) {
			kc.put(i + 1, body[i][1], body[i][2]);
		}

		kc.save();

		final CoreSession[] coreSSS = SessionManager.getAllSocketSessions();
		for (int i = 0; i < coreSSS.length; i++) {
			final J2SESession coreSS = (J2SESession) coreSSS[i];
			if (UserThreadResourceUtil.isInServing(coreSS.context)) {
				sendExtMouse(coreSS);
			}
		}
	}

	@Override
	public void notifyCancle() {
		super.notifyCancle();
	}

	private String getSelectImageURL() {
		return imagesURL[jcbImage.getSelectedIndex()];
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		int selectedRow = table.getSelectedRow();
		if (event.getSource() == jbAdd) {
			if (jbAdd.getText() == MODIFY_TAG) {
				// ResourceUtil.addMaybeUnusedResource(body[selectedRow][2],
				// false);
				body[selectedRow][1] = buildStoreDesc();
				body[selectedRow][2] = getSelectImageURL();
				icons[selectedRow] = null;
				// ResourceUtil.addMaybeUnusedResource(body[selectedRow][2],
				// true);
				table.updateUI();
			} else {
				body[size][1] = buildStoreDesc();
				body[size++][2] = getSelectImageURL();
				refreshButton();
			}
		} else if (event.getSource() == jbRemove) {
			// ResourceUtil.addMaybeUnusedResource(body[selectedRow][2], false);
			while ((++selectedRow) < size) {
				body[selectedRow - 1][1] = body[selectedRow][1];
				body[selectedRow - 1][2] = body[selectedRow][2];
				icons[selectedRow - 1] = icons[selectedRow];
			}
			body[selectedRow - 1][1] = "";
			body[selectedRow - 1][2] = "";
			icons[selectedRow - 1] = null;
			size--;

			refreshButton();
		} else if (event.getSource() == jbUp) {
			final int toIdx = selectedRow - 1;
			swapRow(selectedRow, toIdx);
			table.setRowSelectionInterval(toIdx, toIdx);
		} else if (event.getSource() == jbDown) {
			final int toIdx = selectedRow + 1;
			swapRow(selectedRow, toIdx);
			table.setRowSelectionInterval(toIdx, toIdx);
		} else if (event.getSource() == jbCancel) {
			notifyCancle();
			if (inFrame != null) {
				inFrame.dispose();
				return;
			}
		} else if (event.getSource() == jbSave) {
			notifySave();
			if (inFrame != null) {
				inFrame.dispose();
				return;
			}
		} else if (event.getSource() == jbBuildImage) {
			final BufferedImage bi = buildImage(buildImageDesc());

			StoreDirManager.createDirIfNeccesary(StoreDirManager.ICO_DIR);
			final String rid = ResourceUtil.createResourceID();

			final String urlResource = StoreDirManager.ICO_DIR + File.separator + rid + ".png";
			final File file = new File(ResourceUtil.getBaseDir(), "." + urlResource);
			try {
				ImageIO.write(bi, "png", file);
				// ResourceUtil.addMaybeUnusedResource(urlResource, true);

				appendImgToCombox(urlResource);
				jcbImage.setSelectedIndex(imagesURL.length - 1);

			} catch (final IOException e) {
				ExceptionReporter.printStackTrace(e);
				return;
			}
			return;
		}
		table.updateUI();
	}

	/**
	 * 
	 * @param width
	 * @param height
	 * @param tryFont
	 * @param g
	 * @param str
	 * @param failBigSize
	 *            0:表示向上+1未尝试过；>0表示该尺寸已尝试并失败
	 * @param pad
	 * @return
	 */
	private static int searchBestFontSizeForImage(final int width, final int height, final Font tryFont, final Graphics2D g,
			final String str, final int failBigSize, final int pad) {
		final FontRenderContext frc = g.getFontRenderContext();
		final Rectangle2D r2d = tryFont.getStringBounds(str, frc);
		final int drawStrWidth = (int) r2d.getWidth();
		final int drawStrHeight = (int) r2d.getHeight();
		if (drawStrWidth + pad > width || drawStrHeight + pad > height) {
			return searchBestFontSizeForImage(width, height, new Font(tryFont.getName(), tryFont.getStyle(), tryFont.getSize() - 1), g, str,
					tryFont.getSize(), pad);
		} else if (failBigSize > 0) {
			return tryFont.getSize();
		} else {
			return searchBestFontSizeForImage(width, height, new Font(tryFont.getName(), tryFont.getStyle(), tryFont.getSize() + 1), g, str,
					0, pad);
		}
	}

	private BufferedImage buildImage(final String desc) {
		final String[] items = desc.split("\\+");
		String imgStr = "";
		if (items.length == 1) {
			if (items[0].length() < 3) {
				imgStr = items[0];
			} else {
				imgStr = items[0].substring(0, 3);
			}
		} else if (items.length == 2) {
			imgStr = items[0].substring(0, 1) + "+" + items[1].substring(0, 1);
		} else {
			imgStr = items[0].substring(0, 1) + items[1].substring(0, 1) + items[2].substring(0, 1);
		}
		final int size = STANDARD_ICON_SIZE;

		Font font = new Font(Font.DIALOG, Font.BOLD, 12);
		final BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = (Graphics2D) bi.getGraphics();
		final int pad = 2;
		final int bestFontSize = searchBestFontSizeForImage(size, size, font, g, imgStr, 0, pad);
		font = new Font(font.getName(), font.getStyle(), bestFontSize);

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// g.setColor(Color.BLUE);
		// g.fillRect(0, 0, size, size);
		final FontRenderContext frc = g.getFontRenderContext();
		final Rectangle2D r2d = font.getStringBounds(imgStr, frc);
		final int drawStrWidth = (int) r2d.getWidth();
		final int drawStrHeight = (int) r2d.getHeight();
		int p_x = 0, p_y = 0;
		p_y = (size - drawStrHeight) / 2 - (int) r2d.getY();
		p_x = (size - drawStrWidth) / 2;
		g.setFont(font);

		final int[] shiftX = { -1, 0, 1, -1, 0, 1, -1, 0, 1 };
		final int[] shiftY = { -1, -1, -1, 0, 0, 0, 1, 1, 1 };
		g.setColor(Color.WHITE);
		for (int i = 0; i < shiftY.length; i++) {
			g.drawString(imgStr, p_x + shiftX[i], p_y + shiftY[i]);
		}
		g.setColor(Color.BLACK);
		g.drawString(imgStr, p_x, p_y);

		// int[] rgbArray = new int[outWidth * outWidth];
		// bi.getRGB(0, 0, outWidth, outWidth, rgbArray, 0, outWidth);
		// bi = new BufferedImage(outWidth, outWidth,
		// BufferedImage.TYPE_INT_ARGB);
		// bi.setRGB(0, 0, outWidth, outWidth, rgbArray, 0, outWidth);

		// 缩放成16X16
		// Image image=bi.getScaledInstance(16,16,Image.SCALE_DEFAULT);//获取缩略图
		// /*再创建一个BufferedImage对象 用于创建100*100大小的图像*/
		// BufferedImage oimage = new BufferedImage(16,16,Image.SCALE_DEFAULT);
		// /*获取图像上下文对象，然后把刚才的Image对象画到BufferedImage中去
		// 切忌，
		// drawImage()方法有很多重载方法，一定要选用下面的这个，它会95%的复制原图的图片质量。其他重载方法你也可以试试，可能生成出来的图片很丑噢~哈哈
		// */
		// oimage.getGraphics().drawImage(image,0,0, null);
		// return ResourceUtil.resizeImage(bi, 32, 32);
		return bi;
	}

	private void swapRow(final int fromIdx, final int toIdx) {
		final String v1 = body[toIdx][1];
		final String v2 = body[toIdx][2];
		final ImageIcon ii = icons[toIdx];

		body[toIdx][1] = body[fromIdx][1];
		body[toIdx][2] = body[fromIdx][2];
		icons[toIdx] = icons[fromIdx];

		body[fromIdx][1] = v1;
		body[fromIdx][2] = v2;
		icons[fromIdx] = ii;
	}

	public static BufferedImage rotateImage(final BufferedImage bufferedimage, final int degree) {
		final int w = bufferedimage.getWidth();
		final int h = bufferedimage.getHeight();
		final int type = bufferedimage.getColorModel().getTransparency();
		BufferedImage img;
		Graphics2D graphics2d;
		(graphics2d = (img = new BufferedImage(w, h, type)).createGraphics()).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2d.rotate(Math.toRadians(degree), w / 2, h / 2);
		graphics2d.drawImage(bufferedimage, 0, 0, null);
		graphics2d.dispose();
		return img;
	}

	private String buildStoreDesc() {
		String desc = buildCtrlAltShiftDesc();
		desc += getKeyCodeInputDesc(jcb3, keyCodes);

		return desc;
	}

	private String buildCtrlAltShiftDesc() {
		String desc = getKeyCodeInputDesc(jcb1, combKeyCodes);
		if (desc.length() > 0) {
			desc += "+";
		}
		final String keyCodeInputDesc = getKeyCodeInputDesc(jcb2, combKeyCodes);
		desc += keyCodeInputDesc;
		if (keyCodeInputDesc.length() > 0) {
			desc += "+";
		}
		return desc;
	}

	private String buildCtrlAltShiftIcon() {
		String desc = getKeyCodeInputIcon(jcb1, combKeyCodes);
		if (desc.length() > 0) {
			desc += "+";
		}
		final String keyCodeInputDesc = getKeyCodeInputIcon(jcb2, combKeyCodes);
		desc += keyCodeInputDesc;
		if (keyCodeInputDesc.length() > 0) {
			desc += "+";
		}
		return desc;
	}

	private String buildImageDesc() {
		String desc = buildCtrlAltShiftIcon();
		desc += keyTexts[jcb3.getSelectedIndex()];

		return desc;
	}

	public String getKeyCodeInputDesc(final JComboBox comboBox, final int[] mapingArray) {
		final int keyCode = mapingArray[comboBox.getSelectedIndex()];
		return fromKeyCodeToDesc(keyCode);
	}

	public String getKeyCodeInputIcon(final JComboBox comboBox, final int[] mapingArray) {
		final int keyCode = mapingArray[comboBox.getSelectedIndex()];
		return fromKeyCodeToDescIcon(keyCode);
	}

	private String fromKeyCodeToDesc(final int keyCode) {
		if (keyCode == KeyEvent.VK_UNDEFINED) {
			return "";
		} else {
			return getKeyDescOfClassFromKeyCode(keyCode);
		}
	}

	private String fromKeyCodeToDescIcon(final int keyCode) {
		if (keyCode == KeyEvent.VK_UNDEFINED) {
			return "";
		} else {
			return getHCKeyText(keyCode);
		}
	}

	private static final Vector<Integer> KEY_CODE_ALL_STATIC_FIELDS = new Vector<Integer>();
	private static final Vector<String> KEY_DESC_ALL_STATIC_FIELDS = new Vector<String>();

	/**
	 * 从静态类中提取FIELD_NAME <==> KeyCode对应关系，以便调用
	 */
	private static String getKeyDescOfClassFromKeyCode(final int keyCode) {
		if (KEY_CODE_ALL_STATIC_FIELDS.size() == 0) {
			//  初始化
			final Field[] fields = KeyEvent.class.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				final Field field = fields[i];
				final boolean isStatic = Modifier.isStatic(field.getModifiers());
				if (isStatic && field.getName().startsWith("VK_")) {
					try {
						KEY_CODE_ALL_STATIC_FIELDS.add((Integer) field.get(null));
						KEY_DESC_ALL_STATIC_FIELDS.add(field.getName().substring(3));// 去掉VK_，仅保留后段描述
					} catch (final Exception e) {
					}
				}
			}
		}

		for (int i = 0; i < KEY_CODE_ALL_STATIC_FIELDS.size(); i++) {
			if (KEY_CODE_ALL_STATIC_FIELDS.elementAt(i) == keyCode) {
				return KEY_DESC_ALL_STATIC_FIELDS.elementAt(i);
			}
		}

		return null;
	}

	public static void sendExtMouse(final J2SESession coreSS) {
		// 在Android环境下且内网下，需要进行延时操作
		new HCTimer("", 2500, true) {
			@Override
			public final void doBiz() {
				sendExtMouseBiz(coreSS);
				remove(this);
			}
		};
	}

	private static void sendExtMouseBiz(final J2SESession coreSS) {
		// 以下是测试段，
		// 传送用户自定义扩展MouseIcon
		// String[] icons =
		// {"iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9sJFAwmIeDrjusAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAACXElEQVQ4y1WQTYhNYRjHf+9xGTVxZZSFz9E9jERZmGaioREy"+
		// "rAgNDbNgYaZGytJKKRvFQqaQ3DsTJhYyGJpZkG8rSnHvxkZqSucc19R1z9znb3HPNde/3nqf932e/8cDgAVgIQAoJK2AE/aHLnVwpOcZpWbxMGP0+aIRICPwVe3HAv5BIQMW8EkhawEEG7ufo1VCGUO+EWfEyVq/L0AJgQKuKqRoAYv+EcK+e71cXC7O+sY7v0oiX1wB8K3mIqTPQmQBewC0I3mHp4LROsUWX4xkhHzRX4vQ"+
		// "qJCKBXykDgabDKTqaa3/8439vpjKGPNSznFMwgOG66wvFLxP7gCvBEscTApwHvczRsU5elGRUf1CEm3JQLvBG0FbzYHBYcFtQXu9kyXikMcEnXoJaqfVYEzwGkg5eOuqQ0NUdzEteC0YMzgoGrZd6Uk1pR6d7zqz58PjScF6YD6Aq1NxSQwHLomjkr956c+D5+62xpUYTDvrFjdXcFwwYdCmmSV2C+4YbAH4cl/t+aGK8sN6"+
		// "mKLISiKtI+0+e1ACrht8czDBjHpOsMyDHwCzpuKtVWv2xANGgAeEtqLmxINxQb9gOonTURtOcBhhkm5Vy8hOE5oR6oQLKrMSZU/wwmAc4PeGXQAUcvHefK6sQrbcx3+IdIPQRKTvRLqEtPuz3zIoOFBryWfLTYVcXCzkyoPVOgYizZCEdopQRiQxJXVfG5JgI0AhW15TyMUf89nyQK29kItJkXYQGaQ9WOBdJrSb4HqBrlJD"+
		// "w3aaO1u+XphYrPL0Uoc6Vh+dE+VzZRwOv2c2fwEzP0r3y/J05gAAAABJRU5ErkJggg==",
		// "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9sJFAwmIeDrjusAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAACXElEQVQ4y1WQTYhNYRjHf+9xGTVxZZSFz9E9jERZmGaioREy"+
		// "rAgNDbNgYaZGytJKKRvFQqaQ3DsTJhYyGJpZkG8rSnHvxkZqSucc19R1z9znb3HPNde/3nqf932e/8cDgAVgIQAoJK2AE/aHLnVwpOcZpWbxMGP0+aIRICPwVe3HAv5BIQMW8EkhawEEG7ufo1VCGUO+EWfEyVq/L0AJgQKuKqRoAYv+EcK+e71cXC7O+sY7v0oiX1wB8K3mIqTPQmQBewC0I3mHp4LROsUWX4xkhHzRX4vQ"+
		// "qJCKBXykDgabDKTqaa3/8439vpjKGPNSznFMwgOG66wvFLxP7gCvBEscTApwHvczRsU5elGRUf1CEm3JQLvBG0FbzYHBYcFtQXu9kyXikMcEnXoJaqfVYEzwGkg5eOuqQ0NUdzEteC0YMzgoGrZd6Uk1pR6d7zqz58PjScF6YD6Aq1NxSQwHLomjkr956c+D5+62xpUYTDvrFjdXcFwwYdCmmSV2C+4YbAH4cl/t+aGK8sN6"+
		// "mKLISiKtI+0+e1ACrht8czDBjHpOsMyDHwCzpuKtVWv2xANGgAeEtqLmxINxQb9gOonTURtOcBhhkm5Vy8hOE5oR6oQLKrMSZU/wwmAc4PeGXQAUcvHefK6sQrbcx3+IdIPQRKTvRLqEtPuz3zIoOFBryWfLTYVcXCzkyoPVOgYizZCEdopQRiQxJXVfG5JgI0AhW15TyMUf89nyQK29kItJkXYQGaQ9WOBdJrSb4HqBrlJD"+
		// "w3aaO1u+XphYrPL0Uoc6Vh+dE+VzZRwOv2c2fwEzP0r3y/J05gAAAABJRU5ErkJggg=="};
		String[] icons = null;
		if (coreSS.cap == null) {
			return;
		}
		KeyComper mis = coreSS.cap.mobiUserIcons;

		if (mis == null) {
			// 设计器先于初始化出现。
			DAOKeyComper.getInstance(coreSS.cap);
			mis = coreSS.cap.mobiUserIcons;
		}
		if (mis != null) {
			final int size = mis.size();

			final String[] tempIcons = new String[size];
			String path = null;
			try {
				for (int i = 1; i <= size; i++) {
					path = mis.getImagePath(i);
					final byte[] imgDataBa = ResourceUtil.getAbsPathContent(path);
					tempIcons[i - 1] = ByteUtil.encodeBase64(imgDataBa);
				}

				icons = tempIcons;
			} catch (final Exception e) {
				LogManager.err("Unable load mobi ext image content:" + path);
			}

		}

		if (icons == null) {
			final String[] is = { "null" };
			icons = is;
		}

		final StringBuilder sb = StringBuilderCacher.getFree();
		JcipManager.appendArray(sb, icons, false);
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		coreSS.context.send(MsgBuilder.E_SCREEN_EXT_MOUSE_ICON, out);
		// LogManager.log("Send out ExtMouseIco");
	}

}