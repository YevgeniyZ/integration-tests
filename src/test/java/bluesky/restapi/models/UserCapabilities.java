package bluesky.restapi.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserCapabilities {

  private String userId;
  private int capabilityTypeId;
  private List<String> accessLevel;
  private List<String> applications;
  private List<String> businessEntityExtensions;
  private List<ManageAccounts> manageAccounts;
  private List<PermitLimitations> permitLimitations;

  public String getUserId() {

    return userId;
  }

  public UserCapabilities setUserId(String userId) {

    this.userId = userId;
    return this;
  }

  public int getCapabilityTypeId() {

    return capabilityTypeId;
  }

  public UserCapabilities setCapabilityTypeId(int capabilityTypeId) {

    this.capabilityTypeId = capabilityTypeId;
    return this;
  }

  public List<String> getAccessLevel() {

    return accessLevel;
  }

  public UserCapabilities setAccessLevel(List<String> accessLevel) {

    this.accessLevel = accessLevel;
    return this;
  }

  public List<String> getApplications() {

    return applications;
  }

  public UserCapabilities setApplications(List<String> applications) {

    this.applications = applications;
    return this;
  }

  public List<String> getBusinessEntityExtensions() {

    return businessEntityExtensions;
  }

  public void setBusinessEntityExtensions(List<String> businessEntityExtensions) {

    this.businessEntityExtensions = businessEntityExtensions;
  }

  public List<ManageAccounts> getManageAccounts() {

    return manageAccounts;
  }

  public UserCapabilities setManageAccounts(List<ManageAccounts> manageAccounts) {

    this.manageAccounts = manageAccounts;
    return this;
  }

  public List<PermitLimitations> getPermitLimitations() {

    return permitLimitations;
  }

  public UserCapabilities setPermitLimitations(List<PermitLimitations> permitLimitations) {

    this.permitLimitations = permitLimitations;
    return this;
  }

  public static class ManageAccounts {

    private long accessHierarchy;
    private String accessLevel;
    private String accessType;

    public long getAccessHierarchy() {

      return accessHierarchy;
    }

    public ManageAccounts setAccessHierarchy(long accessHierarchy) {

      this.accessHierarchy = accessHierarchy;
      return this;
    }

    public String getAccessLevel() {

      return accessLevel;
    }

    public ManageAccounts setAccessLevel(String accessLevel) {

      this.accessLevel = accessLevel;
      return this;
    }

    public String getAccessType() {

      return accessType;
    }

    public ManageAccounts setAccessType(String accessType) {

      this.accessType = accessType;
      return this;
    }

  }


  public static class PermitLimitations {

    private Currency currency;
    private int limit;
    private String operator;

    public Currency getCurrency() {

      return currency;
    }

    public PermitLimitations setCurrency(Currency currency) {

      this.currency = currency;
      return this;
    }

    public int getLimit() {

      return limit;
    }

    public PermitLimitations setLimit(int limit) {

      this.limit = limit;
      return this;
    }

    public String getOperator() {

      return operator;
    }

    public PermitLimitations setOperator(String operator) {

      this.operator = operator;
      return this;
    }
  }


  public static class Currency {

    private String value;

    public String getValue() {

      return value;
    }

    public Currency setValue(String value) {

      this.value = value;
      return this;
    }
  }

}
	