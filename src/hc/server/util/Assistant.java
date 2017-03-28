package hc.server.util;

import hc.server.msb.Robot;
import hc.server.ui.Dialog;
import hc.server.ui.HTMLMlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ScriptPanel;

import javax.swing.JComponent;

/**
 * the assistant of current project, which process voice command.<BR>
 * @see ProjectContext#registerAssistant(Assistant)
 * @since 7.47
 */
public class Assistant {
	
	/**
	 * receive a voice command, and process it.
	 * <BR><BR>
	 * <STRONG>Important :</STRONG><BR>
	 * 1. menu items is preferentially matched by server, if fail then dispatched to this method.<BR>
	 * 2. current assistant instance perhaps serve multiple sessions at same time.<BR>
	 * 3. no all clients support all languages voice command.<BR>
	 * 4. it is not easy to process voice commands, server will do our best to process voice for you if voice is complex, but it will take a long time to learn.
	 * <BR><BR>
	 * know more<BR>
	 * 1. set keywords by {@link JComponent#setToolTipText(String)}<BR>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;for example, there are two project on server, one for ice box, and other for air conditioner. 
	 * when people speaks "what is the temperature of ice box?", of cause the voice command should NOT be dispatched to air conditioner,
	 * server need more information about project for ice box, if there is a keywords "ice box" in menu, text field/label of form, 
	 * server can be sure it does right, but if there is no keywords in form, but icons or {@link ScriptPanel} in form, you should set keywords by {@link JComponent#setToolTipText(String)}.
	 * <BR>
	 * 2. implements business in {@link Robot#operate(long, Object)}<BR>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;business may be driven from UI (voice assistant, {@link HTMLMlet} and {@link Dialog}), so it is good practice to implement business in {@link Robot#operate(long, Object)}.
	 * @param cmd
	 * @return true means the voice command is consumed and never be dispatched to other projects.
	 */
	public boolean onVoice(final VoiceCommand cmd){
		return false;
	}
}
