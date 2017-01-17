package hc.core.util;

public class JSCore {

	public static final String MOUSE_DRAGGED = "mouseDragged";
	public static final String MOUSE_CLICKED = "mouseClicked";
	public static final String MOUSE_ENTERED = "mouseEntered";
	public static final String MOUSE_EXITED = "mouseExited";
	public static final String MOUSE_PRESSED = "mousePressed";
	public static final String MOUSE_RELEASED = "mouseReleased";
	public static final String CLICK_CHECKBOX = "clickCheckbox";
	public static final String CLICK_RADIO_BUTTON = "clickRadioButton";
	public static final String NOTIFY_TEXT_AREA_VALUE = "notifyTextAreaValue";
	public static final String NOTIFY_TEXT_FIELD_VALUE = "notifyTextFieldValue";
	public static final String SELECT_COMBO_BOX = "selectComboBox";
	public static final String SELECT_SLIDER = "selectSlider";
	public static final String CLICK_BUTTON = "clickButton";
	public static final String CLICK = "click";
	public static final String CHANGE = "change";
	public static final String NOTIFY = "notify";

	public static final byte[] actionExt = "actionExt".getBytes();

	public static final byte[] back = "back".getBytes();
	
	public static final byte[] click = CLICK.getBytes();
	public static final byte[] change = CHANGE.getBytes();
	public static final byte[] notify = NOTIFY.getBytes();
	
	public static final byte[] clickButton = CLICK_BUTTON.getBytes();
	public static final byte[] selectSlider = SELECT_SLIDER.getBytes();
	public static final byte[] selectComboBox = SELECT_COMBO_BOX.getBytes();
	public static final byte[] notifyTextFieldValue = NOTIFY_TEXT_FIELD_VALUE.getBytes();
	public static final byte[] notifyTextAreaValue = NOTIFY_TEXT_AREA_VALUE.getBytes();
	public static final byte[] clickRadioButton = CLICK_RADIO_BUTTON.getBytes();
	public static final byte[] clickCheckbox = CLICK_CHECKBOX.getBytes();

	public static final byte[] mouseReleased = MOUSE_RELEASED.getBytes();
	public static final byte[] mousePressed = MOUSE_PRESSED.getBytes();
	public static final byte[] mouseExited = MOUSE_EXITED.getBytes();
	public static final byte[] mouseEntered = MOUSE_ENTERED.getBytes();
	public static final byte[] mouseClicked = MOUSE_CLICKED.getBytes();
	public static final byte[] mouseDragged = MOUSE_DRAGGED.getBytes();
	
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
