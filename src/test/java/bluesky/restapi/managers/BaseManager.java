package bluesky.restapi.managers;


import org.apache.commons.lang3.RandomStringUtils;

import javax.security.auth.login.Configuration;
import java.util.prefs.Preferences;

public class BaseManager {

    public static int incrementIndex() {
        Preferences prefs = Preferences.userNodeForPackage(Configuration.class);
        int result = prefs.getInt("runNumber", 1);
        prefs.putInt("runNumber", result + 1 >= 1000 ? 1 : result + 1);
        return result;
    }

    public static String generateUniqueNumber(){
        return incrementIndex()+ RandomStringUtils.randomAlphabetic(5);
    }
}
