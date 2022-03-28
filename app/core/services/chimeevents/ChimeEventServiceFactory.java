package core.services.chimeevents;

import core.utils.ChimeEnums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ChimeEventServiceFactory {
	private static final Logger logger = LoggerFactory.getLogger(ChimeEventServiceFactory.class);
	
	@Autowired
	@Qualifier("chimeMediaConvertEventServiceImpl")
	private ChimeEventService chimeMediaConvertEventServiceImpl;
	
	@Autowired
	@Qualifier("chimeMeetingEventService")
	private ChimeEventService chimeMeetingEventServiceImpl;

	@Autowired
	@Qualifier("chimeS3EventServiceImpl")
	private ChimeEventService chimeS3EventServiceImpl;
	
	@Autowired
	@Qualifier("chimeRecordingEventServiceImpl")
	private ChimeEventService chimeRecordingEventServiceImpl;

	@Autowired
	@Qualifier("chimeUIServiceEventImpl")
	private ChimeEventService chimeUIServiceEventImpl;
	
	public ChimeEventService getChimeEventService(String eventSource) {
		if (ChimeEnums.EventSource.ChimeServer.getName().equalsIgnoreCase(eventSource)) {
			logger.info("Returning chimeMeetingEventServiceImpl for processing Events.");
			return chimeMeetingEventServiceImpl;
		} else if (ChimeEnums.EventSource.MediaConvertJob.getName().equalsIgnoreCase(eventSource)) {
			logger.info("Returning chimeMediaConvertEventServiceImpl for processing Events.");
			return chimeMediaConvertEventServiceImpl;
		} else if (ChimeEnums.EventSource.S3.getName().equalsIgnoreCase(eventSource)) {
			logger.info("Returning chimeS3EventServiceImpl for processing Events.");
			return chimeS3EventServiceImpl;
		} else if (ChimeEnums.EventSource.RecordingECSTask.getName().equalsIgnoreCase(eventSource)) {
			logger.info("Returning chimeRecordingEventServiceImpl for processing Events.");
			return chimeRecordingEventServiceImpl;
		} else if (ChimeEnums.EventSource.WorkAppsWebApp.getName().equalsIgnoreCase(eventSource)) {
			logger.info("Returning chimeUIServiceEventImpl for processing Events.");
			return chimeUIServiceEventImpl;
		}
		throw new RuntimeException("Unsupported Chime Event Service for eventSource - " + eventSource);
	}

	public boolean supports(String eventSource){
		return ChimeEnums.EventSource.ChimeServer.getName().equalsIgnoreCase(eventSource) ||
				ChimeEnums.EventSource.MediaConvertJob.getName().equalsIgnoreCase(eventSource)  ||
				ChimeEnums.EventSource.RecordingECSTask.getName().equalsIgnoreCase(eventSource) ||
				ChimeEnums.EventSource.S3.getName().equalsIgnoreCase(eventSource) ||
				ChimeEnums.EventSource.WorkAppsWebApp.getName().equalsIgnoreCase(eventSource);

	}
}
