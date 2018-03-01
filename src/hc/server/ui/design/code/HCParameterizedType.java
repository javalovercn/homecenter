package hc.server.ui.design.code;

import hc.core.util.StringBufferCacher;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class HCParameterizedType implements ParameterizedType {
	private final Type[] actualTypes;
	private final Type rawType;
	private final Type ownType;

	public HCParameterizedType(final Type[] actualTypes, final Type rawType, final Type ownType) {
		this.actualTypes = actualTypes;
		this.rawType = rawType;
		this.ownType = ownType;
	}

	@Override
	public Type[] getActualTypeArguments() {
		return actualTypes;
	}

	@Override
	public Type getRawType() {
		return rawType;
	}

	@Override
	public Type getOwnerType() {
		return ownType;
	}

	@Override
	public final String toString() {
		final StringBuffer sb = StringBufferCacher.getFree();
		if (rawType != null) {
			sb.append(ReturnType.getGenericReturnTypeDesc(rawType));
			if (actualTypes != null) {
				sb.append("<");
				for (int i = 0; i < actualTypes.length; i++) {
					if (i > 0) {
						sb.append(", ");
					}
					sb.append(ReturnType.getGenericReturnTypeDesc(actualTypes[i]));
				}
				sb.append(">");
			}
		}

		final String out = sb.toString();
		StringBufferCacher.cycle(sb);
		return out;
	}
}
