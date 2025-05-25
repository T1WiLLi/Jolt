package io.github.t1willi.utils;

import java.util.regex.Pattern;

public final class Constant {
    public static final class Security {
        // Algorithm for encryption and security
        public static final String PBKDF2_SHA512 = "PBKDF2WithHmacSHA512";
        public static final String PBKDF2_SHA256 = "PBKDF2WithHmacSHA256";
        public static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
        public static final String AES_CBC_NOPADDING = "AES/CBC/NoPadding";
        public static final String AES_CTR_NOPADDING = "AES/CTR/NoPadding";
        public static final String AES_ECB_PKCS5 = "AES/ECB/PKCS5Padding";
        public static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";

        // SecurityConfiguration Constants
        public static final int DEFAULT_MAX_REQUEST_PER_USER_PER_SECOND = 25;
        public static final String CSRF_TOKEN_NAME = "_csrf";
        public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

        // Session Constants
        public static final String DEFAULT_SESSION_TABLE_NAME = "session";

        // JWT Constants
        public static final long DEFAULT_JWT_TOKEN_EXPIRATION_IN_MS = 1_800_000l;
    }

    public static final class SessionKeys {
        public static final String INITIALIZED = "jolt_session_initialized";
        public static final String IP_ADDRESS = "ip_address";
        public static final String USER_AGENT = "user_agent";
        public static final String ACCESS_TIME = "access_time";
        public static final String EXPIRE_TIME = "expire_time";
        public static final String IS_AUTHENTICATED = "is_authenticated";
    }

    public static final class Database {
        public static final int DATABASE_DEFAULT_MAX_CONNECTIONS = 10;
        public static final Pattern VALID_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
        public static final Pattern VALID_COLUMN_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?$");
        public static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
                "(?i)(\\b)(union|select|insert|update|delete|drop|alter|exec|execute|create|where|having|or|and)(\\b)");
        public static final Pattern SAFE_WHERE_CLAUSE_PATTERN = Pattern.compile(
                "^(WHERE\\s+)?(([a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?)\\s*(=|!=|<>|>|<|>=|<=|LIKE|IN|IS NULL|IS NOT NULL|BETWEEN)\\s*((\\?)|('[^']*')|([0-9]+)))"
                        + "(\\s+(AND|OR)\\s+\\2)*$");
    }

    public static final class Filter {
        public static final int INTERNAL_FILTER_OFFSET = 100;
    }

    public static final class Scheduling {
        public static int DEFAULT_POOL_SIZE = 10;
    }
}
