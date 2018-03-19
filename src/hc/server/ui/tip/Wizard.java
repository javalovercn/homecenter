package hc.server.ui.tip;

import hc.server.HCActionListener;
import hc.App;
import java.awt.Point;
import java.awt.TrayIcon.MessageType;
import javax.swing.JButton;

public class Wizard {
	JButton[] buttons;
	int index;

	public Wizard(JButton[] btns) {
		this.buttons = btns;
	}

	final HCActionListener al = new HCActionListener(new Runnable() {
		@Override
		public void run() {
			index++;
			showTip();
		}
	}, App.getThreadPoolToken());

	public void showTip() {
		if (index == buttons.length) {
			return;
		}

		JButton jButton = buttons[index];
		final Point location = jButton.getLocationOnScreen();
		location.x += jButton.getSize().width / 2;
		location.y += jButton.getSize().height / 2;

		TipPop.CornerPosition position = MessageTipFrame.converTo(location.x, location.y);
		MessageTipFrame instanceMessageFrame = new MessageTipFrame();
		instanceMessageFrame.setNextMode((index + 1 == buttons.length) ? false : true, al);
		instanceMessageFrame.setCornerPosition(position, location.x, location.y, 0, 0);
		instanceMessageFrame.showMessage(jButton.getText(), jButton.getToolTipText(), MessageType.INFO);
	}
}
