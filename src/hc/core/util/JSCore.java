package hc.core.util;

public class JSCore {

	public static final byte[] actionExt = "actionExt".getBytes();

	public static final byte[] back = "back".getBytes();
	
	public static final byte[] clickJButton = "clickJButton".getBytes();
	public static final byte[] selectSlider = "selectSlider".getBytes();
	public static final byte[] selectComboBox = "selectComboBox".getBytes();
	public static final byte[] notifyTextFieldValue = "notifyTextFieldValue".getBytes();
	public static final byte[] notifyTextAreaValue = "notifyTextAreaValue".getBytes();
	public static final byte[] clickJRadioButton = "clickJRadioButton".getBytes();
	public static final byte[] clickJCheckbox = "clickJCheckbox".getBytes();

	public static final byte[] mouseReleased = "mouseReleased".getBytes();
	public static final byte[] mousePressed = "mousePressed".getBytes();
	public static final byte[] mouseExited = "mouseExited".getBytes();
	public static final byte[] mouseEntered = "mouseEntered".getBytes();
	public static final byte[] mouseClicked = "mouseClicked".getBytes();
	public static final byte[] mouseDragged = "mouseDragged".getBytes();
	
	public static final byte[] splitBS = StringUtil.SPLIT_LEVEL_2_JING.getBytes();
	
	private static final String JING = new String(splitBS, 0, 1);
	private static final String JING_ENCODE = "\\" + JING;
	
	/**
	 * 将串中的#转为\#
	 * @param cmds
	 * @return
	 */
	public static final String encode(final String cmds){
		return StringUtil.replace(cmds, JING, JING_ENCODE);
	}
	
	/**
	 * 将串中的\#转为#
	 * @param cmds
	 * @return
	 */
	public static final String decode(final String cmds){
		return StringUtil.replace(cmds, JING_ENCODE, JING);
	}
	
}
