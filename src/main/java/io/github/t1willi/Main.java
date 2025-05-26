package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.openapi.annotations.OpenApi;

@OpenApi(title = "User management API", version = "1.0.0", description = "API for managing user data in the Jolt Application", termsOfService = "https://example.com/terms", contactName = "William Beaudin", contactUrl = "https://example.com/contact", contactEmail = "william.beaudin@t1willi.io", licenseName = "Apache-2.0", licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.html", path = "/openapi.json")
public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }
}
