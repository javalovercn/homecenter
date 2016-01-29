package hc.server.ui;

public interface IMletCanvas {
	public void setMlet(Mlet mlet, final ProjectContext projectCtx);
	
	public Mlet getMlet();
	
	public void init();
	
	public void setScreenIDAndTitle(String screenID, String title);
	
	public boolean isSameScreenID(final byte[] bs, final int offset, final int len);
	
	/**
	 * cmdBs
	 * @param bs
	 * @param offset cmdBs
	 * @param len cmdBs
	 */
	public void actionJSInput(byte[] bs, int offset, int len);
}
