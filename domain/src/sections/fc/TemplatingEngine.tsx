// TemplatingEngine.jsx
import { FileCode, Code, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function TemplatingEngine() {
    const exampleCode1 = `
<!-- template.ftl -->
<!DOCTYPE html>
<html>
<head>
    <title>Welcome Page</title>
</head>
<body>
    <h1>Welcome, \${user.name}!</h1>

    <#if user.age >= 18>
        <p>You are eligible to vote.</p>
    <#else>
        <p>You are not eligible to vote yet.</p>
    </#if>

    <h2>Your Hobbies:</h2>
    <ul>
        <#list user.hobbies as hobby>
            <li>\${hobby}</li>
        <#else>
            <li>No hobbies listed.</li>
        </#list>
    </ul>

    <h2>Your Friends:</h2>
    <#if user.friends?has_content>
        <ul>
            <#list user.friends as friend>
                <li>\${friend.name} (\${friend.age} years old)</li>
            </#list>
        </ul>
    <#else>
        <p>You have no friends listed.</p>
    </#if>
</body>
</html>
    `.trim();

    const exampleCode2 = `
public class MyApp extends JoltApplication {
    public static void main(String[] args) {
        launch(MyApp.class);
    }

    @Override
    protected void setup() {
        get("/welcome", ctx -> {
            JoltModel model = new JoltModel();

            // Sample user data
            Map<String, Object> user = new HashMap<>();
            user.put("name", "Alice");
            user.put("age", 25);

            // List of hobbies
            List<String> hobbies = Arrays.asList("Reading", "Hiking", "Painting");
            user.put("hobbies", hobbies);

            // List of friends
            List<Map<String, Object>> friends = new ArrayList<>();
            Map<String, Object> friend1 = new HashMap<>();
            friend1.put("name", "Bob");
            friend1.put("age", 30);
            Map<String, Object> friend2 = new HashMap<>();
            friend2.put("name", "Charlie");
            friend2.put("age", 22);
            friends.add(friend1);
            friends.add(friend2);
            user.put("friends", friends);

            model.with("user", user);

            return ctx.render("template.ftl", model);
        });
    }
}
    `.trim();

    return (
        <div id="templating-engine" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <FileCode className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Templating Engine</h1>
            </div>

            <p className="text-gray-300 mb-6">
                Jolt uses <a href="https://freemarker.apache.org/" className="text-blue-400 hover:underline">FreeMarker</a> as its templating engine to render dynamic HTML pages. FreeMarker allows you to create reusable templates with dynamic content by embedding variables, conditionals, loops, and more. This section covers the basics of using FreeMarker in Jolt, including variable interpolation, <code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#if&gt;</code> for conditionals, and <code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#list&gt;</code> for loops.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features of FreeMarker</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Variable interpolation: Embed dynamic values using <code className="text-blue-400 bg-gray-800 px-1 rounded">{'${variable}'}</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Conditionals: Use <code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#if&gt;</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#else&gt;</code> for conditional logic.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Loops: Iterate over lists with <code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#list&gt;</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Reusability: Create reusable templates with dynamic data passed via <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltModel</code>.
                    </span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Basic FreeMarker Usage</h2>
            <p className="text-gray-300 mb-4">
                FreeMarker templates are written in <code className="text-blue-400 bg-gray-800 px-1 rounded">.ftl</code> files and use special directives to render dynamic content. Below is an example of a FreeMarker template that demonstrates variable interpolation, conditionals, and loops.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: A FreeMarker template (<code className="text-blue-400 bg-gray-800 px-1 rounded">template.ftl</code>):
                </p>
                <CodeBlock code={exampleCode1} language="html" />
            </div>
            <p className="text-gray-300 mb-4">
                Key FreeMarker directives:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">{'${variable}'}</code>: Interpolates a variable's value (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">{'${user.name}'}</code>).</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#if condition&gt;</code>: Evaluates a condition and renders content if true (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">{'&lt;#if user.age >= 18&gt;'}</code>). Learn more in the <a href="https://freemarker.apache.org/docs/ref_directive_if.html" className="text-blue-400 hover:underline">FreeMarker if directive documentation</a>.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#else&gt;</code>: Provides an alternative block if the <code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#if&gt;</code> condition is false.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#list items as item&gt;</code>: Loops over a list and renders content for each item (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">&lt;#list user.hobbies as hobby&gt;</code>). Learn more in the <a href="https://freemarker.apache.org/docs/ref_directive_list.html" className="text-blue-400 hover:underline">FreeMarker list directive documentation</a>.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">?has_content</code>: Checks if a sequence or collection has content (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">user.friends?has_content</code>).</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Usage Example</h2>
            <p className="text-gray-300 mb-4">
                Here's a complete example of a Jolt application that renders the FreeMarker template shown above. The application prepares a <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltModel</code> with user data and renders the template using <code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.render()</code>.
            </p>
            <div className="mb-6">
                <CodeBlock code={exampleCode2} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                In this example:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">/welcome</code> route prepares a <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltModel</code> with user data, including name, age, hobbies, and friends.</li>
                    <li>The data is passed to <code className="text-blue-400 bg-gray-800 px-1 rounded">template.ftl</code>, which uses FreeMarker directives to render the dynamic content.</li>
                    <li>The rendered HTML is returned to the client, displaying a personalized welcome page.</li>
                </ul>
            </p>

            <p className="text-gray-300">
                I do <span className="font-semibold">recommend</span> checking out the Freemarker documentation for more information on the directives and features available.
            </p>
            <a
                href="https://freemarker.apache.org/" target="_blank" rel="noopener noreferrer"
                className="text-blue-400 hover:text-blue-300 transition duration-200"
            >
                Freemarker Documentation
            </a>

            <div className="bg-gray-800 p-4 mt-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        Now that you're familiar with FreeMarker basics, explore the{' '}
                        <a href="#security" className="text-blue-400 hover:underline">
                            Localization
                        </a>{' '}
                        section to learn how to use localization within your Jolt application.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default TemplatingEngine;