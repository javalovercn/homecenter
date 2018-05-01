package hc.server.util;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import hc.core.L;
import hc.core.util.LogManager;

public abstract class DownlistButton extends JButton {
	private final Vector<ListAction> actionList = new Vector<ListAction>();
	private Action defaultAction;

	private static final String downArrow = " ▼";// ◥◤▼↓

	public DownlistButton(final String text) {
		this(text, null);
	}

	public DownlistButton(final String text, final Icon icon) {
		super(text, icon);
		addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
				processClick(e);
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
			public void mouseClicked(final MouseEvent e) {// mouse drag时，不触发此事件
			}

			final void processClick(final MouseEvent e) {
				final DownlistButton self = DownlistButton.this;
				if (self.isEnabled()) {
					final int actionButtonWidth = self.getWidth();

					final Point p = e.getPoint();
					Vector<ListAction> ipList = null;
					if (p.x > (actionButtonWidth - actionButtonWidth / 5) && (ipList = self.getList()).size() > 0) {
						final JPopupMenu popMenu = new JPopupMenu();
						for (int i = 0; i < ipList.size(); i++) {
							final ListAction ip = ipList.get(i);
							final JMenuItem item = new JMenuItem(ip.getDisplayName());
							item.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(final ActionEvent e) {
									self.listActionPerformed(ip);
								}
							});
							popMenu.add(item);
						}
						popMenu.show(self, 0, self.getHeight());
					} else {
						defaultAction.actionPerformed(null);
					}
				} else {
					L.V = L.WShop ? false : LogManager.log("activate button is disable");
				}
			}
		});
	}

	public final void setDefaultAction(final Action defaultAction) {
		this.defaultAction = defaultAction;
	}

	public final void reset() {
		removeDownArrow();
		synchronized (actionList) {
			actionList.removeAllElements();
		}
	}

	public final void removeDownArrow() {
		synchronized (actionList) {
			final String btnText = getText();
			final int downIdx = btnText.indexOf(downArrow);
			if (downIdx > 0) {
				setText(btnText.substring(0, downIdx));
			}
		}
	}

	private final void addDownArrow() {
		synchronized (actionList) {
			final String btnText = getText();
			final int downIdx = btnText.indexOf(downArrow);
			if (downIdx < 0) {
				setText(btnText + downArrow);
			}
		}
	}

	public final void removeListAction(final ListAction item) {
		synchronized (actionList) {
			actionList.remove(item);
			if (actionList.size() == 0) {
				removeDownArrow();
			}
		}
	}

	public final void addListAction(final ListAction item) {
		synchronized (actionList) {
			if(actionList.contains(item)) {
				return;
			}
			
			actionList.add(item);
			if (actionList.size() == 1) {
				addDownArrow();
			}
		}
	}

	public final Vector<ListAction> getList() {
		synchronized (actionList) {
			final Vector<ListAction> out = new Vector<ListAction>();
			out.addAll(actionList);
			return out;
		}
	}

	public abstract void listActionPerformed(final ListAction action);
}
