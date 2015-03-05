package edu.illinois.i3.emop.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import edu.illinois.i3.emop.apps.dbspellcheck.Constants;


public class DBUtils {

	private static final Logger log = LoggerFactory.getLogger(DBUtils.class);

	public static BoneCP createDBConnectionPool(String dbDriver, String dbUrl, String dbUser, String dbPasswd) throws SQLException {
		try {
			Class.forName(dbDriver);
		}
		catch (Exception e) {
			log.error("Could not load the JDBC driver: " + dbDriver, e);
			throw new SQLException(e);
		}

		BoneCPConfig config = new BoneCPConfig();
		config.setJdbcUrl(dbUrl);
		config.setMinConnectionsPerPartition(Constants.BONECP_MIN_CONN_PER_PART);
		config.setMaxConnectionsPerPartition(Constants.BONECP_MAX_CONN_PER_PART);
		config.setPartitionCount(Constants.BONECP_PARTITION_COUNT);
		config.setUsername(dbUser);
		config.setPassword(dbPasswd);

		return new BoneCP(config);
	}

	/**
     * Rolls back the last DB transaction for a connection
     *
     * @param connection The connection
     * @return True if success / False otherwise
     */
    public static boolean rollbackTransaction(Connection connection) {
        if (connection == null) return false;

        try {
            connection.rollback();
            return true;
        }
        catch (SQLException e) {
            log.warn("Error rolling back DB transaction!", e);
            return false;
        }
    }

    /**
     * Returns a connection back to the connection pool
     *
     * @param connection The connection
     * @param statements  (Optional) Any ResultSet(s) that need to be closed before the connection is released
     */
    public static void releaseConnection(Connection connection, Statement... statements) {
        if (statements != null)
            for (Statement stmt : statements)
                closeStatement(stmt);

        if (connection != null) {
            try {
                connection.close();
            }
            catch (Exception e) {
                log.warn("Error releasing DB connection!", e);
            }
        }
    }

    /**
     * Closes a Statement
     *
     * @param stmt The Statement
     */
    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            }
            catch (SQLException e) {
                log.warn("Error closing DB statement!", e);
            }
        }
    }

}
