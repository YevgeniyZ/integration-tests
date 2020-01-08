package bluesky.restapi.lightWeightAccount.bulkUploadIotDevices;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.base.BaseAssertions;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.AccountState;
import bluesky.restapi.models.AccountStatus;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.NodeStatus;
import bluesky.restapi.models.OwnershipModes;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static bluesky.restapi.helpers.FileProcessingHelper.createCsvFileWithData;
import static bluesky.restapi.managers.BaseManager.generateUniqueNumber;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;
import static java.lang.Integer.parseInt;

@Story("BLUESKY-27 LW Bulk Accounts Backend API - Retrieve Nodes for Responsible (Current)")
public class BulkRetrieveNodesForResponsibleTest extends BaseBulkUploadNodes {

  private static final int ONE_PAGE = 1;
  private static Integer responsible1Id, responsible2Id, responsible3Id, responsibleIdWith30Nodes;
  private static Integer settlement1Id, settlement2Id;

  private LightWeightAccount lwa1, lwa1Update1, lwa1Update2, lwa1Update3, lwa1Update4;
  private LightWeightAccount lwa2;
  private LightWeightAccount lwa3, lwa3Update1;
  private LightWeightAccount lwa4Update1;
  private LightWeightAccount lwa4Update2;

  private HashMap<String, Object> emptyFilterParam = new HashMap<String, Object>() {{
    put(" ", " ");
  }};

  private String thirtySecondDayAfterStartDay = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START
      , 32);

  @BeforeClass
  private void initialPrecondition() {

    createPreconditionResponsibles();
    createPreconditionSettlements();
    createPreconditionLWA();
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-808")
  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-903")
  @Test(description = "Verify that the API request returns all fields from the Node table", groups = "smoke")
  public void bulkRetrieveIOTDeviceAndVerifyAllFieldsFromNodeTablePositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1, responsible1Id);

    verifyOneNodeResponse(response, lwa1);
  }

  @Test(description = "Verify that the API returns data that can be displayed directly (does not contain internal "
      + "ECB ids)", groups = "smoke")
  public void bulkRetrieveIOTDeviceAndVerifyThatDataDoesNotContainInternalECBIdsPositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1, responsible1Id);

    //Verify APi response - NodeType as string (not numeric)
    verifyAmountRecordsPerPage(response, 1);
    verifyGetLwaFields(response, "records[0].",
        new HashMap<String, Object>() {{
          put("responsibleId", responsible1Id.toString());
          put("nodeType", lwa1.getNode().getNodeType());
        }});
  }

  @Test(description =
      "Verify that only Nodes to which the Responsible has access are returned when the Request Date is "
          + "equal to the Node's startDate", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenTheRequestDateIsEqualToTheNodesStartDatePositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1, responsible1Id);

    //Verify APi response
    verifyOneNodeResponse(response, lwa1);
  }

  @Test(description = "Verify that only Nodes to which the Responsible has access are returned when the Request Date "
      + "is equal to the Node's endDate", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenTheRequestDateIsEqualToTheNodesEndDatePositiveTest() {

    Response response =
        getResponseForFutureDate(DateTimeHelper.setCustomDatePattern("yyyy-MM-dd'T'23:59:58'Z'"), 3, responsible1Id);

    //Verify APi response
    verifyOneNodeResponse(response, lwa1);
  }

  @Test(description = "Verify that only Nodes to which the Responsible has access are returned when the Request Date "
      + "is equal to the  Node's startDate after changing status", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenTheRequestDateIsEqualToTheNodesStartDateAfterChangingStatusPositiveTest() {

    Response response =
        getResponseForFutureDate(DateTimeHelper.setCustomDatePattern("yyyy-MM-dd'T'00:00:01'Z'"), 4, responsible1Id);

    //Verify APi response
    verifyOneNodeResponse(response, lwa1Update1);
  }

  @Test(description = "Verify that only nodes to which the Responsible has access are returned when the "
      + "Request Date is equal to the second Node's startDate and in the range of the first Node", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenTheRequestDateIsEqualToTheSecondNodesStartDateAndInRangeOfTheFirstNodePositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 30, responsible1Id);

    //Verify APi response
    verifyAmountRecordsPerPage(response, 3);
    verifyThreeNodes(response, lwa1Update1, lwa2, lwa4Update2);
  }

  @Test(description = "Verify that only nodes to which the Responsible has access are returned when the Request Date "
      + "is equal to the first Node's startDate and in the range of several Nodes's date", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenTheRequestDateIsEqualToFirstNodesStartDateAndInRangeOfSeveralNodePositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 31, responsible1Id);

    //Verify APi response
    verifyFourNodeResponse(response, lwa1Update1, lwa2, lwa3, lwa4Update2);
  }

  @Test(description = "Verify that only Node information is returned for the period for which the Responsible has "
      + "access", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenTheRequestDateIsFromPeriodForWhichTheResponsibleHasAccessPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods
        .bulkRetrieveIotDeviceWithoutParams(responsible1Id.toString(), thirtySecondDayAfterStartDay);

    //Verify APi response
    verifyFourNodeResponse(response, lwa2, lwa3, lwa1Update2, lwa4Update2);
  }

  @Test(description = "Verify that only nodes to which the Responsible has access are returned when the Request Date "
      + "is in the range date for several Nodes", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenRequestDateIsInTheRangeDateForSeveralNodesPositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 45, responsible1Id);

    verifyAmountRecordsPerPage(response, 3);

    //Verify APi response
    verifyThreeNodes(response, lwa2, lwa3Update1, lwa4Update2);
  }

  @Test(description = "Verify that only nodes to which the Responsible has access are returned when the Request Date "
      + "is equal Node's startDate after changing responsible", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenRequestDateIsEqualNodesStartDateAfterChangingResponsiblePositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 45, responsible2Id);

    //Verify API response
    verifyOneNodeResponse(response, lwa1Update3);
  }

  @Test(description = "Verify that only Node information is returned to Responsible when Request Date is from Node's "
      + "terminated interval", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenRequestDateIsFromNodesTerminatedIntervalPositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 62, responsible2Id);

    //Verify API response
    verifyOneNodeResponse(response, lwa1Update4);
  }

  @Test(description = "Verify that only Nodes to which the Responsible has access are returned when the Request Date "
      + "is equal to the Node's endDate before change Node's Responsible", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenRequestDateIsEqualResponsiblesEndDateBeforeChangingStatusPositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper
        .setCustomDatePattern("yyyy-MM-dd'T'12:00:00'Z'"), 22, responsible3Id);

    //Verify API response
    verifyOneNodeResponse(response, lwa4Update1);
  }

  @Test(description = "Verify that Nodes don't return for Responsible after change Node's Responsible and "
      + "Node's status in one day", groups = "smoke")
  public void bulkDontRetrieveIOTDeviceIfRequestDateIsAfterChangeNodesResponsibleAndNodesStatusInOneDayPositiveTest() {

    Response response =
        getResponseForFutureDate(DateTimeHelper.setCustomDatePattern("yyyy-MM-dd'T'12:00:01'Z'"), 22, responsible3Id);

    //Verify API response
    verifyAmountRecordsPerPage(response, 0);
  }

  @Test(description = "Verify that only Nodes to which the Responsible has access are returned after change Node's "
      + "Responsible and Node's status in one day", groups = "smoke")
  public void bulkRetrieveIOTDeviceIfRequestDateIsAfterChangeNodesResponsibleAndNodesStatusInOneDayPositiveTest() {

    Response response = getResponseForFutureDate(DateTimeHelper
        .setCustomDatePattern("yyyy-MM-dd'T'12:00:01'Z'"), 22, responsible1Id);

    //Verify response
    verifyAmountRecordsPerPage(response, 2);
    verifyTwoNodes(response, lwa1Update1, lwa4Update2);
  }

  @Test(description = "Verify that the API returned page specified in the request for retrieve Nodes", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenPageSpecifiedInTheRequestForRetrieveNodesPositiveTest() {

    int totalCount = 30, currentPage = 2, totalPageSize = 10, totalPages = 2;

    //Bulk retrieve Nodes
    Response response =
        getResponseWithParamsAndFutureDate(1, responsibleIdWith30Nodes, Integer.toString(currentPage), null, null);

    //Verify API response - main headers: totalCount, currentPage, totalPageSize, totalPages
    HashMap<String, Object> responseHeaderFields = new HashMap<String, Object>() {{
      put("totalCount", totalCount);
      put("currentPage", currentPage);
      put("totalPageSize", totalPageSize);
      put("totalPages", totalPages);
    }};

    verifyMainHeaderFields(response, responseHeaderFields);
    verifyAmountRecordsPerPage(response, totalPageSize);

    //Verify that API response contains sequence records from 21 to 30
    verifySequenceRecordsPerPage(response, totalPageSize, 21);
  }

  @Test(description = "Verify that the API returned number of records per page matching with size in the request for "
      + "retrieve Nodes", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSizeSpecifiedInTheRequestForRetrieveNodesPositiveTest() {

    int totalCount = 30, totalPageSize = 20, totalPages = 2;
    //Bulk retrieve Nodes
    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1);

    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsibleIdWith30Nodes,
        requestDate, "1", Integer.toString(totalPageSize), null, emptyFilterParam);

    //Verify API response - main headers: totalCount, currentPage, totalPageSize, totalPages
    HashMap<String, Object> responseHeaderFields = new HashMap<String, Object>() {{
      put("totalCount", totalCount);
      put("currentPage", 1);
      put("totalPageSize", totalPageSize);
      put("totalPages", totalPages);
    }};

    verifyMainHeaderFields(response, responseHeaderFields);

    //Check amount records per page
    verifyAmountRecordsPerPage(response, totalPageSize);

    //Verify that API response contains sequence records from 1 to 20 on first Page
    verifySequenceRecordsPerPage(response, totalPageSize, 1);

    //Verify records on second Page
    response = LightWeightAccountServiceMethods
        .bulkRetrieveIotDeviceWithParams(responsibleIdWith30Nodes, requestDate, "2", Integer.toString(totalPageSize),
            null, emptyFilterParam);

    //Verify API response - main headers: totalCount, currentPage, totalPageSize, totalPages
    responseHeaderFields = new HashMap<String, Object>() {{
      put("totalCount", totalCount);
      put("currentPage", 2);
      put("totalPageSize", 10);
      put("totalPages", totalPages);
    }};

    verifyMainHeaderFields(response, responseHeaderFields);

    //Check amount records per page
    verifyAmountRecordsPerPage(response, 10);

    //Verify that API response contains sequence records from 21 to 30
    verifySequenceRecordsPerPage(response, 10, 21);
  }

  @Test(description = "Verify that the API response contains sorted records when sort by Node Type and sort direction "
      + "doesn't set", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByNodeTypeAndSortDirectionDoesNotSetPositiveTest() {

    Response response = getResponseWithParamsAndFutureDate(31, responsible1Id, null, null, "nodeType");
    verifyFourNodeResponse(response, lwa2, lwa4Update2, lwa1Update1, lwa3);
  }

  @Test(description = "Verify that the API response contains sorted records by Node Type and by ascending", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByNodeTypeByAscendingPositiveTest() {

    Response response = getResponseWithParamsAndFutureDate(31, responsible1Id, null, null, "nodeType|asc");

    verifyFourNodeResponse(response, lwa2, lwa4Update2, lwa1Update1, lwa3);
  }

  @Test(description = "Verify that the API response contains sorted records by Node Type and by descending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByNodeTypeByDescendingPositiveTest() {

    Response response = getResponseWithParamsAndFutureDate(31, responsible1Id, null, null, "nodeType|desc");

    //Verify API response
    verifyFourNodeResponse(response, lwa1Update1, lwa3, lwa4Update2, lwa2);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-792")
  @Test(description = "Verify that the API response contains sorted records by Node Status and by ascending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByNodeStatusByAscendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "nodeStatus|asc", emptyFilterParam);
    verifyFourNodeResponse(response, lwa1Update2, lwa4Update2, lwa2, lwa3);

  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-792")
  @Test(description = "Verify that the API response contains sorted records by Node Status and by descending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByNodeStatusByDescendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "nodeStatus|desc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa2, lwa3, lwa4Update2, lwa1Update2);
  }

  @Test(description = "Verify that the API response contains sorted records by OrderId and by ascending", groups =
      "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByOrderIdByAscendingPositiveTest() {

    //Create Responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Create a few Nodes to Responsible with different OrderId
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwaSort1 = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOrderId("123")
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaSort1).then().statusCode(200);
    lwaSort1.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    LightWeightAccount lwaSort2 = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOrderId("124")
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaSort2).then().statusCode(200);
    lwaSort2.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    LightWeightAccount lwaSort3 = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOrderId("123a")
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaSort3).then().statusCode(200);
    lwaSort3.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    LightWeightAccount lwaSort4 = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOrderId("a123")
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaSort4).then().statusCode(200);
    lwaSort4.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    Response response = getResponseWithParamsAndFutureDate(2, responsibleId, null, null, "orderId|asc");

    //Verify API response
    verifyFourNodeResponse(response, lwaSort1, lwaSort3, lwaSort2, lwaSort4);
  }

  @Test(description = "Verify that the API response contains sorted records by OrderId and by descending", groups =
      "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByOrderIdByDescendingPositiveTest() {

    //Create Responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Create a few Nodes to Responsible with different OrderId
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwaSort1 = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOrderId("123")
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaSort1).then().statusCode(200);
    lwaSort1.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    LightWeightAccount lwaSort2 = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOrderId("124")
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaSort2).then().statusCode(200);
    lwaSort2.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    LightWeightAccount lwaSort3 = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOrderId("123a")
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaSort3).then().statusCode(200);
    lwaSort3.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    LightWeightAccount lwaSort4 = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOrderId("a123")
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwaSort4).then().statusCode(200);
    lwaSort4.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    Response response = getResponseWithParamsAndFutureDate(2, responsibleId, null, null, "orderId|desc");

    //Verify API response
    verifyFourNodeResponse(response, lwaSort4, lwaSort2, lwaSort3, lwaSort1);
  }

  @Test(description = "Verify that the API response contains sorted records by SettlementId and by ascending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedBySettlementIdByAscendingPositiveTest() {
    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "settlementId|asc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa1Update2, lwa4Update2, lwa2, lwa3);
  }

  @Test(description = "Verify that the API response contains sorted records by SettlementId and by descending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedBySettlementIdByDescendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "settlementId|desc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa2, lwa3, lwa4Update2, lwa1Update2);
  }

  @Test(description = "Verify that the API response contains sorted records by ExternalId and by ascending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByExternalIdByAscendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "externalId|asc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa1Update2, lwa2, lwa3, lwa4Update2);
  }

  @Test(description = "Verify that the API response contains sorted records by ExternalId and by descending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByExternalIdByDescendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "externalId|desc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa4Update2, lwa3, lwa2, lwa1Update2);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-792")
  @Test(description = "Verify that the API response contains sorted records by OwnershipMode and by ascending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByOwnerShipModeByAscendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "ownershipMode|asc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa1Update2, lwa4Update2, lwa2, lwa3);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-792")
  @Test(description = "Verify that the API response contains sorted records by OwnershipMode and by descending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByOwnerShipModeByDescendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "ownershipMode|desc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa2, lwa3, lwa1Update2, lwa4Update2);
  }

  @Test(description = "Verify that the API response contains sorted records by Start Date and by ascending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByStartDateByAscendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "startDate|asc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa4Update2, lwa2, lwa3, lwa1Update2);
  }

  @Test(description = "Verify that the API response contains sorted records by Start Date and by descending",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByStartDateByDescendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "startDate|desc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa1Update2, lwa3, lwa2, lwa4Update2);
  }

  @Test(description = "Verify that the API response contains sorted records by End Date and by ascending", groups =
      "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByEndDateByAscendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, " ", null, "endDate|asc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa3, lwa1Update2, lwa4Update2, lwa2);
  }

  @Test(description = "Verify that the API response contains sorted records by End Date and by descending", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSortedByEndDateByDescendingPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "endDate|desc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa2, lwa4Update2, lwa1Update2, lwa3);
  }

  @Test(description = "Verify Multiple sort criteria by NodeType, NodeStatus, externalId", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenMultipleSortCriteriaByNodeTypeNodeStatusExternalIdPositiveTest() {

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "nodeType|asc, nodeStatus|desc, externalId|desc", emptyFilterParam);

    //Verify API response
    verifyFourNodeResponse(response, lwa4Update2, lwa2, lwa3, lwa1Update2);
  }

  @Test(description = "Verify that the API response contains only Nodes with NodeType indicated in Filter",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenNodeTypeIndicatedInFilterPositiveTest() {

    //Bulk retrieve Nodes when filter is nodeType = CAMERA
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, null,
        new HashMap<String, Object>() {{
          put("nodeType", "CAMERA");
        }});

    //Verify API response
    verifyAmountRecordsPerPage(response, 2);
    verifyTwoNodes(response, lwa3, lwa1Update2);

    //Bulk retrieve Nodes when filter is nodeType = AirSensor
    response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, null,
        new HashMap<String, Object>() {{
          put("nodeType", "AIRSENSOR");
        }});

    //Verify API response
    verifyAmountRecordsPerPage(response, 2);
    verifyTwoNodes(response, lwa2, lwa4Update2);
  }

  @Test(description = "Verify that the API response contains only Nodes with NodeStatus indicated in Filter",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenNodeStatusIndicatedInFilterPositiveTest() {

    //Bulk retrieve Nodes when filter is nodeStatus = ACTIVE
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, null,
        new HashMap<String, Object>() {{
          put("nodeStatus", NodeStatus.ACTIVE);
        }});

    //Verify API response
    verifyOneNodeResponse(response, lwa1Update2);

    //Bulk retrieve Nodes when filter is nodeStatus = SUSPENDED
    response = LightWeightAccountServiceMethods
        .bulkRetrieveIotDeviceWithParams(responsible1Id, thirtySecondDayAfterStartDay, null, null, null,
            new HashMap<String, Object>() {{
              put("nodeStatus", NodeStatus.SUSPENDED);
            }});

    //Verify API response
    verifyAmountRecordsPerPage(response, 3);
    verifyThreeNodes(response, lwa2, lwa3, lwa4Update2);
  }

  @Test(description = "Verify that the API response contains only Nodes with OrderId indicated in Filter", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenOrderIdIndicatedInFilterPositiveTest() {

    //Bulk retrieve Nodes when filter is orderId = 'str5'
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, null,
        new HashMap<String, Object>() {{
          put("orderId", "Str5");
        }});

    //Verify API response
    verifyAmountRecordsPerPage(response, 2);
    verifyTwoNodes(response, lwa3, lwa4Update2);
  }

  @Test(description = "Verify that the API response contains only Nodes with ExternalId indicated in Filter", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenExternalIdIndicatedInFilterPositiveTest() {

    //Bulk retrieve Nodes when filter is orderId = 'str5'
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, null,
        new HashMap<String, Object>() {{
          put("orderId", "Str5");
        }});

    //Verify API response
    verifyAmountRecordsPerPage(response, 2);
    verifyTwoNodes(response, lwa3, lwa4Update2);
  }

  @Test(description = "Verify that the API response contains only Nodes with OwnershipMode indicated "
      + "in Filter", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenOwnerShipModeIndicatedInFilterPositiveTest() {

    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 36);

    //Bulk retrieve Nodes when filter is OwnerShipMode = 'Purschase'
    Response response = LightWeightAccountServiceMethods
        .bulkRetrieveIotDeviceWithParams(responsible1Id, requestDate, null, null, null, new HashMap<String, Object>() {{
          put("ownershipMode", OwnershipModes.OWNER);
        }});

    //Verify API response
    verifyAmountRecordsPerPage(response, 2);
    verifyTwoNodes(response, lwa2, lwa3Update1);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-830")
  @Test(description = "Verify that the API response contains only Nodes with Start Date indicated in Filter",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenStartDateIndicatedInFilterPositiveTest() {

    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 30);

    //Bulk retrieve Nodes when filter is Start Date equal
    String startDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 30);
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        requestDate, null, null, null, new HashMap<String, Object>() {{
          put("startDate", startDate);
        }});

    //Verify API response
    verifyOneNodeResponse(response, lwa2);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-830")
  @Test(description = "Verify that the API response contains only Nodes with "
      + "End Date indicated in Filter", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenEndDateIndicatedInFilterPositiveTest() {

    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 30);

    //Bulk retrieve Nodes when filter is Start Date equal
    String endDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_END, 31);
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        requestDate, null, null, null, new HashMap<String, Object>() {{
          put("endDate", endDate);
        }});

    //Verify API response
    verifyOneNodeResponse(response, lwa1Update1);
  }

  @Test(description = "Verify bulk retrieve IOT device when Multiple Filter is enable", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenEnabledMultipleFilterPositiveTest() {

    //retrieve Nodes when multiple filter by nodeStatus, nodeType, settlementId, attributes: msisdn
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, null,
        new HashMap<String, Object>() {{
          put("nodeStatus", NodeStatus.SUSPENDED);
          put("nodeType", "AIRSENSOR");
          put("settlementId", settlement2Id.toString());
          put("attributes", new HashMap<String, Object>() {{
            put("msisdn", "33455500112432");
          }});
        }});

    //Verify API response
    verifyOneNodeResponse(response, lwa4Update2);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-851")
  @Test(description = "Verify that API request is rejected and contains validation message when value in sort is "
      + "incorrect (NegativeTest)", groups = "smoke")
  public void verifyThatRequestIsRejectedWhenIncorrectValueInSortNegativeTest() {

    Response response = getResponseWithParamsAndFutureDate(1, responsible1Id, null, null, "odeType");

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400, "Failed to execute bad "
        + "request. Reason: Unknown field: odeType");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-817")
  @Test(description = "Verify that API request is rejected and contains validation message when ResponsibleId doesn't "
      + "exist in DB and more 10 symbols (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTRequestIsRejectedWhenResponsibleIdDoesNotExistInDBNegativeTest() {

    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1);

    //Bulk retrieve Nodes
    Response response =
        LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithoutParams("12345678901", requestDate);

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Maximum length of accountId is 10");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-818")
  @Test(description = "Verify that API request returned validation message and code 482 when ResponsibleId contains "
      + "any letter characters (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTRequestIsRejectedWhenResponsibleIdIsContainsAnyCharNegativedTest() {

    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1);

    //Bulk retrieve Nodes
    Response response =
        LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithoutParams("124a", requestDate);

    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid responsibleId format: 124a");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-828")
  @Test(description = "Verify that API request is rejected and contains validation message when ResponsibleId is empty "
      + "(NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTRequestIsRejectedWhenResponsibleIdIsEmptyNegativeTest() {

    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1);

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithoutParams("", requestDate);

    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Account Id should not be empty");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-827")
  @Test(description = "Verify that API request is rejected and contains validation message when ResponsibleId is space "
      + "character (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTRequestIsRejectedWhenResponsibleIdIsSpaceCharacterNegativeTest() {

    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1);

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithoutParams(" ", requestDate);

    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Account Id should not be empty");
  }

  @Test(description = "Verify that the node information is not provided to the user who does not have access to this "
      + "period (NegativeTest)", groups = "smoke")
  public void bulkDontRetrieveIOTDeviceIfUserDoesNotHaveAccessToThisRequestDateNegativeTest() {

    String requestDate = DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START);

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithoutParams(responsible3Id.toString(),
        requestDate);

    //Verify API response
    verifyAmountRecordsPerPage(response, 0);
  }

  @Test(description = "Verify that the API returned all page when \"page\" is space character in the request for "
      + "retrieve Nodes (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenPageIsSpaceCharacterNegativeTest() {

    int totalCount = 30, totalPageSize = 10, totalPages = 3;

    //Bulk retrieve Nodes
    Response response =
        getResponseWithParamsAndFutureDate(1, responsibleIdWith30Nodes, " ", Integer.toString(totalPageSize), null);

    //Verify API response - main headers: totalCount, currentPage, totalPageSize, totalPages
    HashMap<String, Object> responseHeaderFields = new HashMap<String, Object>() {{
      put("totalCount", totalCount);
      put("currentPage", ONE_PAGE);
      put("totalPageSize", totalPageSize);
      put("totalPages", totalPages);
    }};

    verifyMainHeaderFields(response, responseHeaderFields);
    verifyAmountRecordsPerPage(response, totalPageSize);

    //Verify that API response contains sequence records from 1 to 10
    verifySequenceRecordsPerPage(response, totalPageSize, 1);
  }

  @Test(description = "Verify that the API returned all page when \"page\" is zero in the request for retrieve Nodes",
      groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenPageIsZeroPositiveTest() {

    int totalCount = 30, currentPage = 0, totalPageSize = 10, totalPages = 3;

    //Bulk retrieve Nodes
    Response response =
        getResponseWithParamsAndFutureDate(1, responsibleIdWith30Nodes, "0", Integer.toString(totalPageSize), null);

    //Verify API response - main headers: totalCount, currentPage, totalPageSize, totalPages
    HashMap<String, Object> responseHeaderFields = new HashMap<String, Object>() {{
      put("totalCount", totalCount);
      put("currentPage", currentPage);
      put("totalPageSize", totalPageSize);
      put("totalPages", totalPages);
    }};

    verifyMainHeaderFields(response, responseHeaderFields);
    verifyAmountRecordsPerPage(response, totalPageSize);

    //Verify that API response contains sequence records from 1 to 10
    verifySequenceRecordsPerPage(response, totalPageSize, 1);
  }

  @Test(description = "Verify that API response contains validation message if \"page\" value is incorrect in the "
      + "request for retrieve Nodes (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTDeviceRejectWhenPageIsIncorrectNegativeTest() {

    int totalPageSize = 10;
    String currentPage = "-1";
    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1);

    //Bulk retrieve Nodes when Page = -1
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsibleIdWith30Nodes,
        requestDate, currentPage, null, null, emptyFilterParam);

    //Verify API response - main headers: totalCount, currentPage, totalPageSize, totalPages
    BaseAssertions.verifyErrorResponse(response, 400, "Page index must not be "
        + "less than zero!");

    //Bulk retrieve Nodes when Page = abc
    currentPage = "abc";
    response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsibleIdWith30Nodes, requestDate,
        currentPage, Integer.toString(totalPageSize), null, emptyFilterParam);

    BaseAssertions.verifyErrorResponse(response, 400, "NumberFormatException: "
        + "For input string: \"abc\"");
  }

  @Test(description = "Verify that the API returned all page when \"page\" is out of range in the request for retrieve "
      + "Nodes (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTDeviceReturnPageWhenPageIsOutOfRangeNegativeTest() {

    int totalCount = 30, currentPage = 3, totalPageSize = 20, totalPages = 2;

    //Bulk retrieve Nodes when page = 4
    Response response = getResponseWithParamsAndFutureDate(1, responsibleIdWith30Nodes, Integer.toString(currentPage),
        Integer.toString(totalPageSize), null);

    //Verify API response - main headers: totalCount, currentPage, totalPageSize, totalPages
    HashMap<String, Object> responseHeaderFields = new HashMap<String, Object>() {{
      put("totalCount", totalCount);
      put("currentPage", currentPage);
      put("totalPageSize", totalPageSize);
      put("totalPages", totalPages);
    }};

    verifyMainHeaderFields(response, responseHeaderFields);
    verifyAmountRecordsPerPage(response, 0);
  }

  @Test(description = "Verify that the API returned all records when \"size\" contains space character in the request "
      + "for retrieve Nodes (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSizeIsSpaceCharacterNegativeTest() {

    int totalCount = 30, totalPageSize = 20, totalPages = 2;

    //Bulk retrieve Nodes when size = " "
    Response response =
        getResponseWithParamsAndFutureDate(1, responsibleIdWith30Nodes, Integer.toString(ONE_PAGE), " ", null);

    //Verify API response - main headers: totalCount, currentPage, totalPageSize, totalPages
    HashMap<String, Object> responseHeaderFields = new HashMap<String, Object>() {{
      put("totalCount", totalCount);
      put("currentPage", ONE_PAGE);
      put("totalPageSize", totalPageSize);
      put("totalPages", totalPages);
    }};

    verifyMainHeaderFields(response, responseHeaderFields);
    verifyAmountRecordsPerPage(response, totalPageSize);
  }

  @Test(description = "Verify that the API returned all records when \"size\" contains zero in the request for "
      + "retrieve Nodes (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSizeIsZeroNegativeTest() {

    //Bulk retrieve Nodes when size = 0
    Response response =
        getResponseWithParamsAndFutureDate(1, responsibleIdWith30Nodes, Integer.toString(1), "0", null);

    BaseAssertions.verifyErrorResponse(response, 400,
        "Page size must not be less than one!");
  }

  @Test(description = "Verify that the API returned all records when \"size\" value is incorrect in the request for "
      + "retrieve Nodes (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenSizeIsIncorrectNegativeTest() {

    //Bulk retrieve Nodes when size = @abc
    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1);

    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(
        responsibleIdWith30Nodes, requestDate, Integer.toString(ONE_PAGE), "@abc", null, emptyFilterParam);

    //Verify API response
    BaseAssertions.verifyErrorResponse(response, 400, "NumberFormatException: "
        + "For input string: \"@abc\"");

    //Bulk retrieve Nodes when Size = -1
    response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsibleIdWith30Nodes,
        requestDate, Integer.toString(ONE_PAGE), "-1", null, emptyFilterParam);

    BaseAssertions.verifyErrorResponse(response, 400, "Page size must not be less than one!");
  }

  @Test(description = "Verify that the API response contains all records when \"filter\" containsspace character in "
      + "the request for retrieve Nodes (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenFilterContainsSpaceCharacterNegativeTest() {

    //Bulk retrieve Nodes when filter is " "
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, null, emptyFilterParam);

    verifyFourNodeResponse(response, lwa2, lwa3, lwa1Update2, lwa4Update2);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-845")
  @Test(description = "Verify that the API response don't return code 500 when sort by 'attributes' or contains space "
      + "(NegativeTest)", groups = "smoke")
  public void verifyThatAPIResponseDoNotReturnCode500WhenSortByAttributesOrContainsSpaceNegativeTest() {

    //Bulk retrieve Nodes when sort by "Attributes"
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, "attributes", emptyFilterParam);

    //Verify API response
    BaseAssertions.verifyErrorResponse(response, 400, "Failed to execute bad "
        + "request. Reason: Dynamic attributes sorting is unsupported");

    //Bulk retrieve Nodes when sort by "Attributes"
    response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, " ", emptyFilterParam);

    BaseAssertions.verifyErrorResponse(response, 400, "Property must not null"
        + " or empty!");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-846")
  @Test(description = "Verify that API response contains validation message if \"filter\" value for is settlementId "
      + "contains non integer value when request for retrieve Nodes (NegativeTest)", groups = "smoke")
  public void VerifyAPIResponseForRetrNodesContainsMessageIfFilterValueNonIntForSettlementIdNegativeTest() {

    //Bulk retrieve Nodes when filter is "settlementId": "123a"
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, null, new HashMap<String, Object>() {{
          put("settlementId", "123a");
        }});

    //Verify API response
    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid settlementId format: 123a");
  }

  @Test(description = "Verify that API request for retrieve Nodes ignored value in \"filter\" when value is incorrect "
      + "(NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTDeviceWhenFilterIsIncorrectNegativeTest() {

    //Bulk retrieve Nodes when filter is "abc@.123"
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithParams(responsible1Id,
        thirtySecondDayAfterStartDay, null, null, null, new HashMap<String, Object>() {{
          put("abc@.123", "abc@.123");
        }});

    verifyFourNodeResponse(response, lwa2, lwa3, lwa1Update2, lwa4Update2);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-820")
  @Test(description = "Verify that API response is rejected and contains validation message when Request Date contains "
      + "mistake (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTResponseIsRejectedWhenRequestDateContainsMistakeNegativeTest() {

    String requestDate = "0019-12-11T23:59:5Z";

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithoutParams(responsible1Id.toString(),
        requestDate);

    //Verify API response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Invalid date format. Start date must be in "
            + "ISO_LOCAL_DATE_TIME format: yyyy-MM-ddThh:mm:ss");
  }

  @Test(description = "Verify that API request is rejected and contains validation message when Request Date is empty "
      + "(NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTResponseIsRejectedWhenRequestDateIsEmptyNegativeTest() {

    String requestDate = "";

    //Bulk retrieve Nodes
    Response response =
        LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithoutParams(responsible1Id.toString(), requestDate);

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Requested date should not be empty");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-829")
  @Test(description = "Verify that API request is rejected and contains validation message when Request Date is space "
      + "character (NegativeTest)", groups = "smoke")
  public void bulkRetrieveIOTRequestIsRejectedWhenRequestDateIsSpaceCharacterNegativeTest() {

    String requestDate = " ";

    //Bulk retrieve Nodes
    Response response = LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithoutParams(responsible1Id.toString(),
        requestDate);

    //Verify APi response
    BaseAssertions.verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: Requested date should not be empty");
  }

  private void mergeNode(LightWeightAccount lwaInitial, LightWeightAccount lwaUpdate) {

    //set lwaUpdate.StartDate-1 sec to lwaInitial.EndDate
    lwaInitial.setEndDate(DateTimeHelper.getDateMinusSeconds(lwaUpdate.getStartDate(),
        DateTimeHelper.setCustomDatePattern("yyyy-MM-dd'T'HH:mm:ss'Z'"), 1));

    //merge Nodes
    if (lwaUpdate.getResponsibleId() == null)
      lwaUpdate.setResponsibleId(lwaInitial.getResponsibleId());
    if (lwaUpdate.getOwnershipMode() == null)
      lwaUpdate.setOwnershipMode(lwaInitial.getOwnershipMode());
    if (lwaUpdate.getNodeStatus() == null)
      lwaUpdate.setNodeStatus(lwaInitial.getNodeStatus());
    if (lwaUpdate.getOrderId() == null)
      lwaUpdate.setOrderId(lwaInitial.getOrderId());
    if (lwaUpdate.getSettlementId() == null)
      lwaUpdate.setSettlementId(lwaInitial.getSettlementId());
    if (lwaUpdate.getStartDate() == null)
      lwaUpdate.setStartDate(lwaInitial.getStartDate());
    if (lwaUpdate.getEndDate() == null)
      lwaUpdate.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    //Copy Node's Attributes from lwaInitial to lwaUpdate
    lwaUpdate.getNode().setAttributes(new HashMap<String, Object>() {{
      put("oem", lwaInitial.getNode().getAttributes().get("oem"));
      put("location", lwaInitial.getNode().getAttributes().get("location"));
      put("model", lwaInitial.getNode().getAttributes().get("model"));
      put("imsi", lwaInitial.getNode().getAttributes().get("imsi"));
      put("msisdn", lwaInitial.getNode().getAttributes().get("msisdn"));
    }});
  }

  private Response getResponseWithParamsAndFutureDate(final int futureDays, final Integer responsible1Id,
                                                      final String page, final String size, final String nodeType) {

    String requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, futureDays);

    //Bulk retrieve Nodes
    return LightWeightAccountServiceMethods
        .bulkRetrieveIotDeviceWithParams(responsible1Id, requestDate, page, size, nodeType, emptyFilterParam);
  }

  private Response getResponseForFutureDate(final String datePatternDefaultStart, final int daysInTheFuture,
                                            final Integer responsible1Id) {

    String requestDate = DateTimeHelper.getDatePlusDays(datePatternDefaultStart, daysInTheFuture);

    //Bulk retrieve Nodes
    return LightWeightAccountServiceMethods.bulkRetrieveIotDeviceWithoutParams(responsible1Id.toString(), requestDate);
  }

  private void verifyFourNodeResponse(final Response response, final LightWeightAccount lwaFirst,
                                      final LightWeightAccount lwaSecond, final LightWeightAccount lwaThird,
                                      final LightWeightAccount lwaFourth) {

    //Verify API response
    verifyAmountRecordsPerPage(response, 4);
    verifyFourNodes(response, lwaFirst, lwaSecond, lwaThird, lwaFourth);
  }

  private void verifyFourNodes(final Response response, final LightWeightAccount lwaFirst,
                               final LightWeightAccount lwaSecond,
                               final LightWeightAccount lwaThird, final LightWeightAccount lwaFourth) {

    verifyThreeNodes(response, lwaFirst, lwaSecond, lwaThird);
    verifyLwaFields(response, lwaFourth, String.format("records[%d].", getIndexFromExtId(response,
        lwaFourth.getExternalId())));
  }

  private void verifyThreeNodes(final Response response, final LightWeightAccount lwaFirst,
                                final LightWeightAccount lwaSecond,
                                final LightWeightAccount lwaThird) {

    verifyTwoNodes(response, lwaFirst, lwaSecond);
    verifyLwaFields(response, lwaThird, String.format("records[%d].", getIndexFromExtId(response,
        lwaThird.getExternalId())));
  }

  private void verifyTwoNodes(final Response response, final LightWeightAccount lwaFirst,
                              final LightWeightAccount lwaSecond) {

    verifyLwaFields(response, lwaFirst, String.format("records[%d].", getIndexFromExtId(response,
        lwaFirst.getExternalId())));
    verifyLwaFields(response, lwaSecond, String.format("records[%d].", getIndexFromExtId(response,
        lwaSecond.getExternalId())));
  }

  private void verifyOneNodeResponse(final Response response, final LightWeightAccount lwa1) {

    verifyAmountRecordsPerPage(response, 1);
    verifyLwaFields(response, lwa1, "records[0].");
  }

  private void verifyLwaFields(Response response, LightWeightAccount lwa, String lwaPath) {

    this.verifyGetLwaFields(response, lwaPath, getExpectedLwaFields(lwa));

    String nodeAttributesPath = lwaPath + "attributes.";
    this.verifyGetNodeAttributes(response, nodeAttributesPath, getExpectedNodeAttributes(lwa));
  }

  @Step("Verify Node fields are correct")
  private void verifyGetLwaFields(Response response, String request, HashMap<String, Object> nodeFields) {

    nodeFields
        .forEach((key, value) -> Assert.assertEquals(response.getBody().jsonPath().getString(request + key), value,
            "Field " + key + " is incorrect or not found"));
  }

  @Step("Verify that response contains main header lines and they are correct ")
  private void verifyMainHeaderFields(Response response, HashMap<String, Object> Fields) {

    Fields.forEach((key, value) -> Assert.assertEquals(response.getBody().jsonPath().getString(key), value.toString(),
        "Field " + key + " is incorrect or not found"));
  }

  @Step("Verify Node Attributes are correct")
  private void verifyGetNodeAttributes(Response response, String request,
                                       HashMap<String, Object> nodeAttributes) {

    nodeAttributes.forEach((key, value) -> Assert
        .assertEquals(response.getBody().jsonPath().getString(request + key), value,
            "Field " + key + " is incorrect or not found"));
  }

  private int getIndexFromExtId(final Response response, final String externalId) {

    List<Object> records = response.getBody().jsonPath().getList("records");
    for (int i = 0; i < records.size(); i++) {
      String responseExtId = response.getBody().jsonPath().getString(String.format("records[%d].externalId", i));
      if (responseExtId.equals(externalId)) {
        return i;
      }
    }
    return 0;
  }

  private static HashMap<String, Object> getExpectedLwaFields(LightWeightAccount lwa) {

    return new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwa.getStartDate());
      put("endDate", lwa.getEndDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
      put("nodeType", lwa.getNode().getNodeType());
    }};
  }

  private static HashMap<String, Object> getExpectedNodeAttributes(LightWeightAccount lwa) {

    return new HashMap<String, Object>() {{
      put("oem", lwa.getNode().getAttributes().get("oem"));
      put("location", lwa.getNode().getAttributes().get("location"));
      put("model", lwa.getNode().getAttributes().get("model"));
      put("imsi", lwa.getNode().getAttributes().get("imsi"));
      put("msisdn", lwa.getNode().getAttributes().get("msisdn"));
    }};
  }

  private void verifyAmountRecordsPerPage(Response response, int expectedAmount) {

    String result = response.getBody().jsonPath().getString("records.size()");
    int actualAmount = parseInt(result);
    Assert.assertEquals(actualAmount, expectedAmount, "The expected and actual number of records doesn't match");
  }

  private void verifySequenceRecordsPerPage(Response response, int totalPageSize, int start) {

    for (int i = 0; i < totalPageSize; i++) {
      String msisdnPath = String.format("records[%s].attributes.msisdn", i);
      int expectedResult = start + i;
      Assert.assertEquals(response.getBody().jsonPath().getString(msisdnPath), Integer.toString(expectedResult),
          "The expected and actual Node's msisdn doesn't match");
    }
  }

  private void createPreconditionLWA() {

    Date date = new Date();

    //1 Create Node 1 to Responsible 1, StartDay = CurrentDay + 1
    LightWeightAccount.Node node1 = createDefaultNode()
        .setNodeType("CAMERA")
        .setAttributes(new HashMap<String, Object>() {{
          put("msisdn", "1234abc");
          put("imsi", "abc1234");
          put("oem", "1233");
          put("location", "1234");
          put("model", "123455");
        }});

    lwa1 = createDefaultLightWeightAccount(node1)
        .setResponsibleId(responsible1Id.toString())
        .setExternalId("BulkRetrieve" + date.getTime() + "1")
        .setOrderId("10234")
        .setOwnershipMode(OwnershipModes.LEASE)
        .setSettlementId(settlement1Id.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa1).then().statusCode(200);

    //2 Update Node1 status to Suspended from CurrentDay + 4
    lwa1Update1 = new LightWeightAccount()
        .setExternalId(lwa1.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 4))
        .setNode(new LightWeightAccount.Node().setNodeType(node1.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa1Update1).then().statusCode(200);

    //merge Nodes
    mergeNode(lwa1, lwa1Update1);

    //3 Create Node 2 to Responsible 1, StartDay=CurrentDay + 30
    LightWeightAccount.Node node2 = createDefaultNode()
        .setNodeType("AIRSENSOR")
        .setAttributes(new HashMap<String, Object>() {{
          put("msisdn", "1235");
          put("oem", "1234");
          put("location", "1235");
          put("model", "123456");
        }});

    lwa2 = createDefaultLightWeightAccount(node2)
        .setExternalId("BulkRetrieve" + date.getTime() + "2")
        .setResponsibleId(responsible1Id.toString())
        .setOwnershipMode(OwnershipModes.OWNER)
        .setOrderId("10235")
        .setNodeStatus(NodeStatus.SUSPENDED)
        .setSettlementId(settlement2Id.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 30));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    //set endDate to lwa2
    lwa2.setEndDate(BaseApiTest.EXPECTED_DATE_STRING_WITH_TIMEZONE);

    //4 Create Node 3 to Responsible 1, StartDay=CurrentDay + 31
    LightWeightAccount.Node node3 = createDefaultNode()
        .setNodeType("CAMERA")
        .setAttributes(new HashMap<String, Object>() {{
          put("msisdn", "1235abc");
          put("imsi", "abc1235");
          put("oem", "1233");
          put("location", "1236");
          put("model", "123458");
        }});

    lwa3 = createDefaultLightWeightAccount(node3)
        .setExternalId("BulkRetrieve" + date.getTime() + "3")
        .setResponsibleId(responsible1Id.toString())
        .setOwnershipMode(OwnershipModes.OWNER)
        .setNodeStatus(NodeStatus.SUSPENDED)
        .setSettlementId(settlement2Id.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 31));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa3).then().statusCode(200);

    //5 Update Node3 status to Active from CurrentDay + 35
    lwa3Update1 = new LightWeightAccount()
        .setExternalId(lwa3.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 35))
        .setNode(new LightWeightAccount.Node().setNodeType(node3.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa3Update1).then().statusCode(200);

    //merge Nodes
    mergeNode(lwa3, lwa3Update1);

    //6 Update Node1 status to Active from CurrentDay + 32
    lwa1Update2 = new LightWeightAccount()
        .setExternalId(lwa1.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 32))
        .setNode(new LightWeightAccount.Node().setNodeType(node1.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa1Update2).then().statusCode(200);

    //merge Nodes
    mergeNode(lwa1Update1, lwa1Update2);

    //7 Update Responsible for Node1 with Responsible1 to Responsible2 from CurrentDay + 45
    lwa1Update3 = new LightWeightAccount()
        .setExternalId(lwa1.getExternalId())
        .setResponsibleId(responsible2Id.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 45))
        .setNode(new LightWeightAccount.Node().setNodeType(node1.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa1Update3).then().statusCode(200);

    //merge Nodes
    mergeNode(lwa1Update2, lwa1Update3);

    //8 Update Node1 status to Terminate from CurrentDay + 61
    lwa1Update4 = new LightWeightAccount()
        .setExternalId(lwa1.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 62))
        .setNode(new LightWeightAccount.Node().setNodeType(node1.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa1Update4).then().statusCode(200);

    //merge Nodes
    mergeNode(lwa1Update3, lwa1Update4);

    //9 Create Node 4 to Responsible 3, StartDay=CurrentDay + 1
    LightWeightAccount.Node node4 = createDefaultNode()
        .setNodeType("AIRSENSOR")
        .setAttributes(new HashMap<String, Object>() {{
          put("msisdn", "33455500112432");
          put("oem", "a1234");
          put("location", "b1235");
          put("model", "c123456");
        }});

    LightWeightAccount lwa4 = createDefaultLightWeightAccount(node4)
        .setExternalId("BulkRetrieve" + date.getTime() + "4")
        .setResponsibleId(responsible3Id.toString())
        .setOwnershipMode(OwnershipModes.LEASE)
        .setNodeStatus(NodeStatus.ACTIVE)
        .setSettlementId(settlement2Id.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa4).then().statusCode(200);

    //10 Update Node1 status to Suspended
    lwa4Update1 = new LightWeightAccount()
        .setExternalId(lwa4.getExternalId())
        .setStartDate(
            DateTimeHelper.getDatePlusDays(DateTimeHelper.setCustomDatePattern("yyyy-MM-dd'T'12:00:00'Z'"), 22))
        .setNode(new LightWeightAccount.Node().setNodeType(node4.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa4Update1).then().statusCode(200);

    //merge Nodes
    mergeNode(lwa4, lwa4Update1);

    //11 Update Node4: Responsible3 -> Responsible1
    lwa4Update2 = new LightWeightAccount()
        .setExternalId(lwa4.getExternalId())
        .setResponsibleId(responsible1Id.toString())
        .setStartDate(
            DateTimeHelper.getDatePlusDays(DateTimeHelper.setCustomDatePattern("yyyy-MM-dd'T'12:00:01'Z'"), 22))
        .setNode(new LightWeightAccount.Node().setNodeType(node4.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwa4Update2).then().statusCode(200);

    //merge Nodes
    mergeNode(lwa4Update1, lwa4Update2);

    // Bulk create 30 Nodes for responsibleIdWith30Nodes
    String fileName = getFileName("BulkUpload");

    List<String[]> lwaData = new ArrayList<>();

    for (int i = 1; i <= 30; i++) {
      lwaData.add(
          new String[] { "bulkUpload" + generateUniqueNumber(), Integer.toString(i),
              "310410010522518001" + i,
              "GE" + i, "",
              "SmartCamx01" });
    }

    List<String[]> data = prepareDataForBulkUploadNodesFile(validCameraFileHeaders, lwaData);

    createCsvFileWithData(fileName, data);
    LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName),
        responsibleIdWith30Nodes).then().statusCode(202);
  }

  private static void createPreconditionResponsibles() {

    //Create Responsible (30 Nodes)
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    responsibleIdWith30Nodes = getAccountId(responsibleResponse);

    //Create Responsible 1 with start date = current date
    AccountEntity responsible1 = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response responsible1Response = AccountEntityServiceMethods.createAccountEntityRequest(responsible1);
    responsible1Id = getAccountId(responsible1Response);

    //Create Responsible 2 with a few status (AC -> SU) and start date = current date + 45 days
    AccountEntity responsible2 = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 45));
    Response responsible2Response = AccountEntityServiceMethods.createAccountEntityRequest(responsible2);
    responsible2Id = getAccountId(responsible2Response);

    //Set account status SU to Responsible 2
    AccountState accountState = new AccountState()
        .setAccountId(responsible2Id)
        .setVtStart(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 61))
        .setAccountState(AccountStatus.SUSPENDED);
    AccountEntityServiceMethods.accountStateUpdateRequest(accountState);

    //Create Responsible 3 with start date = current date
    AccountEntity responsible3 = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response responsible3Response = AccountEntityServiceMethods.createAccountEntityRequest(responsible3);
    responsible3Id = getAccountId(responsible3Response);
  }

  private static void createPreconditionSettlements() {
    //Create Settlement1
    AccountEntity settlement = new AccountEntityManager().createAccountEntity();
    Response settlementResponse =
        AccountEntityServiceMethods.createAccountEntityRequest(settlement);
    settlement1Id = getAccountId(settlementResponse);

    //Create Settlement2
    settlement = new AccountEntityManager().createAccountEntity();
    settlementResponse = AccountEntityServiceMethods.createAccountEntityRequest(settlement);
    settlement2Id = getAccountId(settlementResponse);
  }
}