package core.services.chimeevents;

import org.json.JSONObject;

import java.util.regex.Pattern;

public interface ChimeEventService {
    void processEvents(JSONObject queueMessageJson, String eventSource, String messageId);

    default boolean isNumeric(String strNum) {
        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        if (strNum == null) {
            return false;
        }
        return pattern.matcher(strNum).matches();
    }
}