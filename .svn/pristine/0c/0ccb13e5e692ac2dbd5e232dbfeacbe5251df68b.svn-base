package core.utils;


import com.fasterxml.jackson.databind.JsonNode;
import core.services.CacheService;

import core.services.VideokycServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OrgUtil {

	final static Logger logger = LoggerFactory.getLogger(OrgUtil.class);
    public static final String AssignRejectedKycToAuditor = "AssignRejectedKycToAuditor";

    @Autowired
    private CacheService cacheService;

    public Boolean getPreferenceAsBoolean(Integer orgId, String preference){
        String value = getPreference(orgId, preference);
        return Boolean.valueOf(value);
    }

    public String getPreference(Integer orgId, String preference){
        return getPreference(cacheService.getOrgJson(orgId), preference);
    }

    public String getPreference(JsonNode orgJson, String preference){
    	logger.info("preference: "+preference);
        JsonNode prefs = orgJson.findPath("settings");
        if (prefs != null && prefs.isArray()) {
            for (JsonNode pref : prefs) {            	
                if (preference.equalsIgnoreCase(pref.findPath("preference").asText())) {
                	logger.info("preference: "+pref.findPath("preference").asText());
                	logger.info("value: "+pref.findPath("value"));
                    return pref.findPath("value").asText();
                }
            }
        }
        return null;
    }
    
    public JsonNode getPreferenceValue(Integer orgId, String preference){        
    	logger.info("preference: "+preference);
    	JsonNode orgJson =cacheService.getOrgJson(orgId);
        JsonNode prefs = orgJson.findPath("settings");
        if (prefs != null && prefs.isArray()) {
            for (JsonNode pref : prefs) {            	
                if (preference.equalsIgnoreCase(pref.findPath("preference").asText())) {
                	logger.info("preference: "+pref.findPath("preference").asText());
                	logger.info("value: "+pref.findPath("value"));
                    return pref.findPath("value");
                }
            }
        }
        return null;
    }

}
