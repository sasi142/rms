package core.mediaconvert;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.DescribeEndpointsRequest;
import software.amazon.awssdk.services.mediaconvert.model.DescribeEndpointsResponse;

import java.net.URI;

@Configuration
public class MediaConvertConfig {

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

    @Value("${aws.mediaconvert.use-iam-role}")
    private Boolean mediaConvertUseIamRole;

    @Value("${aws.access.key}")
    private String apiKey;

    @Value("${aws.secret.key}")
    private String apiSecret;

    @Value("${aws.region}")
    private String region;

    public MediaConvertClient getMediaConvertClient() {
        final AwsCredentials basicAWSCredentials = AwsBasicCredentials.create(apiKey, apiSecret);
        MediaConvertClient mediaConvertClient = MediaConvertClient.builder().region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(basicAWSCredentials)).build();

        DescribeEndpointsResponse res =
                mediaConvertClient.describeEndpoints(DescribeEndpointsRequest.builder().maxResults(20).build());
        if (res.endpoints().size() <= 0) {
            System.out.println("Cannot find MediaConvert service endpoint URL!");
            System.exit(1);
        }
        String endpointURL = res.endpoints().get(0).url();
        System.out.println("MediaConvert service URL: " + endpointURL);
        return MediaConvertClient.builder().region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(basicAWSCredentials))
                .endpointOverride(URI.create(endpointURL)).build();
    }
}