package core.mediaconvert;

import core.exceptions.InternalServerErrorException;
import core.utils.Enums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service("mediaConvertServiceImpl")
public class MediaConvertServiceImpl implements MediaConvertService {
    private static final Logger logger = LoggerFactory.getLogger(MediaConvertServiceImpl.class);

    private final MediaConvertConfig mediaConvertConfig;

    @Value("${aws.mediaconvert.mediaconvertrolearn}")
    private String mediaConvertRoleARN;

    public MediaConvertServiceImpl(MediaConvertConfig mediaConvertConfig) {
        this.mediaConvertConfig = mediaConvertConfig;
    }

    @Override
    public String initiateMediaJob(String sourceS3Path, String destinationS3Path) {
        MediaConvertClient endPointMediaClient = mediaConvertConfig.getMediaConvertClient();

      //  String mediaConvertJobId =
      //          createMediaJob(endPointMediaClient, sourceS3Path, destinationS3Path);

        String mediaConvertJobId =
                createMediaJobForJobTemplate(endPointMediaClient, sourceS3Path, destinationS3Path);

        endPointMediaClient.close();

        logger.info("MediaConvert job created. Job Id = " + mediaConvertJobId);

        return mediaConvertJobId;
    }

    private String createMediaJobForJobTemplate(MediaConvertClient endPointMediaClient, String sourceS3Path, String destinationS3Path) {
        try {
            // output dashISOVideoOutput Profile
            Output dashISOVideoOutput = createOutputForVideo("_vid@480p", 500000);
            // output dashISOAudioOutput Profile
            Output dashISOAudioOutput = createOutputForAudio("_aud@96k", 96000);

            OutputGroup dashISOOutputGroup = OutputGroup
                    .builder().name("DASH ISO").customName(
                            "Output to S3")
                    .outputGroupSettings(OutputGroupSettings.builder().type(OutputGroupType.DASH_ISO_GROUP_SETTINGS)
                            .dashIsoGroupSettings(DashIsoGroupSettings.builder()
                                    .audioChannelConfigSchemeIdUri(
                                            DashIsoGroupAudioChannelConfigSchemeIdUri.MPEG_CHANNEL_CONFIGURATION)
                                    .segmentLength(10).destination(destinationS3Path).fragmentLength(2)
                                    .segmentControl(DashIsoSegmentControl.SEGMENTED_FILES)
                                    .writeSegmentTimelineInRepresentation(DashIsoWriteSegmentTimelineInRepresentation.ENABLED)
                                    .build())
                            .build())
                    .outputs(dashISOVideoOutput, dashISOAudioOutput).build();

            Map<String, AudioSelector> audioSelectors = new HashMap<String, AudioSelector>();
            audioSelectors.put("Audio Selector 1",
                    AudioSelector.builder().defaultSelection(AudioDefaultSelection.DEFAULT).build());

            JobSettings jobSettings =
                    JobSettings.builder().timecodeConfig(TimecodeConfig.builder().source(TimecodeSource.ZEROBASED).build())
                            .inputs(Input.builder().audioSelectors(audioSelectors)
                                    .videoSelector(
                                            VideoSelector.builder().colorSpace(ColorSpace.FOLLOW).rotate(InputRotate.DEGREE_0).build())
                                    .timecodeSource(InputTimecodeSource.ZEROBASED).fileInput(sourceS3Path).build())
                            .outputGroups(dashISOOutputGroup).build();

            CreateJobRequest createJobRequest = CreateJobRequest.builder()
                    .jobTemplate("arn:aws:mediaconvert:ap-south-1:558173974864:jobTemplates/workapps-test")
                    .role(mediaConvertRoleARN).settings(jobSettings)
                    .accelerationSettings(AccelerationSettings.builder().mode(AccelerationMode.DISABLED).build())
                    .statusUpdateInterval(StatusUpdateInterval.SECONDS_10).priority(0)
                    .hopDestinations(Collections.emptyList()).build();

            CreateJobResponse createJobResponse = endPointMediaClient.createJob(createJobRequest);
            return createJobResponse.job().id();

        } catch (MediaConvertException e) {
            logger.error("Failed to process MediaConvert Job :" + e);
            throw new InternalServerErrorException(Enums.ErrorCode.MEDIACONVERT_PROCESS_FAILED, Enums.ErrorCode.MEDIACONVERT_PROCESS_FAILED.getName());
        }
    }

    private final Output createOutputForVideo(String nameModifier, int maxBitrate) {

        Output output = null;
        try {
            output = Output.builder().nameModifier(nameModifier)
                    .containerSettings(ContainerSettings.builder().container(ContainerType.MPD)
                            .mpdSettings(MpdSettings.builder().captionContainerType(MpdCaptionContainerType.FRAGMENTED_MP4)
                                    .build())
                            .build())
                    .videoDescription(VideoDescription.builder().width(640).height(480).sharpness(50)
                            .codecSettings(VideoCodecSettings.builder().codec(VideoCodec.H_264)
                                    .h264Settings(H264Settings.builder().parNumerator(1).numberReferenceFrames(3)
                                            .gopClosedCadence(1).hrdBufferInitialFillPercentage(90).gopSize((double) 4)
                                            .rateControlMode(H264RateControlMode.QVBR).maxBitrate(maxBitrate)
                                            .gopBReference(H264GopBReference.ENABLED).parDenominator(1)
                                            .sceneChangeDetect(H264SceneChangeDetect.TRANSITION_DETECTION)
                                            .qualityTuningLevel(H264QualityTuningLevel.SINGLE_PASS_HQ)
                                            .gopSizeUnits(H264GopSizeUnits.SECONDS).parControl(H264ParControl.SPECIFIED)
                                            .numberBFramesBetweenReferenceFrames(3).dynamicSubGop(H264DynamicSubGop.ADAPTIVE).build())
                                    .build())
                            .build())
                    .build();
        } catch (MediaConvertException e) {
            logger.error("Failed to process createOutputForVideo :" + e);
            throw new InternalServerErrorException(Enums.ErrorCode.MEDIACONVERT_VIDEO_OUTPUT_PROCESS_FAILED, Enums.ErrorCode.MEDIACONVERT_VIDEO_OUTPUT_PROCESS_FAILED.getName());
        }
        return output;
    }

    private final Output createOutputForAudio(String nameModifier, int bitRate) {
        Output output = null;
        try {
            output = Output.builder().nameModifier(nameModifier)
                    .containerSettings(ContainerSettings.builder().container(ContainerType.MPD)
                            .mpdSettings(
                                    MpdSettings.builder().captionContainerType(MpdCaptionContainerType.FRAGMENTED_MP4).build())
                            .build())
                    .audioDescriptions(AudioDescription.builder().audioSourceName("Audio Selector 1")
                            .codecSettings(AudioCodecSettings
                                    .builder().codec(AudioCodec.AAC).aacSettings(AacSettings.builder()
                                            .codingMode(AacCodingMode.CODING_MODE_2_0).sampleRate(48000).bitrate(bitRate).build())
                                    .build())
                            .build())
                    .build();
        } catch (MediaConvertException e) {
            logger.error("Failed to process createOutputForAudio :" + e);
            throw new InternalServerErrorException(Enums.ErrorCode.MEDIACONVERT_AUDIO_OUTPUT_PROCESS_FAILED, Enums.ErrorCode.MEDIACONVERT_AUDIO_OUTPUT_PROCESS_FAILED.getName());
        }
        return output;
    }

    public String createMediaJob(MediaConvertClient endPointMediaClient, String sourceS3Path, String destinationS3Path) {


        String thumbsOutput = destinationS3Path + "/thumbs/";
        String mp4Output = destinationS3Path + "/mp4/";

        try {

            // output dashISOOutput Profile
            Output dashISOOutput = createOutput("-video-hi-res", "_$dt$", 50000);

            OutputGroup dashISOOutputGroup = OutputGroup.builder().name("DASH ISO").customName("Media")
                    .outputGroupSettings(OutputGroupSettings.builder().type(OutputGroupType.DASH_ISO_GROUP_SETTINGS)
                            .dashIsoGroupSettings(DashIsoGroupSettings.builder().segmentLength(30)
                                    .destination(destinationS3Path).fragmentLength(2).build())
                            .build())
                    .outputs(dashISOOutput).build();

            OutputGroup fileMp4 = OutputGroup.builder().name("File Group").customName("mp4")
                    .outputGroupSettings(OutputGroupSettings.builder().type(OutputGroupType.FILE_GROUP_SETTINGS)
                            .fileGroupSettings(FileGroupSettings.builder().destination(mp4Output).build()).build())
                    .outputs(Output.builder().extension("mp4")
                            .containerSettings(ContainerSettings.builder().container(ContainerType.MP4).build())
                            .videoDescription(VideoDescription.builder().width(1280).height(720)
                                    .scalingBehavior(ScalingBehavior.DEFAULT).sharpness(50).antiAlias(AntiAlias.ENABLED)
                                    .timecodeInsertion(VideoTimecodeInsertion.DISABLED).colorMetadata(ColorMetadata.INSERT)
                                    .respondToAfd(RespondToAfd.NONE).afdSignaling(AfdSignaling.NONE)
                                    .dropFrameTimecode(DropFrameTimecode.ENABLED)
                                    .codecSettings(VideoCodecSettings.builder().codec(VideoCodec.H_264)
                                            .h264Settings(H264Settings.builder().rateControlMode(H264RateControlMode.QVBR)
                                                    .parControl(H264ParControl.INITIALIZE_FROM_SOURCE)
                                                    .qualityTuningLevel(H264QualityTuningLevel.SINGLE_PASS)
                                                    .qvbrSettings(H264QvbrSettings.builder().qvbrQualityLevel(8).build())
                                                    .codecLevel(H264CodecLevel.AUTO).codecProfile(H264CodecProfile.MAIN).maxBitrate(2400000)
                                                    .framerateControl(H264FramerateControl.INITIALIZE_FROM_SOURCE).gopSize(2.0)
                                                    .gopSizeUnits(H264GopSizeUnits.SECONDS).numberBFramesBetweenReferenceFrames(2)
                                                    .gopClosedCadence(1).gopBReference(H264GopBReference.DISABLED)
                                                    .slowPal(H264SlowPal.DISABLED).syntax(H264Syntax.DEFAULT).numberReferenceFrames(3)
                                                    .dynamicSubGop(H264DynamicSubGop.STATIC).fieldEncoding(H264FieldEncoding.PAFF)
                                                    .sceneChangeDetect(H264SceneChangeDetect.ENABLED).minIInterval(0)
                                                    .telecine(H264Telecine.NONE)
                                                    .framerateConversionAlgorithm(H264FramerateConversionAlgorithm.DUPLICATE_DROP)
                                                    .entropyEncoding(H264EntropyEncoding.CABAC).slices(1)
                                                    .unregisteredSeiTimecode(H264UnregisteredSeiTimecode.DISABLED)
                                                    .repeatPps(H264RepeatPps.DISABLED).adaptiveQuantization(H264AdaptiveQuantization.HIGH)
                                                    .spatialAdaptiveQuantization(H264SpatialAdaptiveQuantization.ENABLED)
                                                    .temporalAdaptiveQuantization(H264TemporalAdaptiveQuantization.ENABLED)
                                                    .flickerAdaptiveQuantization(H264FlickerAdaptiveQuantization.DISABLED).softness(0)
                                                    .interlaceMode(H264InterlaceMode.PROGRESSIVE).build())
                                            .build())
                                    .build())
                            .audioDescriptions(AudioDescription.builder().audioTypeControl(AudioTypeControl.FOLLOW_INPUT)
                                    .languageCodeControl(AudioLanguageCodeControl.FOLLOW_INPUT)
                                    .codecSettings(AudioCodecSettings.builder().codec(AudioCodec.AAC)
                                            .aacSettings(AacSettings.builder().codecProfile(AacCodecProfile.LC)
                                                    .rateControlMode(AacRateControlMode.CBR).codingMode(AacCodingMode.CODING_MODE_2_0)
                                                    .sampleRate(44100).bitrate(160000).rawFormat(AacRawFormat.NONE)
                                                    .specification(AacSpecification.MPEG4)
                                                    .audioDescriptionBroadcasterMix(AacAudioDescriptionBroadcasterMix.NORMAL).build())
                                            .build())
                                    .build())
                            .build())
                    .build();
            OutputGroup thumbs = OutputGroup.builder().name("File Group").customName("thumbs")
                    .outputGroupSettings(OutputGroupSettings.builder().type(OutputGroupType.FILE_GROUP_SETTINGS)
                            .fileGroupSettings(FileGroupSettings.builder().destination(thumbsOutput).build()).build())
                    .outputs(Output.builder().extension("jpg")
                            .containerSettings(ContainerSettings.builder().container(ContainerType.RAW).build())
                            .videoDescription(VideoDescription.builder().scalingBehavior(ScalingBehavior.DEFAULT).sharpness(50)
                                    .antiAlias(AntiAlias.ENABLED).timecodeInsertion(VideoTimecodeInsertion.DISABLED)
                                    .colorMetadata(ColorMetadata.INSERT).dropFrameTimecode(DropFrameTimecode.ENABLED)
                                    .codecSettings(VideoCodecSettings.builder().codec(VideoCodec.FRAME_CAPTURE)
                                            .frameCaptureSettings(FrameCaptureSettings.builder().framerateNumerator(1)
                                                    .framerateDenominator(1).maxCaptures(10000000).quality(80).build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            Map<String, AudioSelector> audioSelectors = new HashMap<String, AudioSelector>();
            audioSelectors.put("Audio Selector 1",
                    AudioSelector.builder().defaultSelection(AudioDefaultSelection.DEFAULT).offset(0).build());

            JobSettings jobSettings = JobSettings.builder()
                    .inputs(Input.builder().audioSelectors(audioSelectors)
                            .videoSelector(
                                    VideoSelector.builder().colorSpace(ColorSpace.FOLLOW).rotate(InputRotate.DEGREE_0).build())
                            .timecodeSource(InputTimecodeSource.ZEROBASED).fileInput(sourceS3Path).build())
                    .outputGroups(dashISOOutputGroup, fileMp4, thumbs).build();

            CreateJobRequest createJobRequest =
                    CreateJobRequest.builder().role(mediaConvertRoleARN).settings(jobSettings).build();

            CreateJobResponse createJobResponse = endPointMediaClient.createJob(createJobRequest);
            return createJobResponse.job().id();

        } catch (MediaConvertException e) {
            logger.error("Failed to process MediaConvert Job :" + e);
            throw new InternalServerErrorException(Enums.ErrorCode.MEDIACONVERT_PROCESS_FAILED, Enums.ErrorCode.MEDIACONVERT_PROCESS_FAILED.getName());
        }
    }

    private final Output createOutput(String nameModifier, String segmentModifier, int qvbrMaxBitrate) {
        try {
            Output output =
                    Output.builder().nameModifier(nameModifier)
                            .containerSettings(ContainerSettings.builder().container(ContainerType.MPD).build())
                            .videoDescription(VideoDescription.builder()
                                    .codecSettings(VideoCodecSettings.builder().codec(VideoCodec.H_264)
                                            .h264Settings(H264Settings.builder().rateControlMode(H264RateControlMode.QVBR)
                                                    .maxBitrate(qvbrMaxBitrate)
                                                    .sceneChangeDetect(H264SceneChangeDetect.TRANSITION_DETECTION).build())
                                            .build())
                                    .build())
                            .audioDescriptions(AudioDescription.builder().audioSourceName("Audio Selector 1")
                                    .codecSettings(AudioCodecSettings.builder().codec(AudioCodec.AAC)
                                            .aacSettings(AacSettings.builder().codingMode(AacCodingMode.CODING_MODE_2_0)
                                                    .sampleRate(48000).bitrate(96000).specification(AacSpecification.MPEG4).build())
                                            .build())
                                    .build())
                            .build();
            return output;
        } catch (MediaConvertException e) {
            logger.error("Failed to process MediaConvert Job :" + e);
            throw new InternalServerErrorException(Enums.ErrorCode.MEDIACONVERT_PROCESS_FAILED, Enums.ErrorCode.MEDIACONVERT_PROCESS_FAILED.getName());
        }
    }
}
