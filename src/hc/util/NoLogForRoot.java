package hc.util;

import hc.core.util.ILog;

public class NoLogForRoot implements ILog {

	@Override
	public void log(final String msg) {
	}

	@Override
	public void errWithTip(final String msg) {
	}

	@Override
	public void err(final String msg) {
	}

	@Override
	public void info(final String msg) {
	}

	@Override
	public void warning(final String msg) {
	}

	@Override
	public void flush() {
	}

	@Override
	public void exit() {
	}

}
