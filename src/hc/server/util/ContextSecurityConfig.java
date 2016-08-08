package hc.server.util;

import hc.core.IConstant;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.design.LinkProjectStore;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.hpj.HCjar;
import hc.server.ui.design.hpj.HCjarHelper;
import hc.util.SocketDesc;
import hc.util.SocketEditPanel;

import java.net.SocketPermission;
import java.security.PermissionCollection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class ContextSecurityConfig {
	
	public final String projID;
	final HashMap<String, Object> permissionMap = new HashMap<String, Object>();
	Vector<SocketDesc> allowSockets;
	protected String tempUserDir;
	ProjResponser projResponser;
	ThreadGroup threadGroup;
	ProjectContext ctx;
	private PermissionCollection sockCollection;
	
	public void copyToSocketPanel(final SocketEditPanel sockPanel){
		CCoreUtil.checkAccess();
		
		sockPanel.allowSockets = this.allowSockets;
	}
	
	public final void setProjResponser(final ProjResponser resp){
		CCoreUtil.checkAccess();
		
		projResponser = resp;
		threadGroup = (ThreadGroup)projResponser.threadPool.getThreadGroup();
		this.ctx = projResponser.context;
	}
	
	public final ProjectContext getProjectContext(){
		return ctx;
	}
	
	public final void setProjectContext(final ProjectContext context){
		CCoreUtil.checkAccess();
		
		ctx = context;
	}
	
	public final void initSocketPermissionCollection(){
		CCoreUtil.checkAccess();
		
		if(isSocketLimitOn(this)){
			if(allowSockets == null){
				loadToVector();
			}

			final int size = allowSockets.size();
			if(size == 0){
				sockCollection = new SocketPermission(SocketDesc.LOCAL_HOST_FOR_SOCK_PERMISSION, SocketDesc.defaultAction).newPermissionCollection();
			}else{
				for (int i = 0; i < size; i++) {
					final SocketDesc desc = allowSockets.elementAt(i);
					final String actionDesc = desc.getActionDesc();
					final String hostIP = desc.getHostIPDesc();
					try{
						final SocketPermission sockPerm = new SocketPermission(hostIP, actionDesc);
						if(sockCollection == null){
							sockCollection = sockPerm.newPermissionCollection();
						}
						sockCollection.add(sockPerm);
					}catch (final Exception e) {
						LogManager.errToLog("Error SocketPermission, host : " + hostIP + ", action : " + actionDesc + " in project [" + projID + "].");
						e.printStackTrace();
					}
				}
			}
			sockCollection.setReadOnly();
		}
	}
	
	/**
	 * 如果没有限制，则返回null
	 * @return
	 */
	final PermissionCollection getSocketPermissionCollection(){
		return sockCollection;
	}
	
	public final void buildNewProjectPermissions(){
		CCoreUtil.checkAccess();
		
		setWrite(false);
		setExecute(false);
		setDelete(false);
		setExit(false);
		
		setSysPropRead(true);
		setSysPropWrite(false);
		setLoadLib(false);
		setRobot(false);
//		setListenAllAWTEvents(false);
//		setAccessClipboard(false);
		setShutdownHooks(false);
		setSetIO(false);
		setSetFactory(false);
		
		setMemberAccessSystem(false);
		
		allowSockets = new Vector<SocketDesc>();
		addDefaultNewSocket(allowSockets);
		saveToMap();
	}
	
	public final void buildDefaultPermissions(){
		CCoreUtil.checkAccess();
		
		setWrite(true);
		setExecute(true);
		setDelete(true);
		setExit(false);
		
	}
	
	public final void loadToVector(){
		if(allowSockets == null){
			allowSockets = new Vector<SocketDesc>();
		}else{
			allowSockets.removeAllElements();
		}
		
		final String num = (String)permissionMap.get(HCjar.PERMISSION_SOCK_NUM);
		if(num != null){
			final int size = Integer.valueOf(num);
			for (int i = 0; i < size; i++) {
				final String item = (String)permissionMap.get(HCjar.PERMISSION_SOCK_ITEM_HEADER + i);
				allowSockets.add(SocketDesc.decode(item));
			}
		}
	}
	
	public final void saveToMap(){
		if(allowSockets == null){//升级时，旧系统未含此结构信息
			return;
		}
		
		HCjarHelper.removeHeaderStartWith(permissionMap, HCjar.PERMISSION_SOCK_ITEM_HEADER);
		
		final int size = allowSockets.size();
		for (int i = 0; i < size; i++) {
			final SocketDesc socket = allowSockets.elementAt(i);
			final String serial = SocketDesc.encode(socket);
			
			permissionMap.put(HCjar.PERMISSION_SOCK_ITEM_HEADER + i, serial);
		}
		permissionMap.put(HCjar.PERMISSION_SOCK_NUM, String.valueOf(size));
	}
	
	/**
	 * 
	 * @param key
	 * @param defaultValue 由于权限的特殊性，对于没有创设的权限，应放行，即true
	 * @return
	 */
	private final boolean isTrue(final String key, final boolean defaultValue){
		final Object v = permissionMap.get(key);
		if(v == null){
			return defaultValue;
		}else if(v != null && v.equals(IConstant.TRUE)){
			return true;
		}
		return false;
	}
	
	private final boolean isTrue(final String key){
		return isTrue(key, false);
	}
	
	private final void setBoolean(final String key, final boolean bool){
		CCoreUtil.checkAccess();
		
		permissionMap.put(key, bool?IConstant.TRUE:IConstant.FALSE);
	}
	
	public final boolean isWrite(){
		return isTrue(HCjar.PERMISSION_WRITE);
	}
	
	public final boolean isExecute(){
		return isTrue(HCjar.PERMISSION_EXECUTE);
	}
	
	public final boolean isExit(){
		return isTrue(HCjar.PERMISSION_EXIT);
	}
	
	public final boolean isDelete(){
		return isTrue(HCjar.PERMISSION_DELETE);
	}
	
	public final void setWrite(final boolean bool){
		setBoolean(HCjar.PERMISSION_WRITE, bool);
	}
	
	public final void setExecute(final boolean bool){
		setBoolean(HCjar.PERMISSION_EXECUTE, bool);
	}
	
	public final void setExit(final boolean bool){
		setBoolean(HCjar.PERMISSION_EXIT, bool);
	}
	
	public final void setDelete(final boolean bool){
		setBoolean(HCjar.PERMISSION_DELETE, bool);
	}
	
	public ContextSecurityConfig(final String projID){
		this.projID = projID;
	}
	
	public final boolean isSysPropWrite(){
		return isTrue(HCjar.PERMISSION_SYS_PROP_WRITE, true);
	}
	
	public final boolean isSysPropRead(){
		return isTrue(HCjar.PERMISSION_SYS_PROP_READ, true);
	}
	
	public final void setSysPropWrite(final boolean bool){
		setBoolean(HCjar.PERMISSION_SYS_PROP_WRITE, bool);
	}
	
	public final void setSysPropRead(final boolean bool){
		setBoolean(HCjar.PERMISSION_SYS_PROP_READ, bool);
	}
	
	public final void setLoadLib(final boolean bool){
		setBoolean(HCjar.PERMISSION_LOAD_LIB, bool);
	}
	
	public final boolean isLoadLib(){
		return isTrue(HCjar.PERMISSION_LOAD_LIB, true);
	}
	
	public final void setRobot(final boolean bool){
		setBoolean(HCjar.PERMISSION_ROBOT, bool);
	}
	
	public final boolean isRobot(){
		return isTrue(HCjar.PERMISSION_ROBOT, true);
	}
	
	//------------------注意：请开启buildNewProjectPermissions内的初值
//	public final void setListenAllAWTEvents(boolean bool){
//		setBoolean(HCjar.PERMISSION_LISTEN_ALL_AWT_EVNTS, bool);
//	}
//	
//	public final boolean isListenAllAWTEvents(){
//		return isTrue(HCjar.PERMISSION_LISTEN_ALL_AWT_EVNTS, true);
//	}
//	
//	public final void setAccessClipboard(boolean bool){
//		setBoolean(HCjar.PERMISSION_ACCESS_CLIPBOARD, bool);
//	}
//	
//	public final boolean isAccessClipboard(){
//		return isTrue(HCjar.PERMISSION_ACCESS_CLIPBOARD, true);
//	}
	
	public final void setShutdownHooks(final boolean bool){
		setBoolean(HCjar.PERMISSION_SHUTDOWNHOOKS, bool);
	}
	
	public final boolean isShutdownHooks(){
		return isTrue(HCjar.PERMISSION_SHUTDOWNHOOKS, true);
	}
	
	public final void setSetIO(final boolean bool){
		setBoolean(HCjar.PERMISSION_SETIO, bool);
	}
	
	public final void setMemberAccessSystem(final boolean bool){
		setBoolean(HCjar.PERMISSION_MEMBER_ACCESS_SYSTEM, bool);
	}
	
	public final boolean isSetIO(){
		return isTrue(HCjar.PERMISSION_SETIO, true);
	}

	public final boolean isMemberAccessSystem(){
		return isTrue(HCjar.PERMISSION_MEMBER_ACCESS_SYSTEM, true);
	}
	
	public final boolean isSetFactory(){
		return isTrue(HCjar.PERMISSION_SET_FACTORY, true);
	}
	
	public final void setSetFactory(final boolean bool){
		setBoolean(HCjar.PERMISSION_SET_FACTORY, bool);
	}
	
	public final boolean isAccessPrivateAddress(){
		if(isSocketLimitOn(this)){
			return isTrue(HCjar.PERMISSION_ACCESS_PRIVATE_ADDRESS, false);
		}else{
			return false;
		}
	}

	public final void setAccessPrivateAddress(final boolean bool){
		setBoolean(HCjar.PERMISSION_ACCESS_PRIVATE_ADDRESS, bool);
	}
	
	public final static ContextSecurityConfig getContextSecurityConfig(final LinkProjectStore linkProjectStore){
		final ContextSecurityConfig csc = new ContextSecurityConfig(linkProjectStore.getProjectID());
		final HashMap<String, Object> targetMap = csc.permissionMap;
		
		final Iterator keys = linkProjectStore.keySet().iterator();
		boolean hasKey = false;
		while(keys.hasNext()){
			final String key = (String)keys.next();
			if(key.startsWith(HCjar.PERMISSION_HEADER, 0)){
				hasKey = true;
				targetMap.put(key, linkProjectStore.get(key));
			}
		}
		
		if(hasKey == false){
			csc.buildDefaultPermissions();
		}
		
		return csc;
	}

	public static void copyMapsToLPS(final LinkProjectStore linkProjectStore, final ContextSecurityConfig csc, final boolean forceUpdate) {
		final HashMap<String, Object> map = csc.permissionMap;
		final Iterator keys = map.keySet().iterator();
		while(keys.hasNext()){
			final String key = (String)keys.next();
			//已存在的值，保留旧值
			if(forceUpdate || (linkProjectStore.containsKey(key) == false)){
				linkProjectStore.put(key, map.get(key));
			}
		}
	}

	public static final ContextSecurityConfig getPermissionFromHARMap(final Map<String, Object> map){
		final ContextSecurityConfig csc = new ContextSecurityConfig((String)map.get(HCjar.PROJ_ID));
		final HashMap<String, Object> targetMap = csc.permissionMap;
		
		boolean hasKey = false;
		final Iterator<String> keys = map.keySet().iterator();
		while(keys.hasNext()){
			final String key = keys.next();
			if(key.startsWith(HCjar.PERMISSION_HEADER, 0)){
				hasKey = true;
				targetMap.put(key, map.get(key));
			}
		}
		
		if(hasKey == false){
			csc.buildDefaultPermissions();
		}
		
		return csc;
	}

	public static final void copyPermissionsFromConfig(final Map<String, Object> map, final ContextSecurityConfig csc){
		csc.saveToMap();
		map.putAll(csc.permissionMap);
	}

	public static boolean isSocketLimitOn(final ContextSecurityConfig contextSecurityConfig){
		return HCjarHelper.isTrue(contextSecurityConfig.permissionMap, HCjar.PERMISSION_SOCK_LIMIT_ON, false);
	}

	public static void setSocketLimitOn(final ContextSecurityConfig contextSecurityConfig, final boolean isOn) {
		CCoreUtil.checkAccess();
		
		HCjarHelper.setBoolean(contextSecurityConfig.permissionMap, HCjar.PERMISSION_SOCK_LIMIT_ON, isOn);
	}

	public static void resetPermission(final ContextSecurityConfig contextSecurityConfig, final LinkProjectStore linkProjectStore) {
		CCoreUtil.checkAccess();
		
		final Map<String, Object> map = HCjar.loadHarFromLPS(linkProjectStore);
		final ContextSecurityConfig defaultPermissions = ContextSecurityConfig.getPermissionFromHARMap(map);
		final HashMap<String, Object> storeCSC = contextSecurityConfig.permissionMap;
		storeCSC.clear();
		storeCSC.putAll(defaultPermissions.permissionMap);
	}

	public static void addDefaultNewSocket(final Vector<SocketDesc> vector) {
		CCoreUtil.checkAccess();
		
		final String defaultPort = "";
		vector.add(new SocketDesc(SocketDesc.LOCAL_HOST_FOR_SOCK_PERMISSION, "", defaultPort, "", "", SocketDesc.defaultAction));
	}
}
