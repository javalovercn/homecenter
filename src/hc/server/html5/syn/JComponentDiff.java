package hc.server.html5.syn;

import javax.swing.JComponent;

public class JComponentDiff implements IDiff {
	@Override
	public void diff(final int hcCode, Object src, DifferTodo todo) {
		JComponent compSrc = (JComponent) src;

		{
			final boolean isEnable = compSrc.isEnabled();
			if (isEnable == false) {
				todo.notifyModifyJComponentEnable(hcCode, isEnable);
			}
		}

		{
			todo.notifyJComponentLocation(compSrc);
		}

	}

}
