package org.elasticsearch.mapping;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.client.Client;
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
	private Node node;
	private boolean isClient;
	private boolean isLocal;
	private String clusterName;
	private boolean resetData = false;

	@PostConstruct
	public void initialize() {
		this.node = NodeBuilder.nodeBuilder().client(this.isClient).clusterName(this.clusterName).local(this.isLocal)
				.node();
		if (this.resetData) { // removes all indices from elastic search. For Integration testing only.
			this.node.client().admin().indices().prepareDelete(new String[] {}).execute().actionGet();
		}
	}

	/**
	 * Get the elastic search client.
	 * 
	 * @return The elastic search client.
	 */
	public Client getClient() {
		return this.node.client();
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