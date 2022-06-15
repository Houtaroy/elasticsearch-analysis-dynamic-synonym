package cn.houtaroy.elasticsearch.synonym;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.elasticsearch.analysis.common.ESSolrSynonymParser;
import org.elasticsearch.analysis.common.ESWordnetSynonymParser;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

/**
 * @author Houtaroy
 */
public final class SynonymMapHelper {
  private SynonymMapHelper() {
  }

  /**
   * parse synonym map
   *
   * @param reader     reader
   * @param properties properties
   * @param analyzer   analyzer
   * @return synonym map
   * @throws IOException    IOException
   * @throws ParseException ParseException
   */
  public static SynonymMap parse(Reader reader, SynonymProperties properties, Analyzer analyzer)
    throws IOException, ParseException {
    SynonymMap.Parser parser = getParserFactory(properties.getFormat()).create(
      true,
      properties.isExpand(),
      properties.isLenient(),
      analyzer
    );
    parser.parse(reader);
    return parser.build();

  }

  /**
   * get synonym parser factory
   *
   * @param format format
   * @return synonym parser factory
   */
  public static SynonymParserFactory getParserFactory(String format) {
    return "wordnet".equalsIgnoreCase(format) ? ESWordnetSynonymParser::new : ESSolrSynonymParser::new;
  }
}
