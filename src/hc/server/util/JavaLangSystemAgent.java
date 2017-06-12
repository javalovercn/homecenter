package hc.server.util;

import hc.util.ClassUtil;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.Channel;
import java.util.Properties;

/**
 * the HomeCenter provide build-in SecurityManager for HAR project.
 * <BR>each JRuby code is based on reflection, malicious code can obtain system privileges through private field security of Class java.lang.System,
 * so even if the {@link System#currentTimeMillis()} in JRuby will throws SecurityException about <code>block memberAccess(reflection)</code> by SecurityManager if JRE < 1.7.
 * <BR><BR>if JRE >= 1.7, the field <code>security</code> of  java.lang.System is gone.
 * <BR><BR>so you can choose one of the following for your scripts (NOT code in jar) :
 * <BR> 1. to support all JRE version, replace {@link System java.lang.System} with {@link JavaLangSystemAgent} in JRuby scripts (<STRONG>NOT</STRONG> java library).
 * <BR> 2. or upgrade JRE to 1.7
 * <BR> 3. or enable [permissions->memberAccess java.lang.System] to suppress the exception if JRE < 1.7. 
 * <BR><BR><STRONG>Important</STRONG> : 
 * <BR>if your project is developed or distributed before version 7.1 of HomeCenter, permission [memberAccess java.lang.System] is enabled default, otherwise is disabled.
 * @since 7.1
 */
public class JavaLangSystemAgent {
	
    /**
     * the agent method for <code>System.in</code>.
     * @return a InputStream or null
     * @since 7.1
     */
    public final static InputStream getIN(){
    	return System.in;
    }

    /**
     * the agent method for <code>System.out</code>.
     * @return a PrintStream or null
     * @since 7.1
     */
    public final static PrintStream getOUT(){
    	return System.out;
    }

    /**
     * the agent method for <code>System.err</code>.
     * @return a PrintStream or null
     * @since 7.1
     */
    public final static PrintStream getERR(){
    	return System.err;
    }

    /**
     * the agent method for <code>System.setIn</code>.
     * @since 7.1
     */
    public static void setIn(final InputStream in) {
        System.setIn(in);
    }

    /**
     * the agent method for <code>System.setOut</code>.
     * @since 7.1
     */
    public static void setOut(final PrintStream out) {
        System.setOut(out);
    }

    /**
     * the agent method for <code>System.setErr</code>.
     * @since 7.1
     */
    public static void setErr(final PrintStream err) {
        System.setErr(err);
    }

    /**
     * the agent method for <code>System.console</code>.
     * @return a Console or null
     * @since 7.1
     */
    public final static Console console() {
    	return System.console();
    }

    /**
     * the agent method for <code>System.inheritedChannel</code>.
     * @return a Channel or null
     * @since 7.1
     */
    public final static Channel inheritedChannel() throws IOException {
        return System.inheritedChannel();
    }

    /**
     * the agent method for <code>System.setSecurityManager</code>.
     * @since 7.1
     */
    public final static void setSecurityManager(final SecurityManager s) {
    	System.setSecurityManager(s);
    }

    /**
     * the agent method for <code>System.getSecurityManager</code>.
     * @return a SecurityManager or null
     * @since 7.1
     */
    public final static SecurityManager getSecurityManager() {
        return System.getSecurityManager();
    }

    /**
     * the agent method for <code>System.currentTimeMillis</code>.
     * @return current time
     * @since 7.1
     */
    public final static long currentTimeMillis(){
    	return System.currentTimeMillis();
    }

    /**
     * the agent method for <code>System.nanoTime</code>.
     * @return nano time
     * @since 7.1
     */
    public final static long nanoTime(){
    	return System.nanoTime();
    }

    /**
     * the agent method for <code>System.arraycopy</code>.
     * @since 7.1
     */
    public final static void arraycopy(final Object src,  final int  srcPos,
                                        final Object dest, final int destPos,
                                        final int length){
    	System.arraycopy(src, srcPos, dest, destPos, length);
    }

    /**
     * the agent method for <code>System.identityHashCode</code>.
     * @return identity hash code
     * @since 7.1
     */
    public final static int identityHashCode(final Object x){
    	return System.identityHashCode(x);
    }

    /**
     * the agent method for <code>System.getProperties</code>.
     * @return the system properties
     * @since 7.1
     */
    public final static Properties getProperties() {
        return System.getProperties();
    }

    /**
     * the agent method for <code>System.lineSeparator</code>.
     * @return Returns the system-dependent line separator string.  It always
     * returns the same value - the initial value of the {@linkplain #getProperty(String) system property} {@code line.separator}.
     * <p>On UNIX systems, it returns {@code "\n"}; on Microsoft
     * Windows systems it returns {@code "\r\n"}.
     * @since 7.1
     */
    public final static String lineSeparator() {
    	return (String)ClassUtil.invoke(System.class, System.class, "lineSeparator", ClassUtil.NULL_PARA_TYPES, ClassUtil.NULL_PARAS, false);
//        return System.lineSeparator();//不是java 1.6的API
    }

    /**
     * the agent method for <code>System.setProperties</code>.
     * @since 7.1
     */
    public final static void setProperties(final Properties props) {
        System.setProperties(props);
    }

    /**
     * the agent method for <code>System.getProperty</code>.
     * @return the string value of the system property or null
     * @since 7.1
     */
    public final static String getProperty(final String key) {
        return System.getProperty(key);
    }

    /**
     * the agent method for <code>System.getProperty</code>.
     * @return the string value of the system property or the def value
     * @since 7.1
     */
    public final static String getProperty(final String key, final String def) {
        return System.getProperty(key, def);
    }

    /**
     * the agent method for <code>System.setProperty</code>.
     * @return the previous value of the system property or null
     * @since 7.1
     */
    public final static String setProperty(final String key, final String value) {
        return System.setProperty(key, value);
    }

    /**
     * the agent method for <code>System.clearProperty</code>.
     * @return the previous string value of the system property or null
     * @since 7.1
     */
    public final static String clearProperty(final String key) {
        return System.clearProperty(key);
    }

    /**
     * the agent method for <code>System.getenv</code>.
     * @return the string value of the variable or null
     * @since 7.1
     */
    public final static String getenv(final String name) {
        return System.getenv(name);
    }

    /**
     * the agent method for <code>System.getenv</code>.
     * @return the environment as a map of variable names to values
     * @since 7.1
     */
    public final static java.util.Map<String,String> getenv() {
        return System.getenv();
    }

    /**
     * the agent method for <code>System.exit</code>.
     * @since 7.1
     */
    public final static void exit(final int status) {
        System.exit(status);
    }

    /**
     * the agent method for <code>System.gc</code>.
     * @since 7.1
     */
    public final static void gc() {
        System.gc();
    }

    /**
     * the agent method for <code>System.runFinalization</code>.
     * @since 7.1
     */
    public final static void runFinalization() {
        System.runFinalization();
    }

    /**
     * the agent method for <code>System.runFinalizersOnExit</code>.
     * @since 7.1
     */
    public final static void runFinalizersOnExit(final boolean value) {
    	System.runFinalizersOnExit(value);
    }

    /**
     * the agent method for <code>System.load</code>.
     * @since 7.1
     */
    public final static void load(final String filename) {
        System.load(filename);
    }

    /**
     * the agent method for <code>System.loadLibrary</code>.
     * @since 7.1
     */
    public final static void loadLibrary(final String libname) {
        System.loadLibrary(libname);
    }

    /**
     * the agent method for <code>System.mapLibraryName</code>.
     * @return a platform-dependent native library name.
     * @since 7.1
     */
    public final static String mapLibraryName(final String libname){
    	return System.mapLibraryName(libname);
    }

}
