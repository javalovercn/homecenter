package hc.server.util.ai;

import hc.core.ContextManager;
import hc.core.HCConditionWatcher;
import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPriorityManager;
import hc.server.html5.syn.DifferTodo;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.HTMLMlet;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.SessionMobiMenu;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.MobiUIResponsor;
import hc.server.ui.design.SystemDialog;
import hc.server.ui.design.SystemHTMLMlet;
import hc.server.util.VoiceCommand;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;

import third.hsqldb.jdbc.JDBCDriver;

public class AIPersistentManager {
	
	final static File AI_DIR = new File(ResourceUtil.getBaseDir(), "ai");
	
	static {
		if(AI_DIR.exists() == false){
			AI_DIR.mkdirs();
		}
	}
	
    public final IdentitySM identitySM;
    public final LabelManager labelSM;
    public final XMLFlowManager xmlSM;
    public final CtrlManager ctrlSM;
    public final KeySM keySM;
    public final RLabelKeySM rLabelKeySM;
    public final ItemTitleSM itemTitleSM;
    public final ProjectTitleSM projTitleSM;
    
    private Connection conn;
    final String projectID;
    public final boolean isLowcase = true;
    public final Locale locale = Locale.getDefault();
    
    public AIPersistentManager(final String projectID) throws SQLException {
    	this.projectID = projectID;
    	
    	identitySM = new IdentitySM(this);
    	rLabelKeySM = new RLabelKeySM(this);
    	keySM = new KeySM(this, rLabelKeySM);
    	labelSM = new LabelManager(this, keySM);
    	xmlSM = new XMLFlowManager(this);
    	ctrlSM = new CtrlManager(this);
    	projTitleSM = new ProjectTitleSM(this);
    	itemTitleSM = new ItemTitleSM(this);
    }
    
	private static Boolean isEnableHCAI;
	
	public static boolean isAnalysableUIObject(final Object uiObj){
		if(uiObj == null){
			return false;
		}
		
		if(uiObj instanceof SystemHTMLMlet
				|| uiObj instanceof SystemDialog){
			return false;
		}
		
		return true;
	}
	
	/**
	 * null means not found
	 * @param locale
	 * @param voice
	 * @return
	 */
	public static ProjectTargetForAI query(final J2SESession coreSS, final String locale, final String voice, 
			final String[] projIDS, final int projNum){
		final List<String> keys = LuceneManager.tokenizeString(locale, voice);
		if(keys == null){
			return null;
		}
		
		final TotalScreenScore tss = new TotalScreenScore();
		delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
				final Query query = new Query(keys);
				
				for (int i = 0; i < projNum; i++) {
					final String projectID = projIDS[i];
					final AIPersistentManager mgr = AIPersistentManager.getManagerByProjectIDInDelay(projectID);

					try{
						mgr.keySM.queryLableID(query);
						final Vector<MatchScore> score = query.getScoreList();
						if(score != null){
							final int size = score.size();
							for (int j = 0; j < size; j++) {
								final MatchScore ms = score.get(j);
								final int labelID = ms.labelData.id;
								final String target = mgr.labelSM.getTarget(labelID, ms.labelData.lableSrc);
								if(target != null){//null 无效类型
									tss.addScreenScore(projectID, target, ms);
								}
							}
							
							for (int j = 0; j < size; j++) {
								final MatchScore ms = score.get(j);
								final int labelID = ms.labelData.id;
								final String target = mgr.labelSM.getTitleTarget(labelID, ms.labelData.lableSrc);
								if(target != null){//null 无效类型
									tss.addKeyOnly(projectID, target, ms.fromKey);
								}
							}
						}
						query.reset();
					}catch (final Exception e) {
						ExceptionReporter.printStackTrace(e);
					}
				}
				
				synchronized (tss) {
					tss.notify();
				}
			}
		});
		
		synchronized (tss) {
			try{
				tss.wait();
			}catch (final Exception e) {
			}
		}
		
		final Vector<ScreenScore> listScore = tss.listScore;
		if(listScore == null){
			return null;
		}
		
		Collections.sort(listScore);
		
		if(L.isInWorkshop){
			final StringBuilder sb = StringBuilderCacher.getFree();
			{
				final ScreenScore screenScore = listScore.get(0);
				final Vector<String> listKeys = screenScore.listKeys;
				sb.append("----------------------0--------------------------");
				final int keySize = listKeys.size();
				sb.append("\nmatched keys : ");
				for (int i = 0; i < keySize; i++) {
					if(i != 0){
						sb.append(", ");
					}
					sb.append(listKeys.get(i));
				}
				sb.append("\n");
				if(screenScore.labelScore != null){
					sb.append("Highest MatchScore : ");
					final MatchScore matchScore = screenScore.labelScore.get(0);
					sb.append("matchKeyNum = " + matchScore.matchKeyNum);
					sb.append(", matchSequenceNum = " + matchScore.matchSequenceNum);
					sb.append("\n");
				}
				sb.append("-------------------------------------------------");
			}
			
			if(listScore.size() > 1){
				final ScreenScore screenScore = listScore.get(1);
				
				sb.append("----------------------1--------------------------");
				final Vector<String> listKeys = screenScore.listKeys;
				final int keySize = listKeys.size();
				sb.append("matched keys : ");
				for (int i = 0; i < keySize; i++) {
					if(i != 0){
						sb.append(", ");
					}
					sb.append(listKeys.get(i));
				}
				sb.append("\n");
				if(screenScore.labelScore != null){
					sb.append("Highest MatchScore : ");
					final MatchScore matchScore = screenScore.labelScore.get(0);
					sb.append("matchKeyNum = " + matchScore.matchKeyNum);
					sb.append(", matchSequenceNum = " + matchScore.matchSequenceNum);
					sb.append("\n");
				}
				sb.append("-------------------------------------------------");
			}
			
			LogManager.log(sb.toString());
			
			StringBuilderCacher.cycle(sb);
		}
		
		ScreenScore screenScore = null;
		for (int i = 0; i < listScore.size(); i++) {
			screenScore = listScore.get(i);
			final String url = screenScore.target;
			
			if(searchURL(coreSS, screenScore, url)){//大小写不敏感，原为大小写敏感
				break;
			}else{
				final AIPersistentManager mgr = AIPersistentManager.getManagerByProjectIDInDelay(screenScore.projectID);
				try{
					L.V = L.WShop ? false : LogManager.log("delete screen ID : " + url);
					mgr.itemTitleSM.deleteScreenID(url);
					mgr.xmlSM.deleteScreenID(url);
				}catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}
				screenScore = null;
			}
		}

		if(screenScore == null){
			return null;
		}
		
		final ProjectTargetForAI target = new ProjectTargetForAI(screenScore.projectID, screenScore.target);
		return target;
	}
	
	public static final void processProjectNameAndItemName(final J2SESession coreSS, final MobiUIResponsor resp){
		delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
				try{
					resp.processProjectNameAndItemNameImpl(coreSS);
				}catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
		});
	}

	private static final boolean searchURL(final J2SESession coreSS, final ScreenScore screenScore, final String url) {
		final String sessionProjID = screenScore.projectID;
		final SessionMobiMenu sessionMobiMenu = coreSS.getMenu(sessionProjID);
		if(sessionMobiMenu == null){
			L.V = L.WShop ? false : LogManager.log("project [" + sessionProjID + "] is disabled!");
			return false;
		}
		
		return sessionMobiMenu.searchMenuItem(url.toLowerCase(), null) != null;
//		final Vector<MenuItem> menuItems = sessionMobiMenu.getFlushMenuItems();//coreSS.getDisplayMenuItems();
//		final int size = menuItems.size();
//		for (int j = 0; j < size; j++) {
//			final MenuItem item = menuItems.elementAt(j);
//			final String itemUrl = ServerUIAPIAgent.getMobiMenuItem_URL(item);
//			if(url.equals(itemUrl)){
//				return true;
//			}
//		}
//		return false;
	}
	
	public static boolean isEnableHCAI(){
		if(isEnableHCAI == null){
			synchronized (AIPersistentManager.class) {
				if(isEnableHCAI == null){
					isEnableHCAI = PropertiesManager.isTrue(PropertiesManager.p_isEnableHCAI, true);
				}
			}
		}
		return isEnableHCAI;
	}
	
	public static void refreshEnableHCAI(){
		synchronized (AIPersistentManager.class) {
			isEnableHCAI = null;
			isEnableHCAI();
		}
	}
	
	boolean isShutdown;
	final long delayMS = 1000;
	
	private static Connection createConnection(final String projectID) throws SQLException{
		final File projDir = buildAIDirForProj(projectID);
		final File defaultFile = new File(projDir, "hc");
		if(defaultFile.exists() == false){
			defaultFile.mkdirs();
		}
		return getConnection(defaultFile.getAbsolutePath(), "root", "123456");
	}

	public static Connection getConnection(final String absolutePath, final String user, final String password) throws SQLException {
		final String url = "jdbc:hsqldb:file:" + absolutePath;
		
		final Properties props = new Properties();
        props.setProperty("loginTimeout", Integer.toString(0));
		props.setProperty("user", user);
        props.setProperty("password", password);
		return JDBCDriver.getConnection(url, props);
	}
	
	public static void removeAndNotUpgrade(final String[] projID){
		CCoreUtil.checkAccess();
		
		if(projID == null){
			return;
		}
		
		delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
				for (int i = 0; i < projID.length; i++) {
					final String projectID = projID[i];
					if(projectID != null){//可能为null
						PropertiesManager.addDelFile(AIPersistentManager.buildAIDirForProj(projectID));				
						removeManagerByProjectID(projectID);
//						AIPersistentManager.removeAndCloseDB(projectID);//有可能用户不重启
					}
				}
			}
		});
	}
	
	private static String convertToProjectNameFromDir(final String dirBase64){
		final byte[] bs = ByteUtil.toBytesFromHexStr(dirBase64);
		return StringUtil.bytesToString(bs, 0, bs.length);
	}
	
	private static File buildAIDirForProj(final String projectID) {
		final String base64 = ByteUtil.toHex(StringUtil.getBytes(projectID));//注意：它的逆方法convertToProjectNameFromDir
		final File projDir = new File(AI_DIR, base64);
		return projDir;
	}
	
    public static void printRS( final ResultSet rs ) throws SQLException{
        while(rs.next()){
            for(int i=1; i<=rs.getMetaData().getColumnCount(); i++){
                System.out.print(rs.getObject(i)+"\t");
            }
            System.out.println();
        }
    }
    
    final static HashMap<String, AIPersistentManager> projectMap = new HashMap<String, AIPersistentManager>(6);
    
    //注意：在工程关闭前退出。参见ExitManager.exit/HCTimer.shutDown
    //请不要将实现移到各工程的sequenceExecutor，因为需要Query及共用数据库
    final static HCConditionWatcher delayExecutor = new HCConditionWatcher("AIPersistent", ThreadPriorityManager.AI_BACKGROUND);
    
    public static void waitForAllDone(){
    	delayExecutor.waitForAllDone();
    	delayExecutor.notifyAllDoneAfterShutdown();
    }
    
    public static void processFormData(final FormData data){
    	delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
				processFormDataImpl(data);
			}
		});
    }
    
    /**
     * 
     * @param labelLocale
     * @param ctrlID
     * @param textOrArray 可能为String或String[]
     * @param ctx
     */
    public static void processCtrl(final String labelLocale, final String ctrlID, final Object textOrArray, final ProjectContext ctx){
    	delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
				final AIPersistentManager mgr = getManagerByProjectIDInDelay(ctx.getProjectID());
				if(textOrArray instanceof String){
					processAttribute(labelLocale, ctrlID, (String)textOrArray, mgr);
				}else{
					final String[] attributes = (String[])textOrArray;
					final int size = attributes.length;
					for (int i = 0; i < size; i++) {
						final String att = attributes[i];
						processAttribute(labelLocale, ctrlID, att, mgr);
					}
				}
			}

			private final void processAttribute(final String labelLocale, final String ctrlID, final String statusAtt,
					final AIPersistentManager mgr) {
				int labelID = mgr.labelSM.getID(LabelManager.LABEL_SRC_CTRL_ATTRIBUTE, labelLocale, statusAtt);
		    	if(labelID == -1){
		    		final List<String> keys = LuceneManager.tokenizeString(labelLocale, statusAtt);
		    		if(keys == null){
		    			return;
		    		}

		    		labelID = mgr.labelSM.appendData(LabelManager.LABEL_SRC_CTRL_ATTRIBUTE, labelLocale, statusAtt, 
		    				keys);
		    		
		    		final int id = mgr.identitySM.getNextID(mgr.ctrlSM.tableName);
		    		mgr.ctrlSM.appendCtrlData(id, labelID, ctrlID);
		    		
		    		return;
		    	}//注意：else仍要继续下步
		    	
		    	if(labelID == -1){
		    		return;
		    	}
		    	
		    	if(mgr.ctrlSM.searchCtrlData(labelID, ctrlID) > 0){
		    		return;
		    	}
		    	
		    	final int id = mgr.identitySM.getNextID(mgr.xmlSM.tableName);
		    	mgr.ctrlSM.appendCtrlData(id, labelID, ctrlID);
		    	
		    	L.V = L.WShop ? false : LogManager.log("[AI] success append Ctrllor data!");
			}
		});
    }
    
    public static final boolean isEnableForSomeComponent = false;
    
    public static final boolean isEnableAnalyseFlow = false;
    
    public static final String EMPTY_DIFF_TEXT = "";
    public static final String EXEC_SCRIPT = "execScript";
    public static final String setButtonText = "setButtonText";
    public static final String setCheckboxText = "setCheckboxText";
    public static final String changeSliderValue = "changeSliderValue";
    public static final String setProgressBarValue = "setProgressBarValue";
    public static final String changeComboBoxSelected = "changeComboBoxSelected";
    public static final String setLabelText = "setLabelText";
    public static final String setTextComponentText = "setTextComponentText";
    public static final String setTextAreaText = "setTextAreaText";
    
    public static void processJComponentToolTip(final J2SESession coreSS, final ProjectContext ctx, final HTMLMlet mlet){
    	delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
				ctx.runAndWaitInProjectLevel(new Runnable() {
					@Override
					public void run() {
						processJComponentToolTip(coreSS, mlet);
					}
				});
			}
			
			private final void processJComponentToolTip(final J2SESession coreSS, final JComponent jcomp){
				final String toolTip = jcomp.getToolTipText();
				if(toolTip != null && toolTip.length() > 0){
					processLabel(coreSS, jcomp, ctx, "", toolTip, LabelManager.LABEL_SRC_XML_TOOLTIP);
				}
				
				if(jcomp instanceof JPanel){
					try{
						final int size = jcomp.getComponentCount();
						for (int i = 0; i < size; i++) {
							final Component sub = jcomp.getComponent(i);
							if(sub instanceof JComponent){
								processJComponentToolTip(coreSS, (JComponent)sub);
							}
						}
					}catch (final ArrayIndexOutOfBoundsException  e) {
					}
				}
			}
		});
    }
    
    public static void processDiffNotify(final boolean isJLabel, final J2SESession coreSS, final Component comp, 
    		final ProjectContext ctx, final String cmd, final String text){//in user thread
    	delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
				ctx.runAndWaitInProjectLevel(new Runnable() {
					@Override
					public void run() {
						processLabel(coreSS, comp, ctx, cmd, text, LabelManager.LABEL_SRC_XML_DATA);
					}
				});
			}
		});
    }
    
    private static void buildFlowRecord(final Component comp, final String labelLocale, final String screenID, final String locKey, 
    		final ProjectContext ctx, final String cmd, String text, final int labelSrc){
    	final AIPersistentManager mgr = getManagerByProjectIDInDelay(ctx.getProjectID());
    	if(mgr.xmlSM.isReachMax(cmd, locKey, screenID)){
    		return;
    	}
    	
    	text = AIUtil.findPattern(text);
    	
    	int labelID = mgr.labelSM.getID(labelSrc, labelLocale, text);
    	if(labelID == -1){
    		final List<String> keys = LuceneManager.tokenizeString(labelLocale, text);
    		if(keys == null){
    			return;
    		}

    		labelID = mgr.labelSM.appendData(labelSrc, labelLocale, text, keys);
    		
    		final int xmlID = mgr.identitySM.getNextID(mgr.xmlSM.tableName);
    		mgr.xmlSM.appendXMLData(xmlID, cmd, locKey, screenID, labelID);
    		
    		return;
    	}
    	
    	if(labelID == -1){
    		return;
    	}
    	
    	if(mgr.xmlSM.searchXMLData(cmd, locKey, screenID, labelID) > 0){
    		return;
    	}
    	
    	final int xmlID = mgr.identitySM.getNextID(mgr.xmlSM.tableName);
    	mgr.xmlSM.appendXMLData(xmlID, cmd, locKey, screenID, labelID);
    	
    	L.V = L.WShop ? false : LogManager.log("[AI] success append XML data!");
    }
    
    private static Mlet buildComponentLocKey(final Component comp, final StringBuilder sb){
    	final Container c = comp.getParent();
    	
    	if(c == null){
    		return null;
    	}
    	
    	Mlet out;
    	if(c instanceof Mlet){
    		out = (Mlet)c;
    	}else{
    		out = buildComponentLocKey(c, sb);
    		if(out == null){
    			return null;
    		}
    	}
    	
		final Class cc = comp.getClass();
		sb.append('/');
		final int size = c.getComponentCount();
		for (int i = 0; i < size; i++) {
			final Component cItem = c.getComponent(i);
			if(cItem == comp){
				sb.append(i);
				sb.append('/');
				break;
			}
		}
    	sb.append(cc.getSimpleName());
    	
    	return out;
    }
    
    public static void processAddComponent(final JComponent comp, final DifferTodo todo){
    	delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
				processAddComponentImpl(comp, todo);
			}
		});
    }
    
    private static void processAddComponentImpl(final JComponent comp, final DifferTodo todo){
    	
    }
    
    public static void processRobotData(final RobotData data){
    	delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
				processRobotDataImpl(data);
			}
		});
    }
    
    public static void processRobotEventAndRecycle(final RobotEventData data){
		delayExecutor.addWatcher(new AIExecBiz() {
			@Override
			public void doBiz() {
			}
		});
    }
    
    private static Boolean isDoneCheck;
    
    public static final void checkCompact(){
    	if(isDoneCheck == null){
	    	isDoneCheck = Boolean.TRUE;

	    	final String projID = PropertiesManager.getValue(PropertiesManager.p_compackingAIDB);
	    	if(projID != null){
	    		final File projDir = buildAIDirForProj(projID);
	    		ResourceUtil.deleteDirectoryNow(projDir, true);
	    		
	    		PropertiesManager.remove(PropertiesManager.p_compackingAIDB);
	    		PropertiesManager.saveFile();
	    	}
	    	
	    	final String lastMS = PropertiesManager.getValue(PropertiesManager.p_compackAIDBLastMS);
	    	long last = 0;
	    	if(lastMS != null){
	    		try{
	    			last = Long.parseLong(lastMS);
	    		}catch (final Exception e) {
				}
	    	}
	    	if(last == 0){
	    		PropertiesManager.setValue(PropertiesManager.p_compackAIDBLastMS, String.valueOf(System.currentTimeMillis()));
	    		PropertiesManager.saveFile();
	    	}else{
	    		final long diff = System.currentTimeMillis() - last;
	    		if(diff > HCTimer.ONE_DAY * 30){
	    			ContextManager.getThreadPool().run(new Runnable() {
	    				@Override
	    				public void run() {
	    					try{
	    						Thread.sleep(1000 * 60 * 5);
	    					}catch (final Exception e) {
	    					}
	    					
	    					final File[] projs = AI_DIR.listFiles();
	    					final int size = projs.length;
	    					for (int i = 0; i < size; i++) {
	    						if(J2SESessionManager.isShutdown()){
	    							return;
	    						}
	    						
	    						final File file = projs[i];
	    						if(file.isDirectory()){
	    							final String projID = convertToProjectNameFromDir(file.getName());
	    							
	    							delayExecutor.addWatcher(new AIExecBiz() {
	    								@Override
	    								public void doBiz() {
	    	    							final AIPersistentManager aip = getManagerByProjectIDInDelay(projID);
	    	    							aip.compact();
	    									projectMap.remove(projID);//注意：连接已关闭，实例的preparestatement已失效
	    								}
	    							});
	    						}
	    					}
	    					
	    					delayExecutor.addWatcher(new AIExecBiz() {
								@Override
								public void doBiz() {
			    					PropertiesManager.setValue(PropertiesManager.p_compackAIDBLastMS, String.valueOf(System.currentTimeMillis()));
			    		    		PropertiesManager.saveFile();
								}
							});
	    				}
	    			});
    			}
	    	}
    	}
    	
    	isEnableHCAI();//初始化
    }
    
    private static void processRobotDataImpl(final RobotData data){
    	AIObjectCache.cycleRobotData(data);
    }
    
    /**
     * 
     * @param vc
     * @return true means consumed
     */
    public static boolean processVoiceCommand(final VoiceCommand vc){
    	//注意：不delayExec
    	return false;
    }
    
    private static void processFormDataImpl(final FormData data){
    	AIObjectCache.cycleFormData(data);
    }
    
    public static AIPersistentManager getManagerByProjectIDInDelayFromResp(final String proj){
    	CCoreUtil.checkAccess();
    	return getManagerByProjectIDInDelay(proj);
    }
    
    private static AIPersistentManager getManagerByProjectIDInDelay(final String proj){
    	AIPersistentManager out = projectMap.get(proj);
    	if(out == null){
    		try{
	    		out = new AIPersistentManager(proj);
	    		projectMap.put(proj, out);
    		}catch (final SQLException e) {
    			ExceptionReporter.printStackTrace(e);
    		}
    	}
    	return out;
    }
    
    public static void clear(){//无需实现
//    	projectMap.clear();
    }
    
    private static void removeManagerByProjectID(final String proj){
    	final AIPersistentManager mgr = projectMap.remove(proj);
    	if(mgr != null){
    		mgr.close();
    	}
    }
    
    public static void main(final String[] args){
    	checkCompact();
    	
//    	final AIPersistentManager mgr = new AIPersistentManager("TestProj");
//    	new Thread(){
//    		@Override
//			public void run(){
//    	    	final Connection conn = mgr.getConnection();
//    	    	doStep(conn);
//    		}
//    	}.start();
//    	new Thread(){
//    		@Override
//			public void run(){
//    	    	final Connection conn = mgr.getConnection();
//    	    	doStep2(conn);
//    		}
//    	}.start();
    }

	private static void doStep(final Connection conn) {
		Statement state;
		try {
//	    	conn.setAutoCommit(false);

	    	state = conn.createStatement();
//	    	state.execute("create table HC2 (rowid integer, rowdata varchar(100));");
//	    	state.executeUpdate("insert into HC (rowid, rowdata) values (12, 'helloworld12');");
//	    	state.executeUpdate("insert into HC (rowid, rowdata) values (2, 'HelloW2');");
	    	
//	    	state.execute("insert into HC (rowid, rowdata) values (10, 'helloworld中文');");
//	    	state.execute("insert into HC2 (rowid, rowdata) values (1, 'HelloW1');");
//	    	System.exit(0);
//	    	conn.commit();
	    	{
				final ResultSet rs = state.executeQuery("select rowid, rowdata from HC;");
				while(rs.next()){
					System.out.println("rowid : " + rs.getInt(1) + ", rowdata : " + rs.getString(2));
				}
				rs.close();
	    	}
	    	{
				final ResultSet rs = state.executeQuery("select rowid, rowdata from HC2;");
				while(rs.next()){
					System.out.println("rowid : " + rs.getInt(1) + ", rowdata : " + rs.getString(2));
				}
				rs.close();
	    	}
	    	
	    	try{
	    		Thread.sleep(1000);
	    	}catch (final Exception e) {
			}
	    	
	    	state.close();
			conn.close();
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void doStep2(final Connection conn) {
		Statement state;
		try {
	    	conn.setAutoCommit(false);

	    	state = conn.createStatement();
//	    	state.execute("create table HC (rowid integer, rowdata varchar(100));");
//	    	state.executeUpdate("insert into HC (rowid, rowdata) values (1, 'helloworld');");
//	    	state.executeUpdate("insert into HC (rowid, rowdata) values (2, 'HelloW2');");
	    	
//	    	state.execute("insert into HC2 (rowid, rowdata) values (2, 'HelloW2');");
	    	state.execute("insert into HC (rowid, rowdata) values (8, 'helloworld8');");
//	    	System.exit(0);
	    	conn.commit();
	    	
	    	{
				final ResultSet rs = state.executeQuery("select rowid, rowdata from HC;");
				while(rs.next()){
					System.out.println("rowid : " + rs.getInt(1) + ", rowdata : " + rs.getString(2));
				}
				rs.close();
	    	}
	    	{
				final ResultSet rs = state.executeQuery("select rowid, rowdata from HC2;");
				while(rs.next()){
					System.out.println("rowid : " + rs.getInt(1) + ", rowdata : " + rs.getString(2));
				}
				rs.close();
	    	}
			
//			conn.close();
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}
    
    private static void processLabel(final J2SESession coreSS, final Component comp,
			final ProjectContext ctx, final String cmd, final String text, final int labelSrc) {
		final StringBuilder sb = StringBuilderCacher.getFree();
		final Mlet mlet = buildComponentLocKey(comp, sb);
		if(mlet == null){
			StringBuilderCacher.cycle(sb);
			return;
		}
		
		final String screenID = mlet.getTarget();
		
		final String locKey = sb.toString();
		StringBuilderCacher.cycle(sb);
  	
//		buildStruct(comp, screenID, locKey, ctx);
		final String locale = UserThreadResourceUtil.getMobileLocaleFrom(coreSS);
		buildFlowRecord(comp, locale, screenID, locKey, ctx, cmd, text, labelSrc);
	}
    
    private final void compact(){
    	try{
    		PropertiesManager.setValue(PropertiesManager.p_compackingAIDB, projectID);
    		PropertiesManager.saveFile();
    		Thread.sleep(200);
    		
    		conn = getConnection();

    		final Statement state = conn.createStatement();
    		LogManager.log("ready to compact AI database for project : " + projectID);
	    	state.execute(ResourceUtil.SHUTDOWN_COMPACT);
	    	LogManager.log("successful done compact AI database for project : " + projectID);
	    	conn = null;
	    	
	    	PropertiesManager.remove(PropertiesManager.p_compackingAIDB);
	    	PropertiesManager.saveFile();
	    	Thread.sleep(200);
    	}catch (final Throwable e) {
    		ExceptionReporter.printStackTrace(e);
		}
    }

	public final Connection getConnection(){
    	try{
	    	if(conn == null || conn.isClosed()){
	    		conn = createConnection(projectID);
	    	}
	    	return conn;
    	}catch (final Exception e) {
    		ExceptionReporter.printStackTrace(e);
		}
    	
    	return null;
    }
    
    public final void close(){
    	try {
    		if(conn != null && conn.isClosed() == false){
    			conn.close();
    		}
		} catch (final SQLException e) {
			ExceptionReporter.printStackTrace(e);
		}
    	conn = null;
    }
    
    public final boolean isClosed(){
    	try {
			return conn != null && conn.isClosed();
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
    	return false;
    }
    
}
