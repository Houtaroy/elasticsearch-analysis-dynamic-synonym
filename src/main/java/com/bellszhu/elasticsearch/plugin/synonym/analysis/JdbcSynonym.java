package com.bellszhu.elasticsearch.plugin.synonym.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.elasticsearch.SpecialPermission;

import java.io.Reader;
import java.io.StringReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC Synonym
 *
 * @author Houtaroy
 */
public class JdbcSynonym implements SynonymFile {
    private static final Logger logger = LogManager.getLogger("dynamic-synonym");
    protected final SynonymProperties properties;
    protected final Analyzer analyzer;
    protected int version;

    JdbcSynonym(SynonymProperties properties, Analyzer analyzer) {
        this.properties = properties;
        this.analyzer = analyzer;
        loadDriverClass(properties.getDriverClassName());
        intervalTooSmallWarning();
    }

    @Override
    public SynonymMap reloadSynonymMap() {
        try {
            logger.info("start reload jdbc synonym from {}", properties.getUri());
            return SynonymMapHelper.parse(getReader(), properties, analyzer);
        } catch (Exception e) {
            logger.error("reload jdbc synonym {} error!", properties.getUri(), e);
            throw new IllegalArgumentException("could not reload jdbc synonyms to build synonyms", e);
        }
    }

    @Override
    public boolean isNeedReloadSynonymMap() {
        int persistence = getVersion();
        if (persistence <= 0) {
            return false;
        }
        boolean result = persistence > version;
        version = persistence;
        return result;
    }

    @Override
    public Reader getReader() {
        SpecialPermission.check();
        return AccessController.doPrivileged((PrivilegedAction<Reader>) () -> {
            try (Connection connection = getConnection();
                 ResultSet rs = connection.prepareStatement(properties.getSynonymSql()).executeQuery()) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    sb.append(rs.getString(1)).append(System.getProperty("line.separator"));
                }
                return new StringReader(sb.toString());
            } catch (SQLException e) {
                logger.error("get jdbc synonyms error!", e);
                throw new IllegalArgumentException("could not get jdbc synonyms", e);
            }
        });
    }

    /**
     * get version
     *
     * @return version, example: 1
     */
    protected int getVersion() {
        SpecialPermission.check();
        return AccessController.doPrivileged((PrivilegedAction<Integer>) () -> {
            try (Connection connection = getConnection();
                 ResultSet rs = connection.prepareStatement(properties.getVersionSql()).executeQuery()) {
                rs.next();
                return rs.getInt(1);
            } catch (SQLException e) {
                logger.error("get jdbc synonym version error", e);
                return -1;
            }
        });
    }

    /**
     * get JDBC connection
     *
     * @return JDBC connection
     * @throws SQLException SQLException
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(properties.getUri(), properties.getUsername(), properties.getPassword());
    }

    /**
     * interval too small warning
     * if the interval is less than 3600 seconds, we will warn the user
     */
    protected void intervalTooSmallWarning() {
        if (properties.getInterval() < Constants.SECONDS_PER_HOUR) {
            logger.warn(
                    "The interval of jdbc synonym is less than {}, it may cause performance issue",
                    Constants.SECONDS_PER_HOUR
            );
        }
    }

    /**
     * load jdbc driver class
     *
     * @param className jdbc driver class name
     */
    protected void loadDriverClass(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.error("could not load jdbc driver {}", className, e);
            throw new IllegalArgumentException("could not load jdbc driver class", e);
        }
    }
}
