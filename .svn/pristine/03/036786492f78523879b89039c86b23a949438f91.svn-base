package core.daos.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.daos.VideokycAgentQueueDao;
import core.entities.VideokycAgentQueue;
import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceNotFoundException;
import core.utils.Constants;
import core.utils.Enums.ErrorCode;
import core.utils.HttpConnectionManager;
import core.utils.PropertyUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import play.libs.Json;
import utils.RmsApplicationContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

@Repository
public class VideokycAgentQueueDaoImpl extends AbstractJpaDAO<VideokycAgentQueue> implements VideokycAgentQueueDao {

    final static Logger logger = LoggerFactory.getLogger(VideokycAgentQueueDaoImpl.class);

    @Autowired
    private HttpConnectionManager httpConnectionManager;

    @PersistenceContext(unitName = "readEntityManagerFactory")
    protected EntityManager readEntityManager;

    @Autowired
    private Environment env;

    public VideokycAgentQueueDaoImpl() {
        super();
        setClazz(VideokycAgentQueue.class);
    }


    @Override
    public VideokycAgentQueue getByGroupAndAgentId(Integer userId, Long groupId) {
        logger.info("get agent by group id: " + userId + ", groupId: " + groupId);
        try {
            TypedQuery<VideokycAgentQueue> query = entityManager.createNamedQuery("VideokycAgentQueue.getByGroupAndAgentId", VideokycAgentQueue.class);
            query.setParameter("agentId", userId);
            query.setParameter("groupId", groupId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

//    @Override
//    public List<VideokycAgentQueue> findByAgentId(Integer agentId) {
//        logger.info("get by agent id: " + agentId);
//        TypedQuery<VideokycAgentQueue> query = entityManager.createNamedQuery("VideokycAgentQueue.GetByAgentId", VideokycAgentQueue.class);
//        query.setParameter("agentId", agentId);
//        List<VideokycAgentQueue> agents = query.getResultList();
//        logger.info("agent is found size: {}", agents.size());
//        return agents;
//    }
//

//    @Override
//    public void updateAgentQueue(Integer agentId, Byte agentStatus) {
//        logger.info("update queue where agentId: " + agentId);
//        Query query = entityManager.createNamedQuery("VideokycAgentQueue.UpdateQueueByGroupId");
//        query.setParameter("agentStatus", agentStatus);
//        query.setParameter("agentId", agentId);
//        query.setParameter("updatedDate", System.currentTimeMillis());
//        //query.setMaxResults(1);
//        query.executeUpdate();
//        logger.info("update is done ");
//    }


    @Override
    public void updateAgentQueueStatus(Integer agentId, Byte agentStatus) {
        logger.info("update queue where agentId: " + agentId + " & status: " + agentStatus);
        StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("VideokycAgentQueue.UpdateAgentQueueStatus");
        spQuery.setParameter("P_AgentId", agentId); // 0
        spQuery.setParameter("P_AgentStatus", agentStatus); // in sec.
        spQuery.executeUpdate();
        logger.info("update is done ");
    }


//    @Override
//    public VideokycAgentQueue findByGroupId(Long groupId, Byte agentStatus) {
//        logger.info("get by group id: " + groupId + ", agentStatus: " + agentStatus);
//        TypedQuery<VideokycAgentQueue> query = entityManager.createNamedQuery("VideokycAgentQueue.GetByGroupId", VideokycAgentQueue.class);
//        query.setParameter("groupId", groupId);
//        query.setParameter("agentStatus", agentStatus);
//        List<VideokycAgentQueue> list = query.getResultList();
//        return list.stream().findFirst().orElse(null);
//    }
//
//    @Override
//    public void markAllAgentInactive() {
//        logger.info("mark all agent inactive");
//        Query query = entityManager.createNamedQuery("VideokycAgentQueue.makeAllAgentNotAvailable");
//        query.setParameter("updatedDate", System.currentTimeMillis());
//        query.executeUpdate();
//        logger.info("agents are marked inactive");
//    }

    @Override
    public void changeVideoKycStatus(Integer videoKycId, String status, Integer userId, Integer orgId) {
        logger.info("change video kyc status in IMS : videoKycId: " + videoKycId + ", status: " + status + ",userId: " + userId + ", orgId: " + orgId);
        CloseableHttpClient client = httpConnectionManager.getHttpClient();
        String imsVideoKycChangeStatusApi = env.getProperty(Constants.IMS_VIDEOKYC_SYSTEM_CHANGE_STATUS);
        String url = MessageFormat.format(imsVideoKycChangeStatusApi, String.valueOf(videoKycId));
        logger.info("change kyc url : " + url);
        HttpPut httpPut = new HttpPut(url);
        String clientId = RmsApplicationContext.getInstance().getClientId();
        httpPut.addHeader(Constants.CLIENT_ID, clientId);
        httpPut.addHeader("Content-Type", "application/json");
        String requestId = UUID.randomUUID().toString();
        httpPut.addHeader(Constants.X_REQUEST_ID, requestId);
        Long timestamp = System.currentTimeMillis();
        httpPut.addHeader(Constants.TIMESTAMP_STR, String.valueOf(timestamp));
        String imsApiKey = PropertyUtil.getProperty(Constants.IMS_API_KEY);
        httpPut.addHeader(Constants.API_KEY, imsApiKey);

        String apiTimePair = imsApiKey + ":" + timestamp;
        logger.info("created apiTimePair as  " + apiTimePair);

        Mac imsSHA256_HMAC = getImsShaHmacInstance();
        String signature = Base64.encodeBase64String(imsSHA256_HMAC.doFinal(apiTimePair.getBytes()));
        httpPut.addHeader(Constants.API_SIGNATURE, signature);

        CloseableHttpResponse response = null;
        try {
            ObjectNode node = Json.newObject();
            node.put("status", status);
            node.put("orgId", orgId);
            node.put("userId", userId);
            String json = node.toString();
            logger.info("json: " + json);
            httpPut.setEntity(new StringEntity(json, Consts.UTF_8));
            logger.info("executing the video kyc change status ");
            response = client.execute(httpPut);
            if (response != null) {
                logger.info("response.getStatusLine().getStatusCode() : " + response.getStatusLine().getStatusCode());
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("sending the events failed as response code is : "
                            + response.getStatusLine().getStatusCode());
                } else {
                    logger.info("send Event is successful. Resposne code is 200.");
                }
                if (logger.isInfoEnabled()) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    StringBuffer result = new StringBuffer();
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }
                    logger.info(result.toString());
                }
                response.close();
            }
        } catch (Exception e) {
            logger.info("Failed to send event, exception is : ", e);
            throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "Error while Sending " + status + " Event to IMS");
        } finally {
            httpPut.releaseConnection();
        }
    }

    //  @SuppressWarnings("unchecked")
    @Override
    public void refreshVideoKycCache(List<Integer> videoKycIds, List<Long> groupIds) {
        logger.info("refresh videoKyc cache started for videoKycIds: " + videoKycIds + " & guestGroupIds: " + groupIds);
        CloseableHttpClient client = httpConnectionManager.getHttpClient();
        String url = env.getProperty(Constants.IMS_REFRESH_VIDEOKYC_CACHE);
        logger.info("refresh videokyc url : " + url);
        HttpPut httpPut = new HttpPut(url);
        String clientId = RmsApplicationContext.getInstance().getClientId();
        httpPut.addHeader(Constants.CLIENT_ID, clientId);
        httpPut.addHeader("Content-Type", "application/json");

        String requestId = UUID.randomUUID().toString();
        httpPut.addHeader(Constants.X_REQUEST_ID, requestId);
        Long timestamp = System.currentTimeMillis();
        httpPut.addHeader(Constants.TIMESTAMP_STR, String.valueOf(timestamp));
        String imsApiKey = PropertyUtil.getProperty(Constants.IMS_API_KEY);
        httpPut.addHeader(Constants.API_KEY, imsApiKey);

        String apiTimePair = imsApiKey + ":" + timestamp;
        logger.info("created apiTimePair as  " + apiTimePair);

        Mac imsSHA256_HMAC = getImsShaHmacInstance();
        String signature = Base64.encodeBase64String(imsSHA256_HMAC.doFinal(apiTimePair.getBytes()));
        httpPut.addHeader(Constants.API_SIGNATURE, signature);

        CloseableHttpResponse response = null;
        try {
            JSONObject node = new JSONObject();
            JSONArray kycIdList = new JSONArray(videoKycIds);
            node.put("videoKycIds", kycIdList);
            node.put("guestGroupIds", new JSONArray(groupIds));
            String json = node.toString();
            logger.info("json: " + json);
            httpPut.setEntity(new StringEntity(json, Consts.UTF_8));
            logger.info("executing the video kyc change status ");
            response = client.execute(httpPut);
            if (response != null) {
                logger.info("response.getStatusLine().getStatusCode() : " + response.getStatusLine().getStatusCode());
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("sending the Refresh VideKyc cache event failed as response code is : "
                            + response.getStatusLine().getStatusCode());
                } else {
                    logger.info("send Refresh VideKyc cache Event is successful. Resposne code is 200.");
                }

            }
        } catch (Exception e) {
            logger.info("Failed to send event, exception is : ", e);
            throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "Error while Sending Event to IMS");
        } finally {
            httpPut.releaseConnection();
        }
    }


    @Override
    public Integer GetVideoKycGroupCallWait(Integer groupId, Byte priority, Short breathingTime, Integer avgCallDuration) {
        logger.info("get call wait time from db:  groupId: " + groupId + ", priority: " + priority
                + ", breathingTime: " + breathingTime + ", avgCallDuration: " + avgCallDuration);
        Integer callWait = null;
        try {
            StoredProcedureQuery spQuery = readEntityManager.createNamedStoredProcedureQuery("VideokycAgentQueue.GetVideoKycGroupCallWait");
            spQuery.setParameter("P_GroupId", groupId);
            spQuery.setParameter("P_Priority", priority); // 1 ,2
            spQuery.setParameter("P_BreathingTime", breathingTime); // 0
            spQuery.setParameter("P_AvgCallDuration", avgCallDuration); // in sec.
            callWait = (Integer) spQuery.getOutputParameterValue("O_CallWaitDuration");
            logger.info("call wait time: " + callWait);
        } catch (Exception ex) {
            throw new ResourceNotFoundException(ErrorCode.InvalidGroup, "Group with " + groupId + " not exists in agent queue", ex);
        }
        return callWait;
    }

    @Override
    public List<VideokycAgentQueue> getCallWaitTime(Short breathingTime, Integer avgCallDuration, Boolean syncStatus) {
        logger.info("get call wait time breathingTime: " + breathingTime + ", avgCallDuration: " + avgCallDuration + " started" + " syncStatus: " + syncStatus);
        List<VideokycAgentQueue> agentGuestMapping = null;
        try {
            StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("VideokycAgentQueue.GetCallWaitTime");
            spQuery.setParameter("P_BreathingTime", breathingTime); // 0
            spQuery.setParameter("P_AvgCallDuration", avgCallDuration);    // in sec.
            spQuery.setParameter("P_SyncStatus", syncStatus); //boolean
            agentGuestMapping = spQuery.getResultList();
            logger.info("GetCallWaitTime: " + agentGuestMapping.size());
        } catch (Exception ex) {
            throw new ResourceNotFoundException(ErrorCode.InvalidGroup, "Group with not exists in agent queue", ex);
        }
        return agentGuestMapping;
    }

    private Mac getImsShaHmacInstance() {
        try {
            String secret = (PropertyUtil.getProperty(Constants.IMS_API_SECRET)).trim();
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKey);
            return sha256_HMAC;
        } catch (Exception ex) {
            logger.error("failed to get hmac instance for ims ", ex);
            throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
                    "failed to get hmac instance for ims", ex);
        }
    }

}
