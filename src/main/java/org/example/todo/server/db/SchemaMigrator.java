package org.example.todo.server.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


public class SchemaMigrator {
    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    private final DataSourceProvider dsp;

    public SchemaMigrator(DataSourceProvider dsp) {
        this.dsp = dsp;
    }

    public void ensureSchema() throws SQLException {
        try (Connection c = dsp.getConnection()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                st.addBatch("PRAGMA foreign_keys = ON");

                st.addBatch("CREATE TABLE IF NOT EXISTS users (" +
                        "id TEXT PRIMARY KEY, " +
                        "username TEXT UNIQUE NOT NULL, " +
                        "password_hash TEXT NOT NULL, " +
                        "password_salt TEXT NOT NULL, " +
                        "created_at INTEGER NOT NULL" +
                        ")");

                st.addBatch("CREATE TABLE IF NOT EXISTS boards (" +
                        "id TEXT PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                        "created_at INTEGER NOT NULL" +
                        ")");

                st.addBatch("CREATE TABLE IF NOT EXISTS board_members (" +
                        "board_id TEXT NOT NULL REFERENCES boards(id) ON DELETE CASCADE, " +
                        "user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                        "role TEXT NOT NULL CHECK (role IN ('OWNER','MEMBER')), " +
                        "created_at INTEGER NOT NULL, " +
                        "PRIMARY KEY (board_id, user_id)" +
                        ")");

                st.addBatch("CREATE TABLE IF NOT EXISTS tasks (" +
                        "id TEXT PRIMARY KEY, " +
                        "board_id TEXT NOT NULL REFERENCES boards(id) ON DELETE CASCADE, " +
                        "title TEXT NOT NULL, " +
                        "description TEXT, " +
                        "status TEXT NOT NULL CHECK (status IN ('TODO','IN_PROGRESS','DONE')), " +
                        "priority TEXT NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH')), " +
                        "due_date INTEGER, " +
                        "created_at INTEGER NOT NULL" +
                        ")");

                st.addBatch("CREATE INDEX IF NOT EXISTS idx_tasks_board_created ON tasks(board_id, created_at)");
                st.addBatch("CREATE INDEX IF NOT EXISTS idx_tasks_board_due ON tasks(board_id, due_date)");
                st.addBatch("CREATE INDEX IF NOT EXISTS idx_tasks_board_priority ON tasks(board_id, priority)");

                st.addBatch("CREATE TABLE IF NOT EXISTS sessions (" +
                        "jti TEXT PRIMARY KEY, " +
                        "user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                        "expires_at INTEGER NOT NULL, " +
                        "created_at INTEGER NOT NULL" +
                        ")");

                st.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
        log.info("Schema ensured.");
    }
}
