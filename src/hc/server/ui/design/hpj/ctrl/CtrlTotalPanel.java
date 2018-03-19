package hc.server.ui.design.hpj.ctrl;

import hc.core.util.CNCtrlKey;
import hc.core.util.CtrlItem;
import hc.core.util.CtrlKeySet;
import hc.core.util.CtrlMap;
import hc.server.ui.design.Designer;
import hc.server.ui.design.hpj.BaseMenuItemNodeEditPanel;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class CtrlTotalPanel extends JPanel {
	// 可用图标列表区
	final JPanel panle_but_list = new JPanel(new GridLayout(0, 1));
	public final CtrlPanel panel_canvas;
	final JPanel scriptPanel;
	final BaseMenuItemNodeEditPanel baseMenuItemPanel;
	JSplitPane panelSubMRInfo;
	JPanel comp;
	public CtrlMap ctrlMap;
	private final CNCtrlKey ctrlKey = new CNCtrlKey();

	private static final Cursor defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

	public void loadMap(final CtrlMap ctrlMap) {
		this.ctrlMap = ctrlMap;
	}

	public CtrlTotalPanel(final JPanel sp, final BaseMenuItemNodeEditPanel base, final JRadioButton h_button, final JRadioButton v_button) {
		panel_canvas = new CtrlPanel(this);
		this.scriptPanel = sp;
		this.baseMenuItemPanel = base;

		final MouseListener ml = new MouseListener() {
			@Override
			public void mouseClicked(final MouseEvent e) {
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				if (((JButton) e.getSource()).isEnabled()) {
					final int keyValue = findKeyValue(e);
					final Toolkit toolkit = Toolkit.getDefaultToolkit();
					final BufferedImage img = ResourceUtil.unAlphaImage(cursor_images[keyValue]);
					final Cursor c = toolkit.createCustomCursor(img, new Point(img.getWidth() / 2, img.getHeight() / 2), "img");
					updateCursor(c);
				}
			}

			private int findKeyValue(final MouseEvent e) {
				final String keyDesc = ((JButton) e.getSource()).getText();
				int keyValue = 0;
				for (; keyValue < ctrlKey.desc.length; keyValue++) {
					final String tmp_key = ctrlKey.desc[keyValue];
					if (tmp_key != null && tmp_key.equals(keyDesc)) {
						break;
					}
				}
				return keyValue;
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				if (((JButton) e.getSource()).isEnabled()) {
					updateCursor(defaultCursor);
					final int x = e.getXOnScreen();
					final int y = e.getYOnScreen();
					final Point locationOnScreen = panel_canvas.getLocationOnScreen();
					if (x > locationOnScreen.x && y > locationOnScreen.y && x < locationOnScreen.x + panel_canvas.getWidth()
							&& y < locationOnScreen.y + panel_canvas.getHeight()) {
						final int keyValue = findKeyValue(e);

						final int center_x = x - locationOnScreen.x;
						final int center_y = y - locationOnScreen.y;
						final CtrlItem ci = addToCanvas(keyValue, center_x, center_y);
						panel_canvas.repaint();

						panel_canvas.requestFocus();
						ctrlMap.addButtonOnCanvas(keyValue, ci.center_x, ci.center_y);
						panel_canvas.setCurrSelectedItem(keyValue);
					}
				}
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
			}

			@Override
			public void mouseExited(final MouseEvent e) {
			}
		};
		{
			final int[] keys = ctrlKey.getDispKeys();
			final Toolkit tk = Toolkit.getDefaultToolkit();
			Dimension d = null;
			for (int i = 0; i < keys.length; i++) {
				final int keyValue = keys[i];
				final String pngName = ctrlKey.getPNGName(keyValue);
				final BufferedImage bufferedImage = Designer.loadBufferedImage(pngName + CNCtrlKey.PNG_EXT);
				if (d == null) {
					d = tk.getBestCursorSize(bufferedImage.getWidth(), bufferedImage.getHeight());
				}
				BufferedImage r_img;
				if (bufferedImage.getWidth() > bufferedImage.getHeight()) {
					r_img = ResourceUtil.resizeImage(bufferedImage, d.width,
							bufferedImage.getHeight() * d.width / bufferedImage.getWidth());
				} else {
					r_img = ResourceUtil.resizeImage(bufferedImage, bufferedImage.getWidth() * d.height / bufferedImage.getHeight(),
							d.height);
				}
				Graphics2D graphics2d;
				final BufferedImage bufferedImage2 = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
				graphics2d = bufferedImage2.createGraphics();
				final int y = (d.height - r_img.getHeight()) / 2;
				// graphics2d.setComposite(AlphaComposite.SrcOver);//g.setComposite(AlphaComposite.Src);
				graphics2d.drawImage(r_img, 0, y, r_img.getWidth(), r_img.getHeight(), null);
				graphics2d.dispose();
				cursor_images[keyValue] = bufferedImage2;

				final BufferedImage rImage = findRoundImage(pngName);
				if (rImage != null) {
					canvas_images[keyValue] = rImage;
				} else {
					canvas_images[keyValue] = bufferedImage;
				}
				final JButton jb = new JButton(ctrlKey.getKeyDesc(keyValue), new ImageIcon(cursor_images[keyValue]));
				jb.setHorizontalAlignment(SwingConstants.LEFT);
				jb.addMouseListener(ml);

				listButtons[keyValue] = jb;
				panle_but_list.add(jb);
			}
		}

		{
			this.setLayout(new BorderLayout());

			comp = new JPanel(new BorderLayout());
			comp.add(new JScrollPane(panle_but_list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
					BorderLayout.WEST);

			comp.setBorder(new TitledBorder(""));

			add(comp, BorderLayout.CENTER);
		}

	}

	private static BufferedImage findRoundImage(final String pngName) {
		return Designer.loadBufferedImage(pngName + CNCtrlKey.CORNET_EXT + CNCtrlKey.PNG_EXT);
	}

	public void buildSplitPanel(final int splitType) {
		if (panelSubMRInfo != null) {
			comp.remove(panelSubMRInfo);
		}

		panelSubMRInfo = new JSplitPane(splitType,
				new JScrollPane(panel_canvas, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
				scriptPanel);
		final String dviLoca = PropertiesManager.getValue(PropertiesManager.p_DesignerCtrlDividerLocation);
		if (dviLoca == null) {
			panelSubMRInfo.setDividerLocation(CtrlPanel.BLOCK_WIDTH);
		} else {
			panelSubMRInfo.setDividerLocation(Integer.parseInt(dviLoca));
		}
		comp.add(panelSubMRInfo, BorderLayout.CENTER);
		panelSubMRInfo.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
			@Override
			public void propertyChange(final java.beans.PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
					PropertiesManager.setValue(PropertiesManager.p_DesignerCtrlDividerLocation,
							String.valueOf(panelSubMRInfo.getDividerLocation()));
					PropertiesManager.saveFile();
				}
			}
		});
	}

	private CtrlItem addToCanvas(final int keyValue, final int x, final int y) {
		listButtons[keyValue].setEnabled(false);

		baseMenuItemPanel.notifyModified(true);

		return panel_canvas.addItemToCanvas(keyValue, x, y);
	}

	public void removeFromCanvas(final int keyValue) {
		listButtons[keyValue].setEnabled(true);

		panel_canvas.removeItemToCanvas(keyValue);

		baseMenuItemPanel.notifyModified(true);
	}

	private void repainCanvas() {
		panel_canvas.repaint();
	}

	public void repainCanvasInit() {
		panel_canvas.resetSelectItem();

		final int[] keys = ctrlMap.getButtonsOnCanvas();

		// panel_canvas.clearAll();

		final int[] keyValues = ctrlKey.getDispKeys();
		for (int k = 0; k < keyValues.length; k++) {
			boolean isDone = false;
			final int i = keyValues[k];
			for (int j = 0; j < keys.length; j++) {
				if (keys[j] == i) {
					addToCanvas(i, ctrlMap.getCenterXOfButton(i), ctrlMap.getCenterYOfButton(i));
					isDone = true;
					break;
				}
			}
			if (isDone == false) {
				removeFromCanvas(i);
			}
		}

		ctrlMap.setBlockWidth(CtrlPanel.BLOCK_WIDTH);

		repainCanvas();
	}

	private final void updateCursor(final Cursor c) {
		SwingUtilities.invokeLater(new Runnable() {// 否则后一个可能不执行不稳定
			@Override
			public void run() {
				Designer.getInstance().setCursor(c);
				panle_but_list.setCursor(c);
				panel_canvas.setCursor(c);// 必须，否则进入后无效
			}
		});
	}

	public final BufferedImage[] canvas_images = new BufferedImage[CtrlKeySet.MAX_CTRL_ITEM_NUM];
	private final BufferedImage[] cursor_images = new BufferedImage[CtrlKeySet.MAX_CTRL_ITEM_NUM];
	private final JButton[] listButtons = new JButton[CtrlKeySet.MAX_CTRL_ITEM_NUM];

}
