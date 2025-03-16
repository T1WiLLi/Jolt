package ca.jolt.database.models;

import java.util.List;

public final class QueryResult<T> {
    private final List<T> result;
    private final int affectedRowCount;
    private final Object lastInsertedId;
    private final long executionTimeMillis;

    public QueryResult(List<T> result, int affectedRowCount, Object lastInsertedId, long executionTimeMillis) {
        this.result = result;
        this.affectedRowCount = affectedRowCount;
        this.lastInsertedId = lastInsertedId;
        this.executionTimeMillis = executionTimeMillis;
    }

    public List<T> getResult() {
        return result;
    }

    public int getAffectedRowCount() {
        return affectedRowCount;
    }

    public Object getLastInsertedId() {
        return lastInsertedId;
    }

    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }
}
