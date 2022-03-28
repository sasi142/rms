package core.utils;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ChimeEnums {

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum EventSource {
		WorkAppsServer(1, "WorkAppsServer"),
		ChimeServer(2, "ChimeServer"),
		RecordingECSTask(3, "RecordingECSTask"),
		MediaConvertJob(4, "MediaConvertJob"),
		S3(5, "S3"),
		WorkAppsWebApp(6, "WorkAppsWebApp"),
		ChimeJavaScriptSDK(7, "ChimeJavaScriptSDK"),
		WorkAppsAndroidApp(8, "WorkAppsAndroidApp"),
		ChimeAndroidSDK(9, "ChimeAndroidSDK"),
		WorkAppsIOSApp(10, "WorkAppsIOSApp"),
		ChimeIOSSDK(11, "ChimeIOSSDK");

		private Integer	id;
		private String	name;

		private EventSource(final Integer id, final String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		public void setId(final Integer id) {
			this.id = id;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public static EventSource getEnum(String name) {
			EventSource sources[] = EventSource.values();
			EventSource eventSource = null;
			for (EventSource source:sources) {
				if (source.getName().equalsIgnoreCase(name)) {
					eventSource = source;
					break;
				}
			}
			return eventSource;
		}
	}

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum EventType {
		WorkappsMeetingCreated(1,"Workapps:MeetingCreated"),
		WorkappsMeetingStarting(2,"Workapps:MeetingStarting"),
		ChimeMeetingStarted(3,"Chime:MeetingStarted"),
		ChimeMeetingEnded(4,"Chime:MeetingEnded"),
		ChimeAttendeeAdded (5,"Chime:AttendeeAdded"),
		ChimeAttendeeDeleted (6,"Chime:AttendeeDeleted"),
		ChimeAttendeeAuthorized (7,"Chime:AttendeeAuthorized"),
		ChimeAttendeeJoined(8,"Chime:AttendeeJoined"),
		ChimeAttendeeLeft(9,"Chime:AttendeeLeft"),
		ChimeAttendeeDropped(10,"Chime:AttendeeDropped"),
		ChimeAttendeeVideoStarted(11,"Chime:AttendeeVideoStarted"),
		ChimeAttendeeVideoStopped(12,"Chime:AttendeeVideoStopped"),
		ChimeAttendeeContentJoined(13,"Chime:AttendeeContentJoined"),
		ChimeAttendeeContentLeft(14,"Chime:AttendeeContentLeft"),
		ChimeAttendeeContentDropped(15,"Chime:AttendeeContentDropped"),
		ChimeAttendeeContentVideoSStarted(16,"Chime:AttendeeContentVideoStarted"),
		ChimeAttendeeContentVideoSStopped(17,"Chime:AttendeeContentVideoStopped"),
		RmsRecordingStarting(18,"Rms:RecordingStarting"),
		RmsRecordingStarted(19,"Rms:RecordingStarted"),
		RmsRecordingStopping(20,"Rms:RecordingStopping"),
		RmsRecordingStopped(21,"Rms:RecordingStopped"),
		RmsRecordingFileAvailable(22,"Rms:RecordingFileAvailable"),
		OwbProcessingStarting(23,"Owb:ProcessingStarting"),
		OwbProcessingFailed (24,"Owb:ProcessingFailed"),
		MconvertSubmitted(25,"SUBMITTED"),
		MconvertProcessingStarted(26,"PROGRESSING"),
		MconvertProcessed(27,"COMPLETE"),
		MconvertFailed(28,"ERROR"),
		MeetingStartRequested(29,"MeetingStartRequested"),
		MeetingStartSucceeded(30,"MeetingStartSucceeded"),
		MeetingReconnected(31,"MeetingReconnected"),
		MeetingStartFailed (32,"MeetingStartFailed"),
		MeetingEnded(33,"MeetingEnded"),
		MeetingFailed(34,"MeetingFailed"),
		AttendeePresenceReceived(35,"AttendeePresenceReceived"),
		AudioInputSelected(36,"AudioInputSelected"),
		AudioInputUnselected (37,"AudioInputUnselected"),
		AudioInputFailed (38,"AudioInputFailed"),
		VideoInputSelected(39,"VideoInputSelected"),
		VideoInputUnselected(40,"VideoInputUnselected"),
		VideoInputFailed(41,"VideoInputFailed"),
		SignalingDropped(42,"SignalingDropped"),
		ReceivingAudioDropped(43,"ReceivingAudioDropped"),
		AudioAttendeeRemoved(44,"AudioAttendeeRemoved"),
		AudioAuthenticationRejected(45,"AudioAuthenticationRejected"),
		AudioCallAtCapacity(46,"AudioCallAtCapacity"),
		AudioDisconnected(47,"AudioDisconnected"),
		AudioInternalServerError(48,"AudioInternalServerError"),
		AudioJoinedFromAnotherDevice(49,"AudioJoinedFromAnotherDevice"),
		AudioServiceUnavailable(50,"AudioServiceUnavailable"),
		ConnectionHealthReconnect(51,"ConnectionHealthReconnect"),
		ICEGatheringTimeoutWorkaround(52,"ICEGatheringTimeoutWorkaround"),
		IncompatibleSDP(53,"IncompatibleSDP"),
		Left(54,"Left"),
		NoAttendeePresent(55,"NoAttendeePresent"),
		OK(56,"OK"),
		RealtimeApiFailed(57,"RealtimeApiFailed"),
		SignalingBadRequest(58,"SignalingBadRequest"),
		SignalingInternalServerError(59,"SignalingInternalServerError"),
		SignalingRequestFailed(60,"SignalingRequestFailed"),
		TURNCredentialsForbidden(61,"TURNCredentialsForbidden"),
		TaskFailed(62,"TaskFailed"),
		VideoCallAtSourceCapacity(63,"VideoCallAtSourceCapacity"),
		VideoCallSwitchToViewOnly(64,"VideoCallSwitchToViewOnly"),
		RmsRecordingRunning(65,"Rms:RecordingRunning"),
		RmsRecordingPending(66,"Rms:RecordingPending"),
		S3PutObject(67,"PutObject");

		private Integer	id;
		private String	name;


		private EventType(final Integer id, final String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		public void setId(final Integer id) {
			this.id = id;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public static EventType getEnum(String name) {
			EventType types[] = EventType.values();
			EventType eventType = null;
			for (EventType type:types) {
				if (type.getName().equalsIgnoreCase(name)) {
					eventType = type;
					break;
				}
			}
			return eventType;
		}
	}
}
