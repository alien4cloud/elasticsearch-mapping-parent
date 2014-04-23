package org.elasticsearch.mapping;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prepare the node to work with elastic search.
 * 
 * @author luc boutier
 */
@Component
public class ElasticSearchClient {

    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);

    private Node node;
    private boolean isClient;
    private boolean isLocal;
    private String clusterName;
    private boolean resetData = false;

    @PostConstruct
    public void initialize() {
        this.node = NodeBuilder.nodeBuilder().client(this.isClient).clusterName(this.clusterName).local(this.isLocal).node();

        if (this.resetData) { // removes all indices from elastic search. For Integration testing only.
            // this.node.client().admin().indices().prepareDelete().execute().actionGet();
        }
        LOGGER.info("Initialized ElasticSearch client for cluster <" + this.clusterName + ">");
    }

    @PreDestroy
    public void close() {
        if (node != null) {
            node.close();
        }
        LOGGER.info("Closed ElasticSearch client for cluster <" + this.clusterName + ">");
    }

    /**
     * Get the elastic search client.
     * 
     * @return The elastic search client.
     */
    public Client getClient() {
        return this.node.client();
    }

    /**
     * Wait for green status for the given indices.
     * 
     * @param indices The indices to wait for.
     * @return A {@link ClusterHealthResponse} that contains the cluster health after waiting maximum 5 minutes for green status.
     */
    public ClusterHealthResponse waitForGreenStatus(String... indices) {
        ClusterHealthRequestBuilder builder = new ClusterHealthRequestBuilder(node.client().admin().cluster());
        builder.setIndices(indices);
        builder.setWaitForGreenStatus();
        builder.setTimeout(TimeValue.timeValueSeconds(30));
        ClusterHealthResponse response = builder.execute().actionGet();
        LOGGER.debug("getStatus                : {}", response.getStatus());
        LOGGER.debug("getActivePrimaryShards   : {}", response.getActivePrimaryShards());
        LOGGER.debug("getActiveShards          : {}", response.getActiveShards());
        LOGGER.debug("getInitializingShards    : {}", response.getInitializingShards());
        LOGGER.debug("getNumberOfDataNodes     : {}", response.getNumberOfDataNodes());
        LOGGER.debug("getNumberOfNodes         : {}", response.getNumberOfNodes());
        LOGGER.debug("getRelocatingShards      : {}", response.getRelocatingShards());
        LOGGER.debug("getUnassignedShards      : {}", response.getUnassignedShards());
        LOGGER.debug("getAllValidationFailures : {}", response.getAllValidationFailures());
        return response;
    }

    @PreDestroy
    public void tearDown() {
        this.node.close();
    }

    @Value("#{elasticsearchConfig['elasticSearch.clusterName']}")
    public void setClusterName(final String clusterName) {
        this.clusterName = clusterName;
    }

    @Value("#{elasticsearchConfig['elasticSearch.local']}")
    public void setLocal(final boolean isLocal) {
        this.isLocal = isLocal;
    }

    @Value("#{elasticsearchConfig['elasticSearch.client']}")
    public void setClient(final boolean isClient) {
        this.isClient = isClient;
    }

    @Value("#{elasticsearchConfig['elasticSearch.resetData']}")
    public void setResetData(final boolean resetData) {
        this.resetData = resetData;
    }
}