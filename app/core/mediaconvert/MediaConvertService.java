package core.mediaconvert;

public interface MediaConvertService
{
    String initiateMediaJob(String sourceS3Path, String destinationS3Path);
}
