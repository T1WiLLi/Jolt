# Application Properties 

### All settings available in Jolt.

#### General Server Settings
- server.appName = "My App" Define the name of the application
- server.port = 80 Define the port number for HTTP
- server.tempDir = "/tmp" Define the temporary directory for tomcat
- server.directory.listing = false Disable/Enable directory listing
- server.directory.listing.path = "/directory" Define the path for listing
- server.threads.min = 10 Define the minimum number of threads
- server.threads.max = 100 Define the maximum number of threads
- server.daemon = true Define the daemon status
- server.logging.level = INFO Define the logging level
- server.logging.logfile = "jolt.log" Define the log file and ENABLE it 

#### Session Settings
- session.lifetime = 900 Define the session lifetime in seconds
- session.httponly = true Define the session http only
- session.secure = true Define the session secure
- session.path = "/" Define the session path
- session.samesite = "strict" Define the session samesite
- session.encrypt = false Define if the session should be encrypted
- session.expirationSliding = false Define if the session expiration should be sliding


#### Security Settings

#### HTTP & HTTPS Settings

#### Database Settings

#### File Handling Settings