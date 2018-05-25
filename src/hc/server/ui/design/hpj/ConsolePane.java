package hc.server.ui.design.hpj;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.LogManager;
import hc.server.ui.design.Designer;
import hc.util.ResourceUtil;

public class ConsolePane extends JPanel {
	private final JButton closeButton = new JButton(Designer.loadImg("exit_16.png"));
	final ConsoleTextPane ctp;

	public ConsolePane(final ConsoleTextPane ctp) {
		super(new BorderLayout());
		this.ctp = ctp;

		final ScriptEditPanel sep = ctp.sep;
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						sep.showConsole(false);
					}
				});
			}
		});

		final JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(closeButton, BorderLayout.LINE_END);
		topPanel.add(new JLabel("Evaluation Console :"), BorderLayout.LINE_START);
		topPanel.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
			}
			
			@Override
			public void mousePressed(final MouseEvent e) {
			}
			
			@Override
			public void mouseExited(final MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(final MouseEvent e) {
				L.V = L.WShop ? false : LogManager.log("[CodeTip] mouseExitHideDocForMouseMovTimer enable from mouseEntered of ConsolePane.");
				final MouseExitHideDocForMouseMovTimer timer = Designer.getInstance().codeHelper.mouseExitHideDocForMouseMovTimer;
				timer.resetTimerCount();
				timer.setEnable(true);
			}
			
			@Override
			public void mouseClicked(final MouseEvent e) {
			}
		});

		add(topPanel, BorderLayout.NORTH);
		ResourceUtil.removeFromParent(ctp.textPane);
		final JScrollPane scrollpane = new JScrollPane(ctp.textPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollpane, BorderLayout.CENTER);
	}
}
