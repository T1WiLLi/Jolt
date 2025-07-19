package io.github.t1willi.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.template.TemplateConfiguration;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.template.TemplateEngine;
import io.github.t1willi.template.TemplateEngineConfig;
import io.github.t1willi.security.config.CsrfConfiguration;
import io.github.t1willi.security.config.SecurityConfiguration;
import io.github.t1willi.utils.Flash;

@ExtendWith(MockitoExtension.class)
public class TemplatingContextTest {
    static class DummyEngine implements TemplateEngine {
        JoltModel lastModel;

        @Override
        public String render(String templatePath, JoltModel model) {
            this.lastModel = model;
            return "<!-- rendered -->";
        }

        @Override
        public void initialize(TemplateEngineConfig config) {
            // no-op
        }

        @Override
        public String getName() {
            return "dummy";
        }

        @Override
        public String getFileExtension() {
            return ".dummy";
        }

        @Override
        public boolean isInitialized() {
            return true;
        }
    }

    private MockedStatic<JoltContainer> joltContainerStatic;
    private MockedStatic<JoltDispatcherServlet> joltDispatcherStatic;
    private JoltContainer fakeContainer;
    private DummyEngine engine;
    private TemplatingContext templating;
    private HttpServletRequest fakeReq;
    private HttpServletResponse fakeResp;

    @BeforeEach
    void setUp() {
        // Prepare dummy template engine
        engine = new DummyEngine();
        TemplateConfiguration fakeConfig = mock(TemplateConfiguration.class);
        when(fakeConfig.getEngine()).thenReturn(engine);

        // Prepare dummy security config for CSRF
        SecurityConfiguration fakeSec = mock(SecurityConfiguration.class);
        CsrfConfiguration fakeCsrf = mock(CsrfConfiguration.class);
        when(fakeCsrf.getTokenName()).thenReturn("csrf_token");
        when(fakeSec.getCsrfConfig()).thenReturn(fakeCsrf);

        // Mock JoltContainer to return our fake beans
        joltContainerStatic = mockStatic(JoltContainer.class);
        fakeContainer = mock(JoltContainer.class);
        joltContainerStatic.when(JoltContainer::getInstance).thenReturn(fakeContainer);
        when(fakeContainer.getBean(TemplateConfiguration.class)).thenReturn(fakeConfig);
        when(fakeContainer.getBean(SecurityConfiguration.class)).thenReturn(fakeSec);

        // Create a JoltContext backed by mocked servlet request/response
        fakeReq = mock(HttpServletRequest.class);
        fakeResp = mock(HttpServletResponse.class);
        JoltContext fakeJoltCtx = new JoltContext(fakeReq, fakeResp, null, List.of());

        // Mock dispatcher to return our fake context for Flash
        joltDispatcherStatic = mockStatic(JoltDispatcherServlet.class);
        joltDispatcherStatic.when(JoltDispatcherServlet::getCurrentContext).thenReturn(fakeJoltCtx);

        // Instantiate the templating context now that beans are stubbed
        templating = new TemplatingContext();
    }

    @AfterEach
    void tearDown() {
        joltDispatcherStatic.close();
        joltContainerStatic.close();
    }

    @Test
    void flashSurvivesMergeThenInject() {
        // Arrange: set a flash message (populates ThreadLocal + response cookies)
        Flash.success("Voilà!");

        // Arrange: user model data
        JoltModel userModel = JoltModel.of("name", "Will");

        // Act: render via templating
        ResponseContext ctx = new ResponseContext(fakeResp);
        templating.render(ctx, "index", userModel);

        // Assert: engine saw the merged+injected model
        JoltModel m = engine.lastModel;
        assertNotNull(m, "Engine should have seen a model");
        assertEquals("Will", m.get("name"));
        assertTrue(m.getKeys().contains("flash"), "Model must contain 'flash'");

        // Assert: flash wrapper exposes the message and type
        var fw = (TemplatingContext.FlashWrapper) m.get("flash");
        assertTrue(fw.has(), "FlashWrapper.has() should be true");
        assertEquals("Voilà!", fw.message());
        assertEquals("success", fw.type());
    }
}