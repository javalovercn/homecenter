package hc.server.ui.design.code;

import hc.core.L;
import hc.core.util.LogManager;

import java.util.regex.Pattern;

public class RubyMethodItem {
	public final String methodOrField;
	public final String methodOrFieldForDoc;
	public final boolean isPublic;
	public boolean isConstant;
	public final RubyClassAndDoc returnType;// 有可能无法解析时，出现null

	private static final Pattern multipleSpaces = Pattern.compile("[ ]{2,}");

	public final void setConstant(final boolean isConstant) {
		this.isConstant = isConstant;
	}

	public RubyMethodItem(final String method) {
		this(method, true, null, null);
	}

	@Override
	public String toString() {
		if (returnType != null) {
			return (isPublic ? "public " : "") + methodOrField + " : " + returnType.docShortName;
		} else {
			return (isPublic ? "public " : "") + methodOrField + "()";
		}
	}

	public RubyMethodItem(final String method, final boolean isPublic, final String rt,
			final Class c) {
		this(method, method, isPublic, false, rt, c);
	}

	public RubyMethodItem(final String method, final String methodForDoc, final boolean isPublic,
			final boolean isConstant, final RubyClassAndDoc returnType) {
		this.methodOrField = method;
		this.methodOrFieldForDoc = methodForDoc;
		this.isPublic = isPublic;
		this.returnType = returnType;
		this.isConstant = isConstant;
	}

	private RubyMethodItem(final String method, final String methodForDoc, final boolean isPublic,
			final boolean isConstant, String rt, final Class c) {
		this.methodOrField = method;
		this.methodOrFieldForDoc = methodForDoc;
		this.isPublic = isPublic;
		this.isConstant = isConstant;

		if (method.equals("each") && RubyHelper.isEnumerable(c) == false) {
			System.err.println(c.getName() + " should be Enumerable!!!");
			System.exit(0);
		}

		if (rt != null) {
			rt = multipleSpaces.matcher(rt).replaceAll(" ");
			rt = rt.trim().replace(" or nil", "");
			final String new_pre = "new_";
			if (rt.startsWith(new_pre)) {
				rt = rt.substring(new_pre.length());
			}

			if (rt.equals("self")) {
				this.returnType = RubyHelper.searchRubyClass(c);
			} else {
				this.returnType = RubyHelper.searchRubyClassByDocReturn(rt);
				if (returnType != null) {
					// L.V = L.WShop ? false : LogManager.log("[RubyReturn] " +
					// rt + " => " + returnType.docShortName);
				} else {
					L.V = L.WShop ? false
							: LogManager.log("[RubyReturn] unkown return type : " + rt
									+ ", for method : " + method + ", class : "
									+ ((c != null) ? c.getName() : ""));
				}
			}
		} else {
			this.returnType = null;
		}
	}
}
