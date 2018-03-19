package hc.server.util;

import hc.server.msb.Robot;
import hc.server.ui.ProjectContext;
import javax.swing.JComponent;

/**
 * the <code>Assistant</code> of current project is implemented by project provider, which process
 * voice command.<BR>
 * to register the assistant for a project, please invoke
 * {@link ProjectContext#registerAssistant(Assistant)}.<BR>
 * <BR>
 * if you don't want to implement a assistant, and hope HCAI (HomeCenter AI) to do same work for
 * you, please see detail on {@link #onVoice(VoiceCommand)}.
 * 
 * @since 7.47
 */
public class Assistant {
	private ProjectContext ctx;

	/**
	 * returns the project context.
	 * 
	 * @return
	 */
	public final ProjectContext getProjectContext() {
		if (ctx == null) {
			ctx = ProjectContext.getProjectContext();
		}
		return ctx;
	}

	/**
	 * process a voice command. <BR>
	 * <BR>
	 * please don't implement voice command for following cases, server will do it for you :<BR>
	 * 1. the voice is for open/execute menu items.<BR>
	 * 2. the voice is to open <code>CtrlResponse</code>, and the key words of status of
	 * <code>CtrlResponse</code> are included in voice. (open once is required)<BR>
	 * 3. the voice is to open <code>HTMLMlet</code>, and the key words of JLable(s) or tool tip
	 * text of JComponent are included in voice. (open once is required) <BR>
	 * <BR>
	 * <STRONG>Important :</STRONG><BR>
	 * 1. assistant instance serve only one session at same time (synchronism).<BR>
	 * 2. no all clients support all languages voice command.<BR>
	 * 3. server will try to process voice for you instead of writing codes here, which is based on
	 * analysing the <code>AnalysableRobotParameter</code> between <code>Robot</code> and UI, so it
	 * is NOT recommended to use complex object defined by yourself in
	 * {@link Robot#operate(long, Object)}. <BR>
	 * <BR>
	 * How to more effectively help the server understand the relationship between UI and
	 * <code>Robot</code>, to enhance the automatic processing of voice instruction accuracy?<BR>
	 * 1. set keywords by {@link JComponent#setToolTipText(String)}<BR>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;for example, there are two project on server, one for ice box,
	 * and other for air conditioner. when people speaks "what is the temperature of ice box?", of
	 * cause the voice command should NOT be dispatched to air conditioner, server need more
	 * information about project for ice box, if there is a keywords "ice box" in menu, text
	 * field/label of form, server can be sure it does right, but if there is no keywords in form,
	 * but icons or <code>ScriptPanel</code> in form, you should set keywords by
	 * {@link JComponent#setToolTipText(String)}. <BR>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<STRONG>Important : </STRONG><BR>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;set tool tip text for locale of current session, NOT for all
	 * locales, because different locale means different lucene analyzer. <BR>
	 * 2. implements business in {@link Robot#operate(long, Object)}<BR>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;business may be driven from UI (voice assistant,
	 * <code>HTMLMlet</code> and <code>Dialog</code>), so it is good practice to implement business
	 * in {@link Robot#operate(long, Object)}.<BR>
	 * 3. use the <code>AnalysableRobotParameter</code> instead of complex object defined by
	 * yourself.
	 * 
	 * @param cmd
	 * @return true means the voice command is consumed and never be dispatched to other projects.
	 */
	public boolean onVoice(final VoiceCommand cmd) {
		return false;
	}
}
