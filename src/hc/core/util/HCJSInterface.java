package hc.core.util;

public interface HCJSInterface {
	public void actionExt(String cmd);

	public boolean clickButton(String id);

	public boolean selectSlider(String id, String value);

	public boolean selectComboBox(String id, String selectedIndex);

	public boolean notifyTextFieldValue(String id, String value);

	public boolean notifyTextAreaValue(String id, String value);

	public boolean clickRadioButton(String id);

	public boolean clickCheckbox(String id);
}
