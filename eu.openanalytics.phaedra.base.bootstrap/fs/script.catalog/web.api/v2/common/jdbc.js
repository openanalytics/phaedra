jdbc = {};

/**
 * Create a JDBC connection.
 */
jdbc.connect = function(url, username, password) {
    if (password == null) {
      env = Java.type("eu.openanalytics.phaedra.base.environment.Screening").getEnvironment();
      try {
        password = env.getConfig().resolvePassword(username);
      } catch (e) {
        password = env.resolvePassword(username);
      }
    }
    Java.type("eu.openanalytics.phaedra.base.db.JDBCUtils").checkDbType(url);
    dm = Java.type("java.sql.DriverManager");
    conn = dm.getConnection(url, username, password);
    return conn;
};

/**
 * Get a JDBC connection from the Phaedra connection pool.
 */
jdbc.getPhaedraConnection = function() {
    env = Java.type("eu.openanalytics.phaedra.base.environment.Screening").getEnvironment();
    return env.getJDBCConnection();
}

/**
 * Execute a query on the given connection.
 */
jdbc.runQuery = function(connection, query, resultConsumer) {
    stmt = connection.createStatement();
    try {
        rs = stmt.executeQuery(query);
        try {
            resultConsumer(rs);
        } finally {
            rs.close();
        }
    } finally {
        stmt.close();
    }
};

/**
 * Execute a statement that doesn't return a resultset,
 * such as an INSERT, UPDATE, DELETE or DDL.
 */
jdbc.runStatement = function(connection, statement) {
    stmt = connection.createStatement();
    try {
        stmt.executeUpdate(statement);
    } finally {
        stmt.close();
    }
};

/**
 * Convert a value into a quoted string value.
 * If the value is null, it is not quoted.
 */
jdbc.quote = function(value) {
    if (value == null) return value;
    else return "'" + value + "'";
};

/**
 * Convert a date object (Timestamp or Date) to an Oracle date string.
 * Null values are not altered.
 */
jdbc.dateToString = function(timestamp) {
    if (timestamp == null) return timestamp;
    var formatter = new java.text.SimpleDateFormat("dd-MM-YYYY");
    return "to_date('" + formatter.format(timestamp) + "', 'DD-MM-YYYY')";
};

/**
 * Remove all line breaks from a string.
 */
jdbc.removeNewlines = function(value) {
    if (value == null) return value;
    else return value.replace(/(?:\r\n|\r|\n)/g, ' ');
};