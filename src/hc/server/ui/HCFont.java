package hc.server.ui;

import hc.server.ScreenServer;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Map;

/**
 * In {@link Mlet}, if user mobile DPI is lower, then the font size of UI should be smaller,
 * if user mobile DPI is very high, the font size of UI should be bigger.
 * You can use {@link HCFont} to automatically adapter font size to mobile DPI. 
 * <BR><BR>1. in J2SE server NOT in Android server, a instance of {@link HCFont} created in initialize method(process),
 * when a instance of {@link Mlet} call the method {@link #getSize()} of {@link HCFont}, it will return size is fitted to {@link Mlet}. 
 * <BR><BR>2. If in Android server NOT in J2SE server, a instance of {@link HCFont} created in initialize method(process),
 * {@link Mlet} will also automatically adapter font size like in J2SE server. 
 * <BR>Be careful, if a instance of {@link Mlet} call the method {@link #getSize()} of {@link HCFont}, it will return size which is the value of {@link #HCFont(String, int, int)}. 
 * <BR>There is no difference between {@link HCFont} and {@link java.awt.Font} in Android server.
 * <BR><BR>
 * There is an other good way to show font in {@link Mlet}.
 * <BR>
 * <BR>1. create PNG files, which include font info.
 * <BR>2. get mobile information by {@link ProjectContext#getMobileWidth()}, {@link ProjectContext#getMobileHeight()}, 
 * {@link ProjectContext#getMobileDPI()} 
 * <BR>3. create method <code>public void paint(Graphics g)</code> for {@link Mlet}.
 * <BR>4. call {@link Mlet#resizeImage(java.awt.image.BufferedImage, int, int)} to resize image to the best size of mobile.
 * <BR>5. use <code>java.awt.Graphics.drawImage</code> to draw PNG on the {@link Mlet}.
 */
public class HCFont extends java.awt.Font {
	private static final long serialVersionUID = -6621384324121693339L;
	private final static boolean isStandardJ2SE = checkIsStandardJ2SE();
	private final ScreenAdapter screenAdapter = ScreenAdapter.initScreenAdapterFromContext();
	
	private final static boolean checkIsStandardJ2SE(){
		Class c  = null;
		try{
			c = HCFont.class.getClassLoader().loadClass("hc.android.J2SEInitor");
		}catch (Throwable e) {
		}
		return c == null;
	}
	
	public HCFont(String name, int style, int size) {
		super(name, style, size);
	}
	
	public HCFont(Map<? extends Attribute, ?> attributes) {
		super(attributes);
	}
	
	/**
	 * @see #getSize2D()
	 * @since 7.0
	 */
	public int getSize() {
		if(isStandardJ2SE && screenAdapter != null){
			return (int)(convert(size) + 0.5);
		}else{
			return size;
		}
    }
	
	private float convert(float _size){
		return _size * screenAdapter.mobileDPI / ScreenServer.J2SE_STANDARD_DPI;
	}
	
	/**
	 * @see #getSize()
	 * @since 7.0
	 */
	public float getSize2D() {
		if(isStandardJ2SE && screenAdapter != null){
			return convert(size);
		}else{
			return pointSize;
		}
    }
}
