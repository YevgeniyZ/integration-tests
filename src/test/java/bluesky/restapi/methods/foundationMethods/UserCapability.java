package bluesky.restapi.methods.foundationMethods;

import bluesky.restapi.methods.customerMethods.BaseApiMethod;
import bluesky.restapi.methods.customerMethods.EndPoints;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;

import java.sql.SQLException;

import static bluesky.restapi.base.BaseApiTest.token;
import static bluesky.restapi.helpers.DBHelper.getCapabilityIdFromDB;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public class UserCapability extends BaseApiMethod {

    private static boolean addUserCapability(String capability) {

        Integer capabilityId = Integer.parseInt(capability);
        RestAssured.useRelaxedHTTPSValidation();
        Response response = given()
                .auth().oauth2(token)
                .log().ifValidationFails()
                .contentType(JSON)
                .body("{}").post(EndPoints.getAddUserCapabilityUrl(), 137, capabilityId);
        //verify response and return result
        return response.statusCode() == 200;
    }

    public static void addSystemUserCapabilitiesForStatusChange() throws SQLException {
        boolean result1, result2, result3;
        //Get capabilities Id from DB
        String activeToClosedCapability = getCapabilityIdFromDB("Metratech.MTUpdAccFromActiveToClosedCapability");
        String activeToSuspendedCapability = getCapabilityIdFromDB("Metratech.MTUpdAccFromActiveToSuspendedCapability");
        String suspendedToActiveCapability = getCapabilityIdFromDB("Metratech.MTUpdAccFromSuspendedToActiveCapability");

        //Add capability to System User - Admin
        result1 = addUserCapability(activeToClosedCapability);
        result2 = addUserCapability(activeToSuspendedCapability);
        result3 = addUserCapability(suspendedToActiveCapability);

        //if capability is successfully added to user than tests need to rerun
        if (result1 || result2 || result3)
            Assert.fail("Capability is successfully added to user. Run tests again");
    }
}
