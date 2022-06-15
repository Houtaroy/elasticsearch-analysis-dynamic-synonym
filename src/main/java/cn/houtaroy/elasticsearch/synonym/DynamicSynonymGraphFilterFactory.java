package cn.houtaroy.elasticsearch.synonym;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

public class DynamicSynonymGraphFilterFactory extends DynamicSynonymFilterFactory {

  public DynamicSynonymGraphFilterFactory(
    IndexSettings indexSettings, Environment env, String name, Settings settings
  ) {
    super(indexSettings, env, name, settings);
  }

  @Override
  protected IDynamicSynonymFilter createSynonymFilter(TokenStream input, SynonymMap synonyms) {
    return new DynamicSynonymGraphFilter(input, synonyms, false);
  }
}
