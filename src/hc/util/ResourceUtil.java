package hc.util;

import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.StarterManager;
import hc.server.data.KeyComperPanel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

public class ResourceUtil {
	private final static Class starterClass = getStarterClass();
	
	public static boolean validEmail(String email) {
		String email_pattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
				+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(email_pattern);
		Matcher m = pattern.matcher(email);
		return m.find();
	}
	
	public static boolean checkEmailID(final String donateIDStr, final Component parent){
		if (donateIDStr.startsWith("0") == false && ResourceUtil.validEmail(donateIDStr) == false) {//保留旧HomeCenterID支持
			JOptionPane.showMessageDialog(parent,
					(String)ResourceUtil.get(9073),
					(String) ResourceUtil.get(IContext.ERROR),
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (IConstant.checkUUID(donateIDStr) == false) {
			JOptionPane.showMessageDialog(parent, 
					StringUtil.replace((String)ResourceUtil.get(9072), "{max}", "" + MsgBuilder.LEN_MAX_UUID_VALUE),
					(String) ResourceUtil.get(IContext.ERROR),
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	public static String getTimeStamp(){
		final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		final StringBuffer timestampBuf = new StringBuffer(25);
		timestampBuf.append((timestamp.getYear() + 1900));
		final int month = (timestamp.getMonth() + 1);
		timestampBuf.append((month < 10?("0"+month):month));
		final int day = timestamp.getDate();
		timestampBuf.append(day < 10?("0"+day):day);
		final int hour = timestamp.getHours();
		timestampBuf.append("_");
		timestampBuf.append(hour < 10?("0"+hour):hour);
		final int minute = timestamp.getMinutes();
		timestampBuf.append((minute < 10?("0"+minute):minute));
		final int second = timestamp.getSeconds();
		timestampBuf.append((second < 10?("0"+second):second));
		
		return timestampBuf.toString();
	}
	
	public static int getAbstractCtrlKeyCode(){
		if(isMacOSX()){
			return KeyEvent.VK_META;
		}else{
			return KeyEvent.VK_CONTROL;
		}
	}
	
	/**
	 * 替换串中如{1234}，{uuid}等内容
	 * @param str
	 * @return
	 */
	public static String replaceWithI18N(String str){
		//替换{1234}为相应值
		while(true){
			final String regua = "\\{.*?\\}";
			Pattern p = Pattern.compile(regua);
			Matcher ma = p.matcher(str);
			if(ma.find()){
				String g = ma.group();
				String key = g.substring(1, g.length() - 1);
				if(key.equals("uuid")){
					str = str.replace(g, IConstant.uuid);
				}else{
					str = str.replace(g, (String)ResourceUtil.get(Integer.parseInt(key)));
				}
			}else{
				break;
			}
		}
		
		return str;
	}
	
	public static int getAbstractCtrlInputEvent(){
		if(isMacOSX()){
			return InputEvent.META_MASK;
		}else{
			return InputEvent.CTRL_MASK;
		}
	}
	
	public static KeyStroke getRefreshKeyStroke(){
		if(isMacOSX()){
			return KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_MASK);
		}else{
			return KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
		}
	}
	
	public static String getRefreshKeyText(){
		if(isMacOSX()){
			return getMacOSCommandKeyText() + " + R";
		}else{
			return "F5";
		}
	}
	
	public static String getAbstractCtrlInputEventText(){
		final int key = getAbstractCtrlInputEvent();
		
		if(key == InputEvent.META_MASK){
			return getMacOSCommandKeyText();
		}else if(key == InputEvent.CTRL_MASK){
			return "Ctrl";
		}
		return "";
	}
	
	public static String getAbstractCtrlKeyText(){
		return KeyComperPanel.getHCKeyText(getAbstractCtrlKeyCode());
	}
	
	public static String getMacOSCommandKeyText(){
		return String.valueOf((char)Integer.parseInt("8984"));
	}
	
	public static void buildAcceleratorKeyOnAction(final Action action, final int keyCode){
		action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyCode, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}
	
	private static Class getStarterClass(){
		try {
			return Class.forName(StarterManager.CLASSNAME_STARTER_STARTER);
		} catch (ClassNotFoundException e) {
//			LogManager.err("Cant find Class : " + starterClass);
//			e.printStackTrace();
		}
		return null;
	}
	
	public static String toMD5(byte tmp[]){
		char hexDigits[] = {       // 用来将字节转换成 16 进制表示的字符
			     '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',  'e', 'f'};
		char str[] = new char[16 * 2];   // 每个字节用 16 进制表示的话，使用两个字符，
		int k = 0;                                // 表示转换结果中对应的字符位置
		for (int i = 0; i < 16; i++) {          // 从第一个字节开始，对 MD5 的每一个字节
		                                 // 转换成 16 进制字符的转换
			byte byte0 = tmp[i];                 // 取第 i 个字节
			str[k++] = hexDigits[byte0 >>> 4 & 0xf];  // 取字节中高 4 位的数字转换, 
		                                             // >>> 为逻辑右移，将符号位一起右移
			str[k++] = hexDigits[byte0 & 0xf];            // 取字节中低 4 位的数字转换
		} 
		return new String(str);                     
	}

	
	public static Object get(int id){
		if(id < 10000){
			return UILang.getUILang(id);
		}
		return null;
	}
	
	private static final ClassLoader cldr = ResourceUtil.class.getClassLoader();
	
	public static URL getResource(String fileName){
	    return cldr.getResource(fileName);
	}

	public static byte[] getAbsPathContent(String path) throws Exception{
		return getResource(getAbsPathInputStream(path));
	}
	
	public static DataInputStream getAbsPathInputStream(String path) throws Exception{
		URL uri = null;
		try{
			if(starterClass != null){
				uri = starterClass.getResource(path);
				return getInputStream(uri);
			}
		}catch (Throwable e) {
		}
		
		try{
			return getInputStream(ResourceUtil.class.getResource(path));
		}catch (Throwable e) {
		}
		
		uri = new File("." + path).toURI().toURL();
		return getInputStream(uri);
	}
	
	private static DataInputStream getInputStream(URL url) throws Exception {
		return new DataInputStream(url.openStream());
	}
	
	public static BufferedImage loadImage(final String imageFileName){
		try {
			return ImageIO.read(ResourceUtil.getResource("hc/res/" + imageFileName));
		} catch (Exception e) {
		}
		return null;
	}

	public static byte[] getResource(DataInputStream dataIs) throws Exception {
		byte[] imgDataBa = new byte[dataIs.available()];
//		DataInputStream dataIs = new DataInputStream(uri.openStream());
		dataIs.readFully(imgDataBa);
		dataIs.close();
		return imgDataBa;
	}

	public static synchronized String createResourceID(){
		String p_id = PropertiesManager.getValue(PropertiesManager.p_ResourceID);
		if(p_id == null){
			p_id = "1";
		}else{
			p_id = String.valueOf(Integer.parseInt(p_id) + 1);
		}
		PropertiesManager.setValue(PropertiesManager.p_ResourceID, p_id);
		PropertiesManager.saveFile();
		
		return p_id;
	}
	
	static {
		//初始时，要删除上次可能因停电产生的需要待删除的资源。
		notifyCancle();
	}
	
	public static void notifySave(){
		PropertiesManager.setValue(PropertiesManager.p_ResourcesMaybeUnusedNew, "");
		removeUnused(PropertiesManager.p_ResourcesMaybeUnusedOld);
		PropertiesManager.saveFile();
	}
	
	public static void notifyCancle() {
		PropertiesManager.setValue(PropertiesManager.p_ResourcesMaybeUnusedOld, "");
		removeUnused(PropertiesManager.p_ResourcesMaybeUnusedNew);
		PropertiesManager.saveFile();
	}

	private static void removeUnused(String pNew) {
		String rr = PropertiesManager.getValue(pNew);
		if(rr == null){
			return;
		}
		String baseDir = System.getProperty("user.dir");
		String[] toRemove = rr.split(";");
		for (int i = 0; i < toRemove.length; i++) {
			String url = toRemove[i];
			if(url.length() > 0){
				if(url.indexOf("hc/res/") >= 0){
					continue;
				}
				File file = new File(baseDir + url);
				//强制释放可能没有关闭的资源。
				System.gc();
				boolean b = file.delete();
				//强制释放可能没有关闭的资源。
				System.gc();
//				System.out.println("Del ico : " + file.toString() + ", succe:" + b);
			}
		}
		PropertiesManager.setValue(pNew, "");
		PropertiesManager.saveFile();
	}
	
	public static void addMaybeUnusedResource(String url, boolean isNew){
		String p = null;
		if(isNew){
			p = PropertiesManager.p_ResourcesMaybeUnusedNew;
		}else{
			p = PropertiesManager.p_ResourcesMaybeUnusedOld;
		}
		addUnused(url, p);
	}

	private static void addUnused(String url, String p_new) {
		String rr = PropertiesManager.getValue(p_new);
		if(rr == null){
			rr = "";
		}
		if(rr.length() == 0){
			rr = url;
		}else{
			rr += ";" + url;
		}
		PropertiesManager.setValue(p_new, rr);
		PropertiesManager.saveFile();
	}

	public static void addJar(final ClassLoader cl, final File jarfile) throws Exception{
		Method addPath = null;
	    addPath = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
	    addPath.setAccessible(true);
		addPath.invoke(cl, new Object[] { jarfile.toURI().toURL() });
	}
	
	public static void loadJar(File jarfile) throws Exception{
		URLClassLoader loader = (URLClassLoader) ClassLoader.getSystemClassLoader();

		addJar(loader, jarfile);
	}

	public static BufferedImage resizeImage(final BufferedImage bufferedimage, final int w, final int h){
		int type = bufferedimage.getColorModel().getTransparency();        
		BufferedImage img;        
		Graphics2D graphics2d;        
		(graphics2d = (img = new BufferedImage(w, h, type)).createGraphics()).setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2d.drawImage(bufferedimage, 0, 0, w, h, 0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), null);
		graphics2d.dispose();        
		return img;        
	}
	
	/**
	 * 去掉全通透部分，改半透明为底白效果，即不透明
	 * @param src
	 * @return
	 */
	public static BufferedImage unAlphaImage(final BufferedImage src){
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] data = new int[w * h];
		src.getRGB(0, 0, w, h, data, 0, w);
		for (int i = 0; i < data.length; i++) {
			final int rgb = data[i];
			if((rgb | 0x00FFFFFF) != 0x00FFFFFF){
				data[i] = 0xFFFFFFFF;
			}
		}
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		out.setRGB(0, 0, w, h, data, 0, w);
		Graphics g = out.getGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return out;
	}

	public static boolean isWindowsOS() {
		String os = System.getProperty("os.name");
		final boolean isWindow = os.toLowerCase().indexOf("windows") >= 0;
		return isWindow;
	}
	
	public static boolean isLinuxRelease(String issue){
		final String lowIssue = issue.toLowerCase();
		boolean isRelease = false;
		try {
			Process process = Runtime.getRuntime().exec("lsb_release -a");//cat /etc/issue
			InputStreamReader ir = new InputStreamReader(
					process.getInputStream());
			LineNumberReader input = new LineNumberReader(ir);
			String line;
			while ((line = input.readLine()) != null){
				if((isRelease == false) && (line.toLowerCase().indexOf(lowIssue) >= 0)){
					isRelease = true;
				}
			}
		} catch (Throwable e) {
		}
		return isRelease;
	}
	
	public static boolean isLinux() {
		String os = System.getProperty("os.name");
		final boolean isLinux = os.toLowerCase().indexOf("linux") >= 0;
		return isLinux;
	}
	
	public static boolean isWindowsXP(){
		final String os = System.getProperty("os.name");
		return os.equals("Windows XP");
	}
	
	public static boolean isWindows2003(){
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("windows") >= 0) && (os.indexOf("2003") > 0);
	}
	
	
	public static boolean isWindowsVista(){
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("Windows Vista") >= 0);
	}
	
	
	public static boolean isWindows2008(){
		//Windows Server 2008
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("windows") >= 0) && (os.indexOf("2008") > 0);
	}
	
	public static boolean isMacOSX(){
		//Mac OS X
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("mac os x") >= 0);
	}
	
	public static Properties loadThirdLibs() {
		final Properties thirdlibs = new Properties();
		try {
			String url = "http://homecenter.mobi/ajax/thirdlib.php";
			url = HttpUtil.replaceSimuURL(url, PropertiesManager.isTrue(PropertiesManager.p_IsSimu));
			thirdlibs.load(new URL(url).openStream());
		} catch (Exception e1) {
			JOptionPane.showMessageDialog(null, "Can NOT connect HomeCenter, please try after few seconds!", "Error Connect", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		return thirdlibs;
	}

	public static BufferedImage rotateImage(final BufferedImage bufferedimage,
	        final int degree) {
	    int w = bufferedimage.getWidth();
	    int h = bufferedimage.getHeight();
	    //int type = bufferedimage.getColorModel().getTransparency();
	    BufferedImage img;
	    Graphics2D graphics2d;
	    (graphics2d = (img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB))
	            .createGraphics()).setRenderingHint(
	            RenderingHints.KEY_INTERPOLATION,
	            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	    graphics2d.rotate(Math.toRadians(degree), w / 2, h / 2);
	    graphics2d.drawImage(bufferedimage, 0, 0, null);
	    graphics2d.dispose();
	    return img;
	}
	
	/**
	 * 水平翻转
	 * @param bufferedImage
	 * @return
	 */
	public static BufferedImage flipHorizontalJ2D(BufferedImage bufferedimage) {
		int w = bufferedimage.getWidth();
        int h = bufferedimage.getHeight();
        BufferedImage img;
        Graphics2D graphics2d;
        (graphics2d = (img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)).createGraphics())
                .drawImage(bufferedimage, 0, 0, w, h, w, 0, 0, h, null);
        graphics2d.dispose();
        return img;
    }
	
	public static Dimension getScreenSize(){
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		return toolkit.getScreenSize();
	}
	
	public static BufferedImage toBufferedImage(Image img)
	{
	    if (img instanceof BufferedImage){
	        return (BufferedImage)img;
	    }

	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g = bimage.createGraphics();
	    g.drawImage(img, 0, 0, null);
	    g.dispose();

	    return bimage;
	}

	public static String createRandomFileNameWithExt(final File parent, final String ext) {
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		while(true){
			int r = random.nextInt(99999999);
			final String str_r = String.valueOf(r) + ext;
			File file_r;
			if(parent == null){
				file_r = new File(str_r);
			}else{
				file_r = new File(parent, str_r);
			}
			if(file_r.exists() == false){
				return str_r;
			}
		}
	}

	public static String getMD5(final File file) {
		String filemd5 = "";
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(byteBuffer);
			BigInteger bi = new BigInteger(1, md5.digest());
			filemd5 = bi.toString(16);
		} catch (Exception e) {
			L.V = L.O ? false : LogManager.log("Error get MD5 of file : " + file.toString());
			e.printStackTrace();
		} finally {
			if(null != in) {
				try {
					in.close();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		return filemd5;
	}
}
