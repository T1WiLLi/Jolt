// Cookies.jsx
import { Cookie, Code, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function Cookies() {
    const exampleCode1 = `
HttpServletResponse response = ...;
new CookieBuilder(response)
    .setName("sessionId")
    .setValue("abc123")
    .httpOnly(true)
    .secure(true)
    .maxAge(3600)
    .path("/")
    .sameSite("Strict")
    .build();
    `.trim();

    const exampleCode2 = `
get("/set-cookie", ctx -> {
    ctx.addCookie()
        .setName("userId")
        .setValue("12345")
        .httpOnly(true)
        .secure(true)
        .maxAge(3600)
        .path("/")
        .sameSite("Strict")
        .build();
    return ctx.text("Cookie set!");
});

get("/get-cookie", ctx -> {
    Optional<String> userId = ctx.getCookieValue("userId");
    if (userId.isPresent()) {
        return ctx.text("User ID: " + userId.get());
    }
    return ctx.status(HttpStatus.NOT_FOUND).text("Cookie not found");
});

get("/remove-cookie", ctx -> {
    ctx.removeCookie("userId");
    return ctx.text("Cookie removed!");
});
    `.trim();

    const exampleCode3 = `
public class MyApp extends JoltApplication {
    public static void main(String[] args) {
        launch(MyApp.class);
    }

    @Override
    protected void setup() {
        JoltContainer container = JoltContainer.getInstance();
        container.autoScan();
        container.initialize();

        get("/set-cookie", ctx -> {
            ctx.addCookie()
                .setName("sessionId")
                .setValue("abc123")
                .httpOnly(true)
                .secure(true)
                .maxAge(3600)
                .path("/")
                .sameSite("Strict")
                .build();
            return ctx.text("Session cookie set!");
        });

        get("/get-cookie", ctx -> {
            ctx.getCookieValue("sessionId",
                value -> ctx.text("Session ID: " + value),
                () -> ctx.status(HttpStatus.NOT_FOUND).text("Session cookie not found")
            );
            return ctx; // Response already handled in getCookieValue
        });

        get("/remove-cookie", ctx -> {
            ctx.removeCookie("sessionId");
            return ctx.text("Session cookie removed!");
        });
    }
}
    `.trim();

    return (
        <div id="cookies" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Cookie className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Cookies</h1>
            </div>

            <p className="text-gray-300 mb-6">
                Cookies are a fundamental part of web applications, used for session management, user preferences, and more. In Jolt, the <code className="text-blue-400 bg-gray-800 px-1 rounded">CookieBuilder</code> class provides a fluent API to create and configure cookies, while <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> offers convenient methods to add, retrieve, and remove cookies from HTTP requests and responses.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Fluent cookie creation: Use <code className="text-blue-400 bg-gray-800 px-1 rounded">CookieBuilder</code> to configure cookies with options like <code className="text-blue-400 bg-gray-800 px-1 rounded">httpOnly</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">secure</code>, and <code className="text-blue-400 bg-gray-800 px-1 rounded">sameSite</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        JoltContext integration: Add, retrieve, and remove cookies directly using <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> methods.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Security options: Configure cookies with security settings like <code className="text-blue-400 bg-gray-800 px-1 rounded">httpOnly</code> and <code className="text-blue-400 bg-gray-800 px-1 rounded">secure</code> to protect against common vulnerabilities.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Flexible retrieval: Use <code className="text-blue-400 bg-gray-800 px-1 rounded">getCookieValue()</code> with callbacks to handle cookie presence or absence.
                    </span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Using CookieBuilder</h2>
            <p className="text-gray-300 mb-4">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">CookieBuilder</code> class provides a fluent API to create and configure cookies before adding them to an <code className="text-blue-400 bg-gray-800 px-1 rounded">HttpServletResponse</code>. You can set properties like the cookie’s name, value, security settings, and expiration time.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Creating a secure session cookie with <code className="text-blue-400 bg-gray-800 px-1 rounded">CookieBuilder</code>:
                </p>
                <CodeBlock code={exampleCode1} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">setName(String name)</code>: Set the cookie’s name (required).</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">setValue(String value)</code>: Set the cookie’s value.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">httpOnly(boolean)</code>: Make the cookie inaccessible to JavaScript.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">secure(boolean)</code>: Ensure the cookie is only sent over HTTPS.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">maxAge(int)</code>: Set the cookie’s expiration time in seconds.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">sameSite(String)</code>: Set the SameSite policy (e.g., "Strict", "Lax").</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">build()</code>: Finalize and add the cookie to the response.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Managing Cookies with JoltContext</h2>
            <p className="text-gray-300 mb-4">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> class provides convenient methods to manage cookies directly in your route handlers. You can add cookies using <code className="text-blue-400 bg-gray-800 px-1 rounded">addCookie()</code>, retrieve them with <code className="text-blue-400 bg-gray-800 px-1 rounded">getCookieValue()</code>, and remove them with <code className="text-blue-400 bg-gray-800 px-1 rounded">removeCookie()</code>.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Adding, retrieving, and removing cookies in route handlers:
                </p>
                <CodeBlock code={exampleCode2} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">addCookie()</code>: Returns a <code className="text-blue-400 bg-gray-800 px-1 rounded">CookieBuilder</code> to create and add a cookie.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getCookieValue(String name)</code>: Retrieves a cookie's value as an <code className="text-blue-400 bg-gray-800 px-1 rounded">Optional</code>.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getCookieValue(String name, {`Consumer<String>`}, Runnable)</code>: Retrieves a cookie's value and processes it with callbacks.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">removeCookie(String name)</code>: Expires a cookie by setting its <code className="text-blue-400 bg-gray-800 px-1 rounded">maxAge</code> to 0.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Usage Example</h2>
            <p className="text-gray-300 mb-4">
                Here's a complete example of managing cookies in a Jolt application. The application defines routes to set, retrieve, and remove a session cookie, demonstrating the use of <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> methods.
            </p>
            <div className="mb-6">
                <CodeBlock code={exampleCode3} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                In this example:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">/set-cookie</code> route creates a secure session cookie using <code className="text-blue-400 bg-gray-800 px-1 rounded">addCookie()</code>.</li>
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">/get-cookie</code> route retrieves the cookie using <code className="text-blue-400 bg-gray-800 px-1 rounded">getCookieValue()</code> with callbacks.</li>
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">/remove-cookie</code> route removes the cookie using <code className="text-blue-400 bg-gray-800 px-1 rounded">removeCookie()</code>.</li>
                </ul>
            </p>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        Now that you can manage cookies, explore the{' '}
                        <a href="#forms" className="text-blue-400 hover:underline">
                            Forms
                        </a>{' '}
                        section to learn how to handle form submissions in Jolt.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Cookies;