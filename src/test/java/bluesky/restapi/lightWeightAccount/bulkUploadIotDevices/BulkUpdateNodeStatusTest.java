package bluesky.restapi.lightWeightAccount.bulkUploadIotDevices;

import bluesky.restapi.base.BaseAssertions;
import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
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

import static bluesky.restapi.base.BaseAssertions.verifyErrorResponse;
import static bluesky.restapi.helpers.FileProcessingHelper.createCsvFileWithData;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;

@Story("BLUESKY- 605 Bulk Backend - Upload Validation Logic - Bulk Update Status")
public class BulkUpdateNodeStatusTest extends BaseBulkUploadNodes {

  private Integer responsibleId;
  private Integer responsibleId2;

  private String querySuspended = SELECT_NODE_QUERY + "and status = 'Suspended'";
  private String queryTerminated = SELECT_NODE_QUERY + "and status = 'Terminated'";

  @BeforeClass
  private void createResponsible() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    this.responsibleId = getAccountId(responsibleResponse);

    //Add new account
    AccountEntity responsible2 = new AccountEntityManager().createAccountEntity();
    Response responsible2Response = AccountEntityServiceMethods.createAccountEntityRequest(responsible2);
    this.responsibleId2 = getAccountId(responsible2Response);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-1023")
  @Test(description = "Bulk change Nodes Status to Suspended with minimal mandatory fields and history in DB",
      groups = "smoke")
  public void bulkChangeNodeStatusToSuspendedPositiveTest() throws SQLException {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String nodeStatusSuspended1 = String.format(querySuspended, lwaList.get(0).getExternalId());
    String node1Status = DBHelper.getValueFromDB(nodeStatusSuspended1, "status");

    String nodeStatusSuspended2 = String.format(querySuspended, lwaList.get(1).getExternalId());
    String node2Status = DBHelper.getValueFromDB(nodeStatusSuspended2, "status");

    Assert.assertEquals(node1Status, "Suspended", "Node status is not updated in DB");

    Assert.assertEquals(node2Status, "Suspended", "Node status is not updated in DB");
  }

  @Test(description = "Bulk change Nodes Status to Active with minimal mandatory fields and history in DB (Positive "
      + "Test)", groups = "smoke")
  public void bulkChangeNodeStatusToActivePositiveTest() throws SQLException {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedWithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(0).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(0).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedWithSuspendedStatus).then()
        .statusCode(200);

    LightWeightAccount lwa2UpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(1).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(1).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa2UpdatedwithSuspendedStatus).then()
        .statusCode(200);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String queryActive = SELECT_NODE_QUERY + "and status = 'Active'";
    String nodeStatusSuspended1 = String.format(queryActive, lwaList.get(0).getExternalId());

    String node1Status = DBHelper.getValueFromDB(nodeStatusSuspended1, "status");

    String nodeStatusSuspended2 = String.format(queryActive, lwaList.get(1).getExternalId());

    String node2Status = DBHelper.getValueFromDB(nodeStatusSuspended2, "status");

    Assert.assertEquals(node1Status, "Active", "Node status is not updated in DB");

    Assert.assertEquals(node2Status, "Active", "Node status is not updated in DB");
  }

  @Test(description = "Bulk change Nodes status to SUSPENDED with NULL instead StartDate in request (Negative Test)",
      groups = "smoke")
  public void bulkChangeNodesStatustoSuspendedWithNullStartDateNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", null);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Invalid date format. Start date must be in "
            + "ISO_LOCAL_DATE_TIME format: yyyy-MM-ddThh:mm:ss");
  }

  @Test(description = "Bulk change Nodes status to SUSPENDED when Nodes already in the same status", groups = "smoke")
  public void bulkChangeNodesStatusWhenSameStatusNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedWithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(0).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(0).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedWithSuspendedStatus).then()
        .statusCode(200);

    LightWeightAccount lwa2UpdatedWithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(1).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(1).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa2UpdatedWithSuspendedStatus).then()
        .statusCode(200);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(),
          "Failed to validate the request. Reason: Invalid request: Requested node status should not be same as exist "
              + "node status");
      put(lwaList.get(1).getExternalId(),
          "Failed to validate the request. Reason: Invalid request: Requested node status should not be same as exist "
              + "node status");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed: ");
  }

  @Test(description = "Bulk change Nodes status to SUSPENDED and also change ResponsibleID, Ownership, SettlementID,"
      + "OrderId in one request.", groups = "smoke")
  public void bulkChangeNodesStatusSuspendedWithAllFieldsPositiveTest() throws SQLException {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update the nodes
    String fileNameUpdate = getFileName("BulkUpdate");

    List<String[]> lwaDataUpdate = new ArrayList<>();

    lwaDataUpdate.add(new String[] { lwaList.get(0).getExternalId(), "msisdnup", "oem", "make", "model" });
    lwaDataUpdate.add(new String[] { lwaList.get(1).getExternalId(), "msisdnup", "oem", "make", "model" });

    createFileForUpload(fileNameUpdate, lwaDataUpdate, validCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileNameUpdate), new HashMap<String, String>() {{
          put("responsibleId", responsibleId2.toString());
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
          put("ownershipMode", "LEASE");
          put("orderId", "str1");
          put("settlementId", responsibleId2.toString());
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String nodeStatusSuspended1 = String.format(querySuspended, lwaList.get(0).getExternalId());

    String node1Status = DBHelper.getValueFromDB(nodeStatusSuspended1, "status");

    String nodeStatusSuspended2 = String.format(querySuspended, lwaList.get(1).getExternalId());

    String node2Status = DBHelper.getValueFromDB(nodeStatusSuspended2, "status");

    Assert.assertEquals(node1Status, "Suspended", "Node status is not updated in DB");

    Assert.assertEquals(node2Status, "Suspended", "Node status is not updated in DB");
  }

  @Test(description = "Bulk change Nodes status to SUSPENDED with empty csv file in request (NegativeTest)", groups =
      "smoke")
  public void bulkBulkChangeNodesStatusEmptyFileNegativeTest() {

    String fileName = getFileName("BulkUpload");

    List<String[]> lwaData = new ArrayList<>();
    String[] emptyHeaders = new String[0];

    createFileForUpload(fileName, lwaData, emptyHeaders);

    //Upload empty file
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
        }});

    BaseAssertions.verifyErrorResponse(response, 400, "Failed to execute bad "
        + "request. Reason: Invalid file content");
  }

  @Test(description = "Bulk change Nodes status to Suspended when it status already was updated to Suspended for this "
      + "day (Negative Test)", groups = "smoke")
  public void bulkChangeNodesStatusSameDayNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(0).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(0).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then()
        .statusCode(200);

    LightWeightAccount lwa2UpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(1).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(1).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa2UpdatedwithSuspendedStatus).then()
        .statusCode(200);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "Failed to validate the request. Reason: Invalid request: start date must "
          + "be after last update");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: Invalid request: start date must "
          + "be after last update");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File "
        + "incorrectly processed: ");
  }

  @Test(description = "Bulk change Nodes Status to Suspended before Nodes created (Negative Test)", groups = "smoke")
  public void bulkChangeNodesStatusBeforeNodesCreatedNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "Failed to validate the request. Reason: Invalid request: start date must "
          + "be after last update");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: Invalid request: start date must "
          + "be after last update");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed: ");
  }

  @Test(description = "Bulk change Nodes status to ACTIVE when Account Status is ARCHIVED (Negative Test)", groups =
      "smoke")
  public void bulkChangeNodesStatusAccountArchivedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleIdArchived = getAccountId(responsibleResponse);

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleIdArchived,
        NodeTypes.NODE_TYPE_CAMERA);

    //Update Node's status to Suspended    
    LightWeightAccount lwaUpdatedWithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(0).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(0).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedWithSuspendedStatus).then()
        .statusCode(200);

    LightWeightAccount lwa2UpdatedWithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(1).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(1).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa2UpdatedWithSuspendedStatus).then()
        .statusCode(200);

    //Change Responsible status to Archived
    AccountEntityManager.updateAccountStatusToArchived(responsibleIdArchived);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "Failed to validate the request. Reason: responsible is ineffective "
          + "on the requested date");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: responsible is ineffective "
          + "on the requested date");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File "
        + "incorrectly processed: ");
  }

  @Test(description =
      "Checking that the Bulk change status to Suspended is rejected when Nodes with type CAMERA in csv "
          + "file does not exist, but exists nodes with same external id with type AirSensor (Negative Test)", groups =
      "smoke")
  public void bulkChangeNodesStatusWhenNodeNotExistNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_AIRSENSOR);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(),
          "Failed to validate the request. Reason: Node not found for the provided external id and node type");
      put(lwaList.get(1).getExternalId(),
          "Failed to validate the request. Reason: Node not found for the provided external id and node type");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly "
        + "processed: ");
  }

  @Test(description = "Checking that Bulk change status to Suspended successfully updated one year later(Positive test",
      groups = "smoke")
  public void bulkChangeNodesStatusYearLaterPositiveTest() throws SQLException {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 365));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String nodeStatusSuspended1 = String.format(querySuspended, lwaList.get(0).getExternalId());

    String node1Status = DBHelper.getValueFromDB(nodeStatusSuspended1, "status");

    String nodeStatusSuspended2 = String.format(querySuspended, lwaList.get(1).getExternalId());

    String node2Status = DBHelper.getValueFromDB(nodeStatusSuspended2, "status");

    Assert.assertEquals(node1Status, "Suspended", "Node status is not updated in DB");

    Assert.assertEquals(node2Status, "Suspended", "Node status is not updated in DB");
  }

  @Test(description = "Bulk change AIRSENSOR Nodes status to SUSPENDED with only mandatory fields in header and "
      + "history updated in DB (Positive Test)", groups = "smoke")
  public void bulkChangeStatusAirSensorOnlyMandatoryPositiveTest() throws SQLException {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_AIRSENSOR);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", AIRSENSOR_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String nodeStatusSuspended1 = String.format(querySuspended, lwaList.get(0).getExternalId());

    String node1Status = DBHelper.getValueFromDB(nodeStatusSuspended1, "status");

    String nodeStatusSispended2 = String.format(querySuspended, lwaList.get(1).getExternalId());

    String node2Status = DBHelper.getValueFromDB(nodeStatusSispended2, "status");

    Assert.assertEquals(node1Status, "Suspended", "Node status is not updated in DB");

    Assert.assertEquals(node2Status, "Suspended", "Node status is not updated in DB");
  }

  @Test(description = "Bulk change Nodes status to SUSPENDED and also change ResponsibleID, Ownership, SettlementID, "
      + "OrderId in one request(Positive Test)", groups = "smoke")
  public void bulkChangeStatusWithAllAttributesPositiveTest() throws SQLException {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_AIRSENSOR);

    //Update the nodes
    String fileNameUpdate = getFileName("BulkUpdate");

    List<String[]> lwaDataUpdate = new ArrayList<>();

    lwaDataUpdate.add(new String[] { lwaList.get(0).getExternalId(), "msisdnup", "oemup", "makeup", "modelup" });
    lwaDataUpdate.add(new String[] { lwaList.get(1).getExternalId(), "msisdnup", "oemup", "makeup", "modelup" });

    createFileForUpload(fileNameUpdate, lwaDataUpdate, validAirsensorFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileNameUpdate), new HashMap<String, String>() {{
          put("responsibleId", responsibleId2.toString());
          put("schemaType", AIRSENSOR_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
          put("ownershipMode", "LEASE");
          put("orderId", "str1");
          put("settlementId", responsibleId2.toString());
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String nodeStatusSuspended1 = String.format(querySuspended, lwaList.get(0).getExternalId());

    String node1Status = DBHelper.getValueFromDB(nodeStatusSuspended1, "status");

    String nodeStatusSispended2 = String.format(querySuspended, lwaList.get(1).getExternalId());

    String node2Status = DBHelper.getValueFromDB(nodeStatusSispended2, "status");

    Assert.assertEquals(node1Status, "Suspended", "Node status is not updated in DB");

    Assert.assertEquals(node2Status, "Suspended", "Node status is not updated in DB");
  }

  @Test(description = "Bulk upload nodes without status of Nodes in the request (Negative Test)", groups = "smoke")
  public void bulkRequestWithoutStatusInRequestNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Assert update is rejected
    verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Status is required for input Schema Type");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-1076")
  @Test(description = "Bulk change Node Status to Terminate (Positive test)", groups = "smoke")
  public void bulkChangeStatusToTerminatePositiveTest() throws SQLException {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_TERMINATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String nodeStatusTerminated1 = String.format(queryTerminated, lwaList.get(0).getExternalId());

    String node1Status = DBHelper.getValueFromDB(nodeStatusTerminated1, "status");

    String nodeStatusTerminated2 = String.format(queryTerminated, lwaList.get(1).getExternalId());

    String node2Status = DBHelper.getValueFromDB(nodeStatusTerminated2, "status");

    Assert.assertEquals(node1Status, "Terminated", "Node status is not updated in DB");

    Assert.assertEquals(node2Status, "Terminated", "Node status is not updated in DB");
  }

  @Test(description = "Bulk change Nodes status to TERMINATED when Nodes already in the same status (Negative Test)",
      groups = "smoke")
  public void bulkChangeStatusTerminatedWhenAlreadyTerminatedNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedWithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(0).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(0).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedWithTerminatedStatus).then()
        .statusCode(200);

    LightWeightAccount lwa2UpdatedWithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(1).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(1).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa2UpdatedWithTerminatedStatus).then()
        .statusCode(200);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_TERMINATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "Failed to validate the request. Reason: Node status in Terminated state");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: Node status in Terminated state");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed: ");
  }

  @Test(description = "Bulk change Nodes status to TERMINATED with NULL instead StartDate in request(Negative Test)",
      groups = "smoke")
  public void bulkTerminateWhenStartDateNullNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_TERMINATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", null);
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Invalid date format. Start date must be in "
            + "ISO_LOCAL_DATE_TIME format: yyyy-MM-ddThh:mm:ss");
  }

  @Test(description = "Bulk change Nodes status to TERMINATED when Account Status is ARCHIVED (Negative Test)",
      groups = "smoke")
  public void bulkNodesTerminateWhenAccountArchivedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleIdArchived = getAccountId(responsibleResponse);

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleIdArchived,
        NodeTypes.NODE_TYPE_CAMERA);

    //Change Responsible status to Archived
    AccountEntityManager.updateAccountStatusToArchived(responsibleIdArchived);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_TERMINATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "Failed to validate the request. Reason: responsible is ineffective "
          + "on the requested date");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: responsible is ineffective "
          + "on the requested date");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File "
        + "incorrectly processed: ");
  }

  @Test(description = "Checking that request rejected when update node status to Terminated with timestamp(Negative Test)",
      groups = "smoke")
  public void bulkTerminateWithTimestampNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_TERMINATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(),
          "Failed to validate the request. Reason: Invalid request: Requested date should not include timestamp while "
              + "terminating node");
      put(lwaList.get(1).getExternalId(),
          "Failed to validate the request. Reason: Invalid request: Requested date should not include timestamp while "
              + "terminating node");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed: ");
  }

  @Test(description = "Checking that request rejected when Bulk Change status to Active when Node in Terminated "
      + "status(negative test)", groups = "smoke")
  public void bulkChangeStatusToActiveWhenTerminatedNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedWithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(0).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(0).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedWithTerminatedStatus).then()
        .statusCode(200);

    LightWeightAccount lwa2UpdatedWithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwaList.get(1).getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(lwaList.get(1).getNode().getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa2UpdatedWithTerminatedStatus).then()
        .statusCode(200);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.ACTIVE);
          put("startDate", DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), "Failed to validate the request. Reason: Node status in Terminated state");
      put(lwaList.get(1).getExternalId(), "Failed to validate the request. Reason: Node status in Terminated state");
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File incorrectly processed: ");
  }

  @Test(description = "Bulk change ARISENSOR Nodes status to TERMINATED with only mandatory fields in header and "
      + "history updated in DB (Positive Test", groups = "smoke")
  public void bulkTerminateAirSensorOnlyMandatoryPositiveTest() throws SQLException {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId,
        NodeTypes.NODE_TYPE_AIRSENSOR);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", AIRSENSOR_TERMINATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String nodeStatusSuspended1 = String.format(queryTerminated, lwaList.get(0).getExternalId());

    String node1Status = DBHelper.getValueFromDB(nodeStatusSuspended1, "status");

    String nodeStatusSispended2 = String.format(queryTerminated, lwaList.get(1).getExternalId());

    String node2Status = DBHelper.getValueFromDB(nodeStatusSispended2, "status");

    Assert.assertEquals(node1Status, "Terminated", "Node status is not updated in DB");

    Assert.assertEquals(node2Status, "Terminated", "Node status is not updated in DB");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-1128")
  @Test(description = "Bulk Change Nodes Status to Suspended with Bulk_Terminate Schema Type (Negative test)",
      groups = "smoke")
  public void bulkChangeCameraToSuspendedWithTerminateSchemaNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update the nodes
    String fileNameUpdate = getFileName("BulkUpdate");

    List<String[]> lwaDataUpdate = new ArrayList<>();

    lwaDataUpdate.add(new String[] { lwaList.get(0).getExternalId() });
    lwaDataUpdate.add(new String[] { lwaList.get(1).getExternalId() });

    createFileForUpload(fileNameUpdate, lwaDataUpdate, validCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileNameUpdate), new HashMap<String, String>() {{
          put("schemaType", CAMERA_TERMINATE_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400, "Failed to execute bad "
        + "request. Reason: Status SUSPENDED is invalid for input Schema Type");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-1121")
  @Test(enabled = false, description = "Bulk Change Nodes Status to Terminated with Bulk Status Change Schema Type "
      + "(Negative test)", groups = "smoke")
  public void bulkCameraTerminateWithBulkStatusChangeSchemaNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update the nodes
    String fileNameUpdate = getFileName("BulkUpdate");

    List<String[]> lwaDataUpdate = new ArrayList<>();

    lwaDataUpdate.add(new String[] { lwaList.get(0).getExternalId() });
    lwaDataUpdate.add(new String[] { lwaList.get(1).getExternalId() });

    createFileForUpload(fileNameUpdate, lwaDataUpdate, validCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileNameUpdate), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400, "Failed to execute bad "
        + "request. Reason: Reason: Invalid Schema Type");
  }

  @Test(description = "Bulk change Nodes status to SUSPENDED with Same Ownership in one request (Negative Test)",
      groups = "smoke")
  public void bulkChangeNodesStatusSuspendedWithSameOwnershipNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update the nodes
    String fileNameUpdate = getFileName("BulkUpdate");

    List<String[]> lwaDataUpdate = new ArrayList<>();

    lwaDataUpdate.add(new String[] { lwaList.get(0).getExternalId() });
    lwaDataUpdate.add(new String[] { lwaList.get(1).getExternalId() });

    createFileForUpload(fileNameUpdate, lwaDataUpdate, validCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileNameUpdate), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
          put("ownershipMode", OwnershipModes.OWNER);
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String expectedResult = "Failed to validate the request. Reason: Invalid request: current ownership mode is Owner";
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), expectedResult);
      put(lwaList.get(1).getExternalId(), expectedResult);
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File "
        + "incorrectly processed: ");
  }

  @Test(description = "Bulk change Nodes status to SUSPENDED with Same ResponsibleID in one request(Negative test)",
      groups = "smoke")
  public void bulkChangeNodesStatusSuspendedWithSameResponsibleIDNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update the nodes
    String fileNameUpdate = getFileName("BulkUpdate");

    List<String[]> lwaDataUpdate = new ArrayList<>();

    lwaDataUpdate.add(new String[] { lwaList.get(0).getExternalId() });
    lwaDataUpdate.add(new String[] { lwaList.get(1).getExternalId() });

    createFileForUpload(fileNameUpdate, lwaDataUpdate, validCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileNameUpdate), new HashMap<String, String>() {{
          put("responsibleId", responsibleId.toString());
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String expectedResult = String
        .format("Failed to validate the request. Reason: Invalid request: current responsible is %s",
            responsibleId.toString());
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), expectedResult);
      put(lwaList.get(1).getExternalId(), expectedResult);
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File "
        + "incorrectly processed: ");
  }

  @Test(description = "Bulk change Nodes status to SUSPENDED with Same Settlement in one request (Negative test)",
      groups = "smoke")
  public void bulkChangeNodesStatusSuspendedWithSameSettlementNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Update the nodes
    String fileNameUpdate = getFileName("BulkUpdate");

    List<String[]> lwaDataUpdate = new ArrayList<>();
    lwaDataUpdate.add(new String[] { lwaList.get(0).getExternalId() });
    lwaDataUpdate.add(new String[] { lwaList.get(1).getExternalId() });

    createFileForUpload(fileNameUpdate, lwaDataUpdate, validCameraFileHeaders);

    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileNameUpdate), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
          put("settlementId", responsibleId.toString());
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Get file status and assert the error code
    String expectedResult = String.format("Failed to validate the request. Reason: Invalid request: current settlement "
        + "party is %s", responsibleId.toString());
    String responseBody = getBulkUploadResponseBody(requestId);
    HashMap<String, String> expectedFileProcessedResults = new HashMap<String, String>() {{
      put(lwaList.get(0).getExternalId(), expectedResult);
      put(lwaList.get(1).getExternalId(), expectedResult);
    }};

    HashMap<String, String> actualFileProcessedResults = getFileProcessedResults(responseBody);

    //Assert the results is the same
    Assert.assertEquals(actualFileProcessedResults, expectedFileProcessedResults, "File "
        + "incorrectly processed: ");
  }

  @Test(description = "Bulk change Nodes status to SUSPENDED in past (Positive Test)", groups = "smoke")
  public void bulkChangeStatusToSuspendedCameraInPastPositiveTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3));
    Response responsibleResponse =
        AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleIdInPast = getAccountId(responsibleResponse);

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createNodesList(2, responsibleIdInPast,
        DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2), NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_UPDATE_STATUS_SCHEMA_TYPE);
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("startDate", DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));
        }});

    //Verify APi response
    String requestId = response.getBody().jsonPath().get("requestId");

    //Wait until file is processed
    LightWeightAccountServiceMethods.downloadProcessedFile(requestId);

    //Query for DB
    String nodeStatusSuspended1 = String.format(querySuspended, lwaList.get(0).getExternalId());
    String node1Status = DBHelper.getValueFromDB(nodeStatusSuspended1, "status");

    String nodeStatusSuspended2 = String.format(querySuspended, lwaList.get(1).getExternalId());
    String node2Status = DBHelper.getValueFromDB(nodeStatusSuspended2, "status");

    Assert.assertEquals(node1Status, "Suspended", "Node status is not updated in DB");

    Assert.assertEquals(node2Status, "Suspended", "Node status is not updated in DB");
  }

  @Test(description = "Bulk Change Status to Terminate and Change responsibleId (Negative test)")
  public void bulkTerminateWithResponsibleIdNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_TERMINATE_SCHEMA_TYPE);
          put("responsibleId", responsibleId2.toString());
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: ResponsibleId is invalid for input Schema Type");
  }

  @Test(description = "Bulk Change Status to Terminate and Change SettlementId (Negative Test)", groups = "smoke")
  public void bulkTerminateCameraWithSettlementNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_TERMINATE_SCHEMA_TYPE);
          put("settlementId", responsibleId2.toString());
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Settlement is invalid for input Schema Type");
  }

  @Test(description = "Bulk Terminate Nodes Camera and Change Ownership (Negative test)", groups = "smoke")
  public void bulkTerminateWithOwnershipNegativeTest() {

    //Create 2 Nodes
    List<LightWeightAccount> lwaList = createTwoNodesListStartCurrentDate(responsibleId, NodeTypes.NODE_TYPE_CAMERA);

    //Create csv file with Node's externalId
    String fileName = createCsvFile(lwaList);

    //Bulk update Status
    Response response = LightWeightAccountServiceMethods
        .bulkUploadNodes(new File(fileName), new HashMap<String, String>() {{
          put("schemaType", CAMERA_TERMINATE_SCHEMA_TYPE);
          put("ownershipMode", OwnershipModes.LEASE);
          put("nodeStatus", NodeStatus.TERMINATED);
          put("startDate", DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
        }});

    //Verify APi response
    verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Ownership is invalid for input Schema Type");
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

}
