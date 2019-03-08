package org.elasticsearch.mapping;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.util.AddressParserUtil;
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
    private boolean isTransportClient;
    private List<InetSocketTransportAddress> adresses;
    private boolean isLocal;
    private String clusterName;
    private boolean resetData = false;
    private Client client;

    @PostConstruct
    public void initialize() {
        if (this.isClient && this.isTransportClient) {
            // when these both option are set, we use a transport client
            Settings settings = Settings.settingsBuilder()
                .put("cluster.name", this.clusterName)
                .build();
            TransportClient transportClient = TransportClient.builder().settings(settings).build();
            for (InetSocketTransportAddress add : adresses) {
                transportClient.addTransportAddress(add);
            }
            this.client = transportClient;
        } else {
            // when only 'client' option is set, a node without data is initialized and joins the cluster
            this.node = NodeBuilder.nodeBuilder().client(this.isClient).clusterName(this.clusterName).local(this.isLocal).node();
            this.client = node.client();
        }

//        if (this.resetData) { // removes all indices from elastic search. For Integration testing only.
            // this.node.client().admin().indices().prepareDelete().execute().actionGet();
//        }
        LOGGER.info("Initialized ElasticSearch client for cluster <" + this.clusterName + ">");
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
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
        return this.client;
    }

    /**
     * Wait for green status for the given indices.
     * 
     * @param indices The indices to wait for.
     * @return A {@link ClusterHealthResponse} that contains the cluster health after waiting maximum 5 minutes for green status.
     */
    public ClusterHealthResponse waitForGreenStatus(String... indices) {
        ClusterHealthRequestBuilder builder = new ClusterHealthRequestBuilder(this.client.admin().cluster(), ClusterHealthAction.INSTANCE);
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
        //LOGGER.debug("getAllValidationFailures : {}", response.getAllValidationFailures());
        return response;
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

    @Value("#{elasticsearchConfig['elasticSearch.transportClient']}")
    public void setTransportClient(final String isTransportClient) {
        this.isTransportClient = Boolean.parseBoolean(isTransportClient);
    }

    @Value("#{elasticsearchConfig['elasticSearch.hosts']}")
    public void setHosts(final String hosts) {
        this.adresses = AddressParserUtil.parseHostCsvList(hosts);
    }

    @Value("#{elasticsearchConfig['elasticSearch.resetData']}")
    public void setResetData(final boolean resetData) {
        this.resetData = resetData;
    }
}
