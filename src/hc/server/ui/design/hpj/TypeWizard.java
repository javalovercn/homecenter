package hc.server.ui.design.hpj;

import hc.App;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.res.ImageSrc;
import hc.server.ui.design.Designer;
import hc.util.ResourceUtil;
import hc.util.UILang;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
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

public class TypeWizard {
	static int type;
	static HPMenuItem wizardEnd;
	private static final String DESKTOP = "desktop";
	
	public static HPMenuItem chooseWizard(final JFrame owner, final Component relativeTo){
		//初始化
		type = 0;
		wizardEnd = null;
		
		final JDialog dialog = new JDialog(owner, "select menu type", true);
		final ButtonGroup buttonGroup = new ButtonGroup();
		final JPanel panel = new JPanel(new GridLayout(1, HPNode.TYPE_MENU_ITEM_SIZE));
		panel.setBorder(new TitledBorder("Menu Item Type:"));
		final JRadioButton[] rbs = new JRadioButton[HPNode.TYPE_MENU_ITEM_SIZE];
		final JLabel[] dispButton = new JLabel[HPNode.TYPE_MENU_ITEM_SIZE];
		BufferedImage image = null;
		try {
			image = ImageIO.read(ImageSrc.OK_ICON);
		} catch (IOException e) {
		}
		final String nextStepStr = (String) ResourceUtil.get(1029);
		final JButton ok = new JButton(nextStepStr, new ImageIcon(image));
		final int[] typeDescs = {
				HPNode.TYPE_MENU_ITEM_CONTROLLER, 
				HPNode.TYPE_MENU_ITEM_CMD, 
				HPNode.TYPE_MENU_ITEM_SCREEN};
		String[] icons = {"controller_22.png", "cmd_22.png", "screen_22.png"};
		final String desc = "<html>" +
				"<STRONG>CONTROLLER</STRONG> : simulate a controller of smart device on mobile.<BR><BR>" +
				"<STRONG>CMD</STRONG> : such as exit/back, enter config form, run JRuby script on server.<BR><BR>" +
				"<STRONG>SCREEN</STRONG> : display remote desktop, or a Mlet which contain many swing components." +
				"</html>";
		
		final ItemListener itemListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() != ItemEvent.SELECTED){
					return;
				}
				
//				System.out.println("itemStateChanged" + System.currentTimeMillis());

				ok.setEnabled(true);
				final JRadioButton jop = (JRadioButton) e.getSource();
				for (int i = 0; i < rbs.length; i++) {
					if (rbs[i] == jop) {
						type = typeDescs[i];
						break;
					}
				}
				if(type == HPNode.TYPE_MENU_ITEM_CONTROLLER){
					final String ok_text = (String)ResourceUtil.get(IContext.OK);
					if(ok.getText().equals(ok_text) == false) {
						ok.setText(ok_text);
					}
				}else{
					if(ok.getText().equals(nextStepStr) == false){
						ok.setText(nextStepStr);
					}
				}
			}
		};
		ok.setEnabled(false);
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(type == HPNode.TYPE_MENU_ITEM_CONTROLLER){
					final String url = buildDefaultTypeURL(HCURL.CONTROLLER_PROTOCAL) + MenuManager.getNextNodeIdx();
					HCURL hcurl = HCURLUtil.extract(url);
					wizardEnd = buildItem(HPNode.TYPE_MENU_ITEM_CONTROLLER, hcurl);
					dialog.dispose();
				}else{
					//进入下一步
//					dialog.setVisible(false);
					ItemListener il = new ItemListener() {
						@Override
						public void itemStateChanged(ItemEvent e) {
							if(e.getStateChange() == ItemEvent.SELECTED){
								String text = ((JRadioButton)e.getSource()).getText();
								if(text.equals(DESKTOP)){
									text = HCURL.REMOTE_HOME_SCREEN;
								}else if(text.equals(HCURL.DATA_CMD_EXIT) 
										|| text.equals(HCURL.DATA_CMD_CONFIG)
										){
									
								}else{
									text += MenuManager.getNextNodeIdx();
								}
								final String url = HCURL.buildStandardURL(BaseMenuItemNodeEditPanel.getProtocal(type), text);
								HCURL hcurl = HCURLUtil.extract(url);
								wizardEnd = buildItem(type, hcurl);
								HCURLUtil.hcurlCacher.cycle(hcurl);
							}
						}
					};
					if(type == HPNode.TYPE_MENU_ITEM_CMD){
						//exit = "exit current menu and return to parent menu or exit");
						//config = "enter mobile config panel in mobile side when current item is clicked.");
						//cmd - My Command "do some response biz in server side when current item is clicked from mobile side");
						String[] subItems = {"my-command", HCURL.DATA_CMD_EXIT, HCURL.DATA_CMD_CONFIG};
						String[] desc = {
								"run a JRuby script in server side when current item is clicked from mobile side",
								"exit current menu and return to parent menu or exit",
								"enter mobile config panel in mobile side when current item is clicked."
								};
						selectSub(owner, ok, subItems, desc, il);
					}else if(type == HPNode.TYPE_MENU_ITEM_SCREEN){
						//screen -Desktop "enter disktop screen of PC in mobile side when current item is clicked.");
						//screen -Mlet "<html>Mlet Screen is a panel in mobile side for dispaly and controlling status of PC side, <BR>which is instance from java class and JRuby in PC side.</html> ");
						String[] subItems = {DESKTOP, "mlet"};
						String[] desc = {
								"enter and control disktop screen of PC.",
								"a panel in mobile side for dispaly and controlling status of PC or smart devices, " +
								"<BR>which is instance of class 'hc.server.ui.Mlet' and runing in PC side."
								};
						selectSub(owner, ok, subItems, desc, il);
					}
					dialog.dispose();
				}
			}
		});
		try {
			image = ImageIO.read(ImageSrc.CANCEL_ICON);
		} catch (IOException e) {
		}		
		final JButton cancel = new JButton((String) ResourceUtil.get(1018), new ImageIcon(image));
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				wizardEnd = null;
			}
		});
		for (int i = 0; i < rbs.length; i++) {
			final int radioIdx = i;
			rbs[i] = new JRadioButton();
			dispButton[i] = new JLabel(HPMenuItem.getTypeDesc(typeDescs[i]), Designer.loadImg(icons[i]), SwingConstants.LEADING);
			dispButton[i].addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {
				}
				
				@Override
				public void mousePressed(MouseEvent e) {
				}
				
				@Override
				public void mouseExited(MouseEvent e) {
				}
				
				@Override
				public void mouseEntered(MouseEvent e) {
				}
				
				@Override
				public void mouseClicked(MouseEvent e) {
					rbs[radioIdx].setSelected(true);
				}
			});
			rbs[i].addItemListener(itemListener);
			buttonGroup.add(rbs[i]);
			{
				JPanel subPanel = new JPanel(new BorderLayout());
				subPanel.add(rbs[i], BorderLayout.WEST);
				subPanel.add(dispButton[i], BorderLayout.CENTER);
				panel.add(subPanel);
			}
		}
		rbs[0].setSelected(true);
		
		Container container = dialog.getContentPane();
		container.setLayout(new GridBagLayout());

		
		Insets insets = new Insets(5, 5, 5, 5);
		{
			JPanel _panel = new JPanel(new BorderLayout());
			_panel.add(panel, BorderLayout.NORTH);
			{
				JPanel _subPanel = new JPanel(new BorderLayout());
				_subPanel.add(new JLabel("Description:"), BorderLayout.NORTH);
				JPanel _desc = new JPanel(new BorderLayout());
				_desc.add(new JLabel(desc), BorderLayout.CENTER);
				_desc.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				_subPanel.add(_desc, BorderLayout.CENTER);
				
				_panel.add(_subPanel, BorderLayout.CENTER);
			}
			container.add(_panel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0,
				0));
		}
		{	
			JPanel separatorPane = new JPanel();
			separatorPane.setLayout(new BoxLayout(separatorPane, BoxLayout.PAGE_AXIS));
			final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
			separatorPane.add(separator);
			container.add(separatorPane, new GridBagConstraints(0, 1, 1, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				insets, 0, 0));
		}
		{
			JPanel subPanel = new JPanel();
			subPanel.setLayout(new GridLayout(1, 2, 5, 5));
			subPanel.add(ok);
			subPanel.add(cancel);
			container.add(subPanel, new GridBagConstraints(0, 2, 1, 1, 0.0,
					0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
					insets, 0, 0));			
		}
		
		dialog.getRootPane().registerKeyboardAction(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						dialog.dispose();
						wizardEnd = null;
					}
				},
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
				wizardEnd = null;
			}
		});
		dialog.pack();
		
		dialog.setLocationRelativeTo(relativeTo);
		dialog.applyComponentOrientation(ComponentOrientation
				.getOrientation(UILang.getUsedLocale()));
		dialog.setVisible(true);
		
		if(type == 0){
			wizardEnd = null;
		}
//		System.out.println("User select type : " + ((wizardEnd==null)?"null":wizardEnd));
		return wizardEnd;
	}
	
	private static HPMenuItem buildItem(final int type, final HCURL hcurl){
		final HPMenuItem menuItem = new HPMenuItem(type,
				"new node " + MenuManager.getNextNodeIdx());
		menuItem.url = hcurl.url;
		menuItem.listener = ScriptModelManager.buildDefaultScript(type, hcurl);
		return menuItem;
	}
	
	private static void selectSub(JFrame owner, Component relativeObj, String[] items, String[] desc, 
			final ItemListener itemListener){
		final JDialog dialog = new JDialog(owner, "Choose Sub Type", true);
		final ButtonGroup buttonGroup = new ButtonGroup();
		final JPanel panel = new JPanel(new GridLayout(1, items.length));
		panel.setBorder(new TitledBorder("Sub Type:"));
		final JRadioButton[] rbs = new JRadioButton[items.length];
		BufferedImage image = null;
		try {
			image = ImageIO.read(ImageSrc.OK_ICON);
		} catch (IOException e) {
		}
		final JButton ok = new JButton((String) ResourceUtil.get(IContext.OK), new ImageIcon(image));
		ActionListener actionListen = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		};
		ok.addActionListener(actionListen);
		
		try {
			image = ImageIO.read(ImageSrc.CANCEL_ICON);
		} catch (IOException e) {
		}		
		final JButton cancel = new JButton((String) ResourceUtil.get(1018), new ImageIcon(image));
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				type = 0;
			}
		});
		String descs = "<html>";
		for (int i = 0; i < items.length; i++) {
			if(i > 0){
				descs += "<BR><BR>";
			}
			descs += "<STRONG>" + items[i] + "</STRONG> : " + desc[i];
			rbs[i] = new JRadioButton(items[i]);
			rbs[i].addItemListener(itemListener);
			buttonGroup.add(rbs[i]);
			panel.add(rbs[i]);
		};
		rbs[0].setSelected(true);
		descs += "</html>";
		
		Container container = dialog.getContentPane();
		container.setLayout(new GridBagLayout());

		
		Insets insets = new Insets(5, 5, 5, 5);
		{
			JPanel _panel = new JPanel(new BorderLayout());
			_panel.add(panel, BorderLayout.NORTH);
			{
				JPanel _subPanel = new JPanel(new BorderLayout());
				_subPanel.add(new JLabel("Description:"), BorderLayout.NORTH);
				JPanel _desc = new JPanel(new BorderLayout());
				_desc.add(new JLabel(descs), BorderLayout.CENTER);
				_desc.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				_subPanel.add(_desc, BorderLayout.CENTER);
				
				_panel.add(_subPanel, BorderLayout.CENTER);
			}

			container.add(_panel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0,
				0));
		}
		{	
			JPanel separatorPane = new JPanel();
			separatorPane.setLayout(new BoxLayout(separatorPane, BoxLayout.PAGE_AXIS));
			final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
			separatorPane.add(separator);
			container.add(separatorPane, new GridBagConstraints(0, 1, 1, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				insets, 0, 0));
		}
		{
			JPanel subPanel = new JPanel();
			subPanel.setLayout(new GridLayout(1, 2, 5, 5));
			subPanel.add(ok);
			subPanel.add(cancel);
			container.add(subPanel, new GridBagConstraints(0, 2, 1, 1, 0.0,
					0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
					insets, 0, 0));			
		}
		
		dialog.getRootPane().registerKeyboardAction(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						dialog.dispose();
						type = 0;
					}
				},
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
				type = 0;
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(relativeObj);
		dialog.setVisible(true);
		dialog.toFront();
	}
	
	private static String buildDefaultTypeURL(final String protocal){
		if(protocal.equals(HCURL.CONTROLLER_PROTOCAL)){
			return HCURL.buildStandardURL(protocal, "myctrl");
		}
		return null;
	}
	
//	public static void main(String[] args) {
//		chooseWizard(null);
//	}
}
