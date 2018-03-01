package hc.server.ui.tip;

import hc.App;
import hc.core.util.ExceptionReporter;
import hc.res.ImageSrc;
import hc.server.PlatformManager;
import hc.server.ui.tip.TipPop.CornerPosition;
import hc.server.util.HCJFrame;
import hc.util.ResourceUtil;
import hc.util.UILang;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MessageTipFrame extends HCJFrame {// LinuxTrayIcon下需要改为HCJFrame，否则HCJDialog会导致UI阻塞。
	static JLabel tipBorderLeftTop, tipBorderRightTop, tipBorderLeftDown, tipBorderRightDown;
	static Dimension screenSize;
	static Icon errImg, warnImg, infoImg;

	final JLabel msgLabel;
	final JLabel capLabel;
	final JLabel msgType;
	boolean isNextMode = false;
	final JLabel footLeftTop, footLeftDown, footRightTop, footRightDown;
	int trayLocX, trayLocY, trayWidth, trayHeight;
	TipPop.CornerPosition position = CornerPosition.UN_KNOWN;
	TransparentPanel box;
	ActionListener al;

	public void setNextMode(final boolean mode, final ActionListener listener) {
		isNextMode = mode;
		al = listener;
	}

	final Runnable waitRun = new Runnable() {
		@Override
		public void run() {
			pack();
			calcLocation();
			TipPop.autoClose.resetTimerCount();
			TipPop.autoClose.setEnable(true);
			setVisible(true);
		}
	};

	public void setCornerPosition(final CornerPosition p, final int trayLocX, final int trayLocY,
			final int trayWidth, final int trayHeight) {
		this.trayLocX = trayLocX;
		this.trayLocY = trayLocY;
		this.trayWidth = trayWidth;
		this.trayHeight = trayWidth;

		if (this.position == p) {
			return;
		}

		this.position = p;

		box = new TransparentPanel();
		box.setLayout(new GridBagLayout());

		final Insets insets = new Insets(0, 0, 0, 0);
		final GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0.5, 0.5,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0);

		if (p == CornerPosition.LEFT_TOP || p == CornerPosition.RIGHT_TOP) {
			if (p == CornerPosition.LEFT_TOP) {
				c.anchor = GridBagConstraints.LINE_START;
				box.add(Box.createHorizontalStrut(TipPop.BORDER_WIDTH), c);

				c.gridx += 1;
				box.add(footLeftTop, c);
			} else {
				c.anchor = GridBagConstraints.LINE_END;
				c.gridx = 1;
				box.add(footRightTop, c);

				c.gridx = 2;
				box.add(Box.createHorizontalStrut(TipPop.BORDER_WIDTH), c);
			}
			// c.anchor = GridBagConstraints.FIRST_LINE_START;
			// c.fill = GridBagConstraints.NONE;
			// outerBox.add(footPanel, c);
			c.gridx = 0;
			c.gridy += 1;
			buildBoxArea(this, c);
		} else if (p == CornerPosition.LEFT_DOWN || p == CornerPosition.RIGHT_DOWN) {
			buildBoxArea(this, c);
			c.gridy += 1;
			c.gridx = 0;
			if (p == CornerPosition.LEFT_DOWN) {
				c.anchor = GridBagConstraints.LINE_START;
				box.add(Box.createHorizontalStrut(TipPop.BORDER_WIDTH), c);

				c.gridx += 1;
				box.add(footLeftDown, c);
			} else {
				c.anchor = GridBagConstraints.LINE_END;
				c.gridx = 1;
				box.add(footRightDown, c);

				c.gridx = 2;
				box.add(Box.createHorizontalStrut(TipPop.BORDER_WIDTH), c);
			}
		}
		setContentPane(box);
	}

	public void close() {
		TipPop.autoClose.setEnable(false);
		this.setVisible(false);
	}

	public MessageTipFrame() {
		setAlwaysOnTop(true);
		setUndecorated(true);// 去掉窗口的边框
		setFocusableWindowState(false);
		try {
			PlatformManager.getService().setWindowOpaque(this, false);// 透明
		} catch (final Throwable e) {
			setBackground(new Color(0, 0, 0, 0));
		}
		{
			BufferedImage RightDown = null;
			try {
				RightDown = ImageIO.read(ResourceUtil.getResource("hc/res/tip_foot.png"));
			} catch (final IOException e) {
			}
			final BufferedImage LeftTop = (RightDown != null)
					? ResourceUtil.rotateImage(RightDown, 180)
					: null;
			footLeftTop = new JLabel(new ImageIcon(LeftTop));
			footLeftTop.setOpaque(false);
			footRightTop = new JLabel(new ImageIcon(ResourceUtil.flipHorizontalJ2D(LeftTop)));
			footRightTop.setOpaque(false);
			footRightDown = new JLabel(new ImageIcon(RightDown));
			footRightDown.setOpaque(false);
			footLeftDown = new JLabel(new ImageIcon(ResourceUtil.flipHorizontalJ2D(RightDown)));
			footLeftDown.setOpaque(false);

		}
		capLabel = new JLabel();
		Font font = new Font(Font.DIALOG, Font.BOLD, 16);
		capLabel.setFont(font);
		capLabel.setBackground(Color.decode(TipPop.BACKGROUND_COLOR));
		capLabel.setForeground(Color.BLACK);
		capLabel.setOpaque(true);

		msgType = new JLabel();

		msgLabel = new JLabel();
		font = new Font(Font.DIALOG, Font.PLAIN, 16);
		msgLabel.setFont(font);
		msgLabel.setBackground(Color.decode(TipPop.BACKGROUND_COLOR));
		msgLabel.setForeground(Color.BLACK);
		msgLabel.setOpaque(true);

	}

	static {
		buildImage();
	}

	private static void buildImage() {
		try {
			BufferedImage LeftTop, RightTop, LeftDown, RightDown;

			LeftTop = ImageIO.read(ResourceUtil.getResource("hc/res/tip_border_6.png"));
			RightTop = ResourceUtil.rotateImage(LeftTop, 90);
			RightDown = ResourceUtil.rotateImage(RightTop, 90);
			LeftDown = ResourceUtil.rotateImage(RightDown, 90);

			tipBorderLeftTop = new JLabel(new ImageIcon(LeftTop));
			tipBorderLeftTop.setOpaque(false);
			tipBorderRightTop = new JLabel(new ImageIcon(RightTop));
			tipBorderRightTop.setOpaque(false);
			tipBorderRightDown = new JLabel(new ImageIcon(RightDown));
			tipBorderRightDown.setOpaque(false);
			tipBorderLeftDown = new JLabel(new ImageIcon(LeftDown));
			tipBorderLeftDown.setOpaque(false);

		} catch (final IOException e) {
		}

		try {
			Icon icon = App.getSysIcon(App.SYS_INFO_ICON);
			infoImg = new ImageIcon(ResourceUtil.resizeImage(ImageSrc.iconToImage(icon), 22, 22));

			icon = App.getSysIcon(App.SYS_WARN_ICON);
			warnImg = new ImageIcon(ResourceUtil.resizeImage(ImageSrc.iconToImage(icon), 22, 22));

			icon = App.getSysIcon(App.SYS_ERROR_ICON);
			errImg = new ImageIcon(ResourceUtil.resizeImage(ImageSrc.iconToImage(icon), 22, 22));
		} catch (final Exception e) {
		}

		screenSize = ResourceUtil.getScreenSize();
	}

	private void buildBoxArea(final MessageTipFrame self, final GridBagConstraints c) {
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.NONE;
		box.add(tipBorderLeftTop, c);

		c.gridx += 1;
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		box.add(new BorderPanel(this, 0, 0, TipPop.BORDER_WIDTH, 0), c);

		c.gridx += 1;
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		box.add(tipBorderRightTop, c);

		{
			// MessageType + Caption + close Icon

			c.gridx = 0;
			c.gridy += 1;
			c.anchor = GridBagConstraints.LINE_START;
			c.fill = GridBagConstraints.VERTICAL;
			box.add(new BorderPanel(this, 0, 0, 0, TipPop.BORDER_WIDTH), c);

			c.gridx += 1;
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.HORIZONTAL;

			{

				final JPanel titlePanel = new JPanel(new BorderLayout());
				titlePanel.setBackground(Color.decode(TipPop.BACKGROUND_COLOR));
				titlePanel.setOpaque(true);

				titlePanel.add(msgType, BorderLayout.LINE_START);

				titlePanel.add(capLabel, BorderLayout.CENTER);

				try {
					JLabel closeLabel;
					if (isNextMode) {
						closeLabel = new JLabel(new ImageIcon(
								ImageIO.read(ResourceUtil.getResource("hc/res/tip_next.png"))));
					} else {
						closeLabel = new JLabel(new ImageIcon(
								ImageIO.read(ResourceUtil.getResource("hc/res/tip_close.png"))));
					}
					titlePanel.add(closeLabel, BorderLayout.LINE_END);
					closeLabel.addMouseListener(new MouseListener() {
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
							if (al != null) {
								al.actionPerformed(null);
							}
							self.close();
						}
					});
				} catch (final IOException e) {
					ExceptionReporter.printStackTrace(e);
				}

				titlePanel.applyComponentOrientation(
						ComponentOrientation.getOrientation(UILang.getUsedLocale()));
				box.add(titlePanel, c);//
			}

			c.gridx += 1;
			c.anchor = GridBagConstraints.LINE_END;
			c.fill = GridBagConstraints.VERTICAL;
			box.add(new BorderPanel(this, TipPop.BORDER_WIDTH, 0, TipPop.BORDER_WIDTH,
					TipPop.BORDER_WIDTH), c);
		}

		c.gridx = 0;
		c.gridy += 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.VERTICAL;
		box.add(new BorderPanel(this, 0, 0, 0, TipPop.BORDER_WIDTH), c);

		c.gridx += 1;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;

		{
			box.add(msgLabel, c);//
		}

		c.gridx += 1;
		c.anchor = GridBagConstraints.LINE_END;
		c.fill = GridBagConstraints.VERTICAL;
		box.add(new BorderPanel(this, TipPop.BORDER_WIDTH, 0, TipPop.BORDER_WIDTH,
				TipPop.BORDER_WIDTH), c);

		c.gridx = 0;
		c.gridy += 1;
		c.anchor = GridBagConstraints.LAST_LINE_START;
		c.fill = GridBagConstraints.NONE;
		box.add(tipBorderLeftDown, c);

		c.gridx += 1;
		c.anchor = GridBagConstraints.PAGE_END;
		c.fill = GridBagConstraints.HORIZONTAL;
		box.add(new BorderPanel(this, 0, TipPop.BORDER_WIDTH, TipPop.BORDER_WIDTH,
				TipPop.BORDER_WIDTH), c);

		c.gridx += 1;
		c.anchor = GridBagConstraints.LAST_LINE_END;
		c.fill = GridBagConstraints.NONE;
		box.add(tipBorderRightDown, c);
	}

	public void showMessage(final String caption, final String msg, final MessageType messageType) {
		msgLabel.setText(msg);
		capLabel.setText(caption);

		if (messageType == MessageType.ERROR) {
			msgType.setIcon(errImg);
		} else if (messageType == MessageType.INFO) {
			msgType.setIcon(infoImg);
		} else if (messageType == MessageType.WARNING) {
			msgType.setIcon(warnImg);
		} else {
			msgType.setIcon(null);
		}

		// setSize(10, 10);
		App.invokeLaterUI(waitRun);

	}

	private void calcLocation() {
		if (position == CornerPosition.LEFT_TOP) {
			setLocation(trayLocX + trayWidth - TipPop.BORDER_WIDTH, trayLocY + trayHeight);
		} else if (position == CornerPosition.RIGHT_TOP) {
			setLocation(trayLocX - getWidth() + TipPop.BORDER_WIDTH, trayLocY + trayHeight);
		} else if (position == CornerPosition.LEFT_DOWN) {
			setLocation(trayLocX + trayWidth - TipPop.BORDER_WIDTH, trayLocY - getHeight());
		} else if (position == CornerPosition.RIGHT_DOWN) {
			setLocation(trayLocX - getWidth() + TipPop.BORDER_WIDTH, trayLocY - getHeight());
		}
	}

	public static TipPop.CornerPosition converTo(final int locX, final int locY) {
		final boolean isRight = locX > screenSize.width / 2;
		final boolean isDown = locY > screenSize.height / 2;
		if (isRight) {
			if (isDown) {
				return TipPop.CornerPosition.RIGHT_DOWN;
			} else {
				return TipPop.CornerPosition.RIGHT_TOP;
			}
		} else {
			if (isDown) {
				return TipPop.CornerPosition.LEFT_DOWN;
			} else {
				return TipPop.CornerPosition.LEFT_TOP;
			}
		}
	}
}