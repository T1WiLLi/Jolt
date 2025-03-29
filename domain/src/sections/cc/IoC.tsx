// IoC.jsx
import { Box, Code, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function IoC() {
    const exampleCode1 = `
@JoltBean
public class MyService {
    public String getMessage() {
        return "Hello from MyService!";
    }
}

@JoltBean
public class MyController {
    @JoltBeanInjection
    private MyService myService;

    public String handleRequest() {
        return myService.getMessage();
    }
}
    `.trim();

    const exampleCode2 = `
MyController controller = JoltContainer.getInstance().getBean(MyController.class);
String message = controller.handleRequest(); // Returns "Hello from MyService!"
    `.trim();

    const exampleCode3 = `
public class MyApp extends JoltApplication {
    public static void main(String[] args) {
        launch(MyApp.class);
    }

    @Override
    protected void setup() {
        get("/message", ctx -> {
            MyController controller = JoltContainer.getInstance().getBean(MyController.class);
            return ctx.text(controller.handleRequest());
        });
    }
}
    `.trim();

    return (
        <div id="Ioc" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Box className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Inversion of Control (IoC)</h1>
            </div>

            <p className="text-gray-300 mb-6">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContainer</code> class is a lightweight dependency injection (IoC) container in the Jolt framework. It manages the lifecycle of beans, performs dependency injection, and ensures proper initialization and shutdown. By scanning for classes annotated with <code className="text-blue-400 bg-gray-800 px-1 rounded">@JoltBean</code>, the container automatically registers and wires dependencies, making it easy to build modular and testable applications.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Bean discovery: Automatically scans packages for classes annotated with <code className="text-blue-400 bg-gray-800 px-1 rounded">@JoltBean</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Dependency injection: Injects dependencies into fields annotated with <code className="text-blue-400 bg-gray-800 px-1 rounded">@JoltBeanInjection</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Lifecycle management: Invokes <code className="text-blue-400 bg-gray-800 px-1 rounded">@PostConstruct</code> and <code className="text-blue-400 bg-gray-800 px-1 rounded">@PreDestroy</code> methods.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Singleton support: Maintains single instances for beans with singleton scope.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Exception handling: Detects circular dependencies and throws <code className="text-blue-400 bg-gray-800 px-1 rounded">CircularDependencyException</code>.
                    </span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Bean Discovery and Registration</h2>
            <p className="text-gray-300 mb-4">
                <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContainer</code> scans specified packages for classes annotated with <code className="text-blue-400 bg-gray-800 px-1 rounded">@JoltBean</code>. It validates these classes (ensuring they are concrete and have a public no-argument constructor) and registers them with a unique name. The name is either specified in the <code className="text-blue-400 bg-gray-800 px-1 rounded">@JoltBean</code> annotation’s <code className="text-blue-400 bg-gray-800 px-1 rounded">value</code> attribute or derived by lowercasing the first letter of the class’s simple name.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Defining and registering beans:
                </p>
                <CodeBlock code={exampleCode1} language="js" />
            </div>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Dependency Injection</h2>
            <p className="text-gray-300 mb-4">
                The container automatically injects dependencies into fields annotated with <code className="text-blue-400 bg-gray-800 px-1 rounded">@JoltBeanInjection</code>. It also supports manual injection into non-managed objects using the <code className="text-blue-400 bg-gray-800 px-1 rounded">inject()</code> method.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Retrieving a bean with injected dependencies:
                </p>
                <CodeBlock code={exampleCode2} language="js" />
            </div>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Lifecycle Management</h2>
            <p className="text-gray-300 mb-4">
                <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContainer</code> manages the lifecycle of beans by invoking methods annotated with <code className="text-blue-400 bg-gray-800 px-1 rounded">@PostConstruct</code> after bean creation and <code className="text-blue-400 bg-gray-800 px-1 rounded">@PreDestroy</code> during container shutdown. It also eagerly initializes non-lazy singleton beans during container initialization.
            </p>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">initialize()</code>: Initializes eager singleton beans and invokes <code className="text-blue-400 bg-gray-800 px-1 rounded">@PostConstruct</code> methods.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">shutdown()</code>: Invokes <code className="text-blue-400 bg-gray-800 px-1 rounded">@PreDestroy</code> methods and releases resources.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Usage Example</h2>
            <p className="text-gray-300 mb-4">
                Here's a complete example of using <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContainer</code> in a Jolt application. The container scans for beans, initializes them, and makes them available for use in route handlers.
            </p>
            <div className="mb-6">
                <CodeBlock code={exampleCode3} language="js" />
            </div>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        With dependency injection set up, explore the{' '}
                        <a href="#filters" className="text-blue-400 hover:underline">
                            Filters
                        </a>{' '}
                        section to learn how to add cross-cutting concerns like logging and authentication to your Jolt application.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default IoC;