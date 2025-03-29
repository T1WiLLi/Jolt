// Installation.jsx
import { Download, AlertTriangle, Rocket } from 'lucide-react';
import CodeBlock from '../lib/CodeBlock';

function Installation() {
    return (
        <div id="installation" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Download className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Installation</h1>
            </div>

            <p className="text-gray-300 mb-6">
                Jolt can be installed in your project using two methods: a recommended GitHub templating project or directly through Maven. I strongly recommend using the GitHub template for a smoother setup and deployment process.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Recommended: GitHub Templating Project</h2>
            <p className="text-gray-300 mb-4">
                The easiest way to get started with Jolt is by using our official GitHub template. This template includes a pre-configured <code className="text-blue-400 bg-gray-800 px-1 rounded">pom.xml</code>, a <code className="text-blue-400 bg-gray-800 px-1 rounded">Dockerfile</code>, a <code className="text-blue-400 bg-gray-800 px-1 rounded">Docker-Compose</code> file, and a sample project structure to help you hit the ground running.
            </p>
            <div className="bg-gray-800 p-4 rounded-md mb-6">
                <div className="flex items-start">
                    <svg className="w-6 h-6 fill-blue-600 mr-3 flex-shrink-0" role="img" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><title>GitHub</title><path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12" /></svg>
                    <div>
                        <h3 className="text-lg font-medium text-gray-100 mb-2">Steps to Use the Template</h3>
                        <ol className="list-decimal list-inside text-gray-300 space-y-2">
                            <li>
                                Visit the official Jolt template repository on GitHub:{' '}
                                <a
                                    href="https://github.com/T1WiLLi/jolt-template"
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="text-blue-400 hover:underline"
                                >
                                    github.com/jolt-framework/jolt-template
                                </a>
                            </li>
                            <li>Click on the <strong>"Use this template"</strong> button to create a new repository for your project.</li>
                            <li>Clone your new repository to your local machine.</li>
                            <li>Run <code className="text-blue-400 bg-gray-800 px-1 rounded">mvn install</code> to install dependencies.</li>
                            <li className="text-gray-100">
                                You can start the project using different techniques:
                                <ul className="space-y-2 list-none pl-5">
                                    <li className="flex items-center text-gray-400">
                                        <span className="mr-2">•</span>
                                        Press <code className="text-blue-400 bg-gray-800 px-1 rounded">Run</code> in your IDE.
                                    </li>
                                    <li className="flex items-center text-gray-400">
                                        <span className="mr-2">•</span>
                                        Using Maven CLI <code className="text-blue-400 bg-gray-800 px-1 rounded">mvn run</code>
                                    </li>
                                    <li className="flex items-center text-gray-400">
                                        <span className="mr-2">•</span>
                                        Using Docker <code className="text-blue-400 bg-gray-800 px-1 rounded">docker compose up</code>
                                    </li>
                                </ul>
                            </li>
                        </ol>
                    </div>
                </div>
            </div>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Alternative: Maven Dependency</h2>
            <p className="text-gray-300 mb-4">
                You can also add Jolt to your project directly via Maven. However, this method requires you to manually create a <code className="text-blue-400 bg-gray-800 px-1 rounded">Dockerfile</code> and configure your <code className="text-blue-400 bg-gray-800 px-1 rounded">pom.xml</code>, which can complicate the deployment process.
            </p>
            <div className="bg-gray-800 p-4 rounded-md mb-6">
                <div className="flex items-start">
                    <AlertTriangle className="w-6 h-6 text-yellow-400 mr-3 flex-shrink-0" />
                    <div>
                        <h3 className="text-lg font-medium text-gray-100 mb-2">Steps to Install via Maven</h3>
                        <p className="text-gray-400 mb-3">
                            Add the following dependency to your <code className="text-blue-400 bg-gray-800 px-1 rounded">pom.xml</code>:
                        </p>
                        <CodeBlock code="<dependency>
                                <groupId>io.github.t1willi</groupId>
                                <artifactId>jolt</artifactId>
                                <version>2.5.6</version>
                            </dependency>" language='xml' />
                        <p className="text-yellow-400 mt-3">
                            <strong>Warning:</strong> You'll need to manually create a <code className="text-blue-400 bg-gray-800 px-1 rounded">Dockerfile</code> and ensure your <code className="text-blue-400 bg-gray-800 px-1 rounded">pom.xml</code> is properly configured for deployment, which can be error-prone.
                        </p>
                    </div>
                </div>
            </div>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        Once Jolt is installed, head over to the{' '}
                        <a href="#configuration" className="text-blue-400 hover:underline">
                            Configuration
                        </a>{' '}
                        section to set up your application and start building with Jolt.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Installation;