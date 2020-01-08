package bluesky.restapi.lightWeightAccount.bulkUploadIotDevices;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.helpers.FileProcessingHelper;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.NodeTypes;
import bluesky.restapi.models.OwnershipModes;
import io.restassured.response.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import static bluesky.restapi.helpers.FileProcessingHelper.createCsvFileWithData;
import static bluesky.restapi.managers.BaseManager.generateUniqueNumber;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static org.hamcrest.Matchers.equalTo;

public class BaseBulkUploadNodes extends BaseApiTest {

  String[] validCameraFileHeaders = new String[] { "external_id", "msisdn", "imsi", "oem", "make", "model" };

  String[] validAirsensorFileHeaders = new String[] { "external_id", "msisdn", "oem", "make", "model" };

  String[] validUpdateHeaders = new String[] { "external_id" };

  public static final String CAMERA_CREATE_SCHEMA_TYPE = "CAMERA_BULK_CREATE";
  static final String AIRSENSOR_UPDATE_RESPONSIBLE_SCHEMA_TYPE = "AIRSENSOR_BULK_UPDATE_RESPONSIBLE";
  static final String CAMERA_UPDATE_RESPONSIBLE_SCHEMA_TYPE = "CAMERA_BULK_UPDATE_RESPONSIBLE";
  static final String CAMERA_UPDATE_STATUS_SCHEMA_TYPE = "CAMERA_BULK_UPDATE_STATUS";
  static final String CAMERA_TERMINATE_SCHEMA_TYPE = "CAMERA_BULK_TERMINATE";
  static final String AIRSENSOR_UPDATE_STATUS_SCHEMA_TYPE = "AIRSENSOR_BULK_UPDATE_STATUS";
  static final String AIRSENSOR_TERMINATE_SCHEMA_TYPE = "AIRSENSOR_BULK_TERMINATE";

  static final String softWareId = "JavaTest";
  static final String defaultPassword = "123";

  static final String MSISDN = "16473706301";
  static final String IMSI = "310410010522518000";
  static final String OEM = "GE";
  static final String CAMERA_SENSOR_TYPE = "SmartCamx02";
  static final String AIRSENSOR_SENSOR_TYPE = "SensAiry02";
  static final String BULK_UPLOAD_PREFIX = "bulkUpload";

  public static List<String[]> prepareDataForBulkUploadNodesFile(String[] headers, List<String[]> data) {

    List<String[]> dataList = new ArrayList<>();

    //Add headers parameter
    dataList.add(headers);

    //Looping through the data and add data to the list one by one
    dataList.addAll(data);
    return dataList;
  }

  HashMap<String, String> getFileProcessedResults(String responseBody) {

    //Read the response body
    Scanner scanner = new Scanner(responseBody);
    ArrayList<String[]> items = new ArrayList<>();
    while (scanner.hasNextLine()) {
      //add quotes by delimiter ','
      items.add(scanner.nextLine().split(","));
    }
    scanner.close();

    HashMap<String, String> results = new HashMap<>();

    //Put data to HashMap
    for (int i = 1; i < items.size(); i++) {
      String externalId = items.get(i)[0];
      String status = items.get(i)[items.get(i).length - 1];
      results.put(externalId, status);

    }
    return results;
  }

  static String getFileName(String prefix) {

    return String.format("src%stest%sresources%s%s%s.csv",
        FileProcessingHelper.SEPARATOR,
        FileProcessingHelper.SEPARATOR,
        FileProcessingHelper.SEPARATOR,
        prefix,
        generateUniqueNumber());
  }

  static String getBulkUploadResponseBody(String requestId) {

    return LightWeightAccountServiceMethods.downloadProcessedFile(requestId).getBody().asString();
  }

  static void createFileForUpload(String fileName, List<String[]> fileContent,
                                  String[] fileHeaders) {

    List<String[]> data = prepareDataForBulkUploadNodesFile(fileHeaders, fileContent);
    createCsvFileWithData(fileName, data);
  }

  ArrayList<String[]> createListOfCameraNodes(int nodesAmount) {

    ArrayList<String[]> lwaData = new ArrayList<>();
    int i = 0;
    while (i < nodesAmount) {
      lwaData.add(new String[] { BULK_UPLOAD_PREFIX + generateUniqueNumber(), MSISDN, IMSI, OEM, "",
          CAMERA_SENSOR_TYPE });
      i++;
    }
    return lwaData;
  }

  static Response getResponseFromUpload(final String fileName,
                                        final HashMap<String, String> headers) {

    return LightWeightAccountServiceMethods.bulkUploadNodes(new File(fileName), headers)
        .then()
        .statusCode(202)
        .body("status", equalTo("JOB_SCHEDULED"))
        .extract()
        .response();
  }

  static LightWeightAccount createLwa(Integer responsibleId) {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();
    return createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());
  }

  static List<LightWeightAccount> createNodesList(int nodesAmount, Integer internalResponsibleId,
                                                  String startDate, String nodeType) {

    List<LightWeightAccount> lwaData = new ArrayList<>();
    int i = 0;

    if (nodeType.equals(NodeTypes.NODE_TYPE_CAMERA)) {
      while (i < nodesAmount) {
        lwaData.add(createLwa(internalResponsibleId).setStartDate(startDate));
        LightWeightAccountServiceMethods.newNodeRegistration(lwaData.get(i)).then().statusCode(200);
        i++;
      }
      return lwaData;
    }

    if (nodeType.equals(NodeTypes.NODE_TYPE_AIRSENSOR)) {
      LightWeightAccount.Node node = createDefaultNode()
          .setAttributes(new HashMap<String, Object>() {{
            put("msisdn", "value11");
            put("oem", "ABC-12345");
            put("location", "US-123");
            put("model", "ABC-123456");
          }})
          .setNodeType(NodeTypes.NODE_TYPE_AIRSENSOR);

      while (i < nodesAmount) {
        lwaData.add(createDefaultLightWeightAccount(node)
            .setResponsibleId(internalResponsibleId.toString())
            .setOwnershipMode(OwnershipModes.RENT)
            .setSettlementId(internalResponsibleId.toString())
            .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START)));

        LightWeightAccountServiceMethods.newNodeRegistration(lwaData.get(i)).then().statusCode(200);
        i++;
      }
      return lwaData;
    }
    assert lwaData.size() != 0 : "non existing Node type provided";
    return lwaData;
  }

  static List<LightWeightAccount> createTwoNodesListStartCurrentDate(Integer responsibleId, String nodeType) {

    return createNodesList(2, responsibleId,
        DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START), nodeType);
  }
}
