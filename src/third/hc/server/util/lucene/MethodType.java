package third.hc.server.util.lucene;

public class MethodType {
	static final Class<?>[] NO_PTYPES = {};
	
	public static MethodType methodType(Class<?> rtype) {
        return makeImpl(rtype, NO_PTYPES, true);
    }
	
	public static MethodType methodType(Class<?> rtype, Class<?> ptype0) {
        return makeImpl(rtype, new Class<?>[]{ ptype0 }, true);
    }
	
	public static MethodType methodType(Class<?> rtype, Class<?>[] ptypes) {
        return makeImpl(rtype, ptypes, false);
    }
	
	static MethodType makeImpl(Class<?> rtype, Class<?>[] ptypes, boolean trusted) {
		return new MethodType();
    }
	
    private static final MethodType[] objectOnlyTypes = new MethodType[20];
}
