package controllers;

import com.google.inject.Singleton;
import controllers.actions.UserAuthAction;
import core.exceptions.BadRequestException;
import core.services.RecordingService;
import core.utils.Constants;
import core.utils.Enums;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.RmsApplicationContext;

@Singleton
public class RecordingController extends Controller {
    private static Logger logger = LoggerFactory.getLogger(RecordingController.class);
    private final int defaultTranscoding;

    private RecordingService recordingService;

    public RecordingController() {
        ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
        recordingService = (RecordingService) ctx.getBean(Constants.RECORDING_SERVICE_BEAN);
        Environment env = ctx.getBean(Environment.class);
        this.defaultTranscoding = Integer.parseInt(env.getProperty(Constants.VIDEO_RECORDING_REPROCESSING_DEFAULT_TRANSCODING, "-1"));
    }

    /**
     * Reprocess the given recording in a groupId. Need to ensure the group is a GCL one.
     * @param groupId
     * @param recordingId
     * @param transcoding -1 = default, 0 = false, 1 = true
     * @return
     */
    @With(UserAuthAction.class)
    public Result markForReprocessing(final Long groupId, final Integer recordingId, final Integer transcoding) throws JSONException {
        logger.info("Recording reprocessing called for groupId {}, recordingId {}, transcoding {}", groupId, recordingId, transcoding);
        final JSONObject applicability = recordingService.checkReprocessApplicability(groupId, recordingId);
        final boolean reprocess = applicability.getBoolean("reprocess");
        if (!reprocess){
            //throw error here
            throw new BadRequestException(Enums.ErrorCode.BadRequest, applicability.getString("statusCode"));
        }
        recordingService.markForReprocessing(groupId, recordingId, isTranscoding(applicability, transcoding));
        logger.info("Recording reprocessing completed for groupId {}, recordingId {}, transcoding {}", groupId, recordingId, transcoding);
        return noContent();
    }

    /**
     * Reprocess all the recordings in a group. Need to ensure the group is a KYC one.
     * @param groupId
     * @param transcoding -1 = default, 0 = false, 1 = true
     * @return
     */
    @With(UserAuthAction.class)
    public Result markForReprocessingAllInGroupId(final Long groupId, final Integer transcoding) throws JSONException {
        //Check user role in [Auditor, KycAdmin, KycMonitor], KycStatus AuditorAssigned,  Agent Status Sucessful, recording status
        logger.info("Recording reprocessing called for groupId {}, transcoding {}", groupId, transcoding);
        final JSONObject applicability = recordingService.checkReprocessApplicability(groupId, null);
        final boolean reprocess = applicability.getBoolean("reprocess");
        if (!reprocess){
            //throw error here
            throw new BadRequestException(Enums.ErrorCode.BadRequest, applicability.getString("statusCode"));
        }
        recordingService.markForReprocessing(groupId, 0, isTranscoding(applicability, transcoding));
        logger.info("Recording reprocessing completed for groupId {}, transcoding {}", groupId, transcoding);
        return noContent();
    }

    /**
     *
     * @param groupId
     * @param recordingId
     * @return
     */
    @With(UserAuthAction.class)
    public Result checkReprocessApplicability(final Long groupId, final Integer recordingId) throws JSONException {
        logger.info("Check Recording reprocessing capability for groupId {}, recordingId {}", groupId, recordingId);
        final JSONObject result = recordingService.checkReprocessApplicability(groupId, recordingId);
        logger.info("Recording reprocessing capability for groupId {}, recordingId {} is found as {}", groupId, recordingId, result);
        return ok(result.toString());
    }

    /**
     *
     * @param groupId
     * @return
     */
    @With(UserAuthAction.class)
    public Result checkReprocessApplicabilityAllInGroupId(final Long groupId) throws JSONException  {
        logger.info("Check Recording reprocessing capability for groupId {}", groupId);
        final JSONObject result = recordingService.checkReprocessApplicability(groupId, null);
        logger.info("Recording reprocessing capability for groupId {} is found as {}", groupId, result);
        return ok(result.toString());
    }

    private boolean isTranscoding(JSONObject reprocessingApplicability, Integer requestedTranscoding) throws JSONException{
        if (requestedTranscoding != -1){
            return requestedTranscoding == 1;
        }
        if (defaultTranscoding != -1){
            return defaultTranscoding == 1;
        }
        final int statusId = reprocessingApplicability.getInt("statusId");
        return (statusId == Enums.RecordingProcessingStatus.AllPassed.getId()
                || statusId == Enums.RecordingProcessingStatus.AtLeastOnePassed.getId()
                || statusId == Enums.RecordingProcessingStatus.ProcessingError.getId()
        );
    }
}
