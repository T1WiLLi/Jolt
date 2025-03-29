// Forms.jsx
import { FileText, Code, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function Forms() {
    const exampleCode1 = `
Form form = ctx.buildForm(); // Retrieve all avaible data, query, body and www-x-form-urlencoded data

form.field("username")
    .required("Username is required")
    .minLength(3, "Username must be at least 3 characters long")
    .alphanumeric("Username must contain only letters and numbers");

form.field("email")
    .required("Email is required")
    .email("Invalid email format");

form.field("age")
    .required("Age is required")
    .asInt()
    .min(18, "You must be at least 18 years old");

if (form.verify()) {
    ctx.text("Form is valid!");
} else {
    Map<String, String> errors = form.getErrors();
    ctx.status(HttpStatus.BAD_REQUEST).json(errors);
}
    `.trim();

    const exampleCode2 = `
public class User {
    private String username;
    private String email;
    private int age;

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}

Form form = ctx.buildForm();
if (form.verify()) {
    User user = form.buildEntity(User.class);
    ctx.json(user);
} else {
    ctx.status(HttpStatus.BAD_REQUEST).json(form.getErrors());
}
    `.trim();

    const exampleCode3 = `
public class MyApp extends JoltApplication {
    public static void main(String[] args) {
        launch(MyApp.class);
    }

    @Override
    protected void setup() {
        post("/register", ctx -> {
            Form form = ctx.buildForm();
            
            // Of course, one could assume that field validation is either part of the service or even a new type of layer !
            // But for the sake of simplicity, let's assume that the form is validated here.

            form.field("username")
                .required("Username is required")
                .minLength(3, "Username must be at least 3 characters long")
                .alphanumeric("Username must contain only letters and numbers");

            form.field("email")
                .required("Email is required")
                .email("Invalid email format");

            form.field("age")
                .required("Age is required")
                .asInt()
                .min(18, "You must be at least 18 years old");

            form.field("birthdate")
                .required("Birthdate is required")
                .date("yyyy-MM-dd", "Invalid date format. Use yyyy-MM-dd");

            if (form.verify()) {
                User user = form.buildEntity(User.class);
                return ctx.json(user);
            } else {
                return ctx.status(HttpStatus.BAD_REQUEST).json(form.getErrors());
            }
        });
    }
}
    `.trim();

    return (
        <div className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <FileText className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Forms</h1>
            </div>

            <p className="text-gray-300 mb-6">
                Handling form submissions is a core part of web applications. In Jolt, the <code className="text-blue-400 bg-gray-800 px-1 rounded">Form</code> class provides a powerful and flexible way to validate form data, while the <code className="text-blue-400 bg-gray-800 px-1 rounded">FieldValidator</code> class allows you to define validation rules for individual fields. Together, they enable robust form processing, error handling, and data conversion.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Fluent validation: Use <code className="text-blue-400 bg-gray-800 px-1 rounded">FieldValidator</code> to define rules like <code className="text-blue-400 bg-gray-800 px-1 rounded">required()</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">email()</code>, and <code className="text-blue-400 bg-gray-800 px-1 rounded">minLength()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Error handling: Access validation errors with <code className="text-blue-400 bg-gray-800 px-1 rounded">getErrors()</code> or <code className="text-blue-400 bg-gray-800 px-1 rounded">getAllErrors()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Data conversion: Convert form data to entities with <code className="text-blue-400 bg-gray-800 px-1 rounded">buildEntity()</code> or models with <code className="text-blue-400 bg-gray-800 px-1 rounded">buildModel()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Type conversion: Parse field values as <code className="text-blue-400 bg-gray-800 px-1 rounded">Integer</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">Double</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">Boolean</code>, or <code className="text-blue-400 bg-gray-800 px-1 rounded">LocalDate</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Conditional validation: Apply rules conditionally with <code className="text-blue-400 bg-gray-800 px-1 rounded">when()</code>.
                    </span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Validating Form Data</h2>
            <p className="text-gray-300 mb-4">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">Form</code> class allows you to validate form data by defining rules with <code className="text-blue-400 bg-gray-800 px-1 rounded">FieldValidator</code>. You can set field values, define validation rules, and check if the form is valid using <code className="text-blue-400 bg-gray-800 px-1 rounded">verify()</code>. If validation fails, errors can be retrieved with <code className="text-blue-400 bg-gray-800 px-1 rounded">getErrors()</code>.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Validating a registration form:
                </p>
                <CodeBlock code={exampleCode1} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">setValue(String, String)</code>: Set a field's value.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">field(String)</code>: Get a <code className="text-blue-400 bg-gray-800 px-1 rounded">FieldValidator</code> for a field.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">required()</code>: Ensure the field is not empty.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">email()</code>: Validate the field as an email address.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">minLength(int)</code>: Enforce a minimum length for the field.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">asInt()</code>: Interpret the field as an integer.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">min(int)</code>: Ensure the field's value is at least a specified number.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">verify()</code>: Validate all fields and return <code className="text-blue-400 bg-gray-800 px-1 rounded">true</code> if valid.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getErrors()</code>: Retrieve validation errors.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Converting Form Data</h2>
            <p className="text-gray-300 mb-4">
                After validation, you can convert form data into a Java object using <code className="text-blue-400 bg-gray-800 px-1 rounded">buildEntity()</code> or into a <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltModel</code> for template rendering with <code className="text-blue-400 bg-gray-800 px-1 rounded">buildModel()</code>. The <code className="text-blue-400 bg-gray-800 px-1 rounded">Form</code> class also supports type conversion for fields (e.g., to <code className="text-blue-400 bg-gray-800 px-1 rounded">Integer</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">LocalDate</code>).
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Converting form data into a <code className="text-blue-400 bg-gray-800 px-1 rounded">User</code> entity:
                </p>
                <CodeBlock code={exampleCode2} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">buildEntity(Class)</code>: Convert form data into a Java object.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">buildModel()</code>: Convert form data into a <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltModel</code>.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getValueAsInt(String)</code>: Parse a field as an integer.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getValueAsDate(String)</code>: Parse a field as a <code className="text-blue-400 bg-gray-800 px-1 rounded">LocalDate</code>.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Usage Example</h2>
            <p className="text-gray-300 mb-4">
                Here's a complete example of handling a form submission in a Jolt application. The application defines a <code className="text-blue-400 bg-gray-800 px-1 rounded">/register</code> route that validates user input and converts the form data into a <code className="text-blue-400 bg-gray-800 px-1 rounded">User</code> entity.
            </p>
            <div className="mb-6">
                <CodeBlock code={exampleCode3} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                In this example:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li>The <code className="text-blue-400 bg-gray-800 px-1 rounded">/register</code> route processes a form submission with fields for username, email, age, and birthdate.</li>
                    <li>Each field is validated using <code className="text-blue-400 bg-gray-800 px-1 rounded">FieldValidator</code> with rules like <code className="text-blue-400 bg-gray-800 px-1 rounded">required()</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">email()</code>, and <code className="text-blue-400 bg-gray-800 px-1 rounded">date()</code>.</li>
                    <li>If validation passes, the form data is converted into a <code className="text-blue-400 bg-gray-800 px-1 rounded">User</code> entity and returned as JSON.</li>
                    <li>If validation fails, the errors are returned with a 400 status code.</li>
                </ul>
            </p>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        With form handling in place, explore the{' '}
                        <a href="#file-uploads" className="text-blue-400 hover:underline">
                            Files Uploads
                        </a>{' '}
                        section to learn how to allow users to upload files.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Forms;