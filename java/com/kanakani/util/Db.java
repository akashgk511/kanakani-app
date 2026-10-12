package com.kanakani.util;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gets connections from the container's connection pool (JNDI name jdbc/kanakani, defined in
 * config/context.xml). Passwords therefore live in the server configuration, not in the code.
 */
public final class Db {

    private static volatile DataSource dataSource;

    private Db() { }

    private static DataSource ds() {
        DataSource d = dataSource;
        if (d == null) {
            synchronized (Db.class) {
                d = dataSource;
                if (d == null) {
                    try {
                        d = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/kanakani");
                    } catch (NamingException e) {
                        throw new IllegalStateException("Database pool jdbc/kanakani is not configured", e);
                    }
                    dataSource = d;
                }
            }
        }
        return d;
    }

    public static Connection connection() throws SQLException {
        return ds().getConnection();
    }
}
