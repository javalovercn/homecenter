package hc.server.ui;

public class CacheString {
	public String jsOrStyles;
	public boolean isCacheEnabled;
	
	public CacheString(final String jsOrStyles, final boolean isCacheEnabled){
		this.jsOrStyles = jsOrStyles;
		this.isCacheEnabled = isCacheEnabled;
	}
}
