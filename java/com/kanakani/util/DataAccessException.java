package com.kanakani.util;

import java.sql.SQLException;

/** Wraps SQLException so the data layer does not force try/catch on every caller. */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final String UNIQUE_VIOLATION = "23505";

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
    }

    /** True when the database refused a duplicate, for example an alias that is already taken. */
    public boolean isUniqueViolation() {
        return getCause() instanceof SQLException e && UNIQUE_VIOLATION.equals(e.getSQLState());
    }
}
