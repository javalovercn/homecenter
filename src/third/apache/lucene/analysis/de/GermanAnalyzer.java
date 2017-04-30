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
package third.apache.lucene.analysis.de;
// This file is encoded in UTF-8


import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;

import third.apache.lucene.analysis.Analyzer;
import third.apache.lucene.analysis.TokenStream;
import third.apache.lucene.analysis.Tokenizer;
import third.apache.lucene.analysis.core.LowerCaseFilter;
import third.apache.lucene.analysis.core.StopFilter;
import third.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import third.apache.lucene.analysis.snowball.SnowballFilter;
import third.apache.lucene.analysis.standard.StandardAnalyzer;
import third.apache.lucene.analysis.standard.StandardFilter;
import third.apache.lucene.analysis.standard.StandardTokenizer;
import third.apache.lucene.analysis.standard.std40.StandardTokenizer40;
import third.apache.lucene.analysis.util.CharArraySet;
import third.apache.lucene.analysis.util.StopwordAnalyzerBase;
import third.apache.lucene.analysis.util.WordlistLoader;
import third.apache.lucene.util.IOUtils;
import third.apache.lucene.util.Version;

/**
 * {@link Analyzer} for German language. 
 * <p>
 * Supports an external list of stopwords (words that
 * will not be indexed at all) and an external list of exclusions (word that will
 * not be stemmed, but indexed).
 * A default set of stopwords is used unless an alternative list is specified, but the
 * exclusion list is empty by default.
 * </p>
 * 
 * <p><b>NOTE</b>: This class uses the same {@link third.apache.lucene.util.Version}
 * dependent settings as {@link StandardAnalyzer}.</p>
 */
public final class GermanAnalyzer extends StopwordAnalyzerBase {
  
  /** File containing default German stopwords. */
  public final static String DEFAULT_STOPWORD_FILE = "german_stop.txt";
  
  /**
   * Returns a set of default German-stopwords 
   * @return a set of default German-stopwords 
   */
  public static final CharArraySet getDefaultStopSet(){
    return DefaultSetHolder.DEFAULT_SET;
  }
  
  private static class DefaultSetHolder {
    private static final CharArraySet DEFAULT_SET;
    static {
      try {
        DEFAULT_SET = WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, 
            DEFAULT_STOPWORD_FILE, Charset.forName("UTF-8")));
      } catch (IOException ex) {
        // default set should always be present as it is part of the
        // distribution (JAR)
        throw new RuntimeException("Unable to load default stopword set");
      }
    }
  }

  /**
   * Contains the stopwords used with the {@link StopFilter}.
   */
 
  /**
   * Contains words that should be indexed but not stemmed.
   */
  private final CharArraySet exclusionSet;

  /**
   * Builds an analyzer with the default stop words:
   * {@link #getDefaultStopSet()}.
   */
  public GermanAnalyzer() {
    this(DefaultSetHolder.DEFAULT_SET);
  }
  
  /**
   * Builds an analyzer with the given stop words 
   * 
   * @param stopwords
   *          a stopword set
   */
  public GermanAnalyzer(CharArraySet stopwords) {
    this(stopwords, CharArraySet.EMPTY_SET);
  }
  
  /**
   * Builds an analyzer with the given stop words
   * 
   * @param stopwords
   *          a stopword set
   * @param stemExclusionSet
   *          a stemming exclusion set
   */
  public GermanAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet) {
    super(stopwords);
    exclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
  }

  /**
   * Creates
   * {@link third.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   * used to tokenize all the text in the provided {@link Reader}.
   * 
   * @return {@link third.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   *         built from a {@link StandardTokenizer} filtered with
   *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
   *         , {@link SetKeywordMarkerFilter} if a stem exclusion set is
   *         provided, {@link GermanNormalizationFilter} and {@link GermanLightStemFilter}
   */
  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    final Tokenizer source;
    if (getVersion().onOrAfter(Version.LUCENE_4_7_0)) {
      source = new StandardTokenizer();
    } else {
      source = new StandardTokenizer40();
    }
    TokenStream result = new StandardFilter(source);
    result = new LowerCaseFilter(result);
    result = new StopFilter(result, stopwords);
    result = new SetKeywordMarkerFilter(result, exclusionSet);
    result = new GermanNormalizationFilter(result);
    result = new GermanLightStemFilter(result);
    return new TokenStreamComponents(source, result);
  }
}
