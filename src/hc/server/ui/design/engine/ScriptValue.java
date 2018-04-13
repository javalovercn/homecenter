package hc.server.ui.design.engine;

import hc.core.util.StringValue;

public class ScriptValue extends StringValue {
	public boolean isOptimized;
	
	public ScriptValue() {
	}
	
	public ScriptValue(final String value) {
		super(value);
	}
}
