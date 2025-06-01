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

server.security.secret_key = "random-base64-value" Define the secret global key
server.security.pepper = "random-base64-value" Define the pepper

#### HTTP & HTTPS Settings

server.ssl.enabled = false Define if SSL is enabled
server.ssl.port = 443 Define the SSL port
server.ssl.keyStore = "keystore.jks" Define the keystore
server.ssl.keyStorePassword = "password" Define the keystore password
server.ssl.keyAlias = "alias" Define the key alias
server.http.enabled = true Define if HTTP is enabled
server.http.redirect = true Define if HTTP should be redirected to HTTPS

#### Database Settings

db.url = "jdbc:mysql://localhost:3306/mydb" Define the database URL
db.username = "myuser" Define the database username
db.password = "mypassword" Define the database password

#### File Handling Settings

server.multipart.maxFileSize = 1048576 Define the max file size
server.multipart.maxRequestSize = 10485760 Define the max request size
server.multipart.fileSizeThreshold = 0 Define the file size threshold

#### Localization & Internationalization Settings

server.lang = "en" Define the default language and enable localization.
