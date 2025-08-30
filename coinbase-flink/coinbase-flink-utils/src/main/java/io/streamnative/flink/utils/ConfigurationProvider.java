package io.streamnative.flink.utils;

import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Unified configuration provider for Flink jobs that supports:
 * 1. Kubernetes ConfigMaps (mounted as files)
 * 2. Local Docker mount points
 * 3. Environment variables
 * 4. Command line parameters
 * 5. Default fallback values
 */
public class ConfigurationProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationProvider.class);
    
    // Configuration paths
    private static final String K8S_CONFIG_PATH = "/etc/flink/config/pulsar.properties";
    private static final String DOCKER_CONFIG_PATH = "/opt/flink/conf/pulsar.properties";
    private static final String CLASSPATH_CONFIG = "/pulsar-config.properties";
    
    // Environment variable names
    private static final String PULSAR_SERVICE_URL = "PULSAR_SERVICE_URL";
    private static final String PULSAR_ADMIN_URL = "PULSAR_ADMIN_URL";
    private static final String PULSAR_AUTH_PLUGIN = "PULSAR_AUTH_PLUGIN";
    private static final String PULSAR_AUTH_PARAMS = "PULSAR_AUTH_PARAMS";
    private static final String PULSAR_TENANT = "PULSAR_TENANT";
    private static final String PULSAR_NAMESPACE = "PULSAR_NAMESPACE";
    
    // Default values
    private static final String DEFAULT_TENANT = "feeds";
    private static final String DEFAULT_NAMESPACE = "realtime";
    private static final String DEFAULT_AUTH_PLUGIN = "org.apache.pulsar.client.impl.auth.AuthenticationToken";
    
    private final Properties properties;
    private final ParameterTool parameterTool;
    
    public ConfigurationProvider(String[] args) {
        this.parameterTool = ParameterTool.fromArgs(args);
        this.properties = loadConfiguration();
        
        LOG.info("Configuration loaded from: {}", determineConfigSource());
        LOG.info("Pulsar configuration: {}", getPulsarConfig());
    }
    
    public ConfigurationProvider(ParameterTool parameterTool) {
        this.parameterTool = parameterTool;
        this.properties = loadConfiguration();
        
        LOG.info("Configuration loaded from: {}", determineConfigSource());
        LOG.info("Pulsar configuration: {}", getPulsarConfig());
    }
    
    /**
     * Get Pulsar configuration with proper precedence:
     * 1. Command line parameters
     * 2. Environment variables
     * 3. Configuration files (K8s ConfigMap, Docker mount, classpath)
     * 4. Default values
     */
    public PulsarConfig getPulsarConfig() {
        String serviceUrl = getProperty("pulsar.service.url", PULSAR_SERVICE_URL, 
                "pulsar://localhost:6650");
        String adminUrl = getProperty("pulsar.admin.url", PULSAR_ADMIN_URL, 
                "http://localhost:8080");
        String authPlugin = getProperty("pulsar.auth.plugin", PULSAR_AUTH_PLUGIN, 
                null);
        String authParams = getProperty("pulsar.auth.params", PULSAR_AUTH_PARAMS, 
                null);
        String tenant = getProperty("pulsar.tenant", PULSAR_TENANT, 
                DEFAULT_TENANT);
        String namespace = getProperty("pulsar.namespace", PULSAR_NAMESPACE, 
                DEFAULT_NAMESPACE);
        
        return new PulsarConfig(serviceUrl, adminUrl, authPlugin, authParams, tenant, namespace);
    }
    
    /**
     * Get a configuration property with fallback precedence
     */
    public String getProperty(String propertyKey, String envKey, String defaultValue) {
        // 1. Command line parameter
        if (parameterTool.has(propertyKey)) {
            String value = parameterTool.get(propertyKey);
            LOG.debug("Using command line parameter for {}: {}", propertyKey, value);
            return value;
        }
        
        // 2. Environment variable
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            LOG.debug("Using environment variable {} for {}: {}", envKey, propertyKey, envValue);
            return envValue;
        }
        
        // 3. Configuration file
        String propValue = properties.getProperty(propertyKey);
        if (propValue != null && !propValue.trim().isEmpty()) {
            LOG.debug("Using configuration file property for {}: {}", propertyKey, propValue);
            return propValue;
        }
        
        // 4. Default value
        LOG.debug("Using default value for {}: {}", propertyKey, defaultValue);
        return defaultValue;
    }
    
    public String getRequiredProperty(String propertyKey, String envKey) {
        String value = getProperty(propertyKey, envKey, null);
        if (value == null) {
            throw new IllegalArgumentException(
                String.format("Required property '%s' (env: '%s') not found", propertyKey, envKey));
        }
        return value;
    }
    
    public int getIntProperty(String propertyKey, String envKey, int defaultValue) {
        String value = getProperty(propertyKey, envKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid integer value '{}' for property '{}', using default: {}", 
                    value, propertyKey, defaultValue);
            return defaultValue;
        }
    }
    
    public boolean getBooleanProperty(String propertyKey, String envKey, boolean defaultValue) {
        String value = getProperty(propertyKey, envKey, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
    
    public ParameterTool getParameterTool() {
        return parameterTool;
    }
    
    private Properties loadConfiguration() {
        Properties config = new Properties();
        
        // Try to load from different sources in order of preference
        String[] configPaths = {K8S_CONFIG_PATH, DOCKER_CONFIG_PATH};
        
        for (String configPath : configPaths) {
            if (loadFromFile(config, configPath)) {
                return config;
            }
        }
        
        // Fallback to classpath resource
        loadFromClasspath(config, CLASSPATH_CONFIG);
        
        return config;
    }
    
    private boolean loadFromFile(Properties config, String filePath) {
        Path path = Paths.get(filePath);
        if (Files.exists(path) && Files.isReadable(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                config.load(is);
                LOG.info("Loaded configuration from file: {}", filePath);
                return true;
            } catch (IOException e) {
                LOG.warn("Failed to load configuration from {}: {}", filePath, e.getMessage());
            }
        } else {
            LOG.debug("Configuration file not found or not readable: {}", filePath);
        }
        return false;
    }
    
    private boolean loadFromClasspath(Properties config, String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                config.load(is);
                LOG.info("Loaded configuration from classpath resource: {}", resourcePath);
                return true;
            } else {
                LOG.debug("Configuration resource not found in classpath: {}", resourcePath);
            }
        } catch (IOException e) {
            LOG.warn("Failed to load configuration from classpath {}: {}", resourcePath, e.getMessage());
        }
        return false;
    }
    
    private String determineConfigSource() {
        if (Files.exists(Paths.get(K8S_CONFIG_PATH))) {
            return "Kubernetes ConfigMap (" + K8S_CONFIG_PATH + ")";
        } else if (Files.exists(Paths.get(DOCKER_CONFIG_PATH))) {
            return "Docker mount (" + DOCKER_CONFIG_PATH + ")";
        } else if (getClass().getResourceAsStream(CLASSPATH_CONFIG) != null) {
            return "Classpath resource (" + CLASSPATH_CONFIG + ")";
        } else {
            return "Environment variables and defaults";
        }
    }
}