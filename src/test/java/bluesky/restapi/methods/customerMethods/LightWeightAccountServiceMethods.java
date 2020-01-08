package bluesky.restapi.methods.customerMethods;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.OwnershipModes;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.response.Response;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static bluesky.restapi.lightWeightAccount.bulkUploadIotDevices.BaseBulkUploadNodes.CAMERA_CREATE_SCHEMA_TYPE;
import static io.restassured.RestAssured.given;


public class LightWeightAccountServiceMethods extends BaseApiMethod {

    @Step("Update node API request")
    public static Response updateNodeRegistration(LightWeightAccount lwa){
        return   given()
                .spec(requestSpecification)
                .body(lwa)
                .when().put(EndPoints.getLightWeightAccRegistrationUrl())
                .then().extract().response();
    }

    @Step("Update node API request with customToken")
    public static Response updateNodeRegistration(LightWeightAccount lwa, String customToken){
        return   given()
                .spec(requestSpecificationForCustomMethod)
                .auth().oauth2(customToken).log().uri().and().log().method()
                .body(lwa).log().body(true)
                .when().put(EndPoints.getLightWeightAccRegistrationUrl())
                .then().log().all(true).extract().response();
    }

    @Step("Create node API request")
    public static Response newNodeRegistration(LightWeightAccount lwa){
        return   given()
                .spec(requestSpecification)
                .body(lwa)
                .when().post(EndPoints.getLightWeightAccRegistrationUrl())
                .then().extract().response();
    }

    @Step("Create node API request with customToken")
    public static Response newNodeRegistration(LightWeightAccount lwa, String customToken){
            return   given()
                    .spec(requestSpecificationForCustomMethod)
                    .auth().oauth2(customToken).log().uri().and().log().method()
                    .body(lwa).log().body(true)
                    .when().post(EndPoints.getLightWeightAccRegistrationUrl())
                    .then().log().all(true).extract().response();
        }


    @Step("Retrieve IOT device")
    public static Response retrieveIotDevice(LightWeightAccount lwa){

        return   given()
                .spec(requestSpecification)
                .when().get(EndPoints.getRetrieveNodeUrl(), lwa.getExternalId(), lwa.getNode().getNodeType())
                .then().extract().response();
    }

    @Step("Retrieve IOT device with customToken")
    public static Response retrieveIotDevice(LightWeightAccount lwa, String customToken){

        return   given()
                .spec(requestSpecificationForCustomMethod)
                .auth().oauth2(customToken).log().uri().and().log().method()
                .log().body(false)
                .when().get(EndPoints.getRetrieveNodeUrl(), lwa.getExternalId(), lwa.getNode().getNodeType())
                .then().log().all(true).extract().response();
    }

    @Step("Download processed file")
    public static Response downloadProcessedFile(String requestId){
        Response response = given()
                    .spec(requestSpecification)
                    .when().get(EndPoints.getDownloadProcessedFileUrl(), requestId)
                    .then()
        .extract().response();
        Integer code = response.statusCode();
        long startTime = System.currentTimeMillis();
        while (!code.equals(200) && (System.currentTimeMillis()-startTime)<60000){
            response = given()
                    .spec(requestSpecification)
                    .when().get(EndPoints.getDownloadProcessedFileUrl(), requestId)
                    .then()
                    .extract().response();
            code = response.statusCode();
        }
        return response;
        }

    @Step("Bulk create node API request with custom headers")
    public static Response bulkUploadNodes(File file, Map<String, ?> headers){
        return given().filter(new AllureRestAssured())
                .accept("application/json")
                .contentType("multipart/form-data")
                .headers(headers)
                .formParam("")
                .multiPart("file", file)
                .log().uri().log().method().log().headers().log().parameters()
                .auth().oauth2(BaseApiTest.token)
                .post(EndPoints.getLightWeightAccountBulkUploadUrl())
                .then().log().everything().extract().response();
    }

    @Step("Bulk create node API request")
    public static Response bulkUploadNodes(File file, Integer accountId){
        return given().filter(new AllureRestAssured())
                .contentType("multipart/form-data")
                .headers(new HashMap<String, String>(){{
                    put("responsibleId",accountId.toString());
                    put("ownershipMode", OwnershipModes.OWNER);
                    put("schemaType", CAMERA_CREATE_SCHEMA_TYPE);
                    put("nodeStatus","ACTIVE");
                    put("startDate", DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
                }})
                .multiPart("file", file)
                .log().uri().log().method().log().headers().log().parameters()
                .auth().oauth2(BaseApiTest.token)
                .post(EndPoints.getLightWeightAccountBulkUploadUrl())
                .then().log().everything().extract().response();
    }

    @Step("Bulk Retrieve IOT device with params")
    public static Response bulkRetrieveIotDeviceWithParams(int responsibleId,String requestDate, String page, String size, String sort, HashMap<String,Object> filter)
    {
        HashMap<String, Object> headers = new HashMap<String, Object>() {{
            put("accountId", responsibleId);
            put("requestedDate", requestDate);
        }};

        return   given()
            .spec(requestSpecification)
            .body(filter)
            .headers(headers)
            .queryParam("page", page)
            .queryParam("size", size)
            .queryParam("sort", sort)
            .when().post(EndPoints.getBulkRetrieveNodeUrl())
            .then().extract().response();
    }

    @Step("Bulk Retrieve IOT device without params")
    public static Response bulkRetrieveIotDeviceWithoutParams(String responsibleId, String requestDate)
    {

        HashMap<String, Object> headers = new HashMap<String, Object>() {{
            put("accountId", responsibleId);
            put("requestedDate", requestDate);
        }};

        return   given()
            .spec(requestSpecification)
            .body("{}")
            .headers(headers)
            .when().post(EndPoints.getBulkRetrieveNodeUrl())
            .then().extract().response();
    }

    @Step("Bulk Retrieve IOT device without params")
    public static Response bulkRetrieveIotDeviceWithoutParams(String responsibleId, String requestDate, String customToken)
    {

        HashMap<String, Object> headers = new HashMap<String, Object>() {{
            put("accountId", responsibleId);
            put("requestedDate", requestDate);
        }};

        return   given()
                .spec(requestSpecification)
                .auth().oauth2(customToken).log().uri().and().log().method()
                .body("{}")
                .headers(headers)
                .when().post(EndPoints.getBulkRetrieveNodeUrl())
                .then().extract().response();
    }
}
