package hc.server.ui.design.hpj;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.swing.tree.DefaultMutableTreeNode;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.StringUtil;
import hc.server.StarterManager;
import hc.server.ui.ProjClassLoaderFinder;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SimuMobile;
import hc.server.ui.design.LinkProjectStore;
import hc.server.ui.design.NativeOSManager;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.server.util.HCLimitSecurityManager;
import hc.util.BaseResponsor;
import hc.util.I18NStoreableHashMapWithModifyFlag;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

public class HCjar {
	public static final String ITEM_NAME = "Name";
	public static final String ITEM_I18N_NAME = "I18nTitle";
	public static final String ITEM_TYPE = "Type";
	public static final String ITEM_URL = "Url";
	public static final String ITEM_IMAGE = "Image";
	public static final String ITEM_LISTENER = "Listener";
	public static final String ITEM_EXTENDMAP = "ExtendMap";

	// 注意：不能用PROC_NAME，极易与PROJ_NAME混淆
	public static final String PROCESSOR_NAME = "ProcName";
	public static final String PROCESSOR_LISTENER = "ProcListener";

	public static final String MAP_FILE_PRE = "__MaP_fILe_pRE_";
	public static final String MAP_FILE_TYPE_PRE = "__MaP_fILe_tYpE_pRE_";
	public static final String VERSION_FILE_PRE = "__VeR_fILe_pRE_";
	public static final String HOMECENTER_VER = "HomeCenter.Ver";
	public static final String JRUBY_VER = "JRuby.Ver";
	public static final String JRE_VER = "JRE.Ver";
	public static final String LAST_3_VER = "Last3.Ver";
	public static final String PROJ_NAME = "Project.Name";
	public static final String PROJ_I18N_NAME = "Project.I18NName";
	public static final String PROJ_ID = "Project.ID";
	public static final String PROJ_NEXT_NODE_IDX = "Project.NodeIdx";
	public static final String PROJ_VER = "Project.Ver";
	public static final String PROJ_LAST_SIGNED_VER = "Project.LastSignedVer";
	public static final String PROJ_UPGRADE_URL = "Project.upgradeURL";
	public static final String PROJ_EXCEPTION_REPORT_URL = "Project.exceptionReportURL";

	public static final String PROJ_COMPACT_DAYS = "Project.CompactDays";
	public static final String PROJ_CONTACT = "Project.Contact";
	public static final String PROJ_COPYRIGHT = "Project.Copyright";
	public static final String PROJ_DESC = "Project.Desc";
	public static final String PROJ_LICENSE = "Project.License";
	public static final String PROJ_STYLES = "Project.Styles";

	public static final String IDX_PATTERN = "(\\d+)";

	public static final String MENU_NUM = "Menu.Num";
	public static final String SHARE_JRUBY_FILES_NUM = "ShareJRuby.Num";
	public static final String SHARE_JRUBY_FILE_NAME = "ShareJRuby." + IDX_PATTERN + ".File";
	public static final String SHARE_JRUBY_FILE_CONTENT = "ShareJRuby." + IDX_PATTERN + ".Content";
	public static final String SHARE_NATIVE_FILES_NUM = "Native.Num";
	public static final String SHARE_NATIVE_FILE_NAME = "Native." + IDX_PATTERN + ".File";
	public static final String SHARE_NATIVE_FILE_OS_MASK = "Native.OS." + IDX_PATTERN + ".Mask";

	// public static final String SHARE_JAR_FILES_NUM = "ShareJar.Num";
	// public static final String SHARE_JAR_FILE_NAME = "ShareJar." +
	// IDX_PATTERN + ".File";
	// public static final String SHARE_JAR_FILE_CONTENT = "ShareJar." +
	// IDX_PATTERN + ".Content";

	public static final String ROBOT_NUM = "Robot.Num";
	public static final String ROBOT_ITEM_HEADER = "Robot.Item.";

	public static final String PERMISSION_HEADER = "Permission.";
	public static final String PERMISSION_WRITE = PERMISSION_HEADER + "Write";
	public static final String PERMISSION_EXECUTE = PERMISSION_HEADER + "Exec";
	public static final String PERMISSION_DELETE = PERMISSION_HEADER + "Delete";
	public static final String PERMISSION_EXIT = PERMISSION_HEADER + "Exit";

	private static final String PERMISSION_SYS_PROP = PERMISSION_HEADER + "SysProp.";
	public static final String PERMISSION_SYS_PROP_READ = PERMISSION_SYS_PROP + "Read";
	public static final String PERMISSION_SYS_PROP_WRITE = PERMISSION_SYS_PROP + "Write";

	public static final String PERMISSION_LOCATION = PERMISSION_HEADER + "Location";
	public static final String PERMISSION_SCRIPT_PANEL = PERMISSION_HEADER + "ScriptPanel";
	public static final String PERMISSION_LOAD_LIB = PERMISSION_HEADER + "LoadLib";
	public static final String PERMISSION_ROBOT = PERMISSION_HEADER + "Robot";
	// public static final String PERMISSION_LISTEN_ALL_AWT_EVNTS =
	// PERMISSION_HEADER + "AllAWTEvents";
	// public static final String PERMISSION_ACCESS_CLIPBOARD =
	// PERMISSION_HEADER + "Clipboard";
	public static final String PERMISSION_SHUTDOWNHOOKS = PERMISSION_HEADER + "Hook";
	public static final String PERMISSION_SETIO = PERMISSION_HEADER + "SetIO";
	public static final String PERMISSION_MEMBER_ACCESS_SYSTEM = PERMISSION_HEADER
			+ "MemberAccessSystem";
	public static final String PERMISSION_ACCESS_PRIVATE_ADDRESS = PERMISSION_HEADER
			+ "PrivateAddress";
	public static final String PERMISSION_SET_FACTORY = PERMISSION_HEADER + "SetFactory";

	private static final String PERMISSION_SOCK_HEADER = PERMISSION_HEADER + "Sock.";
	public static final String PERMISSION_SOCK_ITEM_HEADER = PERMISSION_SOCK_HEADER + "Item.";
	public static final String PERMISSION_SOCK_NUM = PERMISSION_SOCK_HEADER + "Num";
	public static final String PERMISSION_SOCK_LIMIT_ON = PERMISSION_SOCK_HEADER + "LmtON";

	public static final String DEVICE_NUM = "Device.Num";
	public static final String DEVICE_ITEM_HEADER = "Device.Item.";

	public static final String CONVERTER_NUM = "Converter.Num";
	public static final String CONVERTER_ITEM_HEADER = "Converter.Item.";

	public static final String MAIN_MENU_IDX_PRE = "MainMenu.Idx";
	public static final String MENU_ID = "Menu." + IDX_PATTERN + ".Id";
	public static final String MENU_NAME = "Menu." + IDX_PATTERN + ".Name";
	public static final String MENU_COL_NUM = "Menu." + IDX_PATTERN + ".ColNum";
	public static final String MENU_CHILD_COUNT = "Menu." + IDX_PATTERN + ".ChildCount";
	public static final String MENU_ITEM_HEADER = "Menu." + IDX_PATTERN + ".Item.";

	// public static final String RANDOM_SHARE_FOLDER = "RandomFolder";
	// public static final String SCRIPT_NUM = "Script.Num";
	// public static final String SCRIPT_SRC = "Script." + IDX_PATTERN + ".Src";
	// public static final String SCRIPT_FILE_NAME = "script_" + IDX_PATTERN;

	public static final String JarConfigEntry = "config.properties";

	public static byte[] readFromInputStream(final InputStream jarInputStream) throws Throwable {
		final ByteArrayOutputStream classData = new ByteArrayOutputStream();
		int readSize = 0;
		final int BUFFER_SIZE = 1024 * 4;
		final byte[] buffer = new byte[BUFFER_SIZE];
		try {
			while (jarInputStream.available() != 0) {
				// 注：有些时候，即使jarInputStream有超过BUFFER_SIZE的数据
				// 也有可能读到的数据少于BUFFER_SIZE
				readSize = jarInputStream.read(buffer, 0, BUFFER_SIZE);
				// jarInputStream.available()好像不起作用，因而需要加入一下代码
				if (readSize < 0)
					break;
				classData.write(buffer, 0, readSize);
			}
		} catch (final Throwable e) {
			try {
				jarInputStream.close();
			} catch (final Exception ex) {
			}
			throw e;
		}
		return classData.toByteArray();
	}

	/**
	 * final String url =
	 * "http://homecenter.mobi/download/sample.har";或本地resourcepath
	 * 
	 * @param url
	 * @return
	 */
	public static final Map<String, Object> loadJar(final URL url) {
		CCoreUtil.checkAccess();

		final Properties p = new Properties();
		final Hashtable<String, Object> mapString = new Hashtable<String, Object>();// 必须线程安全，且Object可能为StringValue

		JarInputStream jis = null;
		try {
			jis = new JarInputStream(url.openStream());
			JarEntry je;
			while ((je = jis.getNextJarEntry()) != null) {
				loadToMap(je, jis, p, mapString, true);
				jis.closeEntry();
			}
		} catch (final Throwable e) {
		} finally {
			try {
				jis.close();
			} catch (final Exception e) {
			}
		}

		pushStringMap(p, mapString);
		return mapString;
	}

	private static void loadToMap(final JarEntry je, final InputStream is, final Properties p,
			final Hashtable<String, Object> mapString, final boolean loadFileByteArray)
			throws Throwable {
		final String jarEnterName = je.getName();
		if (jarEnterName.startsWith("META-INF", 0)) {
			return;
		} else if (jarEnterName.equals(JarConfigEntry)) {
			pushStringMap(p, mapString, is);
		} else {
			if (je.isDirectory()) {
				return;
			}
			// if(jarEnterName.endsWith(".jar"))
			// 如jar, png, au
			if (loadFileByteArray) {
				final byte[] data = readFromInputStream(is);
				mapString.put(MAP_FILE_PRE + jarEnterName, data);
			}
		}
	}

	private static boolean setNullDefaultValue(final Hashtable<String, Object> map,
			final String key, final String defaultValue) {
		if (map.get(key) == null) {
			// System.out.println("set key["+key+"] to default : " +
			// defaultValue);
			map.put(key, defaultValue);
			return true;
		}
		return false;
	}

	public static void initMap(final Hashtable<String, Object> map) {
		if (setNullDefaultValue(map, PROJ_NAME, "My Project")) {
			final ContextSecurityConfig csc = new ContextSecurityConfig("");
			csc.buildNewProjectPermissions();
			HCjarHelper.setBoolean(map, HCjar.PERMISSION_SOCK_LIMIT_ON, true);
			ContextSecurityConfig.copyPermissionsFromConfig(map, csc);
		}
		setNullDefaultValue(map, PROJ_I18N_NAME, "");
		setNullDefaultValue(map, PROJ_NEXT_NODE_IDX, "1");
		setNullDefaultValue(map, PROJ_ID,
				HPProject.convertProjectIDFromName((String) map.get(PROJ_NAME)));
		setNullDefaultValue(map, PROJ_VER, HPProject.DEFAULT_VER);
		setNullDefaultValue(map, PROJ_LAST_SIGNED_VER, "0");
		setNullDefaultValue(map, PROJ_COMPACT_DAYS, String.valueOf(365 / 2));
		setNullDefaultValue(map, PROJ_UPGRADE_URL, "");
		setNullDefaultValue(map, PROJ_EXCEPTION_REPORT_URL, "");
		setNullDefaultValue(map, PROJ_CONTACT, "");
		setNullDefaultValue(map, PROJ_COPYRIGHT, "");
		setNullDefaultValue(map, PROJ_DESC, "");
		setNullDefaultValue(map, PROJ_LICENSE, "");
		setNullDefaultValue(map, PROJ_STYLES, "");

		if (map.get(HCjar.PERMISSION_WRITE) == null) {
			final ContextSecurityConfig csc = new ContextSecurityConfig("");
			csc.buildDefaultPermissions();
			ContextSecurityConfig.copyPermissionsFromConfig(map, csc);
		}

		final Object object = map.get(MENU_NUM);
		if (object != null) {
			final int menuNum = Integer.parseInt((String) object);
			for (int idx = 0; idx < menuNum; idx++) {
				final String key = replaceIdxPattern(MENU_COL_NUM, idx);
				setNullDefaultValue(map, key, "0");

				final Object menuItemObj = map
						.get(HCjar.replaceIdxPattern(HCjar.MENU_CHILD_COUNT, idx));
				if (menuItemObj != null) {
					final int itemCount = Integer.parseInt((String) menuItemObj);
					final String Iheader = HCjar.replaceIdxPattern(HCjar.MENU_ITEM_HEADER, idx);
					for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
						final String header = Iheader + itemIdx + ".";
						final String key2 = header + HCjar.ITEM_EXTENDMAP;
						setNullDefaultValue(map, key2, "");
					}
				}
			}
		}
	}

	public static final Map<String, Object> loadHarFromLPS(final LinkProjectStore lps) {
		final File har_load = new File(new File(ResourceUtil.getBaseDir(), lps.getHarParentDir()),
				lps.getHarFile());
		return HCjar.loadHar(har_load, false);
	}

	public static final Map<String, Object> loadHar(final File jarfile,
			final boolean loadFileByteArray) {
		JarFile jf = null;
		final Properties p = new Properties();
		final Hashtable<String, Object> mapString = new Hashtable<String, Object>();
		try {
			// if(jarfile.exists() == false){
			// throw new Exception("File["+jarfile.getName()+"] not exists!");
			// }

			jf = new JarFile(jarfile, false);

			for (final Enumeration<JarEntry> em1 = jf.entries(); em1.hasMoreElements();) {
				final JarEntry je = em1.nextElement();
				final InputStream is = jf.getInputStream(je);
				try {
					loadToMap(je, is, p, mapString, loadFileByteArray);
				} finally {
					is.close();
				}
			}
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		} finally {
			try {
				jf.close();
			} catch (final Exception e) {
			}
		}
		pushStringMap(p, mapString);
		return mapString;
	}

	private static void pushStringMap(final Properties p, final Hashtable<String, Object> map,
			final InputStream is) throws IOException {
		p.load(is);
		pushStringMap(p, map);
	}

	private static void pushStringMap(final Properties p, final Hashtable<String, Object> map) {
		for (final Map.Entry<Object, Object> entry : p.entrySet()) {
			map.put((String) entry.getKey(), entry.getValue());
		}
		if (map.isEmpty() == false) {
			initMap(map);
		}
	}

	public static final void toHar(final Map<String, Object> map, final File jarfile) {

		final Properties p = new Properties();

		if (jarfile.exists()) {
			jarfile.delete();
		}

		JarOutputStream jaros = null;
		try {
			jaros = buildJarOutputStream(jarfile);

			// 去掉非String型的value
			Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				final Map.Entry<String, Object> entry = it.next();
				final String keyName = entry.getKey();
				if (keyName.startsWith(MAP_FILE_PRE, 0)) {
					final String fileName = keyName.substring(MAP_FILE_PRE.length());
					jaros.putNextEntry(new JarEntry(fileName));
					jaros.write((byte[]) entry.getValue());
					jaros.closeEntry();

					// 必须时行remove，以保留串型数据，而非二进制数据
					map.remove(keyName);

					it = map.entrySet().iterator();
				}
			}

			p.putAll(map);

			jaros.putNextEntry(new JarEntry(JarConfigEntry));
			p.store(jaros, "");
			jaros.closeEntry();

		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		} finally {
			try {
				jaros.close();
			} catch (final Throwable e) {
			}
		}

	}

	/**
	 * 返回主菜单的Menu结点，如果是新建，则可能返回null
	 * 
	 * @param map
	 * @param root
	 * @return
	 */
	public static final DefaultMutableTreeNode toNode(final Map<String, Object> map,
			final DefaultMutableTreeNode root, final DefaultMutableTreeNode msbFolder,
			final DefaultMutableTreeNode eventFolder, final DefaultMutableTreeNode[] scriptFolder) {
		DefaultMutableTreeNode out_mainMenuNode = null;
		// 装填ROOT
		{
			final ContextSecurityConfig csc = ContextSecurityConfig.getPermissionFromHARMap(map);
			ContextSecurityManager.putContextSecurityConfig(
					(ThreadGroup) HCLimitSecurityManager.getTempLimitRecycleRes().threadPool
							.getThreadGroup(),
					csc);

			final String projID = (String) map.get(PROJ_ID);
			final String projVer = (String) map.get(PROJ_VER);

			final ProjectContext context = ServerUIUtil.buildProjectContext(projID, projVer,
					HCLimitSecurityManager.getTempLimitRecycleRes(), null,
					new ProjClassLoaderFinder() {
						@Override
						public ClassLoader findProjClassLoader() {
							return SimuMobile.getRunTestEngine().getProjClassLoader();
						}
					});
			csc.setProjectContext(context);

			root.setUserObject(new HPProject(HPNode.MASK_ROOT, (String) map.get(PROJ_NAME),
					(String) map.get(PROJ_I18N_NAME), projID, projVer, csc, map));

			MenuManager.setNextNodeIdx(Integer.parseInt((String) map.get(PROJ_NEXT_NODE_IDX)));
		}

		try {// 捕获数据结构可能被异常修改后，可能导致的不正确！

			// 装填Menu
			final Object object = map.get(MENU_NUM);
			if (object != null) {
				final int menuNum = Integer.parseInt((String) object);
				final int MainMenuIdx = Integer.parseInt((String) map.get(MAIN_MENU_IDX_PRE));

				for (int idx = 0; idx < menuNum; idx++) {
					String menuName = (String) map.get(replaceIdxPattern(MENU_NAME, idx));
					menuName = HPNode.NODE_MENU;// 使用统一Menu菜单夹
					final String menuColNum = (String) map
							.get(replaceIdxPattern(MENU_COL_NUM, idx));
					final HPMenu menu = new HPMenu(
							(String) map.get(replaceIdxPattern(MENU_ID, idx)), HPNode.MASK_MENU,
							menuName, (menuColNum == null) ? 0 : Integer.parseInt(menuColNum),
							MainMenuIdx == idx);
					final DefaultMutableTreeNode menuNode = new DefaultMutableTreeNode(menu);

					// 四大事件
					buildMenuEventNodes(map, idx, root, eventFolder);

					root.add(menuNode);

					if (menu.isMainMenu) {
						out_mainMenuNode = menuNode;
					}

					// 装填MenuItem
					{
						final Object menuItemObj = map
								.get(replaceIdxPattern(MENU_CHILD_COUNT, idx));
						if (menuItemObj != null) {
							final int itemCount = Integer.parseInt((String) menuItemObj);
							final String Iheader = replaceIdxPattern(MENU_ITEM_HEADER, idx);
							for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
								final String header = Iheader + itemIdx + ".";
								final HPMenuItem userObject = new HPMenuItem(
										Integer.parseInt((String) map.get(header + ITEM_TYPE)),
										(String) map.get(header + ITEM_NAME),
										HCjar.buildI18nMapFromSerial(
												(String) map.get(header + ITEM_I18N_NAME)),
										(String) map.get(header + ITEM_URL),
										(String) map.get(header + ITEM_IMAGE));

								userObject.listener = (String) map.get(header + ITEM_LISTENER);
								userObject.extendMap
										.restore((String) map.get(header + ITEM_EXTENDMAP));

								final DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(
										userObject);
								menuNode.add(itemNode);
							}
						}
					} // end 装填MenuItem

					// MSB_FOLDER
					root.add(msbFolder);
					buildMSBNodes(map, msbFolder);
				}
			}

			{
				// 装填ShareJRubyFile
				final Object shareJRubyFileNum = map.get(SHARE_JRUBY_FILES_NUM);
				if (shareJRubyFileNum != null) {
					final int shareNum = Integer.parseInt((String) shareJRubyFileNum);

					for (int idx = 0; idx < shareNum; idx++) {
						final String fileName = (String) map
								.get(replaceIdxPattern(SHARE_JRUBY_FILE_NAME, idx));
						final String fileContent = (String) map
								.get(replaceIdxPattern(SHARE_JRUBY_FILE_CONTENT, idx));
						final HPShareJRuby userObject = new HPShareJRuby(HPNode.MASK_SHARE_RB,
								fileName);
						userObject.content = fileContent;
						final DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(
								userObject);
						scriptFolder[0].add(itemNode);
					}
				}
			}

			{
				final Object shareNativeFileNum = map.get(SHARE_NATIVE_FILES_NUM);
				if (shareNativeFileNum != null) {
					final int shareNum = Integer.parseInt((String) shareNativeFileNum);

					for (int idx = 0; idx < shareNum; idx++) {
						final String fileName = (String) map
								.get(replaceIdxPattern(SHARE_NATIVE_FILE_NAME, idx));
						final int osMask = NativeOSManager.getOSMaskFromMap(map, idx);
						final byte[] fileContent = (byte[]) map.get(MAP_FILE_PRE + fileName);
						final HPShareNative userObject = new HPShareNative(HPNode.MASK_SHARE_NATIVE,
								fileName, osMask);
						userObject.content = fileContent;
						userObject.ver = (String) map.get(VERSION_FILE_PRE + fileName);
						final DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(
								userObject);
						scriptFolder[2].add(itemNode);
					}
				}
			}

			// 装填shareFileResource，如jar, png, au
			final DefaultMutableTreeNode shareJarFolder = scriptFolder[1];
			for (final Map.Entry<String, Object> entry : map.entrySet()) {
				final String keyName = entry.getKey();
				if (keyName.startsWith(MAP_FILE_PRE, 0)) {
					final String realName = keyName.substring(MAP_FILE_PRE.length());
					String type = (String) map.get(MAP_FILE_TYPE_PRE + realName);
					if (type == null) {
						type = HPNode.MAP_FILE_JAR_TYPE;
					}

					final int typeInt = Integer.parseInt(type);
					if (HPNode.isNodeType(typeInt, HPNode.MASK_RESOURCE_ITEM)) {
						final HPShareJar jar = new HPShareJar(typeInt, realName);
						jar.ver = (String) map.get(VERSION_FILE_PRE + realName);

						jar.ID = realName;
						jar.content = (byte[]) entry.getValue();

						final DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(jar);
						shareJarFolder.add(itemNode);
					}
				}
			}
		} catch (final Throwable e) {

		}
		return out_mainMenuNode;
	}

	public static DefaultMutableTreeNode buildMSBNodes(final Map<String, Object> map,
			final DefaultMutableTreeNode eventFold) {
		{
			final int msbSize = HCjarHelper.getRobotNum(map);
			for (int idx = 0; idx < msbSize; idx++) {
				final HPProcessor eventItem = new HPProcessor(HPNode.MASK_MSB_ROBOT,
						HCjarHelper.getRobotNameAtIdx(map, idx));
				eventItem.listener = HCjarHelper.getRobotListenerAtIdx(map, idx);
				final DefaultMutableTreeNode eventItemNode = new DefaultMutableTreeNode(eventItem);
				eventFold.add(eventItemNode);
			}
		}
		{
			final int msbSize = HCjarHelper.getConverterNum(map);
			for (int idx = 0; idx < msbSize; idx++) {
				final HPProcessor eventItem = new HPProcessor(HPNode.MASK_MSB_CONVERTER,
						HCjarHelper.getConverterNameAtIdx(map, idx));
				eventItem.listener = HCjarHelper.getConverterListenerAtIdx(map, idx);
				final DefaultMutableTreeNode eventItemNode = new DefaultMutableTreeNode(eventItem);
				eventFold.add(eventItemNode);
			}
		}
		{
			final int msbSize = HCjarHelper.getDeviceNum(map);
			for (int idx = 0; idx < msbSize; idx++) {
				final HPProcessor eventItem = new HPProcessor(HPNode.MASK_MSB_DEVICE,
						HCjarHelper.getDeviceNameAtIdx(map, idx));
				eventItem.listener = HCjarHelper.getDeviceListenerAtIdx(map, idx);
				final DefaultMutableTreeNode eventItemNode = new DefaultMutableTreeNode(eventItem);
				eventFold.add(eventItemNode);
			}
		}

		return eventFold;
	}

	public static void buildMenuEventNodes(final Map<String, Object> map, final int menuIdx,
			final DefaultMutableTreeNode rootNode, final DefaultMutableTreeNode eventFolder) {
		eventFolder.removeAllChildren();
		eventFolder.removeFromParent();

		rootNode.add(eventFolder);

		for (int i = 0; i < BaseResponsor.SCRIPT_EVENT_LIST.length; i++) {
			final HPMenuEventItem eventItem = new HPMenuEventItem(HPNode.MASK_EVENT_ITEM,
					BaseResponsor.SCRIPT_EVENT_LIST[i]);
			final DefaultMutableTreeNode eventItemNode = new DefaultMutableTreeNode(eventItem);
			eventFolder.add(eventItemNode);

			eventItem.content = (String) map.get(buildEventMapKey(menuIdx, eventItem.name));
			if (eventItem.content == null) {
				eventItem.content = "";
			}
		}
	}

	public static final void buildLast3Ver(final Map<String, Object> map) {
		final String last3Split = StringUtil.SPLIT_LEVEL_2_JING;

		final String last3 = map.get(HCjar.JRE_VER) + last3Split + map.get(HCjar.HOMECENTER_VER)
				+ last3Split + map.get(HCjar.JRUBY_VER);

		map.put(LAST_3_VER, last3);
	}

	/**
	 * [JRE_VER] + [HOMECENTER_VER] + [JRUBY_VER]
	 * 
	 * @param map
	 * @return if null returns null
	 */
	public static final String[] splitLast3Ver(final Map<String, Object> map) {
		final String last3 = (String) map.get(LAST_3_VER);
		if (last3 == null) {
			return null;
		} else {
			return StringUtil.splitToArray(last3, StringUtil.SPLIT_LEVEL_2_JING);
		}
	}

	public static final Map<String, Object> toMap(final DefaultMutableTreeNode root,
			final DefaultMutableTreeNode msbFold, final DefaultMutableTreeNode eventFold,
			final DefaultMutableTreeNode[] folders, final HashMap<String, Object> map)
			throws NodeInvalidException {
		map.put(HOMECENTER_VER, StarterManager.getHCVersion());
		map.put(JRE_VER, String.valueOf(App.getJREVer()));
		final String jruby_ver = PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer);
		if (jruby_ver != null) {
			map.put(JRUBY_VER, jruby_ver);
		}

		return toMap(map, root, msbFold, eventFold, folders);
	}

	private static final Map<String, Object> toMap(final HashMap<String, Object> map,
			final DefaultMutableTreeNode root, final DefaultMutableTreeNode msbFold,
			final DefaultMutableTreeNode eventFolder, final DefaultMutableTreeNode[] folders)
			throws NodeInvalidException {
		final int childCount = root.getChildCount();
		checkSameNameNode(root, childCount);

		final HPNode item = (HPNode) root.getUserObject();
		if (item.type == HPNode.MASK_ROOT) {
			map.put(PROJ_NAME, item.name);
			map.put(PROJ_I18N_NAME, item.i18nMap.toSerial());
			final HPProject hpProject = (HPProject) item;
			map.put(PROJ_ID, hpProject.id);
			map.put(PROJ_VER, hpProject.ver);
			map.put(PROJ_LAST_SIGNED_VER, hpProject.lastSignedVer);
			map.put(PROJ_COMPACT_DAYS, hpProject.compactDays);
			map.put(PROJ_NEXT_NODE_IDX, String.valueOf(MenuManager.getCurrNodeIdx()));
			map.put(PROJ_UPGRADE_URL, hpProject.upgradeURL);
			map.put(PROJ_EXCEPTION_REPORT_URL, hpProject.exceptionURL);
			map.put(PROJ_CONTACT, hpProject.contact);
			map.put(PROJ_COPYRIGHT, hpProject.copyright);
			map.put(PROJ_DESC, hpProject.desc);
			map.put(PROJ_LICENSE, hpProject.license);
			map.put(PROJ_STYLES, hpProject.styles);

			ContextSecurityConfig.copyPermissionsFromConfig(map, hpProject.csc);
		} else if (item.type == HPNode.MASK_MENU) {
			final int idx = addOne(map, MENU_NUM);
			final String menuPattern = replaceIdxPattern(MENU_NAME, idx);
			map.put(menuPattern, item.name);
			map.put(replaceIdxPattern(MENU_COL_NUM, idx), String.valueOf(((HPMenu) item).colNum));

			final HPMenu hpmenu = (HPMenu) item;
			if (hpmenu.isMainMenu) {
				map.put(MAIN_MENU_IDX_PRE, String.valueOf(idx));
			}
			{
				// 四大事件：start_proj, mobile_login, mobile_logout,
				// shutdown_proj...
				final int size = eventFolder.getChildCount();
				for (int i = 0; i < size; i++) {
					final HPMenuEventItem eventItem = (HPMenuEventItem) ((DefaultMutableTreeNode) eventFolder
							.getChildAt(i)).getUserObject();

					map.put(buildEventMapKey(idx, eventItem.name), eventItem.content);
				}
			}
			{
				final int size = msbFold.getChildCount();
				checkSameNameNode(msbFold, size);
				// Iot
				for (int i = 0; i < size; i++) {
					final DefaultMutableTreeNode childAt = (DefaultMutableTreeNode) msbFold
							.getChildAt(i);
					final HPProcessor menuItem = (HPProcessor) childAt.getUserObject();
					String header = "";
					int ito_idx = -1;
					if (menuItem.type == HPNode.MASK_MSB_ROBOT) {
						ito_idx = addOne(map, ROBOT_NUM);
						header = ROBOT_ITEM_HEADER;
					} else if (menuItem.type == HPNode.MASK_MSB_DEVICE) {
						ito_idx = addOne(map, DEVICE_NUM);
						header = DEVICE_ITEM_HEADER;
					} else if (menuItem.type == HPNode.MASK_MSB_CONVERTER) {
						ito_idx = addOne(map, CONVERTER_NUM);
						header = CONVERTER_ITEM_HEADER;
					}
					header += ito_idx + ".";
					map.put(header + PROCESSOR_NAME, menuItem.name);
					map.put(header + PROCESSOR_LISTENER, String.valueOf(menuItem.listener));
				}
			}
			map.put(replaceIdxPattern(MENU_ID, idx), hpmenu.menuID);
			final int size = childCount;
			map.put(replaceIdxPattern(MENU_CHILD_COUNT, idx), String.valueOf(size));
			final String Iheader = replaceIdxPattern(MENU_ITEM_HEADER, idx);

			for (int i = 0; i < size; i++) {
				final String header = Iheader + i + ".";

				final HPMenuItem menuItem = (HPMenuItem) ((DefaultMutableTreeNode) root
						.getChildAt(i)).getUserObject();

				map.put(header + ITEM_NAME, menuItem.name);
				map.put(header + ITEM_I18N_NAME, menuItem.i18nMap.toSerial());
				map.put(header + ITEM_TYPE, String.valueOf(menuItem.type));

				map.put(header + ITEM_URL, menuItem.url);
				map.put(header + ITEM_IMAGE, menuItem.imageData);
				map.put(header + ITEM_LISTENER, menuItem.listener);
				map.put(header + ITEM_EXTENDMAP, menuItem.extendMap.toSerial());
				// if(menuItem.listener != null && menuItem.listener.length() >
				// 0){
				// int SciptIdx = addOne(map, SCRIPT_NUM);
				//
				// String scriptFileName = replaceIdxPattern(SCRIPT_FILE_NAME,
				// SciptIdx);
				// map.put(header + "Listener", scriptFileName);
				// map.put(replaceIdxPattern(SCRIPT_SRC, SciptIdx),
				// menuItem.listener);
				// }
			}
			return map;
		} else if (item.type == HPNode.MASK_SHARE_RB_FOLDER) {
			return putMapFor(map, root, childCount, SHARE_JRUBY_FILES_NUM, SHARE_JRUBY_FILE_NAME,
					SHARE_JRUBY_FILE_CONTENT);
		} else if (item.type == HPNode.MASK_SHARE_NATIVE_FOLDER) {
			if (childCount > 0) {
				map.put(SHARE_NATIVE_FILES_NUM, String.valueOf(childCount));

				for (int i = 0; i < childCount; i++) {
					final HPShareNative childItem = (HPShareNative) ((DefaultMutableTreeNode) root
							.getChildAt(i)).getUserObject();

					map.put(replaceIdxPattern(SHARE_NATIVE_FILE_NAME, i), childItem.name);
					map.put(replaceIdxPattern(SHARE_NATIVE_FILE_OS_MASK, i),
							String.valueOf(childItem.osMask));
					map.put(MAP_FILE_PRE + childItem.name, childItem.content);
					map.put(MAP_FILE_TYPE_PRE + childItem.name, String.valueOf(childItem.type));
					map.put(VERSION_FILE_PRE + childItem.name, childItem.ver);
				}
			}
			return map;
		} else if (HPNode.isNodeType(item.type, HPNode.MASK_RESOURCE_FOLDER)) {
			if (childCount > 0) {
				for (int i = 0; i < childCount; i++) {
					final HPShareJar childItem = (HPShareJar) ((DefaultMutableTreeNode) root
							.getChildAt(i)).getUserObject();

					map.put(MAP_FILE_PRE + childItem.name, childItem.content);
					map.put(MAP_FILE_TYPE_PRE + childItem.name, String.valueOf(childItem.type));// HPNode.MAP_FILE_JAR_TYPE);//注意：早期到6.69的版本不含此标识
					map.put(VERSION_FILE_PRE + childItem.name, childItem.ver);
				}
			}
			return map;
		}

		final int size = childCount;
		for (int i = 0; i < size; i++) {
			final DefaultMutableTreeNode subNode = (DefaultMutableTreeNode) root.getChildAt(i);
			if (subNode == msbFold || subNode == eventFolder) {
				continue;
			}
			toMap(map, subNode, msbFold, eventFolder, folders);
		}

		return map;
	}

	private static Map<String, Object> putMapFor(final HashMap<String, Object> map,
			final DefaultMutableTreeNode root, final int childCount, final String TagFileCount,
			final String TagFileName, final String TagFileContent) {
		if (childCount > 0) {
			map.put(TagFileCount, String.valueOf(childCount));

			for (int i = 0; i < childCount; i++) {
				final HPShareContent childItem = (HPShareContent) ((DefaultMutableTreeNode) root
						.getChildAt(i)).getUserObject();

				map.put(replaceIdxPattern(TagFileName, i), childItem.name);
				map.put(replaceIdxPattern(TagFileContent, i), childItem.content);
			}
		}
		return map;
	}

	public static String buildEventMapKey(final int idx, final String eventName) {
		return "Menu." + idx + "." + eventName;
	}

	public static String buildEventMapKeyForStringValue(final int idx, final String eventName) {
		return "SV.Menu." + idx + "." + eventName;
	}

	private static void checkSameNameNode(final DefaultMutableTreeNode parentNode,
			final int childNum) throws NodeInvalidException {
		final HPNode proj = (HPNode) parentNode.getUserObject();
		validate(parentNode, proj);

		for (int i = 0; i < childNum; i++) {
			final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) parentNode
					.getChildAt(i);
			final HPNode i_node = (HPNode) treeNode.getUserObject();
			validate(treeNode, i_node);
			for (int j = i + 1; j < childNum; j++) {
				final HPNode j_node = (HPNode) ((DefaultMutableTreeNode) parentNode.getChildAt(j))
						.getUserObject();
				if (i_node.equals(j_node)) {
					final String desc = "there is same name " + "(or Target Locator) of <strong>["
							+ i_node.name + "]</strong> with other node [<strong>" + j_node.name
							+ "</strong>].";
					throw new NodeInvalidException(treeNode, desc);
				}
			}
		}
	}

	private static void validate(final DefaultMutableTreeNode treeNode, final HPNode i_node)
			throws NodeInvalidException {
		final String v = i_node.validate();
		if (v != null) {
			throw new NodeInvalidException(treeNode, v);
		}
	}

	public static String replaceIdxPattern(final String src, final int value) {
		return src.replace(IDX_PATTERN, String.valueOf(value));
	}

	/**
	 * 返回Idx，即Num - 1
	 * 
	 * @param map
	 * @param key
	 * @return
	 */
	public static final int addOne(final HashMap<String, Object> map, final String key) {
		final Object value = map.get(key);
		if (value != null) {
			try {
				final int num = Integer.parseInt((String) value);
				map.put(key, String.valueOf(num + 1));
				return num;
			} catch (final Throwable e) {

			}
		}
		map.put(key, "1");
		return 0;
	}

	public static final JarOutputStream buildJarOutputStream(final File file) throws Exception {
		final FileOutputStream stream = new FileOutputStream(file);
		return new JarOutputStream(stream);
	}

	public static final void push(final JarOutputStream jarOutStream, final BufferedInputStream in,
			final String jarEntryName) throws Exception {
		final JarEntry entry = new JarEntry(jarEntryName);
		// entry.setTime(source.lastModified());
		jarOutStream.putNextEntry(entry);
		try {
			final byte[] buffer = new byte[1024];
			while (true) {
				final int count = in.read(buffer);
				if (count == -1)
					break;
				jarOutStream.write(buffer, 0, count);
			}
		} finally {
			jarOutStream.closeEntry();
			if (in != null)
				in.close();
		}
	}

	public static final void push(final JarOutputStream jarOutStream,
			final Map<String, String> mapAttributes) throws Exception {
		final Manifest manifest = new Manifest();
		final Iterator<String> it = mapAttributes.keySet().iterator();
		final Attributes mainAttributes = manifest.getMainAttributes();
		while (it.hasNext()) {
			final String key = it.next();
			final Attributes.Name name = new Attributes.Name(key);
			mainAttributes.put(name, mapAttributes.get(key));
		}
		if (!mainAttributes.containsKey(Attributes.Name.MANIFEST_VERSION)) {
			mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		}

		jarOutStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
		manifest.write(jarOutStream);
		jarOutStream.closeEntry();
	}

	public static String getMenuName(final Map<String, Object> map, final int menuIdx) {
		return (String) map.get(HCjar.replaceIdxPattern(HCjar.MENU_NAME, menuIdx));
	}

	public static I18NStoreableHashMapWithModifyFlag buildI18nMapFromSerial(final String serial) {
		final I18NStoreableHashMapWithModifyFlag storeMap = new I18NStoreableHashMapWithModifyFlag();
		storeMap.restore(serial);
		return storeMap;
	}

	public static final int MAIN_MENU_IDX = 0;

	// public static final boolean save(final String baseDir, final Map<String,
	// String> mapAttributes, File harName) throws Exception{
	// FileOutputStream stream = null;
	// JarOutputStream jarOutStream = null;
	// try{
	// stream = new FileOutputStream(harName);
	// final Manifest manifest = new Manifest();
	// Iterator<String> it = mapAttributes.keySet().iterator();
	// final Attributes mainAttributes = manifest.getMainAttributes();
	// while(it.hasNext()){
	// final String key = it.next();
	// final Attributes.Name name = new Attributes.Name(key);
	// mainAttributes.put(name, mapAttributes.get(key));
	// }
	// if (!mainAttributes.containsKey(Attributes.Name.MANIFEST_VERSION)) {
	// mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
	// }
	//
	// jarOutStream = new JarOutputStream(stream);
	//
	// add(baseDir, new File(baseDir), jarOutStream);
	//
	// jarOutStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
	// manifest.write(jarOutStream);
	// jarOutStream.closeEntry();
	//
	// jarOutStream.flush();
	// return true;
	// }finally{
	// try{
	// jarOutStream.close();
	// }catch (Exception e) {
	// }
	// try{
	// stream.close();
	// }catch (Exception e) {
	// }
	// }
	// }
	//
	// private static void add(String baseDir, File source, JarOutputStream
	// jarOutStream) throws Exception {
	// BufferedInputStream in = null;
	// try {
	// if (source.isDirectory()) {
	// String name = getJarEnterName(baseDir, source);
	// if (!name.isEmpty()) {
	// if (!name.endsWith("/"))
	// name += "/";
	// JarEntry entry = new JarEntry(name);
	// entry.setTime(source.lastModified());
	// jarOutStream.putNextEntry(entry);
	// jarOutStream.closeEntry();
	// }
	// for (File nestedFile : source.listFiles())
	// add(baseDir, nestedFile, jarOutStream);
	// return;
	// }else{
	// JarEntry entry = new JarEntry(getJarEnterName(baseDir, source));
	// entry.setTime(source.lastModified());
	// jarOutStream.putNextEntry(entry);
	// in = new BufferedInputStream(new FileInputStream(source));
	//
	// final byte[] buffer = new byte[1024];
	// while (true) {
	// int count = in.read(buffer);
	// if (count == -1)
	// break;
	// jarOutStream.write(buffer, 0, count);
	// }
	// jarOutStream.closeEntry();
	// }
	// } finally {
	// if (in != null)
	// in.close();
	// }
	// }
	//
	// private static String getJarEnterName(String baseDir, File source) {
	// final String strSrc = source.getPath().replace("\\", "/");
	// if(baseDir.length() > strSrc.length()){
	// return "";
	// }else{
	// return strSrc.substring(baseDir.length());
	// }
	// }
}
