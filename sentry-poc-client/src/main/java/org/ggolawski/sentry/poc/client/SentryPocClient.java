package org.ggolawski.sentry.poc.client;

import java.util.Collections;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient.Builder;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentryPocClient {
  private static final Logger log = LoggerFactory.getLogger(SentryPocClient.class);

  public static void main(String[] args) throws Exception {
    log.debug("Starting client...");
    QueryRequest request = new QueryRequest(new SolrQuery("*:*"));
    request.setBasicAuthCredentials(args[0], "");
    log.debug("Creating client...");
    try (CloudSolrClient client = new Builder(Collections.singletonList("127.0.0.1:2181/solr"), Optional.empty())
        .build()) {
      log.debug("Sending request...");
      QueryResponse rsp = request.process(client, "simpleCollection");
      log.debug("Getting results...");
      SolrDocumentList docList = rsp.getResults();
      log.debug(docList.toString());
    } catch (SolrServerException e) {
      if (e.getRootCause().getMessage().contains("User not found in LDAP")) {
        System.exit(2);
      }
      throw e;
    }
  }
}