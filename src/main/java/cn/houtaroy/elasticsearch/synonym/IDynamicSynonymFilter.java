package cn.houtaroy.elasticsearch.synonym;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.synonym.SynonymMap;

/**
 * @author bellszhu
 */
public abstract class IDynamicSynonymFilter extends TokenFilter {
  /**
   * Construct a token stream filtering the given input.
   */
  protected IDynamicSynonymFilter(TokenStream input) {
    super(input);
  }

  abstract void update(SynonymMap synonymMap);
}
