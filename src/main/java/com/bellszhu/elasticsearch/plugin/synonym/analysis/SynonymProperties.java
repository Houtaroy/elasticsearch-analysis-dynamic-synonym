package com.bellszhu.elasticsearch.plugin.synonym.analysis;

import lombok.Getter;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

/**
 * Synonym Properties
 *
 * @author Houtaroy
 */
@Getter
public class SynonymProperties {
    private static final String DEFAULT_SYNONYM_SQL = "select synonym from t_synonym";
    private static final String DEFAULT_SYNONYM_TYPE = "file";
    private static final String DEFAULT_VERSION_SQL = "select version from t_synonym_version";
    protected Environment env;
    protected boolean expand;
    protected boolean lenient;
    protected String format;
    protected String synonymType;
    protected String uri;
    protected int interval;
    protected String username;
    protected String password;
    protected String synonymSql;
    protected String versionSql;

    public SynonymProperties(Environment env, Settings settings) {
        this.env = env;
        this.expand = settings.getAsBoolean("expand", true);
        this.lenient = settings.getAsBoolean("lenient", false);
        this.format = settings.get("format", "");
        this.synonymType = settings.get("synonymType", DEFAULT_SYNONYM_TYPE);
        this.uri = settings.get("uri");
        this.interval = settings.getAsInt("interval", determineInterval(synonymType));
        this.username = settings.get("username");
        this.password = settings.get("password");
        this.synonymSql = settings.get("synonymSql", DEFAULT_SYNONYM_SQL);
        this.versionSql = settings.get("versionSql", DEFAULT_VERSION_SQL);
    }

    /**
     * determine the interval of synonym
     * if the synonymType is jdbc, the interval is 3600, else 60
     *
     * @param synonymType synonymType
     * @return interval
     */
    private int determineInterval(String synonymType) {
        return "jdbc".equals(synonymType) ? Constants.SECONDS_PER_HOUR : Constants.SECONDS_PER_MINUTE;
    }
}
