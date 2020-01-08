package bluesky.restapi.managers;

public class AccountHierarchyEntityManager {

    private Integer accountHierarchyId;
    private String accountHierarchyName;

    public Integer getAccountHierarchyId() { return accountHierarchyId; }
    public String getAccountHierarchyName() { return accountHierarchyName; }

    public AccountHierarchyEntityManager setAccountHierarchyName(String accountHierarchyName) {
        this.accountHierarchyName = accountHierarchyName;
        return this;
    }

    public AccountHierarchyEntityManager setAccountHierarchyId(Integer accountHierarchyId) {
        this.accountHierarchyId = accountHierarchyId;
        return this;
    }

}
