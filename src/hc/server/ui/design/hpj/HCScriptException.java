package hc.server.ui.design.hpj;

import javax.script.ScriptException;

public class HCScriptException extends ScriptException{
	String message;
	int lineNumber;
	
	public HCScriptException(String message) {
		super(message);
	}
	
	public int getLineNumber() {
		return lineNumber;
	}
	
	public void setLineNumber(int ln) {
		lineNumber = ln;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
}