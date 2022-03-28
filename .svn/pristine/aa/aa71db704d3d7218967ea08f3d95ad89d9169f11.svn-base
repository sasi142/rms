package utils;

import core.exceptions.InternalServerErrorException;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.HttpConnectionManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;

public abstract class RestApi {

    @Autowired
    private HttpConnectionManager httpConnectionManager;

    public void put(String url, String body)  {
        putAndPost(new HttpPut(url), body);
    }

    public void post(String url, String body)  {
        putAndPost(new HttpPost(url), body);
    }

    private void putAndPost(HttpEntityEnclosingRequestBase request, String body)  {
        CloseableHttpClient client = httpConnectionManager.getHttpClient();
        attachHeaders(request);
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        try {
            CloseableHttpResponse response = client.execute(request);
            if (!is2xx(response.getStatusLine().getStatusCode())){
                throw new InternalServerErrorException(Enums.ErrorCode.API_RESPONSE_NOT_OK, "Api response not OK - " + response.getStatusLine().getStatusCode());
            }
        }
        catch(IOException ioe){
            throw new InternalServerErrorException(Enums.ErrorCode.API_IO_ERROR, "IO Error - " + ioe.getMessage());
        }
        finally {
            request.releaseConnection();
        }
    }

    private boolean is2xx(int code){
        return code >= 200 && code < 300;
    }

    protected void attachHeaders(HttpEntityEnclosingRequestBase request) {
        String clientId = RmsApplicationContext.getInstance().getClientId();
        String requestId = UUID.randomUUID().toString();
        request.addHeader(Constants.CLIENT_ID, clientId);
        request.addHeader("Content-Type", "application/json");
        request.addHeader(Constants.X_REQUEST_ID, requestId);
    }
}
