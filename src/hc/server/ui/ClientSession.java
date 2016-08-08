package hc.server.ui;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * a {@link ClientSession} instance will be created before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}, and will be released after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}.
 * <BR>
 * a {@link ProjectContext} instance will be created before {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}, and will be released after {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}.
 * <BR><BR>
 * invoke {@link ProjectContext#getClientSession()} to get it.
 * <BR>
 * <BR>{@link ClientSession} is thread safe.
 * @since 7.7
 */
public class ClientSession {
	private final Hashtable<String, Object> attribute_map = new Hashtable<String, Object>();
	
	public ClientSession(){
		
	}
	
	/**
	 * returns the attribute with the given name, or null if there is no
	 * attribute by that name. <BR>
	 * It is thread safe.
	 * 
	 * @param name
	 * @return the attribute with the <code>name</code>.
	 * @since 7.7
	 */
	public final Object getAttribute(final String name) {
		return attribute_map.get(name);
	}
	
	/**
	 * returns the attribute with the given name, or <code>defaultValue</code>
	 * if there is no attribute by that name.
	 * 
	 * @param name
	 * @param defaultValue
	 *            the default value for name.
	 * @return <code>defaultValue</code> if this map contains no attribute for
	 *         the name
	 * @since 7.7
	 */
	public final Object getAttribute(final String name, final Object defaultValue) {
		final Object value = attribute_map.get(name);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}
	
	/**
	 * set an object to a given attribute name. <BR>
	 * 
	 * @param name
	 *            the name of attribute.
	 * @param obj
	 *            the value of the attribute.
	 * @since 7.7
	 */
	public final void setAttribute(final String name, final Object obj) {
		attribute_map.put(name, obj);
	}
	
	/**
	 * removes the attribute with the given name.
	 * 
	 * @param name
	 * @since 7.7
	 */
	public final void removeAttribute(final String name) {
		attribute_map.remove(name);
	}
	
	/**
	 * It is equals with {@link #removeAttribute(String)}
	 * 
	 * @param name
	 * @since 7.7
	 */
	public final void clearAttribute(final String name) {
		removeAttribute(name);
	}
	
	/**
	 * returns an enumeration containing the attribute names available.
	 * 
	 * @return the enumeration of all attribute names.
	 * @since 7.7
	 */
	public final Enumeration getAttributeNames() {
		final HashSet<String> set = new HashSet<String>();
		synchronized (attribute_map) {
			final Enumeration<String> en = attribute_map.keys();
			while (en.hasMoreElements()) {
				final String item = en.nextElement();
				set.add(item);
			}
		}

		final Iterator<String> setit = set.iterator();
		return new Enumeration() {
			@Override
			public boolean hasMoreElements() {
				return setit.hasNext();
			}

			@Override
			public Object nextElement() {
				return setit.next();
			}
		};
	}
	
	/**
	 * returns the number of attribute names.
	 * @return
	 * @since 7.7
	 */
	public final int getAttributeSize() {
		return attribute_map.size();
	}
	
	/**
     * check if the specified object is a attribute name in this session.
     * @param   name possible key
     * @return 
     * @throws  NullPointerException  if the name is null.
     * @since 7.7
     */
    public final boolean containsAttributeName(final Object name) {
        return attribute_map.containsKey(name);
    }
    
    /**
     * returns true if this session maps one or more names to this value.
     * @param obj
     * @return 
     * @throws NullPointerException  if the value is null
     * @since 7.7
     */
    public final boolean containsAttributeObject(final Object obj) {
        return attribute_map.contains(obj);
    }
    
    /**
     * check if these is no names to values.
     *
     * @return  true if empty.
     * @since 7.7
     */
    public final boolean isAttributeEmpty(){
    	return attribute_map.isEmpty();
    }
}
