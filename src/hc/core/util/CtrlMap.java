package hc.core.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class CtrlMap {
	public final StoreableHashMap map;
	
	final String HEAD_KEY = "k_v_";
	final int START_IDX_HEAD_KEY = HEAD_KEY.length();
	final String HEAD_CENTER_X = "k_x_";
	final String HEAD_CENTER_Y = "k_y_";
	//定制显示按钮名
	final String HEAD_KEY_TXT = "k_txt_";
	final String HEAD_SIZE = "k_size";
	private final String HEAD_CTRL_TITLE = "k_title";
	final String HEAD_BLOCK_WIDTH = "k_b_w";
	final String HEAD_PNG = "k_png_";
	final String HEAD_CTRL_ID = "k_id";
	int size;
	
	public CtrlMap(StoreableHashMap map) {
		this.map = map;
		
		final String str_size = (String)map.get(HEAD_SIZE);
		if(str_size == null){
			map.put(HEAD_SIZE, "0");
		}else{
			size = Integer.parseInt(str_size);
		}
	}
	
	public void updateButtonOnCanvas(final int keyValue, final int center_x, final int center_y){
		map.put(HEAD_CENTER_X + keyValue, String.valueOf(center_x));
		map.put(HEAD_CENTER_Y + keyValue, String.valueOf(center_y));
//		L.V = L.O ? false : LogManager.log("updateButtonOnCanvas x : " + center_x + ", y : " + center_y);
	}

	public final void addButtonOnCanvas(final int keyValue, final int center_x, final int center_y){
		synchronized (this) {
			map.put(HEAD_KEY + keyValue, "1");
			map.put(HEAD_CENTER_X + keyValue, String.valueOf(center_x));
			map.put(HEAD_CENTER_Y + keyValue, String.valueOf(center_y));

			map.put(HEAD_SIZE, String.valueOf((++size)));
		}
//		L.V = L.O ? false : LogManager.log("addButtonOnCanvas size : " + size + ", x : " + center_x + ", y : " + center_y);
	}
	
	public final void setButtonTxt(final int keyValue, final String txt){
		//忽略null或空串
		if(txt != null && txt.length() > 0){
			map.put(HEAD_KEY_TXT + keyValue, txt);
		}
	}
	
	public final void removeButtonFromCanvas(final int keyValue){
		synchronized (this) {
			map.remove(HEAD_KEY + keyValue);
			
			map.remove(HEAD_CENTER_X + keyValue);
			map.remove(HEAD_CENTER_Y + keyValue);
			map.remove(HEAD_KEY_TXT + keyValue);

			map.put(HEAD_SIZE, String.valueOf((--size)));
		}
	}
	
	public final int[] getButtonsOnCanvas(){
		synchronized (this) {
			int[] out = new int[size];
			int out_idx = 0;
			Enumeration it = map.keys();
			try{
			while(it.hasMoreElements()){
				final String key = (String)it.nextElement();
				if(key.startsWith(HEAD_KEY)){
					int keyValue = Integer.parseInt(key.substring(START_IDX_HEAD_KEY));
					out[out_idx++] = keyValue;
				}
			}
			}catch (NoSuchElementException e) {
			}
			return out;
		}
	}
	
	public final int getCenterXOfButton(final int keyValue){
		return Integer.parseInt((String)map.get(HEAD_CENTER_X + keyValue));
	}
	
	public final int getCenterYOfButton(final int keyValue){
		return Integer.parseInt((String)map.get(HEAD_CENTER_Y + keyValue));
	}
	
	public final String getTxtOfButton(final int keyValue){
		return (String)map.get(HEAD_KEY_TXT + keyValue);
	}
	
	public final String getPNGBase64(final int keyValue){
		return (String)map.get(HEAD_PNG + keyValue);
	}
	
	public final void setPNGBase64(final int keyValue, final String pngBase64Data){
		map.put(HEAD_PNG + keyValue, pngBase64Data);
	}
	
	public final void setTitle(final String title){
		map.put(HEAD_CTRL_TITLE, title);
	}
	
	public final String getTitle(){
		return (String)map.get(HEAD_CTRL_TITLE);
	}
	
	public final void setID(final String title){
		map.put(HEAD_CTRL_ID, title);
	}
	
	public final String getID(){
		return (String)map.get(HEAD_CTRL_ID);
	}
	
	public final void setBlockWidth(final int width){
		map.put(HEAD_BLOCK_WIDTH, String.valueOf(width));
	}
	
	public final int getBlockWidth(){
		return Integer.parseInt((String)map.get(HEAD_BLOCK_WIDTH));
	}
}
