package core.utils;

import com.fasterxml.jackson.annotation.JsonFormat;


import core.entities.Group;
import core.entities.GroupMember;
import core.entities.User;
import core.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Enums {


	public interface IdEnum<ID extends Comparable<ID>, E extends Enum<E>>{
		ID getId();
	}

	public interface IdNameEnum<ID extends Comparable<ID>, E extends Enum<E>> extends IdEnum<ID, E>{
		String getName();
	}

	public static <ID extends Comparable<ID>, E extends Enum<E>, T extends IdEnum<ID, E>> Optional<T> find(Class<T> eClass, ID id) {
		return Arrays.stream(eClass.getEnumConstants()).filter(v -> v.getId().equals(id)).findAny();
	}

	public static <ID extends Comparable<ID>, E extends Enum<E>, T extends IdNameEnum<ID, E>> Optional<T> find(Class<T> eClass, String name) {
		return Arrays.stream(eClass.getEnumConstants()).filter(v -> v.getName().equals(name)).findAny();
	}

	public static <ID extends Comparable<ID>, E extends Enum<E>, T extends IdEnum<ID, E>> T get(Class<T> eClass, ID id) {
		Optional<T> value = Enums.find(eClass, id);
		if (value.isEmpty()){
			throw new BadRequestException(ErrorCode.Invalid_Enum_Value, "Enum " + eClass.getSimpleName() + " not found for id " + id);
		}
		return value.get();
	}

	public static <ID extends Comparable<ID>, E extends Enum<E>, T extends IdNameEnum<ID, E>> T get(Class<T> eClass, String name) {
		Optional<T> value = Enums.find(eClass, name);
		if (value.isEmpty()){
			throw new BadRequestException(ErrorCode.Invalid_Enum_Value, "Enum " + eClass.getSimpleName() + " not found for name " + name);
		}
		return value.get();
	}

	private static final Logger logger = LoggerFactory.getLogger(Enums.class);
	public enum DatabaseType {
		Ims,
		Rms
	};

	public enum DataOperation {
		Create(0),
		Remove(1);

		private Integer	id;

		private DataOperation(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	};

	public enum MessageType implements IdEnum<Integer, MessageType>{
		AcceptConnection(0),  
		Chat(1),
		Presence(2),
		Error(3),
		IQ(4),
		ACK(5),
		Ping(6),
		Event(7),
		Notification(8),
		Typing(9),
		StopTyping(10),
		ServerPing(11),
		Info(12),
		VideoSignalling(13),
		VideoKyc(14),
		MeetingInfoTrackingEvent(15),
		UserInfoTrackingEvent(16);

		private Integer								id;

		private static Map<Integer, MessageType>	map	= null;

		private MessageType(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public static MessageType getMessageTypeById(Integer id) {
			if (map == null) {
				map = new HashMap<Integer, MessageType>();
				MessageType[] msgTypes = MessageType.values();
				for (MessageType messageType : msgTypes) {
					map.put(messageType.getId(), messageType);
				}
			}
			return map.get(id);
		}
	}	

	public static String getMessageSubTypeByTypeIdSubTypeId(Integer typeId, Integer subTypeId) {
		MessageType msgType = MessageType.getMessageTypeById(typeId);
		String subTypeName = "";
		switch (msgType) {
			case AcceptConnection:
				break;
			case Chat:
				subTypeName = ChatType.getChatTypeById(subTypeId).name();
				break;
			case Presence:
				break;
			case Error:
				subTypeName = ErrorType.getErrorTypeById(subTypeId).name();
				break;
			case IQ:
				subTypeName = IqType.getIqTypeById(subTypeId).name();
				break;
			case ACK:
				subTypeName = ACKType.getACKTypeById(subTypeId).name();
				break;
			case Ping:
				break;
			case Event:
				subTypeName = EventType.getEventTypeById(subTypeId).name();
				break;
			case Notification:
				subTypeName = NotificationType.getNotificationTypeById(subTypeId).name();
				break;
			case Typing:
				break;
			case StopTyping:
				break;
			case ServerPing:
				break;
			case Info:
				break;
		}
		return subTypeName;
	}

	public enum RmsMessageType {
		In,
		Out,
		DeleteActor,
		MobilePush;
	}

	public enum PresenceStatus {
		Unavailable,
		AvailableWeb,
		AvailableMobile,
		Away,
		DnD,
		Idle
	}
	
	public enum UserStatus {
		Available(1, "Available");
		private Integer	id;
		private String	name;

		private UserStatus(final Integer id, final String name) {
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
	}
	
	

	public enum MemoType {
		RegulerMemoUserSelection(1),
		RegulerMemoExcelSelction(2),
		CustomMemo(3),
		CustomMemoSummary(4);


		private Integer								id;	
		private MemoType(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public static MemoType getMemoTypeById(Integer memoTypeId) {
			MemoType memoType = RegulerMemoUserSelection;
			switch (memoTypeId) {		
				case 1:
					memoType = RegulerMemoUserSelection;
					break;
				case 2:
					memoType = RegulerMemoExcelSelction;
					break;
				case 3:
					memoType = CustomMemo;
					break;
				case 4:
					memoType = CustomMemoSummary;
					break;
				default:
					;
			}
			return memoType;
		}
	}	
	

	public enum ChatType {
		One2One(0),
		GroupChat(1),
		MultiChat(2),
		One2OneVideoChat(3);
		private Byte	id;

		private ChatType(final Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		public Byte getId() {
			return id;
		}

		public static ChatType getChatTypeById(Integer intChatType) {
			ChatType chatType = One2One;
			switch (intChatType) {
				case 0:
					chatType = One2One;
					break;
				case 1:
					chatType = GroupChat;
					break;
				case 2:
					chatType = MultiChat;
					break;
				case 3:
					chatType = One2OneVideoChat;
					break;
				default:
					;
			}
			return chatType;
		}
	}
	
	public enum DeleteOption {
		DeleteMessageForMe(1),
		DeleteMessageForAll(2);
		private Integer		id;
		private DeleteOption(final Integer id){
			this.id = id;
		}
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
	}

	public enum MessageStatus {
		UnRead(true),
		Read(false);
		private Boolean	status;

		private MessageStatus(final Boolean status) {
			this.status = status;
		}

		public Boolean getStatus() {
			return status;
		}

		public void setStatus(Boolean status) {
			this.status = status;
		}
	}

	public enum IqType {
		Request(0),
		Response(1);
		private Integer	id;

		private IqType(final Integer id) {
			this.id = id;
		}

		private static Map<Integer, IqType>	map	= null;

		public static IqType getIqTypeById(Integer id) {
			if (map == null) {
				map = new HashMap<Integer, IqType>();
				IqType[] iqTypes = IqType.values();
				for (IqType iqType : iqTypes) {
					map.put(iqType.getId(), iqType);
				}
			}
			return map.get(id);
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	public enum ErrorType {
		Chat(0),
		Presence(1),
		Contact(2),
		IQ(3),
		ACK(4),
		Ping(5),
		Event(6),
		Notification(7),
		RecipientNotAvailable(8);

		private Integer							id;

		private static Map<Integer, ErrorType>	map	= null;

		private ErrorType(final Integer id) {
			this.id = id;
		}

		public static ErrorType getErrorTypeById(Integer id) {
			if (map == null) {
				map = new HashMap<Integer, ErrorType>();
				ErrorType[] errorTypes = ErrorType.values();
				for (ErrorType errorType : errorTypes) {
					map.put(errorType.getId(), errorType);
				}
			}
			return map.get(id);
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	public enum ContactType {
		Active(0),
		Recent(1),
		Normal(2);
		private Integer	id;

		private ContactType(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	public enum ChatContactType {
		OrgContact(1),
		CoWorker(2),
		None(3),
		Group(4),
		Guest(5),
		Customer(6);
		private Integer	id;

		private ChatContactType(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	/**
	 * ResetPassword & Forgot Password is same
	 * Logout & Expire token is same
	 */
	public enum EventType implements IdEnum<Integer, EventType>{
		ContactAdd(0),
		ContactRemove(1),
		DeleteUser(2),
		LockUser(3),
		ResetPassword(4),
		Logout(5),
		UserUpdate(6),
		ContactAddOrg(7),
		CloseConnection(8),
		AddGroup(9),
		AddGroup_AddMember(901),
		UpdateGroup(10),
		UpdateGroup_AddMember(101),
		UpdateGroup_RemoveMember(102),
		UpdateGroup_ChangeGroupName(103),
		UpdateGroup_AddAdmin(104),
		UpdateGroup_RemoveAdmin(105),
		CloseGroup(11),
		LeaveGroup(12),
		ExitGroup(13),
		CreateMemo(14),
		ADSync_AddGroup(15),
		ADSync_UpdateGroup_AddMember(16),
		ADSync_UpdateGroup_RemoveMember(17),
		ADSync_CloseGroup(18),
		ADSync_UpdateGroup_ChangeGroupName(19),
		ChatGuestAdded(20),
		ProcessedVideoRecording(21),
		VideoCallRequest(22),		
		VideoCallEnded(23),
		NotRegisteredMessage(24), // This is a place holder message and gets split between NotRegisteredMessageForSender and NotRegisteredMessageForReceiver at run time
		NotRegisteredMessageSent(25),
		NotRegisteredMessageReceived(26),
		OpenChatEnabledInGroup(27),
		DeviceUpdated(28),
		GuestChatDeviceInfoUpdate(29),
		UpdateUseCaseStatus(30),		
		CreateMessage(31),
		FailedVideoRecording(32), 
		GuestChatDeviceInfo(33),
		VideoCallRejected(34),		
        VideoCallIgnored(35), 
        VideoCallAccepted(36), 
        VideoCallConnected(37),
        VideoCallDisconnected(38),
        VideoCallRating(39),
		SessionExpired(40),
		CreateVideoKyc(41),
		UpdateVideoKycStatus(42),
		SocketConnect(43),
		SocketDisConnect(44),
		UpdateUserRole(45),
		ChatCustomerForwardGuestAdded(46),
		AgentAssignedToVideoKyc(47),
		UpdateGroupBySystem(48),
		UploadAttachment(49),
		UpdateGroupBySystem_RemoveMember(50),
		AgentAssignedToDumpKyc(51);
		

		private Integer	id;

		private EventType(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public static EventType getEventTypeById(Integer id) {
			EventType[] values = EventType.values();
			for (EventType eventType : values) {
				if (eventType.getId().intValue() == id) {
					return eventType;
				}
			}
			return null;
		}
	}

	public enum EventNotificationType {
		NoEvent(0),
		MemeberAdded(1),
		MemberRemoved(2);

		private Byte	id;

		private EventNotificationType(Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		public Byte getId() {
			return id;
		}
	}

	
	public enum NotificationType {
		WorkApps(0),
		Chat(1),
		GroupChat(2),
		AddGroup(3),
		AddGroupMember(4),
		Memo(5),
		ChatGuestAdded(6),
		DeleteMessage(7),
		GuestChatDeviceInfoUpdate(8),
		VideoCall(9),
		VideoKycAgentAssigned(10);
		
		private Integer	id;

		private NotificationType(final Integer id) {
			this.id = id;
		}

		private static Map<Integer, NotificationType>	map	= null;

		public static NotificationType getNotificationTypeById(Integer id) {
			if (map == null) {
				map = new HashMap<Integer, NotificationType>();
				NotificationType[] notificationTypes = NotificationType.values();
				for (NotificationType notificationType : notificationTypes) {
					map.put(notificationType.getId(), notificationType);
				}
			}
			return map.get(id);
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	public enum Env {
		prod,
		dev,
		qa,
		stage
	}

	public enum DataType {
		Text(0),
		File(1);
		private Integer	id;

		private DataType(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
	
	public enum FileType {
		RecordingFile(1, "RecordingFile"),
		ScreenShot(1, "ScreenShot");
		private Integer	id;
		private String	name;

		private FileType(final Integer id, String name) {
			this.id = id;
			this.name= name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public enum DeviceType {
		Android,
		iOS,
		WebBrowser,
		NotSupported;

		public static DeviceType getDeviceType(String type) {
			if (Android.toString().equals(type)) {
				return Android;
			} else if (iOS.toString().equals(type)) {
				return iOS;
			} else if (WebBrowser.toString().equals(type)) {
				return WebBrowser;
			} else {
				return NotSupported;
			}

		}
	}

	public enum IqActionType implements IdNameEnum<Integer, IqActionType> {
		GetUnReadCount(0, "GetUnReadCount"),
		UpdateChatStatusRead(1, "UpdateChatStatusRead"),
		GetPresence(2, "GetPresence"),
		GetUser(3, "GetUser"),
		GetContacts(4, "GetContacts"),
		GetPresenceLocAndUnreadCount(5, "GetPresenceLocAndUnreadCount"),
		GetContactCountToSync(6, "GetContactCountToSync"),
		GetChatHistory(7, "GetChatHistory"),
		GetContactsForFirstTime(8, "GetContactsForFirstTime"),
		GetCustomStatus(9, "GetCustomStatus"),
		GetProfileInfo(10, "GetProfileInfo"),
		GetContactsV2(11, "GetContactsV2"),
		GetUnreadCountV2(12, "GetUnreadCountV2"),
		UpdateChatStatusReadV2(13, "UpdateChatStatusReadV2"),
		GetUnreadMsgCount(14, "GetUnreadMsgCount"),
		MsgReadInfo(15, "MsgReadInfo"),
		GetUserInfo(16, "GetUserInfo"),
		UpdateUserMapWindowStatus(17, "UpdateUserMapWindowStatus"),
		StartVideoRecording(18, "StartVideoRecording"),
		StopVideoRecording(19, "StopVideoRecording"),
		DiscardVideoRecording(20, "DiscardVideoRecording"),
		DeleteChatMessage(21, "DeleteChatMessage"),
		SaveVideoRecording(22, "SaveVideoRecording"),
		TakeAndUploadScreenshot(23, "TakeAndUploadScreenshot"),
		GetOrgContacts(24, "GetOrgContacts"),
		GetNonOrgContacts(25, "GetNonOrgContacts"),
		GetOrgGroupContacts(26, "GetOrgGroupContacts"),
		GetGroupAndNonOrgContacts(27, "GetGroupAndNonOrgContacts"),
		GetUnreadCountV3(28, "GetUnreadCountV3"),
		;
		private Integer	id;
		private String	name;

		private IqActionType(final Integer id, final String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum ErrorCode {
		Internal_Server_Error(4001, "internal server error"),
		InvalidTypeOrSubtype(4002, "invalid type or subtype"),
		UnsupportedTypeSubtype(4003, "unsupported type or subtype"),
		InvalidTo(4004, "invalid to"),
		NotInContact(4005, "Recipients {0} are not in contact of user {1}"),
		InvalidText(4006, "invalid Text"),
		toShouldNotBeSelf(4007, "to should not be self"),
		ContactOffline(4008, "contact is offline"),
		InvalidToken(4009, "invalid token"),
		UserMismatch(4010, "User Mismatch. User in input request {0} does not match with logged in user {1}."),
		Entity_Not_Found(4011, "entity not found"),
		Entity_Already_Deleted(4012, "entity already deleted"),
		EventType_Not_Supported(4013, "event type not supported"),
		Event_Invalid_Data(4014, "invalid event data. Need {0} as part of data."),
		Missing_Auth_Token(4015, "Missing_Auth_Token"),
		Missing_Client_Id(4016, "Missing_Client_Id"),
		Unauthorized_Api_Access(4017, "Only Admin user can access this API. Current user {0} is not an Admin of Organization {1}"),
		Missing_TimeStamp(4018, "missing timestamp"),
		API_Request_Expired(4019, "Api request expired"),
		Missing_Api_Key(4020, "missing api key"),
		InvalidApiKey(4021, "invalid api key"),
		Missing_Signature(4022, "missing signature"),
		Action_Not_Supported(4023, "Action not supported"),
		Iqmessage_Invalid_Data(4024, "invalid iqmessage data.Need {0} as part of data."),
		Invalid_Mid(4025, "invalid mid"),
		Invalid_Uuid(4026, "invalid uuid"),
		User_not_found(4027, "user not found"),
		Invalid_iqMessage_Data(4028, "invalid Iq message found"),
		Invalid_Data(4029, "invalid data"),
		Resource_Not_Found(4030, "resource not found"),
		MinLength_Validation_Failed(4031, "received input as {0}, minimum {1} chars needed in search"),
		UserNotInGroup(4032, "User with id {0} is not in group {1}"),
		UserLeftGroup(4033, "User with id {0} has left the group {1}"),
		GroupClosed(4034, "User with id {0} can not send message to group {1} as this group is closed"),
		InvalidGroup(4035, "Group {0} does not exists"),
		Invalid_Enum_Type(4036, "Invalid Enum Type {0} received. Enum type (Class) can not be null."),
		Invalid_Enum_Value(4037, "Invalid enum value {0} for enum type {1}. Possible values are : {2}"),
		Invalid_Memo_Subject_Text(4038, "Invalid Memo input. subject and text can not be null or empty"),
		Invalid_Memo_No_Recipients(4039, "Invalid Memo input. Recipients can not be null or empty if sendToAll flag is false"),
		Invalid_Memo_Subject_Max_Length(4040, "Invalid Memo Subject ,Subject Length cannot be gretaer than {0} characters"),
		Invalid_Memo_Text_Length(4041, "Invalid Memo input. Length of memo text can not be more than {0}"),
		Invalid_Memo_Subject_Content(4042, "Invalid Memo input. subject should not have htmls or ascii characters"),
		Invalid_Memo_Text_Content(4043, "Invalid Memo input. text should not have ascii characters"),
		Invalid_Memo(4044, "Memo with id {0} does not exists"),
		Invalid_XSRF_Token(4045, "invalid headers"),
		Invalid_entity_type_for_cache_refresh(4046, "Invalid entity type {0} for cache refresh"),
		Invalid_Chat_SenderReceipient(4047, "Sender {0} is a {1} and Receiver {2} is a {3}, so sender can't chat with this receiver."),
		Invalid_Memo_Receipient(4048, "User {0} is not Receipient of Memo {0}"),
		User_Org_Mismatch(4049, "Current user {0} is not an User of Organization {1}"),
		Invalid_Memo_Public_Url(4050, "Memo with public url {0} does not exists"),
		Invalid_Memo_Recipients(4051, "Invalid Memo input. Recipients list is not valid"),
		Delete_Message_Not_Allowed(4052, "Current Org {0} is not allowed for delete message"),
		Forbidden(4053, "Forbidden"),
		Invalid_Memo_ChatList(4054, "Invalid Memo chatList"),
        Invalid_Channel_Id(4055,"Channel Not Found"),
        No_Memo_Receipients(4056,"There are no receiptents selected for this Message"),
        Invalid_Channel_IsChatEnable(4057, "isChatEnable flag is false"),
		Invalid_Memo_Id(4058,"Invalid MemoId"),
		Upload_ReadFail(4059, "Bulk Upload Fail Due to  Read Fail"), 
		Empty_File(4060, "Empty file"), 
		Invalid_Options(4061, "Send to Followers Using Either The Excel Upload Feature Or Through Entering Ids Manually Or Send To All Followers,Using More than one of them is Invalid Operation"),
		Invalid_UploadId(4062,"Invalid UploadId"),
		Invalid_Memo_Subject_Min_Length(4063,"Invalid Memo Subject ,Subject Length cannot be less than {0} characters"),
		Invalid_Memo_Text_Max_Length(4064,"Invalid Memo Subject ,Subject Length cannot be greater than {5000} characters"),
		Invalid_Memo_Subject_Length(4065, "Invalid Memo Subject ,Subject Length is not in proper limits"),
		Invalid_Offset(4066, "Invalid Offset...Offset is NotNull and Positive Number"),
		Invalid_Limit(4067, "Invalid Limit...Limit is NotNull and Positive Number"), 
		Validation_Failed(4068,"Validation failed"),
		Bad_Input(4069,"Validation failed"),
		Redis_Connection_Fail(4070, "Redis_Connection_Fail"),
		Failed_toParse_JSONObject(4071, "Failed_toParse_JSONObject"),
		Invalid_Origin(4072, "invalid headers"),
		Invalid_TimeStamp(4073, "invalid timestamp"),

		// Following Error are from OWB Enums.ErrorCode. Don't Modify these values.
		Failed_To_Save_File(2231, "Failed to save recording file"),
		NoSenderRecordingFiles(2284,"No Sender Recording Files present"),
		NoRecipientRecordingFiles(2285,"No Recipient Recording Files present"),
		FFMPEGTimeOutError(2286,"Ffmpeg video operation timed out."),
		FFMPEGProcessingError(2287,"Ffmpeg video processing error."),
		Failed_To_Parse_Json_Obj(2288, "Failed_To_Parse_Json_Obj"),
		Failed_To_Decrypt_Value(2289, "Failed_To_Decrypt_Value"),
		Failed_To_Encrypt_Value(2290, "Failed_To_Encrypt_Value"),
		InvalidTrackingEventType(2291, "InvalidTrackingEventType"),
		Empty_FFMpeg_Command(2295, "Empty_FFMpeg_Command"),
		Invalid_File_Extension(2296, "Invalid_File_Extension"),
		Invalid_Coloumn_Name(2297, "Invalid_Coloumn_Name"),
		Parse_Excel_Failed(2298, "Parse Excel Failed"),
		Invalid_Memo_Dump_AttachmentId(2299,"Invalid memo Dump AttachmentId"),
		Invalid_Memo_Status(2300,"Memo status is not valid"),
        Invalid_Id(2301, "Invalid_Id"),
		BadRequest(4078, "BadRequest"),
		API_RESPONSE_NOT_OK(4079, "HTTP Response not OK"),
		API_IO_ERROR(4080, "IO Error"),
		FAILED_WITH_RETRY(4081, "Retry failed"),
		KYC_STATUS_INVALID(4082, "KYC Status Invalid"),
		FAILED_TO_CREATE_JSON(4083, "Failed to create JSON"),
		Send_Message_Not_Allowed(4084, "Send message not allowed to non admin user, current userId {0}, groupId {1}"),
    	File_Path_Not_Found(4085, "File path Not Found"),
		MEETING_ID_NOT_FOUND(4086, " Meeting Id not Found"),
		AWS_CHIME_MEETING_START_RECORDING_ERROR(4087,"AWS Chime Start Recording providing Empty Response. "),
		AWS_CHIME_MEETING_STOP_RECORDING_ERROR(4088,"AWS Chime Stop Recording Error. "),
		UPDATE_CHIME_MEETING_ID_ERROR(4089,"Not able to update ChimeMeetingId in MeetingInfo Entity."),
		AWS_CHIME_CREATE_MEETING_ERROR(4090,"AWS Chime Create Meeting Error. "),
		AWS_CHIME_CREATE_ATTENDEE_ERROR(4091,"AWS Chime Create Attendee Error. "),
		FAILED_TO_SEND_MESSAGE_ON_QUEUE(4092, "Failed To Send Message On Queue"),
		FAILED_TO_RECEIVE_MESSAGE_FROM_QUEUE(4093, "Failed To Receive Message From Queue"),
		AWS_CHIME_END_MEETING_ERROR(4094,"AWS Chime Delete Meeting Error. "),
		JSON_PARSING_ERROR(4095,"Error during Json Processing Action"),
		MEETING_ALREADY_ENDED(4096,"Meeting has already ended"),
		FAILED_TO_PARSE_EXTERNAL_MEETING_ID(4097,"Failed to process AWS External MeetingId into RMS MeetingId."),
		MEDIACONVERT_PROCESS_FAILED(4098, "Media Convert Job processing Failed."),
		MEDIACONVERT_AUDIO_OUTPUT_PROCESS_FAILED(4099, "Media Convert Audio Output processing Failed."),
		MEDIACONVERT_VIDEO_OUTPUT_PROCESS_FAILED(4100, "Media Convert Video Output processing Failed."),
		FAILED_TO_SAVE_CHIME_MEETING_EVENT(4101, "Failed to save Chime Meeting Event.");



		private Integer	id;
		private String	name;

		private ErrorCode(final Integer id, final String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public static ErrorCode getEnum(Integer errorCode) {
			for (ErrorCode err : ErrorCode.values()) {
				if (err.id.equals(errorCode)) {
					return err;
				}
			}
			return null;
		}
	}

	public enum ConnectionType {
		Web(0),
		Mobile(1);
		private Integer	id;

		private ConnectionType(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	public enum ConnectionStatus {
		Closed(0),
		Open(1);
		private Integer	id;

		private ConnectionStatus(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	public enum ChatMessageType {
		ChatMessage(1),
		SystemMessage(2),
		VideoCallMessage(3),		
		VideoRecording(4),
		ScreenShot(5),
		Location(6),
		ClientEvent(7),
		CustomerDetails(8),
		CustomerWelcomMessage(9),
		CustomerVerifiedDetails(10),
		CustomerFaceComparison(11),
		CustomerKYCStatus(12),
		SentFromRE(13),
		Video(14),
		CustomerIframeMessage(15);
	
				
		private Byte	id;

		private ChatMessageType(Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		public Byte getId() {
			return id;
		}
	}
	
	public enum videoCallType {
		TwoWay(1),
		OneWay(2),
		Audio(3);
		
		private Byte	id;

		private videoCallType(Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		public Byte getId() {
			return id;
		}
		
		public static videoCallType getVideoCallType(Byte id) {
			videoCallType callType = null;
			videoCallType[] types = videoCallType.values();
			for (videoCallType type : types) {
				if (type.getId().byteValue() == id.byteValue()) {
					callType = type;
				}
			}
			if (callType == null) {
				throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Invalid value for videoCallType - " + id);
			}
			return callType;
		}
	}
	
	public enum VideoSignallingType {
		Create(1),
		Join(2),		
		Offser(3),
		Answer(4),
		IceCandidate(5),
		VideoOnOff(6),
		roomFull(7),
		Bye(8),
		NewParticipants(9),
		AudioOnOff(10); 
		
		private Byte	id;

		private VideoSignallingType(Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		public Byte getId() {
			return id;
		}
	}
	
	public enum VideoCallMessageType {
		TwoWayVideoCallRequest(1),
		OneWayVideoCallRequest(2),
		AudioCallRequest(3),
		VideoCallRejected(4),		
        VideoCallIgnored(5), 
        VideoCallAccepted(6), 
        VideoCallEnded(7),
        VideoCallConnected(8),
        VideoCallDisconnected(9),
        VideoCallRating(10);

		private Byte	id;

		private VideoCallMessageType(Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		public Byte getId() {
			return id;
		}
	}	
		
	public enum ACKType {
		One2OneChat(1),
		GroupChat(2),
		ReadACK(3),
		DeleteChatMessage(4);

		private Byte	id;

		private ACKType(Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		private static Map<Integer, ACKType>	map	= null;

		public static ACKType getACKTypeById(Integer id) {
			if (map == null) {
				map = new HashMap<Integer, ACKType>();
				ACKType[] ackTypes = ACKType.values();
				for (ACKType ackType : ackTypes) {
					map.put(ackType.getId().intValue(), ackType);
				}
			}
			return map.get(id);
		}

		public Byte getId() {
			return id;
		}
	}

	public enum NotificationParamName {
		GrpA("GroupAdmin", User.class),
		GrpC("GroupCreator", User.class),
		Grp("Group", Group.class),
		GrpM("GroupMember", GroupMember.class),
		GrpOldName("GroupOldName", String.class),
		GrpNewName("GroupNewName", String.class),
		ActionTaker("ActionTaker", String.class),
		AffectedMember("AffectedMember", String.class),
		Duration("Duration", String.class),
		Receiver("Receiver", String.class),
		AppName("AppName", String.class),	
		OpenChatURL("OpenChatURL", String.class),
		GuestUserLabel("GuestUserLabel",String.class),
		DeviceBrowserInfo("DeviceBrowserInfo",String.class),
		UseCaseStatus("UseCaseStatus",String.class);
		
		private NotificationParamName(String desc, Class type) {

		}
	}

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum ClientType {
		iOS(1, "iOS", "102"),
		Android(2, "Android", "103"),
		Web(3, "Web", "101"),
		Pns(4, "Pns", "104"),
		Ims(5, "Ims", "105"),
		Owb(6, "Owb", "106"),
		DesktopChatApp(7, "DesktopChatApp", "107"),
		AndroidChatApp(8, "AndroidChatApp", "108"),
		iOSChatApp(9, "iOSChatApp", "109"),
		IIS(110, "IIS", "110"),
		OpenChat(111, "OpenChat", "111"),
		WorkAppsUpdate(112, "WorkAppsUpdate", "112");

		private Integer	id;
		private String	name;
		private String	clientId;

		private ClientType(final Integer id, final String name, String clientId) {
			this.id = id;
			this.name = name;
			this.clientId = clientId;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public static boolean isWebClient(String clientId) {
			if (clientId != null && (Web.getClientId().equalsIgnoreCase(clientId) || DesktopChatApp.getClientId().equalsIgnoreCase(clientId)) ||
					OpenChat.getClientId().equalsIgnoreCase(clientId)) {
				return true;
			}
			return false;
		}
	}

	public enum DeploymentType {
		OnPremise,
		Cloud;
	}

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum OrganizationFlavour {
		Employee(1),
		CustomerChat(2),
		OpenChat(3);

		private Integer	id;

		private OrganizationFlavour(final Integer id) {
			this.id = id;
		}

		public void setId(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum FileUploadType {
		UserMemo(1, "UserMemo"),
		CustomMemo(2, "CustomMemo");
		
		private Integer	id;
		private String	name;

		private FileUploadType(final Integer id, final String name) {
			this.id = id;
			this.name = name;
			
		}

		public void setId(final Integer id) {
			this.id = id;
		}

		public Byte getId() {
			return id.byteValue();
		}
		
		public String getName() {
			return this.name;
		}
	
		public void setName(final String name) {
			this.name = name;
		}

	}
	

	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum MemoStatus {
		Initiated(1, "Initiated"),
		ReadyToProcess(2, "ReadyToProcess"),
		InProcess(3, "InProcess"),
		Processed(4, "Processed");
		
		private Integer	id;
		private String	name;

		private MemoStatus(final Integer id, final String name) {
			this.id = id;
			this.name = name;			
		}

		public void setId(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}
		
		public String getName() {
			return this.name;
		}
	
		public void setName(final String name) {
			this.name = name;
		}
	}
	
	
	

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public static enum RecordingStage {
		Started(1),	
		Stopped(2),
		Discarded(3),
		Processed(4),
		Failed(5),
		ReprocessKyc(10);

		private Byte	id;

		private RecordingStage(final Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		public Byte getId() {
			return id;
		}
	}
	
	public enum UserType {
		Email(0, "Email"),
		Mobile(1, "Mobile"),
		All(2, "All"),
		EmailGuest(3, "EmailGuest"),
		ChatGuest(4, "ChatGuest");
		private Integer	id;
		private String	name;

		private UserType(final Integer id, final String name) {
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
	}
	
	public enum RecordingType {
		TwoWayVideoRecording(1, "TwoWayVideoRecording"),
		TwoWayAudioRecording(2, "TwoWayAudioRecording"),
		// OneWayVideoRecording(3, "OneWayVideoRecording"), // DEPRECATED : DO NOT REMOVE
		// OneWayVideoTwoWayAudioRecording(4, "OneWayVideoTwoWayAudioRecording"), // DEPRECATED : DO NOT REMOVE
		OneWayVideoGuestRecording(5, "OneWayVideoGuestRecording"),
		OneWayVideoRURecording(6, "OneWayVideoGuestRecording");
				
		private Integer	id;
		private String	name;

		private RecordingType(final Integer id, final String name) {
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
		
		public static RecordingType recordingType(Integer id) {
			RecordingType recordingType = null;
			RecordingType[] types = RecordingType.values();
			for (RecordingType type : types) {
				if (type.getId().intValue() == id.intValue()) {
					recordingType = type;
				}
			}
			if (recordingType == null) {
				throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Invalid value for RecordingType - " + id);
			}
			return recordingType;
		}
	}
	
	public enum InfoSubType {
		NetworkUsageOnetoOne(0),
	    NetworkUsageGroup(1),
		DeviceOrientationOnetoOne(2),
		DeviceOrientationGroup(3);
		private Integer	id;

		private InfoSubType(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
	
	public enum RecordingStopEventTriggerBy {
		User(1),
	    System(2);
		private Integer	id;

		private RecordingStopEventTriggerBy(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	public static <E extends Enum<E>> E valueOf(final Class<E> enumType, String enumVal) {
		// TODO : remove performance hit :: Try having static Map<K,Enum> to
		if (enumType == null) {
			throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Enum Type is passed null.");
		}
		if (enumVal == null) {
			throw new BadRequestException(ErrorCode.Invalid_Enum_Value, "Null enum value.");
		}
		for (E e : enumType.getEnumConstants()) {
			if (e.name().equalsIgnoreCase(enumVal.trim())) {
				return e;
			}
		}
		throw new BadRequestException(ErrorCode.Invalid_Enum_Value, "Enum Type : " + enumType.getSimpleName() + ", Passed invalid enum value - " + enumVal);
	}
	
	public enum PushNotificationVisibility implements IdEnum<Byte, PushNotificationVisibility>{
		All((byte)1),
		Recipients((byte)2),
		Sender((byte)3);
				
		private Byte	id;

		private PushNotificationVisibility(byte id) {
			this.id = id;
		}

		public Byte getId() {
			return id;
		}
	}
	
	
    public enum ChannelType {
		Public(1),
		Private(2);
    	
    	private Integer id;
    	
		private ChannelType(Integer id) {
			this.id = Integer.valueOf(id.toString());
		}
		
		public Integer getId() {
			return id;
		}
    }  
    
   
	public enum GroupUseCaseStatus {
		Open(1, "Open"),
		Successful(2, "Successful"),
		Incomplete(3, "Incomplete"),
		Unable(4, "Unable"),
		Rejected (5, "Rejected");

		private Integer	id;
		private String	name;

		private GroupUseCaseStatus(final Integer id, final String name) {
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

		public static GroupUseCaseStatus getGroupUseCaseStatus(Byte groupUseCaseStatusId) {
			GroupUseCaseStatus groupUseCaseStatus = null;
			GroupUseCaseStatus[] statuses = GroupUseCaseStatus.values();
			for (GroupUseCaseStatus status : statuses) {
				if (status.getId().byteValue() == groupUseCaseStatusId) {
					groupUseCaseStatus = status;
				}
			}
			
			return groupUseCaseStatus;
		}
	}
    
	public enum GroupType {
		GroupChat(1, "GroupChat"),
		ADGroup(2, "ADGroup"),
		GuestDirectGroupChat(3, "GuestDirectGroupChat"),
		GuestRoundRobinGroupChat(4, "GuestRoundRobinGroupChat"),
		GuestGroupChat(5, "GuestGroupChat"),
		ConsumerDirectGroupChat(6, "ConsumerDirectGroupChat"),	
		ConsumerGroupChat(7, "ConsumerGroupChat"),
		VideoKycGuestGroupChat(8, "VideoKycGuestGroupChat");


		private Integer	id;
		private String	name;

		private GroupType(final Integer id, final String name) {
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

		public static GroupType getGroupType(Byte groupTypeId) {
			GroupType groupType = null;
			GroupType[] types = GroupType.values();
			for (GroupType type : types) {
				if (type.getId().byteValue() == groupTypeId) {
					groupType = type;
				}
			}
			if (groupType == null) {
				throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Invalid value foe GroupType - " + groupTypeId);
			}
			return groupType;
		}
	}
  
	public enum UseCase {
		VideoKyc(1);
		
		private Byte id;

		private UseCase(Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		public Byte getId() {
			return id;
		}
		public static UseCase getEnum(Byte b) {
			UseCase types[] = UseCase.values();
			UseCase eType = null;
			for (UseCase type:types) {
				if (type.getId().equals(b)) {
					eType = type;
					break;
				}
			}
			return eType;
		}
	}
	public enum ChannelMessageSendingOptions{
		SendToSpecificIds(0),
		SendToAll(1);
    	
    	private Byte id;
    	
		private ChannelMessageSendingOptions(Integer id) {
			this.id = Byte.valueOf(id.toString());
		}
		
		public Byte getId() {
			return id;
		}
	}
	public enum VideoCallStatus {
		Created(1, "Created"),
		Joined(2, "Joined"),
		NewParticipants(3, "NewParticipants"),
		Accepted(4, "Accepted"),
        Rejected(5, "Rejected"),
        Connected(6, "Connected"),
        Disconnected(7, "Disconnected"),
        Ended(8, "Ended"),
        Missed(9, "Missed");
    	
		private Integer	id;
		private String	name;

		private VideoCallStatus(final Integer id, final String name) {
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
	}
	public enum ChimeMeetingStatus {
        Created(1, "Created"),
        Started(2, "Started"),
        Stopped(3, "Stopped");
        
        private Integer id;
        private String  name;

        private ChimeMeetingStatus(final Integer id, final String name) {
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
    }

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public static enum ChimeRecordingStage {
		Created(1),
		Starting(2),
		Started(3),
		Stopping(4),
		Stopped(5),
		ProcessingStarted(6),
		ProcessingInprogress(7),
		Processed(8),
		ProcessingFailed(9);

		private Byte id;

		private ChimeRecordingStage(final Integer id) {
			this.id = Byte.valueOf(id.toString());
		}

		public Byte getId() {
			return id;
		}
	}

	public enum MeetingAttendeeStatus  {
		Joining(1, "Joining"),
		Joined(2, "Joined"),
		Left(3, "Left");

		private Integer id;
		private String  name;

		private MeetingAttendeeStatus (final Integer id, final String name) {
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
	}

	public enum VideKycCallWaitEvent {
		AddAgent(1, "AgentAvailable"),
		RemoveAgent(2, "CustomerJoined"),
		AddCustomer(3, "AgentAssigned"),
		RemoveCustomer(4, "AuditorReady");
    	
		private Integer	id;
		private String	name;

		private VideKycCallWaitEvent(final Integer id, final String name) {
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
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum RoleName {		
		Admin(1, "Admin"),
		User(2, "User"),
		DevOps(3, "DevOps"),
		BizOps(4, "BizOps"),
		PartnerAdmin(5, "PartnerAdmin"),
		Broadcaster(6, "Broadcaster"),
		VideoKYCAgent(7, "VideoKYCAgent"),
		VideoKYCAuditor(8, "VideoKYCAuditor"),
		VideoKYCAdmin(9,"VideoKYCAdmin"),
		VideoKYCMonitor(10, "VideoKYCMonitor"),
		UAMManager(11, "UAMManager");
		private Integer							id;
		private String							name;

		private static Map<Integer, RoleName>	roles	= new HashMap<Integer, RoleName>();

		private RoleName(final Integer id, final String name) {
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

		public static RoleName getRole(Integer id) {
			if (roles.isEmpty()) {
				RoleName[] roleNames = RoleName.values();
				for (RoleName roleName : roleNames) {
					roles.put(roleName.id.intValue(), roleName);
				}
			}
			return roles.get(id);
		}
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum UserCategory {
		Employee(1, "Employee"),
		Customer(2, "Customer"),
		Guest(3, "Guest"),
		Consumer(4, "Consumer"),
		Partner(5, "Partner"),
		VideoKycCustomer(6, "VideoKycCustomer");

		private Integer	id;
		private String name;

		private UserCategory(final Integer id, final String name) {
			this.id = id;
			this.name=name;
		}

		public void setId(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}		
		

		public final String getName() {
			return name;
		}

		public final void setName(String name) {
			this.name = name;
		}

		public static List<Integer> getCategoryIds(String categoryStrings) {
			List<Integer> categoryIds = null;
			if (categoryStrings != null && !categoryStrings.isEmpty()) {
				categoryIds = new ArrayList<Integer>();
				String[] categories = categoryStrings.split(",");
				for (String category : categories) {
					UserCategory userCategory = Enums.valueOf(UserCategory.class, category);
					categoryIds.add(userCategory.getId());
				}
			}
			return categoryIds;
		}
		
		public static UserCategory getUserCategory(Integer id) {
			UserCategory[] values = UserCategory.values();
			for (UserCategory category : values) {
				if (category.getId().intValue() == id.intValue()) {
					return category;
				}
			}
			return null;
		}
	}
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum VideokycAgentStatus {		
		Available(1, "Available"),
		Busy(2, "Busy"),
		NotAvailable(3, "NotAvailable");
		private Integer							id;
		private String							name;
		

		private VideokycAgentStatus(final Integer id, final String name) {
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
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum CustomerPriority {
		Repeat(0, "Repeat"),
		High(1, "High"),
		Normal(2, "Normal");
		

		private Byte id;
		private String name;

		private CustomerPriority(Integer id , String name) {
			this.id = Byte.valueOf(id.toString());
			this.name = name;
		}

		public Byte getId() {
			return id;
		}

		public String getName() {
			return name;
		}	
		public static CustomerPriority getEnum(Byte b) {
			CustomerPriority types[] = CustomerPriority.values();
			CustomerPriority eType = null;
			for (CustomerPriority type:types) {
				if (type.getId().equals(b)) {
					eType = type;
					break;
				}
			}
			return eType;
		}
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum VideoKYCFields {
		AadhaarVerificationDate(1, "AadhaarVerificationDate", "Aadhaar Verification Date","2020-02-08"),
		PANNumber(2, "PANNumber", "PAN Number", "AGKPB6745N"),
		FormSubmissionDate(3, "FormSubmissionDate", "Form Submission Date","2020-02-08"),
		NameAsPerNSDL(4, "NameAsPerNSDL", "Name As Per NSDL","John"),
		NameAsPerPanCard(5, "NameAsPerPanCard", "Name As Per Pan Card","John"),
		NameAsPerAadharCard(6, "NameAsPerAadharCard", "Name As Per Aadhar Card","John"),
		NameAsPerNSDLAPI(7,"NameAsPerNSDLAPI", "Name As Per NSDLAPI", "John"),
		PanNameAsPerNSDLAPI(8,"PanNameAsPerNSDLAPI", "PanName As Per NSDLAPI", "John"),
		AadhaarAddressLine1(9, "AadhaarAddressLine1", "Aadhaar Address Line1", "survey no 6/2"),
		AadhaarAddressLine2(10, "AadhaarAddressLine2", "Aadhaar Address Line2", "Radhanagari Apartment"),
		AadhaarAddressLine3(11, "AadhaarAddressLine3", "Aadhaar Address Line3", "Pimple Saudagar"),
		AadhaarAddressPincode(12, "AadhaarAddressPincode", "Aadhaar Address Pincode", "411027"),
		AadhaarAddressCity(13, "AadhaarAddressCity", "Aadhaar Address City", "Pune"),
		AadhaarAddressState(14, "AadhaarAddressState", "Aadhaar Address State", "Maharashtra"),
		AadhaarAddressCountry(15, "AadhaarAddressCountry", "Aadhaar Address Country", "India"),
		CommunicationAddressLine1(16, "CommunicationAddressLine1", "Communication Address Line1", "survey no 6/2"),
		CommunicationAddressLine2(17, "CommunicationAddressLine2", "Communication Address Line2", "Radhanagari Apartment"),
		CommunicationAddressLine3(18, "CommunicationAddressLine3", "Communication Address Line3", "Pimple Saudagar"),
		CommunicationAddressPinCode(19, "CommunicationAddressPinCode", "Communication Address PinCode", "411027"),
		CommunicationAddressCity(20, "CommunicationAddressCity", "Communication Address City", "Pune"),
		CommunicationAddressState(21, "CommunicationAddressState", "Communication Address State", "Maharashtra"),
		CommunicationAddressCountry(22, "CommunicationAddressCountry", "Communication Address Country","India"),
		CommunicationAddressFlag(23, "CommunicationAddressFlag", "Communication Address Flag", "true"),
		LatLong(24, "LatLong", "Lat Long", "38.8951,-77.0364"),
		LatLongCity(25, "LatLongCity", "Lat Long City", "Pune"),
		LatLongState(26, "LatLongState", "Lat Long State", "Maharashtra"),
		LatLongCountry(27, "LatLongCountry", "Lat Long Country", "India"),
		FathersName(28, "FathersName", "Fathers Name", "Dan"),
		DateOfBirth(29, "DateOfBirth", "Date Of Birth", "1977-02-08"),
		CustomerMobileNumber(30, "CustomerMobileNumber", "Customer Mobile Number", "8756452345"),
		AadhaarMobileNumber(31, "AadhaarMobileNumber", "Aadhaar Mobile Number", "5634256678"),
		EmailID(32,"EmailID", "Email ID", "john@gmail.com"),
		MaritalStatus(33, "MaritalStatus", "Marital Status", "Married"),
		NomineeGivenFlag(34,"NomineeGivenFlag", "Nominee Given Flag", "true"),
		MothersName(35, "MothersName", "Mothers Name", "Daina"),
		Gender(36, "Gender", "Gender", "Male"),
		PlaceofBirth(37, "PlaceofBirth", "Place Of Birth", "Pune"),
		Occupation(38, "Occupation", "Occupation", "Enginner"),
		AnnualIncome(39, "AnnualIncome", "Annual Income", "0.0"),
		OtherInfo(40, "OtherInfo", "Other Info", "Other Info"),
		CustomerId(41, "CustomerId", "Customer Id", "MN1234"),		
		AccountNumber(42, "AccountNumber", "Account Number", "acc1234"),
		AccountRegularizationStatusCode(43, "AccountRegularizationStatusCode", "Account Regularization Status Code", "acc1234"),
		AadhaarNameSameAsNsdlName(44, "AadhaarNameSameAsNsdlName", "AadhaarName Same As NsdlName", "true"),
		AadhaarNameSameAsPanName(45, "AadhaarNameSameAsPanName", "AadhaarName Same As PanName", "true"),
		CommunicationAddressSameAsAadhaarAddress(46, "CommunicationAddressSameAsAadhaarAddress", "Communication Address Same As Addhar Address", "true"),
		CommunicationAddress(47, "CommunicationAddress", "Communication Address", "true"),
		AadhaarAddress(48, "AadhaarAddress", "Aaddhar Address", "true"),
		NatureofBusiness(49, "NatureofBusiness", "Nature Of Business", "IT"),
		EmployeeCode(50, "EmployeeCode", "Employee Code", "EA21"),
		SourceOfIncome(51, "SourceOfIncome", "Source of Income", "Job"),
		CorporateName(52, "CorporateName", "Corporate Name", "Corporate"),
		PassportNumber(53, "PassportNumber", "Passport Number", "Es12345678"),
		PANFaceMatchWithCustomer(54,"PANFaceMatchWithCustomer","PAN Face Match With Customer","True"),
		CompanyNumberOfDirectors(55,"CompanyNumberOfDirectors","Company Number Of Directors","3"),
		CompanyDirectorIsPrimary(56,"CompanyDirectorIsPrimary","Company Director Is Primary","true"),
		CompanyApplicationNumber(57,"CompanyApplicationNumber","Company Application Number","CR100012E"),
		CompanyType(58,"CompanyType","Company Type","PSU"),
		CompanyIdentificationNumber(59,"CompanyIdentificationNumber","Company Identification Number","CR100012E"),
		CompanyName(60,"CompanyName","Company Name","XYZ Inc Pvt. Ltd"),
		CompanyDateofIncorporation(60,"CompanyDateofIncorporation","Company Date of Incorporation","1977-02-08"),
		CompanyEmployeeStrength(61,"CompnayEmployeeStrength","Company Employee Strength","50"),
		CompanyPanNumber(62,"CompanyPanNumber","Compnay Employee Strength","AGKPB6745N"),
		CompanyGSTINNumber(63,"CompanyGSTINNumber","Company GST IN Number","AGKPB6745N"),
		CompanyRegisteredAddress(64,"CompanyRegisteredAddress","Company Registered Address","10th Street, Down Lane, Mumbai"),
		CompanyCommunicationAddress(65,"CompanyCommunicationAddress","Company Communication Address","10th Street, Down Lane, Mumbai"),
		CompanyNatureofBusiness(66,"CompanyNatureofBusiness","Company Nature of Business","TeleCommunication"),
		CompanyAnnualTurnover(67,"CompanyAnnualTurnover","Company Annual Turnover","10000000000"),
		CompanyTypeofOffice(68,"CompanyTypeofOffice","Company Type of Office","Rental"),
		CompanyBusinessActivelyOperatedFrom(69,"CompanyBusinessActivelyOperatedFrom","Company Business Actively Operated From","Mumbai"),
		CompanyForeignNationalDirectors(70,"CompanyForeignNationalDirectors","Company Foreign National Directors","John"),
		CompanyGSTINRegistrationState(71,"CompanyGSTINRegistrationState","Company GSTIN Registration State","Maharashtra"),
		BusinessAddressPinCode(72,"BusinessAddressPinCode","Business Address PinCode","110016"),
		GSTINPDFLink(73,"GSTINPDFLink", "GSTIN PDF Link","https://icicibank.com/gstin.pdf"),
		ITRPDFLink(74,"ITRPDFLink", "GSTIN PDF Link","https://icicibank.com/itr.pdf"),
		NovaFormLink(75,"NovaFormLink","Nova Form Link","https://icicibank.com/itr.pdf"),
		Source(76, "Source", "Source", "Source");

		private Byte id;
		private String name;
		private String displayName;
		private String dummyData;

		private VideoKYCFields(Integer id, String name, String displayName, String dummyData) {
			this.id = Byte.valueOf(id.toString());
			this.name = name;
			this.displayName = displayName;
			this.dummyData = dummyData;
		}

		public Byte getId() {
			return id;
		}

		public String getName() {
			return name;
		}
		
		public String getDisplayName() {
			return displayName;
		}

		public String getDummyData() {
			return dummyData;
		}

		public static VideoKYCFields getEnum(Byte b) {
			VideoKYCFields types[] = VideoKYCFields.values();
			VideoKYCFields eType = null;
			for (VideoKYCFields type:types) {
				if (type.getId().equals(b)) {
					eType = type;
					break;
				}
			}
			if (eType == null) {
				throw new BadRequestException(ErrorCode.Invalid_Enum_Value, "Invalid id value for VideoKYCFields - " + b);
			}
			return eType;
		}
		
		public static VideoKYCFields getVideoKYCFields(String name) {
			VideoKYCFields types[] = VideoKYCFields.values();
			VideoKYCFields eType = null;
			for (VideoKYCFields type:types) {
				if (type.getName().equalsIgnoreCase(name.trim())) {
					eType = type;
					break;
				}
			}
			
			return eType;
		}
	}
	
	public enum AttachmentType {
		UploadedFile(0, "uploadedFile"), Screenshot(1, "screenshot"), VideoRecording(2, "videoRecording"),
		TempVideoRecording(3, "tempVideoRecording"),SentFromRE(4, "sentFromRE");
		private Integer id;
		private String name;

		private AttachmentType(final Integer id, final String name) {
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
	}


	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum SupportedKmsDataEncryptionKey {
		AwsKmsDataEncryptionKey(1, "AwsKmsDataEncryptionKey"),
		AzureKmsDataEncryptionKey(2, "AzureKmsDataEncryptionKey");


		private Byte id;
		private String name;

		private SupportedKmsDataEncryptionKey(Integer id, String name) {
			this.id = Byte.valueOf(id.toString());
			this.name = name;
		}

		public Byte getId() {
			return id;
		}

		public String getName() {
			return name;
		}


	}

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum VideoKYCStatus {
		Open(1, "Open"),
		Waiting(2, "Waiting"),
		AgentAssigned(3, "AgentAssigned"),
		Successful(4, "Successful"),
		Rejected(5, "Rejected"),
		Unable(6, "Unable"),
		AuditorReady(7, "AuditorReady"),
		AuditorAssigned(8, "AuditorAssigned"),		
		Approved(9, "Approved"),
		NotApproved(10, "NotApproved");

		private Byte id;
		private String name;

		private VideoKYCStatus(Integer id , String name) {
			this.id = Byte.valueOf(id.toString());
			this.name = name;
		}

		public Byte getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public static VideoKYCStatus getEnum(String name) {
			VideoKYCStatus types[] = VideoKYCStatus.values();
			VideoKYCStatus eType = null;
			for (VideoKYCStatus type:types) {
				if (type.getName().equalsIgnoreCase(name)) {
					eType = type;
					break;
				}
			}
			return eType;
		}

		public static String getName(Byte b) {
			VideoKYCStatus[] types = VideoKYCStatus.values();	
			for (VideoKYCStatus type : types) {
				if (type.getId().equals(b)) {
					return type.getName();
				}
			}
			return null;
		}
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum TrackingEvent {		
		Meeting(1, "Meeting"),
		User(2, "User");
		
		private Integer							id;
		private String							name;
		

		private TrackingEvent(final Integer id, final String name) {
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
		
		
		public static TrackingEvent getEnum(Byte b) {
			TrackingEvent[] types = TrackingEvent.values();	
			TrackingEvent type = null;
			for (TrackingEvent type1 : types) {
				if (type1.getId().byteValue() == b) {
				   type = type1;
				   break;
				}
			}
			if (type == null) {
				throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Invalid value for TrackingEvent - " + b);
			}
			
			return type;
		}
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum GroupMemberRole {
		Admin(1, "Admin"),
		Member(2, "Member");

		private Integer	id;
		private String	name;

		private GroupMemberRole(final Integer id, final String name) {
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


		public static GroupMemberRole getGroupMemberRole(String name) {
			GroupMemberRole memberRole = null;
			GroupMemberRole[] roles = GroupMemberRole.values();
			for (GroupMemberRole role : roles) {
				if (role.getName().contentEquals(name)) {
					memberRole = role;
					break;
				}
			}
			if (memberRole == null) {
				throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Invalid member enum type");
			}
			return memberRole;
		}
	}

	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum EventTrackingSource {		
		Server(1, "Server"),
		UI(2, "UI");
		
		private Integer							id;
		private String							name;
		

		private EventTrackingSource(final Integer id, final String name) {
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
	}

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum UserInteractionType {		
		User(1, "User"),
		Meeting(2, "Meeting");
		
		private Byte							id;
		private String							name;
		

		private UserInteractionType(final Integer id, final String name) {
			this.id = id.byteValue();
			this.name = name;
		}

		public Byte getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		public void setId(final Byte id) {
			this.id = id;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum MeetingEventType implements IdNameEnum<Integer, MeetingEventType> {
		Start(1, "Start"), // server
		CustomerInvite(2, "CustomerInvite"), // server
		CustomerInviteReceived(3, "CustomerInviteReceived"), // client
		CustomerAcceptButtonClicked(4, "AcceptButtonClicked"), // client
		Accepted(5, "Accepted"), // I
		Rejected(6, "Rejected"), // server
		Ignored(7, "Ignored"), // server
		Join(8, "Join"), // server
		Offer(9, "Offer"), // server
		Answer(10, "Answer"), // server
		IceCandidate(11, "IceCandidate"), //server
		NewParticipants(12, "NewParticipants"), //server
		Connected(13, "Connected"), 
		SwitchCamera(14, "SwitchCamera"), 
		ScreenshotRequestSent(15, "ScreenshotRequestSent"), // in data, add type of screenshot, UserFace, Pancard
		ScreenshotRequestReceived(16, "ScreenshotRequestReceived"), // in data, add type of screenshot, UserFace, Pancard
		ScreenshotTaken(17, "ScreenshotTaken"), // in data, add type of screenshot, UserFace, Pancard
		Disconnected(18, "Disconnected"),
		Failed(19, "Failed"),		
		Ended(20, "Ended"),	
		Bye(21, "Bye"),
		ClientErros(22,"ClientErros"),
		ConnectionClosed(23,"ConnectionClosed"),        
		CustomerSocketNotAvailable(24,"CustomerSocketNotAvailable"),
		CustomerInviteLocalOutActor(25,"CustomerInviteLocalOutActor"),
		CustomerInvitePublished(26,"CustomerInvitePublished"),
		CustomerInviteSubscriberReceived(27,"CustomerInviteSubscriberReceived"),
		CustomerInviteRemoteOutActor(28,"CustomerInviteRemoteOutActor"),
		CustomerWsInviteReceived(29,"CustomerWsInviteReceived"), // UI
		CustomerMediaCaptured(30,"CustomerMediaCaptured"),		
		ScreenshotMessageInServer(31, "ScreenshotMessageInServer"), // Server
		ScreenshotReceivediInChat(32, "ScreenshotReceivediInChat"),
		NoRecordingCallStopped(33, "NoRecordingCallStopped"),
		ShareScreen(34, "ShareScreen"),		
		CustomerAcceptButtonClickedText(35,"CustomerAcceptButtonClickedText"),
		CustomerAcceptButtonClickedPopup(36,"CustomerAcceptButtonClickedPopup"),
		CustomerAcceptButtonClickedAuto(37,"CustomerAcceptButtonClickedAuto"), // UI
		AgentJoined(38,"AgentJoined"),		
		CustomerJoined(39, "CustomerJoined"), // Server
		CustomerJoinedButtonClick(40, "CustomerJoinedButtonClick"),
		AgentJoinedButtonClick(41, "AgentJoinedButtonClick"),
		KYCStatusChanged(42, "KYCStatusChanged"),
		AgentCustomerConnected(43, "AgentCustomerConnected");
		
		
		private Integer	id;
		private String name;
		

		private MeetingEventType(final Integer id, final String name) {
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
		
		public static MeetingEventType getEnum(Byte b) {
			MeetingEventType[] types = MeetingEventType.values();	
			MeetingEventType type = null;
			for (MeetingEventType type1 : types) {
				if (type1.getId().byteValue() == b) {
				   type = type1;
				   break;
				}
			}
			if (type == null) {
				throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Invalid value for MeetingEventType - " + b);
			}
			
			return type;
		}
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum UserEventType implements IdNameEnum<Integer, UserEventType>{
		WaitPageLoadInitiated(1, "CustomerPageLoad"), 
		Logout(2, "Logout"),
		ExchangeTokenDone(3, "ExchangeTokenDone"), // client
		BrowserInfo(4,"BrowserInfo"),
		CallWaitCopyLinkUsed(5,"CallWaitCopyLinkUsed"),
		OnInstructionPage(6, "OnInstructionPage"), // client
		ProceedClicked(7, "ProceedClicked"), // client
		CallWaitError(8, "CallWaitError"), // client
		CallWaitBeforeSocketConnection(9, "CallWaitBeforeSocketConnection"), 
		CallWaitConnected(10, "CallWaitConnected"), 
		CustomerAgentAssigned(11, "CustomerAgentAssigned"), // client
		AgentCustomerAssigned(12, "AgentCustomerAssigned"), // client;
		CallWaitDisConnected(13, "CallWaitDisConnected"), // client
		CustomerChatPageLoaded(14, "CustomerChatPageLoaded"), // client
		ChatConnected(15, "ChatConnected"), // server
		ChatDisconnected(16, "ChatDisconnected"), // server
		InChatFocus(17, "InChatFocus"), // client
		InChatOutFocus(18, "InChatOutFocus"), // client		
		MainChatWindowVideoButtonClicked(19, "MainChatWindowVideoButtonClicked"), // agent video clicked
		AgentVideoCallingPageLoad(20, "AgentVideoCallingPageLoad"), // agent video clicked
		ClientErros(21,"ClientErros"),
		CustomerInChatTab(22,"CustomerInChatTab"),
		CustomerInVideoTab(23,"CustomerInVideoTab"),
		CustomerSessionExpired(24,"CustomerSessionExpired"),
		CustomerRedirected(25,"CustomerRedirected"),
        CustomerSessionExpiredServer(26,"CustomerSessionExpiredServer"),
		ServerError(27,"ServerError"),
		NotSupportedBrowserPage(28, "NotSupportedBrowserPage"),
        TurnServerConnectivityFailed(29, "TurnServerConnectivityFailed"),
        WaitPageLoadStart(30, "WaitPageLoadStart"),
        WaitPageLoadLoaded(31, "WaitPageLoadLoaded"),
        CameraError(32, "CameraError"),
        CookieDisabled(33, "CookieDisabled"),
        VideoKYCStatusChange(34, "VideoKYCStatusChange");
		
		
		private Integer	id;
		private String name;
		

		private UserEventType(final Integer id, final String name) {
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
		
		public static UserEventType getEnum(Byte b) {
			UserEventType[] types = UserEventType.values();	
			UserEventType type = null;
			for (UserEventType type1 : types) {
				if (type1.getId().byteValue() == b) {
				   type = type1;
				   break;
				}
			}
			if (type == null) {
				logger.info("wrong event type "+b);
				throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Invalid value for UserEventType - " + b);
			}
			
			return type;
		}	
	}
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public static enum RecordingMethod{
		Regular(1,"Regular"), Janus(2, "Janus"), AwsEcsTaskRecording(3,"AwsEcsTaskRecording");
		
		private String name;
		private Byte id;

		private RecordingMethod(Integer id, String name) {
			this.id = Byte.valueOf(id.toString());
			this.name = name;
		}
		
		public Byte getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}

		public static RecordingMethod getRecordingMethod(Byte id) {
			RecordingMethod method = null;
			RecordingMethod[] types = RecordingMethod.values();
			for (RecordingMethod type : types) {
				if (type.getId().byteValue() == id.byteValue()) {
					method = type;
				}
			}
			if (method == null) {
				throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Invalid value foe RecordingMethod - " + id);
			}
			return method;
		}
	}

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum SessionExpiryReason{
		Unknown(1,"Unknown"),
		OpenKycNotAvailable(2, "OpenKycNotAvailable"),
		ActiveKycNotAvailable(3, "ActiveKycNotAvailable"),
		KycClosed(3, "KycClosed"),
		TokenNotValidated(4, "TokenNotValidated"),
		KycDeleted(5, "KycDeleted"),
		GroupUseCaseStatusChanged(6, "GroupUseCaseStatusChanged");

		private final String name;
		private final Byte id;

		SessionExpiryReason(Integer id, String name) {
			this.id = Byte.valueOf(id.toString());
			this.name = name;
		}

		public Byte getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum RecordingProcessingStatus implements IdNameEnum<Integer, RecordingProcessingStatus> {

		InReProcess(1, "InReProcess"),
		AllPassed(2, "AllPassed"),
		AtLeastOnePassed(3, "AtLeastOnePassed"),
		FileError(4, "FileError"),
		ProcessingError(5, "ProcessingError"),
		OtherError(6, "OtherError"),
		MaxReprocessingExceeded(7, "MaxReprocessingExceeded"),
		NonKycGroup(8, "NonKycGroup"),
		AgentStatusNotSuccessful(9, "AgentStatusNotSuccessful"),
		StatusNotAuditorAssignedOrReady(10, "StatusNotAuditorAssignedOrReady"),
		NonReprocessRole(11, "NonReprocessRole"),
		OldKyc(12, "OldKyc");
		private final String name;
		private final Integer id;
		RecordingProcessingStatus(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}