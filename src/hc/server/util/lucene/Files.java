package hc.server.util.lucene;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class Files {
	public static InputStream newInputStream(Path path) throws IOException {
        return new FileInputStream(path);
    }
	
    public static void delete(Path path) throws IOException {
        path.delete();
    }
    
    public static boolean deleteIfExists(Path path) throws IOException {
        return path.delete();
    }
    
	public static OutputStream newOutputStream(Path path) throws IOException {
        return new FileOutputStream(path);
    }
	
	public static BufferedReader newBufferedReader(Path path, Charset cs) throws IOException {
        CharsetDecoder decoder = cs.newDecoder();
        Reader reader = new InputStreamReader(newInputStream(path), decoder);
        return new BufferedReader(reader);
    }
	
	public static boolean exists(Path path) {
		return path.exists();
    }
}
