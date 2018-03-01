package hc.server.ui.design;

import hc.core.L;
import hc.core.util.LogManager;

public class UIEventInput {
	public static final int DIALOG_EVENT = 1;// press back 不触发
	public static final int HTMLMLET_EVENT = 1 << 2;
	public static final int MLET_EVENT = 1 << 3;
	public static final int CTRL_EVENT = 1 << 4;
	public static final int MENU_EVENT = 1 << 5;
	public static final int QUESTION_EVENT = 1 << 6;// press back
													// 不触发，但如有cancel且点击，则触发

	private int eventType;

	/**
	 * 存在不同步问题
	 */
	public final void setQuestionEvent() {
		L.V = L.WShop ? false : LogManager.log("[Mobile Event] : QUESTION_EVENT");
		eventType = QUESTION_EVENT;
	}

	public final boolean isQuestionEvent() {
		return eventType == QUESTION_EVENT;
	}

	public final void setCtrlEvent() {
		L.V = L.WShop ? false : LogManager.log("[Mobile Event] : CTRL_EVENT");
		eventType = CTRL_EVENT;
	}

	public final boolean isCtrlEvent() {
		return eventType == CTRL_EVENT;
	}

	public final void setMenuEvent() {
		L.V = L.WShop ? false : LogManager.log("[Mobile Event] : MENU_EVENT");
		eventType = MENU_EVENT;
	}

	public final boolean isMenuEvent() {
		return eventType == MENU_EVENT;
	}

	public final boolean isDialogEvent() {
		return (eventType & DIALOG_EVENT) != 0;
	}

	public final void setHTMLMletEvent(final boolean isDialog) {
		L.V = L.WShop ? false
				: LogManager.log("[Mobile Event] : HTMLMLET_EVENT, isDialog : " + isDialog);
		if (isDialog) {
			eventType = HTMLMLET_EVENT | DIALOG_EVENT;
		} else {
			eventType = HTMLMLET_EVENT;
		}
	}

	public final boolean isHTMLMletEvent() {
		return (eventType & HTMLMLET_EVENT) != 0;
	}

	public final void setMletEvent(final boolean isDialog) {
		L.V = L.WShop ? false
				: LogManager.log("[Mobile Event] : MLET_EVENT, isDialog : " + isDialog);

		if (isDialog) {
			eventType = MLET_EVENT | DIALOG_EVENT;
		} else {
			eventType = MLET_EVENT;
		}
	}

	public final boolean isMletEvent() {
		return (eventType & MLET_EVENT) != 0;
	}
}
