package third.hc.server.util.lucene;

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


public class HCAnalyzer {
	public static void main(String[] args) {
		System.out.println(tokenizeString("zh-CN", "洗澡房温度是多少度"));
		System.out.println(tokenizeString("ja", "Hello the 海からの上海の水"));
		System.out.println(tokenizeString("uk", "Hello the звідки ти?"));
		
		final String[] locales = {"ar", "bg", "br", "ca", "cb", "cz", "da", "de", "el", "en", "es", "eu", "fa", "fi", "fr",
				"ga", "gl", "hi", "hu", "hy", "id", "in", "it", "lt", "lv", "nl", "no", "pt", "ro", "ru", "sr", "sv", "th", "tr",
				"zh-CN", "ja", "xx"};
		for (int i = 0; i < locales.length; i++) {
			System.out.println(tokenizeString(locales[i], "中国上海 Helloden the <BR> test123 звідки yang@yahoo.comти?"));
		}
	}
	
	final static HashMap<String, Analyzer> localeAnalyzer = new HashMap<String, Analyzer>(50);
	
	public static List tokenizeString(final String locale_full, String str) {
		String locale = locale_full;
		final int splitIdx = locale_full.indexOf('-', 0);
		if(splitIdx > 0){
			locale = locale_full.substring(0, splitIdx);
		}
		Analyzer analyzer = localeAnalyzer.get(locale_full);
		if(analyzer == null){
			analyzer = buildAnalyzerForLocale(locale, locale_full);
			localeAnalyzer.put(locale_full, analyzer);
		}
		
		List result = new ArrayList();
		try {
			TokenStream stream = analyzer.tokenStream(null, new StringReader(str));
			stream.reset();
			while (stream.incrementToken()) {
				result.add(stream.getAttribute(CharTermAttribute.class).toString());
			}
			stream.reset();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return result;
	}
	
	private static Analyzer buildAnalyzerForLocale(final String locale, final String locale_full){
		if(locale.equals("ar")){
			return new ArabicAnalyzer();
		}else if(locale.equals("bg")){
			return new BulgarianAnalyzer();
		}else if(locale.equals("br")){
			return new BrazilianAnalyzer();
		}else if(locale.equals("ca")){
			return new CatalanAnalyzer();
		}else if(locale.equals("cb") || locale.equals("ckb")){//cb_IQ , ckb_IQ
			return new SoraniAnalyzer();
		}else if(locale.equals("cz")){
			return new CzechAnalyzer();
		}else if(locale.equals("da")){
			return new DanishAnalyzer();
		}else if(locale.equals("de")){
			return new GermanAnalyzer();
		}else if(locale.equals("el")){
			return new GreekAnalyzer();
		}else if(locale.equals("en")){
			return new EnglishAnalyzer();
		}else if(locale.equals("es")){
			return new SpanishAnalyzer();
		}else if(locale.equals("eu")){
			return new BasqueAnalyzer();
		}else if(locale.equals("fa")){
			return new PersianAnalyzer();
		}else if(locale.equals("fi")){
			return new FinnishAnalyzer();
		}else if(locale.equals("fr")){
			return new FrenchAnalyzer();
		}else if(locale.equals("ga")){
			return new IrishAnalyzer();
		}else if(locale.equals("gl")){
			return new GalicianAnalyzer();
		}else if(locale.equals("hi")){
			return new HindiAnalyzer();
		}else if(locale.equals("hu")){
			return new HungarianAnalyzer();
		}else if(locale.equals("hy")){
			return new ArmenianAnalyzer();
		}else if(locale.equals("id")){
			return new IndonesianAnalyzer();
		}else if(locale.equals("in")){
			return new Analyzer(){
				@Override
				protected TokenStreamComponents createComponents(String fieldName) {
					final Tokenizer source = new StandardTokenizer();
				    TokenStream result = new IndicNormalizationFilter(source);
					return new TokenStreamComponents(source, result);
				}
			};
		}else if(locale.equals("it")){
			return new ItalianAnalyzer();
		}else if(locale.equals("lt")){
			return new LithuanianAnalyzer();
		}else if(locale.equals("lv")){
			return new LatvianAnalyzer();
		}else if(locale.equals("nl")){
			return new DutchAnalyzer();
		}else if(locale.equals("no")){
			return new NorwegianAnalyzer();
		}else if(locale.equals("pt")){
			return new PortugueseAnalyzer();
		}else if(locale.equals("ro")){
			return new RomanianAnalyzer();
		}else if(locale.equals("ru")){
			return new RussianAnalyzer();
		}else if(locale.equals("sr")){
			return new Analyzer(){
				@Override
				protected TokenStreamComponents createComponents(String fieldName) {
					final Tokenizer source = new StandardTokenizer();
				    TokenStream result = new SerbianNormalizationFilter(source);
					return new TokenStreamComponents(source, result);
				}
			};
		}else if(locale.equals("sv")){
			return new SwedishAnalyzer();
		}else if(locale.equals("th")){
			return new ThaiAnalyzer();
		}else if(locale.equals("tr")){
			return new TurkishAnalyzer();
		}else if(locale_full.equalsIgnoreCase("zh-CN") || locale_full.equalsIgnoreCase("zh-Hans-CN")){
			return new SmartChineseAnalyzer();
		}else if(locale.equals("zh") || locale.equals("ja") || locale.equals("ko")){
			return new CJKAnalyzer();
		}
		
		return new StandardAnalyzer();
	}
}
