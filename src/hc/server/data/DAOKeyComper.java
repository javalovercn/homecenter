package hc.server.data;

import hc.core.util.LogManager;
import hc.server.data.screen.KeyComper;
import hc.server.data.screen.ScreenCapturer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Vector;

public class DAOKeyComper implements IDao{
	public static final String header = "icon";
	private static final String MOBI_USER_ICONS = "ext_icons.properties";

	Properties p = new Properties();
	File mu;
	
	private static DAOKeyComper instance = null;
	
	public static DAOKeyComper getInstance(ScreenCapturer sc){
		if(instance == null){
			instance = new DAOKeyComper(sc);
		}else{
			if(sc != null){
				instance.sc = sc;
				if(sc.mobiUserIcons == null){
					sc.mobiUserIcons = instance.comper;
				}
			}
		}
		return instance;
	}
	
	public Properties getProperties(){
		return p;
	}
	
	static String[] images = {"/hc/res/esc_16.png", "/hc/res/tab_16.png", "/hc/res/ctrl_s_16.png", 
		"/hc/res/ctrl_o_16.png", "/hc/res/ctrlad_16.png"};
	
	//四个常用和六个标准图标
	static String[] key_images = null;

	public static final int MAX_KEYCOMP_NUM = 6;
	
	public static String[] getImagesURL(){
		if(key_images == null){
			int standLen = MAX_KEYCOMP_NUM;
			key_images = new String[standLen + images.length];
			for (int i = 0; i < standLen; ) {
				key_images[i] = "/hc/res/k" + (++i) + "_16.png";
			}
			
			for (int i = standLen; i < key_images.length; i++) {
				key_images[i] = images[i - standLen];
			}
		}
		
		return key_images;
	}
	
	private ScreenCapturer sc;
	public DAOKeyComper(ScreenCapturer sc) {
		this.sc = sc;
		
		boolean isExist = false;
		mu = new File(DAOKeyComper.MOBI_USER_ICONS);
		isExist = mu.exists();
		
		if(isExist == false){
//			实际生成内容
//			icon2=CONTROL+Tab\:/hc/res/tab_16.png
//			icon1=Escape\:/hc/res/esc_16.png
			put(1, "Escape", images[0]);
			put(2, "Tab", images[1]);
//			put(3, "Control+s", images[2]);
//			put(4, "Control+o", images[3]);
//			put(5, "Control+Shift+Escape", images[4]);
			
			save();
		}else{
			load();
		}
	}
	
	public void put(int no, String keyComp, String imageURL){
		p.put(header + no, keyComp + ":" + imageURL);
	}
	
	private boolean isLoaded = false;

	@Override
	public void load() {
		if(isLoaded == true){
			return;
		}
		
		isLoaded = true;
		
		try {
			p.load(new FileInputStream(mu));
			refresh();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}		
	}

	@Override
	public void save() {
		isLoaded = true;
		
		try {
			p.store(new FileOutputStream(mu), null);
			
			refresh();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	
	public KeyComper getKeyComper(){
		return comper;
	}
	
	private KeyComper comper;
	private void refresh(){
		comper = new KeyComper();

		try {
			for (int i = 1, end = 1 + MAX_KEYCOMP_NUM; i < end; i++) {
				String v = p.getProperty(DAOKeyComper.header + i);
				if(v == null){
					break;
				}else{
					Vector vect = KeyComper.splitByChar(v, ':', 2);
					if(vect == null || vect.size() == 0 || vect.size() != 2){
						LogManager.err("Unknows data : [" + v + "]");
						break;
					}
					String k = ((String)vect.elementAt(0)).replaceAll(" ", "");
					String imageurl = ((String)vect.elementAt(1));
					
					vect = KeyComper.convertStr(k);
					if(vect == null || vect.size() == 0 || 
							(comper.addMap(vect, imageurl) == false)){
						LogManager.err("Unknows data : [" + v + "]");
						break;
					}
				}
			}
		} catch (Exception e) {
		}
		
		if(sc != null){
			sc.mobiUserIcons = comper;
		}
	}

}
