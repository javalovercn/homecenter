package hc.server.data;

import hc.App;
import hc.server.SingleJFrame;
import hc.util.ResourceUtil;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

public class KeyComperJFrame extends SingleJFrame{
	public KeyComperJFrame(){
		setTitle((String)ResourceUtil.get(9015));
		setIconImage(App.SYS_LOGO);

		final KeyComperPanel panel = new KeyComperPanel(true);
		Container container = getContentPane();
		container.add(panel);

		getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				panel.notifyCancle();
				dispose();
			}},
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);

		panel.setInFrame(this);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				panel.notifyCancle();
				dispose();
			}
		});
		pack();
		
//		jdk8会出现漂移，所以关闭下行
//		setResizable(false);
		
		App.showCenter(this);
	}
}