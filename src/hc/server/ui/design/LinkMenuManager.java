package hc.server.ui.design;

import java.awt.Component;
import java.awt.Window;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import hc.App;
import hc.core.ContextManager;
import hc.core.util.ExceptionReporter;
import hc.server.SingleJFrame;
import hc.server.ui.ClosableWindow;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.SimuMobile;
import hc.server.ui.design.code.J2SEDocHelper;

public class LinkMenuManager {
	static Window currLink;

	/**
	 * 重要，请勿在Event线程中调用，
	 * 
	 * @return
	 */
	public static boolean notifyCloseDesigner() {
		return Designer.notifyCloseDesigner();
		// try {
		// final Class c = getDesignerClass();
		// final Method m = c.getMethod("notifyCloseDesigner", new Class[] {});
		// return((Boolean)m.invoke(c, new Object[] {})).booleanValue();
		// } catch (final Throwable e) {
		// ExceptionReporter.printStackTrace(e);
		// App.showConfirmDialog(null, "load link panel error : " +
		// e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		// }
		// return false;
	}

	public static void showLinkPanel(final JFrame frame) {
		try {
			showLinkPanel(frame, true, null);
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
			App.showConfirmDialog(null, "load link panel error : " + e.toString(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public static synchronized void closeLinkPanel() {
		try {
			if (currLink == null) {
				return;
			}

			if (currLink instanceof ClosableWindow) {
				((ClosableWindow) currLink).notifyClose();
			} else {
				currLink.dispose();
			}
			currLink = null;
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
			App.showConfirmDialog(null, "load link panel error : " + e.toString(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * 重要，请勿在Event线程中调用，
	 * 
	 * @param loadInit
	 */
	public static void startDesigner(final boolean loadInit) {
		SimuMobile.init();

		if (LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_DESIGN)) {
			if (J2SEDocHelper.isBuildIn() == false) {
				if (J2SEDocHelper.isJ2SEDocReady() == false) {
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							J2SEDocHelper.checkJ2SEDocAndDownload();
						}
					});
				}
			}

			try {
				SingleJFrame.showJFrame(Designer.class);
				Designer.getInstance().loadInitProject(loadInit);
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						App.showConfirmDialog(null, "Cant load Designer", "Error",
								JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
					}
				});
			}
		}
	}

	/**
	 * @param parent
	 * @param newFrame
	 * @param relativeTo
	 */
	public static synchronized void showLinkPanel(final JFrame parent, final boolean newFrame,
			final Component relativeTo) {
		if (LinkProjectStatus.tryEnterStatus(parent, LinkProjectStatus.MANAGER_IMPORT) == false) {
			return;
		}
		if (currLink != null && currLink.isShowing()) {
			currLink.toFront();
		} else {
			LinkProjectManager.reloadLinkProjects();
	
			final LinkProjectPanel linkp = new LinkProjectPanel(parent, newFrame, relativeTo);
			currLink = linkp.toShow();
		}
	}

}
