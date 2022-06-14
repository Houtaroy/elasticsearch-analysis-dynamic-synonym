package com.bellszhu.elasticsearch.plugin.synonym.analysis;

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

    public static SynonymParserFactory getParserFactory(String format) {
        return "wordnet".equalsIgnoreCase(format) ? ESWordnetSynonymParser::new : ESSolrSynonymParser::new;
    }
}
