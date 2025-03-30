// Localization.jsx
import { Languages, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function Localization() {
    const exampleCode1 = `
// /resources/lang/en.json
{
    "greetings": {
        "welcome": "Welcome, {{name}}!",
        "goodbye": "Goodbye, {{name}}!"
    },
    "menu": {
        "items": [
            "Home",
            "About",
            "Contact"
        ]
    }
}

// /resources/lang/fr.json
{
    "greetings": {
        "welcome": "Bienvenue, {{name}}!",
        "goodbye": "Au revoir, {{name}}!"
    },
    "menu": {
        "items": [
            "Accueil",
            "À propos",
            "Contact"
        ]
    }
}
    `.trim();

    const exampleCode2 = `
<!-- template.ftl -->
<!DOCTYPE html>
<html>
<head>
    <title>\${lang.greetings.welcome ? replace("{{name}}", user.name)}</title>
</head>
<body>
    <h1>\${lang.greetings.welcome ? replace("{{name}}", user.name)}</h1>
    <p>Language: \${lang.languageCode}</p>

    <h2>Menu</h2>
    <ul>
        <#list lang.menu.items as item>
            <li>\${item}</li>
        </#list>
    </ul>

    <p>\${lang.greetings.goodbye ? replace("{{name}}", user.name)}</p>
</body>
</html>
    `.trim();

    const exampleCode3 = `
public class MyApp extends JoltApplication {
    public static void main(String[] args) {
        launch(MyApp.class);
    }

    @Override
    protected void setup() {
        get("/welcome", ctx -> {
            JoltModel model = new JoltModel();
            Map<String, Object> user = new HashMap<>();
            user.put("name", "Alice");
            model.with("user", user);

            // Optionally set the language (defaults to system locale if not set)
            LanguageService.changeLanguage("fr"); // Use french for the website

            return ctx.render("template.ftl", model);
        });
    }
}
    `.trim();

    return (
        <div className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Languages className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Localization</h1>
            </div>

            <p className="text-gray-300 mb-6">
                Jolt supports localization to make your application accessible in multiple languages. By storing language data in JSON files within the <code className="text-blue-400 bg-gray-800 px-1 rounded">/resources/lang</code> folder, you can easily access localized strings in your FreeMarker templates using the <code className="text-blue-400 bg-gray-800 px-1 rounded">lang</code> object.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Setting Up Localization</h2>
            <p className="text-gray-300 mb-4">
                Place your language files in the <code className="text-blue-400 bg-gray-800 px-1 rounded">/resources/lang</code> folder. Each file should be named after its language code (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">en.json</code> for English, <code className="text-blue-400 bg-gray-800 px-1 rounded">fr.json</code> for French) and contain a JSON object with nested keys and arrays for your localized strings.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Language files for English and French:
                </p>
                <CodeBlock code={exampleCode1} language="json" />
            </div>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Using Localization in Templates</h2>
            <p className="text-gray-300 mb-4">
                In your FreeMarker templates, access localized strings using the <code className="text-blue-400 bg-gray-800 px-1 rounded">lang</code> object with dot notation (e.g., <code className="text-blue-400 bg-gray-800 px-1 rounded">lang.greetings.welcome</code>). You can also access the current language code with <code className="text-blue-400 bg-gray-800 px-1 rounded">lang.languageCode</code>. Use FreeMarker’s <code className="text-blue-400 bg-gray-800 px-1 rounded">?replace</code> to substitute placeholders in strings.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: A FreeMarker template using localization:
                </p>
                <CodeBlock code={exampleCode2} language="html" />
            </div>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Usage Example</h2>
            <p className="text-gray-300 mb-4">
                Here's a simple example of a Jolt application that renders a FreeMarker template with localized content. The application sets the language to French and renders a welcome page.
            </p>
            <div className="mb-6">
                <CodeBlock code={exampleCode3} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                In this example:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li>The language files in <code className="text-blue-400 bg-gray-800 px-1 rounded">/resources/lang</code> define localized strings for English and French.</li>
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">/welcome</code> route sets the language to French with <code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.setLanguage("fr")</code> and renders the template.</li>
                    <li>The template uses <code className="text-blue-400 bg-gray-800 px-1 rounded">lang</code> to display localized strings and the language code.</li>
                </ul>
            </p>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        With localization set up, explore the{' '}
                        <a href="#security" className="text-blue-400 hover:underline">
                            Security
                        </a>{' '}
                        section to learn how to secure your Jolt application with authentication and authorization.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Localization;