// JoltApplication.jsx
import { Code, Server, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function JoltApplication() {
    const exampleCode = `
public class MyApp extends JoltApplication {

    public static void main(String[] args) {
        launch(MyApp.class);
    }

    @Override
    protected void setup() {
        get("/", ctx -> ctx.plain("Hello, World!"));
        get("/user/{id}", ctx -> ctx.html("Hello, User #" + ctx.path("id").get()));
    }
}
    `.trim();

    return (
        <div id="jolt" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Server className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">JoltApplication</h1>
            </div>

            <p className="text-gray-300 mb-6">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltApplication</code> class is the core entry point for any Jolt application. It serves as an abstract base class that sets up the embedded Tomcat server, initializes the IoC container, and provides a simple DSL for defining routes. Subclasses of <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltApplication</code> implement the <code className="text-blue-400 bg-gray-800 px-1 rounded">setup()</code> method to configure routes and additional settings.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Embedded Tomcat server for easy deployment and management.</span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Simple routing DSL to define HTTP routes (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">get()</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">post()</code>).</span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Single-instance enforcement to prevent multiple instances per JVM.</span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Automatic IoC container initialization and bean scanning.</span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Built-in logging and startup configuration for better debugging.</span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Minimal Example</h2>
            <p className="text-gray-300 mb-4">
                Below is a minimal example of a Jolt application that defines two routes: one for the root path (<code className="text-blue-400 bg-gray-800 px-1 rounded">/</code>) and another for a dynamic user path (<code className="text-blue-400 bg-gray-800 px-1 rounded">/user/{'{id}'}</code>).
            </p>
            <div className="bg-gray-800 p-4 rounded-md mb-6">
                <CodeBlock code={exampleCode} language="js" />
            </div>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        Now that you understand the basics of <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltApplication</code>, explore the{' '}
                        <a href="#routes" className="text-blue-400 hover:underline">
                            Routes
                        </a>{' '}
                        section to learn how to define more complex routes and handle HTTP requests.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default JoltApplication;