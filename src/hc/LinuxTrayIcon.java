package hc;

import hc.res.ImageSrc;
import hc.server.ui.tip.TipPop;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.TransparentFrame;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;

public class LinuxTrayIcon extends TransparentFrame implements ITrayIcon {
	Image image;
	static final int MAX_IMG_SIZE = 32;
	boolean autoResize = true;
	ActionListener defaultAction;
	MouseListener iconMouseListener;
	boolean isDraged = false;
	JComponent tipComponent;
	public final Point origin;

	public LinuxTrayIcon(Image image) {
		super(buildHCTrayIcon(image));

		setAlwaysOnTop(true);
		this.origin = new Point();

		if (PropertiesManager.getValue(PropertiesManager.p_TrayX) == null) {
			Dimension d = ResourceUtil.getScreenSize();
			setLocation(d.width - 4 * image.getWidth(null), 4 * image.getHeight(null));
		} else {
			setLocation(Integer.parseInt(PropertiesManager.getValue(PropertiesManager.p_TrayX)),
					Integer.parseInt(PropertiesManager.getValue(PropertiesManager.p_TrayY)));
		}

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				origin.x = e.getX();
				origin.y = e.getY();

				TipPop.close();

				if (iconMouseListener != null) {
					iconMouseListener.mousePressed(e);
				}
			}

			// 窗体上单击鼠标右键关闭程序
			public void mouseClicked(MouseEvent e) {
				if (defaultAction != null) {
					if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
						defaultAction.actionPerformed(null);
						return;
					}
				}
				if (iconMouseListener != null) {
					iconMouseListener.mouseClicked(e);
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (isDraged) {
					saveLocation();
				}
				isDraged = false;
				// LogManager.log("mouseReleased , x : " + e.getXOnScreen() + ",
				// y : " + e.getYOnScreen());
				if (iconMouseListener != null) {
					iconMouseListener.mouseReleased(e);
				}
				super.mouseReleased(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// ToolTipManager.sharedInstance().mouseEntered(e);
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// if(iconMouseListener != null){
				// iconMouseListener.mouseExited(e);
				// }
				ToolTipManager.sharedInstance().mouseExited(e);
			}
		});

		this.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				isDraged = true;
				final Point p = getLocation();
				setLocation(p.x + e.getX() - origin.x, p.y + e.getY() - origin.y);
			}
		});
	}

	@Override
	public void setToolTip(String tooltip) {
		// for (JComponent component :
		// SwingUtils.getDescendantsOfType(JComponent.class,
		// this)) {
		// if (component.getClass().getName().contains("MetalTitlePane")) {
		// tipComponent = component;
		//
		// break;
		// }
		// }
		// this.getLayeredPane().setToolTipText(tooltip);
	}

	@Override
	public void setImage(Image image) {
		if (this.image == image) {
			return;
		}
		this.image = image;
		Image iconImage = buildHCTrayIcon(image);
		super.refresh(iconImage);
	}

	private static Image buildHCTrayIcon(Image image) {
		Image iconImage = image;
		if ((image.getWidth(null) > MAX_IMG_SIZE)) {
			iconImage = ResourceUtil.resizeImage(ResourceUtil.toBufferedImage(image), MAX_IMG_SIZE,
					MAX_IMG_SIZE);
		}
		iconImage = ImageSrc.makeRoundedCorner(ImageSrc.toBufferedImage(iconImage), 16);
		return iconImage;
	}

	private void saveLocation() {
		Point location = this.getLocation();
		PropertiesManager.setValue(PropertiesManager.p_TrayX, String.valueOf(location.x));
		PropertiesManager.setValue(PropertiesManager.p_TrayY, String.valueOf(location.y));
		PropertiesManager.saveFile();
	}

	@Override
	public Image getImage() {
		return image;
	}

	@Override
	public void setImageAutoSize(boolean autosize) {
		this.autoResize = autosize;
	}

	@Override
	public void removeTrayMouseListener(MouseListener listener) {
		iconMouseListener = null;
	}

	@Override
	public void addTrayMouseListener(MouseListener listener) {
		iconMouseListener = listener;
	}

	@Override
	public void removeTray() {
		this.dispose();
		TipPop.exit();
	}

	@Override
	public void setDefaultActionListener(ActionListener listen) {
		this.defaultAction = listen;
	}

	@Override
	public void displayMessage(String caption, String text, MessageType messageType) {
		final Point location = this.getLocation();
		TipPop.setCornerPosition(location.x, location.y, image.getWidth(null),
				image.getHeight(null));
		TipPop.displayMessage(caption, text, messageType);
	}

	@Override
	public void showTray() {
		this.setVisible(true);
	}
}