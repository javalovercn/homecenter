package hc.server.ui.design.hpj;

import javax.script.ScriptException;

public class HCScriptException extends ScriptException {
	String message;
	int lineNumber;

	public final void reset() {
		message = null;
		lineNumber = 0;
	}

	public HCScriptException(final String message) {
		super(message);
	}

	@Override
	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(final int ln) {
		lineNumber = ln;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}
}