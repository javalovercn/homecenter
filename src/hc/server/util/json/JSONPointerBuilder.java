package hc.server.util.json;

import java.util.ArrayList;
import java.util.List;

/*
Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/**
 * This class allows the user to build a JSONPointer in steps, using
 * exactly one segment in each step.
 * @author JSON.org
 * @version 2016-05-14
 */
public class JSONPointerBuilder {

    // Segments for the eventual JSONPointer string
    private final List<String> refTokens = new ArrayList<String>();

    /**
     * Creates a {@code JSONPointer} instance using the tokens previously set using the
     * {@link #append(String)} method calls.
     */
    public JSONPointer build() {
        return new JSONPointer(this.refTokens);
    }

    /**
     * Adds an arbitrary token to the list of reference tokens. It can be any non-null value.
     * 
     * Unlike in the case of JSON string or URI fragment representation of JSON pointers, the
     * argument of this method MUST NOT be escaped. If you want to query the property called
     * {@code "a~b"} then you should simply pass the {@code "a~b"} string as-is, there is no
     * need to escape it as {@code "a~0b"}.
     * 
     * @param token the new token to be appended to the list
     * @return {@code this}
     * @throws NullPointerException if {@code token} is null
     */
    public JSONPointerBuilder append(final String token) {
        if (token == null) {
            throw new NullPointerException("token cannot be null");
        }
        this.refTokens.add(token);
        return this;
    }

    /**
     * Adds an integer to the reference token list. Although not necessarily, mostly this token will
     * denote an array index. 
     * 
     * @param arrayIndex the array index to be added to the token list
     * @return {@code this}
     */
    public JSONPointerBuilder append(final int arrayIndex) {
        this.refTokens.add(String.valueOf(arrayIndex));
        return this;
    }
}
