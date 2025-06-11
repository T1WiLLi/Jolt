# Session

La session devrait être en base de données, Jolt peut intégrer automatiquement une table session dans la base de données de docker. Mais il faudrait également laisser la possibilité aux développeur de configurer la table automatiquement avec les configurations de sessions dans la configuration de sécurité. Aussi, si l'utilisateur ne définit pas de base de données, il faudrait effectivement, utiliser les fichiers, notamment le tmp/tomcat pour stockées les session, Mais je devrait me renseigner car potentiellement, Jakarta EE ServeletSession qui doit certainement déjà avoir des shitsss et après nous ont vas faire un layer on-top de Jakarta EE ServeletSession.


# CSRF

Regarder comment sa fonctionne dans Zephyrus et aller dans le même sense. Sa semble être la meilleur solution, évidemment, un CSRF doit être générer côté serveur et être injecter automatiquement dans les forms HTML. Mais il faudrait aussi laisser la possibilité aux développeurs de configurer le token CSRF, et il faudrait également qu'il puissent appeler genre CsrfToken.generate() pour générer le token CSRF 'manuellement'

# XSS

Pretty much secure, except for file

# Upload file

Pour l'upload de fichier, il ne faut pas utiliser le nom original car ont peut y injecter des code malicieux. Il faut donc utiliser un nom généré par le serveur. De manière cryptographique utilise alphanumeric. Il faut aussi vérifier le code lui-même du fichier, potentiellement regarder en ligne pour un truc qui le fait un peu à notre place.
Faire des recherches pour protéger les fichiers dans le code.

# Logging

Improve logging for security and different logging level.

# Freemarker templating

Update freemarker default configuration to always enforce escaping variables to automatically prevent XSS.

# Amélioration des méthodologie dans le code en lui-même : 

- CSP
    - La nouvelle syntaxe devrait permettre les choses suivantes : 
```js

    withCORS()
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE")
        .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization")
        .allowedCredentials(false)
        .maxAge(3600);

    withHeaders()
        .withXssProtection(XssProtectionPolicy.ENABLE_BLOCK)
        .withFrameOptions(FrameOptionsPolicy.DENY)
        .withHsts(HstsPolicy.ONE_YEAR_WITH_SUBDOMAINS_PRELOAD)
        .withReferrerPolicy(ReferrerPolicy.SAME_ORIGIN)
        .httpsOnly(false)
        .withCacheControl(CacheControlPolicy.NO_CACHE);
        .withCSP() // Without preventing to simply pass a 'string' withCSP (string)
            .withFontSources("self", "https://example.com")
            .withStyleSources("self", "https://example.com", ContentSecurityPolicy.UNSAFE_INLINE)
            .withScriptSources("self", "https://example.com")
            .withChildSources("self")
            .withWorkerSources("blob:")
            .withConnectSources("self", "https://api.mapbox.com")
            .withImageSources("self", "blob", "data")

    .withCSRF()
        .enable() // .disable();
        .addIgnoreUrlPatterns("/login", "/register", "/forgot-password", "/reset-password");
        .withHttpOnlyFalse(); // .withHttpOnly(true);
        .withHandler(new CustomHandler()) // A way to handle the CSRF token.

    .withRoutes()
        .route("/public/**")
            .permitAll()
        .route("/threads/**")
            .methods(GET, PATCH)
            .authenticated()
        .route("/admin/**")
            .authenticated()
        .route("/users/**")
            .methods(GET)
            .authenticated()
            .onSuccess("/dashboard") // Same as onFailure
            .onFailure("/") // Should also be able to pass a Function(JoltContext context, JoltContext context); to make further decisions based on the context and we return the updated context
        .route("/jokes")
            .methods(GET)
            .authenticated(new CustomAuthStrategy())
        .anyRoute()
            .denyAll();
```


# Controllers : 

```java
@Controller("[controller]")
@Authenticated(Auth.Session)
public class UserController implements BaseController {

    @JoltBeanInjection
    private UserService userService;

    @Get()
    public JoltContext getAll(JoltContext context) {
        return userService.getAll(context);
    }

    @Get("/{id}")
    public JoltContext get(JoltContext context) {
        int id = context.path("id");
        return userService.get(id, context);
    }

    @Post()
    public JoltContext create(JoltContext context) {
        return userService.create(context);
    }
}
```

# Scheduled

```java
@JoltBean
public class ScheduledTask { // To schedule element, you must be within a @JoltBean

    @Scheduled(fixe = 5, timeUnit = TimeUnit.SECONDS) // 5 secs 
    public void hello() {
        System.out.println("Hello!");
    }

    @Scheduled(cron="*/5 * * * * MON-FRI")
    public void hello2() {
        System.out.println("Hello2!");
    }
}
```

# Routes securing 

```java
    .route("/special/**")
        .authenticated(Auth.Session)
        .roles(DefaultRoles.USER)
        .check(ctx -> ctx.user().getDepartment().equals("Legal")); // This is a lambda, won't necessarily use CTX.

    ... // Controller

    @Controller("/orders")
    @Authorize(auth=Auth.Session, roles={DefaultRoles.USER}) // Auth on the whole controller
    public class OrderController { … }

    @Get("/{id}")
    @Authorize(auth=Auth.JWT, roles={MyAppRoles.PREMIUM}) // Auth on a specific function.
    public Order fetchPremium(@Path int id) { … }
```

# Versionning

```java
@Controller("[controller]", version = 2) // 0 won't add anything to the URL but you could have '/v1'
public class UserController { // 'http://localhost/v2/user'

    @Get(version = 3) // 'http://localhost/v3/user
    public List<User> getUsers() {
        return new UserService().getAll();
    }

    @Get("{id}") // 'http://localhost/v2/user/{id}'
    public User getUser(@Path("id") int userID) {
        return new UserService().getById(userID);
    }

    // ... DSL

    get("/api/form", ctx -> ...).version(2); // '/v2/api/form'
    group("api", 2, ctx -> {
        get("/form", ctx -> ...); // '/api/v2/form'
        get("/allo", ctx -> ...).version(3); // Wont be added, since we are in a group, thus group has priority -> '/api/v2/allo'
        // ...
    });
}
```

# Http Client factory

```java
HttpClient client = new HttpClientFactory.create(Duration.of(5), true);
...

// This is the simplest approach
public static void test() {
    HttpResponse response = client.async("https://example.com/api/users"); // client.sync();
    String responseBody = response.body();
    client.dispose();
}

public static void test2() {
    HttpResponse response = client.async("https://example.com/api/users");
    List<Users> users = response.asList(User.class); // .as(TypeReference T)
    client.dispose();
}
```

# Form Update

```java
public void validate(Form form) {
    form.field("name")
        .required()
        .minLength(3, "Minimum length is 3.");
    form.field("email")
        .required()
        .email("Must be a valid email");
    
    if (!form.verify()) {
        return ctx.json(form.getAllErrors());
    }
    String name = form.getValue("name");
    String email = form.getValue("email");
    return ctx.html("Hello " + name + ". Email: " + email);
    ... 
}

public void validateNew(Form form) {
    form.field("name")
        .stringLength(3, "Minimum length is 3."); // We infer required() which is obvious and a better naming convention
    form.field("email")
        .email("Must be a valid email for this domain", "@cegepst.qc.ca"); // Should likely be a regex, as a string and behind we use a Pattern object, we should still be able to nromal 'email();'
    form.field("age")
        .min(18, "You must be at least 18 years old");
    
    if (!form.validate()) { // Return true if no error, false otherwise.
        return ctx.json(form.errors()) // Build a json object with the form errors, we return the first error we encounter when validating the form, otherwise the user can do : form.allErrors(); Which return the same FormError object but with a list of failed rule for each field. 
    }

    String name = form.field("name").get();
    String email = form.field("email").get();
    int age = form.field("age").asInt();
}
```

# Template system & Controllers

```java
public class BaseController {

    protected JoltContext context;

    public BaseController() {
        this.context = JoltDispatcher.getCurrentContext();
    }
}

public class ApiController extends BaseController {
    // Add function for API-like element.
}

public class MvcController extends BaseController {
    // Add function for MVC-like element : 

    public ResponseEntity<ModelView> render(String template, JoltModel model) {
        // ...
    }

    public void redirect(String location) {
        super.context.redirect(location);
    }

    // And so on ...
}

// Update the return type of the functions of any given element : 

- JoltContext (ctx)
- String (static element, like a simple text)
- ModelView (dynamic element, like template (.ftl, .html, .jsp, etc.))
- ResponseEntity<T> (Json, XML, Object, ModelView, etc.)
- Any Object directly as JSON

- ResponseEntity<T> 
- A ResponseEntity<T> is a wrapper around the response object, which allows you to set the status code, headers, and body of the response in a single object. This is useful for returning complex responses that require more than just a simple body :

```java
public ResponseEntity<String> get() {
    return ResponseEntity
        .status(HttpStatus.OK)
        .header("Content-Type", "application/json")
        .body("{\"message\": \"Hello World\"}");
}

public ResponseEntity<ModelView> get() {
    return ResponseEntity
        .status(HttpStatus.OK)
        .header("Content-Type", "text/html")
        .body(ModelView.of("index.ftl", JoltModel.with(Map.of(...))));
}

public ResponseEntity<Void> get() {
    return redirect("/index");
}
```

# OpenAPI documentation support : 

- To define OpenAPI, we can use the '@OpenAPI' annotation on the class or method level. if use on the class extending JoltApplication, it will be used for the whole application.

```java
@OpenAPI( // The OpenAPI annotation, altought has a lot of parameters, none of them are required. Element such as title, version and description are the most important ones, but can all be get from the controller itself, so they are not required to be set here.
    title = "My API",
    version = "1.0.0", // If the @Version annotation is used, it will precede the version of the OpenAPI annotation.
    description = "My API description",
    contact = @Contact(
        name = "John Doe",
        email = "john.doe@example.com",
        url = "https://example.com"
    ),
    license = @License(
        name = "Apache 2.0",
        url = "https://www.apache.org/licenses/LICENSE-2.0.html"
    ),
    tags = {
        @Tag(name = "users", description = "User management"),
        @Tag(name = "products", description = "Product management")
    }
)
@Controller("[controller]")
public class HomeController extends ApiController {

    @Get // OpenAPI, sees a GET, no param, no body, returns a String, in this case application/json because it's set.
    public ResponseEntity<String> get() {
        return ResponseEntity
            .status(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body("{\"message\": \"Hello World\"}");
    }
}
```

# Authentification configuration

```java
// ... In DefaultSecurityConfiguration.java

    .auth(auth -> auth
        .login(login -> login
            .page("/login")
            .successUrl("/dashboard") // Always redirect to this page.
            .failureUrl("/login") // Also allow Function(JoltContext ctx, JoltContext ctx) in all of those
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .destroySession(true)
            .logoutSuccessUrl("/")
        )
    )

    .withRoutes()
        .route("/dashboard")
            .methods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT)
            .authenticated()
            .onFailure("/") // Can be a function, or a string
        .anyRoute()
            .denyAll();
```

# WebHooks and Hook-Event-Driven-Architecture
```java
@Bean // Only pick up @Event annotated method that are public and in @Bean annotated classes
public class Something {


    @Event("MyEvent") // If no name is provided, it will be the method name.
    public void doingSomething() {

    }

    public void doingSomethingElse() {
        HookEvent.trigger("MyEvent2")
    }
}

// Example in a controller : 

@Get("/trigger-event")
@EventListener("MyEvent") // This will trigger when MyEvent method is invoke
public ResponseEntity<Void> triggerEvent() {
    // ... Do something ...
}

@EventListener("MyEvent2")
public void onMyEvent2(JoltContext ctx) {
    // ... Do something ...
}
```

New server properties : 

server.logging.level=SEVERE, WARNING, INFO, FINE, FINER, FINEST, ALL, OFF
server.logging.logfile=jolt.log

server.http.enabled=false // By default, HTTP is enabled. This is more of an SSL/TLS feature.

session.expirationSliding=false // By default, this is false
session.encrypt=false // By default, this is false.
session.httponly=true // By default, this is true.
session.secure=true // By default, this is true.
session.path=/ // By default, this is "/".
session.samesite=Strict // By default, this is "Strict".
session.name=tomcat_session // By default, this is tomcat_session (represent the name of the table which will be created in the database to store the session data).

session.rate.limit.max=10 // By default, this is 10 session per the given rate.limit.window=60 seconds.
session.rate.limit.window=60 // By default, this is 60sec.