package hc.server.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFormattedTextField;

public class NumberFormatTextField extends JFormattedTextField {
	public NumberFormatTextField(){
		this(false);
	}
	
	public NumberFormatTextField(final boolean enableNegative){
		super();//new NumberFormatter(NumberFormat.getInstance())
		this.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				char keyCh = e.getKeyChar();
		        if ((enableNegative && keyCh == '-') || ((keyCh >= '0') && (keyCh <= '9'))){
		        }else{
		        	e.setKeyChar('\0');
		        }
	        }
			
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
			}
		});		
	}
}