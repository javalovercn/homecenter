package hc.server.ui;

import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.SessionManager;
import hc.core.cache.CacheManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.RecycleRes;
import hc.core.util.StringUtil;
import hc.core.util.UIUtil;
import hc.server.ProcessingWindowManager;
import hc.server.ThirdlibManager;
import hc.server.msb.MSBAgent;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.MobiUIResponsor;
import hc.server.ui.design.ProjResponser;
import hc.server.util.CacheComparator;
import hc.util.BaseResponsor;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class ServerUIUtil {
	public static final Color LIGHT_BLUE_BG = new Color(245, 250, 254);
	
	public static final Object LOCK = new Object();
	public static final int SCROLLPANE_VERTICAL_UNIT_PIXEL = 8;//缺省为1
	
	public static boolean useHARProject = PropertiesManager.isTrue(PropertiesManager.p_IsMobiMenu);
	private static BaseResponsor responsor;
	
	public static boolean isStarted(){
		return isStared;
	}
	
	public static ProjectContext buildProjectContext(final String id, final String ver,
			final RecycleRes recycleRes, final ProjResponser projResponser,
			final ProjClassLoaderFinder finder) {
		return new ProjectContext(id, ver, recycleRes, projResponser, finder);
	}
	
	public static BaseResponsor getResponsor(){
		CCoreUtil.checkAccess();
		
		return responsor;//synchronized (LOCK) 会使SIPManager.startLineOffForce互锁
	}
	
	/**
	 * 所有组件BorderLayout.NORTH
	 * @param components
	 * @param startIdx
	 * @param lastLayout
	 * @return
	 */
	public static JPanel buildNorthPanel(final JComponent[] components, int startIdx, final String lastLayout){
		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(components[startIdx], BorderLayout.NORTH);
		
		if(++startIdx < components.length){
			final JPanel out = buildNorthPanel(components, startIdx, lastLayout);
			if(startIdx == components.length - 1){
				panel.add(out, lastLayout);
			}else{
				panel.add(out, BorderLayout.CENTER);
			}
		}
		
		return panel;
	}
	
	public static JPanel buildDescPanel(final String htmlWithoutHTML){
		final JPanel descPanel = new JPanel(new BorderLayout());
		descPanel.add(new JLabel("<html>" + htmlWithoutHTML + "</html>"), BorderLayout.CENTER);
		descPanel.setBorder(new TitledBorder((String)ResourceUtil.get(9095)));
		return descPanel;
	}
	
	private static boolean isStared = false;
	
	public static BaseResponsor buildMobiUIResponsorInstance(final ExceptionCatcherToWindow ec){
		CCoreUtil.checkAccess();
		
		try {
			return new MobiUIResponsor(ec);
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
			return null;
		}
	}
	
	private static boolean isModiThirdLibs = false;
	
	public static boolean isModiThridLibs(){
		return isModiThirdLibs;
	}
	
	public static void notifyModiThridLibs(){
		CCoreUtil.checkAccess();
		
		isModiThirdLibs = true;
	}
	
	/**
	 * 
	 * @param owner 可能为null。因为MyFirst.har发布时，无此对象
	 * @param mobiUIRep
	 * @return
	 */
	public static BaseResponsor restartResponsorServer(final JFrame owner, final BaseResponsor mobiUIRep){
		CCoreUtil.checkAccess();
		
		synchronized (LOCK) {
			stop();
			
			if(useHARProject){
				if(isModiThirdLibs){
					isModiThirdLibs = false;
					ThirdlibManager.loadThirdLibs();
					ResourceUtil.getJRubyClassLoader(true);
				}
				
				BaseResponsor respo = null;
				try{
					respo = (mobiUIRep != null)?mobiUIRep:(BaseResponsor)buildMobiUIResponsorInstance(new ExceptionCatcherToWindow(owner));
					responsor = respo.checkAndReady(owner);
				}catch (final Throwable e) {
					if(respo instanceof MobiUIResponsor){
						final ExceptionCatcherToWindow ec = ((MobiUIResponsor)respo).ec;
						if(ec != null){
							ec.setThrowable(e);
						}
					}
					//出现构建失败
					ExceptionReporter.printStackTrace(e);
				}
				if(responsor == null){
					L.V = L.O ? false : LogManager.log("cancel all HAR projects.");
					cancelHAR(respo);
				}
				if(responsor == null){
					responsor = new DefaultUIResponsor();
				}
			}else{
				responsor = new DefaultUIResponsor();
			}
			try{
				CacheManager.clearBuffer();
				MSBAgent.resetDeviceSet();
				
				responsor.start();
				
				if(responsor instanceof MobiUIResponsor){
					L.V = L.O ? false : LogManager.log("successful start all HAR projects");
					((MobiUIResponsor)responsor).preLoadJRubyScripts();
				}
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
				
				//出现启动失败
				cancelHAR(responsor);
				
				if(responsor instanceof MobiUIResponsor){
					responsor = new DefaultUIResponsor();
					responsor.start();
				}
			}
			
			isStared = true;

			return responsor;
		}
	}

	private static void cancelHAR(final BaseResponsor respo) {
		if(respo != null && respo instanceof MobiUIResponsor){
			((MobiUIResponsor)respo).release();
		
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					final Window w = ProcessingWindowManager.showCenterMessage((String)ResourceUtil.get(9091));
					try{
						Thread.sleep(2000);
					}catch (final Exception e) {
					}
					w.dispose();
				}
			});
		}
	}

	public static void stop() {
		CCoreUtil.checkAccess();
		
		BaseResponsor snap;
		synchronized (LOCK) {
			snap = responsor;
			if(snap != null && isStared){
				isStared = false;
				responsor = null;
			}
		}
		
		if(snap != null){
			try{
				snap.stop();
				if(snap instanceof MobiUIResponsor){
					L.V = L.O ? false : LogManager.log("shutdown all projects.");
				}
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
			
			if(snap instanceof MobiUIResponsor){
				try{
					final int halfSleep = ResourceUtil.getIntervalSecondsForNextStartup() * 1000 / 2;
					
					Thread.sleep(halfSleep);
					System.gc();
					Thread.sleep(halfSleep);
					System.gc();
				}catch (final Exception e) {
				}
			}
		}
	}

	public static void transMenuWithCache(final J2SESession coreSS, final String menuData, final String projID) {
		final byte[] data = StringUtil.getBytes(menuData);
		
		final byte[] projIDbs = ByteUtil.getBytes(projID, IConstant.UTF_8);
		final String softUID = UserThreadResourceUtil.getMobileSoftUID(coreSS);
		final byte[] softUidBS = ByteUtil.getBytes(softUID, IConstant.UTF_8);
		final String urlID = CacheManager.ELE_URL_ID_MENU;
		final byte[] urlIDbs = CacheManager.ELE_URL_ID_MENU_BS;
		
		final CacheComparator menuCacheComparer = new CacheComparator(projID, softUID, urlID, projIDbs, softUidBS, urlIDbs) {
			@Override
			public void sendData(final Object[] paras) {
				response(coreSS, menuData);
			}
		};
		
		menuCacheComparer.encodeGetCompare(coreSS, true, data, 0, data.length, null);
	}
	
	public static boolean response(final CoreSession coreSS, final String out) {
		coreSS.context.send(MsgBuilder.E_CANVAS_MAIN, out);
		return true;
	}

	/**
	 * 注意：方法必须在一个线程内同步完成
	 * @param isQuery
	 * @param parent 可能为null。因为初始发布MyFirst.har
	 * @return
	 */
	public static boolean promptAndStop(final boolean isQuery, final JFrame parent){
		CCoreUtil.checkAccess();
		
		final boolean isPrompt = isServing();
		if(isPrompt){
			HttpUtil.notifyStopServer(isQuery, parent);
			
			J2SESessionManager.stopAllSession(true, true, false);
		}

		ServerUIUtil.stop();		
		
		return isPrompt;
	}

	public static boolean isServing() {
		return SessionManager.checkAtLeastOneMeet(ContextManager.STATUS_SERVER_SELF);
	}

	public static void restartResponsorServerDelayMode(final JFrame owner, final BaseResponsor mobiUIRep) {
		CCoreUtil.checkAccess();
		
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				restartResponsorServer(owner, mobiUIRep);
			}
		});
	}

	/**
	 * 如果解析失败，则返回null
	 * @param base64
	 * @return
	 */
	public static BufferedImage base64ToBufferedImage(final String base64) {
		final byte[] bs = ByteUtil.decodeBase64(base64);
		
		final ByteArrayInputStream bais = new ByteArrayInputStream(bs);
		try {
			return ImageIO.read(bais);
		} catch (final IOException e) {
		}
		return null;
	}

	/**
	 * 如果解析失败，则返回null
	 * @param bi
	 * @param iconByteArrayos
	 * @return
	 */
	public static String imageToBase64(final BufferedImage bi, final HCByteArrayOutputStream iconByteArrayos) {
		final int doubleSize = bi.getHeight() * bi.getWidth() * 2;
		byte[] iconBytes = iconByteArrayos.buf;//new byte[1024 * 20];
		
		if(iconBytes == null || iconBytes.length < doubleSize){
			if(iconBytes != null){
				ByteUtil.byteArrayCacher.cycle(iconBytes);
			}
			iconBytes = new byte[doubleSize];
			iconByteArrayos.reset(iconBytes, 0);
		}else{
			iconByteArrayos.reset();
		}
		try {
			ImageIO.write(bi, "png", iconByteArrayos);//Android环境下支持
		} catch (final Exception e1) {
			return null;
		}
		final int pngDataLen = iconByteArrayos.size();
		final byte[] data = new byte[pngDataLen];
		System.arraycopy(iconBytes, 0, data, 0, pngDataLen);
		return ByteUtil.encodeBase64(data);
	}

	public static HCByteArrayOutputStream buildForMaxIcon() {
		return new HCByteArrayOutputStream(UIUtil.ICON_MAX * UIUtil.ICON_MAX * 2);
	}

}
