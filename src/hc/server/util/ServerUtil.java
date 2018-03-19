package hc.server.util;

import java.io.File;
import java.util.Vector;

import javax.swing.JPanel;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.server.JRubyInstaller;
import hc.server.PlatformManager;
import hc.server.PlatformService;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

public class ServerUtil {

	public static ClassLoader buildProjClassPath(final File deployPath, final ClassLoader jrubyClassLoader, final String projID) {
		CCoreUtil.checkAccess();

		final Vector<File> jars = new Vector<File>();
		final String[] subFileNames = deployPath.list();
		for (int i = 0; subFileNames != null && i < subFileNames.length; i++) {
			final String lowerCaseFileName = subFileNames[i].toLowerCase();
			if (lowerCaseFileName.endsWith(ResourceUtil.EXT_JAR) && (lowerCaseFileName.endsWith(ResourceUtil.EXT_DEX_JAR) == false)) {
				jars.add(new File(deployPath, subFileNames[i]));
			}
		}
		final File[] jars_arr = new File[jars.size()];
		return PlatformManager.getService().loadClasses(jars.toArray(jars_arr), jrubyClassLoader, false, ResourceUtil.USER_PROJ + projID);
	}

	public static ClassLoader buildProjClassLoader(final File libAbsPath, final String projID) {
		return buildProjClassPath(libAbsPath, getJRubyClassLoader(false), projID);
	}

	static ClassLoader rubyAnd3rdLibsClassLoaderCache;

	public static synchronized void clearJRubyClassLoader() {
		CCoreUtil.checkAccess();

		rubyAnd3rdLibsClassLoaderCache = null;
	}

	public static synchronized ClassLoader getJRubyClassLoader(final boolean forceRebuild) {
		CCoreUtil.checkAccess();

		if (rubyAnd3rdLibsClassLoaderCache == null || forceRebuild) {
		} else {
			return rubyAnd3rdLibsClassLoaderCache;
		}
		// PlatformManager.getService().closeLoader(rubyAnd3rdLibsClassLoaderCache);

		rubyAnd3rdLibsClassLoaderCache = buildJRubyClassLoader();
		if (rubyAnd3rdLibsClassLoaderCache == null) {
			final String message = "Error to get JRuby/3rdLibs ClassLoader!";
			LogManager.errToLog(message);
			final JPanel panel = App.buildMessagePanel(message, App.getSysIcon(App.SYS_ERROR_ICON));
			App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getErrorI18N(), false, null, null, null, null, null, false, true, null, false,
					false);// JFrame
		} else {
			LogManager.log("Successful (re) create JRuby engine classLoader.");
		}

		return rubyAnd3rdLibsClassLoaderCache;
	}

	private static ClassLoader buildJRubyClassLoader() {
		final File jruby = new File(ResourceUtil.getBaseDir(), JRubyInstaller.jrubyjarname);

		final PlatformService service = PlatformManager.getService();

		service.setJRubyHome(PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer), jruby.getAbsolutePath());
		final File[] files;
		if (ResourceUtil.isAndroidServerPlatform()) {
			final File[] rubotoFile = service.getRubotoAndDxFiles();// in J2SE,
																	// return
																	// null;
			files = new File[] { jruby, rubotoFile[0], rubotoFile[1] };
		} else {
			files = new File[] { jruby };
		}
		return service.loadClasses(files, service.get3rdAndServClassLoader(null), true, "hc.jruby");
	}
}
