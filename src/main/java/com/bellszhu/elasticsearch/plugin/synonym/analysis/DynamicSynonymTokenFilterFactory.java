package com.bellszhu.elasticsearch.plugin.synonym.analysis;


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
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author bellszhu
 */
public class DynamicSynonymTokenFilterFactory extends AbstractTokenFilterFactory {
    private static final Logger logger = LogManager.getLogger("dynamic-synonym");
    private static final AtomicInteger id = new AtomicInteger(1);
    private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r);
        thread.setName("monitor-synonym-Thread-" + id.getAndAdd(1));
        return thread;
    });
    private volatile ScheduledFuture<?> scheduledFuture;
    private final int interval;
    protected SynonymMap synonymMap;
    protected Map<AbsSynonymFilter, Integer> dynamicSynonymFilters = new WeakHashMap<>();
    protected final Environment environment;
    protected final AnalysisMode analysisMode;
    protected final SynonymProperties properties;

    public DynamicSynonymTokenFilterFactory(
            IndexSettings indexSettings,
            Environment env,
            String name,
            Settings settings
    ) {
        super(indexSettings, name, settings);
        this.properties = new SynonymProperties(env, settings);
        this.interval = settings.getAsInt("interval", 60);
        this.analysisMode = settings.getAsBoolean("updateable", false) ?
                AnalysisMode.SEARCH_TIME : AnalysisMode.ALL;
        this.environment = env;
    }

    @Override
    public AnalysisMode getAnalysisMode() {
        return this.analysisMode;
    }


    @Override
    public TokenStream create(TokenStream tokenStream) {
        throw new IllegalStateException(
                "Call getChainAwareTokenFilterFactory to specialize this factory for an analysis chain first");
    }

    public TokenFilterFactory getChainAwareTokenFilterFactory(
            TokenizerFactory tokenizer,
            List<CharFilterFactory> charFilters,
            List<TokenFilterFactory> previousTokenFilters,
            Function<String, TokenFilterFactory> allFilters
    ) {
        final Analyzer analyzer = buildSynonymAnalyzer(tokenizer, charFilters, previousTokenFilters, allFilters);
        synonymMap = buildSynonyms(analyzer);
        final String name = name();
        return new TokenFilterFactory() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public TokenStream create(TokenStream tokenStream) {
                // fst is null means no synonyms
                if (synonymMap.fst == null) {
                    return tokenStream;
                }
                AbsSynonymFilter synonymFilter = createSynonymFilter(tokenStream, synonymMap);
                dynamicSynonymFilters.put(synonymFilter, 1);

                return synonymFilter;
            }

            @Override
            public TokenFilterFactory getSynonymFilter() {
                // In order to allow chained synonym filters, we return IDENTITY here to
                // ensure that synonyms don't get applied to the synonym map itself,
                // which doesn't support stacked input tokens
                return IDENTITY_FILTER;
            }

            @Override
            public AnalysisMode getAnalysisMode() {
                return analysisMode;
            }
        };
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

    SynonymMap buildSynonyms(Analyzer analyzer) {
        try {
            return getSynonymFile(analyzer).reloadSynonymMap();
        } catch (Exception e) {
            logger.error("failed to build synonyms", e);
            throw new IllegalArgumentException("failed to build synonyms", e);
        }
    }

    SynonymFile getSynonymFile(Analyzer analyzer) {
        try {
            SynonymFile synonymFile = SynonymFactory.create(properties, analyzer);
            monitor(synonymFile, interval);
            return synonymFile;
        } catch (Exception e) {
            logger.error("failed to get synonyms from uri: {}", properties.getUri(), e);
            throw new IllegalArgumentException("failed to get synonyms : " + properties.getUri(), e);
        }
    }

    /**
     * create synonym filter
     * you can override this method to create your own synonym filter
     *
     * @param input    input
     * @param synonyms synonyms
     * @return synonym filter
     */
    protected AbsSynonymFilter createSynonymFilter(TokenStream input, SynonymMap synonyms) {
        return new DynamicSynonymFilter(input, synonyms, false);
    }

    /**
     * start monitor
     *
     * @param synonym  synonym
     * @param interval interval
     */
    protected void monitor(SynonymFile synonym, int interval) {
        if (scheduledFuture == null) {
            scheduledFuture = pool.scheduleAtFixedRate(new Monitor(synonym), interval, interval, TimeUnit.SECONDS);
        }
    }

    public class Monitor implements Runnable {

        private final SynonymFile synonymFile;

        Monitor(SynonymFile synonymFile) {
            this.synonymFile = synonymFile;
        }

        @Override
        public void run() {
            if (synonymFile.isNeedReloadSynonymMap()) {
                synonymMap = synonymFile.reloadSynonymMap();
                for (AbsSynonymFilter dynamicSynonymFilter : dynamicSynonymFilters.keySet()) {
                    dynamicSynonymFilter.update(synonymMap);
                    logger.info("success reload synonym");
                }
            }
        }
    }
}