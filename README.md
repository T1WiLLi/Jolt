[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=T1WiLLi_Jolt&metric=coverage)](https://sonarcloud.io/summary/new_code?id=T1WiLLi_Jolt) [![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=T1WiLLi_Jolt&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=T1WiLLi_Jolt) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=T1WiLLi_Jolt&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=T1WiLLi_Jolt) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=T1WiLLi_Jolt&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=T1WiLLi_Jolt) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=T1WiLLi_Jolt&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=T1WiLLi_Jolt) [![Maven Central](https://img.shields.io/maven-central/v/io.github.t1willi/jolt.svg)](https://central.sonatype.com/artifact/io.github.t1willi/jolt) [![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# **Jolt Development is currently on hold !**
> Hi, I'm William, the creator of Jolt. First of all, I want to let you know that i'm not 
> abandoning the Jolt project, but I'm currently working full-time on my internship, while 
> I absolutely love working on Jolt, It's simply not possible for me to keep up with the pace.
> I believe that Jolt has reach a point where it is stable and bugfree enough to be used in
> production, and I'm proud of what was achieved so far. I'm not entierly sure when I'll be able to start working on Jolt again. In the meantime, I'll keep an eye on the issues and will try to answer any questions you may have. Regarding external contributions, I'll be happy to review and merge any pull requests, but I won't be able to provide any rapid feedback. I apologize for any inconvenience this may cause. While Jolt is on hold, some of my other projects might be more active, one of which being the [GitNar Project](https://github.com/T1WiLLi/Gitnar) which you can learn more about on the repository's README. 

> Thank you for your understanding and I wish you all the best with your projects.

# **Jolt**

### A lightweight ⚡ yet enterprise‑grade Java web framework

> **Jolt** is a pragmatic Java framework that lets you ship clean, maintainable web services in minutes – without losing the power you need tomorrow.

## ✨ Why Jolt?

Jolt is a simplistic, easy to learn and performant framework, allowing you to easily implement any web application, ranging from REST APIs to SPAs and full-fledged websites. It combines the best of both worlds: the simplicity of modern microframeworks with the power and extensibility needed for enterprise applications.

-   **🚀 Fast development** - Get started in minutes with minimal configuration
-   **💪 Powerful features** - Enterprise-grade capabilities when you need them
-   **🧩 Modular design** - Use only what you need, nothing more
-   **🔒 Security-focused** - Built with modern security practices in mind
-   **📦 Zero dependencies** - Core functionality without bloat

## 🚀 Quick Start

### Installation

Add Jolt to your Maven project:

```xml
<dependency>
    <groupId>io.github.t1willi</groupId>
    <artifactId>jolt</artifactId>
    <version>{latest.version}</version>
</dependency>
```

Or with Gradle:

```gradle
implementation 'io.github.t1willi:jolt:{latest.version}'
```

### Hello World Example

```java
import io.github.t1willi.jolt.Jolt;

public class HelloWorld extends JoltApplication {
    public static void main(String[] args) {
        launch(HelloWorld.class);
    }
	
	@Override
	public void init() {
		get("", ctx -> ctx.html("Hello World!")); // Up at http://localhost
	}
}
```

## 🛠️ Key Features

### Intuitive Routing

```java
// Basic routing
get("/users", ctx -> {
    // Handle GET request
});

post("/users", ctx -> {
    // Handle POST request
});

// Path parameters
get("/users/{id}", ctx -> {
    String userId = ctx.path("id");
    ctx.result("User ID: " + userId);
});

// Route grouping
group("/api", 2, () -> { // The version of the route '2' -> "/v2/api/..."
    get("/users", UserController::getAllUsers);
    post("/users", UserController::createUser);
});
```

### Powerful Dependency Injection

```java
// Define a service
@JoltBean
public class UserService {
    public List<User> findAllUsers() {
        // Implementation
    }
}

// Inject and use the service
@Controller("[controller]") // "http://localhost/user"
public class UserController {
    
    @JoltBeanInjection
    private final UserService userService;
    
    @Get
    public List<User> getAllUsers() {
        return userService.findAllUsers();
    }
	
	@Get("{id}")
	public JoltContext getUser(JoltContext ctx) {
		return userService.findUserById(ctx.path("id"));
	}
}
```

## 📚 Documentation & Resources

-   **[Official Documentation](https://jolt-framework.org)** - Comprehensive guides and API references
-   **[Tutorials](https://jolt-framework.org/tutorials)** - Step-by-step tutorials for beginners
-   **[Maven Central](https://central.sonatype.com/artifact/io.github.t1willi/jolt)** - Download the latest release

## 💡 About Jolt

Hi! I'm T1WiLLi, the creator of Jolt. I started working on Jolt, not because I wanted to create something for others, but because at the time I was actually working a lot with the Spring framework, which, I must say, is probably the best Java framework out there.

However, people that know me know that I've always loved to learn and experiment with new things. So, as I started learning Spring, I also started to wonder, how did they do this? How did they make it so powerful? And, I started to think, what if I could do something similar, but with a more lightweight approach? And, that's how Jolt was born.

Of course, the idea of creating a new framework was a bit scary at first, but I was convinced that I could make something that would help me get a better understanding of how web frameworks work, and, maybe, even help others.

_**And, that's exactly what I did.**_

## 🙏 Acknowledgements

Jolt draws inspiration from several excellent frameworks and projects:

-   **David Tucker - Zephyrus**: For the Form system, security integrations, and best practices
-   **Javalin**: For the DSL approach and extensive configuration options
-   **Jakarta EE**: For the enterprise-grade components which Jolt is composed of

## 🤝 Contributing

Contributions are welcome! Whether you want to fix a bug, add a feature, or improve documentation, your help is appreciated.

1.  Fork the repository
2.  Create your feature branch (`git checkout -b feature/amazing-feature`)
3.  Commit your changes (`git commit -m 'Add some amazing feature'`)
4.  Push to the branch (`git push origin feature/amazing-feature`)
5.  Open a Pull Request

## 📄 License

Jolt is licensed under the MIT License. See [LICENSE](LICENSE) for details.