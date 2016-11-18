package hc.server.ui.design;

public class HCPermissionConstant {
	private static final String NOT_AFFECT = "the file got by <STRONG>hc.server.ui.ProjectContext</STRONG>.getPrivateFile(fileName) is NOT affected by this option.</html>";

	public static final String WRITE_TIP = "<html>write file to system. <br>" + NOT_AFFECT;
	public static final String EXIT_TIP = "terminates the currently running JVM.";
	public static final String DELETE_TIP = "<html>delete file in system. <br>" + NOT_AFFECT;
	public static final String EXECUTE_TIP = "<html>execute system command in OS. " +
			"<br><STRONG>Note :</STRONG>important documents may be read</html>";

	public static final String WRITE_SYSTEM_PROPERTIES = "write system properties";

	public static final String READ_SYSTEM_PROPERTIES = "read system properties";
	public static final String MEMBER_ACCESS_SYSTEM = "memberAccess java.lang.System";
	
	public static final String WRITE_PROP_TIP = "<html>allows <STRONG>System</STRONG>.setProperty to be called.</html>";

	public static final String READ_PROP_TIP = "<html>allows <STRONG>System</STRONG>.getProperty to be called.</html>";
	public static final String MEMBER_ACCESS_SYSTEM_TIP = "<html>enable use java.lang.System in JRuby and suppress the exception if JRE &lt; 1.7." +
			"<br><STRONG>Note :</STRONG>malicious code may obtain system privileges if JRE &lt; 1.7</html>";
	
	public static final String LOAD_LIB_TIP = "<html>dynamic linking of the specified native code library." +
			"<br>see <STRONG>java.lang.Runtime</STRONG>.loadXXX()</html>";
	
	public static final String ROBOT_TIP = "<html>The <STRONG>java.awt.Robot</STRONG> (not IoT Robot) object allows code to generate mouse and keyboard events." +
			"<br>It could allow malicious code to control the system, run other programs, read the display.</html>";
	
	public static final String LISTEN_ALL_AWT_EVENTS_TIP = "<html>malicious code may scan all AWT events dispatched in the system, allowing it to read all user input (such as passwords)." +
			"<br>see <STRONG>java.awt.Toolkit</STRONG>.addAWTEventListener/removeAWTEventListener" +
			"</html>";

	public static final String ACCESS_EVENT_QUEUE_TIP = "<html>malicious code may peek at and even remove existing events from the system." +
			"<br>see <STRONG>java.awt.Toolkit</STRONG>.getSystemEventQueue()</html>";
	
	public static final String ACCESS_CLIPBOARD_TIP = "<html>share potentially sensitive or confidential information from clipboard." +
			"<br>see <STRONG>java.awt.Toolkit</STRONG>.getSystemClipboard()</html>";
	
	public static final String READ_DISPLAY_PIXELS_TIP = "<html>the <STRONG>java.awt.Composite</STRONG> interface which allow arbitrary code to examine pixels on the display " +
			"enable malicious code to snoop on the activities of the user." +
			"<br>see <STRONG>java.awt.Graphics2d</STRONG>.setComposite(Composite comp)</html>";
	
	public static final String SHUTDOWN_HOOKS_TIP = "<html>registration and cancellation of virtual-machine shutdown hooks." +
			"<br>see <STRONG>java.lang.Runtime</STRONG>.addShutdownHook/removeShutdownHook</html>";
	
	public static final String SETIO_TIP = "<html>changing the value of the standard system streams of <STRONG>System.out</STRONG>, " +
			"<STRONG>System.in</STRONG>, or <STRONG>System.err</STRONG></html>";
	
	public static final String SET_FACTORY_TIP = "<html>set global factory object for <STRONG>socket</STRONG>, " +
			"<STRONG>URL</STRONG>, <STRONG>Naming</STRONG> or other.</html>";
}
