package hc.server.ui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import hc.App;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.SessionManager;
import hc.core.data.DataPNG;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LangUtil;
import hc.core.util.LogManager;
import hc.core.util.RecycleRes;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.core.util.StringValue;
import hc.server.CallContext;
import hc.server.PlatformManager;
import hc.server.PlatformService;
import hc.server.StarterManager;
import hc.server.data.StoreDirManager;
import hc.server.data.screen.KeyComper;
import hc.server.msb.Converter;
import hc.server.msb.Device;
import hc.server.msb.Robot;
import hc.server.msb.RobotListener;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.SessionContext;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.engine.RubyExector;
import hc.server.util.Assistant;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.server.util.HCEventQueue;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.SafeDataManager;
import hc.server.util.Scheduler;
import hc.server.util.SystemEventListener;
import hc.server.util.VoiceCommand;
import hc.server.util.ai.AIObjectCache;
import hc.server.util.ai.AIPersistentManager;
import hc.server.util.ai.AnalysableData;
import hc.server.util.ai.FormData;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.PropertiesMap;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;
import hc.util.TokenManager;
import third.hsqldb.jdbc.JDBCDriver;

/**
 * There is one <code>ProjectContext</code> instance for each HAR Project at
 * runtime. <BR>
 * <BR>
 * a <code>ProjectContext</code> instance will be created before
 * {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}, and will be released after
 * {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}. <BR>
 * reloading HAR projects (NOT restart server), new instance of
 * <code>ProjectContext</code> will be created. <BR>
 * <BR>
 * it is easy to get instance of ProjectContext from everywhere, for examples :
 * <br>
 * 1. {@link ProjectContext#getProjectContext()} by static method,<br>
 * 2. {@link Mlet#getProjectContext()}, <br>
 * 3. {@link Assistant#getProjectContext()}, <br>
 * 4. {@link CtrlResponse#getProjectContext()}, <br>
 * 5. {@link Robot#getProjectContext()}, <br>
 * 6. {@link Converter#getProjectContext()}, <br>
 * 7. {@link Device#getProjectContext()},
 * 
 * @since 7.0
 */
public class ProjectContext {
	private final String projectID;
	private final boolean isLoggerOn;
	private final String projectVer;
	Assistant assistant;
	final RecycleRes recycleRes;
	final Vector<SystemEventListener> projectLevelEventListeners = new Vector<SystemEventListener>();
	Hashtable<String, Scheduler> schedulerMapForProjAndSess;// 线程安全必需
	final String dbPassword;
	final ProjectPropertiesManager ppm;
	final ProjDesignConf projDesignConfig;

	/**
	 * for example HAR is installing from client, and it is required to input
	 * token for device connection, you can use <code>Dialog</code> for custom
	 * input, otherwise if is installing from PC desktop, you must use
	 * {@link #showInputDialog(String, String[], String[])}. <BR>
	 * <BR>
	 * it returns false when project normal start up even if it was installed
	 * from client.
	 * 
	 * @return true means current project is installing from client.
	 * @since 7.46
	 */
	public final boolean isInstallingFromClient() {
		final Object out = ServerUIAPIAgent
				.getSysAttrInUserThread(ServerUIAPIAgent.KEY_IS_INSTALL_FROM_CLIENT, false);
		if (out != null && out instanceof Boolean) {
			return ((Boolean) out).booleanValue();
		}

		return false;
	}

	/**
	 * set GPS/WiFi location update interval time.<BR>
	 * it is a suggested value.<BR>
	 * <BR>
	 * if <code>minTimes</code> is upper than the default, then do nothing.<BR>
	 * if <code>minTimes</code> is lower than other project, then new
	 * <code>minTimes</code> applies to all projects.
	 * 
	 * @param milliseconds
	 *            minimum time interval between location updates, in
	 *            milliseconds
	 * @see #EVENT_SYS_MOBILE_LOCATION
	 * @see ClientSession#getLocationLongitude()
	 */
	public final void setLocationUpdates(final long milliseconds) {
		ServerUIAPIAgent.getProjResponserMaybeNull(this).setLocationUpdates(milliseconds);
	}

	/**
	 * the level for server class loader.
	 * 
	 * @see #getClassLoader(int)
	 * @deprecated
	 */
	@Deprecated
	static final int CLASSLOADER_SERVER_LEVEL = 1;
	/**
	 * the level for third libraries class loader.
	 * 
	 * @see #getClassLoader(int)
	 * @deprecated
	 */
	@Deprecated
	static final int CLASSLOADER_THIRD_LIBS_LEVEL = 2;
	/**
	 * the level for project class loader.
	 * 
	 * @see #getClassLoader(int)
	 * @deprecated
	 */
	@Deprecated
	static final int CLASSLOADER_PROJECT_LEVEL = 3;

	/**
	 * 1. classloader_system_level is created first, it includes HomeCenter
	 * runtime/API jars.<br>
	 * 2. if you added third jar libraries to HomeCenter server in configuration
	 * panel, then <i>classload_<b>server</b>_level = [third_libs...] +
	 * classload_<b>system</b>_level</i>; if no third jar is added, then
	 * <i>classload_<b>server</b>_level ≈ classload_<b>system</b>_level(NOT
	 * =)</i>.<br>
	 * 2.1 user maybe add third jar when HAR is running, then system will stop
	 * all running HAR, create new instance of classload_server_level, and start
	 * HAR again.<br>
	 * 3. jar libraries maybe added to HAR project, before start HAR,
	 * classloader_project_level is created. <i>classloader_<b>project</b>_level
	 * = [jar in HAR...] + classload_<b>server</b>_level</i>, if no jar in HAR,
	 * <i>classloader_<b>project</b>_level ≈ classload_<b>server</b>_level(NOT
	 * =)</i>.<br>
	 * 3.1 when add new HAR, or upgrade HAR, all HAR project instance will be
	 * shutdown, and new classloader_project_level instance will be created
	 * before start HAR. <br>
	 * <br>
	 * <b>important</b> : it is also available in Android server (NOT client).
	 * Jar library in J2SE standard will be converted to dex library
	 * dynamically, and dex classloader instance will be created.
	 * 
	 * @param level
	 *            one of {@link #CLASSLOADER_SERVER_LEVEL},
	 *            {@link #CLASSLOADER_THIRD_LIBS_LEVEL},
	 *            {@link #CLASSLOADER_PROJECT_LEVEL}.
	 * @return the ClassLoader.
	 * @deprecated
	 * @since 7.0
	 */
	@Deprecated
	private final ClassLoader getClassLoader(final int level) {
		if (level == CLASSLOADER_SERVER_LEVEL) {
			return PlatformService.SYSTEM_CLASS_LOADER;
		} else if (level == CLASSLOADER_THIRD_LIBS_LEVEL) {
			return PlatformManager.getService().get3rdAndServClassLoader(null);
		} else if (level == CLASSLOADER_PROJECT_LEVEL) {
			return finder.findProjClassLoader();
		} else {
			throw new Error("unknow classloader level. it shoud be one of "
					+ "CLASSLOADER_SYSTEM_LEVEL, CLASSLOADER_SERVER_LEVEL, CLASSLOADER_PROJECT_LEVEL");
		}
	}

	/**
	 * to create or open exists database.<BR>
	 * <BR>
	 * the database is stored in directory
	 * <i>{HC_Home}/user_data/{projectID}/_HC/DB/{dbName}</i>, for more see
	 * {@link #getPrivateFile(String)} <BR>
	 * <BR>
	 * <STRONG>Note :</STRONG><BR>
	 * 1. {@link java.sql.SQLXML} is NOT supported.<BR>
	 * 2. the default WRITE DELAY property of database is <code>true</code>,
	 * which is 500 milliseconds. To change it see
	 * <a href="http://hsqldb.org/doc/guide/guide.html">SET FILES WRITE
	 * DELAY</a>.<BR>
	 * 3. be careful the exception of deadlock when in multiple threads. <BR>
	 * <BR>
	 * the latest build-in database server is HSQLDB 2.3.4, user guide is
	 * <a href="http://hsqldb.org/doc/2.0/guide/index.html">here</a>.
	 * 
	 * @param dbName
	 * @param username
	 * @param password
	 * @return null means database error occurs.
	 * @see #closeDB(Connection)
	 * @see #removeDB(String)
	 * @see #backup()
	 * @since 7.50
	 */
	public final Connection getDBConnection(final String dbName, final String username,
			final String password) {
		return getDBConnection(dbName, username, password, false);
	}

	/**
	 * create or open exists database, which stored in files or
	 * all-in-memory.<BR>
	 * <BR>
	 * for more, see {@link #getDBConnection(String, String, String)}.
	 * 
	 * @param dbName
	 * @param username
	 * @param password
	 * @param isAllInMem
	 *            true means stored entirely in RAM, without any persistence
	 *            beyond the JVM process's life
	 * @return null means database error occurs.
	 * @see #getDBConnection(String, String, String)
	 * @see #getDBConnection(String, String, String, boolean, Properties)
	 * @see #closeDB(Connection)
	 * @see #removeDB(String)
	 * @since 7.50
	 */
	public final Connection getDBConnection(final String dbName, final String username,
			final String password, final boolean isAllInMem) {
		return getDBConnection(dbName, username, password, isAllInMem, buildDBProp());
	}

	private final Properties buildDBProp() {
		final Properties props = new Properties();
		props.setProperty("loginTimeout", Integer.toString(0));
		return props;
	}

	private final Object lockForGetDBConn = new Object();

	/**
	 * create or open exists database, which stored in files or
	 * all-in-memory.<BR>
	 * <BR>
	 * for more, see {@link #getDBConnection(String, String, String)}.
	 * 
	 * @param dbName
	 * @param username
	 * @param password
	 * @param isAllInMem
	 * @param props
	 *            list of arbitrary string tag/value pairs as connection
	 *            arguments
	 * @return null means database error occurs.
	 * @see #getDBConnection(String, String, String)
	 * @see #getDBConnection(String, String, String, boolean)
	 * @see #closeDB(Connection)
	 * @see #removeDB(String)
	 * @since 7.50
	 */
	public final Connection getDBConnection(final String dbName, final String username,
			final String password, final boolean isAllInMem, final Properties props) {
		try {
			synchronized (lockForGetDBConn) {
				final String url;
				if (isAllInMem) {
					final String hexProjID = ByteUtil.toHex(StringUtil.getBytes(projectID));
					url = "jdbc:hsqldb:mem:" + hexProjID + dbName;
				} else {
					url = buildDBURL(dbName);
				}

				props.setProperty("user", username);
				props.setProperty("password", password);

				Connection result = JDBCDriver.getConnection(url, props);
				if (isAllInMem == false) {
					if (compackDB(result, dbName)) {
						result = JDBCDriver.getConnection(url, props);
					}
				}
				return result;
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}

		return null;
	}

	private final boolean compackDB(final Connection connection, final String domainName) {
		if (System.currentTimeMillis()
				- getLastCompactMS(domainName) > projDesignConfig.compactDayMS) {// 低频率能增强数据库安全
			try {
				LogManager.log(
						"compacting database : " + domainName + " in project [" + projectID + "].");
				final Statement state = connection.createStatement();
				state.execute(ResourceUtil.SHUTDOWN_COMPACT);
				state.close();
				LogManager.log("done compacting database : " + domainName + " in project ["
						+ projectID + "].");
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
			setLastCompactMS(domainName);
			return true;
		}
		return false;
	}

	private final void setLastCompactMS(final String dbName) {
		final String prop = ServerUIAPIAgent.PROJ_USER_DB_COMPACT_MS + dbName;
		__setPropertySuperOnProj(prop, String.valueOf(System.currentTimeMillis()));
		saveProperties();
	}

	private final void removeLastCompactMS(final String dbName) {
		final String prop = ServerUIAPIAgent.PROJ_USER_DB_COMPACT_MS + dbName;
		__removePropertySuperOnProj(prop);
		saveProperties();
	}

	private final long getLastCompactMS(final String dbName) {
		final String prop = ServerUIAPIAgent.PROJ_USER_DB_COMPACT_MS + dbName;
		final String value = __getPropertySuperOnProj(prop);
		if (value == null) {
			return 0;
		} else {
			try {
				return Long.parseLong(value);
			} catch (final Exception e) {
			}
		}
		return 0;
	}

	private final String buildDBURL(final String dbName) {
		final String url;
		final File hcSysDir = buildDBBaseDir(dbName);
		if (hcSysDir.exists() == false) {
			setLastCompactMS(dbName);
		}
		final File defaultFile = new File(hcSysDir, dbName);
		url = "jdbc:hsqldb:file:" + defaultFile.getAbsolutePath();
		return url;
	}

	private final File buildDBBaseDir(final String dbName) {
		return getPrivateFile(StoreDirManager.HC_SYS_FOR_USER_PRIVATE_DIR
				+ StoreDirManager.DB_SUB_DIR_FOR_USER_PRIVATE_DIR + dbName + "/");
	}

	/**
	 * close a working database.<BR>
	 * <BR>
	 * it is not required to close DB when shutdown project.<BR>
	 * <BR>
	 * <STRONG>Tip</STRONG> :<BR>
	 * 1. server will compact each databases after six months (182 days).<BR>
	 * 2. to change frequency, see <STRONG>[Compact DB Days]</STRONG> of project
	 * node in designer.<BR>
	 * 
	 * @param connection
	 * @since 7.72
	 * @see #removeDB(String)
	 */
	public final void closeDB(final Connection connection) {
		closeDB(connection, false);
	}

	/**
	 * close a working database with compact or not.<BR>
	 * <BR>
	 * <STRONG>deprecated</STRONG>, replaced by
	 * {@link #closeDB(Connection)}.<BR>
	 * <BR>
	 * <STRONG>Tip</STRONG> :<BR>
	 * 1. server will compact each databases after six months.<BR>
	 * 2. if you need compact it more frequent, please do it in
	 * {@link #EVENT_SYS_PROJ_STARTUP}, NOT in
	 * {@link #EVENT_SYS_PROJ_SHUTDOWN}.<BR>
	 * 
	 * @param connection
	 * @param isCompactAlso
	 *            true means close database with compact mode.
	 * @since 7.50
	 * @see #removeDB(String)
	 * @deprecated
	 */
	@Deprecated
	public final void closeDB(final Connection connection, final boolean isCompactAlso) {
		ResourceUtil.shutdownHSQLDB(connection, isCompactAlso);
	}

	/**
	 * remove database named <code>dbName</code>. <BR>
	 * <BR>
	 * <STRONG>Note</STRONG> :<BR>
	 * before remove database, please ensure that the database is closed.<BR>
	 * to close a working database, invoke {@link #closeDB(Connection)}.
	 * 
	 * @param dbName
	 * @return true means all sub directories and files are deleted, false means
	 *         some file may be using and not removed.
	 * @see #getDBConnection(String, String, String)
	 * @since 7.50
	 */
	public final boolean removeDB(final String dbName) {
		removeLastCompactMS(dbName);
		log("remove user database : " + dbName);
		final File baseDir = buildDBBaseDir(dbName);
		return ResourceUtil.deleteDirectoryNow(baseDir, true);
	}

	private final Object lockForScheduler = new Object();

	/**
	 * create or open exists project/session level job scheduler. <BR>
	 * <BR>
	 * if current thread is session level, then return a session level
	 * scheduler.<BR>
	 * if current thread is project level, then return a project level
	 * scheduler.<BR>
	 * for session level scheduler, same <code>domain</code> in difference
	 * session means difference scheduler. <BR>
	 * <BR>
	 * usage of schedule (JRuby) :
	 * 
	 * <pre>
	 * import Java::hc.server.util.calendar.WeeklyJobCalendar
	 * import java.lang.StringBuilder
	 * 
	 * ctx = Java::hc.server.ui.ProjectContext::getProjectContext()
	 * scheduler = ctx.getScheduler("MyScheduler1")
	 * scheduler.start()
	 * 
	 * if scheduler.isExistsTrigger("Trigger1") == false
	 * &nbsp;&nbsp;builder = StringBuilder.new(100)
	 * &nbsp;&nbsp;builder.append("ctx = Java::hc.server.ui.ProjectContext::getProjectContext()\n")
	 * &nbsp;&nbsp;builder.append("ctx.showTipOnTray(\"executing job1\")\n")
	 * &nbsp;&nbsp;scheduler.addJob("Job1", builder.toString())
	 * 
	 * &nbsp;&nbsp;weeklyCalendar = WeeklyJobCalendar.new()
	 * &nbsp;&nbsp;weeklyCalendar.setDayExcluded(java.util.Calendar::SUNDAY, true)
	 * &nbsp;&nbsp;weeklyCalendar.setDayExcluded(java.util.Calendar::SATURDAY, true)
	 * &nbsp;&nbsp;scheduler.addCalendar("Calendar1", weeklyCalendar)
	 * 
	 * &nbsp;&nbsp;scheduler.addCronTrigger("Trigger1", "0/30 * * * * ?", "Calendar1", "Job1")#trigger "Job1" when "Calendar1" every 30 seconds
	 * &nbsp;&nbsp;puts "Trigger1 next fire time : " + scheduler.getTriggerNextFireTime("Trigger1").toString()
	 * end
	 * </pre>
	 * 
	 * <STRONG>Tip :</STRONG><BR>
	 * session level scheduler will be shutdown by server after session is
	 * logout/lineoff.<BR>
	 * project level scheduler will be shutdown by server after shutdown
	 * project. <BR>
	 * <BR>
	 * <STRONG>one scheduler or multiple?</STRONG><BR>
	 * 1. please add multiple jobs, calendars and triggers in one scheduler,
	 * <BR>
	 * 2. start, standby, clear, and shutdown a scheduler, <BR>
	 * 3. to stop/clear some triggers, not others, then you should create
	 * multiple schedulers.<BR>
	 * <BR>
	 * current job scheduler is based on Quartz 2.2.3<BR>
	 * 
	 * @param domain
	 *            each domain is stored in different database.
	 * @return if a scheduler is shutdown and invoke this method with the same
	 *         <code>domain</code>, then a new instance scheduler will be
	 *         created, otherwise the same instance returned.
	 * @see #getScheduler(String, boolean)
	 * @see #getScheduler(String, boolean, boolean)
	 * @see #backup()
	 * @since 7.57
	 */
	public final Scheduler getScheduler(final String domain) {
		return getScheduler(domain, false);
	}

	/**
	 * null means shutdown all.
	 * 
	 * @param list
	 */
	final void shutdownSchedulers(Vector<String> list) {
		synchronized (lockForScheduler) {
			if (schedulerMapForProjAndSess != null) {
				if (list == null) {
					list = new Vector<String>();
					list.addAll(schedulerMapForProjAndSess.keySet());
				}
			}
		}

		if (list == null) {
			return;
		}

		final int size = list.size();
		for (int i = 0; i < size; i++) {
			final String domain = list.elementAt(i);
			try {
				final Scheduler scheduler = schedulerMapForProjAndSess.remove(domain);
				if (scheduler != null) {// session级已shutdown，但仍在J2SESession中保留domain
					LogManager.log("shutdown scheduler [" + domain
							+ "] (session level format [sessionID_domain]) in project [" + projectID
							+ "].");
					scheduler.shutdown(true);
				}
			} catch (final Throwable e) {
				// e.printStackTrace();
			}
		}
	}

	/**
	 * 必须的，因为用户级调用shutdown，导致被动移除。
	 * 
	 * @param domain
	 */
	final void removeScheduler(final String domain) {
		if (schedulerMapForProjAndSess != null) {// 不能加锁，因为Job内可能调用此逻辑
			schedulerMapForProjAndSess.remove(domain);
		}
	}

	/**
	 * create or open exists project/session level job scheduler. <BR>
	 * <BR>
	 * if current thread is session level, then return a session level
	 * scheduler.<BR>
	 * if current thread is project level, then return a project level
	 * scheduler.<BR>
	 * for session level scheduler, same <code>domain</code> in difference
	 * session means difference scheduler.<BR>
	 * session level scheduler is all in RAM, even though pass
	 * <code>isAllInRAM</code> with false. <BR>
	 * <BR>
	 * <STRONG>Tip :</STRONG><BR>
	 * session level scheduler will be shutdown by server after session is
	 * logout/lineoff.<BR>
	 * project level scheduler will be shutdown by server after shutdown
	 * project.<BR>
	 * 
	 * @param domain
	 * @param isAllInRAM
	 *            true means all in RAM, false means stored in database.
	 * @return
	 * @since 7.57
	 * @see #getScheduler(String)
	 * @see #getScheduler(String, boolean, boolean)
	 */
	public final Scheduler getScheduler(final String domain, final boolean isAllInRAM) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return getScheduler(domain, isAllInRAM, null);
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			return getScheduler(domain, isAllInRAM, null);
		} else {
			return getScheduler(domain, isAllInRAM, coreSS);
		}
	}

	/**
	 * create or open exists project/session level job scheduler. <BR>
	 * <BR>
	 * for session level scheduler, same <code>domain</code> in difference
	 * session means difference scheduler.<BR>
	 * session level scheduler is all in RAM, even though pass
	 * <code>isAllInRAM</code> with false. <BR>
	 * <BR>
	 * <STRONG>Tip :</STRONG><BR>
	 * session level scheduler will be shutdown by server after session is
	 * logout/lineoff.<BR>
	 * project level scheduler will be shutdown by server after shutdown
	 * project.<BR>
	 * 
	 * @param domain
	 * @param isAllInRAM
	 *            true means all in RAM, false means stored in database.
	 * @param isSessionScheduler
	 *            true means create or get a session level scheduler. <BR>
	 * 			if current thread is project level, and
	 *            <code>isSessionScheduler</code> is true, then return null.
	 *            <BR>
	 * 			if current thread is session level, and
	 *            <code>isSessionScheduler</code> is false, then return a
	 *            project level.
	 * @return null means current thread is project level, but
	 *         <code>isSessionScheduler</code> is true.
	 * @see #getScheduler(String)
	 */
	public final Scheduler getScheduler(final String domain, final boolean isAllInRAM,
			final boolean isSessionScheduler) {
		if (isSessionScheduler) {
			if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
				return null;
			}

			final SessionContext sessionContext = __projResponserMaybeNull
					.getSessionContextFromCurrThread();
			J2SESession coreSS;
			if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
				return null;
			} else {
				return getScheduler(domain, isAllInRAM, coreSS);
			}
		} else {
			return getScheduler(domain, isAllInRAM, null);
		}
	}

	/**
	 * 
	 * @param domain
	 * @param isAllInRAM
	 * @param j2seSession
	 *            null means for project level
	 * @return
	 */
	private final Scheduler getScheduler(String domain, boolean isAllInRAM,
			final J2SESession j2seSession) {
		synchronized (lockForScheduler) {
			if (schedulerMapForProjAndSess == null) {
				schedulerMapForProjAndSess = new Hashtable<String, Scheduler>(4);
			}
			if (j2seSession != null) {
				if (isAllInRAM == false) {
					isAllInRAM = true;
					LogManager.warn("all in RAM is required for session level scheduler [" + domain
							+ "], force set isAllInRAM to TRUE.");
				}

				domain += "_for_session_" + j2seSession.getSessionID();
			}
			Scheduler cronManager = schedulerMapForProjAndSess.get(domain);
			if (cronManager == null) {
				cronManager = new Scheduler(this, domain, isAllInRAM, j2seSession);
				schedulerMapForProjAndSess.put(domain, cronManager);
				if (j2seSession == null) {
					LogManager.log("create/open scheduler [" + domain + "] (project level).");
				} else {
					LogManager.log("create/open scheduler [" + domain + "] (session level).");
				}
			}
			return cronManager;
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	ProjResponser __projResponserMaybeNull;

	private final Hashtable<String, Object> att_map = new Hashtable<String, Object>();
	private final ProjClassLoaderFinder finder;

	private static final Thread eventDispatchThread = HCLimitSecurityManager
			.getEventDispatchThread();
	private static final HCEventQueue hcEventQueue = HCLimitSecurityManager.getHCEventQueue();

	/**
	 * @deprecated
	 */
	@Deprecated
	ProjectContext(final String id, final String ver, final RecycleRes recycleRes,
			final ProjResponser projResponser, final ProjClassLoaderFinder finder,
			final String dbPassword) {// 为了不在代码提示中出来，请使用ServerUIUtil.buildProjectContext
		projectID = id;
		projectVer = ver;
		isLoggerOn = ServerUIAPIAgent.isLoggerOn();
		projDesignConfig = new ProjDesignConf(projResponser);
		this.recycleRes = recycleRes;
		this.__projResponserMaybeNull = projResponser;// 由于应用复杂，可能为null
		this.finder = finder;
		proj_prop_map = new PropertiesMap(PropertiesManager.p_PROJ_RECORD + getProjectID());
		ppm = new ProjectPropertiesManager(proj_prop_map, this, projectID);
		if (dbPassword == ServerUIAPIAgent.CRATE_DB_PASSWORD) {
			String dbPwd = ServerUIAPIAgent.getHCSysProperties(this,
					ServerUIAPIAgent.PROJ_DB_PASSWORD);
			if (dbPwd == null) {
				dbPwd = ResourceUtil.buildUUID();
				ServerUIAPIAgent.setHCSysProperties(this, ServerUIAPIAgent.PROJ_DB_PASSWORD, dbPwd);
				__saveSysPropertiesOnHC();// 注意，一定要用这个，不能用PropertiesManager.save
			}
			this.dbPassword = dbPwd;
		} else {
			this.dbPassword = dbPassword;
		}
	}

	/**
	 * register/substitute a assistant, when a {@link VoiceCommand} is coming,
	 * it will be dispatched to {@link Assistant#onVoice(VoiceCommand)}. <BR>
	 * <BR>
	 * it is not required to register assistant for project, if a voice command
	 * is too complex to process, maybe server will do it for you, but it will
	 * take a long time for the machine to learn.
	 * 
	 * @param assistant
	 *            null means deregister a assistant.
	 * @since 7.47
	 */
	public final void registerAssistant(final Assistant assistant) {
		if (this.assistant != null) {
			LogManager.warning("override assistant in project [" + projectID + "].");
		}else{
			LogManager.log("register assistant in project [" + projectID + "].");
		}
		this.assistant = assistant;
	}

	/**
	 * the version of HomeCenter server.
	 * 
	 * @return the version of HomeCenter server.
	 * @since 7.0
	 */
	public final String getHomeCenterVersion() {
		return StarterManager.getHCVersion();
	}

	/**
	 * return the version of JRE of standard JVM of Oracle or implementation of
	 * AWT/Swing for Android server.
	 * 
	 * @return
	 * @since 7.5
	 */
	public final String getJREVersion() {
		return String.valueOf(App.getJREVer());
	}

	/**
	 * log an error message to log system or console.
	 * 
	 * @param logMessage
	 * @see #err(String)
	 * @since 7.0
	 */
	public final void error(final String logMessage) {
		LogManager.errToLog(logMessage);
	}

	/**
	 * 获得工程级或会话级MobiMenu
	 * 
	 * @return
	 */
	private final MobiMenu getMobiMenu() {
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMobiMenu");
			}
			return __projResponserMaybeNull.jarMainMenu.projectMenu;
		} else {
			return coreSS.getMenu(projectID);
		}
	}

	/**
	 * go/run target URL by <code>locator</code>. <BR>
	 * <BR>
	 * <STRONG>Warning</STRONG> : in project level it will do nothing.
	 * 
	 * @param scheme
	 *            one of {@link MenuItem#CMD_SCHEME},
	 *            {@link MenuItem#CONTROLLER_SCHEME},
	 *            {@link MenuItem#FORM_SCHEME} or
	 *            {@link MenuItem#SCREEN_SCHEME}.
	 * @param locator
	 *            for example, run scripts of menu item "cmd://myCommand", the
	 *            scheme is {@linkplain MenuItem#CMD_SCHEME}, and locator is
	 *            "myCommand",
	 * @see #goWhenInSession(String)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.30
	 */
	public final void goWhenInSession(final String scheme, final String locator) {
		final String target_url = HCURL.buildStandardURL(scheme, locator);
		goWhenInSession(target_url);
	}

	/**
	 * Jump mobile to following targets:<BR>
	 * 1. <i>{@link Mlet#URL_SCREEN}</i> : enter the desktop screen of server
	 * from mobile, <BR>
	 * 2. <i>form://myMlet</i> : open and show a form, <BR>
	 * 3. <i>controller://myctrl</i> : open and show a controller, <BR>
	 * 4. <i>cmd://myCmd</i> : run script commands only, <BR>
	 * <BR>
	 * Bring to top : <BR>
	 * 1. jump to <i>form://B</i> from <i>form://A</i>, <BR>
	 * 2. ready jump to <i>form://A</i> again from <i>form://B</i>.<BR>
	 * 3. system will bring the target (form://A) to top if it is opened. <BR>
	 * <BR>
	 * <STRONG>Warning</STRONG> : <BR>
	 * in project level it will do nothing. <BR>
	 * <BR>
	 * <STRONG>Note</STRONG> :<BR>
	 * go to external URL (for example, http://homecenter.mobi), invoke
	 * {@link #goExternalURLWhenInSession(String)}.
	 * 
	 * @param url
	 * @see #isCurrentThreadInSessionLevel()
	 * @see #goWhenInSession(String, String)
	 * @see #goMletWhenInSession(Mlet, String)
	 * @since 7.30
	 */
	public final void goWhenInSession(final String url) {
		if (Mlet.URL_EXIT.equals(url)) {
			LogManager.log("do nothing for \"" + Mlet.URL_EXIT + "\" in goWhenInSession.");
			// return false;
			return;
		}

		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			// return false;
			return;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			errWhenInSession("goWhenInSession");
			// return false;
			return;
		} else {
			ServerUIAPIAgent.goInServer(coreSS, this, url);
			// return true;
			return;
		}
	}

	/**
	 * <STRONG>Warning</STRONG> : when in project level it will do nothing. <BR>
	 * <BR>
	 * open/go to external URL in client application. <BR>
	 * <BR>
	 * <STRONG>Important : </STRONG> <BR>
	 * socket/connect permissions is required even if the domain of external URL
	 * is the same with the domain of upgrade HAR project URL. <BR>
	 * <BR>
	 * More about back :<BR>
	 * 1. there is always a float button for back. 2.
	 * <code>onclick='javascript:window.hcserver.back();'</code><BR>
	 * 3. <code>history.back = function(){window.hcserver.back();};</code><BR>
	 * <BR>
	 * <BR>
	 * <STRONG>Warning : </STRONG> <BR>
	 * 1. the external URL may be sniffed when in moving (exclude HTTPS). <BR>
	 * 2. iOS 9 and above must use secure URLs.
	 * 
	 * @param url
	 *            for example : http://homecenter.mobi
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.30
	 */
	public final void goExternalURLWhenInSession(final String url) {
		// 注意:ios的实现，请同步webView evaluateJavaScript back
		goExternalURLWhenInSession(url, false);
	}

	/**
	 * <STRONG>Warning</STRONG> : when in project level it will do nothing. <BR>
	 * <BR>
	 * <STRONG>deprecated</STRONG>, replaced by
	 * {@link #goExternalURLWhenInSession(String)}. <BR>
	 * <BR>
	 * go to external URL in system web browser or client application. <BR>
	 * <BR>
	 * <STRONG>Important : </STRONG> <BR>
	 * socket/connect permissions is required even if the domain of external URL
	 * is the same with the domain of upgrade HAR project URL. <BR>
	 * <BR>
	 * <STRONG>Warning : </STRONG> <BR>
	 * 1. the external URL may be sniffed when in moving (exclude HTTPS). <BR>
	 * 2. iOS 9 and above must use secure URLs. <BR>
	 * 3. In iOS (not Android), when go external URL and
	 * <code>isUseExtBrowser</code> is true, the application will be turn into
	 * background and released after seconds. In future, it maybe keep alive in
	 * background.
	 * 
	 * @param url
	 * @param isUseExtBrowser
	 *            true : use system web browser to open URL; false : the URL
	 *            will be opened in client application and still keep
	 *            foreground.
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.30
	 */
	@Deprecated
	public final void goExternalURLWhenInSession(final String url, final boolean isUseExtBrowser) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			// return false;
			return;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			errWhenInSession("goExternalURLWhenInSession");
			// return false;
			return;
		} else {
			ServerUIAPIAgent.goExternalURL(coreSS, this, url, isUseExtBrowser);
			// return true;
			return;
		}
	}

	/**
	 * <STRONG>Warning</STRONG> : when in project level it will do nothing. <BR>
	 * <BR>
	 * go and open a <code>Mlet</code> or <code>HTMLMlet</code> (which is
	 * probably created by {@link ProjectContext#eval(String)}). <BR>
	 * <BR>
	 * the target of <i>toMlet</i> will be set as <i>targetOfMlet</i>.<BR>
	 * <BR>
	 * <STRONG>Important : </STRONG> <BR>
	 * if the same name <i>target</i> or <i>form://target</i> is opened, then it
	 * will be brought to top. <BR>
	 * for more, see {@link #goWhenInSession(String, String)}.
	 * 
	 * @param toMlet
	 * @param targetOfMlet
	 *            target of <code>Mlet</code>. The prefix <i>form://</i> is
	 *            <STRONG>NOT</STRONG> required.
	 * @see ProjectContext#eval(String)
	 * @see #isCurrentThreadInSessionLevel()
	 * @see #goWhenInSession(String, String)
	 * @since 7.30
	 */
	public final void goMletWhenInSession(final Mlet toMlet, final String targetOfMlet) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			// return false;
			return;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			errWhenInSession("goMletWhenInSession");
			// return false;
			return;
		} else {
			final Mlet fromMlet = null;
			ServerUIAPIAgent.goMlet(coreSS, this, fromMlet, toMlet, targetOfMlet, false);
			// return true;
			return;
		}
	}

	private final void errWhenInSession(final String method) {
		LogManager.errToLog("[" + method + "] in project [" + projectID
				+ "] must be invoked in session level.");
	}

	/**
	 * add a menu item for project or session at run-time.<BR>
	 * <BR>
	 * if in project level, add item to list of project level.<BR>
	 * if in session level, add item to list of session level. <BR>
	 * <BR>
	 * a menu presented on the mobile client is composed of menu items of
	 * project level (priority) and session level. <BR>
	 * the menu items of session level will gone after line-off / logout. <BR>
	 * <BR>
	 * <STRONG>Important :</STRONG><BR>
	 * the scripts of menu item works in session level, no matter which is added
	 * to list of project level or session level. <BR>
	 * <BR>
	 * it is thread safe.
	 * 
	 * @param item
	 *            the <code>MenuItem</code> to add
	 * @see #insertMenuItem(MenuItem)
	 * @see #addMenuItemToProjectLevel(MenuItem, int)
	 * @see #addMenuItemToSessionLevel(MenuItem, int)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final void addMenuItem(final MenuItem item) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		getMobiMenu().addModifiableItem(item);
	}

	/**
	 * it is equals with {@link #addMenuItem(MenuItem)}.
	 * 
	 * @param item
	 * @see #insertMenuItemToProjectLevel(MenuItem, int)
	 * @see #insertMenuItemToSessionLevel(MenuItem, int)
	 * @since 7.20
	 */
	public final void insertMenuItem(final MenuItem item) {
		addMenuItem(item);
	}

	/**
	 * inserts the menu item at a given <code>position</code> into project list.
	 * <BR>
	 * <BR>
	 * <STRONG>Important :</STRONG><BR>
	 * the scripts of menu item works in session level, no matter which is added
	 * to list of project level or session level. <BR>
	 * <BR>
	 * it is thread safe.
	 * 
	 * @param item
	 *            the <code>item</code> to add
	 * @param position
	 *            the <code>position</code> at which to add the new
	 *            <code>item</code>
	 * @return true means insert successfully, false means index out of bounds.
	 * @see #addMenuItem(MenuItem)
	 * @see #getMenuItemFromProjectLevel(int)
	 * @see #getMenuItemBy(String, String)
	 * @see #getMenuItemsSizeOfProjectLevel()
	 * @since 7.20
	 */
	public final boolean insertMenuItemToProjectLevel(final MenuItem item, final int position) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_SUCC_INSERT;
		}

		return __projResponserMaybeNull.jarMainMenu.projectMenu.insertModifiableItem(item,
				position);
	}

	/**
	 * it is equals with {@link #insertMenuItemToProjectLevel(MenuItem, int)}.
	 * 
	 * @param item
	 * @param position
	 * @return
	 * @since 7.20
	 */
	public final boolean addMenuItemToProjectLevel(final MenuItem item, final int position) {
		return insertMenuItemToProjectLevel(item, position);
	}

	/**
	 * it is equals with {@link #insertMenuItemToSessionLevel(MenuItem, int)}.
	 * 
	 * @param item
	 * @param position
	 * @return
	 * @since 7.20
	 */
	public final boolean addMenuItemToSessionLevel(final MenuItem item, final int position) {
		return insertMenuItemToSessionLevel(item, position);
	}

	/**
	 * inserts the menu item at a given <code>position</code> into session list.
	 * <BR>
	 * <BR>
	 * it is thread safe.
	 * 
	 * @param item
	 *            the <code>item</code> to add
	 * @param position
	 *            the <code>position</code> at which to add the new
	 *            <code>item</code>
	 * @return true means insert successfully, false means index out of bounds
	 *         or current thread is in project level.
	 * @see #addMenuItem(MenuItem)
	 * @see #getMenuItemFromSessionLevel(int)
	 * @see #getMenuItemBy(String, String)
	 * @see #getMenuItemsSizeOfSessionLevel()
	 * @since 7.20
	 */
	public final boolean insertMenuItemToSessionLevel(final MenuItem item, final int position) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_SUCC_INSERT;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("insertMenuItemToSessionLevel");
			}

			ServerUIAPIAgent.printInProjectLevelWarn("insertMenuItemToSessionLevel");
			return false;
		} else {
			return coreSS.getMenu(projectID).insertModifiableItem(item, position);
		}
	}

	/**
	 * removes the specified menu item. <BR>
	 * the <code>item</code> may be in project level or session level. <BR>
	 * <BR>
	 * it is thread safe.
	 * 
	 * @param item
	 *            the <code>item</code> to be removed
	 * @return true if contained the specified item
	 * @see #getMenuItemBy(String, String)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final boolean removeMenuItem(final MenuItem item) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_SUCC_REMOVED;
		}

		return getMobiMenu().removeModifiableItem(item);
	}

	/**
	 * return <code>position</code> item from list of project level. <BR>
	 * <BR>
	 * it is thread safe. <BR>
	 * <BR>
	 * the menu items added possibly by system (for example, add QR) are
	 * excluded.
	 * 
	 * @param position
	 *            the position of the item to be retrieved.
	 * @return menu item at the specified position, or null if index out of
	 *         bounds.
	 * @see #getMenuItemBy(String, String)
	 * @see #getMenuItemFromSessionLevel(int)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final MenuItem getMenuItemFromProjectLevel(final int position) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_MENU_ITEM;
		}

		return __projResponserMaybeNull.jarMainMenu.projectMenu.getModifiableItemAt(position);
	}

	/**
	 * return <code>position</code> item from list of session level. <BR>
	 * <BR>
	 * it is thread safe. <BR>
	 * <BR>
	 * the menu items added possibly by system (for example, add QR) are
	 * excluded.
	 * 
	 * @param position
	 *            the position of the item to be retrieved.
	 * @return menu item at the specified position, or null if index out of
	 *         bounds or current thread is in project level.
	 * @see #getMenuItemBy(String, String)
	 * @see #getMenuItemFromProjectLevel(int)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final MenuItem getMenuItemFromSessionLevel(final int position) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_MENU_ITEM;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			ServerUIAPIAgent.printInProjectLevelWarn("getMenuItemFromSessionLevel");
			return null;
		} else {
			return coreSS.getMenu(projectID).getModifiableItemAt(position);
		}
	}

	/**
	 * remove <code>position</code> from list of project level. <BR>
	 * <BR>
	 * it is thread safe. <BR>
	 * <BR>
	 * the menu items added possibly by system (for example, add QR) are
	 * excluded.
	 * 
	 * @param position
	 *            the position of the item to be removed.
	 * @return element that was removed, or null if index out of bounds.
	 * @see #getMenuItemsSizeOfProjectLevel()
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final MenuItem removeMenuItemFromProjectLevel(final int position) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_MENU_ITEM;
		}

		return __projResponserMaybeNull.jarMainMenu.projectMenu.removeModifiableItemAt(position);
	}

	/**
	 * remove <code>position</code> from list of session level. <BR>
	 * <BR>
	 * it is thread safe. <BR>
	 * <BR>
	 * the menu items added possibly by system (for example, add QR) are
	 * excluded.
	 * 
	 * @param position
	 *            the position of the item to be removed.
	 * @return element that was removed, or null if index out of bounds or
	 *         current thread is in project level.
	 * @see #getMenuItemsSizeOfSessionLevel()
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final MenuItem removeMenuItemFromSessionLevel(final int position) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_MENU_ITEM;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			ServerUIAPIAgent.printInProjectLevelWarn("removeMenuItemFromSessionLevel");
			return null;
			// return __projResponserMaybeNull.jarMainMenu.projectMenu;
		} else {
			return coreSS.getMenu(projectID).removeModifiableItemAt(position);
		}
	}

	// /**
	// * if in project level, return the list size of project level.<BR>
	// * if in session level, return total list size of project level and
	// session level.
	// * <BR><BR>
	// * to get the size of project level when current thread is in session
	// level, please invoke {@link #getMenuItemsSizeOfProjectLevel()}.
	// * <BR><BR>
	// * the menu items added possibly by system (for example, add QR) are
	// excluded.
	// * @return
	// * @see #getMenuItemsSizeOfProjectLevel()
	// * @see #getMenuItemsSizeOfSessionLevel()
	// * @see #isCurrentThreadInSessionLevel()
	// * @since 7.20
	// */
	// public final int getMenuItemsSize(){
	// if(__projResponserMaybeNull == null ||
	// SimuMobile.checkSimuProjectContext(this)){
	// return SimuMobile.MOBILE_MENU_ITEMS_SIZE;
	// }
	//
	// return getMobiMenu().getModifiableItemsCount();
	// }

	/**
	 * for example, to get menu item "cmd://myCommand", <BR>
	 * the scheme is "{@link MenuItem#CMD_SCHEME}", and element ID is
	 * "myCommand", <BR>
	 * <BR>
	 * if in project level, search item from list of project level.<BR>
	 * if in session level, search item from total list of project level and
	 * session level. <BR>
	 * <BR>
	 * it is thread safe.
	 * 
	 * @param scheme
	 *            one of {@link MenuItem#CMD_SCHEME},
	 *            {@link MenuItem#CONTROLLER_SCHEME},
	 *            {@link MenuItem#FORM_SCHEME}, {@link MenuItem#SCREEN_SCHEME}.
	 * @param elementID
	 * @return null means not found.
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final MenuItem getMenuItemBy(final String scheme, final String elementID) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_MENU_ITEM;
		}

		String urlLowercase = HCURL.buildStandardURL(scheme, elementID).toLowerCase();
		final MobiMenu menu = getMobiMenu();

		final MenuItem out = menu.getModifiableItemBy(urlLowercase);
		if (out != null) {
			return out;
		}

		if (urlLowercase.startsWith(HCURL.FORM_MLET_PREFIX)) {
			urlLowercase = HCURL.buildMletAliasURL(urlLowercase);
			return menu.getModifiableItemBy(urlLowercase);
		}

		return null;
	}

	/**
	 * return the list size of project level. <BR>
	 * <BR>
	 * it is thread safe. <BR>
	 * <BR>
	 * the menu items added possibly by system (for example, add QR) are
	 * excluded.
	 * 
	 * @return
	 * @see #getMenuItemsSizeOfSessionLevel()
	 * @see #insertMenuItemToProjectLevel(MenuItem, int)
	 * @see #insertMenuItemToSessionLevel(MenuItem, int)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final int getMenuItemsSizeOfProjectLevel() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_MENU_ITEMS_SIZE;
		}

		return __projResponserMaybeNull.jarMainMenu.projectMenu.getModifiableItemsCount();
	}

	/**
	 * return the list size of session level or -1 in project level. <BR>
	 * <BR>
	 * it is thread safe. <BR>
	 * <BR>
	 * the menu items added by system possibly are excluded.
	 * 
	 * @return -1 means if current thread in project level, or the size of
	 *         session level.
	 * @see #getMenuItemsSizeOfProjectLevel()
	 * @see #insertMenuItemToProjectLevel(MenuItem, int)
	 * @see #insertMenuItemToSessionLevel(MenuItem, int)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final int getMenuItemsSizeOfSessionLevel() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_MENU_ITEMS_SIZE;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMenuItemsSizeOfSessionLevel");
			}
			return -1;
		} else {
			return coreSS.getMenu(projectID).getSessionItemsCount();
		}
	}

	/**
	 * show a dialog for input on this server, <STRONG>NOT</STRONG> on client.
	 * <BR>
	 * if user is installing (scan QR code) current project from client, then
	 * the dialog is shown on client. <BR>
	 * <BR>
	 * you can also send custom {@link Dialog} when is installing HAR from
	 * client, see {@link #isInstallingFromClient()}. <BR>
	 * <BR>
	 * this method is mainly used in the following scenarios :<BR>
	 * for example, in order to connect device, user may be required to input
	 * token of devices.
	 * 
	 * @param title
	 *            the title of input dialog
	 * @param fieldNames
	 *            the name of each field.
	 * @param fieldDescs
	 *            the description of the each field. The HTML tags, such as
	 *            &lt;STRONG&gt; and &lt;BR&gt;, are supported.
	 * @return the input values of each fields.
	 * @since 7.0
	 * @see #sendDialogWhenInSession(Dialog)
	 */
	public static final String[] showInputDialog(final String title, final String[] fieldNames,
			final String[] fieldDescs) {
		checkHARInput(fieldNames, fieldDescs);

		final ProjectContext ctx = ProjectContext.getProjectContext();
		if (ctx != null && ctx.isInstallingFromClient()) {// switch input from
															// client , not from
															// PC desktop
			final String[] waitLock = new String[fieldNames.length];

			ctx.sendDialogByBuilding(new Runnable() {
				@Override
				public void run() {
					new ProjectInputDialog(title, fieldNames, fieldDescs, waitLock);
				}
			});

			synchronized (waitLock) {
				try {
					waitLock.wait(1000 * 60 * 30);// 半小时
				} catch (final InterruptedException e) {
				}
			}

			return waitLock;
		} else {
			return App.showHARInputDialog(appendProjectIDForTitle(title), fieldNames, fieldDescs);
		}
	}

	private static void checkHARInput(final String[] fieldName, final String[] fieldDesc)
			throws Error {
		if (fieldName == null || searchNull(fieldName)) {
			throw new Error("fieldName is null or null member");
		}
		if (fieldDesc == null || searchNull(fieldDesc)) {
			throw new Error("fieldDesc is null or null member");
		}
		if (fieldDesc.length != fieldDesc.length) {
			throw new Error("the length of fieldName must equals to the length of fieldDesc");
		}
	}

	private static final boolean searchNull(final String[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == null) {
				return true;
			}
		}
		return false;
	}

	private static final String appendProjectIDForTitle(final String title) {
		String titleModel = ResourceUtil.get(9211);

		// {proj}] {msg}
		final String projID = getProjectContext().getProjectID();
		titleModel = StringUtil.replace(titleModel, "{proj}", projID);
		titleModel = StringUtil.replace(titleModel, "{title}", title);

		return titleModel;
	}

	/**
	 * pop up an information message dialog on this server, <STRONG>NOT</STRONG>
	 * on client. <BR>
	 * <BR>
	 * if the <code>message</code> is displaying and not closed, then it will
	 * NOT be showed twice at same time. <br>
	 * <br>
	 * <STRONG>Important : </STRONG><BR>
	 * the current thread will NOT be blocked.
	 * 
	 * @param message
	 *            the body of message.
	 * @since 7.0
	 */
	public static final void showMessageDialog(final String message) {
		showMessageDialog(message, ResourceUtil.get(9210), JOptionPane.INFORMATION_MESSAGE, null);
	}

	/**
	 * pop up a dialog to display a message on this server, <STRONG>NOT</STRONG>
	 * on client. <BR>
	 * <BR>
	 * if the <code>message</code> is displaying and not closed, then it will
	 * NOT be showed twice at same time. <br>
	 * <br>
	 * <STRONG>Important : </STRONG><BR>
	 * the current thread will NOT be blocked.
	 * 
	 * @param message
	 *            the body of message.
	 * @param title
	 *            the title of message.
	 * @param messageType
	 *            one of the following :<BR>
	 *            <code>{@link JOptionPane#ERROR_MESSAGE}</code>,<BR>
	 *            <code>{@link JOptionPane#INFORMATION_MESSAGE}</code>,<BR>
	 *            <code>{@link JOptionPane#WARNING_MESSAGE}</code>,<BR>
	 *            <code>{@link JOptionPane#QUESTION_MESSAGE}</code>,<BR>
	 *            <code>{@link JOptionPane#PLAIN_MESSAGE}</code>
	 * @since 7.0
	 */
	public static final void showMessageDialog(final String message, final String title,
			final int messageType) {
		showMessageDialog(message, title, messageType, null);
	}

	/**
	 * pop up a dialog to display a message on this server, <STRONG>NOT</STRONG>
	 * on client. <BR>
	 * <BR>
	 * if the <code>message</code> is displaying and not closed, then it will
	 * NOT be showed twice at same time. <br>
	 * <br>
	 * <STRONG>Important : </STRONG><BR>
	 * the current thread will NOT be blocked.
	 * 
	 * @param message
	 *            the body of message.
	 * @param title
	 *            the title of message.
	 * @param messageType
	 *            one of the following :<BR>
	 *            <code>{@link JOptionPane#ERROR_MESSAGE}</code>,<BR>
	 *            <code>{@link JOptionPane#INFORMATION_MESSAGE}</code>,<BR>
	 *            <code>{@link JOptionPane#WARNING_MESSAGE}</code>,<BR>
	 *            <code>{@link JOptionPane#QUESTION_MESSAGE}</code>,<BR>
	 *            <code>{@link JOptionPane#PLAIN_MESSAGE}</code>
	 * @param icon
	 *            the icon of message. null for no icon.
	 * @since 7.0
	 */
	public final static void showMessageDialog(final String message, final String title,
			final int messageType, final Icon icon) {
		App.showHARMessageDialog(message, appendProjectIDForTitle(title), messageType, icon);
	}

	/**
	 * get the wrapper of robot by name.<BR>
	 * <BR>
	 * <STRONG>Important</STRONG> : <BR>
	 * it is a wrapper of robot, not the instance created by your scripts.<BR>
	 * it means that your methods not extend from Robot will NOT be accessed
	 * from here.<BR>
	 * because robot may be driven by intelligent assistant, for example voice
	 * command.
	 * 
	 * @param name
	 *            the name of robot.
	 * @return null means the robot is not found.
	 * @since 7.0
	 */
	public final Robot getRobot(final String name) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.buildSimuRobot();
		}

		return __projResponserMaybeNull.getRobotWrapper(name);
	}

	/**
	 * run a shell of JRuby script, and return a object if with
	 * <code>return</code> command. <BR>
	 * <BR>
	 * for example, a scripts with four lines, (<STRONG>'\n'</STRONG> is
	 * required for each line at end) : <BR>
	 * <BR>
	 * <i> <code>t = 100<BR> i = 2 + t<BR>puts i<BR> return i # 102<BR></code>
	 * </i> <BR>
	 * it is recommended that set variable in
	 * {@link #setAttribute(String, Object)} for project level and
	 * {@link ClientSession#setAttribute(String, Object)} for session level
	 * rather than set global variable (begin with $) in JRuby scripts. <BR>
	 * <BR>
	 * the above script is safe even if multiple threads calling, because they
	 * are all local variable (TRANSIENT). <BR>
	 * <BR>
	 * <STRONG>CAUTION : </STRONG><BR>
	 * variable (if it is NOT local variable) set by one thread will be visible
	 * to another thread.
	 * 
	 * <pre>
	 * import Java::hc.server.util.JavaLangSystemAgent
	 * 
	 * {@literal @}callMS = JavaLangSystemAgent.currentTimeMillis().to_s()
	 * sleep(10)
	 * puts "callMS : " + {@literal @}callMS
	 * </pre>
	 * 
	 * if thread B call it after thread A one second, then print out the same
	 * value.
	 * 
	 * @param shell
	 *            the evaluate shell scripts.
	 * @return a object if with <code>return</code> command.
	 * @since 7.0
	 */
	public final Object eval(final String shell) {
		if (shell == null) {
			return null;
		}

		final HCJRubyEngine engine = ((__projResponserMaybeNull == null
				|| SimuMobile.checkSimuProjectContext(this)) ? SimuMobile.getRunTestEngine()
						: __projResponserMaybeNull.hcje);

		final CallContext callCtx = CallContext.getFree();
		try {
			return RubyExector.runAndWaitOnEngine(callCtx, new StringValue(shell),
					"evalInProjectContext", null, engine);
		} catch (final Throwable e) {
			// if(ExceptionReporter.isCauseByLineOffSession(e)){
			throw new Error(ExceptionReporter.THROW_FROM, e);
			// }
		} finally {
			CallContext.cycle(callCtx);
		}
	}

	/**
	 * this method is useful when current thread is in session level, <BR>
	 * and the <code>runnable</code> should be executed in project level. <BR>
	 * <BR>
	 * Note : <BR>
	 * 1. please put long time task and network operation in here,<BR>
	 * 2. it is a good choice to put task in {@link #getScheduler(String)},
	 * because it will be shutdown by server and release all resources.<BR>
	 * 3. there is NOT API for task to be executed in session level when in
	 * project level thread. Maybe you need
	 * {@link #sendDialogByBuilding(Runnable)}.
	 * 
	 * @param runnable
	 * @see #run(Runnable)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final void runInProjectLevel(final Runnable runnable) {
		recycleRes.threadPool.run(runnable);
	}

	/**
	 * the <code>runnable</code> will be executed in project level, no matter
	 * current thread is in session or project level.
	 * 
	 * @param runnable
	 * @see #runAndWait(Runnable)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.20
	 */
	public final void runAndWaitInProjectLevel(final Runnable runnable) {
		final ReturnableRunnable run = new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				runnable.run();
				return null;
			}
		};

		recycleRes.threadPool.runAndWait(run);
	}

	/**
	 * start a {@link Runnable} task asynchronous in project thread pool or
	 * session thread pool depends on current thread is in project or session
	 * level. <br>
	 * <br>
	 * in session level, the runnable task should be finished after
	 * {@link #EVENT_SYS_MOBILE_LOGOUT}. <BR>
	 * in project level, the runnable task should be finished after
	 * {@link #EVENT_SYS_PROJ_SHUTDOWN}. <BR>
	 * <BR>
	 * Note : <BR>
	 * 1. please put long time task and network operation in here.<BR>
	 * 2. it is a good choice to put task in {@link #getScheduler(String)},
	 * because it will be shutdown by server and release all resources.
	 * 
	 * @param runnable
	 * @see #runAndWait(Runnable)
	 * @see #runInProjectLevel(Runnable)
	 * @see #addSystemEventListener(SystemEventListener)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final void run(final Runnable runnable) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		final J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("run");
			}
			recycleRes.threadPool.run(runnable);
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			sessionContext.recycleRes.threadPool.run(runnable);
		}
	}

	/**
	 * start a {@link Runnable} task and wait for done in project thread pool or
	 * session thread pool depends on current thread is in project or session
	 * level. <br>
	 * <br>
	 * in session level, the runnable task should be finished after
	 * {@link #EVENT_SYS_MOBILE_LOGOUT}. <BR>
	 * in project level, the runnable task should be finished after
	 * {@link #EVENT_SYS_PROJ_SHUTDOWN}.
	 * 
	 * @param runnable
	 * @see #run(Runnable)
	 * @see #runAndWaitInProjectLevel(Runnable)
	 * @see #addSystemEventListener(SystemEventListener)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final void runAndWait(final Runnable runnable) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final ReturnableRunnable run = new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				runnable.run();
				return null;
			}
		};

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("runAndWait");
			}
			recycleRes.threadPool.runAndWait(run);
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);

			sessionContext.recycleRes.threadPool.runAndWait(run);
		}
	}

	/**
	 * print all thread stack to log system.
	 * 
	 * @since 7.0
	 */
	public final static void printAllThreadStack() {
		App.printThreadStackForProjectContext(null);
	}

	/**
	 * return the version of current project.
	 * 
	 * @return
	 * @since 7.0
	 */
	public final String getProjectVersion() {
		return projectVer;
	}

	private final J2SESession[] toArray(final J2SESession coreSS) {
		final J2SESession[] out = { coreSS };
		return out;
	}

	/**
	 * send a question to mobile if in session level, or send same question to
	 * all client sessions if in project level. <br>
	 * in project level, if one session replies, then the same question in other
	 * sessions will be dismissed. <br>
	 * <br>
	 * if there is an alert message, other question or a <code>Dialog</code> is
	 * presented on client and NOT be closed, the question will be delayed. <br>
	 * <br>
	 * this method is <STRONG>asynchronous</STRONG>. system will NOT wait for
	 * the result of question to the caller. <BR>
	 * for <STRONG>synchronous</STRONG>, please use
	 * {@link #sendQuestionAndWait(String, String, BufferedImage, Runnable, Runnable, Runnable)}.
	 * <BR>
	 * <BR>
	 * <STRONG>Important</STRONG> : the <code>yesRunnable</code>,
	 * <code>noRunnable</code>, <code>cancelRunnable</code> will be executed in
	 * session level, no matter current thread is project or session level. <BR>
	 * <BR>
	 * Note : if mobile is in background ({@link #isMobileInBackground()}), a
	 * notification is also created for mobile.<BR>
	 * <BR>
	 * if mobile option [Message, Notification to Speech also] is
	 * <STRONG>on</STRONG>, it may be spoken.<BR>
	 * the speech or not is depends on text, TTS engine, locale and mute of
	 * mobile.
	 * 
	 * @param caption
	 *            the caption of the question.
	 * @param text
	 *            the content of the question.
	 * @param image
	 *            a image to describe the question. Null for no image.
	 * @param yesRunnable
	 *            the runnable will be executed if choosing 'YES'. Null for
	 *            doing nothing.
	 * @param noRunnable
	 *            the runnable will be executed if choosing 'NO'. Null for doing
	 *            nothing.
	 * @param cancelRunnable
	 *            the runnable will be executed if choosing 'CANCEL'. It will
	 *            not be executed when line off. Set null to not display cancel
	 *            button in question, but it is still cancelable. To set
	 *            question to be not cancelable, use
	 *            {@link #sendQuestionNotCancelable(String, String, BufferedImage, Runnable, Runnable)}.
	 * @since 7.0
	 * @see #sendQuestionAndWait(String, String, BufferedImage, Runnable,
	 *      Runnable, Runnable)
	 * @see #sendQuestionNotCancelable(String, String, BufferedImage, Runnable,
	 *      Runnable)
	 * @see #isCurrentThreadInSessionLevel()
	 * @see #isClientLineOn()
	 */
	public final void sendQuestion(final String caption, final String text,
			final BufferedImage image, final Runnable yesRunnable, final Runnable noRunnable,
			final Runnable cancelRunnable) {
		sendQuestionImpl(false, caption, text, image, yesRunnable, noRunnable, cancelRunnable,
				true);
	}

	/**
	 * for more, see
	 * {@link #sendQuestion(String, String, BufferedImage, Runnable, Runnable, Runnable)}
	 * <BR>
	 * <BR>
	 * <STRONG>About return status :</STRONG><BR>
	 * 1. in project level, return true means question is processed/canceled by
	 * one client, false means all sessions are line off.<BR>
	 * 2. in session level, return true means question is processed by client,
	 * false means canceled by client, when line off the execution will be
	 * terminated.
	 * 
	 * @param caption
	 * @param text
	 * @param image
	 * @param yesRunnable
	 * @param noRunnable
	 * @param cancelRunnable
	 * @see #isClientLineOn()
	 */
	public final boolean sendQuestionAndWait(final String caption, final String text,
			final BufferedImage image, final Runnable yesRunnable, final Runnable noRunnable,
			final Runnable cancelRunnable) {
		return sendQuestionImpl(true, caption, text, image, yesRunnable, noRunnable, cancelRunnable,
				true);
	}

	/**
	 * send a not cancelable question to mobile if in session level, or send
	 * same question to all client sessions if in project level. <BR>
	 * <BR>
	 * a cancelable question can be canceled, for example pressing
	 * <code>back</code> key in Android.
	 * 
	 * @param caption
	 *            the caption of the question.
	 * @param text
	 *            the content of the question.
	 * @param image
	 *            a image to describe the question. Null for no image.
	 * @param yesRunnable
	 *            the runnable will be executed if choosing 'YES'. Null for
	 *            doing nothing.
	 * @param noRunnable
	 *            the runnable will be executed if choosing 'NO'. Null for doing
	 *            nothing.
	 * @see #sendQuestionAndWaitNotCancelable(String, String, BufferedImage,
	 *      Runnable, Runnable)
	 * @see #sendQuestion(String, String, BufferedImage, Runnable, Runnable,
	 *      Runnable)
	 */
	public final void sendQuestionNotCancelable(final String caption, final String text,
			final BufferedImage image, final Runnable yesRunnable, final Runnable noRunnable) {
		sendQuestionImpl(false, caption, text, image, yesRunnable, noRunnable, null, false);
	}

	/**
	 * see
	 * {@link #sendQuestion(String, String, BufferedImage, Runnable, Runnable, Runnable)}
	 * for more. <BR>
	 * <BR>
	 * <STRONG>About return status :</STRONG><BR>
	 * 1. in project level, return true means question is processed by one
	 * client, false means all sessions are line off.<BR>
	 * 2. in session level, return true means question is processed by client,
	 * when line off the execution will be terminated.
	 * 
	 * @param caption
	 * @param text
	 * @param image
	 * @param yesRunnable
	 * @param noRunnable
	 * @return
	 * @see #isClientLineOn()
	 */
	public final boolean sendQuestionAndWaitNotCancelable(final String caption, final String text,
			final BufferedImage image, final Runnable yesRunnable, final Runnable noRunnable) {
		return sendQuestionImpl(true, caption, text, image, yesRunnable, noRunnable, null, false);
	}

	private final boolean sendQuestionImpl(final boolean isWaiting, String caption, String text,
			final BufferedImage image, final Runnable yesRunnable, final Runnable noRunnable,
			final Runnable cancelRunnable, final boolean isCancelable) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return true;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		final J2SESession[] coreSSS;
		J2SESession coreSS = null;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("sendQuestion");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		final boolean isForSession = coreSS != null;

		if (coreSSS == null || coreSSS.length == 0) {
			return false;
		}

		if (caption == null) {
			caption = ResourceUtil.getInfoI18N();
		}
		if (text == null) {
			text = "";
		}

		String imageData = "";
		if (image != null) {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try {
				ImageIO.write(image, "png", byteArrayOutputStream);
				final byte[] out = byteArrayOutputStream.toByteArray();
				imageData = ByteUtil.encodeBase64(out);
				byteArrayOutputStream.close();
			} catch (final IOException e) {
				ExceptionReporter.printStackTrace(e);
				return false;
			}
		}

		final String[] para = { HCURL.DATA_PARA_QUESTION_ID, "isCancelable", "caption", "text",
				"image", "withCancel" };

		final String p_caption = caption;
		final String p_text = text;
		final String p_imageData = imageData;
		final ProjectContext p_ctx = this;

		// 如果同时发出两个Ques，则可能不同步，所以以下要wait
		final QuestionGlobalLock quesLock = (QuestionGlobalLock) ServerUIAPIAgent
				.runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						final QuestionGlobalLock quesLock = new QuestionGlobalLock(isForSession,
								coreSSS, isWaiting);
						final int questionID = ServerUIAPIAgent.buildQuestionID();// 便于撤消

						for (int i = 0; i < coreSSS.length; i++) {
							ServerUIAPIAgent.buildQuestionParameter(coreSSS[i], p_ctx, quesLock,
									questionID, p_text, yesRunnable, noRunnable, cancelRunnable);

							final String[] values = { String.valueOf(questionID),
									IConstant.toString(isCancelable), p_caption, p_text,
									p_imageData, (cancelRunnable != null) ? "1" : "0" };// 注意：必须在外部转换
							HCURLUtil.sendCmd(coreSSS[i], HCURL.DATA_CMD_SendPara, para, values);
						}
						return quesLock;
					}
				});

		if (isWaiting) {
			final boolean processed = quesLock.waitingResult(coreSS);
			if (isForSession) {
				ServerUIUtil.checkLineOnForAPI(coreSS);
			}
			return processed;
		} else {
			return true;
		}
	}

	/**
	 * backup user properties and private files of current project.<BR>
	 * <BR>
	 * usually this method is not used, <BR>
	 * because the server will automatically backup regularly, only when the
	 * following conditions are meet :<BR>
	 * 1. some data saved local, and some stored on cloud, if power off occurs,
	 * the status saved recently will gone, but the cloud not gone.<BR>
	 * 2. if this method is invoked, when power off occurs, server will restore
	 * all at the next startup.<BR>
	 * <BR>
	 * <STRONG>Know More :</STRONG><BR>
	 * 1. DB is not required to close before backup.<BR>
	 * 2. private file which is opening or writing is not required to close
	 * before backup.<BR>
	 * 3. backup will not work on Windows because of exclusive lock.<BR>
	 * 4. backup works well on Linux, macOS, Android.<BR>
	 * 5. return normally means backup successfully.
	 */
	public final void backup() {
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				SafeDataManager.backup(projectID);
				return null;
			}
		});
	}

	/**
	 * return current project ID.
	 * 
	 * @return
	 * @since 7.0
	 */
	public final String getProjectID() {
		return projectID;
	}

	/**
	 * it is equals with {@link #getMobileWidth()}.
	 * 
	 * @return
	 * @since 7.50
	 * @deprecated
	 */
	@Deprecated
	public final int getClientWidth() {
		return getMobileWidth();
	}

	/**
	 * you can't invoke it before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}
	 * or after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}.
	 * 
	 * @return the width pixel of login mobile; 0 means mobile not login or not
	 *         in session level.
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 * @deprecated
	 */
	@Deprecated
	public final int getMobileWidth() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_WIDTH;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMobileWidth");
			}
			return SimuMobile.PROJ_LEVEL_MOBILE_WIDTH;
		}

		return UserThreadResourceUtil.getMletWidthFrom(coreSS);
	}

	/**
	 * set an object to a given attribute name. <BR>
	 * <BR>
	 * to set an attribute for session, please see {@link #getClientSession()}.
	 * <BR>
	 * <BR>
	 * <STRONG>Note : </STRONG>you can't save attribute to persistent system,
	 * but you can save properties of current project to persistent system. <BR>
	 * <BR>
	 * it is thread safe.
	 * 
	 * @see #removeAttribute(String)
	 * @see #clearAttribute(String)
	 * @see #getAttribute(String)
	 * @see #getAttributeNames()
	 * @param name
	 *            the name of attribute.
	 * @param obj
	 *            the value of the attribute.
	 * @return the previous value of the specified name, or null if it did not
	 *         have one
	 * @since 7.0
	 */
	public final Object setAttribute(final String name, final Object obj) {
		return att_map.put(name, obj);
	}

	/**
	 * if name is not exists or not Boolean object, then return false
	 * 
	 * @param name
	 * @return
	 * @see #getBooleanAttribute(String, boolean)
	 */
	public final boolean getBooleanAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null && obj instanceof Boolean) {
			return ((Boolean) obj).booleanValue();
		}
		return false;
	}

	/**
	 * if name is not exists or not Boolean object, then return defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getBooleanAttribute(String)
	 */
	public final boolean getBooleanAttribute(final String name, final boolean defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Boolean) {
				return ((Boolean) obj).booleanValue();
			}
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getByteAttribute(String, byte)
	 */
	public final byte getByteAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Byte) {
				return ((Byte) obj).byteValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).byteValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).byteValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).byteValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).byteValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).byteValue();
			}
		}
		return 0;
	}

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getByteAttribute(String)
	 */
	public final byte getByteAttribute(final String name, final byte defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Byte) {
				return ((Byte) obj).byteValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).byteValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).byteValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).byteValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).byteValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).byteValue();
			}
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not byte[] object, then return null.
	 * 
	 * @param name
	 * @return
	 */
	public final byte[] getByteArrayAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null && obj instanceof byte[]) {
			return (byte[]) obj;
		}

		return null;
	}

	/**
	 * if name is not exists or not byte[] object, then return defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public final byte[] getByteArrayAttribute(final String name, final byte[] defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null && obj instanceof byte[]) {
			return (byte[]) obj;
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not Character object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getCharAttribute(String, char)
	 */
	public final char getCharAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Character) {
				return ((Character) obj);
			}
		}
		return 0;
	}

	/**
	 * if name is not exists or not Character object, then return defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getCharAttribute(String)
	 */
	public final char getCharAttribute(final String name, final char defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Character) {
				return ((Character) obj);
			}
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getShortAttribute(String, short)
	 */
	public final short getShortAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Short) {
				return ((Short) obj).shortValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).shortValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).shortValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).shortValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).shortValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).shortValue();
			}
		}
		return 0;
	}

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getShortAttribute(String)
	 */
	public final short getShortAttribute(final String name, final short defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Short) {
				return ((Short) obj).shortValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).shortValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).shortValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).shortValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).shortValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).shortValue();
			}
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getIntAttribute(String, int)
	 */
	public final int getIntAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Integer) {
				return ((Integer) obj).intValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).intValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).intValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).intValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).intValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).intValue();
			}
		}
		return 0;
	}

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getIntAttribute(String)
	 */
	public final int getIntAttribute(final String name, final int defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Integer) {
				return ((Integer) obj).intValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).intValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).intValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).intValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).intValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).intValue();
			}
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getLongAttribute(String, long)
	 */
	public final long getLongAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Long) {
				return ((Long) obj).longValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).longValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).longValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).longValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).longValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).longValue();
			}
		}
		return 0;
	}

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getLongAttribute(String)
	 */
	public final long getLongAttribute(final String name, final long defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Long) {
				return ((Long) obj).longValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).longValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).longValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).longValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).longValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).longValue();
			}
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getFloatAttribute(String, float)
	 */
	public final float getFloatAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Float) {
				return ((Float) obj).floatValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).floatValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).floatValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).floatValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).floatValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).floatValue();
			}
		}
		return 0;
	}

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getFloatAttribute(String)
	 */
	public final float getFloatAttribute(final String name, final float defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Float) {
				return ((Float) obj).floatValue();
			} else if (obj instanceof Double) {
				return ((Double) obj).floatValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).floatValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).floatValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).floatValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).floatValue();
			}
		}

		return defaultValue;
	}

	/**
	 * if name is not exists or not convertible object, then return 0
	 * 
	 * @param name
	 * @return
	 * @see #getDoubleAttribute(String, double)
	 */
	public final double getDoubleAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Double) {
				return ((Double) obj).doubleValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).doubleValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).doubleValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).doubleValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).doubleValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).doubleValue();
			}
		}
		return 0;
	}

	/**
	 * if name is not exists or not convertible object, then return
	 * defaultValue.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getDoubleAttribute(String)
	 */
	public final double getDoubleAttribute(final String name, final double defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof Double) {
				return ((Double) obj).doubleValue();
			} else if (obj instanceof Float) {
				return ((Float) obj).doubleValue();
			} else if (obj instanceof Long) {
				return ((Long) obj).doubleValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).doubleValue();
			} else if (obj instanceof Short) {
				return ((Short) obj).doubleValue();
			} else if (obj instanceof Byte) {
				return ((Byte) obj).doubleValue();
			}
		}
		return defaultValue;
	}

	/**
	 * if name is not exists, return null; if the attribute of name is not
	 * String object, return obj.toString()
	 * 
	 * @param name
	 * @return
	 * @see #getStringAttribute(String, String)
	 */
	public final String getStringAttribute(final String name) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof String) {
				return (String) obj;
			} else {
				return obj.toString();
			}
		}
		return null;
	}

	/**
	 * if name is not exists, then return defaultValue; if the attribute of name
	 * is not String object, return obj.toString()
	 * 
	 * @param name
	 * @param defaultValue
	 * @return the value of attribute <code>name</code>
	 * @see #getStringAttribute(String)
	 */
	public final String getStringAttribute(final String name, final String defaultValue) {
		final Object obj = att_map.get(name);
		if (obj != null) {
			if (obj instanceof String) {
				return (String) obj;
			} else {
				return obj.toString();
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * removes the attribute with the given name. <BR>
	 * <BR>
	 * it is thread safe.
	 * 
	 * @see #setAttribute(String, Object)
	 * @see #getAttribute(String)
	 * @see #getAttributeNames()
	 * @see #clearAttribute(String)
	 * @param name
	 *            the key that needs to be removed
	 * @return the attribute to which the name had been mapped, or null if the
	 *         name did not have a mapping
	 */
	public final Object removeAttribute(final String name) {
		return att_map.remove(name);
	}

	/**
	 * It is equals with {@link #removeAttribute(String)}
	 * 
	 * @param name
	 *            the key that needs to be removed
	 * @return the attribute to which the name had been mapped, or null if the
	 *         name did not have a mapping
	 * @since 7.0
	 */
	public final Object clearAttribute(final String name) {
		return removeAttribute(name);
	}

	/**
	 * returns the attribute with the given name, or null if there is no
	 * attribute by that name. <BR>
	 * It is thread safe.
	 * 
	 * @param name
	 * @return the attribute with the <code>name</code>.
	 * @since 6.98
	 * @see #getIntAttribute(String)
	 * @see #getBooleanAttribute(String)
	 * @see #getStringAttribute(String)
	 * @see #getAttributeNames()
	 * @see #setAttribute(String, Object)
	 * @see #removeAttribute(String)
	 * @see #clearAttribute(String)
	 */
	public final Object getAttribute(final String name) {
		return att_map.get(name);
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setBooleanAttribute(final String name, final boolean value) {
		att_map.put(name, Boolean.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setByteAttribute(final String name, final byte value) {
		att_map.put(name, Byte.valueOf(value));
	}

	/**
	 * set <code>value</code> for the <code>name</code> in attribute.<BR>
	 * to copy to new byte array , please invoke
	 * {@link #setByteArrayAttribute(String, byte[], int, int)}.
	 * 
	 * @param name
	 * @param value
	 *            is NOT copied.
	 */
	public final void setByteArrayAttribute(final String name, final byte[] value) {
		att_map.put(name, value);
	}

	/**
	 * set name with new byte array, which copy values from bs.
	 * 
	 * @param name
	 * @param bs
	 *            the values is copied to new byte array.
	 * @param offset
	 * @param length
	 * @see #setByteArrayAttribute(String, byte[])
	 */
	public final void setByteArrayAttribute(final String name, final byte[] bs, final int offset,
			final int length) {
		final byte[] outbs = new byte[length];
		System.arraycopy(bs, offset, outbs, 0, length);
		att_map.put(name, outbs);
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setCharAttribute(final String name, final char value) {
		att_map.put(name, Character.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setShortAttribute(final String name, final short value) {
		att_map.put(name, Short.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setIntAttribute(final String name, final int value) {
		att_map.put(name, Integer.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setLongAttribute(final String name, final long value) {
		att_map.put(name, Long.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setFloatAttribute(final String name, final float value) {
		att_map.put(name, Float.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setDoubleAttribute(final String name, final double value) {
		att_map.put(name, Double.valueOf(value));
	}

	/**
	 * set attribute name with value.
	 * 
	 * @param name
	 * @param value
	 */
	public final void setStringAttribute(final String name, final String value) {
		att_map.put(name, value);
	}

	/**
	 * returns the attribute with the given name, or <code>defaultValue</code>
	 * if there is no attribute for that name.
	 * 
	 * @param name
	 * @param defaultValue
	 *            the default value for name.
	 * @return <code>defaultValue</code> if this map contains no attribute for
	 *         the name
	 * @since 7.0
	 * @see #getIntAttribute(String, int)
	 * @see #getBooleanAttribute(String, boolean)
	 * @see #getStringAttribute(String, String)
	 */
	public final Object getAttribute(final String name, final Object defaultValue) {
		final Object value = getAttribute(name);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}

	/**
	 * it is equals with {@link #getClientSession()}.
	 * 
	 * @return null if mobile not login, logout or in project level.
	 * @since 7.50
	 */
	public final ClientSession getMobileSession() {
		return getClientSession();
	}

	/**
	 * you can't invoke it before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}
	 * or after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}
	 * 
	 * @return null if mobile not login, logout or in project level.
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.8
	 */
	public final ClientSession getClientSession() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.testSimuClientSession;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getClientSession");
			}
			return null;
		}

		return sessionContext.getClientSession();
	}

	/**
	 * returns an enumeration containing the attribute names available within
	 * this context.
	 * 
	 * @return the enumeration of all attribute names.
	 * @see #getAttribute(String)
	 * @see #setAttribute(String, Object)
	 * @see #removeAttribute(String)
	 * @since 6.98
	 */
	public final Enumeration getAttributeNames() {
		return att_map.keys();
	}

	/**
	 * it is equals with {@link #getMobileHeight()}.
	 * 
	 * @return
	 * @since 7.50
	 * @deprecated
	 */
	@Deprecated
	public final int getClientHeight() {
		return getMobileHeight();
	}

	/**
	 * return the height pixel of login mobile; <BR>
	 * <BR>
	 * you can't invoke it before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}
	 * or after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}.
	 * 
	 * @return 0 means mobile not login or in project level.
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 * @deprecated
	 */
	@Deprecated
	public final int getMobileHeight() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_HEIGHT;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMobileHeight");
			}
			return SimuMobile.PROJ_LEVEL_MOBILE_HEIGHT;
		}

		return UserThreadResourceUtil.getMletHeightFrom(coreSS);
	}

	/**
	 * it is equals with {@link #getMobileDPI()}.
	 * 
	 * @return
	 * @since 7.50
	 */
	public final int getClientDPI() {
		return getMobileDPI();
	}

	/**
	 * for Android client, it means densityDpi; <BR>
	 * <BR>
	 * you can't invoke it before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}
	 * or after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}.
	 * 
	 * @return the DPI of login mobile, 0 means unknown; -1 means mobile not
	 *         login or in project level.
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final int getMobileDPI() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_DPI;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMobileDPI");
			}
			return SimuMobile.PROJ_LEVEL_MOBILE_DPI;
		}

		return UserThreadResourceUtil.getMletDPIFrom(coreSS);
	}

	/**
	 * set a property with new value. <BR>
	 * <BR>
	 * it is thread safe. <br>
	 * <BR>
	 * <code>{@link #saveProperties()}</code> is required to save properties to
	 * persistent system.
	 * 
	 * @param key
	 *            it can't start with reserved string '_HC_SYS'.
	 * @param value
	 *            the value of the property.
	 * @return the previous property of the specified key, or null if it did not
	 *         have one
	 * @since 7.0
	 * @see #getProperty(String)
	 * @see #removeProperty(String)
	 * @see #clearProperty(String)
	 * @see #saveProperties()
	 */
	public final String setProperty(final String key, final String value) {
		if (key.startsWith(CCoreUtil.SYS_RESERVED_KEYS_START, 0)) {
			throw new Error("the key of property can't start with '"
					+ CCoreUtil.SYS_RESERVED_KEYS_START + "', it is reserved by system.");
		}

		return (String) ppm.propertie.put(key, value);
	}

	/**
	 * returns an enumeration containing the properties names available within
	 * this context.
	 * 
	 * @since 7.0
	 * @return an enumeration of all properties names.
	 */
	public final synchronized Enumeration getPropertiesNames() {
		final Iterator<Object> it = ppm.propertie.keySet().iterator();
		final HashSet<Object> set = new HashSet<Object>();
		while (it.hasNext()) {
			final String item = (String) it.next();
			if (item.startsWith(CCoreUtil.SYS_RESERVED_KEYS_START, 0)) {
				continue;
			} else {
				set.add(item);
			}
		}

		final Iterator<Object> setit = set.iterator();
		return new Enumeration() {
			@Override
			public boolean hasMoreElements() {
				return setit.hasNext();
			}

			@Override
			public Object nextElement() {
				return setit.next();
			}
		};
	}

	/**
	 * print stack trace to logger and report it to project provider explicitly
	 * (not implicit). <BR>
	 * <BR>
	 * More about report exception :
	 * <UL>
	 * <LI>enable option-&gt;developer-&gt;<STRONG>report exception</STRONG> is
	 * required.</LI>
	 * <LI>if exception is caught by server, NOT by project, it will be reported
	 * to project provider implicitly.</LI>
	 * <LI>if field <STRONG>Report exception URL / Email address</STRONG> of
	 * project is blank, then reporting is DISABLED and print stack trace
	 * only.</LI>
	 * <LI>it is NOT required to apply for the permission of connection.</LI>
	 * <LI>it reports stack trace on server, NOT for mobile.</LI>
	 * </UL>
	 * <BR>
	 * PHP code to receive exception:<BR>
	 * <BR>
	 * <code>if ('POST' == $_SERVER['REQUEST_METHOD']){//POST to server<BR>
	 * &nbsp;&nbsp;$exception = json_decode(file_get_contents('php://input'), true);//JSON object is UTF-8<BR>
	 * &nbsp;&nbsp;//print_r($exception);//all fields are string even if tag_structureVersion.<BR>
	 * <BR>
	 * &nbsp;&nbsp;//echo $exception['tag_structureVersion'];<BR>
	 * &nbsp;&nbsp;//tag_structureVersion is used to check the structure version of description. if new field is added, it will be upgraded.<BR>
	 * }</code>
	 * 
	 * @param throwable
	 * @since 7.4
	 */
	public static final void printAndReportStackTrace(final Throwable throwable) {
		ExceptionReporter.printStackTrace(throwable, null, null, ExceptionReporter.INVOKE_HAR);
	}

	/**
	 * @deprecated
	 * @param key
	 * @param value
	 */
	@Deprecated
	final String __setPropertySuperOnProj(final String key, final String value) {
		return (String) ppm.propertie.put(key, value);
	}

	/**
	 * @deprecated
	 * @param key
	 * @param value
	 */
	@Deprecated
	final synchronized String __setPropertySuperOnHC(final String key, final String value) {
		return proj_prop_map.put(key, value);
	}

	/**
	 * get the attribute with the given key. <BR>
	 * <BR>
	 * it is thread safe.
	 * 
	 * @param key
	 * @return the value to which the specified key is mapped, or null if
	 *         contains no mapping for the key.
	 * @see #setProperty(String, String)
	 * @see #removeProperty(String)
	 * @see #getPropertiesNames()
	 * @see #clearProperty(String)
	 * @see #saveProperties()
	 * @since 7.0
	 */
	public final String getProperty(final String key) {
		// if (key.startsWith(CCoreUtil.SYS_RESERVED_KEYS_START, 0)) {
		// throw new Error("the key of property can't start with '"
		// + CCoreUtil.SYS_RESERVED_KEYS_START + "'");
		// }

		return ppm.propertie.getProperty(key);
	}

	/**
	 * get the attribute with the given key, return <code>defaultValue</code> if
	 * the key is not set before.
	 * 
	 * @param key
	 * @param defaultValue
	 *            the default value for key.
	 * @return
	 * @see #getProperty(String)
	 * @since 7.0
	 */
	public final String getProperty(final String key, final String defaultValue) {
		final String value = getProperty(key);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}

	/**
	 * @deprecated
	 * @param key
	 * @return the property of key.
	 */
	@Deprecated
	final String __getPropertySuperOnProj(final String key) {
		return (String) ppm.propertie.get(key);
	}

	/**
	 * @deprecated
	 * @param key
	 * @return the property of key.
	 */
	@Deprecated
	final synchronized String __getPropertySuperOnHC(final String key) {
		return proj_prop_map.get(key);
	}

	/**
	 * remove the property with the given name.
	 * 
	 * @see #getProperty(String)
	 * @see #setProperty(String, String)
	 * @see #clearProperty(String)
	 * @see #saveProperties()
	 * @param key
	 *            key whose property is to be removed
	 * @return the property to which the key had been mapped, or null if the key
	 *         did not have a property
	 */
	public final String removeProperty(final String key) {
		if (key.startsWith(CCoreUtil.SYS_RESERVED_KEYS_START, 0)) {
			throw new Error("the key of property can't start with '"
					+ CCoreUtil.SYS_RESERVED_KEYS_START + "'");
		}

		return (String) ppm.propertie.remove(key);
	}

	/**
	 * It is equals with {@link #removeProperty(String)}
	 * 
	 * @param key
	 *            key whose property is to be removed
	 * @return the property to which the key had been mapped, or null if the key
	 *         did not have a property
	 * @since 7.0
	 */
	public final String clearProperty(final String key) {
		return removeProperty(key);
	}

	/**
	 * @deprecated
	 * @param key
	 */
	@Deprecated
	final String __removePropertySuperOnProj(final String key) {
		return (String) ppm.propertie.remove(key);
	}

	/**
	 * @deprecated
	 * @param key
	 */
	@Deprecated
	final synchronized String __removePropertySuperOnHC(final String key) {
		return proj_prop_map.remove(key);
	}

	/**
	 * for example :<BR>
	 * 1. <code>getPrivateFile("testFile");</code><br>
	 * 2. <code>getPrivateFile("mySubDir").mkdir();</code><br>
	 * 3.
	 * <code>getPrivateFile("mySubDir2/subSubDir").mkdirs();//it works on Windows, Linux and Mac</code>
	 * <br>
	 * 4. <code>getPrivateFile("mySubDir/testFile");</code><br>
	 * 5. <code>getPrivateFile("mySubDir2/subSubDir/testFile");</code><br>
	 * 6. <code>new File(getPrivateFile("mySubDir"), "testFile");</code><br>
	 * 7.
	 * <code>new File(getPrivateFile("mySubDir2/subSubDir"), "testFile");</code><BR>
	 * <BR>
	 * <STRONG>Tip :</STRONG><BR>
	 * if parent directory of result is not exists, then it will be created
	 * before return.<BR>
	 * <BR>
	 * <STRONG>private file VS DB connection :</STRONG><BR>
	 * When project runs in Home environment, please pay more attention about
	 * power off, it may be caused by children, no wait to shutdown. If private
	 * file is frequently written, using DB connection is a better choice,
	 * because it appends data to log at the tail of file and DB manager will
	 * fix error that abnormal shutdown. All-In-RAM DB never write file to
	 * disk.<BR>
	 * <br>
	 * <STRONG>More about private file :</STRONG><BR>
	 * 1. the file is allowed full access and NOT limited by HomeCenter security
	 * manager, and it is also protected from being read or written by other HAR
	 * projects. <br>
	 * 2. the File <code>fileName</code> is read/written in directory
	 * <i>{HC_Home}/user_data/{projectID}/{fileName}</i>. <br>
	 * 3. sub directory <code>"{projectID}/_HC/"</code> is reservered by server,
	 * see {@link #getDBConnection(String, String, String)},<br>
	 * 4. if current project is deleted, all private files of current project
	 * will be deleted. <BR>
	 * 5. to save small data, please invoke {@link #saveProperties()}.<BR>
	 * 6. if the data is important, please encrypt data or stored
	 * separately.<BR>
	 * 7. if this server runs on Android Marshmallow or later, Android will do
	 * "Auto Backup for Apps".
	 * 
	 * @param fileName
	 * @return the private file <code>fileName</code>.
	 * @see #createTempFile(String)
	 * @see #saveProperties()
	 * @see #getDBConnection(String, String, String)
	 * @since 7.0
	 */
	public final File getPrivateFile(final String fileName) {
		final String absProjBasePath = StoreDirManager.getUserDataBaseDir(projectID);
		final String absPathname = absProjBasePath + HttpUtil.encodeFileName(fileName);
		final File fileResult = new File(absPathname);// App.getBaseDir
		try {
			final File parent = fileResult.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return fileResult;
	}

	/**
	 * it is equals with {@link #getPrivateFile(String)}.
	 * 
	 * @param fileName
	 * @return
	 */
	public final File createPrivateFile(final String fileName) {
		return getPrivateFile(fileName);
	}

	/**
	 * save properties to persistent system. <br>
	 * <br>
	 * <STRONG>Important :</STRONG> <br>
	 * 1. data of properties will be deleted if current project is removed. <br>
	 * 2. it is a good practice to save small data in properties system and save
	 * big data in local disk / cloud system. <br>
	 * 3. if you want to create file on local disk, please use
	 * {@link #getPrivateFile(String)} to get instance of File.<br>
	 * 4. to create and open database, please invoke
	 * {@link #getDBConnection(String, String, String)}.<br>
	 * 5. if this server runs on Android Marshmallow or later, Android will
	 * 'Auto Backup for Apps'.
	 * 
	 * @see #setProperty(String, String)
	 * @see #getProperty(String)
	 * @see #removeProperty(String)
	 * @see #backup()
	 * @since 7.0
	 */
	public final void saveProperties() {
		ppm.save();
	}

	final void __saveSysPropertiesOnHC() {
		proj_prop_map.save();
	}

	private final PropertiesMap proj_prop_map;

	/**
	 * if your mobile is Android, it will return this after invoke
	 * {@link #getMobileOS()}.
	 * 
	 * @since 7.0
	 */
	public static final String OS_ANDROID = ConfigManager.OS_ANDROID_DESC;

	/**
	 * if your mobile is iOS, it will return this after invoke
	 * {@link #getMobileOS()}.
	 * 
	 * @since 7.0
	 */
	public static final String OS_IOS = ConfigManager.OS_IOS_DESC;

	/**
	 * if your mobile is J2ME, it will return this after invoke
	 * {@link #getMobileOS()}.
	 * 
	 * @since 7.0
	 */
	public static final String OS_J2ME = ConfigManager.OS_J2ME_DESC;

	/**
	 * it is equals with {@link #getMobileOSVer()}.
	 * 
	 * @return
	 * @since 7.50
	 */
	public final String getClientOSVer() {
		return getMobileOSVer();
	}

	/**
	 * you can't invoke it before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}
	 * or after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}.
	 * 
	 * @return the version of OS of mobile; <BR>
	 * 		"0.0.1" means mobile not login or not in session level.
	 * @see #getMobileOS()
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final String getMobileOSVer() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_OS_VER;
		}

		// 注意：不能用field来cache，因为可能发生变更
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMobileOSVer");
			}
			return SimuMobile.PROJ_LEVEL_MOBILE_OS_VER;
		}

		return UserThreadResourceUtil.getMobileOSVerFrom(coreSS);
	}

	/**
	 * it is equals with {@link #getMobileOS()}.
	 * 
	 * @return
	 * @since 7.50
	 */
	public final String getClientOS() {
		return getMobileOS();
	}

	/**
	 * return one of the following: <br>
	 * 1. empty string if mobile not login or not in session level<br>
	 * 2. {@link #OS_ANDROID} <br>
	 * 3. {@link #OS_IOS} <br>
	 * 4. {@link #OS_J2ME} <br>
	 * 5. and other in the future. <BR>
	 * <BR>
	 * you can't invoke it before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}
	 * or after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}.
	 * 
	 * @return
	 * @see #getMobileOSVer()
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final String getMobileOS() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_OS;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMobileOS");
			}
			return SimuMobile.PROJ_LEVEL_MOBILE_OS;
		}

		// 注意：不能用field来cache，因为可能发生变更
		return UserThreadResourceUtil.getMobileOSFrom(coreSS);
	}

	/**
	 * find the best match locale from a map.<BR>
	 * <BR>
	 * the followings languages are treated as equivalents. (see Java Doc
	 * <a href=
	 * "https://docs.oracle.com/javase/7/docs/api/java/util/Locale.html">Locale</a>)<BR>
	 * 1. <code>he</code> and <code>iw</code><BR>
	 * 2. <code>yi</code> and <code>ji</code><BR>
	 * 3. <code>id</code> and <code>in</code><BR>
	 * <BR>
	 * for example, locale is "zh-Hans-CN", the match process is following :<BR>
	 * 1. if match "zh-Hans-CN", then ok,<BR>
	 * 2. if fail, try match "zh-Hans",<BR>
	 * 3. if fail, try match "zh-CN",<BR>
	 * 4. if fail, try match "zh",<BR>
	 * 5. if fail, try match "en-US",<BR>
	 * 6. if fail, try match "en",<BR>
	 * 7. if fail, return null;<BR>
	 * <BR>
	 * if locale is "he-IL", the match process is following :<BR>
	 * 1. if match "he-IL", then ok,<BR>
	 * 2. if fail, try match "iw-IL",<BR>
	 * 3. if fail, try match "he",<BR>
	 * 4. if fail, try match "iw",<BR>
	 * 5. if fail, try match "en-US",<BR>
	 * 6. if fail, try match "en",<BR>
	 * 7. if fail, return null;
	 * 
	 * @param locale
	 *            case sensitive
	 * @param map
	 * @return null if fail to match.
	 * @see #getMobileLocale()
	 * @since 7.40
	 */
	public static String matchLocale(final String locale, final Map map) {
		return ResourceUtil.matchLocale(locale, map, true);
	}

	/**
	 * check a locale is RTL (Right to Left) or not.
	 * 
	 * @param locale
	 * @return true means RTL.
	 * @see #getMobileLocale()
	 * @since 7.40
	 */
	public static boolean isRTL(final String locale) {
		return LangUtil.isRTL(locale);
	}

	/**
	 * it is equals with {@link #getMobileLocale()}.
	 * 
	 * @return
	 * @since 7.50
	 */
	public final String getClientLocale() {
		return getMobileLocale();
	}

	/**
	 * three format locales : <br>
	 * 1. language ("en", "fr", "ro", "ru", etc.) <br>
	 * 2. language-region ("en-US", "es-419", etc.)<br>
	 * 3. language-region ("zh-Hans-CN", "zh-Hant-CN", etc.)<br>
	 * <br>
	 * Know more :<BR>
	 * 1. to find the best match from an I18N map, see
	 * {@link #matchLocale(String, Map)}.<BR>
	 * 2. user maybe change client language and country/region.<BR>
	 * 3. to check a locale is RTL (Right To Left) or not, see
	 * {@link #isRTL(String)}.<BR>
	 * 4. for RTL layout, see
	 * {@link Mlet#enableApplyOrientationWhenRTL(boolean)}.<BR>
	 * 5. to set RTL or not for HTML DIV and its sub elements, see
	 * {@link HTMLMlet#setRTL(javax.swing.JComponent, boolean)}
	 * 
	 * @return in session level, return mobile locale; <BR>
	 * 		in project level, returns the locale for all sessions or 'en-US'.
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final String getMobileLocale() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_LOCALE;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMobileLocale");
			}
			return ServerUIAPIAgent.getMobileLocaleFromAllSessionsNoCheck();
		}

		return UserThreadResourceUtil.getMobileLocaleFrom(coreSS);
	}

	/**
	 * action keyboard keys on server. For example "Control+Shift+Escape",
	 * "Shift+a" (for input 'A'), "B" (for input 'b'). <BR>
	 * <BR>
	 * more key string , please refer <a target="_blank" href=
	 * "http://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html"
	 * >http
	 * ://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html</a><br>
	 * <br>
	 * NOTE : NOT all keys are supported. (META is Max OS X Command)<br>
	 * <br>
	 * If a HAR package developed in J2SE, and run in Android platform, it does
	 * as following:<br>
	 * 1. the key name is searched first in <code>java.awt.event.KeyEvent</code>
	 * ('VK_' is NOT required. 'VK_' is prefix in J2SE), <br>
	 * 2. convert keyCode in J2SE to Android (e.g.
	 * java.awt.event.KeyEvent.VK_BACK_SPACE =&gt;
	 * android.view.KeyEvent.KEYCODE_DEL). <br>
	 * 3. if your HAR package is designed for Android only, for example,
	 * Shift+KEYCODE_a (for input 'A', 'KEYCODE_' is required prefix for
	 * Android).<br>
	 * 4. more Android keys, please refer <a target="_blank" href=
	 * "http://developer.android.com/reference/android/view/KeyEvent.html"
	 * >http://developer.android.com/reference/android/view/KeyEvent.html</a><br>
	 * 5. in J2SE, keys begin with 'VK_'; in Android, keys begin with
	 * 'KEYCODE_'. <br>
	 * 
	 * @param keys
	 *            for example, "Control+Shift+Escape" or "Tab" (case
	 *            insensitive)
	 * @since 7.0
	 */
	public final void actionKeys(final String keys) {
		if (keys == null) {
			return;
		}

		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				KeyComper.actionKeys(keys);
				return null;
			}
		});
	}

	/**
	 * log an error message to log system or console
	 * 
	 * @param msg
	 * @see #error(String)
	 * @since 7.0
	 */
	public final void err(final String msg) {
		error(msg);
	}

	/**
	 * log message to server
	 * 
	 * @param msg
	 * @since 7.0
	 */
	public final void log(final String msg) {
		LogManager.log(msg);
	}

	private final static String[] buildParaForClass(final int paraNum) {
		final String[] p = new String[paraNum + 1];
		p[0] = HCURL.DATA_PARA_CLASS;
		for (int i = 1; i <= paraNum; i++) {
			p[i] = String.valueOf(i);
		}
		return p;
	}

	private final static void sendCmd(final CoreSession coreSS, final String cmdType,
			final String para, final String value) {
		if (UserThreadResourceUtil.isInServing(coreSS.context)) {
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() throws Throwable {
					HCURLUtil.sendCmd(coreSS, cmdType, para, value);
					return null;
				}
			});
		}
	}

	private final static void sendCmdInSysThread(final CoreSession coreSS, final String cmdType,
			final String[] para, final String[] value) {
		if (UserThreadResourceUtil.isInServing(coreSS.context)) {
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() throws Throwable {
					HCURLUtil.sendCmd(coreSS, cmdType, para, value);
					return null;
				}
			});
		}
	}

	private final static void sendClass(final CoreSession coreSS, final String[] para) {
		sendCmdInSysThread(coreSS, HCURL.DATA_CMD_SendPara, buildParaForClass(para.length - 1),
				para);
	}

	/**
	 * play tone at mobile if in session level; or play tone to all client
	 * session if in project level. <BR>
	 * <BR>
	 * you should disable mute option on mobile first.
	 * 
	 * @param note
	 *            A note is given in the range of 0 to 127 inclusive. Defines
	 *            the tone of the note as specified by the above formula.
	 * @param duration
	 *            The duration of the tone in milli-seconds. Duration must be
	 *            positive.
	 * @param volume
	 *            Audio volume range from 0 to 100. 100 represents the maximum
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 6.98
	 */
	public final void playTone(final int note, final int duration, final int volume) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		final J2SESession[] coreSSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("playTone");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			final String[] v = { "hc.j2me.load.Tone", String.valueOf(note),
					String.valueOf(duration), String.valueOf(volume) };
			for (int i = 0; i < coreSSS.length; i++) {
				sendClass(coreSSS[i], v);
			}
		}
	}

	/**
	 * send notification to current mobile if in session level, or send
	 * notification to all client session if in project level. <BR>
	 * <BR>
	 * if mobile option [Message, Notification to Speech also] is
	 * <STRONG>on</STRONG>, it may be spoken.<BR>
	 * the speech or not is depends on text, TTS engine, locale and mute of
	 * mobile.
	 * 
	 * @param title
	 *            the title of notification.
	 * @param text
	 *            the body of notification.
	 * @param flags
	 *            one of the following or combination, or none. <BR>
	 *            {@link #FLAG_NOTIFICATION_SOUND} : notification with sound. If
	 *            mute option of mobile is enabled, then no sound; <BR>
	 *            {@link #FLAG_NOTIFICATION_VIBRATE} : notification with vibrate
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final void sendNotification(final String title, final String text, final int flags) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		final J2SESession[] coreSSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("sendNotification");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			final String[] v = { ConfigManager.HC_J2ME_LOAD_NOTIFICATION, title, text,
					String.valueOf(flags) };
			for (int i = 0; i < coreSSS.length; i++) {
				sendClass(coreSSS[i], v);
			}

			processFormData(text);
		}
	}

	/**
	 * it is useful for thread of user to stop task and release resource when
	 * stop project.<BR>
	 * you can set new value in [option/other/interval seconds between stop and
	 * restart HAR project]. <BR>
	 * <BR>
	 * <STRONG>deprecated</STRONG><BR>
	 * please use {@link #getScheduler(String, boolean)} for time scheduler.
	 * 
	 * @return the milliseconds between stop and restart HAR project(s).
	 * @since 7.0
	 */
	public final int getIntervalMSForRestart() {
		// 注意：不能用field来cache，因为可能发生变更
		return (Integer) ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				return ResourceUtil.getIntervalSecondsForNextStartup() * 1000;
			}
		});
	}

	/**
	 * in session level, true means mobile is line on.<BR>
	 * in project level, true means one or more mobiles are online.
	 * 
	 * @return
	 */
	public final boolean isMobileLineOn() {
		return isClientLineOn();
	}

	/**
	 * it is equals with {@link #isMobileLineOn()}.
	 * 
	 * @return
	 */
	public final boolean isClientLineOn() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_CONNECTING;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS = null;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("isClientLineOn");
			}
			return ServerUIAPIAgent.isClientLineOn();
		} else {
			return coreSS.isPreLineOff() == false;
		}
	}

	/**
	 * check current server is running on Android or not.
	 * 
	 * @return true means run on Android; false means run on Oracle JRE/JDK,
	 *         OpenJDK JVM or other. platform.
	 * @see #isJ2SEPlatform()
	 * @since 7.0
	 */
	public final boolean isAndroidPlatform() {
		if (isAndroidPlatform == null) {
			isAndroidPlatform = (Boolean) ServerUIAPIAgent
					.runAndWaitInSysThread(new ReturnableRunnable() {
						@Override
						public Object run() throws Throwable {
							return ResourceUtil.isAndroidServerPlatform();
						}
					});
		}
		return isAndroidPlatform;
	}

	private Boolean isAndroidPlatform;

	/**
	 * check current server is running on J2SE platform (Oracle JDK/JRE, OpenJDK
	 * JVM) or not.
	 * 
	 * @return true means runs on J2SE; false means runs on Android or other
	 *         platform.
	 * @see #isAndroidPlatform()
	 * @since 7.0
	 */
	public final boolean isJ2SEPlatform() {
		if (isJ2SEPlatform == null) {
			isJ2SEPlatform = (Boolean) ServerUIAPIAgent
					.runAndWaitInSysThread(new ReturnableRunnable() {
						@Override
						public Object run() throws Throwable {
							return ResourceUtil.isStandardJ2SEServer();
						}
					});
		}
		return isJ2SEPlatform;
	}

	private Boolean isJ2SEPlatform;

	/**
	 * notify vibrate to current mobile if in session level, or notify vibrate
	 * to all client session if in project level.
	 * 
	 * @param duration
	 *            the number of milliseconds the vibrator should be run
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 6.98
	 */
	public final void vibrate(final int duration) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		final J2SESession[] coreSSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("vibrate");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			final String[] v = { "hc.j2me.load.Vibrate", String.valueOf(duration) };
			for (int i = 0; i < coreSSS.length; i++) {
				sendClass(coreSSS[i], v);
			}
		}
	}

	/**
	 * cancel alert which is created in session or project level, no matter
	 * current thread is session or project level.
	 * 
	 * @param alertKey
	 *            the key will be canceled from alert. If the key is canceled
	 *            already or not exists, then do nothing.
	 * @see #isCurrentThreadInSessionLevel()
	 * @see #alertOn(String)
	 * @since 7.71
	 */
	public final void alertOff(final String alertKey) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final J2SESession[] coreSS;
		// final SessionContext sessionContext =
		// __projResponserMaybeNull.getSessionContextFromCurrThread();
		// if(sessionContext == null || (coreSS =
		// sessionContext.j2seSocketSession) == null){
		// if(L.isInWorkshop){
		// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
		// }
		// if(isLoggerOn == false){
		// ServerUIAPIAgent.printInProjectLevelWarn("alertOff");
		// }
		coreSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		// }else{
		// coreSS = toArray(sessionContext.j2seSocketSession);
		// }

		if (coreSS != null && coreSS.length > 0) {
			final String projAlertKey = projectID + alertKey;
			for (int i = 0; i < coreSS.length; i++) {
				final J2SESession oneCoreSS = coreSS[i];
				if (oneCoreSS.alertOff(projAlertKey)) {
					sendCmd(oneCoreSS, HCURL.DATA_CMD_ALERT, "status", IConstant.OFF);
				}
			}
		}
	}

	/**
	 * set alert off on mobile if in session level, or set alert off to all
	 * client sessions if in project level. <BR>
	 * <STRONG>deprecated</STRONG>, replaced by {@link #alertOff(String)}.
	 * 
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 6.98
	 */
	@Deprecated
	public final void alertOff() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final J2SESession[] coreSSS;
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("alertOff");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			for (int i = 0; i < coreSSS.length; i++) {
				sendCmd(coreSSS[i], HCURL.DATA_CMD_ALERT, "status", IConstant.OFF);
			}
		}
	}

	/**
	 * set alert on for mobile if in session level, or set alert on to all
	 * client sessions if in project level. <BR>
	 * <BR>
	 * when alert on, mobile will keep vibrate and play a tone.
	 * 
	 * @param alertKey
	 *            the key is used for cancel the alert, no matter it is created
	 *            in session or project level.
	 * @see #alertOff(String)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.71
	 */
	public final void alertOn(final String alertKey) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final J2SESession[] coreSSS;
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("alertOn");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			final String projAlertKey = projectID + alertKey;
			for (int i = 0; i < coreSSS.length; i++) {
				final J2SESession oneCoreSS = coreSSS[i];
				if (oneCoreSS.alertOn(projAlertKey)) {
					sendCmd(oneCoreSS, HCURL.DATA_CMD_ALERT, "status", IConstant.ON);
				}
			}
		}
	}

	/**
	 * set alert on on mobile if in session level, or set alert on to all client
	 * sessions if in project level. <BR>
	 * <STRONG>deprecated</STRONG>, replaced by {@link #alertOn(String)}.
	 * 
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 6.98
	 */
	@Deprecated
	public final void alertOn() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final J2SESession[] coreSSS;
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("alertOn");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			for (int i = 0; i < coreSSS.length; i++) {
				sendCmd(coreSSS[i], HCURL.DATA_CMD_ALERT, "status", IConstant.ON);
			}
		}
	}

	/**
	 * it is equals with
	 * <code>sendMessage(caption, text, type, null, 0)</code>.<BR>
	 * <BR>
	 * in session level, send a alert message to mobile.<BR>
	 * in project level, send the same alert to all client sessions.<BR>
	 * <BR>
	 * Note : if mobile is in background ({@link #isMobileInBackground()}), a
	 * notification is also created for mobile.<BR>
	 * <BR>
	 * if mobile option [Message, Notification to Speech also] is
	 * <STRONG>on</STRONG>, it may be spoken.<BR>
	 * the speech or not is depends on text, TTS engine, locale and mute of
	 * mobile.
	 * 
	 * @param caption
	 *            the caption of message.
	 * @param text
	 *            the body of message.
	 * @param type
	 *            one of {@link #MESSAGE_ERROR}, {@link #MESSAGE_WARN},
	 *            {@link #MESSAGE_INFO}, {@link #MESSAGE_ALARM},
	 *            {@link #MESSAGE_CONFIRMATION}.
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @since 6.98
	 */
	public final void send(final String caption, final String text, final int type) {
		try {
			sendMessage(caption, text, type, null, 0);
		} catch (final Throwable e) {
			// 设计时，手机非在线时，
		}
	}

	/**
	 * return the login ID/Email, which is NOT verified by HomeCenter.MOBI
	 * possibly. <BR>
	 * <BR>
	 * <STRONG>Important : </STRONG><BR>
	 * user maybe change ID according to their own wishes, project will restart
	 * and new instance of <code>ProjectContext</code> will be created if the
	 * login ID/Email is changed. <BR>
	 * <BR>
	 * do follow steps to check the login Email account is verified by
	 * HomeCenter.MOBI or not: <BR>
	 * 1. get login ID/Email by {@link #getLoginID()} <BR>
	 * 2. get token for check by {@link #getTokenForCheck()} <BR>
	 * 3. if user donate or buy some service, the token will be changed! <BR>
	 * 4. submit URL :
	 * "<STRONG>https</STRONG>://homecenter.mobi/ajax/call.php?f=checkEmail&email=%X%X&token=XX"
	 * <BR>
	 * 5. email should be encoded by
	 * <STRONG>java.net.URLEncoder.encode("email@company.com","UTF-8")</STRONG>
	 * <BR>
	 * 6. token is NOT required to encode. <BR>
	 * 7. if successful then return "verified"; if not , then return "failed".
	 * <BR>
	 * 8. there is not method do above, because this is open source server. <BR>
	 * <BR>
	 * <STRONG>Note : </STRONG> <BR>
	 * 1. if the verification is very important to your business, you should
	 * ensure the <STRONG>https</STRONG> is NOT under Man-in-the-middle attack.
	 * <BR>
	 * 2. user is NOT allowed to setup multiple servers with same login account,
	 * only one server is verified at same time.
	 * 
	 * @return login ID/Email on this server
	 * @see #getMemberID()
	 * @since 7.0
	 */
	public final String getLoginID() {
		if (loginID == null) {
			loginID = IConstant.getUUID();
			if (loginID == null) {
				loginID = SimuMobile.MOBILE_LOGIN_ID;
			}
		}
		return loginID;
	}

	/**
	 * it is equals with {@link #getLoginID()}.
	 * 
	 * @return
	 * @see #getClientMemberID()
	 */
	public final String getClientLoginID() {
		return getLoginID();
	}

	/**
	 * it is equals with {@link #getLoginID()}.
	 * 
	 * @return
	 * @see #getMemberID()
	 */
	public final String getMobileLoginID() {
		return getLoginID();
	}

	String loginID;

	/**
	 * return the token to check login Email is verified or not. <BR>
	 * <BR>
	 * the token may be changed if user donate or buy some service.
	 * 
	 * @return it is not null even if not verified.
	 * @see #getLoginID()
	 * @since 7.14
	 */
	public final String getTokenForCheck() {
		// 注意：不能用field来cache，因为可能发生变更
		return (String) ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				final String token = TokenManager.getToken();
				return ResourceUtil.getMD5(token);
			}
		});
	}

	// /**
	// * the number of unallocated bytes on the partition.
	// * <BR><BR>
	// * <STRONG>Note :</STRONG><BR>
	// * for security, user has no right to call
	// <code>file.getFreeSpace()</code>.
	// * @param file the partition of this file.
	// * @return
	// * @since 7.14
	// */
	// public final static long getFreeSpace(final File file){
	// return (Long)ServerUIAPIAgent.runAndWaitInSysThread(new
	// ReturnableRunnable() {
	// @Override
	// public Object run() throws Throwable {
	// return file.getFreeSpace();
	// }
	// });
	// }

	/**
	 * it is equals with {@link #getSoftUID()}.
	 * 
	 * @return
	 * @since 7.50
	 */
	public final String getClientSoftUID() {
		return getSoftUID();
	}

	/**
	 * it is equals with {@link #getSoftUID()}.
	 * 
	 * @return
	 * @since 7.2
	 */
	public final String getMobileSoftUID() {
		return getSoftUID();
	}

	/**
	 * <code>SoftUID</code> is an identifier created when mobile application is
	 * installed on mobile. <BR>
	 * <BR>
	 * if mobile application is removed and install again on same mobile, the
	 * <code>SoftID</code> is changed. <BR>
	 * it is NOT ID from hardware. <BR>
	 * <BR>
	 * you can't invoke it before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}
	 * or after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}.
	 * 
	 * @return empty string if mobile not login or not in session level.
	 * @see #getLoginID()
	 * @see #getMemberID()
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.74
	 */
	public final String getSoftUID() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_SOFT_UID;
		}

		final String noLoginUID = "";

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMobileSoftUID");
			}
			return noLoginUID;
		}

		if (UserThreadResourceUtil.isInServing(coreSS.context)) {
			return UserThreadResourceUtil.getMobileSoftUID(coreSS);
		} else {
			return noLoginUID;
		}
	}

	/**
	 * it is equals with {@link #getMemberID()}.
	 * 
	 * @return
	 * @since 7.74
	 * @see #getMobileLoginID()
	 */
	public final String getMobileMemberID() {
		return getMemberID();
	}

	/**
	 * it is equals with {@link #getMemberID()}.
	 * 
	 * @return
	 * @since 7.74
	 * @see #getClientLoginID()
	 */
	public final String getClientMemberID() {
		return getMemberID();
	}

	/**
	 * <code>MemberID</code> is used to distinguish between different members of
	 * a family or group, which belongs to same <code>LoginID</code>. <BR>
	 * <BR>
	 * <STRONG>Know more :</STRONG> <BR>
	 * 1. <code>MemberID</code> is stored in client. <BR>
	 * 2. if <code>MemberID</code> is not set, a dialog for input member ID is
	 * showed on client, and current thread is block until return
	 * <code>MemberID</code> or line off. <BR>
	 * 3. this method is synchronized for multiple projects. <BR>
	 * 4. the <code>MemberID</code> dialog can not be canceled (for example back
	 * key in Android). <BR>
	 * 5. user maybe set same <code>MemberID</code> in multiple mobile at same
	 * time. <BR>
	 * 6. if same <code>MemberID</code> connected at same time, then a warning
	 * message is send to each client. <BR>
	 * 7. to set/change <code>MemberID</code> before login, click "Option"
	 * button at client login form. <BR>
	 * 8. it is available in {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}.
	 * 
	 * @return empty string if line off, not login or not in session level.
	 * @see #getLoginID()
	 * @see #getSoftUID()
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.74
	 */
	public final String getMemberID() {
		final String memberID = getMobileMemberIDImpl();
		if (memberID == null || memberID.length() == 0) {
			final ClientSession session = getClientSession();
			if (session == null) {
				return memberID;
			} else {
				session.notifyInputMemberID(this);
			}

			return getMobileMemberIDImpl();
		} else {
			return memberID;
		}
	}

	private final String getMobileMemberIDImpl() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_MEMBER_ID;
		}

		final String noLoginUID = "";

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("getMobileMemberID");
			}
			return noLoginUID;
		}

		if (UserThreadResourceUtil.isInServing(coreSS.context)) {
			return UserThreadResourceUtil.getMobileMemberID(coreSS);
		} else {
			return noLoginUID;
		}
	}

	boolean isShutdown;

	final void shutdown() {
		isShutdown = true;
	}

	/**
	 * return the projectContext instance of the current project.<BR>
	 * <BR>
	 * you can invoke this method anywhere even if there are two or more active
	 * projects.
	 * 
	 * @return
	 */
	public final static ProjectContext getProjectContext() {
		// final ContextSecurityConfig csc = ContextSecurityManager
		// .getConfig(Thread.currentThread().getThreadGroup());
		// if (csc != null) {
		// return csc.getProjectContext();
		// }
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null))
				|| (csc = ContextSecurityManager
						.getConfig(currentThread.getThreadGroup())) != null) {
			return csc.getProjectContext();
		}

		// final ProjectContext ctx = HCLimitSecurityManager
		// .getProjectContextFromDispatchThread();
		// if (ctx != null) {
		// return ctx;
		// }

		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append("\n");
		sb.append("-------------------------------important-----------------------------");
		sb.append("\n");
		sb.append(
				"In designer, ProjectContext.getProjectContext will return a context just for test run!!!");
		sb.append("\n");
		sb.append("------------------------------------------------------------------------");
		LogManager.log(sb.toString());
		StringBuilderCacher.cycle(sb);
		return SimuMobile.simuContext;
	}

	/**
	 * <STRONG>Warning</STRONG> : when in project level it will do nothing. <BR>
	 * <BR>
	 * send a <code>Dialog</code> to client. <br>
	 * <br>
	 * if there is a alert message, question or other dialog is presented on
	 * client and NOT be closed, the dialog will be delayed. <br>
	 * <br>
	 * this method is <STRONG>asynchronous</STRONG>. system will NOT wait for
	 * the result of dialog to the caller. <BR>
	 * <BR>
	 * Note : if mobile is in background ({@link #isMobileInBackground()}), a
	 * notification is also created for mobile.<BR>
	 * <BR>
	 * 
	 * @param dialog
	 * @see #sendDialogAndWaitWhenInSession(Dialog)
	 * @see #sendDialogByBuilding(Runnable)
	 * @see #showInputDialog(String, String[], String[])
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.30
	 */
	public final void sendDialogWhenInSession(final Dialog dialog) {
		sendDialogWhenInSessionImpl(dialog, false);
	}

	/**
	 * see {@link #sendDialogWhenInSession(Dialog)} for more. <BR>
	 * <BR>
	 * <STRONG>About return status :</STRONG><BR>
	 * 1. in session level, return true means dialog is dismissed by client,
	 * false means canceled by client, when line off the execution will be
	 * terminated.<BR>
	 * 2. in project level, return false;
	 * 
	 * @param dialog
	 * @return
	 * @see #isClientLineOn()
	 */
	public final boolean sendDialogAndWaitWhenInSession(final Dialog dialog) {
		return sendDialogWhenInSessionImpl(dialog, true);
	}

	private final boolean sendDialogWhenInSessionImpl(final Dialog dialog,
			final boolean isWaiting) {
		if (dialog == null) {
			return false;
		}

		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return true;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();

		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			errWhenInSession("sendDialogWhenInSession");
			return false;
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);

			final J2SESession[] coreSSS = { coreSS };

			return publishDialog(coreSS, coreSSS, dialog, null, isWaiting);
		}
	}

	/**
	 * when line-off, the script for session which is running or will to run
	 * will be stopped by throwing line off error.
	 * 
	 * @param throwable
	 * @return
	 */
	public final boolean isLineOffThrowable(final Throwable throwable) {
		return ExceptionReporter.isCauseByLineOffSession(throwable);
	}

	/**
	 * send a dialog to mobile if in session level, or send same dialog(s) to
	 * all client sessions if in project level. <br>
	 * <br>
	 * in project level, if one session replies, then the same dialog(s) in
	 * other sessions will be dismissed. <br>
	 * <br>
	 * code sample, <BR>
	 * <BR>
	 * in <STRONG>JRuby</STRONG> :<BR>
	 * <code>
	 * ctx.sendDialogByBuilding {
	 * <BR>
	 * &nbsp;&nbsp;MyDefDialog.new()&nbsp;&nbsp;#important : return is NOT allowed.<BR>
	 * }<BR>
	 * </code> <BR>
	 * in <STRONG>Java</STRONG> :<BR>
	 * <code>
	 * ctx.sendDialogByBuilding(new Runnable() {
	 * <BR>
	 * &nbsp;&nbsp;public void run() {<BR>
	 * &nbsp;&nbsp;&nbsp;&nbsp;new MyDefDialog();<BR>
	 * &nbsp;&nbsp;}<BR>
	 * });<BR>
	 * </code> <BR>
	 * <STRONG>Why</STRONG> the parameter is Runnable to build instance from
	 * defined JRuby class or a Java class? <BR>
	 * because the layout of dialog is depends on the client screen size of that
	 * session.<BR>
	 * for example, there are three sessions, the <code>runnable</code> will be
	 * executed three times, <BR>
	 * and three instances of <code>Dialog</code> are builded for each session.
	 * <br>
	 * <br>
	 * if there is a alert message, question or other dialog is presented on
	 * client and NOT closed, the dialog will be delayed. <br>
	 * <br>
	 * this method is <STRONG>asynchronous</STRONG>, server will NOT wait for
	 * the result of dialog to the caller. <BR>
	 * <BR>
	 * Note : if mobile is in background ({@link #isMobileInBackground()}), a
	 * notification is also created for mobile.
	 * 
	 * @param runnable
	 *            the <code>Runnable</code> to build instance from a defined
	 *            JRuby class or a Java class.
	 * @see #sendDialogAndWaitByBuilding(Runnable)
	 * @see #sendDialogWhenInSession(Dialog)
	 * @see #isClientLineOn()
	 * @since 7.30
	 */
	public final void sendDialogByBuilding(final Runnable runnable) {
		sendDialogByBuildingImpl(runnable, false);
	}

	/**
	 * see {@link #sendDialogByBuilding(Runnable)} for more. <BR>
	 * <BR>
	 * <STRONG>About return status :</STRONG><BR>
	 * 1. in project level, return true means dialog is dismissed/canceled by
	 * one client, false means all sessions are line off.<BR>
	 * 2. in session level, return true means dialog is dismissed by client,
	 * false means canceled by client, when line off the execution will be
	 * terminated.
	 * 
	 * @param runnable
	 * @return
	 * @see #isClientLineOn()
	 */
	public final boolean sendDialogAndWaitByBuilding(final Runnable runnable) {
		return sendDialogByBuildingImpl(runnable, true);
	}

	private final boolean sendDialogByBuildingImpl(final Runnable runnable,
			final boolean isWaiting) {
		if (runnable == null) {
			return false;
		}

		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return true;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS = null;
		final J2SESession[] coreSSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("sendDialogByBulding");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);

			coreSSS = toArray(coreSS);
		}

		if (coreSSS == null || coreSSS.length == 0) {
			return false;
		}

		return publishDialog(coreSS, coreSSS, null, runnable, isWaiting);
	}

	private final boolean publishDialog(final J2SESession session, final J2SESession[] coreSSS,
			final Dialog dialog, final Runnable dialogBuildProc, final boolean isWaiting) {
		final ProjectContext p_ctx = this;
		// 如果同时发出两个Dialog，则可能不同步，所以以下要wait
		final boolean isForSession = session != null;
		final DialogGlobalLock dialogLock = (DialogGlobalLock) ServerUIAPIAgent
				.runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						final int dialogID = ServerUIAPIAgent.buildDialogID();
						final DialogGlobalLock dialogLock = new DialogGlobalLock(isForSession,
								coreSSS, dialogID, isWaiting);// 每个会话共用

						for (int i = 0; i < coreSSS.length; i++) {
							final J2SESession session = coreSSS[i];
							final DialogParameter dialogParameter = ServerUIAPIAgent
									.buildDialogParameter(session, p_ctx, dialogLock, dialogID);
							ServerUIAPIAgent.sendDialog(dialogParameter, session, dialog,
									dialogBuildProc, p_ctx, dialogLock);
						}
						return dialogLock;
					}
				});
		if (isWaiting) {
			final boolean processed = dialogLock.waitingResult(session);
			if (isForSession) {
				ServerUIUtil.checkLineOnForAPI(session);
			}
			return processed;
		} else {
			return true;
		}
	}

	/**
	 * an error message.
	 * 
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_ALARM
	 * @see #MESSAGE_CONFIRMATION
	 * @see #MESSAGE_INFO
	 * @see #MESSAGE_WARN
	 * @since 7.0
	 */
	public static final int MESSAGE_ERROR = 1;

	/**
	 * a warning message.
	 * 
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_ALARM
	 * @see #MESSAGE_CONFIRMATION
	 * @see #MESSAGE_ERROR
	 * @see #MESSAGE_INFO
	 * @since 7.0
	 */
	public static final int MESSAGE_WARN = 2;

	/**
	 * a information message.
	 * 
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_ALARM
	 * @see #MESSAGE_CONFIRMATION
	 * @see #MESSAGE_ERROR
	 * @see #MESSAGE_WARN
	 * @since 7.0
	 */
	public static final int MESSAGE_INFO = 3;

	/**
	 * an alarm message.
	 * 
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_CONFIRMATION
	 * @see #MESSAGE_ERROR
	 * @see #MESSAGE_INFO
	 * @see #MESSAGE_WARN
	 * @since 7.0
	 */
	public static final int MESSAGE_ALARM = 4;

	/**
	 * a confirmation message.
	 * 
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_ALARM
	 * @see #MESSAGE_ERROR
	 * @see #MESSAGE_INFO
	 * @see #MESSAGE_WARN
	 * @since 7.0
	 */
	public static final int MESSAGE_CONFIRMATION = 5;

	/**
	 * send an alert message to current mobile if in session level, or send the
	 * same alert to all client sessions if in project level. <br>
	 * <br>
	 * if there is an other alert message, question or a <code>Dialog</code> is
	 * presented on client and NOT be closed, the alert message will be delayed.
	 * <BR>
	 * <BR>
	 * Note : if mobile is in background ({@link #isMobileInBackground()}), a
	 * notification is also created for mobile.<BR>
	 * <BR>
	 * if mobile option [Message, Notification to Speech also] is
	 * <STRONG>on</STRONG>, it may be spoken.<BR>
	 * the speech or not is depends on text, TTS engine, locale and mute of
	 * mobile.
	 * 
	 * @param caption
	 *            the caption of message.
	 * @param text
	 *            the body of message.
	 * @param type
	 *            one of {@link #MESSAGE_ERROR}, {@link #MESSAGE_WARN},
	 *            {@link #MESSAGE_INFO}, {@link #MESSAGE_ALARM},
	 *            {@link #MESSAGE_CONFIRMATION}.
	 * @param image
	 *            null if no image, it should be small image, please not big
	 *            image.
	 * @param timeOut
	 *            0:forever;a positive time value in milliseconds
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 6.98
	 */
	public final boolean sendMessage(final String caption, final String text, final int type,
			final BufferedImage image, final int timeOut) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return false;
		}

		final J2SESession[] coreSSS;
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("sendMessage");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			ServerUIAPIAgent.sendMessageViaCoreSSInUserOrSys(coreSSS, caption, text, type, image,
					timeOut);

			processFormData(text);
			return true;
		}

		return false;
	}

	/**
	 * send a AU sound data to current mobile if in session level, or send same
	 * AU sound to all client sessions if in project level. <BR>
	 * <BR>
	 * Important : it is deprecated.
	 * 
	 * @deprecated
	 * @param bs
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 6.98
	 */
	@Deprecated
	public final void sendAUSound(final byte[] bs) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final J2SESession[] coreSSS;
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("sendAUSound");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			try {
				final long length = bs.length;

				final int HEAD = DataPNG.HEAD_LENGTH + MsgBuilder.INDEX_MSG_DATA;
				final byte[] bytes = ByteUtil.byteArrayCacher.getFree((int) length + HEAD);
				System.arraycopy(bs, 0, bytes, HEAD, bs.length);

				final DataPNG blob = new DataPNG();
				blob.bs = bytes;

				blob.setPNGDataLen((int) length, 0, 0, 0, 0);
				ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						for (int i = 0; i < coreSSS.length; i++) {
							coreSSS[i].context.sendWrap(MsgBuilder.E_SOUND, bytes,
									MsgBuilder.INDEX_MSG_DATA, (int) length + DataPNG.HEAD_LENGTH);
						}

						ByteUtil.byteArrayCacher.cycle(bytes);
						// LogManager.log("AU length:" + length);
						return null;
					}
				});
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
	}

	/**
	 * display a tip message on tray of this server. <BR>
	 * <BR>
	 * if current server is Android then show a toast. <BR>
	 * <BR>
	 * it is equals with {@link #showTipOnTray(String)}.
	 * 
	 * @param msg
	 *            a tip message on tray.
	 * @since 6.98
	 */
	public final void tipOnTray(final String msg) {
		showTipOnTray(msg);
	}

	/**
	 * display a tip message on tray of this server. <BR>
	 * <BR>
	 * if current server is Android then show a toast.
	 * 
	 * @param msg
	 *            a tip message on tray.
	 * @since 7.57
	 */
	public final void showTipOnTray(final String msg) {
		ServerUIAPIAgent.tipOnTray(msg);
	}

	/**
	 * in session level, true means mobile is log-in and keep connecting (maybe
	 * in background),<BR>
	 * in project level, true means at least one client session is keep
	 * connection;
	 * 
	 * @return
	 * @see #addSystemEventListener(SystemEventListener)
	 * @see #removeSystemEventListener(SystemEventListener)
	 * @see #isMobileInBackground()
	 * @see #isLineOffThrowable(Throwable)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 * @deprecated
	 */
	@Deprecated
	public final boolean isMobileConnecting() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_CONNECTING;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("isMobileConnecting");
			}
			return SessionManager.checkAtLeastOneMeet(ContextManager.STATUS_SERVER_SELF);
		} else {
			return ContextManager.isMobileLogin(coreSS.context);
		}
	}

	/**
	 * in session level, true means mobile is on relay server (not directly
	 * connect to your server);<BR>
	 * in project level, true means at least one client session is on relay.
	 * <BR>
	 * <BR>
	 * Important : when on relay, the data translated to mobile may be slowly.
	 * 
	 * @return
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.2
	 */
	public final boolean isMobileOnRelay() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_ON_RELAY;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("isMobileOnRelay");
			}
			final CoreSession[] coreSSS = ServerUIAPIAgent.getAllSocketSessionsNoCheck();
			for (int i = 0; i < coreSSS.length; i++) {
				if (coreSSS[i].isOnRelay()) {
					return true;
				}
			}
			return false;
		} else {
			return coreSS.isOnRelay();
		}
	}

	/**
	 * it is equals with {@link #isMobileInBackground()}.
	 * 
	 * @return
	 */
	public final boolean isClientInBackground() {
		return isMobileInBackground();
	}

	/**
	 * in session level, true means mobile is keep connecting and run in
	 * background;<BR>
	 * in project level, true means at least one client session is run in
	 * background.<BR>
	 * <BR>
	 * <STRONG>Important</STRONG> : <BR>
	 * background mode may be NOT supported by mobile. <BR>
	 * application may be killed when turn to background.
	 * 
	 * @return
	 * @see #isMobileLineOn()
	 * @see #addSystemEventListener(SystemEventListener)
	 * @see #EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final boolean isMobileInBackground() {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return SimuMobile.MOBILE_IN_BACKGROUND;
		}

		// TODO 为优化性能，将来mobileagent置于projectContext之中
		// 注意：不能用变量暂存
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				throw new Error(ServerUIAPIAgent.ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
				// LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("isMobileInBackground");
			}
			final CoreSession[] coreSSS = ServerUIAPIAgent.getAllSocketSessionsNoCheck();
			for (int i = 0; i < coreSSS.length; i++) {
				if (UserThreadResourceUtil.getMobileAgent((J2SESession) coreSSS[i])
						.isBackground()) {
					return true;
				}
			}
			return false;
		} else {
			return UserThreadResourceUtil.getMobileAgent(coreSS).isBackground();
		}
	}

	/**
	 * return true if current thread in session level; otherwise in project
	 * level. <BR>
	 * <BR>
	 * session level means current thread is serving for a mobile client
	 * session, NOT for project. <BR>
	 * a server can serve multiple sessions which login with same account at
	 * same time. <BR>
	 * <BR>
	 * most of scripts are for session level to response the user request, but
	 * some will start up before session and keep working after session is
	 * closed, <BR>
	 * for example the IoT, {@link #EVENT_SYS_PROJ_STARTUP} and
	 * {@link #EVENT_SYS_PROJ_SHUTDOWN}. <BR>
	 * some IoT threads maybe run as long as the project, and they are required
	 * to serve for all sessions. <BR>
	 * <BR>
	 * <STRONG>Tip :</STRONG> <BR>
	 * 1. all objects based on session level will gone automatically after
	 * {@link #EVENT_SYS_MOBILE_LOGOUT}, exclude {@link #run(Runnable)} and
	 * {@link #runAndWait(Runnable)}. <BR>
	 * 2. in session level, these following methods serve only for that session;
	 * <BR>
	 * 3. in project level, they serve for all sessions. (in other word, one
	 * command for all sessions) <BR>
	 * <BR>
	 * the difference between session level and project level :
	 * <table border='1'>
	 * <tr>
	 * <th>methods of ProjectContext</th>
	 * <th>session level</th>
	 * <th>project level</th>
	 * </tr>
	 * <tr>
	 * <td>{@link #isMobileLineOn()}</td>
	 * <td>return true, if current client keep connection</td>
	 * <td>return true, at least one client session keep connection</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #isMobileInBackground()}</td>
	 * <td>return true, if current client run in background</td>
	 * <td>return true, at least one client session is run in background</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #isMobileOnRelay()}</td>
	 * <td>return true, if current client is on relay server (not directly
	 * connect to server)</td>
	 * <td>return true, at least one client session is on relay</td>
	 * </tr>
	 * <tr>
	 * <td></td>
	 * <td></td>
	 * <td></td>
	 * </tr>
	 * <tr>
	 * <td>sendMessage</td>
	 * <td>send an alert dialog to current mobile</td>
	 * <td>send the same alert to all client sessions</td>
	 * </tr>
	 * <tr>
	 * <td>sendMovingMsg</td>
	 * <td>send a message moving from right to left for current mobile</td>
	 * <td>send same message to all client sessions</td>
	 * </tr>
	 * <tr>
	 * <td>sendQuestion</td>
	 * <td>send a question to mobile to current mobile</td>
	 * <td>send same question to all client sessions</td>
	 * </tr>
	 * <tr>
	 * <td>sendNotification</td>
	 * <td>send notification to current mobile</td>
	 * <td>send notification to all client session</td>
	 * </tr>
	 * <tr>
	 * <td>sendAUSound</td>
	 * <td>send a AU sound data to current mobile</td>
	 * <td>send same AU sound to all client sessions</td>
	 * </tr>
	 * <tr>
	 * <td>playTone</td>
	 * <td>play tone on current mobile</td>
	 * <td>play tone on all client sessions</td>
	 * </tr>
	 * <tr>
	 * <td>vibrate</td>
	 * <td>vibrate on current mobile</td>
	 * <td>vibrate on all client sessions</td>
	 * </tr>
	 * <tr>
	 * <td>alertOn</td>
	 * <td>set alert on on current mobile</td>
	 * <td>set alert on on all client sessions</td>
	 * </tr>
	 * <tr>
	 * <td>alertOff</td>
	 * <td>set alert off on current mobile</td>
	 * <td>set alert off on all client sessions</td>
	 * </tr>
	 * <tr>
	 * <td></td>
	 * <td></td>
	 * <td></td>
	 * </tr>
	 * <tr>
	 * <td>{@link #getMobileLocale()}</td>
	 * <td>locale of current mobile</td>
	 * <td>the locale for all sessions or 'en-US' in project level</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #getMobileWidth()}</td>
	 * <td>mobile width of current session</td>
	 * <td>0 if mobile not login or in project level</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #getMobileHeight()}</td>
	 * <td>mobile height of current session</td>
	 * <td>0 if mobile not login or in project level</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #getMobileOSVer()}</td>
	 * <td>version of mobile OS of current session</td>
	 * <td>"0.0.1" if mobile not login or in project level</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #getMobileOS()}</td>
	 * <td>mobile OS of current session</td>
	 * <td>empty string if mobile not login or in project level</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #getSoftUID()}</td>
	 * <td>mobile Soft UID of current session</td>
	 * <td>empty string if mobile not login or in project level</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #getMobileDPI()}</td>
	 * <td>mobile DPI of current session</td>
	 * <td>-1 if mobile not login or in project level.</td>
	 * </tr>
	 * </table>
	 * <br>
	 * the following scripts are executed in project level :
	 * <UL>
	 * <LI><code>EVENT_SYS_PROJ_STARTUP</code></LI>
	 * <LI>scripts in IoT, such as <code>Robot</code>, <code>Converter</code>,
	 * <code>Device</code></LI>
	 * <LI>the procedure of
	 * {@link RobotListener#action(hc.server.msb.RobotEvent)} if it is added in
	 * project level, <BR>
	 * if it is added in <code>CtrlResponse</code> or <code>HTMLMlet</code>,
	 * then the procedure is in session level</LI>
	 * <LI>the new thread runs in project level if it is builded in project
	 * level</LI>
	 * <LI><code>runnable</code> runs in project level if {@link #run(Runnable)}
	 * is in project level</LI>
	 * <LI><code>runnable</code> runs in project level if
	 * {@link #runAndWait(Runnable)} is in project level</LI>
	 * <LI><code>EVENT_SYS_PROJ_SHUTDOWN</code></LI>
	 * </UL>
	 * <BR>
	 * the following scripts are executed in session level :
	 * <UL>
	 * <LI><code>EVENT_SYS_MOBILE_LOGIN</code></LI>
	 * <LI>menu item scripts, for examples Command,
	 * <code>Mlet</code>/<code>HTMLMlet</code> and
	 * <code>CtrlResponse</code></LI>
	 * <LI>the procedure of {@link SystemEventListener#onEvent(String)}
	 * triggered by session events (<STRONG>NOT</STRONG> project event
	 * EVENT_SYS_PROJ_SHUTDOWN), even if it is added in project level</LI>
	 * <LI>the procedure of
	 * {@link RobotListener#action(hc.server.msb.RobotEvent)} if the
	 * RobotListener is added in session level</LI>
	 * <LI>the procedure of {@link Robot#operate(long, Object)} to execute
	 * command from user</LI>
	 * <LI>the new thread runs in session level if it is builded in session
	 * level</LI>
	 * <LI><code>runnable</code> runs in session level if {@link #run(Runnable)}
	 * is in session level</LI>
	 * <LI><code>runnable</code> runs in session level if
	 * {@link #runAndWait(Runnable)} is in session level</LI>
	 * <LI><code>EVENT_SYS_MOBILE_LOGOUT</code></LI>
	 * </UL>
	 * 
	 * @return
	 * @since 7.20
	 */
	public final static boolean isCurrentThreadInSessionLevel() {
		final ProjectContext ctx = getProjectContext();

		if (ctx.__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(ctx)) {
			return SimuMobile.CURR_IN_SESSION;
		}

		final SessionContext sessionContext = ctx.__projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * send a voice to client/mobile.<BR>
	 * <BR>
	 * if you want show this voice as a moving message and speech again, please
	 * use {@link #sendMovingMsg(String)}. <BR>
	 * <BR>
	 * 
	 * @param voice
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.63
	 */
	public final void sendVoice(final String voice) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final J2SESession[] coreSSS;
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("sendVoice");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			ServerUIAPIAgent.sendVoice(coreSSS, voice);

			processFormData(voice);
		}
	}

	/**
	 * send a message moving from right to left for current mobile if in session
	 * level, or send same message to all client sessions if in project
	 * level.<BR>
	 * <BR>
	 * Note : if mobile is in background ({@link #isMobileInBackground()}), a
	 * notification is also created for mobile.<BR>
	 * <BR>
	 * if mobile option [Message, Notification to Speech also] is
	 * <STRONG>on</STRONG>, it may be spoken.<BR>
	 * the speech or not is depends on text, TTS engine, locale and mute of
	 * mobile. <BR>
	 * <BR>
	 * if the message is too long, {@link #sendVoice(String)} is a good choice.
	 * 
	 * @param msg
	 *            the message to show.
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 6.98
	 */
	public final void sendMovingMsg(final String msg) {
		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final J2SESession[] coreSSS;
		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.warning(ServerUIAPIAgent.CURRENT_THREAD_IS_IN_PROJECT_LEVEL);
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("sendMovingMsg");
			}
			coreSSS = ServerUIAPIAgent.getAllOnlineSocketSessionsNoCheck();
		} else {
			ServerUIUtil.checkLineOnForAPI(coreSS);
			coreSSS = toArray(coreSS);
		}

		if (coreSSS != null && coreSSS.length > 0) {
			ServerUIAPIAgent.sendMovingMsg(coreSSS, msg);

			processFormData(msg);
		}
	}

	private final void processFormData(final String msg) {
		if (AIPersistentManager.isEnableAnalyseFlow && AIPersistentManager.isEnableHCAI()) {
			final FormData data = AIObjectCache.getFormData();
			data.uiType = FormData.UI_TYPE_PROJECT;
			data.uiObject = AnalysableData.NON_UI_FOR_PROJECT_ONLY;
			data.movingMsg = msg;
			data.snap(projectID, getClientLocale(), AnalysableData.DIRECT_OUT);
			AIPersistentManager.processFormData(data);
		}
	}

	/**
	 * Removes the specified system event listener. <BR>
	 * <BR>
	 * the listener added in session level will gone automatically after
	 * {@link #EVENT_SYS_MOBILE_LOGOUT}.
	 * 
	 * @param listener
	 *            If listener is null, no exception is thrown and no action is
	 *            performed.
	 * @return true if the projectContext contained the specified listener
	 * @see #addSystemEventListener(SystemEventListener)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final boolean removeSystemEventListener(final SystemEventListener listener) {
		if (listener == null) {
			return false;
		}

		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return false;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.log("[workshop] remove ProjectLevel SystemEventListener.");
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("removeSystemEventListener");
			}
			return projectLevelEventListeners.remove(listener);
		} else {
			if (L.isInWorkshop) {
				LogManager.log("[workshop] remove SessionLevel SystemEventListener.");
			}
			return coreSS.sessionLevelEventListeners.remove(listener);
		}
	}

	/**
	 * add the specified system event listener. <br>
	 * <br>
	 * If current thread is for session, then the listener is added in session
	 * level, otherwise it is project level. <BR>
	 * the listener in session level will gone automatically after
	 * {@link #EVENT_SYS_MOBILE_LOGOUT}. <BR>
	 * <BR>
	 * the procedure of {@link SystemEventListener#onEvent(String)} is in
	 * session level if triggered by session events (for example
	 * EVENT_SYS_MOBILE_LOGIN), even if it is added in project level. <BR>
	 * <BR>
	 * these following events may be listened:
	 * <ul>
	 * <li>{@link #EVENT_SYS_PROJ_STARTUP},</li>
	 * <li>{@link #EVENT_SYS_MOBILE_LOGIN},</li>
	 * <li>{@link #EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND},</li>
	 * <li>{@link #EVENT_SYS_MOBILE_LOGOUT},</li>
	 * <li>{@link #EVENT_SYS_PROJ_SHUTDOWN},</li>
	 * <li>or other events in the future.</li>
	 * </ul>
	 * 
	 * @param listener
	 *            the listener to be added.
	 * @see #removeSystemEventListener(SystemEventListener)
	 * @see #isCurrentThreadInSessionLevel()
	 * @since 7.0
	 */
	public final void addSystemEventListener(final SystemEventListener listener) {
		if (listener == null) {
			return;
		}

		if (__projResponserMaybeNull == null || SimuMobile.checkSimuProjectContext(this)) {
			return;
		}

		final SessionContext sessionContext = __projResponserMaybeNull
				.getSessionContextFromCurrThread();
		J2SESession coreSS;
		if (sessionContext == null || (coreSS = sessionContext.j2seSocketSession) == null) {
			if (L.isInWorkshop) {
				LogManager.log("[workshop] add ProjectLevel SystemEventListener.");
			}
			if (isLoggerOn == false) {
				ServerUIAPIAgent.printInProjectLevelWarn("addSystemEventListener");
			}
			projectLevelEventListeners.add(listener);
		} else {
			if (L.isInWorkshop) {
				LogManager.log("[workshop] add SessionLevel SystemEventListener.");
			}
			coreSS.sessionLevelEventListeners.add(listener);
		}
	}

	/**
	 * get temporary file in <code>TEMP</code> directory (managed by server and
	 * will be empty at next startup). <BR>
	 * <BR>
	 * it is equals with {@link #createTempFile(String)}.
	 * 
	 * @param fileExtension
	 *            null means "tmp" will be used
	 * @return
	 * @see #getPrivateFile(String)
	 * @see #createTempFile(String)
	 */
	public final File getTempFile(final String fileExtension) {
		return createTempFile(fileExtension);
	}

	/**
	 * get temporary file. <BR>
	 * <BR>
	 * it is equals with {@link #createTempFile(File, String)}.
	 * 
	 * @param parent
	 *            null means create random file in <code>TEMP</code> directory
	 *            (managed by server and will be empty at next startup).
	 * @param fileExtension
	 *            null means "tmp" will be used
	 * @return
	 */
	public final File getTempFile(final File parent, final String fileExtension) {
		return createTempFile(parent, fileExtension);
	}

	/**
	 * create temporary file in <code>TEMP</code> directory (managed by server
	 * and will be empty at next startup). <BR>
	 * <BR>
	 * it is equals with {@link #getTempFile(String)}.
	 * 
	 * @param fileExtension
	 *            null means "tmp" will be used
	 * @return
	 * @see #getPrivateFile(String)
	 */
	public final File createTempFile(final String fileExtension) {
		return ResourceUtil.createTempFileForHAR(this, null, fileExtension);
	}

	/**
	 * create temporary file. <BR>
	 * <BR>
	 * it is equals with {@link #getTempFile(File, String)}.
	 * 
	 * @param parent
	 *            null means create random file in <code>TEMP</code> directory
	 *            (managed by server and will be empty at next startup).
	 * @param fileExtension
	 *            null means "tmp" will be used
	 * @return
	 */
	public final File createTempFile(final File parent, final String fileExtension) {
		return ResourceUtil.createTempFileForHAR(this, parent, fileExtension);
	}

	/**
	 * a notification with vibrate
	 * 
	 * @since 7.0
	 */
	public static final int FLAG_NOTIFICATION_VIBRATE = ConfigManager.FLAG_NOTIFICATION_VIBRATE;

	/**
	 * a notification with sound
	 * 
	 * @since 7.0
	 */
	public static final int FLAG_NOTIFICATION_SOUND = ConfigManager.FLAG_NOTIFICATION_SOUND;

	/**
	 * server shutdown this project
	 * 
	 * @since 7.0
	 */
	public static final String EVENT_SYS_PROJ_SHUTDOWN = "SYS_PROJ_SHUTDOWN";

	/**
	 * server start up this project
	 * 
	 * @since 7.0
	 */
	public static final String EVENT_SYS_PROJ_STARTUP = "SYS_PROJ_STARTUP";

	/**
	 * when mobile client is successful login.
	 * 
	 * @since 7.0
	 */
	public static final String EVENT_SYS_MOBILE_LOGIN = "SYS_MOBILE_LOGIN";

	/**
	 * when mobile location is changed or initialized.
	 * 
	 * @see ClientSession#getLocationLatitude()
	 * @see ClientSession#getLocationLongitude()
	 * @since 7.63
	 */
	public static final String EVENT_SYS_MOBILE_LOCATION = "SYS_MOBILE_LOCATION";

	/**
	 * when mobile client is logout or line off
	 * 
	 * @since 7.0
	 */
	public static final String EVENT_SYS_MOBILE_LOGOUT = "SYS_MOBILE_LOGOUT";

	/**
	 * this event will be triggered when mobile enter pause (background) or
	 * resume.<BR>
	 * invoke {@link #isMobileInBackground()} to check whether is background
	 * (onPause) or not (onResume). <BR>
	 * <BR>
	 * Note :<BR>
	 * 1. server will trigger this event ({@link #isMobileInBackground()}
	 * returns false) <STRONG>after</STRONG> {@link #EVENT_SYS_MOBILE_LOGIN}.
	 * 
	 * @since 7.0
	 */
	public static final String EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND = "SYS_MOBILE_BACKGROUND_OR_FOREGROUND";
}
