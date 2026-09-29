package de.unijena.bioinf.ms.biotransformer;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Lets BioTransformer's HMDB lookup run on the MariaDB client instead of MySQL Connector/J.
 * <p>
 * BioTransformer opens its connections with {@code DriverManager.getConnection("jdbc:mysql://...")},
 * and those URLs are constants compiled into the library, so they cannot be pointed somewhere else.
 * Connector/J is GPLv2; the MariaDB client is LGPL-2.1-or-later and speaks the same wire protocol,
 * but it only accepts the {@code jdbc:mysql:} scheme when the connection string asks it to.
 * <p>
 * This driver claims {@code jdbc:mysql:} URLs, rewrites them and hands them to the MariaDB driver.
 * It is registered through {@code META-INF/services/java.sql.Driver}, and it is the only thing in
 * SIRIUS that uses JDBC - nothing else here talks to a relational database.
 * <p>
 * These connections happen only when the BioTransformer "useDB" option is switched on, which is off
 * by default on every interface.
 * <p>
 * Verified against the HMDB server BioTransformer ships with: it is MariaDB 10.3.10, so the MariaDB
 * client authenticates without trouble. Note that the session comes back with an empty
 * {@code Ssl_cipher}, i.e. it is not encrypted. PathBank is not reachable this way - BioTransformer
 * passes an empty user name for it and the client substitutes the OS user - but nothing in SIRIUS
 * selects PathBank; every connection goes to HMDB.
 */
@Slf4j
public class MySqlSchemeDriver implements Driver {
    private static final String MYSQL_PREFIX = "jdbc:mysql:";
    private static final String MARIADB_PREFIX = "jdbc:mariadb:";
    /** Without this the MariaDB driver rejects anything that is not jdbc:mariadb:. */
    private static final String PERMIT_MYSQL_SCHEME = "permitMysqlScheme";

    private final Driver delegate;

    public MySqlSchemeDriver() {
        this(new org.mariadb.jdbc.Driver());
    }

    MySqlSchemeDriver(Driver delegate) {
        this.delegate = delegate;
    }

    static {
        try {
            DriverManager.registerDriver(new MySqlSchemeDriver());
        } catch (SQLException e) {
            log.error("Could not register the MySQL-scheme JDBC driver. "
                    + "BioTransformer's HMDB lookup will not be able to connect.", e);
        }
    }

    /**
     * {@code jdbc:mysql://host/db} becomes {@code jdbc:mariadb://host/db?permitMysqlScheme}, keeping
     * any parameters the caller already supplied.
     */
    static String toMariaDbUrl(String url) {
        if (url == null || !url.startsWith(MYSQL_PREFIX))
            return url;
        String rewritten = MARIADB_PREFIX + url.substring(MYSQL_PREFIX.length());
        if (rewritten.contains(PERMIT_MYSQL_SCHEME))
            return rewritten;
        return rewritten + (rewritten.indexOf('?') < 0 ? "?" : "&") + PERMIT_MYSQL_SCHEME;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url))
            return null;    // the contract: not ours, let DriverManager try the next one
        return delegate.connect(toMariaDbUrl(url), info);
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith(MYSQL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return delegate.getPropertyInfo(toMariaDbUrl(url), info);
    }

    @Override
    public int getMajorVersion() {
        return delegate.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return delegate.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return delegate.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }
}
