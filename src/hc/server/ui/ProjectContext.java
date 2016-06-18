package hc.server.ui;

import hc.App;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.data.DataPNG;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.Stack;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPool;
import hc.server.PlatformManager;
import hc.server.PlatformService;
import hc.server.StarterManager;
import hc.server.data.screen.KeyComper;
import hc.server.msb.Converter;
import hc.server.msb.Device;
import hc.server.msb.DeviceCompatibleDescription;
import hc.server.msb.MSBAgent;
import hc.server.msb.Message;
import hc.server.msb.Robot;
import hc.server.msb.Workbench;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.SystemEventListener;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.PropertiesMap;
import hc.util.ResourceUtil;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JOptionPane;

/**
 * There is only a {@link ProjectContext} instance for each HAR Project at
 * runtime. <BR>
 * if server shutdown and restart HAR projects (NOT restart server), new
 * {@link ProjectContext} instance is created. <br>
 * <br>
 * to get instance of ProjectContext : <br>
 * 1. invoke {@link ProjectContext#getProjectContext()} <br>
 * 2. invoke {@link Mlet#getProjectContext()} <br>
 * 3. invoke {@link CtrlResponse#getProjectContext()} <br>
 * 4. invoke {@link Robot#getProjectContext()} <br>
 * 5. invoke {@link Converter#getProjectContext()} <br>
 * 6. invoke {@link Device#getProjectContext()}
 * 
 * @since 7.0
 */
public class ProjectContext {
	private final String projectID;
	private final String projectVer;
	final ThreadPool threadPool;
	final LinkedSet systemEventStack = new LinkedSet();

	/**
	 * @deprecated
	 */
	@Deprecated
	Stack mletHistoryUrl;

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
			return PlatformManager.getService().get3rdClassLoader(null);
		} else if (level == CLASSLOADER_PROJECT_LEVEL) {
			return finder.findProjClassLoader();
		} else {
			throw new Error(
					"unknow classloader level. it shoud be one of "
							+ "CLASSLOADER_SYSTEM_LEVEL, CLASSLOADER_SERVER_LEVEL, CLASSLOADER_PROJECT_LEVEL");
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	String __tmp_url;
	/**
	 * @deprecated
	 */
	@Deprecated
	String __tmp_elementID;

	/**
	 * @deprecated
	 */
	@Deprecated
	ProjResponser __projResponserMaybeNull;

	private final Hashtable<String, Object> attribute_map = new Hashtable<String, Object>();
	private final ProjClassLoaderFinder finder;

	/**
	 * @deprecated
	 */
	@Deprecated
	public ProjectContext(final String id, final String ver,
			final ThreadPool pool, final ProjResponser projResponser,
			final ProjClassLoaderFinder finder) {
		projectID = id;
		projectVer = ver;
		this.threadPool = pool;
		this.__projResponserMaybeNull = projResponser;// 由于应用复杂，可能为null
		this.finder = finder;
	}

	/**
	 * @return the version of HomeCenter server.
	 * @since 7.0
	 */
	public final String getHomeCenterVersion() {
		return StarterManager.getHCVersion();
	}
	
	/**
	 * @return the version of JRE of standard JVM of Oracle or implementation of AWT/Swing for Android server.
	 * @since 7.5
	 */
	public final String getJREVersion() {
		return String.valueOf(App.getJREVer());
	}

	/**
	 * log an error message to log system.
	 * 
	 * @param logMessage
	 * @since 7.0
	 */
	public final void error(final String logMessage) {
		LogManager.errToLog(logMessage);
	}

	/**
	 * For example, in order to connect device, user may be required to input
	 * token of devices. <br>
	 * Call the method to input token on this server. <br>
	 * <br>
	 * to save token, see {@link #setProperty(String, String)} and
	 * {@link #saveProperties()}
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
	 */
	public static final String[] showInputDialog(final String title,
			final String[] fieldNames, final String[] fieldDescs) {
		return App.showHARInputDialog(appendProjectIDForTitle(title),
				fieldNames, fieldDescs);
	}
	
	private static final String appendProjectIDForTitle(final String title) {
		final String partOne = " in Project [";
		final String partTwo = "]";

		final String projID = getProjectContext().getProjectID();
		final StringBuilder sb = new StringBuilder(title.length()
				+ partOne.length() + projID.length() + partTwo.length());

		sb.append(title);
		sb.append(partOne);
		sb.append(projID);
		sb.append(partTwo);

		return sb.toString();
	}

	/**
	 * pop up an information message dialog on this server. <br>
	 * if the <code>message</code> is displayed and not closed, the same message
	 * is call also, then it will NOT be showed. <br>
	 * <br>
	 * <STRONG>Important : </STRONG>the current thread will NOT be blocked.
	 * 
	 * @param message
	 *            the body of message.
	 * @since 7.0
	 */
	public static final void showMessageDialog(final String message) {
		App.showHARMessageDialog(message);
	}

	/**
	 * pop up a dialog to display a message on this server. <br>
	 * if the <code>message</code> is displayed and not closed, the same message
	 * is call also, then it will NOT be showed. <br>
	 * <br>
	 * <STRONG>Important : </STRONG>the current thread will NOT be blocked.
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
	public static final void showMessageDialog(final String message,
			final String title, final int messageType) {
		App.showHARMessageDialog(message, appendProjectIDForTitle(title),
				messageType);
	}

	/**
	 * pop up a dialog to display a message on this server. <br>
	 * if the <code>message</code> is displayed and not closed, the same message
	 * is call also, then it will NOT be showed. <br>
	 * <br>
	 * <STRONG>Important : </STRONG>the current thread will NOT be blocked.
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
	public final static void showMessageDialog(final String message,
			final String title, final int messageType, final Icon icon) {
		App.showHARMessageDialog(message, appendProjectIDForTitle(title),
				messageType, icon);
	}

	/**
	 * get the {@link Robot} instance by name in current HAR Project by name.
	 * 
	 * @param name
	 *            the name of robot.
	 * @return null if the Robot is not found.
	 * @since 7.0
	 */
	public final Robot getRobot(final String name) {
		if (__projResponserMaybeNull == null) {
			LogManager.err("In designer panel, create simu robot for testing script.");
			return new Robot() {
				@Override
				public void startup() {
				}
				
				@Override
				public void shutdown() {
				}
				
				@Override
				public void response(final Message msg) {
				}
				
				@Override
				public Object operate(final long functionID, final Object parameter) {
					LogManager.err("In designer panel, create simu result object (empty string) for method [operate] of simu Robot.");
					return "";
				}
				
				@Override
				public DeviceCompatibleDescription getDeviceCompatibleDescription(
						final String referenceDeviceID) {
					return null;
				}
				
				@Override
				public String[] declareReferenceDeviceID() {
					return null;
				}
			};
		}

		Robot[] robots = null;
		try {
			robots = __projResponserMaybeNull.getRobots();
		} catch (final Exception e) {
		}

		if (robots != null) {
			for (int j = 0; j < robots.length; j++) {
				final Robot robot = robots[j];
				if (name.equals(MSBAgent.getName(robot))) {
					return robot;
				}
			}
		}
		LogManager.errToLog("no Robot [" + name + "] in project [" + projectID + "].");
		return null;
	}

	/**
	 * run a shell of JRuby script, and return a object if with
	 * <code>return</code> command. <BR>
	 * <BR>
	 * for example, a scripts with four lines, (<STRONG>'\n'</STRONG> is
	 * <STRONG>required</STRONG> for each line at end) : <BR>
	 * <i>
	 * <code>$g = 100 # g is global variable.<BR> i = 2 + $g # i is local variable<BR>puts i<BR> return i # 102<BR></code>
	 * </i> <BR>
	 * <STRONG>More about JRuby engine :</STRONG> <BR>
	 * the container for running JRuby script of current project is instanced as
	 * following:<BR>
	 * <i>
	 * <code>new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.TRANSIENT, true);</code>
	 * </i>
	 * 
	 * @param shell
	 *            the evaluate shell scripts.
	 * @return a object if with <code>return</code> command.
	 * @since 7.0
	 */
	public final Object eval(final String shell) {
		// synchronized (this) {
		final HCJRubyEngine engine = (__projResponserMaybeNull == null ? ScriptEditPanel.runTestEngine
				: __projResponserMaybeNull.hcje);
		try {
			
			if(engine.isError){
				engine.errorWriter.reset();
				engine.isError = false;
			}
			
			return engine.runScriptlet(shell, "evalInProjectContext");
		} catch (final Throwable e) {
			final String err = engine.errorWriter.getMessage();
			ExceptionReporter.printStackTraceFromHAR(e, shell, err);
		}
		return null;
		// }
	}

	/**
	 * start a {@link Runnable} task asynchronous in current HAR project thread
	 * pool. <br>
	 * the runnable task should be finished when
	 * {@link #EVENT_SYS_PROJ_SHUTDOWN}.
	 * 
	 * @param runnable
	 * @see #runAndWait(Runnable)
	 * @see #addSystemEventListener(SystemEventListener)
	 * @since 7.0
	 */
	public final void run(final Runnable runnable) {
		threadPool.run(runnable);
	}

	/**
	 * start a {@link Runnable} task and wait for done in current HAR project
	 * thread pool. <br>
	 * the runnable task should be finished when
	 * {@link #EVENT_SYS_PROJ_SHUTDOWN}.
	 * 
	 * @param runnable
	 * @see #run(Runnable)
	 * @see #addSystemEventListener(SystemEventListener)
	 * @since 7.0
	 */
	public final void runAndWait(final Runnable runnable) {
		threadPool.runAndWait(new ReturnableRunnable() {
			@Override
			public final Object run() {
				runnable.run();
				return null;
			}
		});
	}

	/**
	 * print all thread stack of JVM (not only the current project) to log
	 * system.
	 * 
	 * @since 7.0
	 */
	public final static void printAllThreadStack() {
		App.printThreadStackForProjectContext(null);
	}

	/**
	 * @return the version of current HAR project.
	 * @since 7.0
	 */
	public final String getProjectVersion() {
		return projectVer;
	}

	/**
	 * send a question to mobile. <br>
	 * <br>
	 * this method is <STRONG>asynchronous</STRONG>. system will NOT wait for
	 * the result of question to the caller. <BR>
	 * <BR>
	 * Note : if mobile is in background ({@link #isMobileInBackground()}), a
	 * notification is also created for mobile.
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
	 *            the runnable will be executed if choosing 'CANCEL'. Set null
	 *            to not display this button.
	 * @since 7.0
	 */
	public final void sendQuestion(String caption, String text,
			final BufferedImage image, final Runnable yesRunnable,
			final Runnable noRunnable, final Runnable cancelRunnable) {
		if (caption == null) {
			caption = (String) ResourceUtil.get(IContext.INFO);
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
				return;
			}
		}

		final QuestionParameter question = ServerUIAPIAgent.buildQuestionID(
				this, yesRunnable, noRunnable, cancelRunnable);

		final String[] para = { HCURL.DATA_PARA_QUESTION_ID, "caption", "text",
				"image", "withCancel" };
		final String[] values = { String.valueOf(question.questionID), caption,
				text, imageData, (cancelRunnable != null) ? "1" : "0" };//注意：必须在外部转换

		//如果同时发出两个Ques，则可能不同步，所以以下要wait
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, para, values);
				return null;
			}
		});

	}

	/**
	 * 
	 * @return current project ID.
	 * @since 7.0
	 */
	public final String getProjectID() {
		return projectID;
	}

	/**
	 * @return the width pixel of login mobile
	 * @since 7.0
	 */
	public final int getMobileWidth() {
		return ClientDesc.getClientWidth();
	}

	/**
	 * set an object to a given attribute name in this context. <BR>
	 * It is thread safe. <BR>
	 * <BR>
	 * <STRONG>Note : </STRONG>you can't save attribute to persistent system.
	 * But you can save properties of current project to persistent system.
	 * 
	 * @see #removeAttribute(String)
	 * @see #clearAttribute(String)
	 * @see #getAttribute(String)
	 * @see #getAttributeNames()
	 * @param name
	 *            the name of attribute.
	 * @param obj
	 *            the value of the attribute.
	 * @since 7.0
	 */
	public final void setAttribute(final String name, final Object obj) {
		if (name.startsWith(Workbench.SYS_RESERVED_KEYS_START, 0)) {
			throw new Error("the name of attribute can't start with '"
					+ Workbench.SYS_RESERVED_KEYS_START
					+ "', it is reserved by system.");
		}
		
		__setAttributeSuper(name, obj);
	}
	
	/**
	 * @deprecated
	 * @param key
	 * @return the attribute of name.
	 */
	@Deprecated
	final void __setAttributeSuper(final String name, final Object obj) {
//		synchronized (attribute_map) {
			attribute_map.put(name, obj);
//		}
	}
	
	/**
	 * @deprecated
	 * @param name
	 */
	@Deprecated
	final void __removeAttributeSuper(final String name) {
//		synchronized (attribute_map) {
			attribute_map.remove(name);
//		}
	}

	/**
	 * removes the attribute with the given name from the context. <BR>
	 * It is thread safe.
	 * 
	 * @see #setAttribute(String, Object)
	 * @see #getAttribute(String)
	 * @see #getAttributeNames()
	 * @see #clearAttribute(String)
	 * @param name
	 */
	public final void removeAttribute(final String name) {
		if (name.startsWith(Workbench.SYS_RESERVED_KEYS_START, 0)) {
			throw new Error("the name of attribute can't start with '"
					+ Workbench.SYS_RESERVED_KEYS_START + "'");
		}
		
		__removeAttributeSuper(name);
	}

	/**
	 * It is equals with {@link #removeAttribute(String)}
	 * 
	 * @param name
	 * @since 7.0
	 */
	public final void clearAttribute(final String name) {
		removeAttribute(name);
	}

	/**
	 * returns the attribute with the given name, or null if there is no
	 * attribute by that name. <BR>
	 * It is thread safe.
	 * 
	 * @see #getAttributeNames()
	 * @see #setAttribute(String, Object)
	 * @see #removeAttribute(String)
	 * @see #clearAttribute(String)
	 * @param name
	 * @return the attribute with the <code>name</code>.
	 * @since 6.98
	 */
	public final Object getAttribute(final String name) {
		if (name.startsWith(Workbench.SYS_RESERVED_KEYS_START, 0)) {
			throw new Error("the name of attribute can't start with '"
					+ Workbench.SYS_RESERVED_KEYS_START + "'");
		}
		
		return __getAttributeSuper(name);
	}
	
	/**
	 * @deprecated
	 * @param key
	 * @return the attribute of name.
	 */
	@Deprecated
	final Object __getAttributeSuper(final String name) {
//		synchronized (attribute_map) {
			return attribute_map.get(name);
//		}
	}

	/**
	 * returns the attribute with the given name, or <code>defaultValue</code>
	 * if there is no attribute by that name.
	 * 
	 * @param name
	 * @param defaultValue
	 *            the default value for name.
	 * @return <code>defaultValue</code> if this map contains no attribute for
	 *         the name
	 * @since 7.0
	 */
	public final Object getAttribute(final String name,
			final Object defaultValue) {
		final Object value = getAttribute(name);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}
	
	/**
	 * return null before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN} or after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}
	 * @return
	 */
	public final ClientSession getClientSession(){
		ClientSession out = null;
		if(__projResponserMaybeNull != null){
			out = __projResponserMaybeNull.getClientSession();
		}
		
		if(out == null){
			LogManager.warning("In designer panel, create simu " + ClientSession.class.getName() + ".");
			return new ClientSession();
		}
		
		return out;
	}

	/**
	 * returns an enumeration containing the attribute names available within
	 * this context.
	 * 
	 * @see #getAttribute(String)
	 * @see #setAttribute(String, Object)
	 * @see #removeAttribute(String)
	 * @return the enumeration of all attribute names.
	 * @since 6.98
	 */
	public final Enumeration getAttributeNames() {
		final HashSet<String> set = new HashSet<String>();
		synchronized (attribute_map) {
			final Enumeration<String> en = attribute_map.keys();
			while (en.hasMoreElements()) {
				final String item = en.nextElement();
				if (item.startsWith(Workbench.SYS_RESERVED_KEYS_START, 0)) {
					continue;
				} else {
					set.add(item);
				}
			}
		}

		final Iterator<String> setit = set.iterator();
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
	 * @return the height pixel of login mobile
	 * @since 7.0
	 */
	public final int getMobileHeight() {
		return ClientDesc.getClientHeight();
	}

	/**
	 * in Android, it means densityDpi;
	 * 
	 * @return the DPI of login mobile, if 0 means unknown.
	 * @since 7.0
	 */
	public final int getMobileDPI() {
		return ClientDesc.getDPI();
	}

	/**
	 * set a property with new value for the current project. <BR>
	 * It is thread safe. <br>
	 * <code>{@link #saveProperties()}</code> is required to save properties to
	 * persistent system.
	 * 
	 * @see #getProperty(String)
	 * @see #removeProperty(String)
	 * @see #clearProperty(String)
	 * @see #saveProperties()
	 * @param key
	 *            it can't start with reserved string '_HC_SYS'.
	 * @param value
	 *            the value of the property.
	 * @since 7.0
	 */
	public final void setProperty(final String key, final String value) {
		if (key.startsWith(Workbench.SYS_RESERVED_KEYS_START, 0)) {
			throw new Error("the key of property can't start with '"
					+ Workbench.SYS_RESERVED_KEYS_START
					+ "', it is reserved by system.");
		}

		__setPropertySuper(key, value);
	}

	/**
	 * returns an enumeration containing the properties names available within
	 * this context.
	 * 
	 * @since 7.0
	 * @return an enumeration of all properties names.
	 */
	public final synchronized Enumeration getPropertiesNames() {
		if (prop_map == null) {
			init();
		}

		final Iterator<String> it = prop_map.keySet().iterator();
		final HashSet<String> set = new HashSet<String>();
		while (it.hasNext()) {
			final String item = it.next();
			if (item.startsWith(Workbench.SYS_RESERVED_KEYS_START, 0)) {
				continue;
			} else {
				set.add(item);
			}
		}

		final Iterator<String> setit = set.iterator();
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
	 * print stack trace to logger and report it to project provider explicitly.
	 * <BR><BR><STRONG>Important : </STRONG>
	 * <UL>
	 * <LI>
	 *  enable option->developer-><STRONG>report exception</STRONG> is required.
	 * </LI>
	 * <LI>
	 * if there is an error in script and is caught by server, NOT by project, it will be reported project provider implicitly.</LI>
	 * <LI>if field <STRONG>Report Exception URL</STRONG> of project is blank, then reporting is DISABLED and print stack trace only.</LI>
	 * <LI>it is NOT required to apply for the permission of connection for current project.</LI>
	 * <LI>it reports stack trace for project on server, NOT for mobile.</LI>
	 * </UL>
	 * <BR>sample code for PHP to receive exception:<BR>
	 * <BR><code>if ('POST' == $_SERVER['REQUEST_METHOD']){//POST to server<BR>
	 * &nbsp;&nbsp;$exception = json_decode(file_get_contents('php://input'), true);//JSON object is UTF-8<BR>
	 * &nbsp;&nbsp;//print_r($exception);//all fields are string even if tag_structureVersion.<BR>
	 * <BR>
	 * &nbsp;&nbsp;//echo $exception['tag_structureVersion'];<BR>
	 * &nbsp;&nbsp;//tag_structureVersion is used to check the structure version of description. if new field is added, it will be upgraded.<BR>
	 * }</code>
	 * @param throwable
	 * @since 7.4
	 */
	public static final void printAndReportStackTrace(final Throwable throwable){
		ExceptionReporter.printStackTrace(throwable, null, null, ExceptionReporter.INVOKE_HAR);
	}

	/**
	 * @deprecated
	 * @param key
	 * @param value
	 */
	@Deprecated
	final synchronized void __setPropertySuper(final String key,
			final String value) {
		if (prop_map == null) {
			init();
		}
		prop_map.put(key, value);
	}

	private final void init() {
		prop_map = new PropertiesMap(PropertiesManager.p_PROJ_RECORD
				+ getProjectID());
	}

	/**
	 * get the attribute with the given key from the current project. <BR>
	 * It is thread safe.
	 * 
	 * @param key
	 * @return null if is not set or saved before.
	 * @see #setProperty(String, String)
	 * @see #removeProperty(String)
	 * @see #getPropertiesNames()
	 * @see #clearProperty(String)
	 * @see #saveProperties()
	 * @since 7.0
	 */
	public final String getProperty(final String key) {
		if (key.startsWith(Workbench.SYS_RESERVED_KEYS_START, 0)) {
			throw new Error("the key of property can't start with '"
					+ Workbench.SYS_RESERVED_KEYS_START + "'");
		}

		return __getPropertySuper(key);
	}

	/**
	 * get the attribute with the given key from the current project, return
	 * <code>defaultValue</code> if the key is not set before.
	 * 
	 * @param key
	 * @param defaultValue
	 *            the default value for key.
	 * @return <code>defaultValue</code> if this map contains no property for
	 *         the key
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
	final synchronized String __getPropertySuper(final String key) {
		if (prop_map == null) {
			init();
		}
		return prop_map.get(key);
	}

	/**
	 * remove the property with the given name from the current project.
	 * 
	 * @see #getProperty(String)
	 * @see #setProperty(String, String)
	 * @see #clearProperty(String)
	 * @see #saveProperties()
	 * @param key
	 */
	public final void removeProperty(final String key) {
		if (key.startsWith(Workbench.SYS_RESERVED_KEYS_START, 0)) {
			throw new Error("the key of property can't start with '"
					+ Workbench.SYS_RESERVED_KEYS_START + "'");
		}

		__removePropertySuper(key);
	}

	/**
	 * It is equals with {@link #removeProperty(String)}
	 * 
	 * @param key
	 * @since 7.0
	 */
	public final void clearProperty(final String key) {
		removeProperty(key);
	}

	/**
	 * @deprecated
	 * @param key
	 */
	@Deprecated
	final synchronized void __removePropertySuper(final String key) {
		if (prop_map == null) {
			init();
		}
		prop_map.remove(key);
	}

	/**
	 * to read and write file to local disk, please use this method to get a
	 * instance of {@link File}. <BR>
	 * you can't use <code>new File("filename")</code> to creating local file
	 * directly, and it may be forbidden in future.
	 * 
	 * <br>
	 * <br>
	 * 1. it is allow full access and NOT limited by HomeCenter security
	 * manager, and it is also protected from being read or written by other HAR
	 * projects. <br>
	 * 2. the File <code>fileName</code> is read/written in directory
	 * <i>{HC_Home}/user_data/{projectID}/{fileName}</i>. <br>
	 * 3. to create private sub directory, just call
	 * <code>getPrivateFile("mySubDir").mkdirs();</code> <br>
	 * 4. to create private file <code>subFile</code> in directory
	 * <code>mySubDir</code>, just call
	 * <code>new File(getPrivateFile("mySubDir"), "subFile");</code> <br>
	 * 5. if current project is deleted, all private files of current project
	 * will be deleted. <BR>
	 * <BR>
	 * If you want to save small data, please see and use
	 * {@link #saveProperties()}.
	 * 
	 * @param fileName
	 * @return the private file instance.
	 * @see #saveProperties()
	 * @since 7.0
	 */
	public final File getPrivateFile(final String fileName) {
		final String absProjBasePath = HCLimitSecurityManager.getUserDataBaseDir(projectID);
		final File userDir = new File(absProjBasePath);//不能使用App.getBaseDir

		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				if (userDir.exists() == false) {
					userDir.mkdirs();
				}
				return null;
			}
		});

		final String absPathname = absProjBasePath + HttpUtil.encodeFileName(fileName);
		return new File(absPathname);//App.getBaseDir
	}

	/**
	 * save properties of current project to persistent system. <br>
	 * <br>
	 * <STRONG>Important :</STRONG> <br>
	 * 1. data of properties will be deleted if current project is removed. <br>
	 * 2. it is a good practice to save small data in properties system and save
	 * big data in cloud system / local disk. <br>
	 * 3. if you want to create file on local disk, please use
	 * {@link #getPrivateFile(String)} to get instance of File.
	 * 
	 * @see #setProperty(String, String)
	 * @see #getProperty(String)
	 * @see #removeProperty(String)
	 * @since 7.0
	 */
	public final void saveProperties() {
		if (prop_map == null) {
			return;
		}

		prop_map.save();
	}

	private PropertiesMap prop_map;

	/**
	 * if your mobile is Android, it will return this after call
	 * {@link #getMobileOS()}.
	 * 
	 * @since 7.0
	 */
	public static final String OS_ANDROID = ConfigManager.OS_ANDROID_DESC;
	/**
	 * if your mobile is iOS, it will return this after call
	 * {@link #getMobileOS()}.
	 * 
	 * @since 7.0
	 */
	public static final String OS_IOS = ConfigManager.OS_IOS_DESC;
	/**
	 * if your mobile is J2ME, it will return this after call
	 * {@link #getMobileOS()}.
	 * 
	 * @since 7.0
	 */
	public static final String OS_J2ME = ConfigManager.OS_J2ME_DESC;

	/**
	 * @return the version of OS of mobile.
	 * @see #getMobileOS()
	 * @since 7.0
	 */
	public final String getMobileOSVer() {
		if(mobileOSVer == null){
			mobileOSVer = (String)ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					return ClientDesc.getAgent().getVer();
				}
			});
		}
		return mobileOSVer;
	}
	
	String mobileOSVer;

	/**
	 * @return one of the following: <br>
	 *         1. {@link #OS_ANDROID} <br>
	 *         2. {@link #OS_IOS} <br>
	 *         3. {@link #OS_J2ME} <br>
	 *         4. and other in the future.
	 * @see #getMobileOSVer()
	 * @since 7.0
	 */
	public final String getMobileOS() {
		if(mobileOS == null){
			mobileOS = (String)ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					final String oldOS = ClientDesc.getAgent().getOS();
					for (int i = 0; i < allOS.length; i++) {
						final String oneOS = allOS[i];
						if(oneOS.equals(oldOS)){
							return oneOS;
						}
					}
					return oldOS;
				}
			});
		}
		return mobileOS;
	}
	
	String mobileOS;

	private static final String[] allOS = {OS_ANDROID, OS_IOS, OS_J2ME};

	/**
	 * return three case legal values: <br>
	 * 1. language ("en", "fr", "ro", "ru", etc.) <br>
	 * 2. language-region ("en-GB", "en-CA", "en-IE", "en-US", etc.)
	 * 
	 * @return if unknown, return default 'en-US'.
	 * @since 7.0
	 */
	public final String getMobileLocale() {
		return ClientDesc.getClientLang();
	}

	/**
	 * action keyboard keys on server. For example "Control+Shift+Escape".
	 * <p>
	 * Control : KeyEvent.VK_CONTROL<br>
	 * Shift : KeyEvent.VK_SHIFT<br>
	 * Escape : KeyEvent.<B>VK_</B>ESCAPE<br>
	 * Meta : KeyEvent.VK_META (Max OS X : Command)
	 * <p>
	 * more examples : "Shift+a" (for input 'A', case insensitive), "B" (for
	 * input 'b'). <BR><b>Important</b> : it also work in Android server.<br>
	 * <BR>
	 * more key string , please refer <a target="_blank" href=
	 * "http://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html"
	 * >http
	 * ://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html</a><br>
	 * <br>
	 * NOTE : NOT all keys are supported.<br>
	 * <br>
	 * <B>Important : </B>If a HAR package developed in J2SE, when run in
	 * Android platform, it does as following:<br>
	 * 1. the key name is searched first in <code>java.awt.event.KeyEvent</code>
	 * ('VK_' is NOT required. 'VK_' is prefix in J2SE), <br>
	 * 2. convert keyCode in J2SE to Android (e.g.
	 * java.awt.event.KeyEvent.VK_BACK_SPACE =>
	 * android.view.KeyEvent.KEYCODE_DEL). <br>
	 * 3. if your HAR package is designed for Android only, for example,
	 * Shift+KEYCODE_a (for input 'A', 'KEYCODE_' is required prefix for Android).<br>
	 * 4. for more Android keys, please refer <a target="_blank" href=
	 * "http://developer.android.com/reference/android/view/KeyEvent.html"
	 * >http://developer.android.com/reference/android/view/KeyEvent.html</a><br>
	 * 5. in J2SE, keys begin with 'VK_'; in Android, keys begin with
	 * 'KEYCODE_'. <br>
	 * <br>
	 * We develop AWT/Swing package for Android platform to support HAR package.
	 * So project can be developed/tested in J2SE and run in Android.
	 * 
	 * @param keys
	 *            for example, "Control+Shift+Escape" or "Tab" (case
	 *            insensitive)
	 * @since 7.0
	 */
	public final void actionKeys(final String keys) {
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				KeyComper.actionKeys(keys);
				return null;
			}
		});
	}

	/**
	 * log error message to this server
	 * 
	 * @param msg
	 * @since 7.0
	 */
	public final void err(final String msg) {
		error(msg);
	}

	/**
	 * log message to this server
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

	private final static void sendCmd(final String cmdType, final String para,
			final String value) {
		if (ServerUIAPIAgent.isToMobile()) {
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					HCURLUtil.sendCmd(cmdType, para, value);
					return null;
				}
			});
		}
	}

	private final static void sendCmd(final String cmdType,
			final String[] para, final String[] value) {
		if (ServerUIAPIAgent.isToMobile()) {
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					HCURLUtil.sendCmd(cmdType, para, value);
					return null;
				}
			});
		}
	}

	private final static void sendClass(final String[] para) {
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				ProjectContext.sendCmd(HCURL.DATA_CMD_SendPara,
						ProjectContext.buildParaForClass(para.length - 1), para);
				return null;
			}
		});
	}

	/**
	 * play tone at mobile, please disable mute on mobile first.
	 * 
	 * @param note
	 *            A note is given in the range of 0 to 127 inclusive. Defines
	 *            the tone of the note as specified by the above formula.
	 * @param duration
	 *            The duration of the tone in milli-seconds. Duration must be
	 *            positive.
	 * @param volume
	 *            Audio volume range from 0 to 100. 100 represents the maximum
	 * @since 6.98
	 */
	public final void playTone(final int note, final int duration,
			final int volume) {
		final String[] v = { "hc.j2me.load.Tone", String.valueOf(note),
				String.valueOf(duration), String.valueOf(volume) };
		ProjectContext.sendClass(v);
	}

	/**
	 * display notification on android/IOS.
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
	 * @since 7.0
	 */
	public final void sendNotification(final String title, final String text,
			final int flags) {
		final String[] v = { "hc.j2me.load.Notification", title, text,
				String.valueOf(flags) };
		ProjectContext.sendClass(v);
	}

	/**
	 * @return the milliseconds between stop and restart HAR project(s).<BR>
	 * it is useful for thread of user to stop task and release resource when stop project.<BR>
	 * you can set new value in [option/other/interval seconds between stop and restart HAR project].
	 * @since 7.0
	 */
	public final int getIntervalMSForRestart(){
		if(msForRestart == 0){
			msForRestart = (Integer)ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					return ResourceUtil.getIntervalSecondsForNextStartup() * 1000;
				}
			});
		}
		return msForRestart;
	}
	
	int msForRestart;
	
	/**
	 * check current HAR project is running on Android server or J2SE platform.
	 * 
	 * @return true : is run on Android server; false , run on J2SE or other
	 *         platform.
	 * @see #isJ2SEPlatform()
	 * @since 7.0
	 */
	public final boolean isAndroidPlatform() {
		if(isAndroidPlatform == null){
			isAndroidPlatform = (Boolean)ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					return ResourceUtil.isAndroidServerPlatform();
				}
			});
		}
		return isAndroidPlatform;
	}
	
	Boolean isAndroidPlatform;

	/**
	 * check current HAR project is running on J2SE platform.
	 * 
	 * @return true : is run on J2SE; false, run on Android or other platform.
	 * @see #isAndroidPlatform()
	 * @since 7.0
	 */
	public final boolean isJ2SEPlatform() {
		if(isJ2SEPlatform == null){
			isJ2SEPlatform = (Boolean)ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					return ResourceUtil.isStandardJ2SEServer();
				}
			});
		}
		return isJ2SEPlatform;
	}
	
	Boolean isJ2SEPlatform;

	/**
	 * requests operation of the mobile device's vibrator, some mobile will do
	 * nothing.
	 * 
	 * @param duration
	 *            the number of milliseconds the vibrator should be run
	 * @since 6.98
	 */
	public final void vibrate(final int duration) {
		final String[] v = { "hc.j2me.load.Vibrate", String.valueOf(duration) };
		ProjectContext.sendClass(v);
	}

	/**
	 * set alert off on mobile.
	 * 
	 * @since 6.98
	 */
	public final void alertOff() {
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				ProjectContext.sendCmd(HCURL.DATA_CMD_ALERT, "status", "off");
				return null;
			}
		});
	}

	/**
	 * set alert on on mobile
	 * 
	 * @since 6.98
	 */
	public final void alertOn() {
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				ProjectContext.sendCmd(HCURL.DATA_CMD_ALERT, "status", "on");
				return null;
			}
		});
	}

	/**
	 * send a alert dialog to mobile.
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
	public final void send(final String caption, final String text,
			final int type) {
		try {
			sendMessage(caption, text, type, null, 0);
		} catch (final Throwable e) {
			// 设计时，手机非在线时，
		}
	}

	/**
	 * because this server is open source, login ID may be a fake, so
	 * authentication is required for HAR project.
	 * 
	 * @return login ID on this server.
	 * @since 7.0
	 */
	public final String getLoginID() {
		if(loginID == null){
			loginID = (String)ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					return IConstant.getUUID();
				}
			});
		}
		return loginID;
	}
	
	String loginID;
	
	/**
	 * <code>SoftUID</code> is an identifier created when mobile application is installed on mobile.
	 * if mobile application is removed and install again, the <code>SoftID</code> is changed.
	 * <BR><BR>Important : it is NOT ID from hardware.
	 * @return
	 * @since 7.2
	 */
	public final String getMobileSoftUID(){
		if(mobileSoftUID == null){
			mobileSoftUID = (String)ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					return ServerUIAPIAgent.getMobileUID();
				}
			});
		}
		return mobileSoftUID;
	}
	
	String mobileSoftUID;

	/**
	 * return the projectContext instance of the current project even if there
	 * are two or more active projects, no matter the caller is in new thread or
	 * event thread.
	 * 
	 * @return the projectContext instance of the current project.
	 */
	public final static ProjectContext getProjectContext() {
		final ContextSecurityConfig csc = ContextSecurityManager
				.getConfig(Thread.currentThread().getThreadGroup());
		if (csc != null) {
			return csc.getProjectContext();
		}
		final ProjectContext ctx = HCLimitSecurityManager
				.getProjectContextFromDispatchThread();
		if (ctx != null) {
			return ctx;
		}

		final StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("-------------------------------important-----------------------------");
		sb.append("\n");
		sb.append("In designer, ProjectContext.getProjectContext will return a context just for test run!!!");
		sb.append("\n");
		sb.append("------------------------------------------------------------------------");
		L.V = L.O ? false : LogManager.log(sb.toString());
		return ServerUIAPIAgent.staticContext;
	}

	/**
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_ALARM
	 * @see #MESSAGE_CONFIRMATION
	 * @see #MESSAGE_INFO
	 * @see #MESSAGE_WARN
	 * @since 7.0
	 */
	public static final int MESSAGE_ERROR = 1;

	/**
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_ALARM
	 * @see #MESSAGE_CONFIRMATION
	 * @see #MESSAGE_ERROR
	 * @see #MESSAGE_INFO
	 * @since 7.0
	 */
	public static final int MESSAGE_WARN = 2;

	/**
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_ALARM
	 * @see #MESSAGE_CONFIRMATION
	 * @see #MESSAGE_ERROR
	 * @see #MESSAGE_WARN
	 * @since 7.0
	 */
	public static final int MESSAGE_INFO = 3;

	/**
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_CONFIRMATION
	 * @see #MESSAGE_ERROR
	 * @see #MESSAGE_INFO
	 * @see #MESSAGE_WARN
	 * @since 7.0
	 */
	public static final int MESSAGE_ALARM = 4;

	/**
	 * @see #sendMessage(String, String, int, BufferedImage, int)
	 * @see #MESSAGE_ALARM
	 * @see #MESSAGE_ERROR
	 * @see #MESSAGE_INFO
	 * @see #MESSAGE_WARN
	 * @since 7.0
	 */
	public static final int MESSAGE_CONFIRMATION = 5;

	/**
	 * send a alert dialog to mobile, <BR>
	 * <BR>
	 * Note : if mobile is in background ({@link #isMobileInBackground()}), a
	 * notification is also created for mobile.
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
	 *            null if no image
	 * @param timeOut
	 *            0:forever;a positive time value in milliseconds
	 * @since 6.98
	 */
	public final void sendMessage(String caption, String text, final int type,
			final BufferedImage image, final int timeOut) {
		if (caption == null) {
			caption = "information";
		}
		if (text == null) {
			text = "";
		}

		if (ServerUIAPIAgent.isToMobile()) {
			String imageData = "";
			if (image != null) {
				final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				try {
					ImageIO.write(image, "png", byteArrayOutputStream);
					final byte[] out = byteArrayOutputStream.toByteArray();
					imageData = "&image=" + ByteUtil.encodeBase64(out);
					byteArrayOutputStream.close();
				} catch (final IOException e) {
					ExceptionReporter.printStackTrace(e);
					return;
				}
			}
			final String url = HCURL.CMD_PROTOCAL + HCURL.HTTP_SPLITTER + HCURL.DATA_CMD_MSG
					+ "?caption=" + StringUtil.replace(caption, "&", "\\&")
					+ "&text=" + StringUtil.replace(text, "&", "\\&")
					+ "&timeOut=" + timeOut + "&type=" + String.valueOf(type)
					+ (imageData);//注意：必须在外部进行转换

			//如果同时发出两个msg，则可能不同步，所以以下要wait
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					ContextManager.getContextInstance().send(
							MsgBuilder.E_GOTO_URL, url);
					return null;
				}
			});
		}
	}

	/**
	 * send a AU sound data to mobile and play.<BR>
	 * Important : it is deprecated.
	 * @deprecated
	 * @param bs
	 * @since 6.98
	 */
	@Deprecated
	public final void sendAUSound(final byte[] bs) {
		try {
			final long length = bs.length;

			final int HEAD = DataPNG.HEAD_LENGTH + MsgBuilder.INDEX_MSG_DATA;
			final byte[] bytes = ByteUtil.byteArrayCacher.getFree((int) length
					+ HEAD);
			System.arraycopy(bs, 0, bytes, HEAD, bs.length);

			final DataPNG blob = new DataPNG();
			blob.bs = bytes;

			blob.setPNGDataLen((int) length, 0, 0, 0, 0);
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					ContextManager.getContextInstance().sendWrap(
							MsgBuilder.E_SOUND, bytes,
							MsgBuilder.INDEX_MSG_DATA,
							(int) length + DataPNG.HEAD_LENGTH);

					ByteUtil.byteArrayCacher.cycle(bytes);
//					LogManager.log("AU length:" + length);
					return null;
				}
			});
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * display a tip message on tray of this server.
	 * 
	 * @param msg
	 *            a tip message on tray.
	 * @since 6.98
	 */
	public final void tipOnTray(final String msg) {
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				ContextManager.displayMessage((String) ResourceUtil.get(IContext.INFO),
						msg, IContext.INFO, 0);		
				return null;
			}
		});
	}

	/**
	 * @return true : if a mobile is log-in and keep connecting, maybe in background;
	 * @see #addSystemEventListener(SystemEventListener)
	 * @see #removeSystemEventListener(SystemEventListener)
	 * @see #isMobileInBackground()
	 * @since 7.0
	 */
	public final boolean isMobileConnecting() {
		return ContextManager.isMobileLogin();
	}
	
	/**
	 * Important : if on relay, the data translated to mobile may be slowly.
	 * @return true, if the connection is on relay server, not directly connect to your server.
	 * @since 7.2
	 */
	public final boolean isMobileOnRelay(){
		return SIPManager.isOnRelay();
	}

	/**
	 * <STRONG>Important</STRONG> : Background mode may be NOT supported by mobile implementation. 
	 * <BR>Application may be killed when turn to background.
	 * @return true : mobile is log-in, keep connecting and run in background.
	 * @see #isMobileConnecting()
	 * @see #addSystemEventListener(SystemEventListener)
	 * @see #EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND
	 * @since 7.0
	 */
	public final boolean isMobileInBackground() {
		//TODO 为优化性能，将来mobileagent置于projectContext之中
		//注意：不能用变量暂存
		return (Boolean)ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				return ClientDesc.getAgent().isBackground();
			}
		});
	}
	
	/**
	 * display a message moving from right to left on mobile <BR>
	 * <BR>
	 * Note : if mobile is in background ({@link #isMobileInBackground()}), a
	 * notification is also created for mobile.
	 * 
	 * @param msg
	 *            the body of moving message.
	 * @since 6.98
	 */
	public final void sendMovingMsg(final String msg) {
		if (ServerUIAPIAgent.isToMobile()) {
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					HCURLUtil.sendCmd(HCURL.DATA_CMD_MOVING_MSG, "value", msg);
					return null;
				}
			});
		}
	}

	/**
	 * Removes the specified system event listener so that it no longer receives
	 * system events from this system. If l is null, no exception is thrown and
	 * no action is performed.
	 * 
	 * @param listener
	 * @see #addSystemEventListener(SystemEventListener)
	 * @since 7.0
	 */
	public final void removeSystemEventListener(
			final SystemEventListener listener) {
		if (listener == null) {
			return;
		}
		synchronized (systemEventStack) {
			systemEventStack.removeData(listener);
		}
	}

	/**
	 * Adds the specified system event listener to receive system events from
	 * this system. <br>
	 * <br>
	 * these following events may be listened:
	 * <ul>
	 * <li>{@link #EVENT_SYS_PROJ_STARTUP},</li>
	 * <li>{@link #EVENT_SYS_MOBILE_LOGIN},</li>
	 * <li>{@link #EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND},</li>
	 * <li>{@link #EVENT_SYS_MOBILE_LOGOUT},</li>
	 * <li>{@link #EVENT_SYS_PROJ_SHUTDOWN},</li>
	 * <li>or other events in the future.</li>
	 * </ul>
	 * .
	 * 
	 * @param listener
	 *            the listener to be added.
	 * @see #removeSystemEventListener(SystemEventListener)
	 * @since 7.0
	 */
	public final void addSystemEventListener(final SystemEventListener listener) {
		if (listener == null) {
			return;
		}
		synchronized (systemEventStack) {
			systemEventStack.addTail(listener);
		}
	}

	/**
	 * @since 7.0
	 */
	public static final int FLAG_NOTIFICATION_VIBRATE = ConfigManager.FLAG_NOTIFICATION_VIBRATE;

	/**
	 * @since 7.0
	 */
	public static final int FLAG_NOTIFICATION_SOUND = ConfigManager.FLAG_NOTIFICATION_SOUND;

	/**
	 * @since 7.0
	 */
	public static final String EVENT_SYS_PROJ_SHUTDOWN = "SYS_PROJ_SHUTDOWN";
	/**
	 * @since 7.0
	 */
	public static final String EVENT_SYS_PROJ_STARTUP = "SYS_PROJ_STARTUP";
	/**
	 * @since 7.0
	 */
	public static final String EVENT_SYS_MOBILE_LOGIN = "SYS_MOBILE_LOGIN";
	/**
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
