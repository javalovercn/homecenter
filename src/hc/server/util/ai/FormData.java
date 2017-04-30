package hc.server.util.ai;

public class FormData extends AnalysableData {
	public static final int UI_TYPE_FORM = 1;
	public static final int UI_TYPE_DIALOG = 2;
	public static final int UI_TYPE_PROJECT = 3;
	public static final int UI_TYPE_CTRLRESP = 4;
	
	public String ctrlKeyText;
	public int ctrlKey;
	public int ctrlClickKey;
	public String movingMsg;
	public String[] attributes;
	public String[] status;
	
	@Override
	public final boolean isSameWithPre(final AnalysableData pre) {
		if(pre.threadID == threadID && pre.direct == direct && movingMsg != null && ((FormData)pre).movingMsg == movingMsg){
			return true;
		}

		return false;
	}
}
