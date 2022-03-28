package utils;

import core.utils.Constants;
import core.utils.PropertyUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
public class ImsApi extends RestApi implements InitializingBean {

    @Autowired
    private PropertyUtil propertyUtil;

    private Mac IMS_SHA256_MAC;
    private String PROP_IMS_API_KEY;

    @Override
    protected void attachHeaders(HttpEntityEnclosingRequestBase request) {
        super.attachHeaders(request);
        Long timestamp = System.currentTimeMillis();
        request.addHeader(Constants.TIMESTAMP_STR, String.valueOf(timestamp));
        request.addHeader(Constants.API_KEY, PROP_IMS_API_KEY);
        request.addHeader(Constants.API_SIGNATURE, apiSignature(timestamp));
    }

    private String apiSignature(Long timestamp) {
        String apiTimePair = PROP_IMS_API_KEY + ":" + timestamp;
        return Base64.encodeBase64String(IMS_SHA256_MAC.doFinal(apiTimePair.getBytes()));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.PROP_IMS_API_KEY = propertyUtil.getProperty(Constants.IMS_API_KEY);
        String secret = propertyUtil.getProperty(Constants.IMS_API_SECRET).trim();
        this.IMS_SHA256_MAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        this.IMS_SHA256_MAC.init(secretKey);
    }
}
