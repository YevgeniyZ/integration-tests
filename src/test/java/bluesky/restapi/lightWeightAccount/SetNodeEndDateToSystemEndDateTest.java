package bluesky.restapi.lightWeightAccount;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.LightWeightAccount;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static bluesky.restapi.base.BaseAssertions.verifyLwaFields;
import static bluesky.restapi.helpers.FileProcessingHelper.createCsvFileWithData;
import static bluesky.restapi.lightWeightAccount.bulkUploadIotDevices.BaseBulkUploadNodes.prepareDataForBulkUploadNodesFile;
import static bluesky.restapi.managers.BaseManager.generateUniqueNumber;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;
import static org.hamcrest.Matchers.equalTo;

@Story("BLUESKY-183 LW Account Backend - Set Node end date to 'system end date'")
public class SetNodeEndDateToSystemEndDateTest extends BaseApiTest {

  private static final String MSISDN = "16473706301";
  private static final String OEM = "310410010522518000";
  private static final String MAKE = "GE";
  private static final String SELECT_EXTERNAL_ID_QUERY = "select * from t_lwa_node where external_id = '%s'";
  private static final String BASE_CSV_DIR = "src\\test\\resources\\BulkUpload";
  private String[] validCameraFileHeaders = new String[] { "external_id", "msisdn", "imsi", "oem", "make", "model" };

  private String[] validAirsensorFileHeaders = new String[] { "external_id", "msisdn", "oem", "make", "model" };

  private HashMap<String, Object> updatedAttributes = new HashMap<String, Object>() {{
    put("msisdn", "updatedValueMSISDN");
    put("imsi", "updatedValueIMSI");
  }};

  private HashMap<String, Object> updatedAIRAttributes = new HashMap<String, Object>() {{
    put("msisdn", "updatedValueMSISDN");

  }};

  private HashMap<String, Object> airSensorAttributes = new HashMap<String, Object>() {{
    put("msisdn", "msisdnOld");
    put("oem", "11");
    put("location", "12356");
    put("model", "1234");
  }};

  @Test(description = "Verify the IOT Device CAMERA is created as a new Node with MTMAXDate value in DB", groups = "smoke")
  public void registerValidLWACameraEndDateCheckPositiveTest() throws SQLException {

    Integer responsibleId = createNewResponsibleAndGetId();
    LightWeightAccount.Node node = createCameraNode();
    LightWeightAccount lwa = createNewLwaAccountAndRegisterNode(responsibleId, node);
    verifyMaxDateBasedOnExternalId(lwa.getExternalId());
  }

  @Test(description = "Verify the IOT Device CAMERA is updated with STATUS SUSPENDED and End_date has MTMaxDate value "
      + "(both DB and API response)", groups = "smoke")
  public void updateLWAStatusEndDateCheckPositiveTest() throws SQLException, ParseException {

    Integer responsibleId = createNewResponsibleAndGetId();
    LightWeightAccount.Node node = createCameraNode();
    LightWeightAccount lwa = createNewLwaAccountAndRegisterNode(responsibleId, node);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setNodeStatus("SUSPENDED")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyLwaFields(response, getExpectedLwaFields(lwaUpdated.setEndDate(EXPECTED_DATE_STRING_WITH_TIMEZONE)));
    verifyMaxDateBasedOnExternalId(lwaUpdated.getExternalId());
  }

  @Test(description = "Verify the IOT Device AIRSENSOR is created as a new Node with MTMAXDate value in DB", groups = "smoke")
  public void registerValidLWAAirSensorEndDateCheckPositiveTest() throws SQLException {

    Integer responsibleId = createNewResponsibleAndGetId();
    LightWeightAccount.Node node = createDefaultNode().setNodeType("AIRSENSOR").setAttributes(airSensorAttributes);
    LightWeightAccount lwa = createNewLwaAccountAndRegisterNode(responsibleId, node);

    verifyMaxDateBasedOnExternalId(lwa.getExternalId());
  }

  @Test(description = "Verify the IOT Device AIRSENSOR is updated with guiding attribute MSISDN and End_date has "
      + "MTMaxDate value (both DB and API response) ", groups = "smoke")
  public void updateLWAAirSensorAttributeMSISDNEndDateCheckPositiveTest() throws SQLException, ParseException {

    Integer responsibleId = createNewResponsibleAndGetId();
    LightWeightAccount.Node node = createDefaultNode().setNodeType("AIRSENSOR").setAttributes(airSensorAttributes);
    LightWeightAccount lwa = createNewLwaAccountAndRegisterNode(responsibleId, node);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAIRAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyLwaFields(response, getExpectedLwaFields(lwaUpdated.setEndDate(EXPECTED_DATE_STRING_WITH_TIMEZONE)));
    verifyMaxDateBasedOnExternalId(lwaUpdated.getExternalId());
  }

  @Test(description = "Verify the BULK Upload devices CAMERA and each node has end_date with MTMaxDate Value in DB",
      groups = "smoke")
  public void bulkUploadDeviceCameraAndVerifyEndDatePositiveTest() throws SQLException {

    Integer responsibleId = createNewResponsibleAndGetId();

    String sensorType = "Camera01";
    List<String[]> lwaData = new ArrayList<>();
    lwaData.add(new String[] { "bulkUpload" + generateUniqueNumber(), MSISDN, OEM, MAKE, "", sensorType });
    lwaData.add(new String[] { "bulkUpload" + generateUniqueNumber(), MSISDN, OEM, MAKE, "", sensorType });
    lwaData.add(new String[] { "bulkUpload" + generateUniqueNumber(), MSISDN, OEM, MAKE, "", sensorType });

    List<String[]> data = prepareDataForBulkUploadNodesFile(validCameraFileHeaders, lwaData);
    String fileName = createCsvFileName();
    createCsvFileWithData(fileName, data);
    Response response = getResponseFromUpload(responsibleId, fileName);

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    String externalId = getExternalIdFromDataArray(lwaData);
    verifyMaxDateBasedOnExternalId(externalId);
  }

  @Test(description = "Verify the BULK Upload devices AIRSENSOR and each node has end_date with MTMaxDate Value in DB",
      groups = "smoke")
  public void bulkUploadDeviceAirSensorAndVerifyEndDatePositiveTest() throws SQLException {

    Integer responsibleId = createNewResponsibleAndGetId();

    String fileName = createCsvFileName();

    String sensorType = "Airsensor01";
    List<String[]> lwaData = new ArrayList<>();
    lwaData.add(new String[] { "bulkUpload" + generateUniqueNumber(), MSISDN, OEM, MAKE, sensorType });
    lwaData.add(new String[] { "bulkUpload" + generateUniqueNumber(), MSISDN, OEM, MAKE, sensorType });
    lwaData.add(new String[] { "bulkUpload" + generateUniqueNumber(), MSISDN, OEM, MAKE, sensorType });

    List<String[]> data = prepareDataForBulkUploadNodesFile(validAirsensorFileHeaders, lwaData);

    createCsvFileWithData(fileName, data);

    Response response = getResponseFromUpload(responsibleId, fileName);

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String externalId = getExternalIdFromDataArray(lwaData);
    verifyMaxDateBasedOnExternalId(externalId);
  }

  @Test(description = "Verify that after update node Responsible, end_date of the node is not NULL and MTmaxDate value "
      + "in response and DB after Update.", groups = "smoke")
  public void updateValidLWACameraResponsibleAndEndDateCheckPositiveTest() throws SQLException {

    Integer responsibleId = createNewResponsibleAndGetId();

    LightWeightAccount.Node node = createCameraNode();
    LightWeightAccount lwa = createNewLwaAccountAndRegisterNode(responsibleId, node);

    Integer responsible2Id = createNewResponsibleAndGetId();

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(responsible2Id.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdated.getResponsibleId());
      put("externalId", lwaUpdated.getExternalId());
      put("endDate", EXPECTED_DATE_STRING_WITH_TIMEZONE);

    }});

    verifyMaxDateBasedOnExternalId(lwaUpdated.getExternalId());
  }

  private void verifyMaxDateBasedOnExternalId(final String externalId) throws SQLException {

    String newAttributesQuery = String.format(SELECT_EXTERNAL_ID_QUERY, externalId);
    verifyMaxEndDate(newAttributesQuery);
  }

  private String createCsvFileName() {

    return BASE_CSV_DIR + generateUniqueNumber() + ".csv";
  }

  private LightWeightAccount createNewLwaAccountAndRegisterNode(final Integer responsibleId,
                                                                final LightWeightAccount.Node node) {

    LightWeightAccount lwa = createDefaultLightWeightAccount(node).setResponsibleId(responsibleId.toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);
    return lwa;
  }

  private LightWeightAccount.Node createCameraNode() {

    return createDefaultNode().setNodeType("CAMERA");
  }

  private void verifyMaxEndDate(final String newAttributesQuery) throws SQLException {

    String EndDateAttributesFromDB = DBHelper.getValueFromDB(newAttributesQuery, "end_date");
    Assert.assertEquals(EndDateAttributesFromDB, EXPECTED_DATE_STRING_NO_TIMEZONE,
        "Node End_Date is not MTMaxDate value");
  }

  private Integer createNewResponsibleAndGetId() {

    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    return getAccountId(responsibleResponse);
  }

  private Response getResponseFromUpload(final Integer responsibleId, final String fileName) {

    return LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName), responsibleId)
        .then()
        .statusCode(202)
        .body("status", equalTo("JOB_SCHEDULED"))
        .extract()
        .response();
  }

  private String getExternalIdFromDataArray(final List<String[]> lwaData) {

    String[] s = lwaData.get(0);
    return s[0];
  }

  private static HashMap<String, Object> getExpectedLwaFields(LightWeightAccount lwa) throws ParseException {

    String formattedDate =
        getTimezoneFormattedDate(
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(lwa.getEndDate()));
    return new HashMap<String, Object>() {{
      put("externalId", lwa.getExternalId());
      put("endDate", formattedDate);

    }};
  }

  private static String getTimezoneFormattedDate(final Date originalDate) {

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    return dateFormatter.format(originalDate);
  }
}
