package bluesky.restapi.lightWeightAccount.updateIotDevice;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.helpers.DateTimeHelper;
import org.testng.annotations.BeforeClass;

import java.util.HashMap;

public class BaseUpdateIotDevice extends BaseApiTest {

  static String MT_MAX_DATE;

  @BeforeClass
  public void initialize() {

    MT_MAX_DATE = DateTimeHelper.getMaxDateValueDb();

  }

  HashMap<String, Object> airSensorAttributes = new HashMap<String, Object>(){{
    put("msisdn","msisdnOld");
    put("oem","11");
    put("location","12356");
    put("model","1234");
  }};

  HashMap<String, Object> updatedAttributes = new HashMap<String,Object>(){{
    put("msisdn","updatedValueMSISDN");
    put("imsi","updatedValueIMSI");
  }};

}
