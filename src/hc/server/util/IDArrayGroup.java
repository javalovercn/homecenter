package hc.server.util;

import hc.App;
import hc.core.ContextManager;
import hc.util.PropertiesManager;

import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class IDArrayGroup {
	final String groupName;
	final ArrayList<String> members = new ArrayList<String>();
	private static final String SPLIT_STR = "#";

	public static final String MSG_ID_DESIGNER = "S1";
	public static final String MSG_ID_LOCK_SCREEN = "S2";
	public static final String MSG_NOTIFIED_SERVER_DIRECT_MODE = "S3";
	public static final String MSG_JRUBY_RUN_NO_COVER = "S4";
	public static final String MSG_SYSTEM_CLASS_LIMITED = "S5";
	public static final String MSG_MIN_JRE_7 = "S6";
	public static final String MSG_CSS_NOTE = "S7";

	public static final void showMsg(final String msgId, final String sys_icon_str, final String title, final String message) {
		if (checkAndAdd(msgId)) {

			final JPanel panel = new JPanel();
			panel.add(new JLabel(message, App.getSysIcon(sys_icon_str), SwingConstants.CENTER));
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					App.showCenterPanelMain(panel, 0, 0, title, false, null, null, null, null, null, true, true, null, false, false);// 改为isNewFrame
				}
			});
		}
	}

	public IDArrayGroup(final String groupName) {
		this.groupName = groupName;

		final String memStrs = PropertiesManager.getValue(groupName);
		try {
			if (memStrs != null && memStrs.length() > 0) {
				final String[] ids = memStrs.split(SPLIT_STR);
				for (int i = 0; i < ids.length; i++) {
					final String item = ids[i];
					if (isIn(item) == false) {
						members.add(item);
					}
				}
			}
		} catch (final Exception e) {
		}
	}

	public boolean isIn(final String id) {
		return members.contains(id);
	}

	/**
	 * 添加并保存
	 * 
	 * @param id
	 */
	public void add(final String id) {
		synchronized (IDArrayGroup.class) {
			if (isIn(id) == false) {
				members.add(id);
			} else {
				return;
			}

			final StringBuffer sb = new StringBuffer();
			final int size = members.size();
			for (int i = 0; i < size; i++) {
				if (i != 0) {
					sb.append(SPLIT_STR);
				}
				sb.append(members.get(i));
			}

			PropertiesManager.setValue(groupName, sb.toString());
			PropertiesManager.saveFile();
		}
	}

	/**
	 * true : 存在；
	 * 
	 * @param msgID
	 * @return
	 */
	public static boolean check(final String msgID) {
		final IDArrayGroup ia = new IDArrayGroup(PropertiesManager.p_ReadedMsgID);
		return ia.isIn(msgID);
	}

	/**
	 * 如果不存在，并添加保存，返回true。否则返回false
	 * 
	 * @param msgID
	 * @return
	 */
	public static boolean checkAndAdd(final String msgID) {
		final IDArrayGroup ia = new IDArrayGroup(PropertiesManager.p_ReadedMsgID);
		if (ia.isIn(msgID)) {
			return false;
		}

		ia.add(msgID);// add方法内含save
		return true;
	}
}
