// Filters.jsx
import { Filter, Code, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function Filters() {
    const exampleCode1 = `
public class AuthFilter extends JoltFilter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!forRoutes(request, response, chain, "/api/users", "/api/posts")) {
            return; // Skip if the route doesn't match
        }

        JoltContext ctx = buildJoltContext(request, response);
        Optional<String> token = ctx.bearerToken();
        if (token.isEmpty()) {
            ctx.status(HttpStatus.UNAUTHORIZED).text("Unauthorized: No token provided");
            return;
        }

        // Perform authentication logic (e.g., validate token)
        if (!isValidToken(token.get())) {
            ctx.status(HttpStatus.UNAUTHORIZED).text("Unauthorized: Invalid token");
            return;
        }

        chain.doFilter(request, response); // Proceed to the next filter or handler
    }

    private boolean isValidToken(String token) {
        // Dummy validation logic
        return token.equals("valid-token");
    }
}
    `.trim();

    const exampleCode2 = `
@JoltConfiguration(value = ConfigurationType.FILTER)
public class CustomFilterConfiguration extends FilterConfiguration {

    @PostConstruct
    public void init() {
        configure(); // If you do not call configure() the filter configuration will not be applied
    }

    @Override
    public void configure() {
        // Exclude specific routes from filtering
        exclude("/login", "/public");

        // Exclude routes dynamically based on a predicate
        excludeIf(ctx -> ctx.getHeader("User-Agent") != null && 
                        ctx.getHeader("User-Agent").contains("Bot"));

        // Set filter order
        setOrder(1, new AuthFilter());
    }
}
    `.trim();

    const exampleCode3 = `
public class MyApp extends JoltApplication {
    public static void main(String[] args) {
        launch(MyApp.class);
    }

    @Override
    protected void setup() {
        // Define routes
        get("/api/users", ctx -> ctx.text("User list"));
        get("/login", ctx -> ctx.text("Login page"));

        // Filters are automatically applied via configuration
    }
}
    `.trim();

    return (
        <div id="filters" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Filter className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Filters</h1>
            </div>

            <p className="text-gray-300 mb-6">
                Filters in Jolt provide a mechanism to intercept and process HTTP requests before they reach route handlers, enabling cross-cutting concerns like authentication, logging, or request modification. The <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltFilter</code> class serves as the base for creating custom filters, while <code className="text-blue-400 bg-gray-800 px-1 rounded">FilterConfiguration</code> allows you to configure filter behavior, such as excluding specific routes or setting execution order.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Route matching: Apply filters to specific routes using <code className="text-blue-400 bg-gray-800 px-1 rounded">forRoutes()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        JoltContext integration: Use <code className="text-blue-400 bg-gray-800 px-1 rounded">buildJoltContext()</code> to access request and response data.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Route exclusion: Exclude routes from filtering using <code className="text-blue-400 bg-gray-800 px-1 rounded">exclude()</code> or <code className="text-blue-400 bg-gray-800 px-1 rounded">excludeIf()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Filter ordering: Control the execution order of filters with <code className="text-blue-400 bg-gray-800 px-1 rounded">setOrder()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Custom configuration: Extend <code className="text-blue-400 bg-gray-800 px-1 rounded">FilterConfiguration</code> to define custom filter behavior.
                    </span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Creating a Filter</h2>
            <p className="text-gray-300 mb-4">
                To create a custom filter, extend the <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltFilter</code> class and override the <code className="text-blue-400 bg-gray-800 px-1 rounded">doFilter()</code> method. Use <code className="text-blue-400 bg-gray-800 px-1 rounded">forRoutes()</code> to apply the filter to specific routes and <code className="text-blue-400 bg-gray-800 px-1 rounded">buildJoltContext()</code> to access the <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> for request and response handling.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: An authentication filter that checks for a bearer token:
                </p>
                <CodeBlock code={exampleCode1} language="js" />
            </div>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Configuring Filters</h2>
            <p className="text-gray-300 mb-4">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">FilterConfiguration</code> class allows you to configure filter behavior. You can exclude specific routes, exclude routes dynamically using predicates, and set the execution order of filters. Create a custom configuration by extending <code className="text-blue-400 bg-gray-800 px-1 rounded">FilterConfiguration</code> and annotating it with <code className="text-blue-400 bg-gray-800 px-1 rounded">@JoltConfiguration(value = ConfigurationType.FILTER)</code>.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Configuring a filter to exclude certain routes and set order:
                </p>
                <CodeBlock code={exampleCode2} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">exclude(String... routes)</code>: Exclude specific routes from filtering.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">excludeIf({`Predicate<JoltContext>`})</code>: Exclude routes based on a dynamic condition.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">setOrder(int order, JoltFilter filter)</code>: Set the execution order of a filter (lower values run first).</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Usage Example</h2>
            <p className="text-gray-300 mb-4">
                Here's a complete example of using filters in a Jolt application. The application defines routes, and the <code className="text-blue-400 bg-gray-800 px-1 rounded">AuthFilter</code> (from the first example) is applied to specific routes based on the configuration defined in <code className="text-blue-400 bg-gray-800 px-1 rounded">CustomFilterConfiguration</code>.
            </p>
            <div className="mb-6">
                <CodeBlock code={exampleCode3} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                In this example:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">AuthFilter</code> is applied to <code className="text-blue-400 bg-gray-800 px-1 rounded">/api/users</code> but not to <code className="text-blue-400 bg-gray-800 px-1 rounded">/login</code> due to the configuration.</li>
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">CustomFilterConfiguration</code> excludes <code className="text-blue-400 bg-gray-800 px-1 rounded">/login</code> and requests from bots.</li>
                    <li>Filters are automatically applied by the framework based on the configuration.</li>
                </ul>
            </p>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        With filters in place, explore the{' '}
                        <a href="#logging" className="text-blue-400 hover:underline">
                            Logging
                        </a>{' '}
                        section to learn about additional features in Jolt.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Filters;