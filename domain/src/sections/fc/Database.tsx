import { Database as DatabaseIcon, Server, Code, Shield, RefreshCw, Rocket } from 'lucide-react';
import CodeBlock from "../../lib/CodeBlock";

const brokerCodeExample = `
// Create a broker for a User entity
public class UserBroker extends RestBroker<Long, User> {
    public UserBroker() {
        super("users", User.class, Long.class);
    }
    
    // Custom finder methods can be added here
    public Optional<User> findByEmail(String email) {
        return selectOne("SELECT * FROM users WHERE email = ?", email);
    }
}
`.trim();

const usageExample = `
// Getting a user by ID
UserBroker userBroker = new UserBroker();
Optional<User> user = userBroker.findById(1L);

// Creating a new user
User newUser = new User();
newUser.setName("John Doe");
newUser.setEmail("john@example.com");
User savedUser = userBroker.save(newUser);

// Querying by criteria
Map<String, Object> criteria = new HashMap<>();
criteria.put("status", "active");
List<User> activeUsers = userBroker.findByCriteria(criteria);
`.trim();

function Database() {
    return (
        <div id="database" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto">
            <div className="flex items-center mb-6">
                <DatabaseIcon className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Database Layer</h1>
            </div>

            <p className="text-gray-300 mb-6">
                The database layer in Jolt provides a robust abstraction for database operations through a set of broker classes.
                This design simplifies CRUD operations while maintaining security and flexibility.
            </p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                <div className="bg-gray-800 rounded-md p-5">
                    <div className="flex items-center mb-3">
                        <Server className="w-6 h-6 text-green-400 mr-2" />
                        <h2 className="text-xl font-semibold text-green-400">Base Broker</h2>
                    </div>
                    <p className="text-gray-300 mb-3">
                        The <code className="bg-gray-700 px-1 rounded">Broker&lt;T&gt;</code> class provides core database functionality:
                    </p>
                    <ul className="list-disc pl-6 text-gray-300 space-y-2">
                        <li>SQL query execution with parameter binding</li>
                        <li>Result set mapping to entity objects</li>
                        <li>Connection management and transaction handling</li>
                        <li>SQL security validation</li>
                    </ul>
                </div>

                <div className="bg-gray-800 rounded-md p-5">
                    <div className="flex items-center mb-3">
                        <RefreshCw className="w-6 h-6 text-purple-400 mr-2" />
                        <h2 className="text-xl font-semibold text-purple-400">REST Broker</h2>
                    </div>
                    <p className="text-gray-300 mb-3">
                        The <code className="bg-gray-700 px-1 rounded">RestBroker&lt;ID, T&gt;</code> extends the base broker with CRUD operations:
                    </p>
                    <ul className="list-disc pl-6 text-gray-300 space-y-2">
                        <li>Finding entities by ID or criteria</li>
                        <li>Intelligent save operation (insert or update)</li>
                        <li>Entity deletion</li>
                        <li>Pagination support</li>
                    </ul>
                </div>
            </div>

            <div className="mb-8">
                <div className="flex items-center mb-4">
                    <Code className="w-6 h-6 text-white-400 mr-2" />
                    <h2 className="text-2xl font-semibold text-white-400">Implementation Example</h2>
                </div>
                <p className="text-gray-300 mb-4">
                    Creating a broker for your entity is straightforward:
                </p>
                <div className="bg-gray-800 rounded-md p-4 overflow-x-auto">
                    <CodeBlock code={brokerCodeExample} language="js" />
                </div>
            </div>

            <div className="mb-8">
                <div className="flex items-center mb-4">
                    <Code className="w-6 h-6 text-white-400 mr-2" />
                    <h2 className="text-2xl font-semibold text-white-400">Usage Example</h2>
                </div>
                <div className="bg-gray-800 rounded-md p-4 overflow-x-auto">
                    <CodeBlock code={usageExample} language="js" />
                </div>
            </div>

            <div className="bg-gray-800 p-5 rounded-md mb-6">
                <div className="flex items-center mb-3">
                    <Shield className="w-6 h-6 text-red-400 mr-2" />
                    <h2 className="text-xl font-semibold text-red-400">Security Features</h2>
                </div>
                <p className="text-gray-300 mb-3">
                    The database layer includes built-in security measures:
                </p>
                <ul className="list-disc pl-6 text-gray-300 space-y-2">
                    <li>SQL injection prevention through prepared statements</li>
                    <li>Validation of SQL identifiers against reserved keywords</li>
                    <li>Transaction management with proper rollback handling</li>
                    <li>Comprehensive error handling with detailed exceptions</li>
                </ul>
            </div>

            <div className="bg-blue-900 bg-opacity-40 p-5 mb-6 rounded-md border border-blue-700">
                <h3 className="text-lg font-semibold text-blue-400 mb-2">Key Advantages</h3>
                <ul className="list-disc pl-6 text-gray-300 space-y-1">
                    <li>Type-safe operations with generics</li>
                    <li>Automatic mapping between database rows and entity objects</li>
                    <li>Centralized connection management</li>
                    <li>Extensible design for custom query methods</li>
                    <li>Robust error handling with detailed exception information</li>
                </ul>
            </div>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        With the Brokers in place, explore the{' '}
                        <a href="#template-engine" className="text-blue-400 hover:underline">
                            Templating Engine
                        </a>{' '}
                        section to learn about freemarker features in Jolt.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Database;