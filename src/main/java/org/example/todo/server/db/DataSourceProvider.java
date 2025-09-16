package org.example.todo.server.db;

import org.example.todo.server.app.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DataSourceProvider {
    private static final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);

    private final String jdbcUrl;

    public DataSourceProvider(ServerConfig config) {
        this.jdbcUrl = config.getJdbcUrl();
        ensureDbDirectory(jdbcUrl);
    }

    private void ensureDbDirectory(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            String path = jdbcUrl.substring("jdbc:sqlite:".length());
            File f = new File(path).getAbsoluteFile();
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) {
                boolean ok = dir.mkdirs();
                if (!ok) log.warn("Could not create DB directory: {}", dir);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}
