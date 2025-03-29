// JoltContext.jsx
import { Heart, Code, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function JoltContext() {
    const exampleCode1 = `
get("/user/{id:int}", ctx -> ctx.html("Hi user #" + ctx.path("id").asInt()));
    `.trim();

    const exampleCode2 = `
get("/user/{id:int}", ctx -> {
    int userId = ctx.path("id").asInt();
    return ctx.json(new User(userId, "John Doe")).status(HttpStatus.CREATED);
});
    `.trim();

    const exampleCode3 = `
post("/user", ctx -> {
    User user = ctx.body(User.class);
    // Save user to database
    return ctx.status(HttpStatus.CREATED).json(user);
});
    `.trim();

    const exampleCode4 = `
get("/download", ctx -> {
    JoltFile file = new JoltFile("example.txt", "text/plain", data.length, data);
    return ctx.download(file, "example.txt");
});
    `.trim();

    const exampleCode5 = `
get("/session", ctx -> {
    ctx.addCookie()
        .name("sessionId")
        .value("abc123")
        .httpOnly(true)
        .build();
    return ctx.text("Session cookie set!");
});
    `.trim();

    const exampleCode6 = `
post("/submit", ctx -> {
    Form form = ctx.buildForm("excludeField");
    String username = form.getValue("username");
    return ctx.html("Submitted: " + username);
});
    `.trim();

    const exampleCode7 = `
get("/page", ctx -> {
    JoltModel model = JoltModel.create()
        .with("title", "Welcome")
        .with("user", "John");
    return ctx.render("index.ftl", model);
});
    `.trim();

    const exampleCode8 = `
get("/user/{id:int}", ctx -> {
    int userId = ctx.path("id").asInt();
    if (userId <= 0) {
        ctx.abortNotFound("User not found");
    }
    return ctx.text("User ID: " + userId);
});
    `.trim();

    return (
        <div id="joltcontext" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Heart className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">JoltContext</h1>
            </div>

            <p className="text-gray-300 mb-6">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> class is the heart of the Jolt framework, providing a unified interface for handling HTTP requests and responses. It encapsulates the <code className="text-blue-400 bg-gray-800 px-1 rounded">HttpServletRequest</code> and <code className="text-blue-400 bg-gray-800 px-1 rounded">HttpServletResponse</code> objects, offering utility methods to access request data, send responses, manage cookies, build forms, render templates, and handle errors. Whether you’re parsing JSON, serving static files, or redirecting users, <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> simplifies the process with a fluent and intuitive API.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Access request data: Retrieve path parameters, query parameters, headers, body, and uploaded files.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Send responses: Support for text, JSON, HTML, redirects, file downloads, and static file serving.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Manage cookies: Add, retrieve, and remove cookies with a fluent <code className="text-blue-400 bg-gray-800 px-1 rounded">CookieBuilder</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Build forms: Aggregate data from query parameters, JSON body, and path parameters into a <code className="text-blue-400 bg-gray-800 px-1 rounded">Form</code> object.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Render templates: Use Freemarker templates with <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltModel</code> for dynamic HTML rendering.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Handle errors: Abort requests with specific HTTP statuses (e.g., 404, 400, 500) and custom messages.
                    </span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Request Handling</h2>
            <p className="text-gray-300 mb-4">
                <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> provides a rich set of methods to access request data, including path parameters, query parameters, headers, body, and uploaded files. It also supports parsing JSON bodies into js objects and extracting bearer tokens for authentication.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Accessing a path parameter and sending an HTML response:
                </p>
                <CodeBlock code={exampleCode1} language="js" />
            </div>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Parsing a JSON body from a POST request:
                </p>
                <CodeBlock code={exampleCode3} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods include:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">path(String name)</code>: Access path parameters (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.path("id").asInt()</code>).</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">query(String name)</code>: Access query parameters with defaults (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.query("page").asIntOrDefault(1)</code>).</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">body(Class&lt;T&gt; type)</code>: Parse JSON body into a java object.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getHeader(String name)</code>: Retrieve request headers (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.getHeader("User-Agent")</code>).</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getFiles()</code>: Access uploaded files as <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltFile</code> objects.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">bearerToken()</code>: Extract a bearer token for authentication.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Response Handling</h2>
            <p className="text-gray-300 mb-4">
                <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> offers a variety of methods to send responses, including plain text, JSON, HTML, redirects, file downloads, and static file serving. It also supports setting status codes, headers, and content types.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Sending a JSON response with a custom status:
                </p>
                <CodeBlock code={exampleCode2} language="js" />
            </div>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Triggering a file download:
                </p>
                <CodeBlock code={exampleCode4} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods include:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">text(String data)</code>: Send a plain-text response.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">json(Object data)</code>: Send a JSON response.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">html(String html)</code>: Send an HTML response.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">redirect(String location)</code>: Redirect to a new URL.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">download(JoltFile file, String filename)</code>: Trigger a file download.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">serve(String resource)</code>: Serve a static file from the <code className="text-blue-400 bg-gray-800 px-1 rounded">/static</code> directory.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">status(HttpStatus status)</code>: Set the HTTP status code.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Managing Cookies</h2>
            <p className="text-gray-300 mb-4">
                <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> provides a fluent API for managing cookies, including adding, retrieving, and removing them. The <code className="text-blue-400 bg-gray-800 px-1 rounded">CookieBuilder</code> makes it easy to configure cookies with options like <code className="text-blue-400 bg-gray-800 px-1 rounded">httpOnly</code>.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Adding a session cookie:
                </p>
                <CodeBlock code={exampleCode5} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods include:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">addCookie()</code>: Create a new cookie with <code className="text-blue-400 bg-gray-800 px-1 rounded">CookieBuilder</code>.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getCookie(String name)</code>: Retrieve a cookie by name.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getCookieValue(String name)</code>: Get a cookie’s value as an <code className="text-blue-400 bg-gray-800 px-1 rounded">Optional</code>.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">removeCookie(String name)</code>: Expire a cookie by setting its <code className="text-blue-400 bg-gray-800 px-1 rounded">maxAge</code> to 0.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Building Forms</h2>
            <p className="text-gray-300 mb-4">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">buildForm()</code> method aggregates data from query parameters, JSON body, and path parameters into a <code className="text-blue-400 bg-gray-800 px-1 rounded">Form</code> object, making it easy to process form submissions.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Building a form from a POST request:
                </p>
                <CodeBlock code={exampleCode6} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key features:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li>Combines data from multiple sources (query, JSON, path parameters).</li>
                    <li>Supports excluding specific fields with <code className="text-blue-400 bg-gray-800 px-1 rounded">buildForm(String... excludes)</code>.</li>
                    <li>Later sources override earlier ones in case of duplicate keys.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Rendering Templates</h2>
            <p className="text-gray-300 mb-4">
                <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> supports rendering Freemarker templates using the <code className="text-blue-400 bg-gray-800 px-1 rounded">render()</code> method. By default, Templates are located in the <code className="text-blue-400 bg-gray-800 px-1 rounded">resources/templates</code> directory.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Rendering a template with a model:
                </p>
                <CodeBlock code={exampleCode7} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key features:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li>Uses <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltModel</code> to pass data to templates.</li>
                    <li>Automatically sets the Content-Type to <code className="text-blue-400 bg-gray-800 px-1 rounded">text/html</code>.</li>
                    <li>Integrates with <code className="text-blue-400 bg-gray-800 px-1 rounded">LanguageService</code> for global language support.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Error Handling</h2>
            <p className="text-gray-300 mb-4">
                <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> provides methods to abort requests with specific HTTP statuses and custom messages, making it easy to handle errors gracefully.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Aborting a request with a 404 status:
                </p>
                <CodeBlock code={exampleCode8} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods include:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">abort(HttpStatus status, String message)</code>: Abort with a custom status and message.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">abortNotFound(String message)</code>: Abort with 404 Not Found.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">abortBadRequest(String message)</code>: Abort with 400 Bad Request.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">abortUnauthorized(String message)</code>: Abort with 401 Unauthorized.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">abortInternalServerError(String message)</code>: Abort with 500 Internal Server Error.</li>
                </ul>
            </p>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        Now that you're familiar with <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code>, explore the{' '}
                        <a href="#Ioc" className="text-blue-400 hover:underline">
                            Ioc and Dependency Injection
                        </a>{' '}
                        section to learn about dependency injection, and more.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default JoltContext;