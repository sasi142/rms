package core.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.dto.MemoDto;
import core.entities.Attachment;
import core.entities.Memo;
import play.libs.Json;

@Component
public class SeoUtil implements InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(SeoUtil.class);
	
	@Autowired
	private Environment env;
	private Integer msgHeadingMaxSize;
	private Integer msgDescMaxSize;
	private String hostName;
	private String photoImageBaseUrl;
	private String attachmentImageBaseUrl;
	private String attachmentImageType;
	private String attachmentTypeMultipart;
	private String charset;

	public void setSeoTagsForFirstAttachment(MemoDto memoDto, Map<String, Object> data) {
		List<Attachment> attachments = memoDto.getAttachments();
		String firstImageUrl = null;
		if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
			logger.debug("Number of attachments in ChannelMessage: " + attachments.size());
			for (Attachment att : attachments) {
				logger.debug("attachmentId:" + att.getId());				
				if (att.getContentType().equalsIgnoreCase(attachmentImageType) || att.getContentType().contains(attachmentImageType)) {
					firstImageUrl = hostName + attachmentImageBaseUrl + att.getUuid();
				} else if (att.getContentType().equals(attachmentTypeMultipart)) {
					String fileExt = FilenameUtils.getExtension(att.getFileName()).toLowerCase();
					String[] imageFileExtenstions = CommonUtil.getImageFileExtension();
					for (String extension : imageFileExtenstions) {
						if (fileExt.equals(extension)) {
							firstImageUrl = hostName + attachmentImageBaseUrl + att.getUuid();
							break;
						}
					}
				}
				if (Objects.nonNull(firstImageUrl)) {
					logger.debug("Got firstImage Attachment with Id:" + att.getId() + " and firstImageUrl:"
							+ firstImageUrl);
					break;
				}
			}
		} else {
			logger.debug("attachments are null...there are no attachments for this message");
		}
		data.put("ScreenshotURL", firstImageUrl);
	}

	public void setSeoTagsForChannelCategoryImageLogoPublicUrl(Memo memo,Map<String, Object> data,String channelName) {
		String channelCategory = memo.getChannelCategory();
		channelCategory = StringUtils.hasText(channelCategory) ? channelCategory : null;
		logger.debug("channelCategory: " + channelCategory);		
		data.put("ChannelCategory", channelCategory);
		String channelLogoUrl = null;
		String iconUrl = memo.getIconUrl();
		channelLogoUrl = Objects.isNull(iconUrl) ? channelLogoUrl : iconUrl;// uuid.jpg
		logger.debug("channelLogoUrl UUID.jpg: " + channelLogoUrl);

		if (StringUtils.hasText(channelLogoUrl)) {
			JsonNode photos = Json.parse(channelLogoUrl);
			logger.info("extracting channelProfilePhoto from channelLogoUrl json");
			channelLogoUrl = photos.findPath("profile").asText();
			logger.debug("Profile url:" + channelLogoUrl);
			channelLogoUrl = hostName + photoImageBaseUrl + channelLogoUrl;
		}
		// for meta tags,assign null if proper values not present...only ..so meta tags
		// disappear,if proper value not present..else for non-meta tags assign "" if
		// proper value not present
		channelLogoUrl = StringUtils.hasText(channelLogoUrl) ? channelLogoUrl : null;
		data.put("LogoImageURL", channelLogoUrl);
		
		channelLogoUrl = StringUtils.hasText(channelLogoUrl) ? channelLogoUrl : "";
		data.put("LogoURL", channelLogoUrl);
		
		String channelPublicUrl = "";
		Integer channelId = memo.getChannelId();
		if (Objects.nonNull(channelId) && Objects.nonNull(channelName)) {
			try {
				channelCategory = URLEncoder.encode(channelCategory, charset);
				channelName = URLEncoder.encode(channelName, charset);
			} catch (UnsupportedEncodingException e) {
				logger.debug("UnsupportedEncodingException:",e);
			}
			
			channelPublicUrl = hostName + "/" + "channel-profiles" + "/" + channelCategory + "/" + channelName + "/"
					+ channelId.toString();
		}
		
		logger.debug("channelPublicUrl: " + channelPublicUrl);
		data.put("channelPublicUrl", channelPublicUrl);
		
		// automatically generated random string for each memo..always have non-whitespace characters
		String followMessageUrl = CommonUtil.getMessagePublicUrl(memo);
		followMessageUrl = hostName + "/" + followMessageUrl;
		logger.debug("FollowMessageUrl:" + followMessageUrl);
		data.put("FollowMessageUrl", followMessageUrl);
	}

	public void setSeoTagsForTitleDescriptionMsgHeading(Memo memo, Map<String, Object> data, String channelName) {
		
		String title = channelName;
		String msgHeading = memo.getSubject();

		if (StringUtils.hasText(msgHeading)) {
			if (msgHeading.length() > msgHeadingMaxSize) {
				title = msgHeading.substring(0, msgHeadingMaxSize);
			}
			String reqChannelName = channelName.length() > 18
					? channelName.substring(0, 18)
					: channelName;
			title = title + "|" + reqChannelName;
		}

		logger.debug("title:" + title + ",msgHeading:" + msgHeading);
		data.put("Title", title);
		data.put("MessageHeading", msgHeading);
		
		String description = null;
		Elements pTags = Jsoup.parse(memo.getMessage()).select("p");
		for (Element pTag : pTags) {
			if (pTag.hasText()) {
				description = pTag.text();
				description = description.length() > msgDescMaxSize ? description.substring(0, msgDescMaxSize) + "...":description;
				break;
			}
		}
		// TODO::if <p> tag has <div> tags inside it...you need to only take text
		// TODO::If <p> tag not present ,then parse the memoText and take only text...donot include <div> tags---issue..if no text is present...the filenames of uploaded attachments will come in description

		logger.debug("Channeldescription:" + (Objects.isNull(description) ? " is null" : description));
		
		data.put("Description", description);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		msgHeadingMaxSize = Integer.parseInt(env.getProperty(Constants.MSG_TITLE_SIZE_MAX));
		logger.info("msgHeadingMaxSize=" + msgHeadingMaxSize);

		msgDescMaxSize = Integer.parseInt(env.getProperty(Constants.MSG_DESCRIPTION_SIZE_MAX));
		logger.info("msgDescMaxSize=" + msgDescMaxSize);

		hostName = env.getProperty(Constants.HOST_NAME);
		logger.info("hostname=" + hostName);

		photoImageBaseUrl = env.getProperty(Constants.PHOTO_IMAGE_BASE_URL);
		logger.info("photoImageBaseUrl=" + photoImageBaseUrl);

		attachmentImageBaseUrl = env.getProperty(Constants.ATTACHMENT_IMAGE_BASE_URL);
		logger.info("attachmentImageBaseUrl=" + attachmentImageBaseUrl);

		attachmentImageType = env.getProperty(Constants.ATTACHMENT_TYPE_IMAGE);
		logger.info("attachmentImageType=" + attachmentImageType);

		attachmentTypeMultipart = env.getProperty(Constants.ATTACHMENT_TYPE_MULTIPART);
		logger.info("attachmentTypeMultipart=" + attachmentTypeMultipart);

		charset = env.getProperty(Constants.CHARSET);
		logger.info("charset=" + charset);		
	}
}
