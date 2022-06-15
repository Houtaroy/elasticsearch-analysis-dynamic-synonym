package cn.houtaroy.elasticsearch.synonym;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.AnalysisMode;
import org.elasticsearch.index.analysis.CharFilterFactory;
import org.elasticsearch.index.analysis.CustomAnalyzer;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author bellszhu
 */
public class DynamicSynonymFilterFactory extends AbstractTokenFilterFactory {
  private static final Logger logger = LogManager.getLogger("dynamic-synonym");
  private static final AtomicInteger id = new AtomicInteger(1);
  private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(1, r -> {
    Thread thread = new Thread(r);
    thread.setName("monitor-synonym-Thread-" + id.getAndAdd(1));
    return thread;
  });
  protected final Environment environment;
  protected final AnalysisMode analysisMode;
  protected final SynonymProperties properties;
  protected Analyzer analyzer;

  public DynamicSynonymFilterFactory(
    IndexSettings indexSettings,
    Environment env,
    String name,
    Settings settings
  ) {
    super(indexSettings, name, settings);
    this.properties = new SynonymProperties(name, env, settings);
    this.analysisMode = settings.getAsBoolean("updateable", false) ?
      AnalysisMode.SEARCH_TIME : AnalysisMode.ALL;
    this.environment = env;
  }

  @Override
  public TokenStream create(TokenStream tokenStream) {
    SynonymFile synonym = SynonymFactory.create(properties, analyzer);
    SynonymMap synonymMap = synonym.reloadSynonymMap();
    if (synonymMap.fst == null) {
      return tokenStream;
    }
    IDynamicSynonymFilter result = createSynonymFilter(tokenStream, synonymMap);
    SynonymMonitorManager.enable(synonym, result);
    return result;
  }

  @Override
  public AnalysisMode getAnalysisMode() {
    return this.analysisMode;
  }

  @Override
  public TokenFilterFactory getSynonymFilter() {
    return IDENTITY_FILTER;
  }

  public TokenFilterFactory getChainAwareTokenFilterFactory(
    TokenizerFactory tokenizer,
    List<CharFilterFactory> charFilters,
    List<TokenFilterFactory> previousTokenFilters,
    Function<String, TokenFilterFactory> allFilters
  ) {
    analyzer = buildSynonymAnalyzer(tokenizer, charFilters, previousTokenFilters, allFilters);
    return super.getChainAwareTokenFilterFactory(tokenizer, charFilters, previousTokenFilters, allFilters);
  }

  Analyzer buildSynonymAnalyzer(
    TokenizerFactory tokenizer,
    List<CharFilterFactory> charFilters,
    List<TokenFilterFactory> tokenFilters,
    Function<String, TokenFilterFactory> allFilters
  ) {
    return new CustomAnalyzer(
      tokenizer,
      charFilters.toArray(new CharFilterFactory[0]),
      tokenFilters.stream().map(TokenFilterFactory::getSynonymFilter).toArray(TokenFilterFactory[]::new)
    );
  }

  /**
   * create synonym filter
   * you can override this method to create your own synonym filter
   *
   * @param input    input
   * @param synonyms synonyms
   * @return synonym filter
   */
  protected IDynamicSynonymFilter createSynonymFilter(TokenStream input, SynonymMap synonyms) {
    return new DynamicSynonymFilter(input, synonyms, false);
  }
}