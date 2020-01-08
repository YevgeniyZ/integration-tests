package bluesky.restapi.lightWeightAccount.bulkUploadIotDevices;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.base.BaseAssertions;
import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.helpers.FileProcessingHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.EndPoints;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.NodeStatus;
import bluesky.restapi.models.OwnershipModes;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bluesky.restapi.helpers.DateTimeHelper.DATE_PATTERN_DEFAULT_START;
import static bluesky.restapi.helpers.FileProcessingHelper.createCsvFileWithData;
import static bluesky.restapi.helpers.FileProcessingHelper.deleteFile;
import static bluesky.restapi.managers.BaseManager.generateUniqueNumber;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Story("BLUESKY-19 Bulk Backend - Upload new IOT Devices (Single) PART 1 - Request")
public class BulkUploadNodesTest extends BaseBulkUploadNodes {

  private Integer responsibleId;

  @BeforeClass
  private void createResponsible() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    this.responsibleId = getAccountId(responsibleResponse);
  }

  @Test(description = "Verify API returns 200 for valid file uploading")
  public void bulkUploadNodeWithValidFilePositiveTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(3);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(responsibleId, fileName);

    BaseAssertions.verifyFieldIsReturnedInResponse(response, "requestId");
  }

  @Test(description = "Verify API nodes are uploaded with all headers")
  public void bulkUploadNodeWithAllHeadersPositiveTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(3);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(responsibleId, fileName);

    BaseAssertions.verifyFieldIsReturnedInResponse(response, "requestId");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-382")
  @Test(description = "Verify API rejects empty file upload (NegativeTest)")
  public void bulkUploadNodeWithEmptyFileNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = new ArrayList<>();
    String[] emptyHeaders = new String[0];

    List<String[]> data = prepareDataForBulkUploadNodesFile(emptyHeaders, lwaData);

    createCsvFileWithData(fileName, data);

    //Upload empty file
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), responsibleId);

    BaseAssertions.verifyErrorResponse(response, 400, "Failed to execute bad "
        + "request. Reason: Invalid file content");
  }

  @Test(description = "Verify API rejects response if duplicate external ids (NegativeTest)")
  public void bulkUploadNodeWithDuplicatedExternalIdNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    String externalId = BULK_UPLOAD_PREFIX + generateUniqueNumber();

    List<String[]> lwaData = new ArrayList<>();
    lwaData.add(new String[] { externalId, MSISDN, IMSI, OEM, "", CAMERA_SENSOR_TYPE });
    lwaData.add(new String[] { externalId, MSISDN, IMSI, OEM, "", CAMERA_SENSOR_TYPE });
    lwaData.add(new String[] { externalId, MSISDN, IMSI, OEM, "", CAMERA_SENSOR_TYPE });

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(responsibleId, fileName);

    BaseAssertions.verifyFieldIsReturnedInResponse(response, "requestId");
  }

  @Test(description = "Verify API rejects for invalid file format uploading")
  public void bulkUploadNodeInvalidFileTypeUploadNegativeTest() {

    String fileName = String.format("src%stest%sresources%stest.png",
        FileProcessingHelper.SEPARATOR, FileProcessingHelper.SEPARATOR, FileProcessingHelper.SEPARATOR);

    LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), responsibleId).then().statusCode(428);
  }

  @Test(description = "Verify API rejects the request if file header contains non existent header (NegativeTest)")
  public void bulkUploadNodeWithNonExistentFileHeaderNegativeTest() {

    String[] invalidCameraFileHeaders =
        new String[] { "external_id", "msisdn", "imsi", "someNonExistentHeader", "oem", "make", "model" };

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, invalidCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), responsibleId);

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Data row not compliant with json schema: #: "
            + "extraneous key [somenonexistentheader] is not permitted");
  }

  @Test(description = "Verify API will return 200 and put the nodes to the db if one of the node will miss external_id")
  public void bulkUploadNodeWithAbsentExternalIdValuePositiveTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = new ArrayList<>();
    lwaData.add(new String[] { BULK_UPLOAD_PREFIX + generateUniqueNumber(), MSISDN, IMSI, OEM, "",
        CAMERA_SENSOR_TYPE });
    lwaData.add(new String[] { "", "111222333", IMSI, OEM, "", "absentOemValue" });
    lwaData.add(new String[] { BULK_UPLOAD_PREFIX + generateUniqueNumber(), MSISDN, IMSI, OEM, "",
        CAMERA_SENSOR_TYPE });

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(responsibleId, fileName);

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);

    //Prepare expected file results
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaData.get(0)[0], "ok");
      put(lwaData.get(1)[0], "Failed to validate the request. Reason: Data row not compliant with json schema: "
          + "#/external_id: string [] does not match pattern ^([\\S]+)$");
      put(lwaData.get(2)[0], "ok");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed");
  }

  @Test(description = "Verify API will reject file upload if first row missed external_id (NegativeTest)")
  public void bulkUploadNodeWithAbsentFileExternalIdValueInTheBeginningNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = new ArrayList<>();
    lwaData.add(new String[] { "", "111222333", IMSI, OEM, "", "absentOemValue" });
    lwaData
        .add(new String[] { BULK_UPLOAD_PREFIX + generateUniqueNumber(), MSISDN, IMSI, OEM, "", CAMERA_SENSOR_TYPE });

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), responsibleId).then().statusCode(428);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-367")
  @Test(description = "Verify API rejects request if file header missed external_id (NegativeTest)")
  public void bulkUploadNodeWithAbsentFileHeaderExternalIdNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    String[] invalidCameraFileHeaders = new String[] { "msisdn", "imsi", "oem", "make", "model" };

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, invalidCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName), responsibleId);

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Invalid file content");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-377")
  @Test(description = "Verify API returns 400 if record amount not equal to headers (NegativeTest)")
  public void bulkUploadNodeWithAbsentOemNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    String[] invalidCameraFileHeaders = new String[] { "externalId", "msisdn", "imsi", "make", "model" };

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, invalidCameraFileHeaders);

    //Upload nodes
    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName), responsibleId);

    //Assert status code
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Invalid file content");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-367")
  @Test(description = "Verify API rejects request if file header miss required msisdn (NegativeTest)")
  public void bulkUploadNodeWithAbsentFileHeaderMsisdnNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    String[] invalidCameraFileHeaders = new String[] { "external_id", "imsi", "oem", "make", "model" };

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, invalidCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName), responsibleId);

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Invalid file content");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-369")
  @Test(description = "Verify API rejects request if file missed (NegativeTest)")
  public void bulkUploadNodeWithAbsentFileNegativeTest() {

    Response response = given()
        .accept("application/json")
        .contentType("multipart/form-data")
        .headers(new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
        }})
        .multiPart("file", "")
        .log().uri().log().method().log().headers().log().parameters()
        .auth().oauth2(BaseApiTest.token)
        .post(EndPoints.getLightWeightAccountBulkUploadUrl())
        .then().log().everything().extract().response();

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400, "Required request part "
        + "'file' is not present");
  }

  @Test(description = "Verify start date equals to machine date if not passed")
  public void bulkUploadNodeDefaultDatePositiveTest() throws SQLException {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(3);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(responsibleId, fileName);

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String[] s = lwaData.get(0);
    String externalId = s[0];

    //Query for DB
    String query = String.format(SELECT_NODE_QUERY, externalId);
    String nodeStartDate = DBHelper.getValueFromDB(query, "start_date").substring(0, 10);

    //Verify APi response
    Assert.assertEquals(nodeStartDate, DateTimeHelper.getCurrentDate("yyyy-MM-dd"),
        "Start date is not current date by default");
  }

  @Test(description = "Verify nodes are uploaded with different types and same external ids")
  public void bulkUploadNodeDifferentTypesSameExternalIdPositiveTest() {

    String fileNameCamera = getFileName(BULK_UPLOAD_PREFIX);

    String fileNameAirsensor = getFileName(BULK_UPLOAD_PREFIX);

    //Prepare content for camera
    List<String[]> lwaDataCamera = createListOfCameraNodes(2);

    createFileForUpload(fileNameCamera, lwaDataCamera, validCameraFileHeaders);

    Response responseCameraUpload = getResponseFromUpload(responsibleId, fileNameCamera);

    //Wait until file is not processed
    String requestId = responseCameraUpload.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String[] s = lwaDataCamera.get(0);
    String cameraExternalId1 = s[0];

    //Prepare content for airsensor
    List<String[]> dataAirsensor = new ArrayList<>();
    dataAirsensor
        .add(new String[] { BULK_UPLOAD_PREFIX + generateUniqueNumber(), MSISDN, IMSI, OEM, AIRSENSOR_SENSOR_TYPE });
    dataAirsensor.add(new String[] { cameraExternalId1, MSISDN, IMSI, OEM, AIRSENSOR_SENSOR_TYPE });

    createFileForUpload(fileNameAirsensor, dataAirsensor, validAirsensorFileHeaders);

    Response responseAirsensorUpload = getResponseFromUpload(fileNameAirsensor, new HashMap<String, String>() {{
      put("responsibleId", responsibleId.toString());
      put("ownershipMode", OwnershipModes.RENT);
      put("nodeStatus", NodeStatus.ACTIVE);
      put("schemaType", "AIRSENSOR_BULK_CREATE");
      put("startDate", OffsetDateTime.now().toString());
    }}, 202, "JOB_SCHEDULED");

    //Wait until file is not processed
    String requestId2 = responseAirsensorUpload.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId2);

    Assert.assertNotNull(DBHelper.getNodes(cameraExternalId1, "Camera"));
    Assert.assertNotNull(DBHelper.getNodes(cameraExternalId1, "Airsensor"));
  }

  @Test(description = "Verify all nodes are registered with valid file upload check in DB")
  public void bulkUploadNodeAllNodesRegisteredCheckInDBPositiveTest() throws SQLException {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(2);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(responsibleId, fileName);

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String[] s = lwaData.get(0);
    String requestExternalId1 = s[0];
    String[] s1 = lwaData.get(1);
    String requestExternalId2 = s1[0];

    //Get nodes from DB
    String nodeQuery1 = String.format(SELECT_NODE_QUERY, requestExternalId1);
    String dbExternalId1 = DBHelper.getValueFromDB(nodeQuery1, "external_id");

    String nodeQuery2 = String.format(SELECT_NODE_QUERY, requestExternalId2);
    String dbExternalId2 = DBHelper.getValueFromDB(nodeQuery2, "external_id");

    //Verify
    Assert.assertEquals(dbExternalId1, requestExternalId1);
    Assert.assertEquals(dbExternalId2, requestExternalId2);
  }

  @Test(description = "Verify all node's fields are populated to DB")
  public void bulkUploadNodeAllFieldsArePopulatedInDbPositiveTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(fileName,
        new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("orderId", "ord1");
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("startDate", DateTimeHelper.getTomorrowDate(DATE_PATTERN_DEFAULT_START));
        }}, 202, "JOB_SCHEDULED");

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String[] s = lwaData.get(0);
    String requestExternalId = s[0];

    //Get nodes from DB
    String nodeQuery = String.format(SELECT_NODE_QUERY, requestExternalId);
    Map<String, Object> actualResults = DBHelper.getResultMap(nodeQuery);
    Assert.assertEquals(actualResults.get("ownership_mode"), "Owner", "Ownership mode is incorrect");
    Assert.assertEquals(actualResults.get("responsible_id"), responsibleId,
        "Responsible Id is incorrect");
    Assert.assertNull(actualResults.get("settlement_id"), "Settlement is incorrect");
    Assert.assertEquals(actualResults.get("vmsisdn"), MSISDN, "Msisdn is incorrect");
    Assert.assertEquals(actualResults.get("order_id"), "ord1", "Order id is incorrect: ");
    Assert.assertEquals(actualResults.get("vimsi"), IMSI, "Imsi is incorrect");
    Assert.assertEquals(actualResults.get("json_attributes"), "{\"oem\":\"GE\",\"model\":\"SmartCamx02\","
            + "\"imsi\":\"310410010522518000\",\"msisdn\":\"16473706301\",\"make\":\"\"}",
        "Json attributes are incorrect");
  }

  @Test(description = "Verify all nodes are created with Ownership Lease")
  public void bulkUploadNodeRequestHeaderOwnershipLeasePositiveTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(fileName,
        new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.LEASE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
        }}, 202, "JOB_SCHEDULED");

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String[] s = lwaData.get(0);
    String requestExternalId = s[0];

    //Get nodes from DB
    String nodeQuery = String.format(SELECT_NODE_QUERY, requestExternalId);
    Map<String, Object> actualResults = DBHelper.getResultMap(nodeQuery);
    Assert.assertEquals(actualResults.get("ownership_mode"), "Lease", "Ownership mode is incorrect: ");
  }

  @Test(description = "Verify all nodes are successfully created with empty orderId")
  public void bulkUploadNodeRequestHeaderOrderEmptyPositiveTest() throws SQLException {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(fileName,
        new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("orderId", "");
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("startDate", DateTimeHelper.getTomorrowDate(DATE_PATTERN_DEFAULT_START));
        }}, 202, "JOB_SCHEDULED");

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String[] s = lwaData.get(0);
    String requestExternalId = s[0];

    //Get nodes from DB
    String nodeQuery = String.format(SELECT_NODE_QUERY, requestExternalId);
    String orderIdDB = DBHelper.getValueFromDB(nodeQuery, "order_id");
    Assert.assertNull(orderIdDB, "Order id is not uploaded");
  }

  @Test(description = "Verify all node's are created with default start date")
  public void bulkUploadNodeRequestHeaderStartDateEmptyNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = getResponseFromUpload(fileName,
        new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("startDate", "");
        }}, 400, null);

    BaseAssertions.verifyErrorResponseContainsMessage(response, 400,
        "Failed to execute bad request. Reason: StartDate is required for input Schema Type");
  }

  @Test(description = "Verify bulk create node will be rejected if mandatory request header startDate has null value")
  public void bulkUploadNodeRequestHeaderStartDateNullNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("startDate", null);
        }});

    BaseAssertions.verifyErrorResponseContainsMessage(response, 400,
        "Invalid date format. Start date must be in ISO_LOCAL_DATE_TIME format: yyyy-MM-ddThh:mm:ss");
  }

  @Test(description = "Verify bulk create node will be rejected if mandatory field startDate is absent")
  public void bulkUploadNodeAbsentRequestHeaderStartDateNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
        }});

    BaseAssertions.verifyErrorResponseContainsMessage(response, 400,
        "Failed to execute bad request. Reason: StartDate is required for input Schema Type");
  }

  @Test(description = "Verify bulk create node will be rejected if mandatory request header responsibleId is absent")
  public void bulkUploadNodeAbsentRequestHeaderResponsibleIdNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        new HashMap<String, String>() {{
          put("ownershipMode", OwnershipModes.OWNER);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
        }});

    BaseAssertions.verifyErrorResponseContainsMessage(response, 400,
        "Failed to execute bad request. Reason: ResponsibleId is required for input Schema Type");
  }

  @Test(description = "Verify bulk create node will be rejected if mandatory request header responsibleId has null value")
  public void bulkUploadNodeRequestHeaderResponsibleIdNullNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        new HashMap<String, String>() {{
          put("ownershipMode", OwnershipModes.OWNER);
          put("responsibleId", null);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
        }});

    BaseAssertions.verifyErrorResponseContainsMessage(response, 428,
        "Reason: Invalid Responsible");
  }

  @Test(description = "Verify bulk create node will be rejected if mandatory request header responsibleId has system user")
  public void bulkUploadNodeRequestHeaderResponsibleIdSystemUserNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        new HashMap<String, String>() {{
          put("ownershipMode", OwnershipModes.OWNER);
          put("responsibleId", "system_user/mt");
          put("nodeStatus", NodeStatus.ACTIVE);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
        }});

    BaseAssertions.verifyErrorResponseContainsMessage(response, 428,
        "Reason: Responsible is ineffective on the requested date");
  }

  @Test(description = "Verify start date in header is used for uploaded lwa")
  public void bulkUploadNodeNotDefaultDatePositiveTest() throws SQLException {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = getResponseFromUpload(fileName, new HashMap<String, String>() {{
      put("responsibleId", responsibleId.toString());
      put("ownershipMode", OwnershipModes.OWNER);
      put("nodeStatus", NodeStatus.ACTIVE);
      put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
      put("startDate", DateTimeHelper.getTomorrowDate(DATE_PATTERN_DEFAULT_START));
    }}, 202, "JOB_SCHEDULED");

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String[] s = lwaData.get(0);
    String externalId = s[0];

    //Query for DB
    String query = String.format(SELECT_NODE_QUERY, externalId);
    String nodeStartDate = DBHelper.getValueFromDB(query, "start_date").substring(0, 10);

    //Verify APi response
    Assert.assertEquals(nodeStartDate, DateTimeHelper.getTomorrowDate("yyyy-MM-dd"),
        "Start date is not current date by default");
  }

  @Test(description = "Verify API returns 200 for valid file uploading with AirSensor nodes")
  public void bulkUploadNodeWithValidFileForAirSensorPositiveTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = new ArrayList<>();
    lwaData.add(new String[] { BULK_UPLOAD_PREFIX + generateUniqueNumber(), MSISDN, IMSI, OEM,
        AIRSENSOR_SENSOR_TYPE });
    lwaData.add(new String[] { BULK_UPLOAD_PREFIX + generateUniqueNumber(), MSISDN, IMSI, OEM,
        AIRSENSOR_SENSOR_TYPE });
    lwaData.add(new String[] { BULK_UPLOAD_PREFIX + generateUniqueNumber(), MSISDN, IMSI, OEM,
        AIRSENSOR_SENSOR_TYPE });

    createFileForUpload(fileName, lwaData, validAirsensorFileHeaders);

    Response response = getResponseFromUpload(responsibleId, fileName);

    BaseAssertions.verifyFieldIsReturnedInResponse(response, "requestId");
  }

  @Test(description = "Verify API returns 200 for valid file uploading with startDate in the past")
  public void bulkUploadNodeInThePastPositiveTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(3);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getYesterdayDay(DATE_PATTERN_DEFAULT_START));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", DateTimeHelper.getYesterdayDay(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
        }}).then().statusCode(202).extract().response();

    //Verify APi response
    Assert.assertEquals(response.getBody().jsonPath().get("status"), "JOB_SCHEDULED", "Status is incorrect");
    BaseAssertions.verifyFieldIsReturnedInResponse(response, "requestId");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Verify API reject request if mandatory header status is absent (NegativeTest)")
  public void bulkUploadNodeAbsentStatusRequestHeaderNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponseContainsMessage(response, 400,
        "Failed to execute bad request. Reason: Status is required for input Schema Type");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Verify API successfully upload file with status set to Suspended")
  public void bulkUploadNodeWithSuspendedStatusRequestHeaderPositiveTest() throws SQLException {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
        }});

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String[] s = lwaData.get(0);
    String requestExternalId1 = s[0];

    //Get nodes from DB
    String statusQuery = String.format(SELECT_NODE_QUERY, requestExternalId1);
    String nodeStatus = DBHelper.getValueFromDB(statusQuery, "status");

    //Verify if status updated in DB
    Assert.assertEquals(nodeStatus.toUpperCase(), "SUSPENDED");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Verify API successfully upload file with valid settlement")
  public void bulkUploadNodeWithSettlementRequestHeaderPositiveTest() throws SQLException {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    //Create settlement
    AccountEntity settlement = new AccountEntityManager().createAccountEntity();
    Response response1 = AccountEntityServiceMethods.createAccountEntityRequest(settlement);
    Integer settlementId = getAccountId(response1);

    List<String[]> lwaData = createListOfCameraNodes(1);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("settlementId", settlementId.toString());
        }});

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //get ExternalId from array
    String[] s = lwaData.get(0);
    String requestExternalId1 = s[0];

    //Get nodes from DB
    String settlementQuery = String.format(SELECT_NODE_QUERY, requestExternalId1);
    String nodeSettlementParty = DBHelper.getValueFromDB(settlementQuery, "settlement_id");

    //Verify if status updated in DB
    Assert.assertEquals(nodeSettlementParty, settlementId.toString(), "SettlementId is incorrect or not found");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Verify API reject request if first record has invalid settlement (NegativeTest)")
  public void bulkUploadNodeInvalidSettlementRequestHeaderNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("settlementId", "system_user/mt");
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428, "Failed to validate the "
        + "request. Reason: SettlementId is ineffective on the requested date");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Verify API reject request if status header set to Terminated (NegativeTest)")
  public void bulkUploadNodeWithTerminatedStatusRequestHeaderNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.TERMINATED);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Node status has to be provided for create schema "
            + "type and cannot be in terminated state.");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Verify API reject request if status header set to non existent status (NegativeTest)")
  public void bulkUploadNodeWithNonExistentStatusRequestHeaderNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", DateTimeHelper.getYesterdayDay(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", "non-existent");
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "IllegalArgumentException: No enum constant com.ericsson.ecb.customer.model.NodeStatus."
            + "non-existent");
  }

  @Test(description = "Verify API reject request if status header has empty value (NegativeTest)")
  public void bulkUploadNodeWithStatusRequestHeaderEmptyNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("orderId", "ord1");
          put("startDate", DateTimeHelper.getYesterdayDay(DATE_PATTERN_DEFAULT_START));
          put("schemaType", "CAMERA_BULK_CREATE");
          put("nodeStatus", "non-existent");
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "IllegalArgumentException: No enum constant com.ericsson.ecb.customer.model.NodeStatus."
            + "non-existent");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-364")
  @Test(description = "Verify API rejects request if OwnershipMode is missed (NegativeTest)")
  public void bulkCreateNodeWithMissedOwnershipModeNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", "");
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponseContainsMessage(response, 400,
        "Failed to execute bad request. Reason: Status is required for input Schema Type");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-364")
  @Test(description = "Verify API rejects request if OwnershipMode is missed (NegativeTest)")
  public void bulkUploadNodeAbsentRequestHeaderOwnershipModeNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
          put("orderId", "ord1");
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Ownership is required for input Schema Type");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-384")
  @Test(description = "Verify API rejects request if OwnershipMode is invalid (NegativeTest)")
  public void bulkUploadNodeWithNonExistentOwnershipModeNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("ownershipMode", "nonExistMethod");
          put("nodeStatus", NodeStatus.ACTIVE);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "IllegalArgumentException: No enum constant com.ericsson.ecb.customer.model.OwnershipMode."
            + "nonExistMethod");
  }

  @Test(description = "Verify API rejects request if OwnershipMode is empty (NegativeTest)")
  public void bulkUploadNodeRequestHeaderOwnershipModeEmptyNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("ownershipMode", "");
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", OffsetDateTime.now().toString());
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponseContainsMessage(response, 400,
        "Failed to execute bad request. Reason: Ownership is required for input Schema Type");
  }

  @Test(description = "Verify API rejects request if OwnershipMode is null (NegativeTest)")
  public void bulkUploadNodeRequestHeaderOwnershipModeNullNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("ownershipMode", null);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponseContainsMessage(response, 400,
        "IllegalArgumentException: No enum constant com.ericsson.ecb.customer.model.OwnershipMode.null");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-371")
  @Test(description = "Verify API request will be rejected if start date will have invalid format (NegativeTest)")
  public void bulkUploadNodeInvalidStartDateRequestHeaderFormatNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", "2019-09-1999");
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Invalid date format. Start date must be in "
            + "ISO_LOCAL_DATE_TIME format: yyyy-MM-ddThh:mm:ss");
  }

  @Test(description = "Verify API request rejected with non existent schema type (NegativeTest)")
  public void bulkUploadNodeNonExistentSchemaTypeRequestHeaderNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("schemaType", "nonExistentSchemaType");
          put("nodeStatus", NodeStatus.ACTIVE);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428, "Failed to validate the "
        + "request. Reason: Schema type is invalid");
  }

  @Test(description = "Verify API request rejected with absent schema type (NegativeTest)")
  public void bulkUploadNodeAbsentSchemaTypeRequestHeaderNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", "");
          put("nodeStatus", NodeStatus.ACTIVE);
          put("orderId", "ord1");
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "MissingRequestHeaderException: Missing request header 'schemaType' for method parameter "
            + "of type String");
  }

  @Test(description = "Verify API rejected if responsible is ineffective for this date (NegativeTest)")
  public void bulkUploadNodeResponsibleIsIneffectiveNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(3);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("startDate", DateTimeHelper.getYesterdayDay(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Responsible is ineffective on the requested date");
  }

  @Test(description = "Verify API rejected if responsible is archived for this date (NegativeTest)")
  public void bulkUploadNodeResponsibleIsArchivedNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(3);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Change Responsible status to Archived
    AccountEntityManager.updateAccountStatusToArchived(responsibleId);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("orderId", "ord1");
          put("startDate", DateTimeHelper.getCurrentDate(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Responsible is ineffective on the requested date");
  }

  @Test(description = "Verify API rejected if responsible is not existed (NegativeTest)")
  public void bulkUploadNodeResponsibleIsNonExistentNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(3);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", "Not_existed_111");
          put("ownershipMode", OwnershipModes.OWNER);
          put("orderId", "ord1");
          put("startDate", DateTimeHelper.getYesterdayDay(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid Responsible");
  }

  @Test(description = "Verify API returns 200 for valid file uploading with startDate in the Future")
  public void bulkUploadNodeInTheFuturePositiveTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = createListOfCameraNodes(3);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getTomorrowDate(DATE_PATTERN_DEFAULT_START));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("orderId", "ord1");
          put("startDate", DateTimeHelper.getTomorrowDate(DATE_PATTERN_DEFAULT_START));
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
        }}).then().statusCode(202).extract().response();

    //Verify APi response
    Assert.assertEquals(response.getBody().jsonPath().get("status"), "JOB_SCHEDULED", "Status is incorrect");
    BaseAssertions.verifyFieldIsReturnedInResponse(response, "requestId");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Checking the api returns error for system user settlement party (NegativeTest)")
  public void bulkUploadFileSettlementRequestHeaderInSystemUserNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    //Upload file
    LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("settlementId", "system_user/mt");
        }}).then().statusCode(400);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Checking the api returns error for system user settlement party (NegativeTest)")
  public void bulkUploadFileSettlementInSystemUserNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", OffsetDateTime.now().toString());
          put("settlementId", "system_user/mt");
        }}).then().statusCode(428);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Checking the api returns file with error for settlement party which has start date in future "
      + "(NegativeTest)")
  public void bulkUploadFileStartDateInFutureSettlementRequestHeaderNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Add new settlement
    AccountEntity settlement = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 3));
    Response settlementResponse =
        AccountEntityServiceMethods.createAccountEntityRequest(settlement);
    Integer settlementId = getAccountId(settlementResponse);

    List<String[]> lwaData = createListOfCameraNodes(3);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    //Upload file
    LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", OffsetDateTime.now().toString());
          put("settlementId", settlementId.toString());
        }}).then().statusCode(428);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Checking the api returns error for non existent user settlement party (NegativeTest)")
  public void bulkUploadFileNonExistentSettlementRequestHeaderNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", OffsetDateTime.now().toString());
          put("settlementId", "non-existentSettlment");
        }}).then().statusCode(428);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Checking the api reject request for settlement is NULL (NegativeTest)")
  public void bulkUploadNodeRequestHeaderSettlementNullNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", OffsetDateTime.now().toString());
          put("settlementId", null);
        }}).then().statusCode(428);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-383")
  @Test(description = "Verify API request will be rejected if there will be non existent header (NegativeTest)")
  public void bulkUploadNodeWithNonExistentRequestHeadersNegativeTest() {

    String fileName = createFileForInvalidHeaders();

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("blablahead", CAMERA_CREATE_SCHEMA_TYPE);
          put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: StartDate is required for input Schema Type");
  }

  @Test(description = "Verify request will be rejected if file is already exist (NegativeTest)")
  public void bulkUploadNodeWithDuplicatedFileNameNegativeTest() {

    //File name of .csv
    String fileName = String.format("src%stest%sresources%sBulkUploadNewOne.csv",
        FileProcessingHelper.SEPARATOR, FileProcessingHelper.SEPARATOR, FileProcessingHelper.SEPARATOR);

    try {
      //Fill the file with a data
      List<String[]> lwaData = createListOfCameraNodes(1);

      createFileForUpload(fileName, lwaData, validCameraFileHeaders);

      //File upload
      LightWeightAccountServiceMethods
          .bulkUploadNodes(new File(fileName), responsibleId);

      //Duplicate file upload
      Response response = LightWeightAccountServiceMethods
          .bulkUploadNodes(new File(fileName), responsibleId);

      //Assert response
      BaseAssertions.verifyErrorResponse(response, 400,
          "Failed to execute bad request. Reason: File already exists");
    } finally {
      deleteFile(fileName);
    }
  }

  private Response getResponseFromUpload(final Integer responsibleId, final String fileName) {

    return LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName), responsibleId)
        .then()
        .statusCode(202)
        .body("status", equalTo("JOB_SCHEDULED"))
        .extract()
        .response();
  }

  private Response getResponseFromUpload(final String fileName, final HashMap<String, String> headers,
                                         final int statusCode, final String statusValue) {

    return LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName), headers)
        .then()
        .statusCode(statusCode)
        .body("status", equalTo(statusValue))
        .extract()
        .response();
  }

  private String createFileForInvalidHeaders() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = new ArrayList<>();
    lwaData.add(
        new String[] { String.format("bulkUpload%s", generateUniqueNumber()), MSISDN, IMSI, OEM,
            "TestMake", CAMERA_SENSOR_TYPE });

    List<String[]> data = prepareDataForBulkUploadNodesFile(validCameraFileHeaders, lwaData);

    createCsvFileWithData(fileName, data);

    return fileName;
  }
}
