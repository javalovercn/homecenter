package hc.core.util;

import hc.core.IConstant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class URLEncoder {
	/**
	 * 编码一个串到URL上，如String data = "para="+encode("参数","UTF-8");
	 * @param s
	 * @param enc
	 * @return
	 */
    public static String encode(String s, String enc) {
        boolean needToChange = false;
        boolean wroteUnencodedChar = false;
        int maxBytesPerChar = 10; // rather arbitrary limit, but safe for now
        StringBuffer out = new StringBuffer(s.length());
        ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(buf, enc);
        } catch (UnsupportedEncodingException ex) {
            try {
                writer = new OutputStreamWriter(buf,defaultEncName);
            } catch (UnsupportedEncodingException e) {
                writer = new OutputStreamWriter(buf);
            }
        }
        
        for (int i = 0; i < s.length(); i++) {
            int c = (int) s.charAt(i);
            if (c <256 && dontNeedEncoding[c]) {
                if (c == ' ') {
                    c = '+';
                    needToChange = true;
                }
                out.append((char)c);
                wroteUnencodedChar = true;
            } else {
                try {
                    if (wroteUnencodedChar) {
                        writer = new OutputStreamWriter(buf, enc);
                        wroteUnencodedChar = false;
                    }
                    if(writer != null){
                        writer.write(c);
                    }

                    if (c >= 0xD800 && c <= 0xDBFF) {

                        if ( (i+1) < s.length()) {
                            int d = (int) s.charAt(i+1);
                            if (d >= 0xDC00 && d <= 0xDFFF) {
                                writer.write(d);
                                i++;
                            }
                        }
                    }
                    writer.flush();
                } catch(IOException e) {
                    buf.reset();
                    continue;
                }
                byte[] ba = buf.toByteArray();
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = forDigit((ba[j] >> 4) & 0xF, 16);
                    if (isLetter(ch)) {
                        ch -= caseDiff;
                 }
                    out.append(ch);
                   
                  ch = forDigit((ba[j] & 0xF), 16);
                    if (isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                buf.reset();
                needToChange = true;
            }
        }
        
        return (needToChange? out.toString() : s);
    }
    
    private static boolean isLetter(char c){
        if( (c >= 'a' && c <= 'z') || (c >='A' && c <= 'Z'))
            return true;
        return false;
    }
    
    private static boolean[] dontNeedEncoding;
    private final static String defaultEncName = IConstant.UTF_8;
    static final int caseDiff = ('a' - 'A');
    static {
        dontNeedEncoding = new boolean[256];

        dontNeedEncoding[' '] = true; // encoding a space to a + is done in the encode() method
        dontNeedEncoding['-'] = true;
        dontNeedEncoding['_'] = true;
        dontNeedEncoding['.'] = true;
        dontNeedEncoding['*'] = true;
        
        int i;
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding[i] = true;
        }
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding[i] = true;
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding[i] = true;
        }
    }
    
    public static final int MIN_RADIX = 2;
    
    public static final int MAX_RADIX = 36;
    
    private static char forDigit(int digit,int radix){
        if ((digit >= radix) || (digit < 0)) {
            return '\0';        }
        if ((radix < MIN_RADIX) || (radix > MAX_RADIX)) {
            return '\0';
        }
        if (digit < 10) {
            return (char)('0' + digit);
        }
        return (char)('a' - 10 + digit);
    }

}
