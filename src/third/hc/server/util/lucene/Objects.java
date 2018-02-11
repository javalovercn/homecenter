package third.hc.server.util.lucene;

public class Objects {
	public static String toString(Object o) {
        return String.valueOf(o);
    }
	
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        return obj;
    }
    
    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }
}
