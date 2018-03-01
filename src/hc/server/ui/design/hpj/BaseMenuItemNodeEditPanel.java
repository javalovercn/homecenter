package hc.server.ui.design.hpj;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.MutableTreeNode;

import hc.App;
import hc.core.ContextManager;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.ThreadPriorityManager;
import hc.core.util.UIUtil;
import hc.res.ImageSrc;
import hc.server.FileSelector;
import hc.server.HCActionListener;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.Designer;
import hc.server.ui.design.I18nTitlesEditor;
import hc.server.ui.design.code.TabHelper;
import hc.server.ui.design.engine.RubyExector;
import hc.util.ResourceUtil;

public abstract class BaseMenuItemNodeEditPanel extends ScriptEditPanel {
	public static final String I18N_BTN_TEXT = "Internationalize...";
	final String displayName = "Display Name";
	private final JPanel localnamePanel = new JPanel();
	protected HCURL hcurl;
	protected JFormattedTextField targetLocator = new JFormattedTextField() {
		@Override
		public void paste() {
			super.paste();
			notifyModifyTargetLocator();
		}
	};
	protected JLabel errCommandTip = new JLabel();
	private final JLabel iconLabel = new JLabel();
	protected final JButton browIconBtn = new JButton(
			"Icon [" + UIUtil.ICON_MAX + " X " + UIUtil.ICON_MAX + "]...");
	protected final ImageIcon sys_icon = Designer
			.loadImg("hc_" + UIUtil.ICON_DESIGN_SHOW_SIZE + ".png");
	final JPanel jtascriptPanel = new JPanel();
	final JLabel targetLoca = new JLabel(HPMenuItem.TARGET_LOCATOR + " :");
	final JPanel cmd_url_panel = new JPanel();
	final JPanel iconPanel = new JPanel();

	private static final int[] URL_PROTOCAL_CODE = { HPNode.TYPE_MENU_ITEM_CMD,
			HPNode.TYPE_MENU_ITEM_SCREEN, HPNode.TYPE_MENU_ITEM_CONTROLLER,
			HPNode.TYPE_MENU_ITEM_FORM, HPNode.TYPE_MENU_ITEM_CFG, HPNode.TYPE_MENU_ITEM_SUB_MENU };

	public abstract void addTargetURLPanel();

	private final void setItemIcon(final ImageIcon icon, final int size) {
		iconLabel.setIcon(icon);
		if (size > 0) {
			iconLabel.setToolTipText(String.valueOf(size) + " X " + String.valueOf(size));
		} else {
			iconLabel.setToolTipText("");
		}
	}

	public boolean verify(final boolean refresh) {
		final String text = targetLocator.getText();
		if (text.length() < 1) {
			errCommandTip.setVisible(true);
			errCommandTip.setText("error empty");
			return false;
		}
		if (text.startsWith("_")) {
			errCommandTip.setVisible(true);
			errCommandTip.setText("start with char '_'");
			return false;
		}
		if (text.indexOf("/") >= 0) {
			errCommandTip.setVisible(true);
			errCommandTip.setText("invalid char /");
			return false;
		}
		if (text.indexOf("\\") >= 0) {
			errCommandTip.setVisible(true);
			errCommandTip.setText("invalid char \\");
			return false;
		}
		errCommandTip.setVisible(false);
		((HPMenuItem) currItem).url = hcurl.protocal + HCURL.HTTP_SPLITTER + text;
		try {
			hcurl = HCURLUtil.extract(((HPMenuItem) currItem).url);
		} catch (final Exception e) {

		}
		if (refresh) {
			//// if(searchSameElementID(tree, currItem.type, hcurl.elementID) >
			//// 1){
			// errCommandTip.setVisible(true);
			// errCommandTip.setText("'" + hcurl.elementID + "' is used by
			//// item");
			// return false;
			// }
			App.invokeLaterUI(updateTreeRunnable);
		}
		notifyModified(true);
		return true;
	}

	public static String getProtocal(final int code) {
		for (int i = 0; i < URL_PROTOCAL_CODE.length; i++) {
			if (code == URL_PROTOCAL_CODE[i]) {
				return HCURL.URL_PROTOCAL[i];
			}
		}
		return "unkown";
	}

	public BaseMenuItemNodeEditPanel() {
		super();

		errCommandTip.setVisible(false);
		errCommandTip.setForeground(Color.RED);

		targetLoca.setToolTipText(
				"<html>it is used for API to go to / open this item from other item." + "<BR><BR>"
						+ "see API <STRONG>ProjectContext.goWhenInSession</STRONG> for more.</html>");
		targetLocator.setColumns(20);
		targetLocator.setFocusLostBehavior(JFormattedTextField.COMMIT);
		targetLocator.getDocument().addDocumentListener(new DocumentListener() {
			private void modify() {
				verify(true);
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
		targetLocator.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {// 输入字符在途
					@Override
					public void run() {
						notifyModifyTargetLocator();
					}
				});
			}

			@Override
			public void keyReleased(final KeyEvent e) {
			}

			@Override
			public void keyPressed(final KeyEvent e) {
			}
		});

		jtascriptPanel.setBorder(new TitledBorder(JRubyNodeEditPanel.JRUBY_SCRIPT_BORDER));
		jtascriptPanel.setLayout(new BorderLayout());
		jtascriptPanel.add(editorPane, BorderLayout.CENTER);
		jtascriptPanel.add(errRunInfo, BorderLayout.SOUTH);

		iconPanel.setLayout(new BorderLayout());
		{
			final GridBagLayout gridbag = new GridBagLayout();
			final JPanel iPanel = new JPanel(gridbag);
			final GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.weightx = 0.0;
			c.weighty = 0.0;
			c.insets = new Insets(0, 10, 0, 0);
			c.fill = GridBagConstraints.NONE;

			gridbag.setConstraints(iconLabel, c);

			setItemIcon(sys_icon, 0);
			iPanel.add(iconLabel);
			iconLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

			iconLabel.addMouseListener(new MouseListener() {

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
					if (sys_icon != iconLabel.getIcon() && e.getClickCount() == 2) {
						final String base64 = ((HPMenuItem) currItem).imageData;
						if (UIUtil.SYS_DEFAULT_ICON.equals(base64)) {
							return;
						}

						ContextManager.getThreadPool().run(new Runnable() {
							@Override
							public void run() {
								File file = FileSelector.selectImageFile(iconLabel,
										FileSelector.PNG_FILTER, false);
								if (file == null) {
									return;
								}
								final String extPNG = ".png";
								if (file.toString().endsWith(extPNG)) {
								} else {
									file = new File(file.getPath() + extPNG);
								}

								try {
									if (ImageIO.write(ServerUIUtil.base64ToBufferedImage(base64),
											"png", file)) {
										App.showMessageDialog(designer,
												"Successful save icon to file!",
												ResourceUtil.getInfoI18N(),
												JOptionPane.INFORMATION_MESSAGE,
												App.getSysIcon(App.SYS_INFO_ICON));
										return;
									}
								} catch (final Throwable ex) {
								}
								App.showMessageDialog(designer, "fail to write image file!",
										ResourceUtil.getErrorI18N(), JOptionPane.ERROR_MESSAGE,
										App.getSysIcon(App.SYS_ERROR_ICON));
							}
						}, threadPoolToken);
					}
				}
			});

			iconPanel.add(iPanel, BorderLayout.WEST);
		}

		localnamePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		nameField.setColumns(10);
		localnamePanel
				.add(ResourceUtil.buildFixedWidthPanel(new JLabel(displayName + " :"), nameField));
		browIconBtn.setIcon(Designer.loadImg("hc_16.png"));
		browIconBtn.setToolTipText("<html>" + "change menu item icon." + "<BR>"
				+ "all icons of items are recommended to be same size, if they are 64, please choose 64 X 64.</html>");
		browIconBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				BufferedImage bi;
				try {
					final File selectImageFile = FileSelector.selectImageFile(browIconBtn,
							FileSelector.IMAGE_FILTER, true);
					if (selectImageFile == null) {
						return;
					}
					bi = ImageIO.read(selectImageFile);
				} catch (final IOException e2) {
					App.showMessageDialog(tree, e2.toString(), "Error select resource!",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (bi != null) {
					bi = ResourceUtil.standardMenuIconForAllPlatform(bi, UIUtil.ICON_MAX, false);

					final String strImageData = ServerUIUtil.imageToBase64(bi, iconBsArrayos);
					if (strImageData == null) {
						return;
					}
					((HPMenuItem) currItem).imageData = strImageData;
					notifyModified(true);

					setItemIcon64(bi);
				}
			}
		}, threadPoolToken));
		localnamePanel.add(browIconBtn);

		{
			final JButton i18nBtn = new JButton(I18N_BTN_TEXT);
			i18nBtn.setIcon(new ImageIcon(ImageSrc.loadImageFromPath("hc/res/global_16.png")));
			i18nBtn.setToolTipText(buildI18nButtonTip(displayName));
			i18nBtn.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					I18nTitlesEditor.showEditor(currItem.i18nMap, new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							if (currItem.i18nMap.isModified()) {
								currItem.i18nMap.clearModifyTag();
								notifyModified(true);
							}
						}
					}, i18nBtn, designer);

				}
			}, threadPoolToken));
			localnamePanel.add(i18nBtn);
		}

		{
			final JPanel iconTotal = new JPanel(new BorderLayout());
			iconTotal.add(localnamePanel, BorderLayout.CENTER);
			iconTotal.add(cmd_url_panel, BorderLayout.SOUTH);

			iconPanel.add(iconTotal, BorderLayout.CENTER);
		}
	}

	public static String buildI18nButtonTip(final String displayName) {
		return "<html>input names for international. "
				+ "<BR>if name for locale is not found, then <STRONG>" + displayName
				+ "</STRONG> as default.</html>";
	}

	@Override
	Map<String, String> buildMapScriptParameter() {
		return RubyExector.toMap(hcurl);
	}

	@Override
	public void init(final MutableTreeNode data, final JTree tree) {
		super.init(data, tree);

		hcurl = HCURLUtil.extract(((HPMenuItem) currItem).url, false);

		currItem.type = (HCURL.getURLProtocalIdx(hcurl.protocal) + HPNode.TYPE_MENU_ITEM_CMD);

		if (((HPMenuItem) currItem).imageData.equals(UIUtil.SYS_DEFAULT_ICON)) {
			setItemIcon(sys_icon, 0);
		} else {
			setItemIcon64(ServerUIUtil.base64ToBufferedImage(((HPMenuItem) currItem).imageData));
		}

		extInit();

		try {
			Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);
		} catch (final Exception e) {
		}

		this.isInited = true;

	}

	protected void initScript() {
		final int pnum = hcurl.getParaSize();
		String pv = "";
		for (int i = 0; i < pnum; i++) {
			if (pv.length() > 0) {
				pv += "&";
			}
			final String key = hcurl.getParaAtIdx(i);
			pv += key + "=" + hcurl.getValueofPara(key);
		}
		targetLocator.setText(hcurl.elementID + ((pv.length() > 0) ? ("?" + pv) : ""));
		final String listener = ((HPMenuItem) currItem).listener;

		TabHelper.initScriptPanel(jtaScript, this, listener);
	}

	@Override
	public void updateScript(final String script) {
		((HPMenuItem) currItem).listener = script;
	}

	private final void setItemIcon64(BufferedImage oriImage) {
		final int oriWidth = oriImage.getWidth();
		oriImage = ResourceUtil.standardMenuIconForAllPlatform(oriImage,
				UIUtil.ICON_DESIGN_SHOW_SIZE, true);
		setItemIcon(new ImageIcon(oriImage), oriWidth);
	}

	private final void notifyModifyTargetLocator() {
		verify(true);

		final int type = currItem.type;

		if (isTypeFromTargetInput(type)) {
			replaceClassName(targetLocator.getText(), targetLocator);
			return;// 在target框上
		}
	}

}