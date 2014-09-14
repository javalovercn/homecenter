package hc.server.ui.design.hpj;

import hc.App;
import hc.server.StarterManager;
import hc.util.BaseResponsor;
import hc.util.PropertiesManager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
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

public class HCjar {
	public static final String ITEM_NAME = "Name";
	public static final String ITEM_TYPE = "Type";
	public static final String ITEM_URL = "Url";
	public static final String ITEM_IMAGE = "Image";
	public static final String ITEM_LISTENER = "Listener";
	public static final String ITEM_EXTENDMAP = "ExtendMap";

	public static final String MAP_FILE_PRE = "__MaP_fILe_pRE_";
	public static final String VERSION_FILE_PRE = "__VeR_fILe_pRE_";
	public static final String HOMECENTER_VER = "HomeCenter.Ver";
	public static final String JRUBY_VER = "JRuby.Ver";
	public static final String JRE_VER = "JRE.Ver";
	public static final String PROJ_NAME = "Project.Name";
	public static final String PROJ_ID = "Project.ID";
	public static final String PROJ_NEXT_NODE_IDX = "Project.NodeIdx";
	public static final String PROJ_VER = "Project.Ver";
	public static final String PROJ_UPGRADE_URL = "Project.upgradeURL";
	
	public static final String PROJ_CONTACT = "Project.Contact";
	public static final String PROJ_COPYRIGHT = "Project.Copyright";
	public static final String PROJ_DESC = "Project.Desc";
	public static final String PROJ_LICENSE = "Project.License";

	public static final String IDX_PATTERN = "(\\d+)";

	public static final String MENU_NUM = "Menu.Num";
	public static final String SHARE_JRUBY_FILES_NUM = "ShareJRuby.Num";
	public static final String SHARE_JRUBY_FILE_NAME = "ShareJRuby." + IDX_PATTERN + ".File";
	public static final String SHARE_JRUBY_FILE_CONTENT = "ShareJRuby." + IDX_PATTERN + ".Content";
	
//	public static final String SHARE_JAR_FILES_NUM = "ShareJar.Num";
//	public static final String SHARE_JAR_FILE_NAME = "ShareJar." + IDX_PATTERN + ".File";
//	public static final String SHARE_JAR_FILE_CONTENT = "ShareJar." + IDX_PATTERN + ".Content";

	public static final String MAIN_MENU_IDX = "MainMenu.Idx";
	public static final String MENU_ID = "Menu." + IDX_PATTERN + ".Id";
	public static final String MENU_NAME = "Menu." + IDX_PATTERN + ".Name";
	public static final String MENU_COL_NUM = "Menu." + IDX_PATTERN + ".ColNum";
	public static final String MENU_CHILD_COUNT = "Menu." + IDX_PATTERN + ".ChildCount";
	public static final String MENU_ITEM_HEADER = "Menu." + IDX_PATTERN + ".Item.";
	
//	public static final String RANDOM_SHARE_FOLDER = "RandomFolder";
//	public static final String SCRIPT_NUM = "Script.Num";
//	public static final String SCRIPT_SRC = "Script." + IDX_PATTERN + ".Src";
//	public static final String SCRIPT_FILE_NAME = "script_" + IDX_PATTERN;

	public static final String JarConfigEntry = "config.properties";

	public static byte[] readFromInputStream(InputStream jarInputStream)
			throws Throwable {
		ByteArrayOutputStream classData = new ByteArrayOutputStream();
		int readSize = 0;
		final int BUFFER_SIZE = 1024 * 4;
		final byte[] buffer = new byte[BUFFER_SIZE];
		try{
		while (jarInputStream.available() != 0) {
			// 注：有些时候，即使jarInputStream有超过BUFFER_SIZE的数据
			// 也有可能读到的数据少于BUFFER_SIZE
			readSize = jarInputStream.read(buffer, 0, BUFFER_SIZE);
			// jarInputStream.available()好像不起作用，因而需要加入一下代码
			if (readSize < 0)
				break;
			classData.write(buffer, 0, readSize);
		}
		}catch (Throwable e) {
			try{
				jarInputStream.close();
			}catch (Exception ex) {
			}
			throw e;
		}
		return classData.toByteArray();
	}

	public static final Map<String, Object> loadJar(String url){
		Properties p = new Properties();
		HashMap<String, Object> mapString = new HashMap<String, Object>();
		
		JarInputStream jis = null;
		try{
			URL jarurl = new URL(url);
			jis = new JarInputStream(jarurl.openStream());
			JarEntry je;
			while( (je = jis.getNextJarEntry()) != null){
				loadToMap(je, jis, p, mapString, true);
				jis.closeEntry();
			}
		}catch (Throwable e) {
		}finally{
			try {
				jis.close();
			} catch (Exception e) {
			}
		}
		
		pushStringMap(p, mapString);
		return mapString;
	}

	private static void loadToMap(JarEntry je, InputStream is,
			Properties p, HashMap<String, Object> mapString, boolean loadFileByteArray) throws Throwable {
		final String jarEnterName = je.getName();
		if(jarEnterName.equals(JarConfigEntry)){
			pushStringMap(p, mapString, is);
		}else {
			//if(jarEnterName.endsWith(".jar"))
			//如jar, png, au
			if(loadFileByteArray){
				byte[] data = readFromInputStream(is);
				mapString.put(MAP_FILE_PRE + jarEnterName, data);
			}
		}
	}
	
	private static void setNullDefaultValue(final HashMap<String, Object> map, final String key, final String defaultValue){
		if(map.get(key) == null){
//			System.out.println("set key["+key+"] to default : " + defaultValue);
			map.put(key, defaultValue);
		}
	}
	
	public static void initMap(final HashMap<String, Object> map){
		setNullDefaultValue(map, PROJ_NAME, "My Project");
		setNullDefaultValue(map, PROJ_NEXT_NODE_IDX, "1");
		setNullDefaultValue(map, PROJ_ID, HPProject.convertProjectIDFromName((String)map.get(PROJ_NAME)));
		setNullDefaultValue(map, PROJ_VER, HPProject.DEFAULT_VER);
		setNullDefaultValue(map, PROJ_UPGRADE_URL, "");
		setNullDefaultValue(map, PROJ_CONTACT, "");
		setNullDefaultValue(map, PROJ_COPYRIGHT, "");
		setNullDefaultValue(map, PROJ_DESC, "");
		setNullDefaultValue(map, PROJ_LICENSE, "");
		
		final Object object = map.get(MENU_NUM);
		if(object != null){
			final int menuNum = Integer.parseInt((String)object);
			for (int idx = 0; idx < menuNum; idx++) {
				final String key = replaceIdxPattern(MENU_COL_NUM, idx);
				setNullDefaultValue(map, key, "0");		
				
				Object menuItemObj = map.get(HCjar.replaceIdxPattern(HCjar.MENU_CHILD_COUNT, idx));
				if(menuItemObj != null){
					final int itemCount = Integer.parseInt((String)menuItemObj);
					final String Iheader = HCjar.replaceIdxPattern(HCjar.MENU_ITEM_HEADER, idx);
					for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
						String header = Iheader + itemIdx + ".";
						final String key2 = header + HCjar.ITEM_EXTENDMAP;
						setNullDefaultValue(map, key2, "");
					}
				}
			}
		}
	}
	
	public static final Map<String, Object> loadHar(File jarfile, boolean loadFileByteArray){
		JarFile jf = null;
		Properties p = new Properties();
		HashMap<String, Object> mapString = new HashMap<String, Object>();
		try{
			jf = new JarFile(jarfile);

			for (Enumeration<JarEntry> em1 = jf.entries(); em1.hasMoreElements();) {
				JarEntry je = em1.nextElement();
				InputStream is = jf.getInputStream(je);
				try {
					loadToMap(je, is, p, mapString, loadFileByteArray);
				} finally {
					is.close();
				}
			}
		}catch (Throwable e) {
		}finally{
			try {
				jf.close();
			} catch (Exception e) {
			}
		}
		pushStringMap(p, mapString);
		return mapString;
	}

	private static void pushStringMap(Properties p,
			HashMap<String, Object> map, InputStream is) throws IOException {
		p.load(is);
		pushStringMap(p, map);
	}

	private static void pushStringMap(Properties p, HashMap<String, Object> map) {
		for(Map.Entry<Object, Object> entry:p.entrySet()){  
			map.put((String)entry.getKey(), entry.getValue());
		}
		if(map.isEmpty() == false){
			initMap(map);
		}
	}
	
	public static final void toHar(Map<String, Object> map, File jarfile){
		
		Properties p = new Properties();

		if(jarfile.exists()){
			jarfile.delete();
		}
		
		JarOutputStream jaros = null;
		try {
			jaros = buildJarOutputStream(jarfile);

			//去掉非String型的value
			Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();  
	        while(it.hasNext()){  
	            Map.Entry<String, Object> entry=it.next();  
	            final String keyName = entry.getKey();
	            if(keyName.startsWith(MAP_FILE_PRE)){
	            	final String fileName = keyName.substring(MAP_FILE_PRE.length());
		            jaros.putNextEntry(new JarEntry(fileName));
					jaros.write((byte[])entry.getValue());
					jaros.closeEntry();
					
					//必须时行remove，以保留串型数据，而非二进制数据
	            	it.remove();
	            }
	        }  
			
			p.putAll(map);
			
			jaros.putNextEntry(new JarEntry(JarConfigEntry));
			p.store(jaros, "");
			jaros.closeEntry();
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				jaros.close();
			} catch (IOException e) {
			}
		}
		
	}

	/**
	 * 需要忽略的结点，如事件结点夹
	 */
	public static final int SKIP_SUB_MENU_ITEM_NUM = 1;

	/**
	 * 返回主菜单的Menu结点，如果是新建，则可能返回null
	 * @param map
	 * @param root
	 * @return
	 */
	public static final DefaultMutableTreeNode toNode(Map<String, Object> map, final DefaultMutableTreeNode root, 
			final DefaultMutableTreeNode[] scriptFolder){
		DefaultMutableTreeNode out_mainMenuNode = null;
		//装填ROOT
		{
			root.setUserObject(new HPProject(HPNode.MASK_ROOT, 
					(String)map.get(PROJ_NAME), (String)map.get(PROJ_ID),
					(String)map.get(PROJ_VER), (String)map.get(PROJ_UPGRADE_URL),
					(String)map.get(PROJ_CONTACT), (String)map.get(PROJ_COPYRIGHT), (String)map.get(PROJ_DESC), (String)map.get(PROJ_LICENSE)));
			MenuManager.setNextNodeIdx(Integer.parseInt((String)map.get(PROJ_NEXT_NODE_IDX)));
		}
		
		try{//捕获数据结构可能被异常修改后，可能导致的不正确！
			
			//装填Menu
			final Object object = map.get(MENU_NUM);
			if(object != null){
				final int menuNum = Integer.parseInt((String)object);
				final int MainMenuIdx = Integer.parseInt((String)map.get(MAIN_MENU_IDX));
	
				for (int idx = 0; idx < menuNum; idx++) {
					String menuName = (String)map.get(replaceIdxPattern(MENU_NAME, idx));
					String menuColNum = (String)map.get(replaceIdxPattern(MENU_COL_NUM, idx));
					final HPMenu menu = new HPMenu((String)map.get(replaceIdxPattern(MENU_ID, idx)), 
							HPNode.MASK_MENU, menuName, (menuColNum==null)?0:Integer.parseInt(menuColNum),
							MainMenuIdx == idx);
					DefaultMutableTreeNode menuNode = new DefaultMutableTreeNode(menu);

					buildMenuEventNodes(map, menuNode, idx);
					
					root.add(menuNode);
					
					if(menu.isMainMenu){
						out_mainMenuNode = menuNode;
					}
					
					//装填MenuItem
					{
						Object menuItemObj = map.get(replaceIdxPattern(MENU_CHILD_COUNT, idx));
						if(menuItemObj != null){
							final int itemCount = Integer.parseInt((String)menuItemObj);
							final String Iheader = replaceIdxPattern(MENU_ITEM_HEADER, idx);
							for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
								String header = Iheader + itemIdx + ".";
								final HPMenuItem userObject = new HPMenuItem(
										Integer.parseInt((String)map.get(header + ITEM_TYPE)), 
										(String)map.get(header + ITEM_NAME));
								
								userObject.url = (String)map.get(header + ITEM_URL);
								userObject.imageData = (String)map.get(header + ITEM_IMAGE);
								userObject.listener = (String)map.get(header + ITEM_LISTENER);
								userObject.extendMap.restore((String)map.get(header + ITEM_EXTENDMAP));
								
								DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(userObject);
								menuNode.add(itemNode);
							}
						}
					}
				}
			}
			
			//装填ShareJRubyFile
			final Object shareJRubyFileNum = map.get(SHARE_JRUBY_FILES_NUM);
			if(shareJRubyFileNum != null){
				final int shareNum = Integer.parseInt((String)shareJRubyFileNum);
	
				for (int idx = 0; idx < shareNum; idx++) {
					final String fileName = (String)map.get(replaceIdxPattern(SHARE_JRUBY_FILE_NAME, idx));
					final String fileContent = (String)map.get(replaceIdxPattern(SHARE_JRUBY_FILE_CONTENT, idx));
					final HPShareJRuby userObject = new HPShareJRuby(
							HPNode.MASK_SHARE_RB, fileName);
					userObject.content = fileContent;
					DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(userObject);
					scriptFolder[0].add(itemNode);
				}				
			}
			
			//装填shareFileResource，如jar, png, au
			final DefaultMutableTreeNode shareJarFolder = scriptFolder[1];
			for(Map.Entry<String, Object> entry:map.entrySet()){  
				final String keyName = entry.getKey();
				if(keyName.startsWith(MAP_FILE_PRE)){
					final String realName = keyName.substring(MAP_FILE_PRE.length());
					
					final HPShareJar jar = new HPShareJar(HPNode.MASK_RESOURCE_JAR, realName);
					jar.ver = (String)map.get(VERSION_FILE_PRE + realName);
					
					jar.ID = realName;
					jar.content = (byte[])entry.getValue();
					
					DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(jar);
					shareJarFolder.add(itemNode);
				}
			}
		}catch (Throwable e) {
			
		}
		return out_mainMenuNode;
	}

	public static void buildMenuEventNodes(Map<String, Object> map,
			DefaultMutableTreeNode menuNode, int menuIdx) {
		final HPMenuEvent folder = new HPMenuEvent(HPNode.MASK_EVENT_FOLDER, "Events");
		DefaultMutableTreeNode eventFold = new DefaultMutableTreeNode(folder);
		menuNode.add(eventFold);
		
		for (int i = 0; i < BaseResponsor.EVENT_LIST.length; i++) {
			HPMenuEventItem eventItem = new HPMenuEventItem(HPNode.MASK_EVENT_ITEM, BaseResponsor.EVENT_LIST[i]);
			DefaultMutableTreeNode eventItemNode = new DefaultMutableTreeNode(eventItem);
			eventFold.add(eventItemNode);
			
			eventItem.content = (String)map.get(buildEventMapKey(menuIdx, eventItem.name));
			if(eventItem.content == null){
				eventItem.content = "";
			}
		}
	}
	
	public static final Map<String, Object> toMap(DefaultMutableTreeNode root, 
			DefaultMutableTreeNode[] folders) throws NodeInvalidException{
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		map.put(HOMECENTER_VER, StarterManager.getHCVersion());
		map.put(JRE_VER, String.valueOf(App.getJREVer()));
		final String jruby_ver = PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer);
		if(jruby_ver != null){
			map.put(JRUBY_VER, jruby_ver);
		}
		
		return toMap(map, root, folders);
	}
	
	private static final Map<String, Object> toMap(HashMap<String, Object> map, 
			DefaultMutableTreeNode root, DefaultMutableTreeNode[] folders) throws NodeInvalidException{
		final int childCount = root.getChildCount();
		checkSameNameNode(root, childCount);

		HPNode item = (HPNode)root.getUserObject();
		if(item.type == HPNode.MASK_ROOT){
			map.put(PROJ_NAME, item.name);
			map.put(PROJ_ID, ((HPProject)item).id);
			map.put(PROJ_VER, ((HPProject)item).ver);
			map.put(PROJ_NEXT_NODE_IDX, String.valueOf(MenuManager.getCurrNodeIdx()));
			map.put(PROJ_UPGRADE_URL, ((HPProject)item).upgradeURL);
			map.put(PROJ_CONTACT, ((HPProject)item).contact);
			map.put(PROJ_COPYRIGHT, ((HPProject)item).copyright);
			map.put(PROJ_DESC, ((HPProject)item).desc);
			map.put(PROJ_LICENSE, ((HPProject)item).license);
		}else if(item.type == HPNode.MASK_MENU){
			int idx = addOne(map, MENU_NUM);
			final String menuPattern = replaceIdxPattern(MENU_NAME, idx);
			map.put(menuPattern, item.name);
			map.put(replaceIdxPattern(MENU_COL_NUM, idx), String.valueOf(((HPMenu)item).colNum));
			
			final HPMenu hpmenu = (HPMenu)item;
			if(hpmenu.isMainMenu){
				map.put(MAIN_MENU_IDX, String.valueOf(idx));
			}
			{
				DefaultMutableTreeNode eventFold = (DefaultMutableTreeNode)root.getChildAt(0);
				int size = eventFold.getChildCount();
				for (int i = 0; i < size; i++) {
					HPMenuEventItem eventItem = (HPMenuEventItem)((DefaultMutableTreeNode)eventFold.getChildAt(i)).getUserObject();
					
					map.put(buildEventMapKey(idx, eventItem.name), eventItem.content);
				}

			}
			map.put(replaceIdxPattern(MENU_ID, idx), hpmenu.menuID);
			final int size = childCount - SKIP_SUB_MENU_ITEM_NUM;//忽略事件结点
			map.put(replaceIdxPattern(MENU_CHILD_COUNT, idx), String.valueOf(size));
			final String Iheader = replaceIdxPattern(MENU_ITEM_HEADER, idx);
			
			for (int i = 0; i < size; i++) {
				String header = Iheader + i + "."; 
				
				HPMenuItem menuItem = (HPMenuItem)((DefaultMutableTreeNode)root.getChildAt(i + SKIP_SUB_MENU_ITEM_NUM)).getUserObject();
				
				map.put(header + ITEM_NAME, menuItem.name);
				map.put(header + ITEM_TYPE, String.valueOf(menuItem.type));
				
				map.put(header + ITEM_URL, menuItem.url);
				map.put(header + ITEM_IMAGE, menuItem.imageData);
				map.put(header + ITEM_LISTENER, menuItem.listener);
				map.put(header + ITEM_EXTENDMAP, menuItem.extendMap.toSerial());
//				if(menuItem.listener != null && menuItem.listener.length() > 0){
//					int SciptIdx = addOne(map, SCRIPT_NUM);
//					
//					String scriptFileName = replaceIdxPattern(SCRIPT_FILE_NAME, SciptIdx);
//					map.put(header + "Listener", scriptFileName);
//					map.put(replaceIdxPattern(SCRIPT_SRC, SciptIdx), menuItem.listener);
//				}
			}
			return map;
		}else if(item.type == HPNode.MASK_SHARE_RB_FOLDER){
			if(childCount > 0){
				map.put(SHARE_JRUBY_FILES_NUM, String.valueOf(childCount));
				
				for (int i = 0; i < childCount; i++) {
					HPShareJRuby childItem = (HPShareJRuby)((DefaultMutableTreeNode)root.getChildAt(i)).getUserObject();
					
					map.put(replaceIdxPattern(SHARE_JRUBY_FILE_NAME, i), childItem.name);
					map.put(replaceIdxPattern(SHARE_JRUBY_FILE_CONTENT, i), childItem.content);
				}
			}
			return map;
		}else if(HPNode.isNodeType(item.type, HPNode.MASK_RESOURCE_FOLDER)){
			if(childCount > 0){
				for (int i = 0; i < childCount; i++) {
					HPShareJar childItem = (HPShareJar)((DefaultMutableTreeNode)root.getChildAt(i)).getUserObject();
					
					map.put(MAP_FILE_PRE + childItem.name, childItem.content);
					map.put(VERSION_FILE_PRE + childItem.name, childItem.ver);
				}
			}
			return map;
		}
		
		final int size = childCount;
		for (int i = 0; i < size; i++) {
			toMap(map, (DefaultMutableTreeNode)root.getChildAt(i), folders);
		}
		
		return map;
	}

	public static String buildEventMapKey(int idx, String eventName) {
		return "Menu." + idx + "." + eventName;
	}
	
	private static void checkSameNameNode(DefaultMutableTreeNode parentNode, 
			final int childNum) throws NodeInvalidException{
		HPNode proj = (HPNode)parentNode.getUserObject();
		validate(parentNode, proj);
			
		for (int i = 1; i < childNum; i++) {
			final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)parentNode.getChildAt(i);
			final HPNode i_node = (HPNode)treeNode.getUserObject();
			validate(treeNode, i_node);
			for (int j = 0; j < i; j++) {
				final HPNode j_node = (HPNode)((DefaultMutableTreeNode)parentNode.getChildAt(j)).getUserObject();
				if(i_node.equals(j_node)){
					NodeInvalidException e = new NodeInvalidException(treeNode);
					e.setDesc("<strong>[" + i_node.name +"]</strong> has same <strong>display name</strong>" +
							"(or <strong>target locator</strong>) with node [<strong>" + j_node.name + "</strong>].");
					throw e;
				}
			}
		}
	}

	private static void validate(final DefaultMutableTreeNode treeNode,
			HPNode i_node) throws NodeInvalidException {
		final String v = i_node.validate();
		if(v != null){
			NodeInvalidException e = new NodeInvalidException(treeNode);
			e.setDesc(v);
			throw e;
		}
	}
	
	public static String replaceIdxPattern(String src, int value){
		return src.replace(IDX_PATTERN, String.valueOf(value));
	}
	
	/**
	 * 返回Idx，即Num - 1
	 * @param map
	 * @param key
	 * @return
	 */
	public static final int addOne(HashMap<String, Object> map, String key){
		Object value = map.get(key);
		if(value != null){
			try{
				int num = Integer.parseInt((String)value);
				map.put(key, String.valueOf(num + 1));
				return num;
			}catch (Throwable e) {
				
			}
		}
		map.put(key, "1");
		return 0;
	}
	
	public static final JarOutputStream buildJarOutputStream(File file) throws Exception{
		FileOutputStream stream = new FileOutputStream(file);
		return new JarOutputStream(stream);
	}
	
	public static final void push(JarOutputStream jarOutStream, BufferedInputStream in, String jarEntryName) throws Exception {
		JarEntry entry = new JarEntry(jarEntryName);
//		entry.setTime(source.lastModified());
		jarOutStream.putNextEntry(entry);
		try {
			final byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
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

	public static final void push(JarOutputStream jarOutStream, final Map<String, String> mapAttributes) throws Exception{
		final Manifest manifest = new Manifest();
		Iterator<String> it = mapAttributes.keySet().iterator();
		final Attributes mainAttributes = manifest.getMainAttributes();
		while(it.hasNext()){
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
	
	public static String getMenuName(final Map<String, Object> map, final int menuIdx){
		return (String)map.get(HCjar.replaceIdxPattern(HCjar.MENU_NAME, menuIdx));
	}

//	public static final boolean save(final String baseDir, final Map<String, String> mapAttributes, File harName) throws Exception{
//		FileOutputStream stream = null;
//		JarOutputStream jarOutStream = null;
//		try{
//			stream = new FileOutputStream(harName);
//			final Manifest manifest = new Manifest();
//			Iterator<String> it = mapAttributes.keySet().iterator();
//			final Attributes mainAttributes = manifest.getMainAttributes();
//			while(it.hasNext()){
//				final String key = it.next();
//				final Attributes.Name name = new Attributes.Name(key);
//				mainAttributes.put(name, mapAttributes.get(key));
//			}
//			if (!mainAttributes.containsKey(Attributes.Name.MANIFEST_VERSION)) {
//				mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
//			}
//			
//			jarOutStream = new JarOutputStream(stream);
//		
//			add(baseDir, new File(baseDir), jarOutStream);
//			
//			jarOutStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
//			manifest.write(jarOutStream);
//			jarOutStream.closeEntry();
//			
//			jarOutStream.flush();
//			return true;
//	    }finally{
//	    	try{
//	    		jarOutStream.close();
//	    	}catch (Exception e) {
//			}
//	    	try{
//	    		stream.close();
//	    	}catch (Exception e) {
//			}
//	    }
//	}
//
//	private static void add(String baseDir, File source, JarOutputStream jarOutStream) throws Exception {
//		BufferedInputStream in = null;
//		try {
//			if (source.isDirectory()) {
//				String name = getJarEnterName(baseDir, source);
//				if (!name.isEmpty()) {
//					if (!name.endsWith("/"))
//						name += "/";
//					JarEntry entry = new JarEntry(name);
//					entry.setTime(source.lastModified());
//					jarOutStream.putNextEntry(entry);
//					jarOutStream.closeEntry();
//				}
//				for (File nestedFile : source.listFiles())
//					add(baseDir, nestedFile, jarOutStream);
//				return;
//			}else{
//				JarEntry entry = new JarEntry(getJarEnterName(baseDir, source));
//				entry.setTime(source.lastModified());
//				jarOutStream.putNextEntry(entry);
//				in = new BufferedInputStream(new FileInputStream(source));
//	
//				final byte[] buffer = new byte[1024];
//				while (true) {
//					int count = in.read(buffer);
//					if (count == -1)
//						break;
//					jarOutStream.write(buffer, 0, count);
//				}
//				jarOutStream.closeEntry();
//			}
//		} finally {
//			if (in != null)
//				in.close();
//		}
//	}
//
//	private static String getJarEnterName(String baseDir, File source) {
//		final String strSrc = source.getPath().replace("\\", "/");
//		if(baseDir.length() > strSrc.length()){
//			return "";
//		}else{
//			return strSrc.substring(baseDir.length());
//		}
//	}
}
