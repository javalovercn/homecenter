package hc.server.ui.design.code;

import hc.core.util.ReturnableRunnable;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.HTMLMlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ScriptCSSSizeHeight;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.util.StringBuilderCacher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyleManager {
	// 注意：如果增加，请同步更改replaceVariable
	public static final char PARAMETER_BORDER_CHAR = '$';

	static final String[] variables = { "$smallFontSize$", "$normalFontSize$", "$largeFontSize$", "$buttonFontSize$", "$buttonHeight$",
			"$mobileWidth$", "$mobileHeight$", "$dialogBorderRadius$", "$colorForBodyByHexString$", "$colorForFontByHexString$" };

	private static final Pattern variableForPattern = Pattern.compile("\\$\\w+\\$");

	public static String replaceVariable(final J2SESession coreSS, final String styles, final ScriptCSSSizeHeight sizeHeightForXML,
			final ProjectContext ctx) {
		if (styles.indexOf(PARAMETER_BORDER_CHAR, 0) > 0) {
			if (coreSS.mobileValuesForCSS == null) {
				coreSS.mobileValuesForCSS = (Object[]) ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS,
						ServerUIAPIAgent.getProjResponserMaybeNull(ctx), new ReturnableRunnable() {
							@Override
							public Object run() throws Throwable {
								final Object[] values = { sizeHeightForXML.getFontSizeForSmall(), sizeHeightForXML.getFontSizeForNormal(),
										sizeHeightForXML.getFontSizeForLarge(), sizeHeightForXML.getFontSizeForButton(),
										sizeHeightForXML.getButtonHeight(), UserThreadResourceUtil.getMletWidthFrom(coreSS),
										UserThreadResourceUtil.getMletHeightFrom(coreSS), sizeHeightForXML.getDialogBorderRadius(),
										HTMLMlet.getColorForBodyByHexString(), HTMLMlet.getColorForFontByHexString() };
								return values;
							}
						});
			}
			final Object[] mobileValuesForCSS = coreSS.mobileValuesForCSS;

			final Matcher matcher = variableForPattern.matcher(styles);
			int appendIdx = 0;
			StringBuilder sb = null;
			while (matcher.find()) {
				final int startIdx = matcher.start();
				final int endIdx = matcher.end();

				if (sb == null) {
					sb = StringBuilderCacher.getFree();
				}

				sb.append(styles.substring(appendIdx, startIdx));

				final String v = styles.substring(startIdx, endIdx);
				final int size = variables.length;

				boolean isReplaced = false;
				for (int i = 0; i < size; i++) {
					if (v.equals(variables[i])) {
						sb.append(mobileValuesForCSS[i]);
						isReplaced = true;
						break;
					}
				}

				if (isReplaced == false) {
					sb.append(v);// 没有找到的变量
				}

				appendIdx = endIdx;
			}

			if (appendIdx == 0) {// 没有变量
				return styles;
			}

			sb.append(styles.substring(appendIdx));

			final String out = sb.toString();
			StringBuilderCacher.cycle(sb);

			return out;
		} else {
			return styles;
		}
	}
}
