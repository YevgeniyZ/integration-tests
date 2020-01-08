package bluesky.restapi.base;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.HashMap;

public class BaseAssertions {


    @Step("Verify node attributes {1} are correct ")
    public static void verifyNodeAttributes(Response response, HashMap<String, Object> nodeAttributes){
        nodeAttributes.forEach((key, value) -> Assert.assertEquals(response.getBody().jsonPath().get("node.attributes." + key), value, "Field " + key + " is incorrect or not found"));
    }

    @Step("Verify lwa fields {1} are correct")
    public static void verifyLwaFields(Response response, HashMap<String, Object> lwaFields){
        Assert.assertEquals(response.getStatusCode(), 200, "Response code is incorrect");
        lwaFields.forEach((key, value) -> Assert.assertEquals(response.getBody().jsonPath().get(String.valueOf(key)), value, "Field " + key + " is incorrect or not found"));
    }

    @Step("Verify node fields {1} are correct")
    public static void verifyNodeFields(Response response, HashMap<String, Object> nodeFields){
        nodeFields.forEach((key, value) -> Assert.assertEquals(response.getBody().jsonPath().get("node." + key), value, "Field " + key + " is incorrect or not found"));
    }

    @Step("Verify error response is correct code is {0}, message is {1}")
    public static void verifyErrorResponse(Response response, int expectedResponseCode, String expectedErrorMessage){
        Assert.assertEquals(response.getStatusCode(), expectedResponseCode, "Response code is incorrect");
        Assert.assertEquals(response.getBody().jsonPath().get("message"), expectedErrorMessage,
            "Error message is not correct: " );
    }

    @Step("Verify error response is correct code is {1}, contains message is {2}")
    public static void verifyErrorResponseContainsMessage(Response response, int expectedResponseCode, String expectedErrorMessage){
        Assert.assertEquals(response.getStatusCode(), expectedResponseCode, "Response code is incorrect");
        Assert.assertTrue(response.getBody().jsonPath().get("message").toString().contains(expectedErrorMessage),
            String.format("Error message is not correct: expected: '%s' but found: '%s'",
                expectedErrorMessage, response.jsonPath().get("message")));
    }

    @Step("Verify field {0} is returned in response")
    public static void verifyFieldIsReturnedInResponse(Response response, String jsonPathToField){
        Assert.assertNotNull(response.getBody().jsonPath().get(jsonPathToField), "Field is not in response");
    }

    @Step("Verify node attributes {1} are correct ")
    public static void verifyGetLwaFields(Response response, String request, HashMap<String,
        Object> nodeFields){
        nodeFields.forEach((key, value) ->
            Assert.assertEquals(response.getBody().jsonPath().getString(request + key),
                value, "Field " + key + " is incorrect or not found"));
    }

    @Step("Verify lwa fields {1} are correct")
    public static void verifyGetNodeAttributes(Response response, String request, HashMap<String,
        Object> lwaFields){
        lwaFields.forEach((key, value) ->
            Assert.assertEquals(response.getBody().jsonPath().getString(request + key),
                value, "Field " + key + " is incorrect or not found"));
    }
}
