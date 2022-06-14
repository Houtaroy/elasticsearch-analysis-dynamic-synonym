package com.bellszhu.elasticsearch.plugin.synonym.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;

/**
 * @author Houtaroy
 */
public interface SynonymParserFactory {
    SynonymMap.Parser create(boolean dedup, boolean expand, boolean lenient, Analyzer analyzer);
}
