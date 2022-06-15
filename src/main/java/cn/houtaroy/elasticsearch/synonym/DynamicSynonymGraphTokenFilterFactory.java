package cn.houtaroy.elasticsearch.synonym;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

public class DynamicSynonymGraphTokenFilterFactory extends DynamicSynonymTokenFilterFactory {

    public DynamicSynonymGraphTokenFilterFactory(
            IndexSettings indexSettings, Environment env, String name, Settings settings
    ) {
        super(indexSettings, env, name, settings);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        throw new IllegalStateException(
                "Call createPerAnalyzerSynonymGraphFactory to specialize this factory for an analysis chain first"
        );
    }

    @Override
    protected AbsSynonymFilter createSynonymFilter(TokenStream input, SynonymMap synonyms) {
        return new DynamicSynonymGraphFilter(input, synonyms, false);
    }
}
