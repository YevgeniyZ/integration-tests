package bluesky.restapi.helpers;

import java.io.IOException;

public class ServicesHelper {

    private static String[] startEcbCustomerService = { "net", "start", "ecb-customer-service-winsvc"};
    private static String[] startEcbOauthService = { "net", "start", "ecb-oauth-winsvc"};
    private static String[] startEcbZuulGatewayService = { "net", "start", "ecb-api-zuul-gateway-winsvc"};
    private static String[] startEcbFoundationService = { "net", "start", "ecb-foundation-service-winsvc"};

    private static String[] stopEcbCustomerService = { "net", "stop", "ecb-customer-service-winsvc"};
    private static String[] stopEcbOauthService = { "net", "stop", "ecb-oauth-winsvc"};
    private static String[] stopEcbZuulGatewayService = { "net", "stop", "ecb-api-zuul-gateway-winsvc"};
    private static String[] stopEcbFoundationService = { "net", "stop", "ecb-foundation-service-winsvc"};

    private static String[] startMsmq = {"net","start","msmq","/y"};
    private static String[] startPipeline = {"net","start","pipeline"};
    private static String[] startActivityService = {"net","start","activityservices"};
    private static String[] startIis = {"net","start","iisadmin","/y"};
    private static String[] startW3SVC = {"net","start","w3svc"};


    public static void startEcbApiServices() throws IOException, InterruptedException {

        Runtime runtime = Runtime.getRuntime();


            Process process = runtime.exec(startEcbOauthService);
            process.waitFor();
            process = runtime.exec(startEcbCustomerService);
            process.waitFor();
            process = runtime.exec(startEcbZuulGatewayService);
            process.waitFor();
            process = runtime.exec(startEcbFoundationService);
            process.waitFor();


    }

    public static void stopEcbApiServices() throws IOException, InterruptedException{

        Runtime runtime = Runtime.getRuntime();

        try {
            Process process = runtime.exec(stopEcbOauthService);
            process.waitFor();
            process = runtime.exec(stopEcbCustomerService);
            process.waitFor();
            process = runtime.exec(stopEcbZuulGatewayService);
            process.waitFor();
            process = runtime.exec(stopEcbFoundationService);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void startMetraTechServices(){

        Runtime runtime = Runtime.getRuntime();

        try {
            Process process = runtime.exec(startMsmq);
            process.waitFor();
            process = runtime.exec(startPipeline);
            process.waitFor();
            process = runtime.exec(startActivityService);
            process.waitFor();
            process = runtime.exec(startIis);
            process.waitFor();
            process = runtime.exec(startW3SVC);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

}
