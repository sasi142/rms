package core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import core.daos.CacheGroupDao;
import core.daos.RecordingDao;
import core.daos.VideokycAgentQueueDao;
import core.entities.*;
import core.entities.projections.VideoKyc;
import core.exceptions.BadRequestException;
import core.exceptions.InternalServerErrorException;
import core.utils.*;
import core.utils.Enums.RecordingProcessingStatus;
import core.utils.Enums.RecordingStage;
import core.utils.Enums.RecordingStopEventTriggerBy;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import play.libs.Json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(rollbackFor = {Exception.class})
public class RecordingServiceImpl implements RecordingService, InitializingBean {
    final static Logger logger = LoggerFactory.getLogger(RecordingServiceImpl.class);

    @Autowired
    private RecordingDao recordingDao;

    @Autowired
    @Qualifier("RmsCacheService")
    private CacheService cacheService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private CacheGroupDao cacheGroupDao;

    @Autowired
    private VideokycAgentQueueDao videokycAgentQueueDao;

    @Autowired
    private VideokycService videokycService;

    @Autowired
    private OrgUtil orgUtil;

    @Autowired
    private JsonUtil jsonUtil;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private UserConnectionService userConnectionService;

    @Autowired
    private Environment env;

    private final ObjectMapper mapper = new ObjectMapper();

    private int maxReprocessingCount = 10;
    private int kycCreatedLastInDays = 2;
    private int kycCreatedLastInMillis = 2 * 24 * 60 * 60 * 1000;

    @Override
    public Recording createVideoRecording(Recording recording) {
        recordingDao.create(recording);
        return recording;
    }
    
    
    @Override
    public Recording getRecordingByMeetingId(Integer meetingId) {        
        return recordingDao.getExistingRecording(meetingId);
    }

    @Override
    public void updateRecordingStage(Integer recordingId, Byte recordingStage) {
        recordingDao.updateRecordingStage(recordingId, recordingStage, System.currentTimeMillis());
    }

    private void updateRecordingOnFailedEvent(Integer recordingId, String failureReason) {
        logger.info("Update recording failed for Id {} with failure reason {}",
                recordingId, failureReason);
        Recording recording = recordingDao.findOne(recordingId);
        recording.setStopEventTriggerBy(RecordingStopEventTriggerBy.System.getId().byteValue());
        recording.setRecordingStage(RecordingStage.Failed.getId());
        recording.setUpdatedDate(System.currentTimeMillis());
        recording.setFailureReason(failureReason);
        recordingDao.update(recording);
    }

    private void updateRecordingForReprocessing(Integer recordingId, boolean kycRecording, String transcodingReason) {
        logger.info("Update recording for reprocessing for Id {} with kycRecording {}, transcoding reason {}",
                recordingId, kycRecording, transcodingReason);
        Recording recording = recordingDao.findOne(recordingId);
        recording.setStopEventTriggerBy(RecordingStopEventTriggerBy.System.getId().byteValue());
        recording.setRecordingStage(kycRecording ? Enums.RecordingStage.ReprocessKyc.getId() : RecordingStage.Stopped.getId());
        recording.setUpdatedDate(System.currentTimeMillis());
        recording.setForcedTranscoding(true);
        recording.setEnableTranscoding(true);
        recording.setTranscodingReason(transcodingReason);
        recording.setFailureReason(null);
        recordingDao.update(recording);
    }

    private void updateRecordingOnProcessedEvent(Integer recordingId, Long chatId, Integer attachmentId,
                                                 Integer senderAttachmentId, Byte recordingStage, Long endDate, BigDecimal duration) {
        logger.info("update recording for recording Id {} with chat Id {}, attachment Id {}, senderAttachmentId {}, "
                        + "recordingStage {}, endDate {}, duration {}",
                recordingId, chatId, attachmentId, senderAttachmentId, recordingStage, endDate, duration);
        Recording recording = recordingDao.findOne(recordingId);
        if (recording.getRecordingStage().intValue() == RecordingStage.Stopped.getId().intValue()) {
            recording.setStopEventTriggerBy(RecordingStopEventTriggerBy.User.getId().byteValue());
        } else {
            recording.setStopEventTriggerBy(RecordingStopEventTriggerBy.System.getId().byteValue());
        }
        recording.setChatId(chatId);
        recording.setAttachmentId(attachmentId);
        recording.setRecordingStage(recordingStage);
        recording.setUpdatedDate(System.currentTimeMillis());
        recording.setSenderAttachmentId(senderAttachmentId);
        recording.setDuration(duration);
        recording.setFailureReason(null);
        //recording.setForcedTranscoding(forcedTranscoding);
        recordingDao.update(recording);
        logger.info("updated recording done");
    }

    @Override
    public List<Recording> getRecordingsByGroupId(Integer groupId) {
        logger.info("get recording list started fro groupId: " + groupId);
        List<Recording> recordingList = recordingDao.getRecordingsByGroupId(groupId);
        logger.info("recording list size : " + recordingList.size());
        return recordingList;
    }


    @Override
    public void handleProcessedVideoRecordingEvent(Event event) throws IOException {
        final Map<String, String> eventData = event.getData();
        logger.debug("Event data {}", eventData);
        Integer to = Integer.valueOf(eventData.get("to"));
        Integer from = Integer.valueOf(eventData.get("from"));
        int recordingId = Integer.parseInt(eventData.get("recordingId"));
        int chatType = Integer.parseInt(eventData.get("chatType"));

        BigDecimal duration = null;
        String dur = eventData.get("duration");
        if (dur != null && !"".equalsIgnoreCase(dur)) {
            duration = new BigDecimal(dur);
            logger.debug("recording duration: " + duration);
        }

        logger.info("Process Video Recording event for recording Id: " + recordingId);

        User sender = cacheService.getUser(from, false);
        ChatMessage message = createRecordingChatMessage(sender, to);

        Integer attachmentId = null;
        Integer senderAttachmentId = null;
        Integer mergedAttachmentId = null;
        Map<Integer, JsonNode> statusMap = Collections.emptyMap();
        Map<Integer, Boolean> successMap = Collections.emptyMap();
//		try {
        ObjectNode dataNode = Json.newObject();
        ArrayNode attsArray = dataNode.putArray("attachments");
//        JsonNode attNode = mapper.readValue(eventData.get("attachment"), JsonNode.class);
//        if (attNode != null && attNode.at("/id") != null) {
//            attachmentId = attNode.at("/id").asInt();
//            logger.info("attachmentId = " + attachmentId);
//        } else {
//            logger.info("attachmentId is null");
//        }
//
//        attsArray.add(attNode);
//
//        String senderFile = eventData.get("senderFile");
//        if (StringUtils.isNotBlank(senderFile)) {
//            JsonNode senderNode = mapper.readValue(senderFile, JsonNode.class);
//            if (senderNode != null && senderNode.at("/id") != null) {
//                senderAttachmentId = senderNode.at("/id").asInt();
//                logger.info("sender attachment id = " + senderNode);
//            } else {
//                logger.info("attachmentId is null");
//            }
//        }
//
//
//
//

        //ONLY main attachment and mergedAttachment is added to message data
        //ONLY main attachment and senderAttachment ID is saved in the recording table.
        if (eventData.containsKey("attachment")) {
            String attachment = eventData.get("attachment");
            logger.debug("attachment : {}", attachment);
            JsonNode attachmentJson = mapper.readValue(attachment, JsonNode.class);
            attsArray.add(attachmentJson);
            attachmentId = attachmentJson.get("id").asInt();
        }

        if (eventData.containsKey("senderFile")) {
            String senderAttachment = eventData.get("senderFile");
            logger.debug("senderAttachment : {}", senderAttachment);
            JsonNode senderAttachmentJson = mapper.readValue(senderAttachment, JsonNode.class);
            senderAttachmentId = senderAttachmentJson.get("id").asInt();
        }

        if (eventData.containsKey("mergedAttachment")) {
            String mergedAttachment = eventData.get("mergedAttachment");
            logger.debug("mergedAttachment : {}", mergedAttachment);
            JsonNode mergedAttachmentJson = mapper.readValue(mergedAttachment, JsonNode.class);
            attsArray.add(mergedAttachmentJson);
            mergedAttachmentId = mergedAttachmentJson.get("id").asInt();
        }

        logger.info("Attachment : {}, Sender Attachment : {}, Merged : {}", attachmentId, senderAttachmentId, mergedAttachmentId);

        String statusMapString = eventData.get("statusMap");
        if (StringUtils.isNotEmpty(statusMapString)) {
            statusMap = mapper.readValue(statusMapString, TypeFactory.defaultInstance()
                    .constructMapType(HashMap.class, Integer.class, JsonNode.class));
        }

        String successMapString = eventData.get("successMap");
        if (StringUtils.isNotEmpty(successMapString)) {
            successMap = mapper.readValue(successMapString, TypeFactory.defaultInstance()
                    .constructMapType(HashMap.class, Integer.class, Boolean.class));
        }

        message.setData(dataNode);

        long utcDate = message.getUtcDate();
        String date = CommonUtil.getDateTimeWithTimeZone(utcDate, sender.getTimezone());
        message.setDate(date);

        boolean forcedTranscoding = Boolean.parseBoolean(eventData.getOrDefault("forcedTranscoding", "false"));

        if (Enums.ChatType.GroupChat.getId().equals((byte) chatType)) {
            //KYC flow
            Group group = cacheGroupDao.getGroup(to);
            // For Guest chats the guest must see the video recordings messages.
            if (group.getGuestUserId() != null) {
                message.setExcludedRecipients(group.getGuestUserId().toString());
            }

            message.setSubtype(Enums.ChatType.GroupChat.getId().intValue());
            GroupChat chat = chatHistoryService.createGroupChatHistory(message, group);
            message.setMid(chat.getId());

            // If video kyc then change video KYC status
            logger.info("check if recording belongs to video kyc");
            Integer guestId = group.getGuestUserId();
            JsonNode kycJson = cacheService.getVideoKycInfo(guestId);
            if (kycJson != null) {
                //kyc recording
                updateVideoKYCStatus(from, kycJson);
            }

            updateRecordingStatus(successMap, statusMap, attachmentId, senderAttachmentId, duration, utcDate, chat.getId());

            List<String> clientIds = new ArrayList<>(commonUtil.getGroupChtSupportedClients());
            Set<Integer> memberIds = cacheGroupDao.getGroupMembersSet(to);
            logger.debug("Received group members {}", memberIds.size());
            // send update to all group members
            memberIds.remove(group.getGuestUserId());

            JsonNode messageNode = Json.toJson(message);
            List<Integer> pushList = userConnectionService.sendMessageToActorSet(memberIds, messageNode, clientIds);
            if (!pushList.isEmpty()) {
                notificationService.sendMobileNotification(Enums.NotificationType.GroupChat.getId(), message,
                        pushList, Enums.PushNotificationVisibility.All);
                logger.debug("called sent mobile push notification for members = " + pushList);
            }

        } else {
            message.setSubtype(Enums.ChatType.One2One.getId().intValue());
            One2OneChat chat = chatHistoryService.createOne2OneChatHistory(message);
            message.setMid(chat.getId());

            updateRecordingStatus(successMap, statusMap, attachmentId, senderAttachmentId, duration, utcDate, chat.getId());

            List<String> clientIds = new ArrayList<>(commonUtil.getOne2oneChtSupportedClients());
            logger.debug("clientIds: " + clientIds);

            sendVideoRecordingUploadEvent(to, from, message, utcDate, chat, clientIds);
            sendVideoRecordingUploadEvent(from, from, message, utcDate, chat, clientIds);
        }
    }

    private void sendVideoRecordingUploadEvent(Integer to, Integer from, ChatMessage message, long utcDate,
                                               One2OneChat chat, List<String> clientIds) {
        Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(to, Json.toJson(message), null, clientIds);
        logger.debug("send One2One Message To Actor returned clients who received messages as " + msgReceiverClients);

        Boolean isSendPN = commonUtil.isAlwaysSendPushNofication(message, msgReceiverClients);
        logger.debug("isSendPN flag = " + isSendPN);
        if (isSendPN) {
            List<Integer> recipients = new ArrayList<>();
            recipients.add(message.getTo());
            logger.warn("contact: " + message.getTo() + "  is offline");

            notificationService.sendMobileNotification(Enums.NotificationType.Chat.getId(), to, from, null,
                    message.getText(), null, utcDate, chat.getId(), recipients, null, chat.getChatMessageType(),
                    Enums.PushNotificationVisibility.All);
            logger.debug("called sent mobile push notification for users = " + recipients);
        }
    }

    private void updateVideoKYCStatus(Integer from, JsonNode kycJson) {
        int kycId = kycJson.findPath("id").asInt();
        logger.info("Try to update kyc status of {}", kycId);

        byte kycStatus = (byte) kycJson.findPath("status").asInt();
        logger.info("Kyc status of kyc {} is {}", kycId, kycStatus);

        JsonNode userJson = cacheService.getUserJson(from);
        Integer orgId = userJson.findPath("orgId").asInt();

        boolean canMakeAuditorReady = Enums.VideoKYCStatus.Successful.getId().equals(kycStatus)
                || (
                Enums.VideoKYCStatus.Rejected.getId().equals(kycStatus) &&
                        orgUtil.getPreferenceAsBoolean(orgId, OrgUtil.AssignRejectedKycToAuditor)
        );

        if (canMakeAuditorReady) {
            logger.info("AuditorReady change status for org {}, KYC {}", orgId, kycId);
            videokycAgentQueueDao.changeVideoKycStatus(kycId, Enums.VideoKYCStatus.AuditorReady.getName(), null, orgId);
        }
    }

    @Override
    public void handleFailedVideoRecordingEvent(Event event) throws IOException {
        logger.info("handling failed video recording event started.");
        Map<String, String> dataMap = event.getData();
        logger.debug("event data " + dataMap);
        if (CollectionUtils.isEmpty(dataMap)) {
            logger.info("failed video recording event data map was empty.");
            return;
        }

        String errorCode = dataMap.get("errorCode");
        String message = dataMap.get("errorMessage");
        logger.info("recording processing failed with errorCode : " + errorCode);

        Map<Integer, JsonNode> statusMap = Collections.emptyMap();
        String statusMapString = event.getData().get("statusMap");
        if (StringUtils.isNotEmpty(statusMapString)) {
            statusMap = mapper.readValue(statusMapString, TypeFactory.defaultInstance()
                    .constructMapType(HashMap.class, Integer.class, JsonNode.class));
        }

        Map<Integer, Boolean> successMap = Collections.emptyMap();
        String successMapString = event.getData().get("successMap");
        if (StringUtils.isNotEmpty(successMapString)) {
            successMap = mapper.readValue(successMapString, TypeFactory.defaultInstance()
                    .constructMapType(HashMap.class, Integer.class, Boolean.class));
        }

        boolean forcedTranscoding = Boolean.parseBoolean(dataMap.getOrDefault("forcedTranscoding", "false"));
        String recordingIds = dataMap.get("recordingList");
        String transcodingReason = forcedTranscoding ? dataMap.get("transcodingReason") : null;
        boolean kycRecording = Boolean.parseBoolean(dataMap.get("kycRecording"));
        if (!StringUtils.isEmpty(recordingIds)) {
            String[] ids = StringUtils.split(recordingIds, ",");
            for (String id : ids) {
                Integer recId = Integer.parseInt(id);
                logger.info("Recording failed status found {} for recording Id {}", statusMap.get(recId), recId);
                ObjectNode node = mapper.createObjectNode();
                node.put("error", errorCode);
                node.put("errorText", message);
                node.set("details", statusMap.get(recId));
                if (forcedTranscoding){
                    updateRecordingForReprocessing(recId, kycRecording, transcodingReason);
                }
                else{
                    updateRecordingOnFailedEvent(recId, String.valueOf(node));
                }
            }
        }
        Integer to = Integer.valueOf(event.getData().get("to"));
        Integer from = Integer.valueOf(event.getData().get("from"));
        Byte chatType = Byte.valueOf(event.getData().get("chatType"));
        if (Enums.ChatType.GroupChat.getId().equals(chatType)) {
            Group group = cacheGroupDao.getGroup(to);
            // If video kyc then change video KYC status
            JsonNode kycJson = cacheService.getVideoKycInfo(group.getGuestUserId());
            if (kycJson != null) {
                logger.info("Found the recording linked to KYC {}", kycJson.findPath("id").asInt());
                updateVideoKYCStatus(from, kycJson);
            }
        }
    }

    @Override
    public void markForReprocessing(final Long groupId, final Integer recordingId, final boolean transcoding) {
        final Optional<String> result = recordingDao.markForReprocessing(groupId, recordingId, transcoding, maxReprocessingCount);
        if (result.isPresent()) {
            throw new BadRequestException(Enums.ErrorCode.BadRequest, result.get());
        }
    }

    private boolean isUserAllowedToReprocess() {
        final UserContext userContext = ThreadContext.getUserContext();
        if (userContext == null) return false;
        final User user = userContext.getUser();
        String[] roleNames = user.getRoles().split(Constants.COMMA_SEPARATOR);
        for (String role : roleNames) {
            role = role.trim();
            if (Enums.RoleName.VideoKYCAuditor.name().equalsIgnoreCase((role))
                    || Enums.RoleName.VideoKYCAdmin.name().equalsIgnoreCase((role))
                    || Enums.RoleName.VideoKYCMonitor.name().equalsIgnoreCase((role))
            ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JSONObject checkReprocessApplicability(final Long groupId, final Integer recordingId) {

        if (!isUserAllowedToReprocess()) {
            return toJsonText(false, RecordingProcessingStatus.NonReprocessRole, 0);
        }

        final Optional<VideoKyc> oKyc = videokycService.getVideoKycStatusByGroupId(groupId);
        if (oKyc.isEmpty()) {
            return toJsonText(false, RecordingProcessingStatus.NonKycGroup, 0);
        }

        final VideoKyc videoKyc = oKyc.get();

        if (videoKyc.getCreatedDate() < System.currentTimeMillis() - kycCreatedLastInMillis) {
            return toJsonText(false, RecordingProcessingStatus.OldKyc, 0);
        }

        if (!Enums.VideoKYCStatus.Successful.getId().equals(videoKyc.getAgentStatus())) {
            return toJsonText(false, RecordingProcessingStatus.AgentStatusNotSuccessful, 0);
        }

        if (!(Enums.VideoKYCStatus.AuditorReady.getId().equals(videoKyc.getStatus())
                || Enums.VideoKYCStatus.AuditorAssigned.getId().equals(videoKyc.getStatus()))) {
            return toJsonText(false, RecordingProcessingStatus.StatusNotAuditorAssignedOrReady, 0);
        }

        final List<Recording> recordings = recordingId != null ?
                Collections.singletonList(recordingDao.findOne(recordingId))
                :
                recordingDao.getRecordingsByGroupId(groupId.intValue());

        final boolean anyReprocessing = recordings.stream().anyMatch(r -> r.getRecordingStage() == 10 || r.getRecordingStage() == 6);
        final OptionalInt reprocessRetry = recordings.stream().mapToInt(r -> r.getReprocessRetry() == null ? 0 : r.getReprocessRetry().intValue()).max();
        final int reprocessCount = reprocessRetry.orElse(0);

        if (anyReprocessing) {
            return toJsonText(false, RecordingProcessingStatus.InReProcess, reprocessCount);
        }

        if (reprocessCount >= maxReprocessingCount) {
            return toJsonText(false, RecordingProcessingStatus.MaxReprocessingExceeded, reprocessCount);
        }

        final boolean allPassed = recordings.stream().allMatch(r -> r.getRecordingStage() == 4);
        if (allPassed) {
            return toJsonText(true, RecordingProcessingStatus.AllPassed, reprocessCount);
        }
        final boolean atLeastOnePassed = recordings.stream().anyMatch(r -> r.getRecordingStage() == 4);
        if (atLeastOnePassed) {
            return toJsonText(true, RecordingProcessingStatus.AtLeastOnePassed, reprocessCount);
        }
        boolean errorDetailsInError = false;
        final List<JsonNode> recordingErrors = new ArrayList<>();
        for (Recording recording : recordings) {
            if (recording.getRecordingStage() == 5) {
                if (recording.getFailureReason() != null && recording.getFailureReason().length() > 0) {
                    try {
                        final JsonNode errorDetail = jsonUtil.readTree(recording.getFailureReason());
                        recordingErrors.add(errorDetail);
                    } catch (Exception e) {
                        logger.warn("Recording error cannot be read as json - " + recording.getFailureReason());
                        errorDetailsInError = true;
                        break;
                    }
                }
            }
        }

        if (errorDetailsInError) {
            return toJsonText(true, RecordingProcessingStatus.OtherError, reprocessCount);
        }

        //none is passed
        //check what errors, can reprocess help
        final boolean fileError = recordingErrors.stream().anyMatch(this::hasFileError);
        if (fileError) {
            return toJsonText(false, RecordingProcessingStatus.FileError, reprocessCount);
        }

        final boolean mjrConversionFailed = recordingErrors.stream().anyMatch(this::hasMJRConversionError);
        final boolean fixDurationError = recordingErrors.stream().anyMatch(e -> this.hasErrorInCombined(e, "FixDurationFailed"));
        final boolean fixVideoError = recordingErrors.stream().anyMatch(e -> this.hasErrorInCombined(e, "FixVideoErrorsFailed"));
        final boolean mergingError = recordingErrors.stream().anyMatch(this::hasMergingError);
        final boolean concatError = recordingErrors.stream().anyMatch(this::hasErrorInPartyConcat);
        if (mjrConversionFailed || fixDurationError || fixVideoError || mergingError || concatError) {
            return toJsonText(true, RecordingProcessingStatus.ProcessingError, reprocessCount);
        }
        return toJsonText(true, RecordingProcessingStatus.OtherError, reprocessCount);
    }

    private boolean hasMergingError(JsonNode error) {
        return hasErrorInPartyCombined(error, "MergingFailedWithTimeout") || hasErrorInPartyCombined(error, "MergingFailedWithNonZero")
                || hasErrorInPartyCombined(error, "MergingFailedOutputFileMissing");
    }

    private boolean hasFileError(JsonNode error) {
        return hasErrorInPartyCombined(error, "EmptyFolder") || hasErrorInPartyCombined(error, "FolderMissing")
                || hasErrorInFile(error, "Missing") || hasErrorInFile(error, "Empty");
    }

    private boolean hasMJRConversionError(JsonNode error) {
        return hasErrorInFile(error, "MjrConversionFailed");
    }

    private boolean hasErrorInPartyCombined(JsonNode error, String code) {
        final String sender = find(error, "details", "sender", "combined");
        final String receiver = find(error, "details", "receiver", "combined");
        return code.equals(sender)
                || code.equals(receiver);
    }

    private boolean hasErrorInPartyConcat(JsonNode error) {
        final String sender = find(error, "details", "sender", "concat");
        final String receiver = find(error, "details", "receiver", "concat");
        return !("Completed".equals(sender)
                || "Completed".equals(receiver));
    }

    private boolean hasErrorInCombined(JsonNode error, String code) {
        final String combined = find(error, "details", "combined");
        return code.equals(combined);
    }

    private boolean hasErrorInFile(JsonNode error, String code) {
        final String senderAudio = find(error, "details", "sender", "audio");
        final String senderVideo = find(error, "details", "sender", "video");
        final String recipientAudio = find(error, "details", "receiver", "audio");
        final String recipientVideo = find(error, "details", "receiver", "video");
        return code.equals(senderAudio)
                || code.equals(senderVideo)
                || code.equals(recipientAudio)
                || code.equals(recipientVideo);
    }

    private String find(JsonNode node, String... paths) {
        for (int i = 0; i < paths.length; i++) {
            if (i + 1 == paths.length) {
                if (node.has(paths[i])) {
                    return node.get(paths[i]).asText();
                } else {
                    return "";
                }
            } else {
                node = node.with(paths[i]);
            }
        }
        return "";
    }

    private JSONObject toJsonText(Boolean reprocess, RecordingProcessingStatus status, Integer retryCount) {
        try {
            JSONObject data = new JSONObject();
            data.put("reprocess", reprocess);
            data.put("statusId", status.getId());
            data.put("statusCode", status.getName());
//            data.put("retryCount", retryCount);
//            data.put("maxRetryCount", maxReprocessingCount);
            return data;
        } catch (JSONException e) {
            throw new InternalServerErrorException(Enums.ErrorCode.FAILED_TO_CREATE_JSON, "Failed to create JSON", e);
        }
    }


    private void updateRecordingStatus(Map<Integer, Boolean> successMap,
                                       Map<Integer, JsonNode> statusMap,
                                       Integer attachmentId,
                                       Integer senderAttachmentId,
                                       BigDecimal duration,
                                       Long utcDate,
                                       Long chatId/*, boolean forcedTranscoding*/) {
        logger.info("update recordingStatus started");
        for (Map.Entry<Integer, Boolean> entry : successMap.entrySet()) {
            if (entry.getValue()) {
                logger.info("Recording with id: {} was successful. Marking it Successful in DB", entry.getKey());
                updateRecordingOnProcessedEvent(entry.getKey(), chatId, attachmentId, senderAttachmentId,
                        RecordingStage.Processed.getId(), utcDate, duration);
            } else {
                String failureReason = String.valueOf(statusMap.get(entry.getKey()));
                logger.info("Recording with id: {} failed. Marking it Failed in DB with reason {}", entry.getKey(), failureReason);
                updateRecordingOnFailedEvent(entry.getKey(), failureReason);
            }
        }
        logger.info("update recordingStatus completed");
    }


    private ChatMessage createRecordingChatMessage(User sender, Integer to) {
        logger.info("createRecordingChatMessage enter");
        ChatMessage message = new ChatMessage();
        message.setTo(to);
        message.setType(Enums.MessageType.Chat.getId());

        message.setChatMessageType(Enums.ChatMessageType.VideoRecording.getId().intValue());
        message.setText("Live Recording");
        message.setFrom(sender.getId());

        logger.info("Process Video Recording event send to userId : " + to + " from userId " + sender.getId());
        message.setName(sender.getName());
        long utcDate = System.currentTimeMillis();
        message.setUtcDate(utcDate);
        logger.info("createRecordingChatMessage completed");
        return message;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        maxReprocessingCount = Integer.parseInt(env.getProperty(Constants.VIDEO_RECORDING_REPROCESSING_MAX_COUNT, "10"));
        logger.info("maxReprocessingCount: {}", maxReprocessingCount);
        kycCreatedLastInDays = Integer.parseInt(env.getProperty(Constants.VIDEO_RECORDING_REPROCESSING_KYC_CREATED_LAST_IN_DAYS, "2"));
        logger.info("kycCreatedLastInDays: {}", kycCreatedLastInDays);
        kycCreatedLastInMillis = kycCreatedLastInDays * 24 * 60 * 60 * 1000;
    }
}