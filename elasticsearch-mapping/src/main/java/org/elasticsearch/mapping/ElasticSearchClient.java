package org.elasticsearch.mapping;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.MockNode;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.MockTcpTransportPlugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.util.AddressParserUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.floragunn.searchguard.ssl.SearchGuardSSLPlugin;
import com.floragunn.searchguard.ssl.util.SSLConfigConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Prepare the node to work with elastic search.
 * 
 * @author luc boutier
 */
@Component
@Slf4j
public class ElasticSearchClient {

    private Node node;
    private boolean isClient;
    private boolean isTransportClient;
    private List<InetSocketTransportAddress> adresses;
    private boolean isLocal;
    private String clusterName;
    private boolean resetData = false;
    private Client client;
    private boolean transportSSL = false;
    private String keystore = null;
    private String truststore = null;
    private String keystorePassword = null;
    private String truststorePassword = null;

    @PostConstruct
    public void initialize() throws Exception {
        if (this.isClient && this.isTransportClient) {
            // when these both option are set, we use a transport client
            Settings.Builder settingsBuilder = Settings.builder()
                .put("cluster.name", this.clusterName);
            if (transportSSL) {
               settingsBuilder = settingsBuilder
                       .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_KEYSTORE_FILEPATH, this.keystore)
                       .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_TRUSTSTORE_FILEPATH, this.truststore)
                       .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_KEYSTORE_PASSWORD, this.keystorePassword)
                       .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_TRUSTSTORE_PASSWORD, this.truststorePassword);
            }
            Settings settings = settingsBuilder.build();
            TransportClient transportClient;
            if (!transportSSL) {
               transportClient =  new PreBuiltTransportClient(settings);
            } else {
               transportClient =  new PreBuiltTransportClient(settings, SearchGuardSSLPlugin.class);
            }
            for (InetSocketTransportAddress add : adresses) {
                transportClient.addTransportAddress(add);
            }
            this.client = transportClient;
        }  else {
            // when only 'client' option is set, a node without data is initialized and joins the cluster
/************************
            this.node = NodeBuilder.nodeBuilder().client(this.isClient).clusterName(this.clusterName).local(this.isLocal).node();
            this.client = node.client();
***************************/
         Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), "target/eshome")
                                                .put("transport.type", MockTcpTransportPlugin.MOCK_TCP_TRANSPORT_NAME)
                                                .put(NetworkModule.HTTP_ENABLED.getKey(), false)
                                                .build();
         ArrayList<Class<? extends Plugin>> plugins = new ArrayList<Class<? extends Plugin>>();
         plugins.add (MockTcpTransportPlugin.class);
         MockNode node = new MockNode(settings, plugins);
         node.start();
         this.client = node.client();
         this.node = node;
        } 

//        if (this.resetData) { // removes all indices from elastic search. For Integration testing only.
            // this.node.client().admin().indices().prepareDelete().execute().actionGet();
//        }
        log.info("Initialized ElasticSearch client for cluster <" + this.clusterName + ">");
    }

    @PreDestroy
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
        if (node != null) {
            node.close();
        }
        log.info("Closed ElasticSearch client for cluster <" + this.clusterName + ">");
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
        log.debug("getStatus                : {}", response.getStatus());
        log.debug("getActivePrimaryShards   : {}", response.getActivePrimaryShards());
        log.debug("getActiveShards          : {}", response.getActiveShards());
        log.debug("getInitializingShards    : {}", response.getInitializingShards());
        log.debug("getNumberOfDataNodes     : {}", response.getNumberOfDataNodes());
        log.debug("getNumberOfNodes         : {}", response.getNumberOfNodes());
        log.debug("getRelocatingShards      : {}", response.getRelocatingShards());
        log.debug("getUnassignedShards      : {}", response.getUnassignedShards());
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

    @Value("#{elasticsearchConfig['searchguard.ssl.transport.enabled']}")
    public void setTransportSSL(Boolean transportSSL) {
        if (transportSSL != null) {
           this.transportSSL = transportSSL.booleanValue();
        }
    }

    @Value("#{elasticsearchConfig['searchguard.ssl.transport.keystore_filepath']}")
    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }

    @Value("#{elasticsearchConfig['searchguard.ssl.transport.truststore_filepath']}")
    public void setTruststore(final String truststore) {
        this.truststore = truststore;
    }

    @Value("#{elasticsearchConfig['searchguard.ssl.transport.keystore_password']}")
    public void setKeystorePassword(final String password) {
        this.keystorePassword = password;
    }

    @Value("#{elasticsearchConfig['searchguard.ssl.transport.truststore_password']}")
    public void setTruststorePassword(final String password) {
        this.truststorePassword = password;
    }

}
