package core.daos.impl;

import core.daos.ElasticSearchDao;
import core.utils.Constants;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;


@Repository
@Qualifier("elasticSearchRestDaoImpl")
public class ElasticSearchRestDaoImpl implements ElasticSearchDao, InitializingBean, DisposableBean {

	final Logger logger = LoggerFactory.getLogger(ElasticSearchRestDaoImpl.class);

	@Autowired
	private Environment			env;

	private RestClient			client;

	private Boolean esEnabled = false;


	private BlockingQueue<ImmutablePair<String, String>> messages = new LinkedBlockingDeque<>();

	@Value("${es.write-job.batch-size:100}")
	private Integer batchSize;

	@Autowired
	@Qualifier("esFlushTaskExecutor")
	private TaskExecutor taskExecutor;


	@Override
	@Scheduled(fixedDelayString = "${es.write-job.delay:1000}")
	public void flush(){
		List<ImmutablePair<String, String>> current = new LinkedList<>();
		int messageCount = messages.drainTo(current);
		if (messageCount == 0) return;

		logger.debug("Will make batches out of {} of batch size {}", messageCount, batchSize);
		for (int i = 0; i < messageCount; i=i+batchSize) {
			List<ImmutablePair<String, String>> batchList;
			if (i+batchSize>messageCount){
				//we have less elements than the batch
				logger.debug("Making short batch from {} to {}", i, messageCount);
				batchList = current.subList(i, messageCount);
			}
			else{
				//we have more elements available, take equal to batch
				logger.debug("Making full batch from {} to {}", i, i+batchSize);
				batchList = current.subList(i, i + batchSize);
			}
			taskExecutor.execute(new Runnable() {
				@Override
				public void run() {
					flush(batchList);
				}
			});
		}
	}

	@Override
	public void flush(List<ImmutablePair<String, String>> messages){
		logger.debug("Writing total messages {}", messages.size());
		StringJoiner joiner = new StringJoiner(String.valueOf('\n'), "", String.valueOf('\n'));

		for (ImmutablePair<String, String> message : messages) {
			joiner.add("{\"index\":{\"_index\":\""  + message.left +   "\",\"_id\":\""  + UUID.randomUUID().toString() +   "\",\"_type\":\"_doc\"}}");
			joiner.add(message.right);
		}
		String content = joiner.toString();
		logger.debug("Message content to write {}", content);

		try {
			Request request = new Request("POST", "/_bulk");
			ContentType contentType = ContentType.create("application/x-ndjson");
			HttpEntity entity = new NStringEntity(content, contentType);
			request.setEntity(entity);
			Response response = client.performRequest(request);
			logger.debug("response : " + EntityUtils.toString(response.getEntity()));
		}
		catch (Exception ex) {
			logger.error("Failed to save to ES. Will push back to queue for writing.", ex);
			//write back to message queue for retry in next run
			this.messages.addAll(messages);
		}
	}

	@Override
	public void add(String indexName, String json) {
		if (esEnabled) {
			logger.info("Add to ES Index {} with data {} ", indexName, json);
			messages.add(new ImmutablePair<>(indexName, json));
		}
	}


	/**
	 * Note:
	 * WARNING- Specifying types in document index requests is deprecated, use the typeless endpoints instead
	 * We are still using below 7 version of ES, so continue using type. We will remove the type afterword
	 */
	//@Override
	public void add2(String indexName, String json) {
		if (esEnabled) {
			try {
				logger.debug("enter data in elastic search: "+json);
				String endpoint = "/" + indexName + "/_doc/"+UUID.randomUUID().toString(); 
				Request request = new Request("PUT", endpoint); 
				logger.debug("add new document for id: "); 
				HttpEntity entity = new NStringEntity(json, ContentType.APPLICATION_JSON); request.setEntity(entity);
				Response response = client.performRequest(request);
				logger.debug("response : " + EntityUtils.toString(response.getEntity())); 
			}
			catch (Exception ex) { 
				logger.error("failed to save to ES", ex);
			}
		}
	}

	@Override
	public String get(String indexName, Integer id) {
		String responseString1= null;
		if (esEnabled) {
			try {
				logger.debug("get data for id: "+id);
				String endpoint = "/" + indexName+"/_search";
				logger.info("endpoint: "+endpoint);
				Request request = new Request("GET", endpoint);

				File file = ResourceUtils.getFile(env.getProperty(Constants.ELASTIC_SEARCH_GET_REQUEST_TEMPLATE_PATH)); 
				String requestBody = new String(Files.readAllBytes(file.toPath())); 
				requestBody = requestBody.replace("$meetingId", id.toString());

				HttpEntity entity = new NStringEntity(requestBody, ContentType.APPLICATION_JSON); request.setEntity(entity);

				Response response = client.performRequest(request);
				responseString1 = EntityUtils.toString(response.getEntity());
				logger.info("response from ES:"+responseString1);
			} 
			catch (Exception ex) { 
				logger.error("failed to get data from ES", ex);
			}
		}
		return responseString1; 
	}

	@Override
	public void afterPropertiesSet() throws Exception {		
		esEnabled = Boolean.valueOf(env.getProperty(Constants.ES_ENABLED, "false"));		
		logger.info("esEnabled: " + esEnabled);
		logger.info("es batch size: " + batchSize);
		if (esEnabled) {
			String url = env.getProperty(Constants.ELASTIC_SEARCH_URL);
			logger.info("elastic search url: " + url);

			int port = Integer.valueOf(env.getProperty(Constants.ELASTIC_SEARCH_PORT));
			logger.info("elastic search port: " + port);

			String protocol = env.getProperty(Constants.ELASTIC_SEARCH_PROTOCOL);
			logger.info("protocol used for elastic search communication:"+protocol);

			String username = env.getProperty(Constants.ELASTIC_SEARCH_USERNAME);
			logger.info("username: "+username);

			String password = env.getProperty(Constants.ELASTIC_SEARCH_PASSWORD);

			String elasticSearchAuthRequired =
					env.getProperty(Constants.ELASTIC_SEARCH_AUTH_REQUIRED);
			logger.info("elasticSearchAuthRequired: "+elasticSearchAuthRequired);

			int elasticSearchMaxConnection =
					Integer.getInteger(env.getProperty(Constants.ELASTIC_SEARCH_POOL_MAX_CONNECTION, "0"), 0);
			logger.info("elasticSearchMaxConnection: "+elasticSearchMaxConnection);

			int elasticSearchThreadCount =
					Integer.getInteger(env.getProperty(Constants.ELASTIC_SEARCH_THREAD_COUNT, "0"), 0);
			logger.info("elasticSearchThreadCount: "+ elasticSearchThreadCount);

			if (elasticSearchAuthRequired != null && "true".equalsIgnoreCase(elasticSearchAuthRequired)) {
				logger.info("auth is created so creating elastic search client with auth");
				final CredentialsProvider credentialsProvider = new
						BasicCredentialsProvider(); credentialsProvider.setCredentials(AuthScope.ANY,
								new UsernamePasswordCredentials(username, password)); RestClientBuilder
								builder = RestClient.builder( new HttpHost(url, port, protocol))
								.setHttpClientConfigCallback(new HttpClientConfigCallback() {

									@Override public HttpAsyncClientBuilder
									customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
										logger.debug("setting credential provider");
										httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
										if (elasticSearchMaxConnection != 0){
											httpClientBuilder.setMaxConnTotal(elasticSearchMaxConnection);
											httpClientBuilder.setMaxConnPerRoute(elasticSearchMaxConnection);
										}
										if (elasticSearchThreadCount != 0){
											httpClientBuilder.setDefaultIOReactorConfig(
													IOReactorConfig.custom()
															.setIoThreadCount(elasticSearchThreadCount)
															.build());
										}
										return httpClientBuilder;
									}
								});

								client = builder.build(); 
			} else { 
				logger.	info("auth is not required so creating elastic search client without auth ");
				client = RestClient.builder(new HttpHost(url, port, protocol)).setHttpClientConfigCallback(new HttpClientConfigCallback() {

					@Override public HttpAsyncClientBuilder
					customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
						if (elasticSearchMaxConnection != 0){
							httpClientBuilder.setMaxConnTotal(elasticSearchMaxConnection);
							httpClientBuilder.setMaxConnPerRoute(elasticSearchMaxConnection);
						}
						if (elasticSearchThreadCount != 0){
							httpClientBuilder.setDefaultIOReactorConfig(
									IOReactorConfig.custom()
											.setIoThreadCount(elasticSearchThreadCount)
											.build());
						}
						return httpClientBuilder;
					}
				}).build();
			}
		}
	}

	@Override
	public void destroy() throws Exception {
		client.close();		
	}
}