package ca.jolt.tomcat.abstraction;

import ca.jolt.exceptions.ServerException;

public interface WebServer {
    public void start() throws ServerException;

    public void stop() throws ServerException;

    public void restart() throws ServerException;
}
