package io.github.t1willi.security.session;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.session.Session;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.t1willi.database.Database;
import io.github.t1willi.server.TomcatServer;
import io.github.t1willi.server.config.ConfigurationManager;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;

public class SessionManager {
    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());

    public static void initialize(TomcatServer server) {
        ServletContext sc = server.getContext().getServletContext();
        configureSessionCookies(sc);
        registerSpringSessionFilter(sc);
    }

    private static void configureSessionCookies(ServletContext sc) {
        SessionCookieConfig cfg = sc.getSessionCookieConfig();
        cfg.setHttpOnly(Boolean.parseBoolean(
                ConfigurationManager.getInstance().getProperty("session.httponly", "true")));
        cfg.setSecure(Boolean.parseBoolean(
                ConfigurationManager.getInstance().getProperty("session.secure", "true")));
        cfg.setPath(ConfigurationManager.getInstance().getProperty("session.path", "/"));
        cfg.setAttribute("SameSite", ConfigurationManager.getInstance()
                .getProperty("session.samesite", "Strict"));
        sc.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));
        logger.info("Session cookies configured");
    }

    private static void registerSpringSessionFilter(ServletContext sc) {
        SessionRepositoryFilter<? extends Session> filter;
        int lifetime = Integer.parseInt(
                ConfigurationManager.getInstance().getProperty("session.lifetime", "1800"));

        if (Database.getInstance().isInitialized()) {
            DataSource ds = Database.getInstance().getDataSource();
            String table = ConfigurationManager.getInstance().getProperty("session.table", "sessions");

            ResourceDatabasePopulator pop = new ResourceDatabasePopulator();
            pop.addScript(new ClassPathResource("schema-sessions.sql"));
            pop.execute(ds);

            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
            PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
            TransactionTemplate txTpl = new TransactionTemplate(tm);
            JdbcIndexedSessionRepository repo = new JdbcIndexedSessionRepository(jdbcTemplate, txTpl);
            repo.setTableName(table);
            repo.setDefaultMaxInactiveInterval(Duration.ofSeconds(lifetime));

            filter = new SessionRepositoryFilter<>(repo);
            logger.info("Using JDBC-backed Spring Session (table=" + table + ")");
        } else {
            MapSessionRepository repo = new MapSessionRepository(new ConcurrentHashMap<>());
            repo.setDefaultMaxInactiveInterval(Duration.ofSeconds(lifetime));
            filter = new SessionRepositoryFilter<>(repo);
            logger.warning("DB not initialized; using in-memory Spring Session");
        }

        sc.addFilter("springSessionFilter", filter)
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
        logger.info("Spring Session filter registered");
    }
}