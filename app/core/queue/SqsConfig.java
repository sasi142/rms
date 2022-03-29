package core.queue;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SQSConfig {

    @Value("${enable.http.proxy.support}")
    private Boolean enableHttpProxySupport;

    @Value("${http.proxy.server.ip.address}")
    private String proxyHost;

    @Value("${http.proxy.server.port}")
    private Integer proxyPort;

    @Value("${http.proxy.server.username}")
    private String proxyUserName;

    @Value("${http.proxy.server.password}")
    private String proxyPassword;

    @Value("${integration.sqs.use-iam-role}")
    private Boolean sqsUseIamRole;

    @Value("${aws.access.key}")
    private String apiKey;

    @Value("${aws.secret.key}")
    private String apiSecret;

    @Value("${aws.region}")
    private String awsRegion;

    public AmazonSQS amazonSQS() {
        AWSCredentialsProvider credentialsProvider = sqsUseIamRole ?
                InstanceProfileCredentialsProvider.getInstance() :
                new AWSStaticCredentialsProvider(new BasicAWSCredentials(apiKey, apiSecret));

        AmazonSQS sqs;
        if (enableHttpProxySupport) {
            ClientConfiguration clientConfig = new ClientConfiguration();
            clientConfig.setProxyHost(proxyHost);
            clientConfig.setProxyPort(proxyPort);
            clientConfig.setProxyUsername(proxyUserName);
            clientConfig.setProxyPassword(proxyPassword);
            sqs = AmazonSQSClientBuilder.standard().withCredentials(credentialsProvider).withClientConfiguration(clientConfig)
                    .withRegion(Regions.fromName(awsRegion)).build();
        } else {
            sqs = AmazonSQSClientBuilder.standard().withCredentials(credentialsProvider).withRegion(Regions.fromName(awsRegion)).build();
        }
        return sqs;
    }
}