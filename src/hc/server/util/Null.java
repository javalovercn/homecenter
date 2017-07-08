package hc.server.util;

public class Null {
	/**
     * There is only intended to be a single instance of the NULL object,
     * so the clone method returns itself.
     *
     * @return NULL.
     */
    @Override
    protected final Object clone() {
        return this;
    }

    /**
     * A Null object is equal to the null value and to itself.
     *
     * @param object
     *            An object to test for nullness.
     * @return true if the object parameter is the JSONObject.NULL object or
     *         null.
     */
    @Override
    public boolean equals(final Object object) {
        return object == null || object == this;
    }
    /**
     * A Null object is equal to the null value and to itself.
     *
     * @return always returns 0.
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Get the "null" string value.
     *
     * @return The string "null".
     */
    @Override
    public String toString() {
        return "null";
    }
}
