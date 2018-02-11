package third.hc.server.util.lucene;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import sun.reflect.Reflection;
import third.hc.server.util.lucene.MethodHandle;
import third.hc.server.util.lucene.MethodType;

public class MethodHandles {
	public static Lookup lookup() {
        return new Lookup(Reflection.getCallerClass(2));
    }
	
	public static final class Lookup {
		private final Class<?> lookupClass;

        private final int allowedModes;
        
        public static final int PUBLIC = Modifier.PUBLIC;

        public static final int PRIVATE = Modifier.PRIVATE;

        public static final int PROTECTED = Modifier.PROTECTED;

        public static final int PACKAGE = Modifier.STATIC;

        private static final int ALL_MODES = (PUBLIC | PRIVATE | PROTECTED | PACKAGE);
        private static final int TRUSTED   = -1;
        
        Lookup(Class<?> lookupClass) {
            this(lookupClass, ALL_MODES);
        }

        private Lookup(Class<?> lookupClass, int allowedModes) {
            this.lookupClass = lookupClass;
            this.allowedModes = allowedModes;
        }
		
        public Class<?> lookupClass() {
            return lookupClass;
        }

        private Class<?> lookupClassOrNull() {
            return (allowedModes == TRUSTED) ? null : lookupClass;
        }
		
		public MethodHandle findVirtual(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            final Method[] methods = refc.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
				final Method m = methods[i];
				if(m.getName().equals(name)){
					return new MethodHandle(m);
				}
			}
            return null;
        }
	}
}
