package io.github.t1willi.security.session;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
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

    /**
     * Configuration properties you can override:
     *
     * session.lifetime → default max-inactive (in seconds)
     * session.cookiename → default "SESSION"
     * session.httponly → default "true"
     * session.secure → default "true"
     * session.path → default "/"
     * session.samesite → default "Strict"
     */
    public static void initialize(TomcatServer server) {
        Context context = server.getContext();
        context.addLifecycleListener(evt -> {
            if (Lifecycle.BEFORE_START_EVENT.equals(evt.getType())) {
                ServletContext sc = context.getServletContext();
                configureSessionCookies(sc);
                registerSpringSessionFilter(sc);
            }
        });
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
        logger.info("Session cookies configured (HttpOnly, Secure, Path, SameSite)");
    }

    @SuppressWarnings({ "rawtypes" })
    private static void registerSpringSessionFilter(ServletContext sc) {
        int lifetime = Integer.parseInt(
                ConfigurationManager.getInstance().getProperty("session.lifetime", "1800"));

        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName(
                ConfigurationManager.getInstance().getProperty("session.cookiename", "SESSION"));
        cookieSerializer.setCookiePath(
                ConfigurationManager.getInstance().getProperty("session.path", "/"));
        cookieSerializer.setUseHttpOnlyCookie(Boolean.parseBoolean(
                ConfigurationManager.getInstance().getProperty("session.httponly", "true")));
        cookieSerializer.setUseSecureCookie(Boolean.parseBoolean(
                ConfigurationManager.getInstance().getProperty("session.secure", "true")));
        cookieSerializer.setSameSite(
                ConfigurationManager.getInstance().getProperty("session.samesite", "Strict"));

        CookieHttpSessionIdResolver resolver = new CookieHttpSessionIdResolver();
        resolver.setCookieSerializer(cookieSerializer);

        SessionRepositoryFilter filter;

        if (Database.getInstance().isInitialized()) {
            if (!tablesExist()) {
                try {
                    ResourceDatabasePopulator pop = new ResourceDatabasePopulator();
                    pop.addScript(new ClassPathResource("org/springframework/session/jdbc/schema-postgresql.sql"));
                    pop.setContinueOnError(true);
                    pop.execute(Database.getInstance().getDataSource());
                    logger.info("Spring Session schema initialized");
                } catch (Exception e) {
                    logger.info("Session tables may already exist: " + e.getMessage());
                }
            } else {
                logger.info("Spring Session tables already exist");
            }

            JdbcTemplate jt = new JdbcTemplate(Database.getInstance().getDataSource());
            PlatformTransactionManager txm = new DataSourceTransactionManager(Database.getInstance().getDataSource());
            TransactionTemplate tx = new TransactionTemplate(txm);

            JdbcIndexedSessionRepository repo = new JdbcIndexedSessionRepository(jt, tx);
            repo.setDefaultMaxInactiveInterval(Duration.ofSeconds(lifetime));

            filter = new SessionRepositoryFilter<>(repo);
            logger.info("Using JDBC-backed Spring Session (default tables)");
        } else {
            MapSessionRepository repo = new MapSessionRepository(new ConcurrentHashMap<>());
            repo.setDefaultMaxInactiveInterval(Duration.ofSeconds(lifetime));
            filter = new SessionRepositoryFilter<>(repo);
            logger.warning("DB not initialized; using in-memory Spring Session");
        }

        filter.setHttpSessionIdResolver(resolver);

        sc.addFilter("springSessionFilter", filter)
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");

        logger.info("Spring Session filter registered with custom cookie settings");
    }

    private static boolean tablesExist() {
        try {
            JdbcTemplate jt = new JdbcTemplate(Database.getInstance().getDataSource());

            String checkQuery = """
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = current_schema()
                    AND table_name = 'spring_session'
                    """;

            Integer count = jt.queryForObject(checkQuery, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}