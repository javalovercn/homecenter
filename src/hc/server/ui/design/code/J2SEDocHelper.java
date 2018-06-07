package hc.server.ui.design.code;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JPanel;

import hc.core.ContextManager;
import hc.core.L;
import hc.core.RootServerConnector;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.ui.design.Designer;
import hc.util.CheckSum;
import hc.util.IBiz;
import hc.util.MultiThreadDownloader;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

public class J2SEDocHelper {
	private static final String j2seDoc = "j2sedoc.jar";
	public static final String j2seDoc10_0_1 = "10.0.1";
	public static final String j2seDoc8 = "8";
	
	private static boolean isBuildIn = checkIsBuildIn();
	private static ClassLoader j2seDocLoader;

	private static boolean checkIsBuildIn() {
		final String docPath = DocHelper.buildClassDocPath(JPanel.class.getName().replace('.', '/'));
		final InputStream in = ResourceUtil.getResourceAsStream(docPath);
		final boolean out = in != null;
		if (out) {
			try {
				in.close();
			} catch (final Throwable e) {
			}
		}
		return out;
	}

	public static boolean isBuildIn() {
		return isBuildIn;
	}

	static boolean isDownloading;// 避免designer线程发起多次

	public static synchronized void checkJ2SEDocAndDownload() {
		if (j2seDocLoader != null) {
			return;
		}

		boolean isSuccDownDoc = false;
		final File docFile = new File(ResourceUtil.getBaseDir(), j2seDoc);

		//注意：如更新版本，请复制"j2sedoc.jar.10.0.1"
		final String j2seDocVersion = j2seDoc10_0_1;
		final String sha512J2seDoc = "45d95726a6db056309fe3dff1c0cf402ab173e85e754118ec0875f14feed0d4a970523f1a214f36d973814a07419ca8f17c8946fa6af2e1eaa3a1e9294ae7cbc";
		final String md5 = "4993a10af1409ae7be539378fa64da20";
		
		final String oldVersion = PropertiesManager.getValue(PropertiesManager.p_J2SEDocVersion);
		if (oldVersion == null || StringUtil.higher(j2seDocVersion, oldVersion)) {
			if (isDownloading) {
				return;
			}
			isDownloading = true;
			docFile.delete();
			final CheckSum checkSum = new CheckSum(md5, sha512J2seDoc);
			final String downloadURL = RootServerConnector.HTTS_HC_44X + "/download/" + j2seDoc + "." + j2seDocVersion;
			final MultiThreadDownloader mtd = new MultiThreadDownloader();
			final Vector<String> urls = new Vector<String>(1);
			urls.add(downloadURL);

			final Boolean[] isDone = { false };

			final IBiz succBiz = new IBiz() {
				@Override
				public void start() {
					isDone[0] = true;
					synchronized (isDone) {
						isDone.notify();
					}
					PropertiesManager.setValue(PropertiesManager.p_J2SEDocVersion, j2seDocVersion);
					PropertiesManager.saveFile();
				}

				@Override
				public void setMap(final HashMap map) {
				}
			};
			final IBiz failBiz = new IBiz() {
				@Override
				public void start() {
					synchronized (isDone) {
						isDone.notify();
					}
				}

				@Override
				public void setMap(final HashMap map) {
				}
			};
			mtd.download(urls, docFile, checkSum, succBiz, failBiz, false, false);// isVisible==false，改为后台下载

			synchronized (isDone) {
				try {
					isDone.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (isDone[0] == false) {
				docFile.delete();
				L.V = L.WShop ? false : LogManager.log("fail to download j2sedoc.jar");

				isDownloading = false;
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						checkJ2SEDocAndDownload();
					}
				});
				return;
			} else {
				isSuccDownDoc = true;
				LogManager.log("successful download j2sedoc.jar!");
			}
		}
		try {
			final URL[] files = { docFile.toURI().toURL() };
			j2seDocLoader = new URLClassLoader(files);
		} catch (final Throwable e) {
			e.printStackTrace();
		}

		if (isSuccDownDoc) {
			final Designer designer = Designer.getInstance();
			if (designer != null) {
				designer.codeHelper.resetAll();
			}
		}
	}

	public static boolean isJ2SEDocReady() {
		return j2seDocLoader != null;
	}

	/**
	 * null means j2sedoc is not ready.
	 */
	public static InputStream getDocStream(final String docPath) {
		if (isBuildIn) {
			return ResourceUtil.getResourceAsStream(docPath);
		} else {
			if (j2seDocLoader != null) {
				return j2seDocLoader.getResourceAsStream(docPath);
			} else {
				return null;
			}
		}
	}
}
