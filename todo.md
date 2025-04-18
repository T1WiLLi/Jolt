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
        .route("/**").permitAll() // denyAll()
        .route("/admin").authenticated(Auth.Session) // .authenticated(Auth.JWT), .authenticated(new CustomAuth());
        .route("/threads").permit(HttpMethod.GET, HttpMethod.PATCH).authenticated(Auth.Session)
        .anyRoute().denyAll();
```


# Controllers : 

```java
@Controller
@Root("[controller]") // '/user'
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

session.rate.limit.max=10 // By default, this is 10 session per the given rate.limit.window=60 seconds.
session.rate.limit.window=60 // By default, this is 60sec.