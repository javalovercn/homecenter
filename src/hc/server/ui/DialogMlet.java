package hc.server.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class DialogMlet extends Mlet {
	DialogGlobalLock resLock;
	public DialogProcessedChecker checker;
	
	public final void setDialogGlobalLock(final DialogGlobalLock resLock){
		this.resLock = resLock;
		checker = new DialogProcessedChecker(resLock);
	}
	
	public DialogMlet(){
	}

	public final void addDialog(final Dialog dialog) {
		setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.fill =  GridBagConstraints.NONE;
		
		enableApplyOrientationWhenRTL(dialog.enableApplyOrientationWhenRTL);
		
		add(dialog, c);
	}
	
	public final boolean isContinueProcess(){
		return checker.isContinueProcess(coreSS);
	}
}
