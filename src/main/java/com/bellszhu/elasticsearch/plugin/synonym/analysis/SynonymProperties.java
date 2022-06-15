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
    protected String driverClassName;
    protected String username;
    protected String password;
    protected String synonymSql;
    protected String versionSql;

    public SynonymProperties(Environment env, Settings settings) {
        this.env = env;
        expand = settings.getAsBoolean("expand", true);
        lenient = settings.getAsBoolean("lenient", false);
        format = settings.get("format", "");
        synonymType = settings.get("synonym_type", DEFAULT_SYNONYM_TYPE);
        uri = settings.get("uri");
        interval = settings.getAsInt("interval", determineInterval(synonymType));
        driverClassName = settings.get("driver_class_name");
        username = settings.get("username");
        password = settings.get("password");
        synonymSql = settings.get("synonym_sql", DEFAULT_SYNONYM_SQL);
        versionSql = settings.get("version_sql", DEFAULT_VERSION_SQL);
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
