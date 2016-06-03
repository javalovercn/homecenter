package hc.core.util;

public interface HCJSInterface {
	public void actionExt(String cmd);
	
	public void clickJButton(int id);

	public void selectSlider(int id, int value);

	public void selectComboBox(int id, int selectedIndex);

	public void notifyTextFieldValue(int id, String value);
	
	public void notifyTextAreaValue(int id, String value);
	
	public void clickJRadioButton(int id);
	
	public void clickJCheckbox(int id);
}
