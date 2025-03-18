package ca.jolt.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import ca.jolt.database.models.QueryResult;
import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.http.HttpStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class Query<T> {

    private static final Logger logger = Logger.getLogger(Query.class.getName());
    private final Class<T> entityClass;
    private final StringBuilder sqlBuilder;
    private final List<Object> parameters;
    private final boolean isSelect;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

    public Query(Class<T> entityClass, String initialSql, boolean isSelect, Object... initialParams) {
        if (!SqlSecurity.isValidRawSql(initialSql)) {
            logger.severe(() -> "Potential SQL injection detected: " + initialSql);
            throw new IllegalArgumentException("Invalid SQL statement detected");
        }

        this.entityClass = entityClass;
        this.sqlBuilder = new StringBuilder(initialSql);
        this.parameters = new ArrayList<>();
        Collections.addAll(this.parameters, initialParams);
        this.isSelect = isSelect;
    }

    public Query<T> with(String clause, Object... params) {
        if (!SqlSecurity.isValidRawSql(clause)) {
            logger.severe(() -> "Potential SQL injection detected in clause: " + clause);
            throw new IllegalArgumentException("Invalid SQL clause detected");
        }

        sqlBuilder.append(" ").append(clause);
        Collections.addAll(this.parameters, params);
        return this;
    }

    public Query<T> where(String whereClause, Object... params) {
        if (!SqlSecurity.isValidWhereClause(whereClause)) {
            logger.severe(() -> "Potential SQL injection detected in WHERE clause: " + whereClause);
            throw new IllegalArgumentException("Invalid WHERE clause detected");
        }

        String normalizedClause = whereClause.trim().toUpperCase();
        if (!normalizedClause.startsWith("WHERE ")) {
            sqlBuilder.append(" WHERE ");
        }

        sqlBuilder.append(" ").append(whereClause);
        Collections.addAll(this.parameters, params);
        return this;
    }

    public Query<T> orderBy(String orderByClause) {
        String[] parts = orderByClause.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            final String columnName;

            if (trimmed.toUpperCase().endsWith(" ASC") || trimmed.toUpperCase().endsWith(" DESC")) {
                columnName = trimmed.substring(0, trimmed.lastIndexOf(" ")).trim();
            } else {
                columnName = trimmed;
            }

            if (!SqlSecurity.isValidColumnName(columnName)) {
                logger.severe(() -> "Invalid column name in ORDER BY clause: " + columnName);
                throw new IllegalArgumentException("Invalid column name in ORDER BY: " + columnName);
            }
        }

        sqlBuilder.append(" ORDER BY ").append(orderByClause);
        return this;
    }

    public Query<T> page(int pageNumber, int pageSize) {
        if (pageNumber <= 0 || pageSize <= 0) {
            throw new IllegalArgumentException("Page number and size must be positive");
        }

        int offset = (pageNumber - 1) * pageSize;
        sqlBuilder.append(" LIMIT ? OFFSET ?");
        parameters.add(pageSize);
        parameters.add(offset);
        return this;
    }

    public List<T> selectList() {
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement ps = prepareStatement(conn);
                ResultSet rs = ps.executeQuery()) {

            List<T> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
            return list;
        } catch (SQLException e) {
            logger.severe(() -> "Error executing query: " + getSql() + ", " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Database query error: " + e.getMessage());
        }
    }

    public Optional<T> selectSingle() {
        List<T> list = selectList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int executeUpdate() {
        if (!isValidForUpdate()) {
            logger.severe(() -> "Attempted potentially unsafe update: " + getSql());
            throw new IllegalStateException("Update query validation failed");
        }

        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement ps = prepareStatement(conn)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe(() -> "Error executing update query: " + getSql() + " " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Database update error: " + e.getMessage());
        }
    }

    public QueryResult<T> execute() {
        if (!isSelect && !isValidForUpdate()) {
            logger.severe(() -> "Attempted potentially unsafe operation: " + getSql());
            throw new IllegalStateException("Query validation failed");
        }

        long start = System.currentTimeMillis();
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement ps = prepareStatement(conn)) {
            if (isSelect && getSql().trim().toUpperCase().startsWith("SELECT")) {
                List<T> list;
                try (ResultSet rs = ps.executeQuery()) {
                    list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(mapResultSet(rs));
                    }
                }
                long execTime = System.currentTimeMillis() - start;
                return new QueryResult<>(list, 0, null, execTime);
            } else {
                int affected = ps.executeUpdate();
                Object lastInserted = null;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        lastInserted = rs.getObject(1);
                    }
                }
                long execTime = System.currentTimeMillis() - start;
                return new QueryResult<>(null, affected, lastInserted, execTime);
            }
        } catch (SQLException e) {
            logger.severe(() -> "Error executing query: " + getSql() + " " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    public String getSql() {
        return sqlBuilder.toString();
    }

    public List<Object> getParameters() {
        return new ArrayList<>(parameters);
    }

    private PreparedStatement prepareStatement(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(getSql(), Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < parameters.size(); i++) {
            ps.setObject(i + 1, parameters.get(i));
        }
        return ps;
    }

    private T mapResultSet(ResultSet rs) throws SQLException {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            var row = new java.util.HashMap<String, Object>();
            for (int i = 1; i <= cols; i++) {
                String colName = meta.getColumnLabel(i);
                if (colName == null || colName.isEmpty()) {
                    colName = meta.getColumnName(i);
                }
                row.put(colName, rs.getObject(i));
            }
            return objectMapper.convertValue(row, entityClass);
        } catch (Exception e) {
            logger.severe(() -> "Error mapping result set to " + entityClass.getName() + " " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Error mapping database results");
        }
    }

    protected <X> X convertValue(Object value, Class<X> targetType) {
        return objectMapper.convertValue(value, targetType);
    }

    /**
     * Validates if the current query is safe for operations that modify data
     * 
     * @return true if the query passes safety checks
     */
    private boolean isValidForUpdate() {
        String sql = getSql().trim().toUpperCase();
        if (sql.contains("DROP ") || sql.contains("TRUNCATE ") ||
                sql.contains("ALTER ") || sql.contains(";")) {
            return false;
        }

        if ((sql.startsWith("UPDATE ") || sql.startsWith("DELETE ")) && !sql.contains(" WHERE ")) {
            return false;
        }

        return true;
    }
}