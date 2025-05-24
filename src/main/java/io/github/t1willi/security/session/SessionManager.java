package io.github.t1willi.security.session;

import java.util.Collections;
import java.util.logging.Logger;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.session.PersistentManager;
import org.apache.catalina.session.StandardManager;

import io.github.t1willi.database.Database;
import io.github.t1willi.server.TomcatServer;
import io.github.t1willi.server.config.ConfigurationManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;

public class SessionManager {
    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());

    public static void initialize(TomcatServer server) {
        configureSessionCookies(server.getContext());
        configureSessionPersistence(server);
    }

    private static void configureSessionCookies(Context context) {
        context.addLifecycleListener(event -> {
            if (event.getType().equals(Lifecycle.BEFORE_START_EVENT)) {
                logger.info("Configuring session cookies...");
                ServletContext servletContext = context.getServletContext();
                SessionCookieConfig cookieConfig = servletContext.getSessionCookieConfig();
                cookieConfig.setHttpOnly(loadHttpOnly());
                cookieConfig.setSecure(loadSecure());
                cookieConfig.setPath(loadPath());
                cookieConfig.setAttribute("SameSite", loadSameSite());

                servletContext.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));

                logger.info("Session cookie configuration completed");
            }
        });
    }

    private static void configureSessionPersistence(TomcatServer server) {
        Context context = server.getContext();
        Database db = Database.getInstance();

        if (db.isInitialized()) {
            PersistentManager manager = new PersistentManager();

            manager.setProcessExpiresFrequency(2);
            manager.setMaxIdleBackup(0);
            manager.setMinIdleSwap(120);
            manager.setMaxActive(-1);
            manager.setSaveOnRestart(true);

            JoltJDBCStore jdbcStore = new JoltJDBCStore();
            manager.setStore(jdbcStore);

            context.setManager(manager);
            logger.info("JoltJDBCStore configured for immediate session persistence.");
        } else {
            StandardManager manager = new StandardManager();
            context.setManager(manager);
            logger.warning("Database not initialized; using in-memory session manager.");
        }
    }

    private static boolean loadHttpOnly() {
        String httpOnlyStr = ConfigurationManager.getInstance().getProperty("session.httponly");
        return httpOnlyStr != null ? Boolean.parseBoolean(httpOnlyStr) : true; // default to true
    }

    private static boolean loadSecure() {
        String secureStr = ConfigurationManager.getInstance().getProperty("session.secure");
        return secureStr != null ? Boolean.parseBoolean(secureStr) : true;
    }

    private static String loadPath() {
        String path = ConfigurationManager.getInstance().getProperty("session.path");
        return path != null ? path : "/";
    }

    private static String loadSameSite() {
        String sameSite = ConfigurationManager.getInstance().getProperty("session.samesite");
        return sameSite != null ? sameSite : "Strict";
    }
}