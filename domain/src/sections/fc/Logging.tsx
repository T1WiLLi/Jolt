import { FileText, Rocket } from "lucide-react";
import CodeBlock from "../../lib/CodeBlock";

const exampleCode1 = `
private static final Logger logger = Logger.getLogger(YourClass.class.getName());
`.trim();

function Logging() {
    return (
        <div id="logging" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <FileText className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Logging</h1>
            </div>

            <p className="text-gray-300 mb-6">
                Jolt uses the <span className="font-medium text-gray-200">Java Util Logging (JUL)</span> API for logging.
                This allows developers to efficiently track application behavior with a lightweight, built-in solution.
            </p>

            <div className="bg-gray-800 rounded-md p-4 overflow-x-auto">
                <CodeBlock code={exampleCode1} language="js" />
            </div>

            <div className="bg-gray-800 p-4 rounded-md flex items-start mt-6">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-semibold text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        Now that logging is covered, check out the{' '}
                        <a href="#cookies" className="text-blue-400 hover:underline">
                            Cookies
                        </a>{' '}
                        section to explore additional Jolt features.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Logging;
