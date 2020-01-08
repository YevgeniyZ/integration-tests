package bluesky.restapi.managers;

import bluesky.restapi.models.UserCapabilities;

import java.util.ArrayList;
import java.util.List;

public class UserCapabilityManager extends BaseManager {
  public UserCapabilities addUserCapability() {

    UserCapabilities.ManageAccounts manageAccounts = new UserCapabilities.ManageAccounts()
        .setAccessHierarchy(1)
        .setAccessLevel("READ")
        .setAccessType("CURRENT_NODE");

    List<UserCapabilities.ManageAccounts> manageAccountsList = new ArrayList<>();
    manageAccountsList.add(manageAccounts);

    return addDefaultUserCapability()
        .setManageAccounts(manageAccountsList);
  }

  private static UserCapabilities addDefaultUserCapability(){

    List<String> applicationsList = new ArrayList<>();
    applicationsList.add("MetraNet");
    applicationsList.add("MetraCare");

    List<String> accessLevelList = new ArrayList<>();
    accessLevelList.add("READ");
    accessLevelList.add("WRITE");

    List<String> businessEntityExtensionsList = new ArrayList<>();
    businessEntityExtensionsList.add("");
    businessEntityExtensionsList.add("");

    UserCapabilities.PermitLimitations permitLimitations = new UserCapabilities.PermitLimitations()
        .setCurrency(new UserCapabilities.Currency()
            .setValue("USD"))
        .setLimit(10)
        .setOperator("GREATER_THAN");

    List<UserCapabilities.PermitLimitations> limitPermitationsList = new ArrayList<>();
    limitPermitationsList.add(permitLimitations);

    return new UserCapabilities()
        .setUserId("jcsr")
        .setCapabilityTypeId(1)
        .setApplications(applicationsList)
        .setAccessLevel(accessLevelList)
        .setPermitLimitations(limitPermitationsList);
  }
}

