package database;

import com.mysql.jdbc.Statement;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import server.ServerProperties;

public class DatabaseConnection {

    public static final int CLOSE_CURRENT_RESULT = 1;
    public static final int KEEP_CURRENT_RESULT = 2;
    public static final int CLOSE_ALL_RESULTS = 3;
    public static final int SUCCESS_NO_INFO = -2;
    public static final int EXECUTE_FAILED = -3;
    public static final int RETURN_GENERATED_KEYS = 1;
    public static final int NO_GENERATED_KEYS = 2;
    private static final HashMap<Integer, ConWrapper> connections = new HashMap();
    private static String dbUrl;
    private static String dbUser;
    private static String dbPass;
    private static boolean propsInited = false;
    private static long connectionTimeOut = 300000L;

    public static Connection getConnection() {
        Thread cThread = Thread.currentThread();
        int threadID = (int) cThread.getId();
        ConWrapper ret = (ConWrapper) connections.get(Integer.valueOf(threadID));
        if (ret == null) {
            Connection retCon = connectToDB();
            ret = new ConWrapper(retCon);
            ret.id = threadID;
            connections.put(Integer.valueOf(threadID), ret);
        }
        return ret.getConnection();
    }

    private static long getWaitTimeout(Connection con) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = (Statement) con.createStatement();
            rs = stmt.executeQuery("SHOW VARIABLES LIKE 'wait_timeout'");
            int aa;
            if (rs.next()) {
                aa = Math.max(1000, rs.getInt(2) * 1000 - 1000);
                return Math.min(aa, 28800);
            }
            return -1L;
        } catch (SQLException ex) {
            long l;
            return -1L;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                } finally {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (SQLException ex1) {
                        }
                    }
                }
            }
        }
    }

    private static Connection connectToDB() {
        if (!propsInited) {
            dbUrl = ServerProperties.getProperty("net.url");
            dbUser = ServerProperties.getProperty("net.user");
            dbPass = ServerProperties.getProperty("net.password");
        }
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("[DB\u4FE1\u606F] Could not locate the JDBC mysql driver.");
        }
        try {
             Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/Zenith?autoReconnect=true&failOverReadOnly=false", "root", "");

            if (!propsInited) {
                long timeout = getWaitTimeout(con);
                if (timeout != -1L) {
                    connectionTimeOut = timeout;
                }

                propsInited = true;
            }

            return con;
        } catch (SQLException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    public static void closeAll()
            throws SQLException {
        for (ConWrapper con : connections.values()) {
            con.connection.close();
        }
        connections.clear();
    }

    static class ConWrapper {

        private long lastAccessTime = 0L;
        private Connection connection;
        private int id;

        public ConWrapper(Connection con) {
            this.connection = con;
        }

        public Connection getConnection() {
            if (expiredConnection()) {
                try {
                    this.connection.close();
                } catch (Throwable err) {
                }
                this.connection = DatabaseConnection.connectToDB(); //I think this is right?
            }
            this.lastAccessTime = System.currentTimeMillis();
            return this.connection;
        }

        public boolean expiredConnection() {
            if (this.lastAccessTime == 0L) {
                return false;
            }
            try {
                return (System.currentTimeMillis() - this.lastAccessTime >= DatabaseConnection.connectionTimeOut) || (this.connection.isClosed());
            } catch (Throwable ex) {
            }
            return true;
        }
    }
}