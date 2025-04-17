package io.github.t1willi.injector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.core.BaseController;
import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.injector.annotation.JoltBean;
import io.github.t1willi.injector.annotation.JoltConfiguration;

/**
 * The BeanScanner is responsible for scanning a given package for classes
 * annotated with
 * {@link JoltBean} and {@link JoltConfiguration}, and delegating registration
 * to the appropriate managers.
 */
final class BeanScanner {

    private static final Logger logger = Logger.getLogger(BeanScanner.class.getName());
    private final BeanRegistry beanRegistry;
    private final ConfigurationManager configurationManager;

    public BeanScanner(BeanRegistry beanRegistry, ConfigurationManager configurationManager) {
        this.beanRegistry = beanRegistry;
        this.configurationManager = configurationManager;
    }

    /**
     * Scans the specified base package for bean and configuration classes.
     *
     * @param basePackage the package to scan.
     * @throws JoltDIException if scanning fails.
     */
    public void scanPackage(String basePackage) {
        logger.info(() -> "Scanning package: " + basePackage);
        try {
            String path = basePackage.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);

            if (!resources.hasMoreElements()) {
                throw new JoltDIException("Package not found: " + basePackage);
            }

            List<Class<?>> classes = new LinkedList<>();

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                classes.addAll(findClasses(resource, basePackage));
            }

            registerClasses(classes);

        } catch (Exception e) {
            throw new JoltDIException("Failed to scan package: " + basePackage, e);
        }
    }

    private void registerClasses(List<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(JoltConfiguration.class)) {
                configurationManager.registerConfiguration(clazz);
            }
            if (clazz.isAnnotationPresent(JoltBean.class) || clazz.isAnnotationPresent(Controller.class)) {
                if (clazz.isAnnotationPresent(Controller.class)) {
                    if (!BaseController.class.isAssignableFrom(clazz)) {
                        throw new JoltDIException("Controller must implement BaseController: " + clazz.getName());
                    }
                }
                if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers())) {
                    throw new JoltDIException("Bean must be a concrete class: " + clazz.getName());
                }
                beanRegistry.registerBean(clazz);
            }
        }
    }

    private List<Class<?>> findClasses(URL resource, String packageName) throws URISyntaxException {
        List<Class<?>> classes = new LinkedList<>();
        String protocol = resource.getProtocol();

        if ("file".equals(protocol)) {
            classes.addAll(findClassesFromDirectory(new File(resource.toURI()), packageName));
        } else if ("jar".equals(protocol)) {
            classes.addAll(findClassesFromJar(resource, packageName));
        } else {
            logger.warning(() -> "Unsupported protocol: " + protocol + " for resource: " + resource);
        }

        return classes;
    }

    private List<Class<?>> findClassesFromDirectory(File directory, String packageName) {
        List<Class<?>> classes = new LinkedList<>();

        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClassesFromDirectory(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                loadClass(classes, className);
            }
        }

        return classes;
    }

    private List<Class<?>> findClassesFromJar(URL resource, String packageName) {
        List<Class<?>> classes = new LinkedList<>();

        try {
            String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));

            if (jarPath.contains("%20")) {
                jarPath = jarPath.replace("%20", " ");
            }

            processJarFile(classes, jarPath, packageName);

        } catch (IOException e) {
            logger.warning("Failed to process JAR file: " + e.getMessage());
        }
        return classes;
    }

    private void processJarFile(List<Class<?>> classes, String jarPath, String packageName) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath)) {
            String packagePath = packageName.replace('.', '/') + "/";

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();

                    if (entryName.startsWith("/")) {
                        entryName = entryName.substring(1);
                    }

                    if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                        String className = entryName.replace('/', '.')
                                .substring(0, entryName.length() - 6);

                        if (className.startsWith(".")) {
                            className = className.substring(1);
                        }
                        loadClass(classes, className);
                    }
                }
            }
        }
    }

    private void loadClass(List<Class<?>> classes, String className) {
        try {
            classes.add(Class.forName(className));
        } catch (ClassNotFoundException e) {
            logger.warning("Failed to load class: " + className + " - " + e.getMessage());
        }
    }
}
