package hc.server.util.ai;

public abstract class AnalysableData {
	public static final int DIRECT_IN = 1;
	public static final int DIRECT_OUT = 2;

	public static final Object NON_UI_FOR_PROJECT_ONLY = new Object();

	public int uiType;
	public Object uiObject;

	public String projectID;
	public long currMS;
	public long threadID;
	public String clientLocale;
	public int direct;

	public final void snap(final String projectID, final String clientLocale, final int direct) {
		this.projectID = projectID;
		currMS = System.currentTimeMillis();
		threadID = Thread.currentThread().getId();
		this.clientLocale = clientLocale;
		this.direct = direct;
	}

	public abstract boolean isSameWithPre(final AnalysableData pre);
}
