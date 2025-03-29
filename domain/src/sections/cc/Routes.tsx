// Routes.jsx
import { Link, Code, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function Routes() {
    const exampleCode1 = `
get("/users/{id:int}", ctx -> {
    int userId = ctx.path("id").asInt();
    // handle logic
    return ctx.html("User ID: " + userId);
});
    `.trim();

    const exampleCode2 = `
get("/users/{id}", ctx -> {
    String userId = ctx.path("id").get(); // Return the path as a string by default
    // handle logic
    return ctx.html("User ID: " + userId);
});
    `.trim();

    const exampleCode3 = `
post("/users", ctx -> {
    // Handle POST request to create a new user
    return ctx.json(Map.of("message","User created!"));
});

get("/users/{id:double}", ctx -> {
    double userId = ctx.path("id").asDouble();
    return ctx.plain("User ID (double): " + userId);
});
    `.trim();

    return (
        <div id="routes" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Link className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Routes</h1>
            </div>

            <p className="text-gray-300 mb-6">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">Route</code> class in Jolt is responsible for mapping HTTP methods and path patterns to specific handlers. Jolt provides a powerful routing system that supports path parameters with optional type constraints, such as <code className="text-blue-400 bg-gray-800 px-1 rounded">{`{id:int}`}</code> for integers or <code className="text-blue-400 bg-gray-800 px-1 rounded">{`{id}`}</code> for strings. Routes are defined using the DSL provided by <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltApplication</code>.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Typed path parameters: Differentiate routes based on parameter types (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">/user/{'{id:int}'}</code> for integers vs. <code className="text-blue-400 bg-gray-800 px-1 rounded">{`/user/{id}`}</code> for strings).
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Support for multiple HTTP methods: Define routes for <code className="text-blue-400 bg-gray-800 px-1 rounded">GET</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">POST</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">PUT</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">DELETE</code>, and more.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Regex-based path matching: Path parameters are compiled into regex patterns for efficient matching.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Access path parameters in handlers using the <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> object.
                    </span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Typed Path Parameters</h2>
            <p className="text-gray-300 mb-4">
                Jolt allows you to define routes with typed path parameters, which enables you to differentiate routes based on the type of the parameter. For example, <code className="text-blue-400 bg-gray-800 px-1 rounded">/user/{'{id:int}'}</code> matches an integer ID, while <code className="text-blue-400 bg-gray-800 px-1 rounded">{`/user/{id}`}</code> matches a string ID. The supported types are <code className="text-blue-400 bg-gray-800 px-1 rounded">int</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">double</code>, and string (default if no type is specified).
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Handling an integer ID:
                </p>
                <CodeBlock code={exampleCode1} language="js" />
            </div>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Handling a string ID:
                </p>
                <CodeBlock code={exampleCode2} language="js" />
            </div>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Defining Routes</h2>
            <p className="text-gray-300 mb-4">
                Routes are defined in the <code className="text-blue-400 bg-gray-800 px-1 rounded">setup()</code> method of your <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltApplication</code> subclass using methods like <code className="text-blue-400 bg-gray-800 px-1 rounded">get()</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">post()</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">put()</code>, and <code className="text-blue-400 bg-gray-800 px-1 rounded">delete()</code>. You can also use <code className="text-blue-400 bg-gray-800 px-1 rounded">route()</code> to specify a custom HTTP method.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Defining routes for different HTTP methods and parameter types:
                </p>
                <CodeBlock code={exampleCode3} language="js" />
            </div>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        With routes defined, learn how to use the <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> in the{' '}
                        <a href="#joltcontext" className="text-blue-400 hover:underline" id="joltcontext">
                            JoltContext
                        </a>{' '}
                        section to handle request data and build dynamic responses.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Routes;