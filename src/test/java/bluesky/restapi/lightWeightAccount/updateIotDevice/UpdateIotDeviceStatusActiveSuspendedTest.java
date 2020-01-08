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
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.HashMap;

import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;

@Story("BLUESKY-36 LW Account Backend - Update IOT Device Status - Active or Suspended")
public class UpdateIotDeviceStatusActiveSuspendedTest extends BaseUpdateIotDevice {

  @Test(description = "Checking that the request with status Suspended is rejected when Node does not exist(Negative "
      + "Test)", groups = "smoke")
  public void requestRejectedWithStatusSuspendedWhenNodeNotExistNegativeTest() {

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId("notExistingNode1234567890")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType("CAMERA"))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then()
        .statusCode(428);
  }

  @Test(description = "Checking the request with status Suspended is rejected when the Node is in Terminated status "
      + "(Negative Test)", groups = "smoke")
  public void requestRejectedNodeTerminatedStatusNegativeTest() {

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

    //Update Node's status to Suspended
    LightWeightAccount lwaAlreadyUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaAlreadyUpdatedwithSuspendedStatus).then()
        .statusCode(428);
  }

  @Test(description = "Update Node status to Active when responsible is in Status Archived(Negative Test)", groups =
      "smoke")
  public void requestRejectedWithStatusActiveWhenResponsibleArchivedNegativeTest() {

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

    //Update Responsible status to 'Archived' in DB
    DBHelper.updateAccountStatusInDB(responsibleId, AccountStatus.ARCHIVED);

    //Update Node's status to Active
    LightWeightAccount lwaUpdatedwithActiveStatus = new LightWeightAccount()
        .setExternalId(lwaUpdatedwithSuspendedStatus.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithActiveStatus).then().statusCode(428);
  }

  @Test(description = "Update Node status to Suspended when responsible is in Status Archived(Negative Test)",
      groups = "smoke")
  public void requestRejectedWithStatusSuspendedWhenResponsibleArchived() {

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

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then()
        .statusCode(428);
  }

  @Test(description = "Checking that the request with status Suspended is rejected when Node does not exist, but exist "
      + "node with same external id with type AirSensor(Negative Test)", groups = "smoke")
  public void requestRejectedIOTCameraNotExistButArisensorSameExternalIDNegativeTest() {

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

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType("CAMERA"))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(428);
  }

  @Test(description = "Checking that node status history updated in DB(Positive Test)", groups = "smoke")
  public void checkingNodeStatusSuspendedActiveHistoryInDB() throws SQLException {

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

    //Update Node's status to Active
    LightWeightAccount lwaUpdatedwithActiveStatus = new LightWeightAccount()
        .setExternalId(lwaUpdatedwithSuspendedStatus.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithActiveStatus).then().statusCode(200);

    //Getting node history from DB
    String nodeHistorySuspendQuery = String
        .format("select * from t_lwa_node where external_id = '%s' and status = 'Suspended'",
            lwaUpdatedwithSuspendedStatus.getExternalId());

    String endDate = DBHelper.getValueFromDB(nodeHistorySuspendQuery, "end_date");

    String nodeHistoryActiveQuery = String
        .format("select * from t_lwa_node where external_id = '%s' and status = 'Active'",
            lwaUpdatedwithSuspendedStatus.getExternalId());

    String startDate = DBHelper.getValueFromDB(nodeHistoryActiveQuery, "start_date");

    Assert.assertEquals(endDate, DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_END_DB));
    Assert.assertEquals(startDate, DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START_DB, 2));
  }

  @Test(description = "Update node status to Active when it status already was updated to Suspended for this day"
      + "(Negative Test)", groups = "smoke")
  public void requestRejectedNodeStatusTwiceInOneDay() {

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

    //Update Node's status to Active
    LightWeightAccount lwaUpdatedwithActiveStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithActiveStatus).then().statusCode(428);
  }

  @Test(description = "Checking that node status Suspended and Active could be successfully updated in future"
      + "(Positive Test)", groups = "smoke")
  public void nodeStatusSuspendedAndActiveUpdatedInFuture() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 8))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(200);

    //Update Node's status Active
    LightWeightAccount lwaUpdatedwithActiveStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 31))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithActiveStatus).then().statusCode(200);
  }

  @Test(description = "Checking that node status Suspended and Active could be successfully updated one year later"
      + "(Positive Test)", groups = "smoke")
  public void nodeStatusSuspendedNextYearPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 365))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(200);
  }

  @Test(description = "Checking that request is rejected when node status already has the same status(Negative Test)",
      groups = "smoke")
  public void requestRejectedWhenNodeSameStatusNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(200);

    //Update Node's status Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatusD = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatusD).then().statusCode(428);
  }

  @Test(description = "Checking that request rejected when updating node status to Suspended before node is created "
      + "(Negative Test", groups = "smoke")
  public void updateNodeSuspendedBeforeCreatedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status to Suspended yesterday
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(428);
  }

  @Test(description = "Checking request Node Status Suspended Before last Update (Negative Test)", groups = "smoke")
  public void updateNodeSuspendedBeforeLastUpdateNegativeTest() {

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

    //Update Node's status to Suspended yesterday
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwaUpdated.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(428);

  }

  @Test(description = "Checking that the request is rejected when the date of the request is before the last update "
      + "of node to Suspended(NegativeTest)", groups = "smoke")
  public void requestRejectedBeforeLastUpdateToSuspendedNegativeTest() {

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
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(200);

    //Update Node's status to Active
    LightWeightAccount lwaUpdatedwithActiveStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithActiveStatus).then().statusCode(428);
  }

  @Test(description = "Verify that the status of the IOT device CAMERA is updated as Suspended and the API returns "
      + "success (Positive Test)", groups = "smoke")
  public void updateNodeStatusToSuspendedPositiveTest() {

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
  }

  @Test(description = "Update Node status to Suspended when responsible is in Status Archived (Negative Test)",
      groups = "smoke")
  public void requestRejectUpdateNodeSuspendedResponsibleArchivedNegativeTest() {

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

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(428);
  }

  @Test(description = "Checking that node AIRSENSOR status Suspended and Active could be successfully updated one year "
      + "later(Positive Test)", groups = "smoke")
  public void nodeAirsensorStatusSuspendedNextYearPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode()
        .setNodeType("AIRSENSOR")
        .setAttributes(airSensorAttributes);

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update Node's status Suspended
    LightWeightAccount lwaUpdatedwithSuspendedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 365))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then().statusCode(200);
  }

  @Test(description = "Request change the status of a Node between Active and Suspended at a specific time (Positive "
      + "Test)", groups = "smoke")
  public void updateNodeStatusBetweenActiveSuspendedPositiveTest() {

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

    //Update Node's status to Active
    LightWeightAccount lwaUpdatedwithActiveStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithActiveStatus).then().statusCode(200);

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithSuspended1Status = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspended1Status).then().statusCode(200);

    //Update Node's status to Active
    LightWeightAccount lwaUpdatedwithActive1Status = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_END, 4))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithActive1Status).then().statusCode(200);

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithSuspended2Status = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 5))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspended2Status).then().statusCode(200);
  }

  @Test(description = "Checking that request rejected when no startdate in request(Negative Test)", groups = "smoke")
  public void requestRejectedWhenNoStartDateInRequestNegativeTest() {

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
        .setStartDate("")
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithSuspendedStatus).then()
        .statusCode(400);
  }

}
