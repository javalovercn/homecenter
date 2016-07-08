package hc.server.data;

import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.data.screen.KeyComper;
import hc.server.data.screen.ScreenCapturer;
import hc.util.ResourceUtil;

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
	
	public static synchronized DAOKeyComper getInstance(final ScreenCapturer sc){
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
	
	public static synchronized String[] getImagesURL(){
		if(key_images == null){
			final int standLen = MAX_KEYCOMP_NUM;
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
	public DAOKeyComper(final ScreenCapturer sc) {
		this.sc = sc;
		
		boolean isExist = false;
		mu = new File(ResourceUtil.getBaseDir(), DAOKeyComper.MOBI_USER_ICONS);
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
	
	public void put(final int no, final String keyComp, final String imageURL){
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
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			return;
		}		
	}

	@Override
	public void save() {
		isLoaded = true;
		
		try {
			p.store(new FileOutputStream(mu), null);
			
			refresh();
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			return;
		}
	}
	
	public KeyComper getKeyComper(){
		return comper;
	}
	private final static int START_IDX = 1;
	private KeyComper comper;
	private void refresh(){
		comper = new KeyComper();

		try {
			for (int i = START_IDX, end = START_IDX + MAX_KEYCOMP_NUM; i < end; i++) {
				final String v = p.getProperty(DAOKeyComper.header + i);
				if(v == null){
					break;
				}else{
					final String[] items = v.split(":");
					if(items == null || items.length == 0 || items.length != 2){
						LogManager.err("Unknows data : [" + v + "]");
						break;
					}
					final String k = items[0].replaceAll(" ", "");
					final String imageurl = items[1];
					
					final Vector vect = KeyComper.convertStr(k);
					if(vect == null || vect.size() == 0 || 
							(comper.addMap(vect, imageurl) == false)){
						LogManager.err("Unknows data : [" + v + "]");
						break;
					}
				}
			}
			
			//删除不用的图标
			{
				final File file = new File(ResourceUtil.getBaseDir(), "." + StoreDirManager.ICO_DIR);
				final File[] icons = file.listFiles();
				final int userSize = comper.size();
				for (int i = 0; i < icons.length; i++) {
					boolean isUsing = false;
					for (int j = START_IDX; j <= userSize; j++) {
						if(comper.getImagePath(j).endsWith(icons[i].getName())){
							isUsing = true;
							break;
						}
					}
					if(isUsing == false){
						L.V = L.O ? false : LogManager.log("delete unused keymap icon : " + icons[i].getAbsolutePath());
						icons[i].delete();
					}
				}
			}
		} catch (final Exception e) {
		}
		
		if(sc != null){
			sc.mobiUserIcons = comper;
		}
	}

}
