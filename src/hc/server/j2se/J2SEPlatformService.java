package hc.server.j2se;

import hc.App;
import hc.PlatformTrayIcon;
import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.WiFiDeviceManager;
import hc.server.PlatformService;
import hc.util.ClassUtil;
import hc.util.ExitManager;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
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
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Security;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

public class J2SEPlatformService implements PlatformService {
	final File baseDir = new File(".");
	
	@Override
	public Object doExtBiz(final int bizID, final Object para){
		return null;
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
	public void setWindowOpaque(final Window win, final boolean bool) {
		com.sun.awt.AWTUtilities.setWindowOpaque(win, bool);//透明		
	}

	@Override
	public void addJCEProvider() {
		Security.addProvider(new com.sun.crypto.provider.SunJCE()); 
	}

	private final Vector<File> added3rdLibs = new Vector<File>();
	
	@Override
	public synchronized ClassLoader get3rdClassLoader(final File[] files) {
		if(files == null){
		}else{
			CCoreUtil.checkAccess();
			
			for (int i = 0; i < files.length; i++) {
				addSystemLib(files[i], true);//由于J2SE需要测试Skin，需即时动态加载；此处不同于Android服务器环境，
			}
		}
		return ClassLoader.getSystemClassLoader();
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
			e.printStackTrace();
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
	public PlatformTrayIcon buildPlatformTrayIcon(final Image image, final String title, final JPopupMenu menu){
		return new JPTrayIcon(image, title, menu);
	}
	
	@Override
	public void setWindowShape(final Window win, final Shape shape) {
		try{
			com.sun.awt.AWTUtilities.setWindowShape(win, shape);
		}catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object createRobotPeer(final Robot robot) throws Throwable{
		return ((sun.awt.ComponentFactory) Toolkit.getDefaultToolkit())
				.createRobot(robot, GraphicsEnvironment
						.getLocalGraphicsEnvironment()
						.getDefaultScreenDevice());
	}
	
	public static Class getCaptureDeviceManagerClass() {
		try{
			return Class.forName("javax.media.CaptureDeviceManager");
		}catch (final Throwable e) {
		}
		return null;
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
	
	@Override
	public void startCapture() {
//		CapManager.startCapture();
		if(getCaptureDeviceManagerClass() != null){
			final Class capManagerClass = CapHelper.getCapManagerClass();
			final Class[] paraTypes = ClassUtil.nullParaTypes;
			final Object[] para = ClassUtil.nullParas;
			ClassUtil.invoke(capManagerClass, capManagerClass, "startCapture", paraTypes, para, true);
		}
	}

	@Override
	public void stopCapture() {
//		CapManager.stopCapture();
		if(getCaptureDeviceManagerClass() != null){
			final Class capManagerClass = CapHelper.getCapManagerClass();
			final Class[] paraTypes = ClassUtil.nullParaTypes;
			final Object[] para = ClassUtil.nullParas;
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
		CCoreUtil.checkAccess();
		ExitManager.startExitSystem();
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
				L.V = L.O ? false : LogManager.log("jar lib is added to ClassLoader, skip loading. [" + jardexFile.getAbsolutePath() + "]");
			}
			return;
		}
		
		L.V = L.O ? false : LogManager.log("load jar lib : [" + jardexFile.getAbsolutePath() + "]");
		
		CCoreUtil.checkAccess();
		
		final URLClassLoader loader = (URLClassLoader) ClassLoader.getSystemClassLoader();

		try {
			addJar(loader, jardexFile);
		} catch (final Exception e) {
			e.printStackTrace();
			App.showMessageDialog(null, e.toString(), "fail to load jar lib!!!", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addJar(final ClassLoader cl, final File jarfile) throws Exception{
		Method addPath = null;
	    addPath = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
	    addPath.setAccessible(true);
		addPath.invoke(cl, new Object[] { jarfile.toURI().toURL() });
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
}