package hc.server.ui.design.code;

import java.util.Vector;

public class RubyClassAndDoc {
	public final Class claz;
	public final String docShortName;
	public final String displayClassName;
	public final Vector<RubyMethodItem> insMethods = new Vector<RubyMethodItem>();
	public final Vector<RubyMethodItem> staticMethods = new Vector<RubyMethodItem>();
	public boolean isInitMethods = false;

	public final RubyMethodItem searchInstanceMethod(final String methodName) {
		checkInitMethods();
		return searchMethod(insMethods, methodName);
	}

	@Override
	public String toString() {
		return docShortName;
	}

	private final RubyMethodItem searchMethod(final Vector<RubyMethodItem> ms, final String methodName) {
		final int size = ms.size();
		for (int i = 0; i < size; i++) {
			final RubyMethodItem item = ms.get(i);
			if (item.methodOrField.equals(methodName)) {
				return item;
			}
		}
		return null;
	}

	public final RubyMethodItem searchStaticMethod(final String methodName) {
		checkInitMethods();
		return searchMethod(staticMethods, methodName);
	}

	private final void checkInitMethods() {
		if (isInitMethods == false) {
			RubyHelper.codeHelper.window.docHelper.processDoc(RubyHelper.codeHelper, claz, false);
		}
	}

	public RubyClassAndDoc(final Class claz, final String docShortName) {
		this(claz, docShortName, "Ruby" + docShortName);
	}

	public RubyClassAndDoc(final Class claz, final String docShortName, final String displayClassName) {
		this(claz, docShortName, displayClassName, null, null);
	}

	/**
	 * 
	 * @param claz
	 * @param docShortName
	 * @param displayClassName
	 * @param additionMethods
	 *            可以为null
	 */
	public RubyClassAndDoc(final Class claz, final String docShortName, final String displayClassName, final String[] additionMethods,
			final String[] additionStaticMethods) {
		this.claz = claz;
		this.docShortName = docShortName;
		this.displayClassName = displayClassName;

		if (additionMethods != null) {
			for (int i = 0; i < additionMethods.length; i++) {
				insMethods.add(new RubyMethodItem(additionMethods[i]));
			}
		}

		if (additionStaticMethods != null) {
			for (int i = 0; i < additionStaticMethods.length; i++) {
				staticMethods.add(new RubyMethodItem(additionStaticMethods[i]));
			}
		}
	}
}
