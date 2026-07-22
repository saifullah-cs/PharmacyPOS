package com.mycompany.pharmacypos;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Hands out JDBC connections to every DAO in the app - now backed by a small
 * connection pool instead of opening a brand-new physical connection on every
 * single call.
 *
 * Previously, every DAO method called getConnection() and got back a fresh
 * connection straight from the driver, then closed it when done. Each call to
 * the database - even a single quick lookup, and the autocomplete dropdown
 * fires one on every keystroke - paid the full cost of a new TCP connection +
 * MySQL authentication handshake. Under one cashier this is barely visible;
 * under a real shop with multiple lookups happening back-to-back, it adds up,
 * and under enough concurrent load it can exhaust MySQL's own connection
 * limit (max_connections) since nothing was ever capping how many could be
 * open at once.
 *
 * getConnection() below still has the exact same signature and behavior from
 * every caller's point of view: call it, use it, call close() on it when
 * done. Nothing else anywhere in the app needs to change. The difference is
 * what close() actually does now - instead of tearing down the physical
 * connection, it hands the connection back to the pool so the next
 * getConnection() call can reuse it. A java.lang.reflect.Proxy is what makes
 * this transparent: it looks and behaves exactly like a normal Connection to
 * every DAO, but its close() is intercepted to return-to-pool instead of
 * really closing.
 */
public class DBConnection {

    // Fallback values so the app still runs out-of-the-box during development.
    // For real deployment, set these via environment variables (DB_URL, DB_USER,
    // DB_PASSWORD) or a config.properties file next to the jar instead of
    // editing this file - see loadConfig() below.
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/pharmacy_pos";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "pharmacy2026@";

    // How many physical connections the pool keeps ready. Sized for a small
    // pharmacy setup (one or a few billing terminals) - raise it if this ever
    // runs with many simultaneous terminals hitting the DB at once.
    private static final int POOL_SIZE = 10;

    private static final ConcurrentLinkedQueue<Connection> pool = new ConcurrentLinkedQueue<>();
    private static volatile boolean initialized = false;

    public static Connection getConnection() {
        try {
            ensureInitialized();

            Connection real = pool.poll();
            boolean pooled = (real != null);

            // A pooled connection can still have gone stale (DB restarted, MySQL's
            // own wait_timeout closed an idle connection, network blip) - check
            // before handing it out rather than discovering that mid-query.
            if (real != null && !isUsable(real)) {
                closeQuietly(real);
                real = null;
                pooled = false;
            }

            if (real == null) {
                real = createPhysicalConnection();
                // Not one of the pool's core connections - it was created because
                // the pool was empty (all core connections currently in use, or
                // the pool couldn't be pre-filled at startup). Released back to
                // the pool anyway on close() as long as the pool isn't over
                // capacity, so the pool can refill itself back up to POOL_SIZE
                // during normal use rather than staying permanently short.
                pooled = true;
            }

            return wrap(real);
        } catch (Exception e) {
            System.out.println("Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }

    /** Fills the pool with POOL_SIZE physical connections the first time
     *  getConnection() is called. If the database isn't reachable yet, this
     *  just leaves the pool empty for now - getConnection() falls back to
     *  opening connections on demand (the old behavior) until the DB comes up
     *  and the pool can refill itself through normal use. */
    private static synchronized void ensureInitialized() {
        if (initialized) return;
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                pool.offer(createPhysicalConnection());
            } catch (Exception ex) {
                // DB not reachable at startup, or pool pre-fill partially failed -
                // not fatal, getConnection() will open connections on demand instead.
                break;
            }
        }
        initialized = true;
    }

    private static boolean isUsable(Connection con) {
        try {
            return !con.isClosed() && con.isValid(2);
        } catch (SQLException ex) {
            return false;
        }
    }

    private static void closeQuietly(Connection con) {
        try {
            con.close();
        } catch (Exception ignored) {
        }
    }

    private static Connection createPhysicalConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        Properties props = loadConfig();
        String url = props.getProperty("db.url", DEFAULT_URL);
        String user = props.getProperty("db.user", DEFAULT_USER);
        String password = props.getProperty("db.password", DEFAULT_PASSWORD);

        return DriverManager.getConnection(url, user, password);
    }

    /** Wraps a real connection so close() returns it to the pool instead of
     *  physically closing it. Everything else (queries, prepareStatement,
     *  commit, rollback, setAutoCommit, ...) passes straight through to the
     *  real connection - callers can't tell the difference. */
    private static Connection wrap(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                DBConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new PooledConnectionHandler(real));
    }

    private static class PooledConnectionHandler implements InvocationHandler {
        private final Connection real;
        private boolean released = false;

        PooledConnectionHandler(Connection real) {
            this.real = real;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                release();
                return null;
            }
            // isClosed() should reflect the *logical* connection (from this
            // caller's point of view), not whether the underlying physical
            // connection is still open in the pool for someone else to use.
            if ("isClosed".equals(method.getName())) {
                return released || real.isClosed();
            }
            try {
                return method.invoke(real, args);
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }

        private void release() {
            if (released) return; // double-close() from a try-with-resources + explicit close() - ignore the second one
            released = true;
            try {
                if (real.isClosed()) return;
                // A transaction (saveSale()'s con.setAutoCommit(false) ... commit()/
                // rollback()) must never leak into whoever borrows this connection
                // next - reset it to the default autoCommit-on state before it goes
                // back in the pool.
                if (!real.getAutoCommit()) {
                    try {
                        real.rollback();
                    } catch (Exception ignored) {
                    }
                    real.setAutoCommit(true);
                }
            } catch (Exception ex) {
                closeQuietly(real);
                return;
            }

            if (pool.size() < POOL_SIZE && isUsable(real)) {
                pool.offer(real);
            } else {
                closeQuietly(real);
            }
        }
    }

    /**
     * Loads DB settings with this priority, so you can override the
     * hardcoded defaults above without touching source code:
     *   1. Environment variables: DB_URL, DB_USER, DB_PASSWORD
     *   2. config.properties file placed next to the running jar
     *   3. The DEFAULT_* constants above (local development fallback)
     */
    private static Properties loadConfig() {
        Properties props = new Properties();

        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPassword = System.getenv("DB_PASSWORD");
        if (envUrl != null) props.setProperty("db.url", envUrl);
        if (envUser != null) props.setProperty("db.user", envUser);
        if (envPassword != null) props.setProperty("db.password", envPassword);

        try (InputStream in = new FileInputStream("config.properties")) {
            Properties fileProps = new Properties();
            fileProps.load(in);
            for (String key : fileProps.stringPropertyNames()) {
                props.putIfAbsent(key, fileProps.getProperty(key));
            }
        } catch (IOException ignored) {
            // No config.properties found next to the jar - that's fine,
            // env vars or the hardcoded defaults will be used instead.
        }

        return props;
    }
}
