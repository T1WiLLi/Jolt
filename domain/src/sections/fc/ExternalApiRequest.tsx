// ExternalApiRequest.jsx
import { Globe, Code, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function ExternalApiRequest() {
    const exampleCode1 = `
Response response = Http.get("https://api.example.com/users")
    .header("Authorization", "Bearer your-token-here")
    .query("id", "123")
    .timeout(Duration.ofSeconds(5))
    .execute();

if (response.isSuccessful()) {
    String content = response.text();
    System.out.println("Response: " + content);
} else {
    System.out.println("Request failed with status: " + response.status());
}
    `.trim();

    const exampleCode2 = `
public class User {
    private String id;
    private String name;
    private String email;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

Response response = Http.get("https://api.example.com/users/123").execute();
if (response.isSuccessful()) {
    User user = response.json(User.class);
    System.out.println("User: " + user.getName() + ", Email: " + user.getEmail());
} else {
    System.out.println("Request failed with status: " + response.status());
}
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

        get("/fetch-user", ctx -> {
            Response response = Http.get("https://api.example.com/users")
                .header("Authorization", "Bearer your-token-here")
                .query("id", ctx.queryParam("id"))
                .execute();

            if (response.isSuccessful()) {
                User user = response.json(User.class);
                return ctx.json(user);
            } else {
                return ctx.status(response.status()).json(Map.of(
                    "error", "Failed to fetch user",
                    "status", response.status()
                ));
            }
        });

        post("/create-user", ctx -> {
            Map<String, String> userData = Map.of(
                "name", ctx.formParam("name"),
                "email", ctx.formParam("email")
            );

            Response response = Http.post("https://api.example.com/users")
                .header("Authorization", "Bearer your-token-here")
                .body(userData)
                .executeAsync()
                .join(); // Wait for the async request to complete

            if (response.isSuccessful()) {
                return ctx.status(HttpStatus.CREATED).json(Map.of(
                    "message", "User created successfully"
                ));
            } else {
                return ctx.status(response.status()).json(Map.of(
                    "error", "Failed to create user",
                    "status", response.status()
                ));
            }
        });
    }
}
    `.trim();

    return (
        <div id="external-api-requests" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Globe className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">External API Requests</h1>
            </div>

            <p className="text-gray-300 mb-6">
                Jolt provides a powerful and flexible way to make external API requests using the <code className="text-blue-400 bg-gray-800 px-1 rounded">Http</code> utility class, along with <code className="text-blue-400 bg-gray-800 px-1 rounded">RequestBuilder</code> and <code className="text-blue-400 bg-gray-800 px-1 rounded">Response</code>. These classes simplify constructing HTTP requests, executing them synchronously or asynchronously, and handling responses, including JSON deserialization.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        HTTP methods: Use shorthand methods like <code className="text-blue-400 bg-gray-800 px-1 rounded">Http.get()</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">Http.post()</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">Http.put()</code>, and <code className="text-blue-400 bg-gray-800 px-1 rounded">Http.delete()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Fluent API: Chain methods with <code className="text-blue-400 bg-gray-800 px-1 rounded">RequestBuilder</code> to set headers, query parameters, body, and more.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        JSON handling: Serialize request bodies and deserialize responses with <code className="text-blue-400 bg-gray-800 px-1 rounded">ObjectMapper</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Async support: Execute requests asynchronously with <code className="text-blue-400 bg-gray-800 px-1 rounded">executeAsync()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        File handling: Send files with <code className="text-blue-400 bg-gray-800 px-1 rounded">RequestBuilder.file()</code> and receive files with <code className="text-blue-400 bg-gray-800 px-1 rounded">Response.file()</code>.
                    </span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Making API Requests</h2>
            <p className="text-gray-300 mb-4">
                Use the <code className="text-blue-400 bg-gray-800 px-1 rounded">Http</code> class to create a <code className="text-blue-400 bg-gray-800 px-1 rounded">RequestBuilder</code> for an HTTP request. Configure the request with headers, query parameters, body content, and other settings, then execute it synchronously or asynchronously.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Making a GET request to fetch user data:
                </p>
                <CodeBlock code={exampleCode1} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">Http.get(String url)</code>: Creates a GET request builder.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">header(String key, String value)</code>: Adds a request header.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">query(String name, String value)</code>: Adds a query parameter.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">body(Object body)</code>: Sets a JSON-serializable request body.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">timeout(Duration timeout)</code>: Sets the request timeout.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">execute()</code>: Executes the request synchronously.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">executeAsync()</code>: Executes the request asynchronously.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Handling Responses</h2>
            <p className="text-gray-300 mb-4">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">Response</code> class provides methods to access the status, headers, and body of an API response. You can retrieve the body as a string, byte array, or deserialize it into a Java object using JSON.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Deserializing a JSON response into a Java object:
                </p>
                <CodeBlock code={exampleCode2} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">status()</code>: Gets the HTTP status code.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">header(String name)</code>: Gets a specific response header.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">headers()</code>: Gets all response headers as a map.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">text()</code>: Gets the response body as a string.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">json(Class clazz)</code>: Deserializes the response body into a Java object.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">isSuccessful()</code>: Checks if the response status is 200-299.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Usage Example</h2>
            <p className="text-gray-300 mb-4">
                Here’s a complete example of a Jolt application that makes external API requests. The application defines routes to fetch a user (GET) and create a user (POST) using an external API.
            </p>
            <div className="mb-6">
                <CodeBlock code={exampleCode3} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                In this example:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">/fetch-user</code> route makes a GET request to an external API to fetch user data and returns it as JSON.</li>
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">/create-user</code> route makes an async POST request to create a user with data from a form submission.</li>
                    <li>Responses are handled with appropriate status codes and error messages if the API request fails.</li>
                </ul>
            </p>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        With external API requests in place, explore the{' '}
                        <a href="#database" className="text-blue-400 hover:underline">
                            Database
                        </a>{' '}
                        section to learn how to use database within your Jolt application.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default ExternalApiRequest;