package bluesky.restapi.managers;

import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.NodeStatus;
import bluesky.restapi.models.NodeTypes;
import bluesky.restapi.models.OwnershipModes;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;



public class LightWeightAccountManager extends BaseManager{


    public static LightWeightAccount.Node createDefaultNode(){
        return new LightWeightAccount.Node()
                .setAttributes(new HashMap<String,Object>(){{
                    put("msisdn","value1");
                    put("imsi","value2");
                    put("oem","11");
                    put("location","12356");
                    put("model","1234");
                }})
                .setEventType("Str1")
                .setId("Str2")
                .setNodeType(NodeTypes.NODE_TYPE_CAMERA)
                .setSchemaType("Str4");
    }

    public static LightWeightAccount createDefaultLightWeightAccount(LightWeightAccount.Node node){
        return new LightWeightAccount()
                .setExternalId("lwaExternalId"+ incrementIndex()+ RandomStringUtils.randomAlphabetic(5))
                .setOwnershipMode(OwnershipModes.OWNER)
                .setNode(node)
                .setNodeStatus(NodeStatus.ACTIVE)
                .setOrderId("Str5")
                .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API));
    }
}
