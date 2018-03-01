package hc.server.ui.design.hpj;

import hc.core.util.ExceptionReporter;

import java.io.File;
import java.io.FileInputStream;

public class HCShareFileResource extends HPNode {
	public HCShareFileResource(final int type, final String name) {
		super(type, name);
		this.ID = name;
	}

	public HCShareFileResource(final int type, final String name, final File file)
			throws Throwable {
		this(type, name);
		this.ID = String.valueOf(MenuManager.getNextNodeIdx());
		content = loadContent(file);
	}

	String ID = "";
	public byte[] content = null;
	String ver = "0.0.1";

	public static byte[] loadContent(final File file) throws Throwable {
		try {
			return HCjar.readFromInputStream(new FileInputStream(file));
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}
}