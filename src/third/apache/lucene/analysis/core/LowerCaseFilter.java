/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package third.apache.lucene.analysis.core;


import java.io.IOException;

import third.apache.lucene.analysis.TokenFilter;
import third.apache.lucene.analysis.TokenStream;
import third.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import third.apache.lucene.analysis.util.CharacterUtils;

/**
 * Normalizes token text to lower case.
 */
public final class LowerCaseFilter extends TokenFilter {
  private final CharacterUtils charUtils = CharacterUtils.getInstance();
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  
  /**
   * Create a new LowerCaseFilter, that normalizes token text to lower case.
   * 
   * @param in TokenStream to filter
   */
  public LowerCaseFilter(TokenStream in) {
    super(in);
  }
  
  @Override
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      charUtils.toLowerCase(termAtt.buffer(), 0, termAtt.length());
      return true;
    } else
      return false;
  }
}
