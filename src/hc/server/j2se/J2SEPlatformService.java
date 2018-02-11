package hc.server.j2se;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.javassist.ClassClassPath;
import org.javassist.ClassPool;
import org.javassist.CtClass;
import org.javassist.CtMethod;
import org.javassist.bytecode.CodeAttribute;
import org.javassist.bytecode.LocalVariableAttribute;
import org.javassist.bytecode.MethodInfo;

import hc.App;
import hc.PlatformTrayIcon;
import hc.core.ContextManager;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.WiFiDeviceManager;
import hc.server.PlatformService;
import hc.util.ClassUtil;
import hc.util.ExitManager;
import hc.util.LogServerSide;
import hc.util.ResourceUtil;

public class J2SEPlatformService implements PlatformService {
	final File baseDir = buildCanonicalBase();
	
	private final File buildCanonicalBase(){
		File file = new File(".");
		try{
			file = file.getCanonicalFile();
		}catch (final Exception e) {
			e.printStackTrace();
		}
		return file;
	}
	
	@Override
	public Object doExtBiz(final int bizID, final Object para){
		if(bizID == BIZ_BCL){
			return "bcl.txt";
		}
		return null;
	}
	
	ClassPool pool = new ClassPool();
	
	@Override
	public void resetClassPool(){
		pool = new ClassPool();
	}
	
	/**
	 * 返回一个方法参数的形参
	 * @param method
	 * @return
	 */
	@Override
	public String[] getMethodCodeParameter(final Method method) {
		try{
			final Class clazz = method.getDeclaringClass();
			final String methodName = method.getName();
			final Class[] paraClasss = method.getParameterTypes();
			
			final String className = clazz.getName();
			CtClass cc;
			try{
				cc = pool.get(className);
			}catch (final Throwable e) {
				pool.insertClassPath(new ClassClassPath(clazz));
				cc = pool.get(className);
			}

			final CtClass[] paraCtClasss = new CtClass[paraClasss.length];
			for (int i = 0; i < paraCtClasss.length; i++) {
				paraCtClasss[i] = pool.get(paraClasss[i].getName());
			}
			final CtMethod cm = cc.getDeclaredMethod(methodName, paraCtClasss);
			final MethodInfo methodInfo = cm.getMethodInfo();
			final CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
			if(codeAttribute == null){
				return null;
			}
			final LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
	        if (attr == null) {
	            return null;
	        }
	        final String[] paramNames = new String[cm.getParameterTypes().length];
	        final int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
	        for (int i = 0; i < paramNames.length; i++)
	        	paramNames[i] = attr.variableName(i + pos);
	        return paramNames;
		}catch (final Throwable e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public File getJRubyAndroidOptimizBaseDir(){
		return null;//do nothing for standard JRE
	}
	
	@Override
	public void setAutoStart(final boolean isAutoStart){
		//不实现
//		System.out.println("set AutoStart : " +  isAutoStart);
	}
	
	@Override
	public boolean isLockScreen(){
		//不实现，仍用ScreenCaptureer的逻辑，但分条件
		return false;
	}
	
	@Override
	public long getAvailableSize(){
		return ResourceUtil.getBaseDir().getFreeSpace();
	}
	
	@Override
	public String[] listAssets(final Object pathurl){
		final Vector<String> out = new Vector<String>();
		
		ZipFile file = null;
		try{  
			file = new ZipFile(new File(((URL)pathurl).toURI()));  
		    final Enumeration<? extends ZipEntry> entries = file.entries();  
		    while ( entries.hasMoreElements()){
		        final ZipEntry entry = entries.nextElement();  
		        final String entryName = entry.getName();
		        if(entryName.indexOf("/") > 0){//目录层级
		        	continue;
		        }
				out.add(entryName);
		        //use entry input stream:  
//		        readInputStream( file.getInputStream( entry ) )  
		    }  
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}finally	{
			try{
				file.close();
			}catch (final Throwable e) {
			}
		}
		
		return out.toArray(new String[out.size()]);
	}
	
	@Override
	public void setWindowOpaque(final Window win, final boolean bool) throws Exception{//外部支持异常
		final Class awtUtilClass = Class.forName("com.sun.awt.AWTUtilities");
		final Method m = awtUtilClass.getMethod("setWindowOpaque", Window.class, boolean.class);
		m.invoke(awtUtilClass, win, bool);
//		com.sun.awt.AWTUtilities.setWindowOpaque(win, bool);//透明，Java 9不支持com.sun.awt.AWTUtilities
	}

	@Override
	public void addJCEProvider() {
		try{
			final Class sunJCEClass = Class.forName("com.sun.crypto.provider.SunJCE");
			Security.addProvider((Provider)sunJCEClass.newInstance());//in OpenJDK
			LogManager.log("successful add provider : com.sun.crypto.provider.SunJCE");
		}catch (final Throwable e) {
			e.printStackTrace();
		}
	}

	private final Vector<File> added3rdLibs = new Vector<File>();
	
	@Override
	public synchronized ClassLoader get3rdAndServClassLoader(final File[] files) {
		if(files == null){
		}else{
			CCoreUtil.checkAccess();
			
			for (int i = 0; i < files.length; i++) {
				addSystemLib(files[i], true);//由于J2SE需要测试Skin，需即时动态加载；此处不同于Android服务器环境，
			}
		}
		return J2SEPlatformService.class.getClassLoader();
	}

	@Override
	public ClassLoader loadClasses(final File[] filePaths, final ClassLoader parent, final boolean isDex, final String loadOpID) {
		if(filePaths == null || filePaths.length == 0){
			return parent;
		}
		
		int i = 0;
		try{
			final URL[] url = new URL[filePaths.length];
			for (; i < url.length; i++) {
				url[i] = filePaths[i].toURL();
			}
			return new URLClassLoader(url, parent);
		}catch (final Exception e) {
			LogManager.err("fail to load jar class : " + filePaths[i]);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}
	
	@Override
	public BufferedImage makeRoundedCorner(final BufferedImage image, final int cornerRadius) {
		final int w = image.getWidth();
		final int h = image.getHeight();
		final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2 = output.createGraphics();
	
	    g2.setComposite(AlphaComposite.Src);//SrcAtop
	    g2.drawImage(image, 0, 0, null);
	    
		final Area clear = new Area(new Rectangle(0, 0, w, h));
	    clear.subtract(new Area(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius,
	            cornerRadius)));
		g2.setComposite(AlphaComposite.Clear);
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	            RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.fill(clear);
	    
	    g2.dispose();
	
		return output;
	}

	@Override
	public BufferedImage composeImage(final BufferedImage base, final BufferedImage cover) {
		final BufferedImage bi = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
	    final Graphics2D g2d = bi.createGraphics();
		g2d.setComposite(AlphaComposite.SrcOver);
	    g2d.drawImage(base, 0, 0, null);
		g2d.setComposite(AlphaComposite.SrcOver);
		g2d.drawImage(cover, 0, 0, null);
		return bi;
	}

	@Override
	public Shape getImageShape(final Image img) {
		final ArrayList<Integer> x = new ArrayList<Integer>();
	    final ArrayList<Integer> y = new ArrayList<Integer>();
	    final int width = img.getWidth(null);
	    final int height = img.getHeight(null);
	
	    final PixelGrabber pg = new PixelGrabber(img, 0, 0, -1, -1, true);
	    try {
	        pg.grabPixels();
	    } catch (final InterruptedException e) {
	        e.getStackTrace();
	    }
	    final int pixels[] = (int[]) pg.getPixels();
	
	    // 循环像素
	    for (int i = 0; i < pixels.length; i++) {
	        final int alpha = (pixels[i] >> 24) & 0xff;
	        if (alpha == 0) {
	            continue;
	        } else {
	            x.add(i % width > 0 ? i % width - 1 : 0);
	            y.add(i % width == 0 ? (i == 0 ? 0 : i / width - 1) : i / width);
	        }
	    }
	
	    final int[][] matrix = new int[height][width];
	    for (int i = 0; i < height; i++) {
	        for (int j = 0; j < width; j++) {
	            matrix[i][j] = 0;
	        }
	    }
	
	    for (int c = 0; c < x.size(); c++) {
	        matrix[y.get(c)][x.get(c)] = 1;
	    }
	
	    final Area rec = new Area();
	    int temp = 0;
	
	    for (int i = 0; i < height; i++) {
	        for (int j = 0; j < width; j++) {
	            if (matrix[i][j] == 1) {
	                if (temp == 0)
	                    temp = j;
	                else if (j == width) {
	                    if (temp == 0) {
	                        final Rectangle rectemp = new Rectangle(j, i, 1, 1);
	                        rec.add(new Area(rectemp));
	                    } else {
	                        final Rectangle rectemp = new Rectangle(temp, i,
	                                j - temp, 1);
	                        rec.add(new Area(rectemp));
	                        temp = 0;
	                    }
	                }
	            } else {
	                if (temp != 0) {
	                    final Rectangle rectemp = new Rectangle(temp, i, j - temp, 1);
	                    rec.add(new Area(rectemp));
	                    temp = 0;
	                }
	            }
	        }
	        temp = 0;
	    }
	    return rec;
	}

	@Override
	public PlatformTrayIcon buildPlatformTrayIcon(final Image image, final String productTip, final JPopupMenu menu){
		return new JPTrayIcon(image, productTip, menu);
	}
	
	@Override
	public void setWindowShape(final Window win, final Shape shape) throws Exception{
		final Class awtUtilClass = Class.forName("com.sun.awt.AWTUtilities");
		final Method m = awtUtilClass.getMethod("setWindowShape", Window.class, Shape.class);
		m.invoke(awtUtilClass, win, shape);
//		com.sun.awt.AWTUtilities.setWindowShape(win, shape);
	}

	@Override
	public Object createRobotPeer(final Robot robot) throws Throwable{
		final GraphicsDevice defaultScreenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment()	.getDefaultScreenDevice();
		final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
		final Class cf = Class.forName("sun.awt.ComponentFactory");//允许出错，由外部拦截
		final Method m = cf.getMethod("createRobot", Robot.class, GraphicsDevice.class);
		return m.invoke(defaultToolkit, robot, defaultScreenDevice);
	}
	
	public static Class getCaptureDeviceManagerClass() {
		return ResourceUtil.loadClass("javax.media.CaptureDeviceManager", false);//注：关闭printNotFound
	}

	@Override
	public void buildCaptureMenu(final JPopupMenu popupTi, final ThreadGroup threadPoolToken) {
		if(getCaptureDeviceManagerClass() != null){
//			CapManager.buildCaptureMenu(popupTi, threadPoolToken);
			final Class capManagerClass = CapHelper.getCapManagerClass();
			final Class[] paraTypes = {JPopupMenu.class, ThreadGroup.class};
			final Object[] para = {popupTi, threadPoolToken};
			ClassUtil.invoke(capManagerClass, capManagerClass, "buildCaptureMenu", paraTypes, para, true);
		}
	}
	
	/**
	 * 遗留功能，新用户停止开放！
	 */
	@Override
	public void startCaptureIfEnable() {
//		CapManager.startCapture();
		if(getCaptureDeviceManagerClass() != null){
			final Class capManagerClass = CapHelper.getCapManagerClass();
			final Class[] paraTypes = ClassUtil.NULL_PARA_TYPES;
			final Object[] para = ClassUtil.NULL_PARAS;
			ClassUtil.invoke(capManagerClass, capManagerClass, "startCapture", paraTypes, para, true);
		}
	}

	@Override
	public void stopCaptureIfEnable() {
//		CapManager.stopCapture();
		if(getCaptureDeviceManagerClass() != null){
			final Class capManagerClass = CapHelper.getCapManagerClass();
			final Class[] paraTypes = ClassUtil.NULL_PARA_TYPES;
			final Object[] para = ClassUtil.NULL_PARAS;
			ClassUtil.invoke(capManagerClass, capManagerClass, "stopCapture", paraTypes, para, true);
		}
	}

	@Override
	public void printAndroidServerInfo() {
	}

	@Override
	public void exitSystem() {
		CCoreUtil.checkAccess();
		
		ContextManager.forceExit();
	}

	@Override
	public void startExitSystem() {
		ExitManager.startExitSystem();//移入CCoreUtil.checkAccess();
	}

	@Override
	public final File getBaseDir() {
		//getPrivateFile, 请勿checkAccess
		return baseDir;
	}
	
	@Override
	public void addSystemLib(final File jardexFile, final boolean isReload){
		if(added3rdLibs.contains(jardexFile)){
			if(isReload == false){
				LogManager.log("jar lib is added to ClassLoader, skip loading. [" + jardexFile.getAbsolutePath() + "]");
			}
			return;
		}
		
		LogManager.log("load jar lib : [" + jardexFile.getAbsolutePath() + "]");
		
		CCoreUtil.checkAccess();
		
		final URLClassLoader loader = (URLClassLoader) J2SEPlatformService.class.getClassLoader();

		try {
			addJar(loader, jardexFile);
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			App.showMessageDialog(null, e.toString(), "fail to load jar lib!!!", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addJar(final ClassLoader cl, final File jarfile) throws Exception{
		Method addPath = null;
	    addPath = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
	    addPath.setAccessible(true);
		addPath.invoke(cl, new Object[] { jarfile.toURI().toURL() });
		addPath.setAccessible(false);
	}
	
	@Override
	public BufferedImage resizeImage(final BufferedImage bufferedimage, final int w, final int h){
		final int type = bufferedimage.getColorModel().getTransparency();        
		BufferedImage img;        
		Graphics2D graphics2d;        
		(graphics2d = (img = new BufferedImage(w, h, type)).createGraphics()).setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2d.drawImage(bufferedimage, 0, 0, w, h, 0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), null);
		graphics2d.dispose();        
		return img;  
	}

	private WiFiDeviceManager wifiDeviceManager;
	
	@Override
	public WiFiDeviceManager getWiFiManager() {
		CCoreUtil.checkAccess();
		
		if(wifiDeviceManager != null){
			return wifiDeviceManager;
		}else{
			wifiDeviceManager = new WiFiDeviceManager() {
				
				@Override
				public InputStream listenFromWiFiMulticast(final String multicastIP, final int port) {
					return null;
				}
				
				@Override
				public boolean isWiFiConnected() {
					return false;
				}
				
				@Override
				public boolean hasWiFiModule() {
					return false;
				}
				
				@Override
				public String[] getWiFiAccount() {
					return new String[0];
				}
				
				@Override
				public String[] getSSIDListOnAir() {
					return new String[0];
				}
				
				@Override
				public boolean canCreateWiFiAccount() {
					return false;
				}
				
				@Override
				public void broadcastWiFiAccountAsSSID(final String[] commands, final String cmdGroup) {
				}
				
				@Override
				public OutputStream createWiFiMulticastStream(final String multicastIP, final int port) {
					return null;
				}

				@Override
				public void startWiFiAP(final String ssid, final String pwd, final String securityOption) {
				}

				@Override
				public void clearWiFiAccountGroup(final String cmdGroup) {
				}
			};
		}
		return wifiDeviceManager;
	}

	@Override
	public void setJRubyHome(final String version, final String absPath) {
		//do nothing for standard JRE, 
	}

	@Override
	public LogServerSide getLog() {
		return new LogServerSide();
	}

	@Override
	public long getFreeMem() {
		return Runtime.getRuntime().freeMemory() >> 20;
	}

	@Override
	public void closeLoader(final ClassLoader loader) {
		if(loader != null && loader instanceof URLClassLoader){
			try{
				final Method close = loader.getClass().getMethod("close");//1.7才有 
				if(close != null){
					close.invoke(loader);
				}
			}catch (final Throwable e) {
			}
		}
	}

	@Override
	public String getOsNameAndVersion() {
		return System.getProperty("os.name") + "/" + System.getProperty("os.version");
	}

	@Override
	public File[] getRubotoAndDxFiles() {
		return null;
	}

}