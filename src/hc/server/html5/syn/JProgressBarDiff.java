package hc.server.html5.syn;

import javax.swing.JProgressBar;

public class JProgressBarDiff extends JComponentDiff{
	@Override
	public void diff(final int hcCode, final Object src, final DifferTodo todo) {
		super.diff(hcCode, src, todo);
		
		final JProgressBar progressBar = (JProgressBar)src;
		
		{
			todo.notifyModifyProgressBarValue(hcCode, progressBar);
		}
	}
}
