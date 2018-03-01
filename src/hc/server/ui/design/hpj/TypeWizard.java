package hc.server.ui.design.hpj;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import hc.App;
import hc.core.IContext;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.res.ImageSrc;
import hc.server.HCActionListener;
import hc.server.HCWindowAdapter;
import hc.server.ui.HTMLMlet;
import hc.server.ui.design.Designer;
import hc.server.util.HCJDialog;
import hc.util.ResourceUtil;
import hc.util.UILang;

public class TypeWizard {
	static int type;
	private static HPNode wizardEnd;

	public static HPNode getWizardEnd() {
		if (type == 0) {
			return null;
		}
		return wizardEnd;
	}

	public static final String htmlmlet = HTMLMlet.class.getSimpleName().toLowerCase();

	final static ItemListener il = new ItemListener() {
		@Override
		public void itemStateChanged(final ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				String text = ((JRadioButton) e.getSource()).getText().toLowerCase();

				if (text.equals(htmlmlet)) {
				}

				if (text.equals(HCURL.DATA_CMD_EXIT) || text.equals(HCURL.DATA_CMD_CONFIG)) {
				} else {
					text = ResourceUtil.buildFirstUpcaseString(text);
					text += MenuManager.getNextNodeIdx();
				}
				final String url = HCURL
						.buildStandardURL(BaseMenuItemNodeEditPanel.getProtocal(type), text);
				final HCURL hcurl = HCURLUtil.extract(url);
				wizardEnd = buildItem(type, hcurl);
				HCURLUtil.hcurlCacher.cycle(hcurl);
			}
		}
	};

	/**
	 * 
	 * @param owner
	 * @param relativeTo
	 * @return 有可能是{@link HPMenuItem}，也有可能是{@link HPProcessor}
	 */
	public static HPNode chooseWizard(final JFrame owner, final Component relativeTo) {
		// 初始化
		type = 0;
		wizardEnd = null;

		final JDialog dialog = new HCJDialog(owner, "Add Menu Item Type", true);
		final ButtonGroup buttonGroup = new ButtonGroup();
		final JPanel panel = new JPanel(new GridLayout(1, HPNode.WIZARD_SELECTABLE_MENU_ITEM_SIZE));
		panel.setBorder(new TitledBorder("Select Menu Item Type:"));
		final JRadioButton[] rbs = new JRadioButton[HPNode.WIZARD_SELECTABLE_MENU_ITEM_SIZE];
		final JLabel[] dispButton = new JLabel[HPNode.WIZARD_SELECTABLE_MENU_ITEM_SIZE];
		final String nextStepStr = ResourceUtil.get(1029);
		final JButton ok = new JButton(nextStepStr, new ImageIcon(ImageSrc.OK_ICON));
		final int[] typeDescs = { HPNode.TYPE_MENU_ITEM_FORM, HPNode.TYPE_MENU_ITEM_CONTROLLER,
				HPNode.TYPE_MENU_ITEM_CMD, HPNode.TYPE_MENU_ITEM_SCREEN,
				HPNode.TYPE_MENU_ITEM_IOT };
		final String[] icons = { "form_22.png", "controller_22.png", "cmd_22.png", "screen_22.png",
				"iot_22.png" };
		final String desc = "<html>"
				+ "<STRONG>form</STRONG> : Mlet/HTMLMlet is implemented by J2SE Swing JComponents.<BR><BR>"
				+ "<STRONG>controller</STRONG> : a controller of smart device on mobile.<BR><BR>"
				+ "<STRONG>cmd</STRONG> : run JRuby script or executable command on server; or open configration form of mobile; or exit/back.<BR><BR>"
				+ "<STRONG>screen</STRONG> : display desktop of current server on mobile.<BR><BR>"
				+ "<STRONG>IoT</STRONG> : device, converter, or robot (coordinate zero or multiple devices) for IoT."
				+ "</html>";

		final ItemListener itemListener = new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if (e.getStateChange() != ItemEvent.SELECTED) {
					return;
				}

				// System.out.println("itemStateChanged" +
				// System.currentTimeMillis());

				ok.setEnabled(true);
				final JRadioButton jop = (JRadioButton) e.getSource();
				for (int i = 0; i < rbs.length; i++) {
					if (rbs[i] == jop) {
						type = typeDescs[i];
						break;
					}
				}
				if (type == HPNode.TYPE_MENU_ITEM_CONTROLLER
						|| type == HPNode.TYPE_MENU_ITEM_SCREEN) {
					final String ok_text = ResourceUtil.get(IContext.OK);
					if (ok.getText().equals(ok_text) == false) {
						ok.setText(ok_text);
					}
				} else {
					if (ok.getText().equals(nextStepStr) == false) {
						ok.setText(nextStepStr);
					}
				}
			}
		};
		ok.setEnabled(false);
		ok.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				if (type == HPNode.TYPE_MENU_ITEM_CONTROLLER) {
					final String url = buildDefaultTypeURL(HCURL.CONTROLLER_PROTOCAL)
							+ MenuManager.getNextNodeIdx();
					final HCURL hcurl = HCURLUtil.extract(url);
					wizardEnd = buildItem(HPNode.TYPE_MENU_ITEM_CONTROLLER, hcurl);
					dialog.dispose();
				} else if (type == HPNode.TYPE_MENU_ITEM_SCREEN) {
					wizardEnd = buildItem(HPNode.TYPE_MENU_ITEM_SCREEN,
							HCURLUtil.extract(HCURL.URL_HOME_SCREEN));
					dialog.dispose();
				} else {
					// 进入下一步
					// dialog.setVisible(false);
					if (type == HPNode.TYPE_MENU_ITEM_CMD) {
						// exit = "exit current menu and return to parent menu
						// or exit");
						// config = "enter mobile config panel in mobile side
						// when current item is clicked.");
						// cmd - My Command "do some response biz in server side
						// when current item is clicked from mobile side");
						final String[] subItems = { "command", HCURL.DATA_CMD_EXIT,
								HCURL.DATA_CMD_CONFIG };
						final String[] desc = {
								"running JRuby script in server side by clicking it from mobile menu.",
								"return to root menu or exit client.",
								"open mobile configration panel by clicking it from mobile menu." };
						selectSub(dialog, ok, subItems, desc);
					} else if (type == HPNode.TYPE_MENU_ITEM_FORM) {
						// screen -Desktop "enter disktop screen of PC in mobile
						// side when current item is clicked.");
						// screen -Mlet "<html>Mlet Screen is a panel in mobile
						// side for dispaly and controlling status of PC side,
						// <BR>which is instance from java class and JRuby in PC
						// side.</html> ");
						final String[] subItems = { "HTMLMlet", "Mlet" };
						final String[] desc = {
								"a HTML panel display and control status  in mobile side, which runs in server side."
										+ "<BR>adding ScriptPanel in it is allowed.",
								"a snapshot panel display and controlling status in mobile side, which runs in server side."
										+ "<BR>adding ScriptPanel in it is <STRONG>NOT</STRONG> allowed." };
						selectSub(dialog, ok, subItems, desc);
					} else if (type == HPNode.TYPE_MENU_ITEM_IOT) {
						selectIOT(dialog, ok);
					}
					dialog.dispose();
				}
			}
		}, App.getThreadPoolToken()));
		final JButton cancel = new JButton(ResourceUtil.get(1018),
				new ImageIcon(ImageSrc.CANCEL_ICON));
		cancel.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
				wizardEnd = null;
			}
		}));
		for (int i = 0; i < rbs.length; i++) {
			final int radioIdx = i;
			rbs[i] = new JRadioButton();
			dispButton[i] = new JLabel(HPMenuItem.getTypeDesc(typeDescs[i]),
					Designer.loadImg(icons[i]), SwingConstants.LEADING);
			dispButton[i].addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(final MouseEvent e) {
				}

				@Override
				public void mousePressed(final MouseEvent e) {
				}

				@Override
				public void mouseExited(final MouseEvent e) {
				}

				@Override
				public void mouseEntered(final MouseEvent e) {
				}

				@Override
				public void mouseClicked(final MouseEvent e) {
					rbs[radioIdx].setSelected(true);
				}
			});
			rbs[i].addItemListener(itemListener);
			buttonGroup.add(rbs[i]);
			{
				final JPanel subPanel = new JPanel(new BorderLayout());
				subPanel.add(rbs[i], BorderLayout.WEST);
				subPanel.add(dispButton[i], BorderLayout.CENTER);
				panel.add(subPanel);
			}
		}
		rbs[0].setSelected(true);

		final Container container = dialog.getContentPane();
		container.setLayout(new GridBagLayout());

		final Insets insets = new Insets(5, 5, 5, 5);
		{
			final JPanel _panel = new JPanel(new BorderLayout());
			_panel.add(panel, BorderLayout.NORTH);
			{
				final JPanel _subPanel = new JPanel(new BorderLayout());
				_subPanel.add(new JLabel("Description :"), BorderLayout.NORTH);
				final JPanel _desc = new JPanel(new BorderLayout());
				_desc.add(new JLabel(desc), BorderLayout.CENTER);
				_desc.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				_subPanel.add(_desc, BorderLayout.CENTER);

				_panel.add(_subPanel, BorderLayout.CENTER);
			}
			container.add(_panel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
		}
		{
			final JPanel separatorPane = new JPanel();
			separatorPane.setLayout(new BoxLayout(separatorPane, BoxLayout.PAGE_AXIS));
			final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
			separatorPane.add(separator);
			container.add(separatorPane, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
		}
		{
			final JPanel subPanel = new JPanel();
			subPanel.setLayout(new GridLayout(1, 2, 5, 5));
			subPanel.add(cancel);
			subPanel.add(ok);
			container.add(subPanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.LINE_END, GridBagConstraints.NONE, insets, 0, 0));
		}

		dialog.getRootPane().registerKeyboardAction(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
				wizardEnd = null;
			}
		}), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		dialog.addWindowListener(new HCWindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				dialog.dispose();
				wizardEnd = null;
			}
		});
		dialog.pack();

		dialog.setLocationRelativeTo(relativeTo);
		dialog.applyComponentOrientation(
				ComponentOrientation.getOrientation(UILang.getUsedLocale()));
		dialog.setVisible(true);

		if (type == 0) {
			wizardEnd = null;
		}
		// System.out.println("User select type : " +
		// ((wizardEnd==null)?"null":wizardEnd));
		return wizardEnd;
	}

	private static HPNode buildItem(final int type, final HCURL hcurl) {
		if (type == HPNode.TYPE_MENU_ITEM_IOT) {
			int subType = 0;
			if (hcurl.getURLLower().indexOf(HCURL.DATA_IOT_ROBOT.toLowerCase()) >= 0) {
				subType = HPNode.MASK_MSB_ROBOT;
			} else if (hcurl.getURLLower().indexOf(HCURL.DATA_IOT_CONVERTER.toLowerCase()) >= 0) {
				subType = HPNode.MASK_MSB_CONVERTER;
			} else if (hcurl.getURLLower().indexOf(HCURL.DATA_IOT_DEVICE.toLowerCase()) >= 0) {
				subType = HPNode.MASK_MSB_DEVICE;
			}
			final HPProcessor menuItem = new HPProcessor(subType, hcurl.elementID);
			menuItem.listener = ScriptModelManager.buildDefaultScript(type, hcurl);
			return menuItem;
		} else {
			final HPMenuItem menuItem = new HPMenuItem(type,
					"new node " + MenuManager.getNextNodeIdx());
			menuItem.url = hcurl.url;
			menuItem.listener = ScriptModelManager.buildDefaultScript(type, hcurl);
			return menuItem;
		}
	}

	private static void selectSub(final Window owner, final Component relativeObj,
			final String[] items, final String[] desc) {
		final JDialog dialog;
		if (owner instanceof Dialog) {
			dialog = new HCJDialog((Dialog) owner, "Choose Sub Type", true);
		} else {
			dialog = new HCJDialog((Frame) owner, "Choose Sub Type", true);
		}
		final ButtonGroup buttonGroup = new ButtonGroup();
		final JPanel panel = new JPanel(new GridLayout(1, items.length));
		panel.setBorder(new TitledBorder("Sub Type:"));
		final JRadioButton[] rbs = new JRadioButton[items.length];
		final JButton ok = new JButton(ResourceUtil.get(IContext.OK),
				new ImageIcon(ImageSrc.OK_ICON));
		final ActionListener actionListen = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
			}
		});
		ok.addActionListener(actionListen);

		final JButton cancel = new JButton(ResourceUtil.get(1018),
				new ImageIcon(ImageSrc.CANCEL_ICON));
		cancel.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
				type = 0;
			}
		}));
		String descs = "<html>";
		for (int i = 0; i < items.length; i++) {
			if (i > 0) {
				descs += "<BR><BR>";
			}
			descs += "<STRONG>" + items[i] + "</STRONG> : " + desc[i];
			rbs[i] = new JRadioButton(items[i]);
			rbs[i].addItemListener(il);
			buttonGroup.add(rbs[i]);
			panel.add(rbs[i]);
		}
		;
		rbs[0].setSelected(true);
		descs += "</html>";

		final Container container = dialog.getContentPane();
		container.setLayout(new GridBagLayout());

		final Insets insets = new Insets(5, 5, 5, 5);
		{
			final JPanel _panel = new JPanel(new BorderLayout());
			_panel.add(panel, BorderLayout.NORTH);
			{
				final JPanel _subPanel = new JPanel(new BorderLayout());
				_subPanel.add(new JLabel("Description :"), BorderLayout.NORTH);
				final JPanel _desc = new JPanel(new BorderLayout());
				_desc.add(new JLabel(descs), BorderLayout.CENTER);
				_desc.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				_subPanel.add(_desc, BorderLayout.CENTER);

				_panel.add(_subPanel, BorderLayout.CENTER);
			}

			container.add(_panel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
		}
		{
			final JPanel separatorPane = new JPanel();
			separatorPane.setLayout(new BoxLayout(separatorPane, BoxLayout.PAGE_AXIS));
			final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
			separatorPane.add(separator);
			container.add(separatorPane, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
		}
		{
			final JPanel subPanel = new JPanel();
			subPanel.setLayout(new GridLayout(1, 2, 5, 5));
			subPanel.add(cancel);
			subPanel.add(ok);
			container.add(subPanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.LINE_END, GridBagConstraints.NONE, insets, 0, 0));
		}

		dialog.getRootPane().registerKeyboardAction(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
				type = 0;
			}
		}), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				dialog.dispose();
				type = 0;
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(relativeObj);
		dialog.setVisible(true);
		dialog.toFront();
	}

	private static String buildDefaultTypeURL(final String protocal) {
		if (protocal.equals(HCURL.CONTROLLER_PROTOCAL)) {
			return HCURL.buildStandardURL(protocal, "Myctrl");
		}
		return null;
	}

	public static void selectIOT(final Window owner, final Component relativeTo) {
		type = HPNode.TYPE_MENU_ITEM_IOT;// 有可能被直接调用，故再次赋值
		wizardEnd = null;

		final String[] subItems = { HCURL.DATA_IOT_ROBOT, HCURL.DATA_IOT_CONVERTER,
				HCURL.DATA_IOT_DEVICE };
		final String[] desc = { "real robot or AI module, which controls one or multiple devices.",
				"convert message between device and robot, if device is NOT supported by robot directly.",
				"drive the real device and controlled by robot." };
		selectSub(owner, relativeTo, subItems, desc);
	}

	// public static void main(String[] args) {
	// chooseWizard(null);
	// }
}
