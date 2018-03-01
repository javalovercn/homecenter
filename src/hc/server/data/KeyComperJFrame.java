package hc.server.data;

import hc.App;
import hc.server.HCActionListener;
import hc.server.HCWindowAdapter;
import hc.server.SingleJFrame;
import hc.util.ResourceUtil;

import java.awt.Container;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

public class KeyComperJFrame extends SingleJFrame {
	public KeyComperJFrame() {
		setTitle((String) ResourceUtil.get(9015));
		setIconImage(App.SYS_LOGO);

		final KeyComperPanel panel = new KeyComperPanel(true);
		final Container container = getContentPane();
		container.add(panel);

		getRootPane().registerKeyboardAction(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				panel.notifyCancle();
				dispose();
			}
		}, threadPoolToken), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		panel.setInFrame(this);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new HCWindowAdapter(new Runnable() {
			@Override
			public void run() {
				panel.notifyCancle();
				dispose();
			}
		}, threadPoolToken));

		// pack后尺寸偏小，故改为setSize
		setSize(700, 500);// 600X400，Window-Nimbus下略小
		// pack();

		// jdk8会出现漂移，所以关闭下行
		// setResizable(false);

		App.showCenter(this);
	}
}