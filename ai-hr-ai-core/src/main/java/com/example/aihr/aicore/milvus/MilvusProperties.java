package com.example.aihr.aicore.milvus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aihr.milvus")
public class MilvusProperties {
    /**
     * Hostname or IP where Milvus is reachable (default: localhost).
     */
    private String host = "localhost";

    /**
     * Milvus gRPC port (default: 19530).
     */
    private int port = 19530;

    /**
     * Collection name used for policy chunks.
     */
    private String collection = "ai_hr_policy_chunks";

    /**
     * Optional database name (Milvus 2.4+). Default: default.
     */
    private String database = "default";

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String uri() {
        return "http://" + host + ":" + port;
    }
}

