import { useState } from 'react';
import { Book, Server, Shield, Globe, FileText } from 'lucide-react';
import CodeBlock from "../lib/CodeBlock";

function Configuration() {
    const [activeTab, setActiveTab] = useState('server');

    const categories = [
        { id: 'server', name: 'Server Configuration', icon: <Server size={18} /> },
        { id: 'security', name: 'Security Settings', icon: <Shield size={18} /> },
        { id: 'multipart', name: 'Multipart Settings', icon: <FileText size={18} /> },
        { id: 'language', name: 'Language & Localization', icon: <Globe size={18} /> },
    ];

    const configProperties = {
        server: [
            { key: 'server.port', defaultValue: '80', description: 'The port on which the server will listen for connections' },
            { key: 'server.tempDir', defaultValue: './tmp', description: 'Directory for temporary files' },
            { key: 'server.directory.listing', defaultValue: 'false', description: 'Enable/disable directory listing' },
            { key: 'server.directory.listing.path', defaultValue: '/directory', description: 'Path for directory listing' },
            { key: 'server.ssl.enabled', defaultValue: 'false', description: 'Enable/disable SSL support' },
            { key: 'server.ssl.port', defaultValue: '8443', description: 'The port for SSL connections' },
            { key: 'server.ssl.keyStore', defaultValue: './keystore.jks', description: 'Path to the keystore file' },
            { key: 'server.ssl.keyStorePassword', defaultValue: 'changeit', description: 'Password for the keystore' },
            { key: 'server.ssl.keyAlias', defaultValue: 'jolt', description: 'Alias for the key in the keystore' },
            { key: 'server.threads.min', defaultValue: '10', description: 'Minimum number of threads in the thread pool' },
            { key: 'server.threads.max', defaultValue: '100', description: 'Maximum number of threads in the thread pool' },
            { key: 'server.threads.timeout', defaultValue: '60000', description: 'Thread timeout in milliseconds' },
            { key: 'server.daemon', defaultValue: 'true', description: 'Run server threads as daemon threads' },
            { key: 'server.appName', defaultValue: 'Jolt', description: 'Application name used for logging and identification' }
        ],
        security: [
            { key: 'server.security.secret_key', defaultValue: '<random-base64-value>', description: 'Secret key used for various security operations (auto-generated if not specified)' },
            { key: 'server.security.pepper', defaultValue: '<random-base64-value>', description: 'Cryptographic pepper for password hashing (auto-generated if not specified)' },
            { key: 'server.security.encryption_key', defaultValue: '<random-base64-value>', description: 'Key used for data encryption (auto-generated if not specified)' }
        ],
        multipart: [
            { key: 'server.multipart.maxFileSize', defaultValue: '1048576', description: 'Maximum file size in bytes for multipart uploads (1MB default)' },
            { key: 'server.multipart.maxRequestSize', defaultValue: '10485760', description: 'Maximum request size in bytes for multipart uploads (10MB default)' },
            { key: 'server.multipart.fileSizeThreshold', defaultValue: '0', description: 'Threshold after which files will be written to disk rather than kept in memory' }
        ],
        language: [
            { key: 'server.defaultLanguage', defaultValue: 'Na', description: 'Default language for the application.' }
        ]
    };


    const exampleConfigCode = `# Server Configuration
server.port=8080
server.appName=MyJoltApp
server.ssl.enabled=true
server.ssl.port=8443

# Thread Pool Settings
server.threads.min=20
server.threads.max=200
server.threads.timeout=30000

# Enable directory listing for specific path
server.directory.listing=true
server.directory.listing.path=/public/resources

# Set default language
server.defaultLanguage=en

# Configure file uploads
server.multipart.maxFileSize=5242880  # 5MB
server.multipart.maxRequestSize=20971520  # 20MB`;

    return (
        <div id="configuration" className="p-6 w-full text-gray-200 bg-gray-900 min-h-screen">
            <div className="mb-8">
                <div className="flex items-center mb-2">
                    <Book className="mr-2 text-blue-400" size={24} />
                    <h1 className="text-2xl font-bold text-blue-400">Configuration</h1>
                </div>
                <p className="text-gray-300 mb-4">
                    Configure your Jolt application through the application.properties file. This is the heart of Jolt configuration,
                    allowing you to customize the behavior of your application without modifying code.
                </p>
                <div className="h-px bg-gray-700 w-full my-4"></div>
            </div>

            <div className="flex flex-wrap gap-2 mb-6">
                {categories.map((category) => (
                    <button
                        key={category.id}
                        onClick={() => setActiveTab(category.id)}
                        className={`cursor-pointer flex items-center px-4 py-2 rounded-md transition ${activeTab === category.id
                            ? 'bg-blue-600 text-white'
                            : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                            }`}
                    >
                        <span className="mr-2">{category.icon}</span>
                        {category.name}
                    </button>
                ))}
            </div>

            <div className="bg-gray-800 rounded-lg overflow-hidden shadow-lg">
                <div className="p-4 bg-gray-700 border-b border-gray-600">
                    <h2 className="text-lg font-semibold text-white">
                        {categories.find(c => c.id === activeTab)?.name} Properties
                    </h2>
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead>
                            <tr className="bg-gray-700">
                                <th className="px-4 py-3 text-left text-sm font-medium text-gray-300">Property Key</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-gray-300">Default Value</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-gray-300">Description</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-700">
                            {configProperties[activeTab as keyof typeof configProperties].map((prop, index) => (
                                <tr key={prop.key} className={index % 2 === 0 ? 'bg-gray-800' : 'bg-gray-750'}>
                                    <td className="px-4 py-3 font-mono text-sm text-blue-400">{prop.key}</td>
                                    <td className="px-4 py-3 font-mono text-sm text-green-400">{prop.defaultValue}</td>
                                    <td className="px-4 py-3 text-sm text-gray-300">{prop.description}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {activeTab === 'security' && (
                <div className="mt-6 p-4 bg-blue-900/30 border border-blue-700/50 rounded-lg">
                    <h3 className="text-blue-400 font-medium mb-2 flex items-center">
                        <Shield size={18} className="mr-2" />
                        Special Note on Security Properties
                    </h3>
                    <p className="text-gray-300 text-sm">
                        Security properties like <code className="bg-gray-800 px-1 py-0.5 rounded text-blue-400">secret_key</code>,{' '}
                        <code className="bg-gray-800 px-1 py-0.5 rounded text-blue-400">pepper</code>, and{' '}
                        <code className="bg-gray-800 px-1 py-0.5 rounded text-blue-400">encryption_key</code> are automatically
                        generated with strong random values if not specified. However, as they are auto-generated, they are not to be used in production code.
                    </p>
                </div>
            )}

            {activeTab === 'language' && (
                <div className="mt-6 p-4 bg-blue-900/30 border border-blue-700/50 rounded-lg">
                    <h3 className="text-blue-400 font-medium mb-2 flex items-center">
                        <Globe size={18} className="mr-2" />
                        Special Note on <code className="bg-gray-800 ms-1 px-1 py-0.5 rounded text-blue-400">server.defaultLanguage</code>
                    </h3>
                    <p className="text-gray-300 text-sm">
                        You don't have to define it at all. If you don't, you simply won't have access to the <strong className="text-blue-400">lang</strong> properties in the freemarker templates
                        For more information, see <a className="text-blue-400" href="#localization">Localization</a>
                    </p>
                </div>
            )}

            <div className="mt-6">
                <h3 className="text-lg font-semibold text-white mb-2">Example Configuration</h3>
                <CodeBlock code={exampleConfigCode} language="properties" />
            </div>

            <div className="mt-6">
                <h3 className="text-lg font-semibold text-white mb-2">Loading Configuration</h3>
                <p className="text-gray-300 mb-4">
                    Jolt automatically loads the <code className="bg-gray-800 px-1 py-0.5 rounded text-blue-400">application.properties</code> file
                    from the classpath.
                </p>
            </div>
        </div>
    );
}

export default Configuration;