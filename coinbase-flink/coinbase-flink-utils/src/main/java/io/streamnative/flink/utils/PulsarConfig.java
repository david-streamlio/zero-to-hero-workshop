package io.streamnative.flink.utils;

import java.io.Serializable;
import java.util.Objects;

/**
 * Configuration class for Pulsar connection settings.
 * Contains all necessary parameters for connecting to Pulsar clusters.
 */
public class PulsarConfig implements Serializable {
    
    private final String serviceUrl;
    private final String adminUrl;
    private final String authPlugin;
    private final String authParams;
    private final String tenant;
    private final String namespace;
    
    public PulsarConfig(String serviceUrl, String adminUrl, String authPlugin, 
                       String authParams, String tenant, String namespace) {
        this.serviceUrl = Objects.requireNonNull(serviceUrl, "serviceUrl cannot be null");
        this.adminUrl = Objects.requireNonNull(adminUrl, "adminUrl cannot be null");
        this.authPlugin = authPlugin;
        this.authParams = authParams;
        this.tenant = Objects.requireNonNull(tenant, "tenant cannot be null");
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
    }
    
    public String getServiceUrl() {
        return serviceUrl;
    }
    
    public String getAdminUrl() {
        return adminUrl;
    }
    
    public String getAuthPlugin() {
        return authPlugin;
    }
    
    public String getAuthParams() {
        return authParams;
    }
    
    public String getTenant() {
        return tenant;
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public boolean hasAuth() {
        return authPlugin != null && authParams != null;
    }
    
    public String getTopicPrefix() {
        return String.format("persistent://%s/%s/", tenant, namespace);
    }
    
    @Override
    public String toString() {
        return "PulsarConfig{" +
                "serviceUrl='" + serviceUrl + '\'' +
                ", adminUrl='" + adminUrl + '\'' +
                ", authPlugin='" + authPlugin + '\'' +
                ", tenant='" + tenant + '\'' +
                ", namespace='" + namespace + '\'' +
                ", hasAuth=" + hasAuth() +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PulsarConfig that = (PulsarConfig) o;
        return Objects.equals(serviceUrl, that.serviceUrl) &&
                Objects.equals(adminUrl, that.adminUrl) &&
                Objects.equals(authPlugin, that.authPlugin) &&
                Objects.equals(authParams, that.authParams) &&
                Objects.equals(tenant, that.tenant) &&
                Objects.equals(namespace, that.namespace);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(serviceUrl, adminUrl, authPlugin, authParams, tenant, namespace);
    }
}