package bluesky.restapi.lightWeightAccount.bulkUploadIotDevices;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.EndPoints;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.OwnershipModes;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static bluesky.restapi.managers.BaseManager.generateUniqueNumber;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;
import static io.restassured.RestAssured.given;

@Story("BLUESKY-96 Bulk Backend - Upload new IOT Devices (Single) PART 2 - Get Status/Result")
public class DownloadProcessedFilesNodesTest extends BaseBulkUploadNodes {

  private Integer responsibleId;

  @BeforeClass
  private void createResponsible() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    this.responsibleId = getAccountId(responsibleResponse);
  }

  @Test(description = "Checking that API returns the status of the (File Processing) Request when file already complete.")
  public void downloadProcessedFileAlreadyCompletedPositiveTest() {

    String fileName = getFileName("BulkUpload");

    List<String[]> lwaData = this.createListOfCameraNodes(2);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    String requestId = this.uploadFileAndGetRequestId(fileName, responsibleId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);

    //Prepare expected file results
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaData.get(0)[0], "ok");
      put(lwaData.get(1)[0], "ok");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed");
  }

  @Test(description = "Checking Device Result File is generated with the same number of entries as the input file")
  public void downloadProcessedFileTheExactAmountPositiveTest() {

    String fileName = getFileName("BulkUpload");

    ArrayList<String[]> lwaData = this.createListOfCameraNodes(5);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    String requestId = this.uploadFileAndGetRequestId(fileName, responsibleId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);

    //Prepare expected file results
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaData.get(0)[0], "ok");
      put(lwaData.get(1)[0], "ok");
      put(lwaData.get(2)[0], "ok");
      put(lwaData.get(3)[0], "ok");
      put(lwaData.get(4)[0], "ok");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-454")
  @Test(description = "Checking the api will accept file for archived settlement party (NegativeTest)")
  public void downloadProcessedFileArchivedSettlementNegativeTest() {

    String fileName = getFileName("BulkUpload");

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Add new settlement
    AccountEntity settlement = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response settlementResponse = AccountEntityServiceMethods.createAccountEntityRequest(settlement);
    Integer settlementId = getAccountId(settlementResponse);

    //Change Settlement status to Archived
    AccountEntityManager.updateAccountStatusToArchived(settlementId);

    String[] validCameraFileHeaders = new String[] { "external_id", "msisdn", "imsi", "oem", "make", "model" };

    List<String[]> lwaData = createListOfCameraNodes(2);

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    //Upload file
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("ownershipMode", OwnershipModes.OWNER);
          put("orderId", "ord1");
          put("startDate", OffsetDateTime.now().toString());
          put("schemaType", "CAMERA_BULK_CREATE");
          put("nodeStatus", "ACTIVE");
          put("settlementId", settlementId.toString());
        }});
    String requestId = response.body().jsonPath().get("requestId");

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);

    //Prepare expected file results
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaData.get(0)[0],
          "Failed to validate the request. Reason: settlement party is ineffective on the requested date");
      put(lwaData.get(1)[0],
          "Failed to validate the request. Reason: settlement party is ineffective on the requested date");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed");
  }

  @Test(description = "Checking length validation of mandatory attribute MSISDN")
  public void downloadProcessedFileMsisdnOverLengthPositiveTest() {

    String fileName = getFileName("BulkUpload");

    String invalidMsisdn = RandomStringUtils.random(855, true, true);

    //Create list of nodes
    ArrayList<String[]> lwaData = new ArrayList<>();
    lwaData.add(
        new String[] { String.format("bulkUpload%s", generateUniqueNumber()), "123456789", "310410010522518000", "GE",
            "", "SmartCamx02" });
    lwaData.add(
        new String[] { String.format("bulkUpload%s", generateUniqueNumber()), invalidMsisdn, "310410010522518000", "GE",
            "", "SmartCamx02" });

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    String requestId = this.uploadFileAndGetRequestId(fileName, responsibleId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);

    //Prepare expected file results
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaData.get(0)[0], "ok");
      put(lwaData.get(1)[0], "Failed to validate the request. Reason: The request was rejected because the length of "
          + "the attribute/s value should not exceed the 1700 bytes.");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-602")
  @Test(description = "Checking result with only \" \" (space) in external_id field (NegativeTest)")
  public void downloadProcessedFileWithSpaceValueExternalIdNegativeTest() {

    String fileName = getFileName("BulkUpload");

    //Create list of nodes
    ArrayList<String[]> lwaData = new ArrayList<>();
    lwaData.add(
        new String[] { String.format("bulkUpload%s", generateUniqueNumber()), "123456789", "310410010522518000", "GE",
            "", "SmartCamx02" });
    lwaData.add(new String[] { " ", "123456789", "310410010522518000", "GE", "", "SmartCamx02" });

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    String requestId = this.uploadFileAndGetRequestId(fileName, responsibleId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);

    //Prepare expected file results
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaData.get(0)[0], "ok");
      put(lwaData.get(1)[0],
          "Failed to validate the request. Reason: Data row not compliant with json schema: #/external_id: string [ ] "
              + "does not match pattern ^([\\S]+)$");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed");
  }

  @Test(description = "Checking result with empty attributes including empty mondatory field MSISDN (NegativeTest)")
  public void downloadProcessedFileWithSpaceValueMsisdnNegativeTest() {

    String fileName = getFileName("BulkUpload");

    //Create list of nodes
    ArrayList<String[]> lwaData = new ArrayList<>();
    lwaData.add(
        new String[] { String.format("bulkUpload%s", generateUniqueNumber()), "123456789", "310410010522518000", "GE",
            "", "SmartCamx02" });
    lwaData.add(
        new String[] { String.format("bulkUpload%s", generateUniqueNumber()), "", "310410010522518000", "GE", "",
            "SmartCamx02" });

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    String requestId = this.uploadFileAndGetRequestId(fileName, responsibleId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);

    //Prepare expected file results
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaData.get(0)[0], "ok");
      put(lwaData.get(1)[0], "Failed to validate the request. Reason: Data row not compliant with json schema: "
          + "#/msisdn: string [] does not match pattern ^(?!\\s).+$");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed");
  }

  @Test(description = "Checking result with nodes external_id which are already exist in database (NegativeTest)",
      groups = "smoke")
  public void downloadProcessedFileExistingNodeValidationNegativeTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    String fileName = getFileName("BulkUpload");

    //Create list of nodes
    ArrayList<String[]> lwaData = this.createListOfCameraNodes(3);
    lwaData.add(new String[] { lwa.getExternalId(), "16473706301", "310410010522518000", "GE", "", "SmartCamx02" });

    createFileForUpload(fileName, lwaData, validCameraFileHeaders);

    String requestId = this.uploadFileAndGetRequestId(fileName, responsibleId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);

    //Prepare expected file results
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaData.get(0)[0], "ok");
      put(lwaData.get(1)[0], "ok");
      put(lwaData.get(2)[0], "ok");
      put(lwaData.get(3)[0],
          "Failed to validate the request. Reason: Node already exists for given external id and node type");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed");
  }

  @Test(description = "Checking request with wrong requestID")
  public void downloadProcessedFileWrongRequestIdTest() {

    //Get file status and assert the error code
    Response response = given()
        .accept(ContentType.JSON)
        .auth().oauth2(BaseApiTest.token)
        .log().uri().and().log().method()
        .when().get(EndPoints.getDownloadProcessedFileUrl(), "nonExistentreq")
        .then().statusCode(404)
        .log().all(true).extract().response();

    Assert.assertEquals(response.getBody().jsonPath().get("status"), "NOT_KNOWN", "Status is incorrect");
    Assert.assertEquals(response.getBody().jsonPath().get("requestId"), "nonExistentreq", "RequestId "
        + "is incorrect");
  }

  private String uploadFileAndGetRequestId(String fileName, Integer responsibleId) {

    return LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName), responsibleId)
        .then().statusCode(202)
        .extract().body().jsonPath()
        .get("requestId");
  }
}
