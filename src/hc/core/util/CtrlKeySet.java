package hc.core.util;

public abstract class CtrlKeySet implements CtrlKey {

	public static final int MAX_CTRL_ITEM_NUM = 256;
	public final int USED_KEY = 1;
	public final int[] used_keys = new int[CtrlKeySet.MAX_CTRL_ITEM_NUM];
	public final String[] pngs = new String[CtrlKeySet.MAX_CTRL_ITEM_NUM];
	public final String[] desc = new String[CtrlKeySet.MAX_CTRL_ITEM_NUM];
	private int keySize;
	public static final String SEND_STATUS = "sendStatus";

	void buildOneMap(int key, String imageName, String ds){
		keySize++;
		used_keys[key] = USED_KEY;
		pngs[key] = imageName;
		desc[key] = ds;
	}	

	public int[] getDispKeys(){
		final int[] out = new int[keySize];
		for (int i = 0, j = 0; i < used_keys.length; i++) {
			if(used_keys[i] == USED_KEY){
				out[j++] = i;
			}
		}
		return out;
	}
	
	public String getKeyDesc(int keyValue){
		return desc[keyValue];
	}
	
	public String getPNGName(int keyValue){
		return pngs[keyValue];
	}

	abstract void buildMapInfo();
	
}
