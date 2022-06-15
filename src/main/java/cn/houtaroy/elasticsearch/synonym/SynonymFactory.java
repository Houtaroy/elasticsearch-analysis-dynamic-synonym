package cn.houtaroy.elasticsearch.synonym;

import org.apache.lucene.analysis.Analyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author Houtaroy
 */
public final class SynonymFactory {
  private SynonymFactory() {
  }

  public static final Map<String, BiFunction<SynonymProperties, Analyzer, SynonymFile>> creators;

  static {
    creators = new HashMap<>();
    creators.put("jdbc", JdbcSynonym::new);
    creators.put("file", LocalSynonymFile::new);
    creators.put("remote", RemoteSynonymFile::new);
  }

  public static SynonymFile create(SynonymProperties properties, Analyzer analyzer) {
    return creators.get(properties.getSynonymType()).apply(properties, analyzer);
  }
}
