package bluesky.restapi.lightWeightAccount.updateIotDevice;

import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.AccountStatus;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.NodeStatus;
import bluesky.restapi.models.OwnershipModes;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.HashMap;

import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;

@Story("BLUESKY-18 LW Account Backend - Update IOT Device Status - Terminated")
public class UpdateIotDeviceStatusTerminatedTest extends BaseUpdateIotDevice {

  private static String MT_MAX_DATE;

  @BeforeClass
  public void initialize() {

    MT_MAX_DATE = DateTimeHelper.getMaxDateValueDb();
  }

  @Test(description = "Verify that the status of the IOT device CAMERA is updated as terminated, and the API returns "
      + "success (Positive Test)", groups = "smoke")
  public void updateNodeStatusToTerminatedPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(200);
  }

  @Test(description = "Update IOT CAMERA Status on Previous Date (positive Test)", groups = "smoke")
  public void updateNodeStatusOnPreviousDatePositiveTest() {

    //Create new responsible on previous
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 7));

    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 6));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 5))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(200);
  }

  @Test(description = "Verify the request IOT Device CAMERA update to status terminated was rejected when node not "
      + "exist (Negative Test)", groups = "smoke")
  public void updateNodeStatusToTerminatedwhenNodeNotExistNegativeTest() {

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId("notExistingNode1234567890")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType("CAMERA"))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then()
        .statusCode(428);
  }

  @Test(description = "Checking the request is rejected when the Node is already in terminated status. (Negative Test)",
      groups = "smoke")
  public void requestRejectedNodeAlreadyTermNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaAlreadyUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaAlreadyUpdatedwithTerminatedStatus).then()
        .statusCode(428);
  }

  @Test(description = "Update Node status to Terminated when responsible is in Status Archived (Negative Test)",
      groups = "smoke")
  public void requestRejectUpdateNodeTerminatedResponsibleArchivedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Responsible status to 'Archived' in DB
    DBHelper.updateAccountStatusInDB(responsibleId, AccountStatus.ARCHIVED);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedWithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedWithTerminatedStatus).then().statusCode(428);
  }

  @Test(description = "Update Node Status to Active when Responsible is in Status Archived (Negative Test)", groups = "smoke")
  public void requestRejectUpdateNodeActiveResponsibleArchivedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Suspend
    LightWeightAccount lwaUpdatedWithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedWithSuspendedStatus).then().statusCode(200);

    //Update Responsible status to 'Archived' in DB
    DBHelper.updateAccountStatusInDB(responsibleId, AccountStatus.ARCHIVED);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(428);
  }

  @Test(description = "Update node status to Terminated when node was updated for this day(Negative Test)", groups = "smoke")
  public void requestRejectUpdateNodeStatusForThisDayNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(200);

    //Update Node's status to Terminated with another timestamp
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(428);
  }

  @Test(description = "Checking that Updated Node information (history) is updated in DB (Positive Test)", groups = "smoke")
  public void checkingNodeTerminatedHistoryInDBPositiveTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(200);

    //Getting node history from DB
    String nodeHistorySuspendQuery = String
        .format("select * from t_lwa_node where external_id = '%s' and status = 'Suspended'",
            lwaUpdatedwithSuspendedStatus.getExternalId());

    String endDate = DBHelper.getValueFromDB(nodeHistorySuspendQuery, "end_date");

    String nodeHistoryTerminateQuery = String
        .format("select * from t_lwa_node where external_id = '%s' and status = 'Terminated'",
            lwaUpdatedwithSuspendedStatus.getExternalId());

    String startDate = DBHelper.getValueFromDB(nodeHistoryTerminateQuery, "start_date");

    Assert.assertEquals(endDate, DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_END_DB));
    Assert.assertEquals(startDate, DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START_DB, 2));

  }

  @Test(description = "Checking that Node has End_date with MTMaxDate Value both in DB and API response after update "
      + "Status to Terminated (Positive Test)", groups = "smoke")
  public void updateTerminatedMtMaxDateInResponseAndDBPositiveTest() throws Exception {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus);

    //Getting node MTMaxDate Value from DB
    response.then().statusCode(200);
    Assert.assertEquals(response.body().jsonPath().get("endDate"),
        DateTimeHelper.convertToAPIFormat(MT_MAX_DATE));

    //Getting node MTMaxDate Value from DB
    String nodeEndDate = String.format("select * from t_lwa_node where external_id = '%s' and status = 'Terminated'",
        lwaUpdatedwithTerminatedStatus.getExternalId());
    String endMaxDate = DBHelper.getValueFromDB(nodeEndDate, "end_date");
    Assert.assertEquals(endMaxDate, MT_MAX_DATE);
  }

  @Test(description = "Checking that request rejected when updating note before node is created (Negative Test", groups = "smoke")
  public void updateNodeTerminatedBeforeCreatedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Terminated yesterday
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(428);
  }

  @Test(description = "Checking request Node Status TErminated Before last Update (Negative Test)", groups = "smoke")
  public void updateNodeTerminatedBeforeLastUodateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "updatedValueMSISDN");
            }}));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(200);

    //Update Node's status to Terminated yesterday
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwaUpdated.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(428);

  }

  @Test(description = "Checking that request Rejected when Updating node Status and Responsible in 1 request (Negative "
      + "Test)", groups = "smoke")
  public void updateNodeRejectedWhenUpdateTerminatedAndUpdateResponsibleNegativeTest() {

    //Create new responsible 1
    AccountEntity responsible1 = new AccountEntityManager().createAccountEntity();
    Response responsible1Response = AccountEntityServiceMethods.createAccountEntityRequest(responsible1);
    Integer responsible1Id = getAccountId(responsible1Response);

    //Create new responsible 2
    AccountEntity responsible2 = new AccountEntityManager().createAccountEntity();
    Response responsible2Response = AccountEntityServiceMethods.createAccountEntityRequest(responsible2);
    Integer responsible2Id = getAccountId(responsible2Response);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsible1Id.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Terminated and update Responsible
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(responsible2Id.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(428);
  }

  @Test(description = "Checking that request rejected when uodating node Status to Terminated with Methode of Sale, "
      + "Settlement and OrderID in 1 request(Negative Test", groups = "smoke")
  public void updateRejectStatusAndMOSSetlementOrderIDinOneRequestNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED)
        .setOwnershipMode(OwnershipModes.RENT)
        .setOrderId("updated")
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(428);
  }

  @Test(description = "Checking the request rejected when updating Node Status and Attributes in 1 request (Negative "
      + "test)", groups = "smoke")
  public void updateRejectedWhenStatusAndAttributesinResponseNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes and Status Terminated
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNodeStatus(NodeStatus.TERMINATED)
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "updatedValueMSISDN");
              put("imsi", "updatedValueIMSI");
            }}));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Checking that Node Status Terminated Could be successfully updated in future (Positive Test)",
      groups = "smoke")
  public void updateNodeStatusTerminatedInFuturePositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1))
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "updatedValueMSISDN");
            }}));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwaUpdated.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(200);
  }

  @Test(description =
      "Verify that the status of the IOT device AIRSENSOR is updated as terminated, and the API returns "
          + "success (Positive Test)", groups = "smoke")
  public void updateNodeAirSensorStatusToTerminatedPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode()
        .setNodeType("AIRSENSOR")
        .setAttributes(airSensorAttributes);

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(200);
  }

  @Test(description =
      "Checking Node AirSensor that request rejected when updating Status and guiding attributes MSISDN "
          + "(Negative Test)", groups = "smoke")
  public void updateRejectedNodeAirsensorTErminatedWithMsisdnNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode()
        .setNodeType("AIRSENSOR")
        .setAttributes(airSensorAttributes);

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes and Status Terminated
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNodeStatus(NodeStatus.TERMINATED)
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "updatedValueMSISDN");
            }}));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Request rejected when updating status IOT Camera status with timestamp (Negative Test)",
      groups = "smoke")
  public void updateRejectedNodeCameraTerminatedWithTimeStampNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus).then().statusCode(428);
  }

}
