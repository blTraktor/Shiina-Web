package dev.osunolimits.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.main.App;
import dev.osunolimits.utils.Validation;
import lombok.Data;

@Data
public final class MySQL implements AutoCloseable{

	private static Logger log = (Logger) LoggerFactory.getLogger("MySQL");

	public long connectionCreated;
	public String caller;
	private Connection currentCon;

	private final int COLUMN_WIDTH = 20;

	public MySQL(Connection currentCon) {
		open(currentCon);
	}

	public synchronized void open(Connection currentCon) {
		this.connectionCreated = System.currentTimeMillis();
		caller = Thread.currentThread().getStackTrace()[4].getClassName();
		Database.runningConnections.add(this);
		this.currentCon = currentCon;
	}

	public synchronized void close() {
		try {
			if (!currentCon.isClosed()) {
				Database.currentConnections--;
				Database.runningConnections.remove(this);
				currentCon.close();
				currentCon = null;
			}
		} catch (Exception ex) {
			logSQL("Failed to close connection");
		}

	}

    private void printResultSet(ResultSet resultSet) {
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                System.out.printf("%-" + COLUMN_WIDTH + "s", columnName);
            }
            System.out.println();

            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnValue = resultSet.getString(i);
                    System.out.printf("%-" + COLUMN_WIDTH + "s", columnValue);
                }
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

	public ResultSet Query(String sql, Object... args) {
		try {
			PreparedStatement stmt = currentCon.prepareStatement(sql);
			for (int i = 0; i < args.length; i++)
                if(Validation.isNumeric(args[i].toString())) {
                    stmt.setInt(i + 1, Integer.parseInt(args[i].toString()));
                } else {
                    stmt.setString(i + 1, (String) args[i]);
                }

			logSQL(stmt.toString());
			return stmt.executeQuery();
		} catch (Exception ex) {

			return null;
		}
	}

	public void printQuery(String sql, String... args) {
		ResultSet rs = Query(sql, (Object[]) args);
		printResultSet(rs);
	}

	public ResultSet Query(String sql, List<String> args) {
		try {
			PreparedStatement stmt = currentCon.prepareStatement(sql);
			for (int i = 0; i < args.size(); i++)
				stmt.setString(i + 1, args.get(i));
                logSQL(stmt.toString());
			return stmt.executeQuery();
		} catch (Exception ex) {

			return null;
		}
	}

	public int Exec(String sql, Object... args) {
		try {
			PreparedStatement stmt = currentCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof String) {
					stmt.setString(i + 1, (String) args[i]);
				} else if (args[i] instanceof Integer) {
					stmt.setInt(i + 1, (Integer) args[i]);
				} else if (args[i] instanceof Long) {
					stmt.setLong(i + 1, (Long) args[i]);
				} else if (args[i] instanceof Boolean) {
					stmt.setBoolean(i + 1, (Boolean) args[i]);
				} else if (args[i] instanceof Double) {
					stmt.setDouble(i + 1, (Double) args[i]);
				} else {
					throw new IllegalArgumentException("Unsupported parameter type: " + args[i].getClass());
				}
			}
	
			int rowsAffected = stmt.executeUpdate();
			logSQL(stmt.toString());
	
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				int generatedKey = rs.getInt(1);
				return generatedKey;
			} else {
				return rowsAffected;
			}
		} catch (Exception ex) {
			log.error("MySQL Exec Error: " + ex.getMessage(), ex);
			return -1;
		}
	}	

    private void logSQL(String message) {
        if(App.loggerEnv.get("MYSQL_LOG").equalsIgnoreCase("TRUE")) {
            log.info(message);
        }
    }
}