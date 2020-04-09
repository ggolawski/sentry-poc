package org.ggolawski.sentry.poc.server;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

import org.apache.hadoop.conf.Configuration;
import org.apache.sentry.service.thrift.SentryService;
import org.apache.sentry.service.thrift.SentryServiceFactory;
import org.apache.sentry.service.common.ServiceConstants.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.cloud.ZkTestServer;
import org.apache.solr.common.cloud.SolrZkClient;

public class SentryPocServer implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(SentryPocServer.class);

  private static final int NUM_SERVERS = 3;

  private final Path dbDir;
  private final SentryService sentryService;
  private static final Path testDataPath = FileSystems.getDefault().getPath("tmp");

  public SentryPocServer(Path testDir) throws Exception {
    this.dbDir = testDir.resolve("sentry_policy_db");
    this.sentryService = SentryServiceFactory.create(getServerConfig());
  }

  private static SentryPocServer createSentryServer() throws Exception {
    SentryPocServer server = new SentryPocServer(testDataPath);
    server.startSentryService();
    log.info("Successfully started Sentry service");

    MiniSolrCloudCluster cluster = buildCluster();
    log.info("Successfully started Solr service");

    String collectionName = "simpleCollection";
    try (CloudSolrClient client = cluster.getSolrClient()) {
      CollectionAdminRequest.createCollection(collectionName, "poc", NUM_SERVERS, 1).process(client);
      client.commit(collectionName, true, true);
    }

    return server;
  }

  private static MiniSolrCloudCluster buildCluster() throws Exception {
    ZkTestServer zkServer = new ZkTestServer(testDataPath.resolve("zookeeper/server1/data").toString(), 2181);
    zkServer.run();
    Path solrDir = new File(SentryPocServer.class.getClassLoader().getResource("solr").toURI()).toPath();
    try (SolrZkClient zkClient = new SolrZkClient(zkServer.getZkHost(), 4500)) {
      zkClient.makePath("/solr/security.json", Files.readAllBytes(solrDir.resolve("security").resolve("security.json")),
          true);
    }

    MiniSolrCloudCluster cluster = new MiniSolrCloudCluster(NUM_SERVERS, testDataPath,
        MiniSolrCloudCluster.DEFAULT_CLOUD_SOLR_XML, JettyConfig.builder().setContext("/solr").build(), zkServer);

    CloudSolrClient client = cluster.getSolrClient();
    ((ZkClientClusterStateProvider) client.getClusterStateProvider())
        .uploadConfig(solrDir.resolve("configsets").resolve("poc").resolve("conf"), "poc");

    return cluster;
  }

  private void startSentryService() throws Exception {
    log.info("Starting server");
    sentryService.start();
    final long start = System.nanoTime();
    while (!sentryService.isRunning()) {
      Thread.sleep(1000);
      if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) > 60) {
        throw new TimeoutException("Server did not start after 60 seconds");
      }
    }
  }

  @Override
  public void close() throws IOException {
    if (this.sentryService != null) {
      try {
        this.sentryService.stop();
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }

  private Configuration getServerConfig() {
    Configuration conf = new Configuration(true);
    conf.set(ServerConfig.SECURITY_MODE, ServerConfig.SECURITY_MODE_NONE);
    conf.set(ServerConfig.SENTRY_VERIFY_SCHEM_VERSION, "false");
    conf.set(ServerConfig.SENTRY_STORE_JDBC_URL, "jdbc:derby:;databaseName=" + dbDir + ";create=true");
    conf.set(ServerConfig.SENTRY_STORE_JDBC_PASS, "dummy");
    return conf;
  }

  public static void main(String[] args) throws Exception {
    SentryPocServer sentryServer = createSentryServer();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        try {
          Thread.sleep(200);
          sentryServer.close();
          MoreFiles.deleteRecursively(testDataPath, RecursiveDeleteOption.ALLOW_INSECURE);
        } catch (Exception e) {
          Thread.currentThread().interrupt();
          e.printStackTrace();
        }
      }
    });

    while (true) {
      Thread.sleep(1000);
    }
  }
}
