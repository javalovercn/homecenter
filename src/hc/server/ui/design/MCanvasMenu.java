package hc.server.ui.design;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.Jcip;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.server.ui.JcipManager;
import hc.server.ui.LongTimeSystemEventListenerException;
import hc.server.ui.MUIView;
import hc.server.ui.MenuItem;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.SessionMobiMenu;
import hc.util.StringBuilderCacher;

import java.util.Vector;

public abstract class MCanvasMenu extends MUIView {
	final String projectID;
	final ProjResponser resp;
	final Object buildLock;

	public MCanvasMenu(final String projectID, final ProjResponser resp) {
		this.projectID = projectID;
		this.resp = resp;
		buildLock = this;
	}

	/**
	 * 如果长度为0，或空串，表示不显示Title
	 * 
	 * @param coreSS
	 * @return
	 */
	public abstract String getTitle(final J2SESession coreSS);

	public abstract String[] getIcons(final J2SESession coreSS, final Vector<MenuItem> menuItems);

	public abstract String[] getIconLabels(J2SESession coreSS, final Vector<MenuItem> menuItems);

	public abstract String[] getURLs(J2SESession coreSS, final Vector<MenuItem> menuItems);

	/**
	 * 无意义
	 * 
	 * @return
	 */
	public abstract int getNumRow(J2SESession coreSS, final Vector<MenuItem> menuItems);

	/**
	 * 设定强制列数。如果为0，则表示根据图片集的最大宽度为公用宽度，最大高度为公用高度，来自动计算实际列数
	 * 
	 * @return
	 */
	public abstract int getNumCol();

	public abstract boolean isFullMode();

	public abstract int getSize(J2SESession coreSS, final Vector<MenuItem> menuItems);

	public abstract String getIconLabel(final J2SESession coreSS, final MenuItem menuItem);

	public abstract String getIcon(final J2SESession coreSS, final MenuItem item);

	public abstract String getURL(final J2SESession coreSS, final MenuItem menuItem);

	public final String buildItemRemoveJcip(final J2SESession coreSS, final MenuItem menuItem) {
		// updateMenuItem(final Image icon, final String label, final String
		// url){
		synchronized (buildLock) {// 注意：存在复用对象，故加锁
			final StringBuilder sb = StringBuilderCacher.getFree();

			sb.append("{'").append(Jcip.MENU_ITEM_REMOVE).append("' : ");

			sb.append('{');
			appendString(sb, projectID, true);

			appendString(sb, getURL(coreSS, menuItem), false);

			sb.append("}}");

			final String out = sb.toString();
			StringBuilderCacher.cycle(sb);
			return out;
		}
	}

	public final String buildItemAddJcip(final J2SESession coreSS, final MenuItem menuItem, final MenuItem afterItem) {
		// updateMenuItem(final Image icon, final String label, final String
		// url){
		final String afterURL = (afterItem == null) ? Jcip.NULL_URL : getURL(coreSS, afterItem);

		synchronized (buildLock) {// 注意：存在复用对象，故加锁
			final StringBuilder sb = StringBuilderCacher.getFree();

			sb.append("{'").append(Jcip.MENU_ITEM_ADD).append("' : ");

			sb.append('{');
			appendString(sb, projectID, true);

			appendString(sb, getIcon(coreSS, menuItem), true);

			appendString(sb, getIconLabel(coreSS, menuItem), true);

			appendString(sb, getURL(coreSS, menuItem), true);

			appendString(sb, menuItem.isEnabled() ? IConstant.TRUE : IConstant.FALSE, true);

			appendString(sb, afterURL, false);

			sb.append("}}");

			final String out = sb.toString();
			StringBuilderCacher.cycle(sb);
			return out;
		}
	}

	public final String buildChangeIconJcip(final J2SESession coreSS, final MenuItem menuItem) {
		// updateMenuItem(final Image icon, final String label, final String
		// url){
		synchronized (buildLock) {// 注意：存在复用对象，故加锁
			final StringBuilder sb = StringBuilderCacher.getFree();

			sb.append("{'").append(Jcip.MENU_ITEM_CHANGE_ICON).append("' : ");

			sb.append('{');
			appendString(sb, projectID, true);

			appendString(sb, getIcon(coreSS, menuItem), true);

			appendString(sb, getURL(coreSS, menuItem), false);

			sb.append("}}");

			final String out = sb.toString();
			StringBuilderCacher.cycle(sb);
			return out;
		}
	}

	public final String buildItemRefreshJcip(final J2SESession coreSS, final MenuItem menuItem) {
		// updateMenuItem(final Image icon, final String label, final String
		// url){
		synchronized (buildLock) {// 注意：存在复用对象，故加锁
			final StringBuilder sb = StringBuilderCacher.getFree();

			sb.append("{'").append(Jcip.MENU_ITEM_MODIFY).append("' : ");

			sb.append('{');
			appendString(sb, projectID, true);

			appendString(sb, getIconLabel(coreSS, menuItem), true);

			appendString(sb, getURL(coreSS, menuItem), true);

			appendString(sb, menuItem.isEnabled() ? IConstant.TRUE : IConstant.FALSE, false);

			sb.append("}}");

			final String out = sb.toString();
			StringBuilderCacher.cycle(sb);
			return out;
		}
	}

	@Override
	public void transMenuWithCache(final J2SESession coreSS) {
		final SessionMobiMenu menu = coreSS.getMenu(projectID);
		int countPrint = 0;
		while (menu.isEnableFlushMenu() == false) {
			if (L.isInWorkshop) {
				LogManager.log("waiting session menu to increment mode...");
			}

			final int moment = ThreadPriorityManager.UI_WAIT_OTHER_THREAD_EXEC_MS;

			countPrint += moment;
			try {
				Thread.sleep(moment);
			} catch (final Throwable e) {
			}

			if (countPrint > 2000) {// 不作超长时间等待
				ServerUIAPIAgent.runInSessionThreadPool(coreSS, resp, new Runnable() {
					@Override
					public void run() {
						final LongTimeSystemEventListenerException exception = new LongTimeSystemEventListenerException(
								"long time task in SystemEventListener, maybe you need ProjectContext.run().");
						ExceptionReporter.printStackTrace(exception, "", LongTimeSystemEventListenerException.class.getName(),
								ExceptionReporter.INVOKE_NORMAL);
					}
				});
				break;
			}
		}

		menu.transMenuWithCache(projectID, this);
	}

	@Override
	public final String buildJcip(final J2SESession coreSS, final Vector<MenuItem> menuItems) {
		synchronized (buildLock) {// 注意：存在复用对象，故加锁
			final StringBuilder sb = StringBuilderCacher.getFree();

			sb.append("{'").append(Jcip.MENU).append("' : ");

			sb.append('{');

			appendString(sb, projectID, true);

			appendString(sb, getTitle(coreSS), true);

			JcipManager.appendArray(sb, getIcons(coreSS, menuItems), true);

			JcipManager.appendArray(sb, getIconLabels(coreSS, menuItems), true);

			JcipManager.appendArray(sb, getURLs(coreSS, menuItems), true);

			JcipManager.appendArray(sb, getEnables(menuItems), true);

			appendInt(sb, getNumRow(coreSS, menuItems), true);

			appendInt(sb, getNumCol(), true);

			appendBool(sb, isFullMode(), true);

			appendInt(sb, getSize(coreSS, menuItems), false);

			sb.append("}}");

			final String out = sb.toString();
			StringBuilderCacher.cycle(sb);
			return out;
		}
	}

	private final String[] getEnables(final Vector<MenuItem> menuItems) {
		final String[] out = new String[menuItems.size()];
		for (int i = 0; i < out.length; i++) {
			final MenuItem item = menuItems.elementAt(i);
			out[i] = item.isEnabled() ? IConstant.TRUE : IConstant.FALSE;
		}
		return out;
	}
}
