package hc.server.ui.design;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.CtrlMap;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.StoreableHashMap;
import hc.server.ScreenServer;
import hc.server.data.screen.ScreenCapturer;
import hc.server.ui.ClientDesc;
import hc.server.ui.CtrlResponse;
import hc.server.ui.MCanvas;
import hc.server.ui.MUIView;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServCtrlCanvas;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.hpj.HCjar;
import hc.server.ui.design.hpj.RubyExector;
import hc.util.BaseResponsor;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class ProjResponser {
	final Map<String, Object> map;
	final JarMainMenu[] menu;
	final int mainMenuIdx;
	final HCJRubyEngine hcje;
	final ProjectContext context;

	public ProjResponser(final Map<String, Object> p_map, final BaseResponsor baseRep,
			final LinkProjectStore lps) {
		this.map = p_map;
		this.hcje = new HCJRubyEngine(new File(lps.getDeployTmpDir()).getAbsolutePath());
		
		context = new ProjectContext((String)map.get(HCjar.PROJ_ID), (String)map.get(HCjar.PROJ_VER));
		
		Object object = map.get(HCjar.MENU_NUM);
		if(object != null){
			final int menuNum = Integer.parseInt((String)object);
			mainMenuIdx = Integer.parseInt((String)map.get(HCjar.MAIN_MENU_IDX));
			menu = new JarMainMenu[menuNum];
			
			for (int i = 0; i < menuNum; i++) {
				menu[i] = new JarMainMenu(i, map, 
						lps.isRoot(), 
						baseRep, lps.getLinkScriptName());
			}
		}else{
			menu = new JarMainMenu[0];
			mainMenuIdx = 0;
		}
	}


	public static boolean deloyToWorkingDir(Map<String, Object> deployMap, File shareResourceTopDir) {
		if(!shareResourceTopDir.exists()){
			shareResourceTopDir.mkdir();
		}
		
		boolean hasResource = false;
		
		//创建共享资源目录
		try{
			String str_shareRubyNum = (String)deployMap.get(HCjar.SHARE_JRUBY_FILES_NUM);
			if(str_shareRubyNum != null){
				
				int shareRubyNum = Integer.parseInt(str_shareRubyNum);
				for (int idx = 0; idx < shareRubyNum; idx++) {
					final String fileName = (String)deployMap.get(HCjar.replaceIdxPattern(HCjar.SHARE_JRUBY_FILE_NAME, idx));
					final String fileContent = (String)deployMap.get(HCjar.replaceIdxPattern(HCjar.SHARE_JRUBY_FILE_CONTENT, idx));
					
					final File jrubyFile = new File(shareResourceTopDir, fileName);
					
					FileOutputStream fos = new FileOutputStream(jrubyFile);
					fos.write(fileContent.getBytes(Charset.forName(IConstant.UTF_8)));
					fos.flush();
					fos.close();
					
					hasResource = true;
				}
			}
			
			
			//创建共享jar
			for(Map.Entry<String, Object> entry:deployMap.entrySet()){  
				final String keyName = entry.getKey();
				if(keyName.startsWith(HCjar.VERSION_FILE_PRE)){
					final String name = keyName.substring(HCjar.VERSION_FILE_PRE.length());
					final byte[] content = (byte[])deployMap.get(HCjar.MAP_FILE_PRE + name);
					
					final File file = new File(shareResourceTopDir, name);
					
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(content);
					fos.flush();
					fos.close();
					
					hasResource = true;
				}
			}
		}catch(Throwable e){
			e.printStackTrace();
		}
		
		return hasResource;
	}
	
	public Object onEvent(Object event) {
		if(event == null){
			return null;
		}else if(isSysEvent(event)){
			
			//优先执行主菜单的事件
			String script = (String)map.get(HCjar.buildEventMapKey(mainMenuIdx, (String)event));
			if(script != null && script.trim().length() > 0){
//				System.out.println("OnEvent : " + event + ", script : " + script + ", scriptcontain : " + hcje.container);
				RubyExector.run(script, null, hcje, context);//考虑到PROJ_SHUTDOWN，所以改为阻塞模式
			}
			
			//其次执行非主菜单的事件脚本，依自然序列
			for (int i = 0; i < menu.length; i++) {
				if(i == mainMenuIdx){
					continue;
				}
				script = (String)map.get(HCjar.buildEventMapKey(i, (String)event));
				if(script != null && script.trim().length() > 0){
//					System.out.println("OnEvent : " + event + ", script : " + script + ", scriptcontain : " + hcje.container);
					RubyExector.run(script, null, hcje, context);//考虑到PROJ_SHUTDOWN，所以改为阻塞模式
				}
			}

			return null;
		}else{
			return null;
		}
	}
	
	public final int PROP_LISTENER = 1;
	public final int PROP_EXTENDMAP = 2;
	public final int PROP_ITEM_NAME = 3;
	
	private String getItemProp(HCURL url, final int type, boolean log) {
		for (int i = 0; i < menu.length; i++) {
			final JarMainMenu jarMainMenu = menu[i];
			final String[][] menuItems = jarMainMenu.items;
			for (int j = 0, size = menuItems.length; j < size; j++) {
				if(menuItems[j][JarMainMenu.ITEM_URL_IDX].equals(url.url)){
					if(log){
						L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "click item : [" + menuItems[j][JarMainMenu.ITEM_NAME_IDX] + "]");
					}
					if(type == PROP_LISTENER){
						return jarMainMenu.listener[j];
					}else if(type == PROP_EXTENDMAP){
						return jarMainMenu.extendMap[j];
					}else if(type == PROP_ITEM_NAME){
						return menuItems[j][JarMainMenu.ITEM_NAME_IDX];
					}else{
						return null;
					}
				}
			}
		}
		return null;
	}
	
	public static boolean isSysEvent(Object event){
		for (int i = 0; i < BaseResponsor.EVENT_LIST.length; i++) {
			if(event == BaseResponsor.EVENT_LIST[i]){
				return true;
			}
		}
		return false;
	}
	
	private boolean isMainElementID(String elementID){
		for (int i = 0; i < menu.length; i++) {
			if(menu[i].menuId.equals(elementID)){
				return menu[i].isRoot;
			}
		}
		return false;
	}

	public boolean doBiz(HCURL url) {
		context.__tmp_target = url.url;
		
		MUIView e = null;
		if(url.protocal == HCURL.MENU_PROTOCAL){
			int currMenuIdx = -1;
			
			final String elementID = url.elementID;
			if(isMainElementID(elementID)){
				currMenuIdx = mainMenuIdx;
			}else{
//				关闭多菜单模式
				for (int i = 0; i < menu.length; i++) {
					if(elementID.equals(menu[i].menuId)){
						currMenuIdx = i;
						break;
					}
				}
			}
			if(currMenuIdx >= 0 && currMenuIdx < menu.length){
				final JarMainMenu currMainMenu = menu[currMenuIdx];
				L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "open menu : [" + currMainMenu.getTitle() + "]");
				ServerUIUtil.response(currMainMenu.buildJcip());
				
				ScreenServer.pushScreen(currMainMenu);
				return true;
	//			}else if(url.elementID.equals("no1")){
	//			e = new Mno1Menu();
			}

		}else if(url.protocal == HCURL.CMD_PROTOCAL){
			//由于可能不同context，所以要进行全遍历，而非假定在前一当前打开的基础上。
			final String listener = getItemProp(url, PROP_LISTENER, true);
			if(listener != null && listener.trim().length() > 0){
				final Map<String, String> mapRuby = RubyExector.toMap(url);
				
				//由于某些长任务，可能导致KeepAlive被长时间等待，而导致手机端侦测断线，所以本处采用后台模式
				RubyExector.runLater(listener, mapRuby, hcje, context);
				return true;
			}

		}else if(url.protocal == HCURL.FORM_PROTOCAL){
//			if(url.elementID.equals("form1")){
//				e = new TestMForm();
//				JcipManager.addFormTimer("form1", new IFormTimer(){
//					int count = 1;
//					boolean isSend = false;
//					public String doAutoResponse() {
//						if(isSend == false){
//							isSend = true;
//							try {
//								ServerUIUtil.sendMessage("Cap", "Hello,Message", IContext.ERROR, ImageIO.read(ResourceUtil.getResource("hc/res/hc_48.jpg")), 0);
//								
//								ServerUIUtil.sendMessage("Cap2", "Hello,Message2", IContext.ERROR, null, 0);
//								
//								try {
//									ServerUIUtil.sendAUSound(ResourceUtil.getAbsPathContent("/hc/server/ui/ship.au"));
//								} catch (Exception e) {
//									e.printStackTrace();
//								}
//							} catch (IOException e1) {
//								e1.printStackTrace();
//							}
//						}
//						if(1+5 < 2){
//							hc.core.L.V=hc.core.L.O?false:LogManager.log("Send out Alert");
//							if(count < 100){
//								ServerUIUtil.alertOn();
//							}else{
//								ServerUIUtil.alertOff();	
//							}
//						}
//						return "{<'/form1'>,<['false','true'],'1'>,<'"+System.currentTimeMillis()+"'>,<'','Sys_Img','1'>,<{'0', '1','16'},'" + (count++) + "'><'0','UCB_BASIC_LATIN','" + (count++) + "'>,<'50','50','50'>}";
//					}
//
//					public int getSecondMS() {
//						return 1000;
//					}});
//				TODO onPause, onResume
//				ScreenServer.pushScreen(e);
//				L.V = L.O ? false : LogManager.log("onStart Form : " + url.elementID);
//			}
		}else if(url.protocal == HCURL.SCREEN_PROTOCAL){
			//由于可能不同context，所以要进行全遍历，而非假定在前一当前打开的基础上。
			final String listener = getItemProp(url, PROP_LISTENER, true);
			if(listener != null && listener.trim().length() > 0){
				final Map<String, String> mapRuby = RubyExector.toMap(url);

				try{
					Mlet mlet = (Mlet)RubyExector.run(listener, mapRuby, hcje, context);
					if(mlet == null){
						LogManager.errToLog("Error object return by JRuby, It should be " + Mlet.class.getName());
						return true;
					}
					MCanvas mcanvas = new MCanvas(ClientDesc.clientWidth, ClientDesc.clientHeight);
					
					mcanvas.setMlet(mlet);
					mcanvas.setCaptureID(url.elementID);
					mcanvas.init();
					
					ScreenServer.pushScreen(mcanvas);

					L.V = L.O ? false : LogManager.log(" onStart Mlet screen : [" + url.elementID + "]");
				}catch (Exception e1) {
					e1.printStackTrace();
				}
				return true;
			}
		}else if(url.protocal == HCURL.CONTROLLER_PROTOCAL){
			final StoreableHashMap map = new StoreableHashMap();
			final String map_str = getItemProp(url, PROP_EXTENDMAP, true);
//			System.out.println("extendMap : " + map_str);
			map.restore(map_str);
			final String listener = getItemProp(url, PROP_LISTENER, false);
			if(listener != null && listener.trim().length() > 0){
				final Map<String, String> mapRuby = null;//RubyExector.toMap(url);

				try{
					CtrlResponse responsor = (CtrlResponse)RubyExector.run(listener, mapRuby, hcje, context);
					if(responsor == null){
						LogManager.errToLog("Error object return by JRuby, It should be " + CtrlResponse.class.getName());
						return true;
					}
					CtrlMap cmap = new CtrlMap(map);
					
					//添加初始按钮名
					final int[] keys = cmap.getButtonsOnCanvas();
					for (int i = 0; i < keys.length; i++) {
						final String txt = responsor.getButtonInitText(keys[i]);
						cmap.setButtonTxt(keys[i], txt);
					}
					
					cmap.setTitle(getItemProp(url, PROP_ITEM_NAME, false));
					cmap.setID(url.url);
					ServerUIUtil.response(new MController(map, cmap.map.toSerial()).buildJcip());
					
					responsor.__hide_currentCtrlID = url.url;
					
					ServCtrlCanvas ccanvas = new ServCtrlCanvas(responsor);
					ScreenServer.pushScreen(ccanvas);

//					L.V = L.O ? false : LogManager.log("onLoad controller : " + url.elementID);
				}catch (Exception e1) {
					e1.printStackTrace();
				}
				return true;
			}
		}
		if(e != null){
			ServerUIUtil.response(e.buildJcip());
			return true;
		}else{
			LogManager.err("Not found resource:" + url.url);
		}
		return false;
	}

}
