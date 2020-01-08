package bluesky.restapi.base;

import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.methods.customerMethods.EndPoints;
import bluesky.restapi.helpers.TokenAccessHelper;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import java.sql.SQLException;
import java.text.ParseException;

import static bluesky.restapi.helpers.ClientRegistrationHelper.tokenRegistration;
import static bluesky.restapi.methods.foundationMethods.UserCapability.addSystemUserCapabilitiesForStatusChange;

public class BaseApiTest {

    public static String token;
    public static String EXPECTED_DATE_STRING_WITH_TIMEZONE;
    protected static String EXPECTED_DATE_STRING_NO_TIMEZONE;
    public static final String SELECT_NODE_QUERY = "select * from t_lwa_node where external_id = '%s'";

    @BeforeSuite
    public static void Initialize() throws SQLException {

        //Skip SSL certificate
        RestAssured.useRelaxedHTTPSValidation();

        //Set base uri for API
        RestAssured.baseURI = EndPoints.getBaseUrl();
        RestAssured.port = 8711;

        //Token registration
        tokenRegistration();

        //get EXPECTED_DATE_STRING_NO_TIMEZONE from DB
        EXPECTED_DATE_STRING_NO_TIMEZONE = DateTimeHelper.getValueFromDBForExpectedDateStringNoTimezone();

        //get EXPECTED_DATE_STRING_WITH_TIMEZONE from DB
        try {
            EXPECTED_DATE_STRING_WITH_TIMEZONE = DateTimeHelper.convertToAPIFormat(EXPECTED_DATE_STRING_NO_TIMEZONE);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //Add system user capabilities for moving account statuses
        token = TokenAccessHelper.getAuthorizationToken();
        addSystemUserCapabilitiesForStatusChange();
    }

    @BeforeTest
    public void StartUp() {

        token = TokenAccessHelper.getAuthorizationToken();

    }
}
