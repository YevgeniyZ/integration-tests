package bluesky.restapi.lightWeightAccount.bulkUploadIotDevices;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.base.BaseAssertions;
import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.helpers.TokenAccessHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.EndPoints;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.AccountState;
import bluesky.restapi.models.AccountStatus;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.NodeStatus;
import bluesky.restapi.models.NodeTypes;
import bluesky.restapi.models.OwnershipModes;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static bluesky.restapi.base.BaseAssertions.verifyGetLwaFields;
import static bluesky.restapi.base.BaseAssertions.verifyGetNodeAttributes;
import static bluesky.restapi.helpers.DateTimeHelper.getMaxDateValueDb;
import static bluesky.restapi.helpers.FileProcessingHelper.createCsvFileWithData;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;
import static io.restassured.RestAssured.given;

@Story("BLUESKY-31 Bulk Backend - Update IOT Devices Payer (Move)")
public class BulkUpdateNodesResponsibleTest extends BaseBulkUploadNodes {

  private Integer initialResponsibleId;
  private String jsonLwaPath = String.format("find { it.nodeStatus == '%s'}.", NodeStatus.ACTIVE);

  @BeforeClass
  private void createResponsible() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    this.initialResponsibleId = getAccountId(responsibleResponse);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-1032")
  @Test(description = "Verify that API returns the status of the file is successfully uploaded for node type Camera"
      + " with only mandatory fields", groups = "smoke")
  public void bulkUpdateResponsibleWhenNodeTypeIsCameraWithOnlyMandatoryFieldsPositiveTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(initialResponsibleId,
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //bulk update responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //lwaList update responsible
    updateInformationInLwaList(lwaList, null, newResponsibleId,
        DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Retrieve first Node via API
    response = retrieveIotDevice(responsible, lwaList.get(0));

    //Verify first Node all fields
    verifyLwaFields(response, lwaList.get(0), jsonLwaPath);

    //Retrieve second Node via API
    response = retrieveIotDevice(responsible, lwaList.get(1));

    //Verify second Node all fields
    verifyLwaFields(response, lwaList.get(1), jsonLwaPath);
  }

  @Test(description = "Verify that API returns the status of the file is successfully uploaded for node type AirSensor "
      + "with only mandatory fields", groups = "smoke")
  public void bulkUpdateResponsibleWhenNodeTypeIsAirSensorWithOnlyMandatoryFieldsGetStatusPositiveTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(initialResponsibleId,
        NodeTypes.NODE_TYPE_AIRSENSOR);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            AIRSENSOR_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //lwaList update responsible
    updateInformationInLwaList(lwaList, null, newResponsibleId,
        DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Retrieve first Node via API
    response = retrieveIotDevice(responsible, lwaList.get(0));

    //Verify first Node all fields
    verifyLwaFields(response, lwaList.get(0), jsonLwaPath);

    //Retrieve second Node via API
    response = retrieveIotDevice(responsible, lwaList.get(1));

    //Verify second Node all fields
    verifyLwaFields(response, lwaList.get(1), jsonLwaPath);
  }

  @Test(description = "Verify that nodes updated with valid file and past date", groups = "smoke")
  public void bulkUpdatResponsibleInPastDatePositiveTest() {

    //Create in the past responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleIdInPast = getAccountId(responsibleResponse);

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createNodesList(2, responsibleIdInPast,
        DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2), NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));
    responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Create headers for bulk update Responsible and ownerShipMode
    HashMap<String, String> headersForBulkUpdate = new HashMap<String, String>() {{
      put("ResponsibleId", newResponsibleId.toString());
      put("schemaType", CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE);
      put("ownerShipMode", OwnershipModes.LEASE);
      put("startDate", DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    }};

    //Bulk update nodes
    Response response = getResponseFromUpload(fileName, headersForBulkUpdate);

    //Update lwaList
    updateInformationInLwaList(lwaList, headersForBulkUpdate, null, null);

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Retrieve first Node via API
    response = retrieveIotDevice(responsible, lwaList.get(0));

    //Verify first Node all fields
    verifyLwaFields(response, lwaList.get(0), jsonLwaPath);

    //Retrieve second Node via API
    response = retrieveIotDevice(responsible, lwaList.get(1));

    //Verify second Node all fields
    verifyLwaFields(response, lwaList.get(1), jsonLwaPath);
  }

  @Test(description = "Verify that nodes updated with valid file and future date", groups = "smoke")
  public void bulkUpdateResponsibleInFutureDatePositiveTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(initialResponsibleId,
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //bulk update responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
            DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //lwaList update responsible
    updateInformationInLwaList(lwaList, null, newResponsibleId,
        DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Retrieve first Node via API
    response = retrieveIotDevice(responsible, lwaList.get(0));

    //Verify first Node all fields
    verifyLwaFields(response, lwaList.get(0), jsonLwaPath);

    //Retrieve second Node via API
    response = retrieveIotDevice(responsible, lwaList.get(1));

    //Verify second Node all fields
    verifyLwaFields(response, lwaList.get(1), jsonLwaPath);
  }

  @Test(description = "Verify that nodes updated with valid file and correct date and check history in DB", groups = "smoke")
  public void bulkUpdateResponsibleAndCheckInDbPositiveTest() throws SQLException {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START), NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Create headers for bulk update Responsible and orderId
    HashMap<String, String> headersForBulkUpdate = new HashMap<String, String>() {{
      put("ResponsibleId", newResponsibleId.toString());
      put("schemaType", CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE);
      put("orderId", "ord5");
      put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    }};

    //Bulk update nodes
    Response response = getResponseFromUpload(fileName, headersForBulkUpdate);

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String query = String.format("select responsible_id from t_lwa_node where external_id = '%s' and end_date <> '%s'",
        lwaList.get(0).getExternalId(), getMaxDateValueDb());
    String firstResponsible = DBHelper.getValueFromDB(query, "responsible_id");

    String query2 = String.format("select responsible_id from t_lwa_node where external_id = '%s' and end_date = '%s'",
        lwaList.get(0).getExternalId(), getMaxDateValueDb());
    String updatedResponsible = DBHelper.getValueFromDB(query2, "responsible_id");

    //Assert responsible is updated for lwa
    Assert.assertEquals(firstResponsible, initialResponsibleId.toString(), "Responsible is not updated in DB ");
    Assert.assertEquals(updatedResponsible, newResponsibleId.toString(), "Responsible is not updated in DB ");
  }

  @Test(description = "Verify that API returns the status of the file is successfully uploaded with new responsible "
      + "in Suspended status", groups = "smoke")
  public void bulkUpdateResponsibleWithSuspendedStatusPositiveTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(initialResponsibleId,
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Change responsible status to Suspended
    AccountEntityManager.updateAccountStatus(newResponsibleId, AccountStatus.SUSPENDED,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //lwaList update responsible
    updateInformationInLwaList(lwaList, null, newResponsibleId,
        DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Retrieve first Node via API
    response = retrieveIotDevice(responsible, lwaList.get(0));

    //Verify first Node all fields
    verifyLwaFields(response, lwaList.get(0), jsonLwaPath);

    //Retrieve second Node via API
    response = retrieveIotDevice(responsible, lwaList.get(1));

    //Verify second Node all fields
    verifyLwaFields(response, lwaList.get(1), jsonLwaPath);
  }

  @Test(description = "Verify that API returns the status of the file is successfully uploaded"
      + "when new responsible has a few statuses", groups = "smoke")
  public void bulkUpdateResponsibleWhenResponsibleHasFewStatusesPositiveTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(initialResponsibleId,
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible with few statuses (AC -> SU -> AC)
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Set account status SU
    AccountState accountState = new AccountState().setAccountId(newResponsibleId)
        .setVtStart(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3))
        .setAccountState(AccountStatus.SUSPENDED);
    AccountEntityServiceMethods.accountStateUpdateRequest(accountState);

    //Set account status AC
    accountState = new AccountState().setAccountId(newResponsibleId)
        .setVtStart(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 5))
        .setAccountState(AccountStatus.ACTIVE);
    AccountEntityServiceMethods.accountStateUpdateRequest(accountState);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
            DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 6),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //lwaList update responsible
    updateInformationInLwaList(lwaList, null, newResponsibleId,
        DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 6));

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Retrieve first Node via API
    response = retrieveIotDevice(responsible, lwaList.get(0));
    //Verify first Node all fields
    verifyLwaFields(response, lwaList.get(0), jsonLwaPath);

    //Retrieve second Node via API
    response = retrieveIotDevice(responsible, lwaList.get(1));
    //Verify second Node all fields
    verifyLwaFields(response, lwaList.get(1), jsonLwaPath);
  }

  @Test(description = "Verify that API returns the status of the file is successfully uploaded for terminated node "
      + "(NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleWhenNodeIsTerminatedNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(initialResponsibleId,
        NodeTypes.NODE_TYPE_CAMERA);

    //Update second Node's status to Terminated
    LightWeightAccount UpdateLwaStatusToTerminated = new LightWeightAccount()
        .setExternalId(lwaList.get(1).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(1).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(UpdateLwaStatusToTerminated).then().statusCode(200);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
            DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Wait until file is not processed
    String requestId = response.body().jsonPath().get("requestId");

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);

    //Prepare expected file results
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "ok");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: Node status in Terminated state");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed");
  }

  @Test(description = "Verify that API reject request if file doesn't contain mandatory header (NegativeTest)",
      groups = "smoke")
  public void bulkUpdateResponsibleWhenAbsentMandatoryHeadersNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaData = new ArrayList<>();
    lwaData.add(new String[] { "someValue" });
    String[] invalidUpdateHeaders = new String[] { "" };
    createFileForUpload(fileName, lwaData, invalidUpdateHeaders);

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        getBulkUpdateHeaders(initialResponsibleId,
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428, "Failed to validate the "
        + "request. Reason: Data row not compliant with json schema: [required key [external_id] not found, extraneous "
        + "key [] is not permitted]");
  }

  @Test(description = "Verify that API reject request if file doesn't contain mandatory value (NegativeTest)",
      groups = "smoke")
  public void bulkUpdateResponsibleWhenAbsentMandatoryValueNegativeTest() {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaDataUpdate = new ArrayList<>();
    lwaDataUpdate.add(new String[] { "" });

    List<String[]> dataUpdate = prepareDataForBulkUploadNodesFile(validUpdateHeaders, lwaDataUpdate);
    createCsvFileWithData(fileName, dataUpdate);

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        getBulkUpdateHeaders(initialResponsibleId,
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Data row not compliant with json schema: "
            + "#/external_id: string [] does not match pattern ^([\\S]+)$");
  }

  @Test(description = "Verify that API reject request if new responsible is archived (NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleWhenNewResponsibleIsArchivedNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START), NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Change Responsible status to Archived
    AccountEntityManager.updateAccountStatusToArchived(newResponsibleId);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName),
            getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
                DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
                CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE)).then().statusCode(428).extract().response();

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Responsible is ineffective on the requested date");
  }

  @Test(description = "Verify that API reject request if old responsible is archived (NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleWhenOldResponsibleIsArchivedNegativeTest() {

    //Create responsible (old)
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer oldResponsibleId = getAccountId(responsibleResponse);

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, oldResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START), NodeTypes.NODE_TYPE_CAMERA);

    //Change Responsible status to Archived
    AccountEntityManager.updateAccountStatusToArchived(oldResponsibleId);

    //Create new responsible
    responsible = new AccountEntityManager().createAccountEntity();
    responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(),
          "Failed to validate the request. Reason: responsible is ineffective on the requested date");
    }};

    //Assert the results is the same
    Assert.assertEquals(getFileProcessedResults(responseBody), expectedFileProcessedResults,
        "File incorrectly processed: ");
  }

  @Test(description = "Verify that API reject request if a new responsible is from system_user (NegativeTest)",
      groups = "smoke")
  public void bulkUpdateResponsibleWhenNewResponsibleFromSystemUserNegativeTest() {

    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START), NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file
    String fileName = createCsvFile(lwaList);

    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        getBulkUpdateHeaders("system_user/admin",
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Responsible is ineffective on the requested date");
  }

  @Test(description = "Verify that API reject request if Responsible isn't change (NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleWhenResponsibleIsNotChangeNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
        NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update same responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(initialResponsibleId.toString(),
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "Failed to validate the request. Reason: Invalid "
          + "request: current responsible is " + initialResponsibleId);
    }};

    //Assert the results is the same
    Assert.assertEquals(getFileProcessedResults(responseBody), expectedFileProcessedResults,
        "File incorrectly processed: ");
  }

  @Test(description = "Verify that API reject request if Responsible is empty (NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenNewResponsibleIsEmptyNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START), NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update same responsible
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), getBulkUpdateResponsibleWithOnlyRequiredHeaders("",
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: ResponsibleId is required for input Schema Type");
  }

  @Test(description = "Verify that API reject request if Responsible is space character (NegativeTest)",
      groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenResponsibleIsSpaceCharacterNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START), NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update same responsible
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), getBulkUpdateResponsibleWithOnlyRequiredHeaders(" ",
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400, "Failed to execute bad "
        + "request. Reason: ResponsibleId is required for input Schema Type");
  }

  @Test(description = "Verify that API reject request when Responsible doesn't exist (NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenResponsibleDoesNotExistNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START), NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update same responsible
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName),
            getBulkUpdateResponsibleWithOnlyRequiredHeaders("12345678901",
                DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
                CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Responsible is not a valid integer: 12345678901");
  }

  @Test(description = "Verify that API request for bulk update responsible reject when start date"
      + "is empty in request (NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenStartDateIsEmptyNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update same responsible
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName),
            new HashMap<String, Object>() {{
              put("responsibleId", newResponsibleId);
              put("schemaType", CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE);
              put("startDate", "");
            }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: StartDate is required for input Schema Type");
  }

  @Test(description = "Verify that API request for bulk update responsible reject when start date is space character "
      + "in request (NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenStartDateIsSpaceCharacterNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update same responsible
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName),
            new HashMap<String, Object>() {{
              put("responsibleId", newResponsibleId);
              put("schemaType", CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE);
              put("startDate", " ");
            }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: StartDate is required for input Schema Type");
  }

  @Test(description = "Verify that API request for bulk update responsible reject when start date contains mistake "
      + "(NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenStartDateContainsMistakeNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update same responsible
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName),
            new HashMap<String, Object>() {{
              put("responsibleId", newResponsibleId);
              put("schemaType", CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE);
              put("startDate", "0019-14-32T10:00:00.000Z");
            }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Invalid date format. Start "
            + "date must be in ISO_LOCAL_DATE_TIME format: yyyy-MM-ddThh:mm:ss");
  }

  @Test(description = "Verify that API reject request startDate before start new Responsible", groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenStartDateBeforeStartNewResponsibleNegativeTest() {

    //Create Nodes
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods
        .createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName),
            getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
                DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
                CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428, "Failed to validate the "
        + "request. Reason: Responsible is ineffective on the requested date");
  }

  @Test(description = "Verify that API returns the status of the processed file for updating node before node created "
      + "(NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleBeforeCreatedDateNegativeTest() {

    //Create 3 Nodes
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
        NodeTypes.NODE_TYPE_CAMERA);

    lwaList.add(createLwa(initialResponsibleId).setStartDate(DateTimeHelper
        .getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3)));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaList.get(1)).then().statusCode(200);

    lwaList.add(createLwa(initialResponsibleId).setStartDate(DateTimeHelper
        .getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START)));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaList.get(2)).then().statusCode(200);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Wait until file is not processed//Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "ok");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: Invalid "
          + "request: start date must be after last update");
      put(lwaList.get(2).getExternalId(), "ok");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults,
        "File incorrectly processed: ");
  }

  @Test(description = "Verify that API returns the status of the processed file for updating node "
      + "before node created (NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleBeforeUpdatedDateNegativeTest() {

    //Create responsible1
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse =
        AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId1 = getAccountId(responsibleResponse);

    //Create 3 Nodes
    List<LightWeightAccount> lwaList = createNodesList(3, responsibleId1,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
        NodeTypes.NODE_TYPE_CAMERA);

    //Create responsible2
    responsible = new AccountEntityManager().createAccountEntity();
    responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId2 = getAccountId(responsibleResponse);

    //Update Responsible in second Node
    LightWeightAccount lwa2Updated = new LightWeightAccount()
        .setExternalId(lwaList.get(1).getExternalId())
        .setResponsibleId(responsibleId2.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(1).getNode().getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa2Updated);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update same responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(initialResponsibleId.toString(),
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "ok");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: Invalid "
          + "request: start date must be after last update");
      put(lwaList.get(2).getExternalId(), "ok");
    }};

    //Assert the results is the same
    Assert.assertEquals(getFileProcessedResults(responseBody), expectedFileProcessedResults,
        "File incorrectly processed: ");
  }

  @Test(description = "Verify that nodes update are failed due to update in one "
      + "day (NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleInTheSameDayNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(initialResponsibleId,
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = getResponseFromUpload(fileName,
        getBulkUpdateResponsibleWithOnlyRequiredHeaders(newResponsibleId.toString(),
            DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE));

    //Wait until file is not processed
    String requestId = response.getBody().jsonPath().get("requestId");

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "Failed to validate the request. Reason: Invalid "
          + "request: start date must be after last update");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: Invalid "
          + "request: start date must be after last update");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults,
        "File incorrectly processed: ");
  }

  @Test(description = "Verify that API reject request when schemaType is wrong (NegativeTest)",
      groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenSchemaTypeIsWrongNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        getBulkUpdateHeaders(newResponsibleId.toString(),
            DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
            CAMERA_CREATE_SCHEMA_TYPE));

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Data row not compliant with "
            + "json schema: #: required key [msisdn] not found");
  }

  @Test(description = "Verify that API reject request when schemaType is missed (NegativeTest)",
      groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenSchemaTypeIsMissedNegativeTest() {

    //Create Node
    List<LightWeightAccount> lwaList = createNodesList(1, initialResponsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START),
        NodeTypes.NODE_TYPE_CAMERA);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer newResponsibleId = getAccountId(responsibleResponse);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update responsible
    Response response = LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        new HashMap<String, Object>() {{
          put("responsibleId", newResponsibleId);
          put("ownershipMode", OwnershipModes.LEASE);
          put("settlement", initialResponsibleId);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "MissingRequestHeaderException: Missing request header 'schemaType' "
            + "for method parameter of type String");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-1078")
  @Test(enabled = false, description = "Verify that API request reject when file is absent in body request"
      + "(NegativeTest)", groups = "smoke")
  public void bulkUpdateResponsibleRejectWhenAbsentFileInBodyNegativeTest() {

    Response response = given()
        .accept("application/json")
        .contentType("multipart/form-data")
        .headers(new HashMap<String, String>() {{
          put("responsibleId", initialResponsibleId.toString());
          put("schemaType", CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE);
        }})
        .log().uri().log().method().log().headers().log().parameters()
        .auth().oauth2(BaseApiTest.token)
        .post(EndPoints.getLightWeightAccountBulkUploadUrl())
        .then().log().everything().extract().response();

    //VerifyAPiresponse
    BaseAssertions.verifyErrorResponse(response, 400,
        "Required request part 'file' is not present");
  }

  private static HashMap<String, Object> getBulkUpdateHeaders(Object responsibleId, String date, String schemaType) {

    return new HashMap<String, Object>() {{
      put("responsibleId", responsibleId);
      put("schemaType", schemaType);
      put("orderId", "newOrd1");
      put("ownershipMode", OwnershipModes.LEASE);
      put("nodeStatus", NodeStatus.SUSPENDED);
      put("settlement", responsibleId);
      put("startDate", parseDate(date));
    }};
  }

  private static HashMap<String, String> getBulkUpdateResponsibleWithOnlyRequiredHeaders(
      String responsibleId, String date, String schemaType) {

    return new HashMap<String, String>() {{
      put("responsibleId", responsibleId);
      put("schemaType", schemaType);
      put("startDate", parseDate(date));
    }};
  }

  private static String parseDate(String date) {

    String dateHeader = DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START);
    if (date != null) {
      dateHeader = date;
    }
    return dateHeader;
  }

  private void updateInformationInLwaList(List<LightWeightAccount> lwaList, HashMap<String, String> headers,
                                          Integer newResponsibleId, String newStartDate) {

    for (LightWeightAccount lightWeightAccount : lwaList) {
      if (headers == null) {
        lightWeightAccount.setResponsibleId(newResponsibleId.toString());
        lightWeightAccount.setStartDate(newStartDate);
      } else {
        if (headers.containsKey("ResponsibleId"))
          lightWeightAccount.setResponsibleId(headers.get("ResponsibleId"));
        if (headers.containsKey("startDate"))
          lightWeightAccount.setStartDate(headers.get("startDate"));
        if (headers.containsKey("nodeStatus"))
          lightWeightAccount.setNodeStatus(headers.get("nodeStatus"));
        if (headers.containsKey("ownerShipMode"))
          lightWeightAccount.setOwnershipMode(headers.get("ownerShipMode"));
        if (headers.containsKey("orderId"))
          lightWeightAccount.setOrderId(headers.get("orderId"));
        if (headers.containsKey("settlementId"))
          lightWeightAccount.setSettlementId(headers.get("settlementId"));
      }
    }
  }

  private String createCsvFile(List<LightWeightAccount> lwaList) {

    String fileName = getFileName(BULK_UPLOAD_PREFIX);

    List<String[]> lwaDataUpdate = new ArrayList<>();
    for (LightWeightAccount lightWeightAccount : lwaList) {
      lwaDataUpdate.add(new String[] { lightWeightAccount.getExternalId() });
    }
    List<String[]> dataUpdate = prepareDataForBulkUploadNodesFile(validUpdateHeaders, lwaDataUpdate);
    createCsvFileWithData(fileName, dataUpdate);
    return fileName;
  }

  private Response retrieveIotDevice(AccountEntity responsible, LightWeightAccount lwa) {
    //Get token for responsible (payer)
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, String.format("%s/%s", responsible.getNameSpace(),
            responsible.getUserName()), defaultPassword);

    //retrieve IOT Device (LWA)
    return LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);
  }

  private void verifyLwaFields(Response response, LightWeightAccount lwa, String lwaPath) {

    verifyGetLwaFields(response, lwaPath, getExpectedLwaFields(lwa));

    String nodePath = lwaPath + "node.";
    verifyGetLwaFields(response, nodePath, new HashMap<String, Object>() {{
      put("nodeType", lwa.getNode().getNodeType());
    }});

    String nodeAttributesPath = nodePath + "attributes.";
    verifyGetNodeAttributes(response, nodeAttributesPath, getExpectedNodeFields(lwa.getNode()));
  }

  private static HashMap<String, Object> getExpectedLwaFields(LightWeightAccount lwa) {

    return new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwa.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }};
  }

  private static HashMap<String, Object> getExpectedNodeFields(LightWeightAccount.Node node) {

    return new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }};
  }
}

