package hc.server.ui;

import hc.server.ui.design.J2SESession;

public interface IMletCanvas {
	public void setMlet(final J2SESession coreSS, Mlet mlet, final ProjectContext projectCtx);
	
	public Mlet getMlet();
	
	public void init();
	
	public void setScreenIDAndTitle(String screenID, String title);
	
	public boolean isSameScreenID(final byte[] bs, final int offset, final int len);
	
	public boolean isSameScreenIDIgnoreCase(final char[] chars, final int offset, final int len);
	
	/**
	 * cmdBs
	 * @param bs
	 * @param offset cmdBs
	 * @param len cmdBs
	 */
	public void actionJSInput(byte[] bs, int offset, int len);
	
	public void onExit(final boolean isAutoReleaseAfterGo);
}
