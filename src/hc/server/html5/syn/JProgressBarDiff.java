package hc.server.html5.syn;

import javax.swing.JProgressBar;

public class JProgressBarDiff extends JComponentDiff{
	@Override
	public void diff(final int hcCode, Object src, DifferTodo todo) {
		super.diff(hcCode, src, todo);
		
		JProgressBar progressBar = (JProgressBar)src;
		
		{
			todo.notifyModifyProgressBarValue(hcCode, progressBar);
		}
	}
}
