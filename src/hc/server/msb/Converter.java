package hc.server.msb;

import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;

/**
 * <code>Converter</code> is useful to convert data format between <code>Robot</code> and <code>Device</code> (device also means data source for cloud), when format between them is inconsistent.
 * <BR><BR>
 * when <code>Device</code> or data source is upgraded, you can still keep <code>Robot</code> unchanged, <BR><BR>
 * for example, <code>Robot</code> <i>R</i> (in HAR project <i>proj_r</i>) drive <code>Device</code> <i>A</i> (in HAR project <i>proj_dev_a</i>). If <code>Device</code> <i>A</i> is substituted by <code>Device</code> <i>B</i>, 
 * then do as following:<BR>
 * 1. remove HAR project <i>proj_dev_a</i> from server,<BR>
 * 2. add HAR project <i>proj_dev_b</i>,<BR>
 * 3. add HAR project <i>proj_cvt_c</i>, which <code>Converter</code> <i>A_to_B</i> is included in, <BR>
 * 4. bind <i>Reference Device ID</i> (in <code>Robot</code> <i>R</i>) to real device ID (in <code>Device</code> <i>B</i>) and set <code>Converter</code> <i>A_to_B</i> between them.<BR>
 * @see Robot
 * @see Device
 * @see Message
 */
public abstract class Converter {
	final ProjectContext __context;
	ConverterWrapper __fpwrapper;
	
	/**
	 * @deprecated
	 */
	@Deprecated
	Workbench __workbench;
	
	private String __name;
	final String classSimpleName;
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public Converter(){
		this("");
	}
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public Converter(final String name){
		this.classSimpleName = this.getClass().getSimpleName();
		__context = ProjectContext.getProjectContext();
		this.__name = ServerUIAPIAgent.getProcessorNameFromCtx(__context, name, ServerUIAPIAgent.CONVERT_NAME_PROP);
	}

	/**
	 * return the <code>ProjectContext</code> instance of current project.
	 * @return
	 * @since 7.0
	 */
	public ProjectContext getProjectContext(){
		return __context;
	}
	
	/**
	 * return the name of <code>Converter</code>
	 * @return
	 * @since 7.0
	 */
	final String getName(){
		return __name;
	}
	
	/**
	 * @param name the name of <code>Converter</code>
	 * @since 7.0
	 */
	final void setName(final String name){
		this.__name = name;
	}
	
	/**
	 * convert <code>Message</code> <code>fromDevice</code> to <code>Message</code> <code>toRobot</code>.
	 * <br><br>after converting, the <code>toRobot</code> will be dispatched to target, the <code>fromDevice</code> will be recycled by server.
	 * <br>it is <Strong>NOT</Strong> allowed to keep any references of <code>Message</code> in the instance of <code>Converter</code>.
	 * <br><br>to print log about creating/converting/transferring/recycling of message, please enable [Option/Developer/log MSB message].
	 * @param fromDevice <code>Message</code> will be converted from.
	 * @param toRobot <code>Message</code> will be converted to.
	 * @see #downConvert(Message, Message)
	 * @since 7.0
	 */
	public abstract void upConvert(Message fromDevice, Message toRobot);
	
	/**
	 * convert <code>Message</code> <code>fromRobot</code> to <code>Message</code> <code>toDevice</code>.
	 * <BR><BR>after converting, the <code>toDevice</code> will be dispatched to target, the <code>fromRobot</code> will be recycled by server.
	 * <BR><BR>it is <Strong>NOT</Strong> allowed to keep any references of <code>Message</code> in the instance of <code>Converter</code>.
	 * <BR><BR>to print log about creating/converting/transferring/recycling of message, please enable [Option/Developer/log MSB message].
	 * @param fromRobot the message will be auto recycled by HomeCenter server.<BR>the <code>Message</code> is NOT allowed to modified any parts, because this <code>Message</code> may be consumed by other <code>Converter</code>.
	 * @param toDevice the target format <code>Message</code>.
	 * @see #upConvert(Message, Message)
	 * @since 7.0
	 */
	public abstract void downConvert(Message fromRobot, Message toDevice);
	
	final void __forward(final Message msg) {
		__fpwrapper.__response(msg, msg.ctrl_is_downward);
	}
	
	/**
	 * return the compatible description of upside <code>Device</code>.
	 * @return
	 * @see #getDownDeviceCompatibleDescription()
	 * @see Robot#getDeviceCompatibleDescription(String)
	 * @since 7.0
	 */
	public abstract DeviceCompatibleDescription getUpDeviceCompatibleDescription();
	
	/**
	 * return the compatible description of downside <code>Device</code>.
	 * @return
	 * @see #getUpDeviceCompatibleDescription()
	 * @see Device#getDeviceCompatibleDescription()
	 * @since 7.0
	 */
	public abstract DeviceCompatibleDescription getDownDeviceCompatibleDescription();
	
	/**
	 * start up and initialize the <code>Converter</code>.
	 * <br><br>this method is executed by server <STRONG>before</STRONG> {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}
	 * @since 7.0
	 */
	public void startup(){
	}
	
	/**
	 * shutdown the <code>Converter</code>
	 * <br><br>this method is executed by server <STRONG>after</STRONG> {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}
	 * @since 7.0
	 */
	public void shutdown() {
	}

	/**
	 * return the description of this <code>Converter</code>.
	 * @return
	 * @since 7.3
	 */
	public String getIoTDesc(){
		return this.classSimpleName + Processor.buildDesc(__name, __context);
	}
}
