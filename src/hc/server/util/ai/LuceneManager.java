package hc.server.util.ai;

import hc.core.L;
import hc.core.util.LangUtil;
import hc.core.util.LogManager;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import third.apache.lucene.analysis.Analyzer;
import third.apache.lucene.analysis.TokenStream;
import third.apache.lucene.analysis.Tokenizer;
import third.apache.lucene.analysis.ar.ArabicAnalyzer;
import third.apache.lucene.analysis.bg.BulgarianAnalyzer;
import third.apache.lucene.analysis.br.BrazilianAnalyzer;
import third.apache.lucene.analysis.ca.CatalanAnalyzer;
import third.apache.lucene.analysis.cjk.CJKAnalyzer;
import third.apache.lucene.analysis.ckb.SoraniAnalyzer;
import third.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import third.apache.lucene.analysis.cz.CzechAnalyzer;
import third.apache.lucene.analysis.da.DanishAnalyzer;
import third.apache.lucene.analysis.de.GermanAnalyzer;
import third.apache.lucene.analysis.el.GreekAnalyzer;
import third.apache.lucene.analysis.en.EnglishAnalyzer;
import third.apache.lucene.analysis.es.SpanishAnalyzer;
import third.apache.lucene.analysis.eu.BasqueAnalyzer;
import third.apache.lucene.analysis.fa.PersianAnalyzer;
import third.apache.lucene.analysis.fi.FinnishAnalyzer;
import third.apache.lucene.analysis.fr.FrenchAnalyzer;
import third.apache.lucene.analysis.ga.IrishAnalyzer;
import third.apache.lucene.analysis.gl.GalicianAnalyzer;
import third.apache.lucene.analysis.hi.HindiAnalyzer;
import third.apache.lucene.analysis.hu.HungarianAnalyzer;
import third.apache.lucene.analysis.hy.ArmenianAnalyzer;
import third.apache.lucene.analysis.id.IndonesianAnalyzer;
import third.apache.lucene.analysis.in.IndicNormalizationFilter;
import third.apache.lucene.analysis.it.ItalianAnalyzer;
import third.apache.lucene.analysis.lt.LithuanianAnalyzer;
import third.apache.lucene.analysis.lv.LatvianAnalyzer;
import third.apache.lucene.analysis.nl.DutchAnalyzer;
import third.apache.lucene.analysis.no.NorwegianAnalyzer;
import third.apache.lucene.analysis.pt.PortugueseAnalyzer;
import third.apache.lucene.analysis.ro.RomanianAnalyzer;
import third.apache.lucene.analysis.ru.RussianAnalyzer;
import third.apache.lucene.analysis.sr.SerbianNormalizationFilter;
import third.apache.lucene.analysis.standard.StandardAnalyzer;
import third.apache.lucene.analysis.standard.StandardTokenizer;
import third.apache.lucene.analysis.sv.SwedishAnalyzer;
import third.apache.lucene.analysis.th.ThaiAnalyzer;
import third.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import third.apache.lucene.analysis.tr.TurkishAnalyzer;
import third.apache.lucene.util.RamUsageEstimator;

public class LuceneManager {
	public static void init(){
		try{
			final int i = RamUsageEstimator.NUM_BYTES_INT;//init static
		}catch (final Throwable e) {
			e.printStackTrace();
		}
	}
	
	private final static HashMap<String, Analyzer> localeAnalyzer = new HashMap<String, Analyzer>(50);
	
	/**
	 * 
	 * @param locale_full
	 * @param str
	 * @return null 表示没有有效的token
	 */
	public static final List<String> tokenizeString(final String locale_full, final String str) {
		String locale = locale_full;
		final int splitIdx = locale_full.indexOf(LangUtil.LOCALE_SPLIT_CHAR, 0);
		if(splitIdx > 0){
			locale = locale_full.substring(0, splitIdx);
		}

		L.V = L.WShop ? false : LogManager.log("build Analyzer for : " + locale_full);
		
		synchronized (localeAnalyzer) {
			Analyzer analyzer = localeAnalyzer.get(locale_full);
			if(analyzer == null){
				analyzer = buildAnalyzerForLocale(locale, locale_full);
				localeAnalyzer.put(locale_full, analyzer);
			}
			
			List<String> result = null;
			try {
				final TokenStream stream = analyzer.tokenStream(null, new StringReader(str));
				stream.reset();
				while (stream.incrementToken()) {
					if(result == null){
						result = new ArrayList<String>();
					}
					result.add(stream.getAttribute(CharTermAttribute.class).toString());
				}
				stream.reset();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
	
			return result;
		}
	}
	
	private static final Analyzer buildAnalyzerForLocale(final String locale, final String locale_full){
		if(LangUtil.isSameLang(locale, "ar", true)){
			return new ArabicAnalyzer();
		}else if(LangUtil.isSameLang(locale, "bg", true)){
			return new BulgarianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "br", true)){
			return new BrazilianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "ca", true)){
			return new CatalanAnalyzer();
		}else if(LangUtil.isSameLang(locale, "cb", true)){//cb_IQ , ckb_IQ
			return new SoraniAnalyzer();
		}else if(LangUtil.isSameLang(locale, "cz", true)){
			return new CzechAnalyzer();
		}else if(LangUtil.isSameLang(locale, "da", true)){
			return new DanishAnalyzer();
		}else if(LangUtil.isSameLang(locale, "de", true)){
			return new GermanAnalyzer();
		}else if(LangUtil.isSameLang(locale, "el", true)){
			return new GreekAnalyzer();
		}else if(LangUtil.isSameLang(locale, "en", true)){
			return new EnglishAnalyzer();
		}else if(LangUtil.isSameLang(locale, "es", true)){
			return new SpanishAnalyzer();
		}else if(LangUtil.isSameLang(locale, "eu", true)){
			return new BasqueAnalyzer();
		}else if(LangUtil.isSameLang(locale, "fa", true)){
			return new PersianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "fi", true)){
			return new FinnishAnalyzer();
		}else if(LangUtil.isSameLang(locale, "fr", true)){
			return new FrenchAnalyzer();
		}else if(LangUtil.isSameLang(locale, "ga", true)){
			return new IrishAnalyzer();
		}else if(LangUtil.isSameLang(locale, "gl", true)){
			return new GalicianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "hi", true)){
			return new HindiAnalyzer();
		}else if(LangUtil.isSameLang(locale, "hu", true)){
			return new HungarianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "hy", true)){
			return new ArmenianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "id", true)){
			return new IndonesianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "in", true)){
			return new Analyzer(){
				@Override
				protected TokenStreamComponents createComponents(final String fieldName) {
					final Tokenizer source = new StandardTokenizer();
				    final TokenStream result = new IndicNormalizationFilter(source);
					return new TokenStreamComponents(source, result);
				}
			};
		}else if(LangUtil.isSameLang(locale, "it", true)){
			return new ItalianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "lt", true)){
			return new LithuanianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "lv", true)){
			return new LatvianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "nl", true)){
			return new DutchAnalyzer();
		}else if(LangUtil.isSameLang(locale, "no", true)){
			return new NorwegianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "pt", true)){
			return new PortugueseAnalyzer();
		}else if(LangUtil.isSameLang(locale, "ro", true)){
			return new RomanianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "ru", true)){
			return new RussianAnalyzer();
		}else if(LangUtil.isSameLang(locale, "sr", true)){
			return new Analyzer(){
				@Override
				protected TokenStreamComponents createComponents(final String fieldName) {
					final Tokenizer source = new StandardTokenizer();
				    final TokenStream result = new SerbianNormalizationFilter(source);
					return new TokenStreamComponents(source, result);
				}
			};
		}else if(LangUtil.isSameLang(locale, "sv", true)){
			return new SwedishAnalyzer();
		}else if(LangUtil.isSameLang(locale, "th", true)){
			return new ThaiAnalyzer();
		}else if(LangUtil.isSameLang(locale, "tr", true)){
			return new TurkishAnalyzer();
		}else if(locale_full.equals("zh") == false 
				&& (LangUtil.isSameLang(locale_full, "zh-CN", true) || LangUtil.isSameLang(locale_full, "zh-Hans", true))){
			return new SmartChineseAnalyzer();
		}else if(LangUtil.isSameLang(locale, "zh", true) || LangUtil.isSameLang(locale, "ja", true) || LangUtil.isSameLang(locale, "ko", true)){
			return new CJKAnalyzer();
		}
		
		return new StandardAnalyzer();
	}

}
