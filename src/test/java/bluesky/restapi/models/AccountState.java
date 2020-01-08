package bluesky.restapi.models;

public class AccountState {

    private Integer accountId;

    private String vtEnd;

    private String vtStart;

    private String accountState;

    public Integer getAccountId ()
    {
        return accountId;
    }

    public AccountState setAccountId (Integer accountId)
    {
        this.accountId = accountId;
        return this;
    }

    public String getVtEnd ()
    {
        return vtEnd;
    }

    public AccountState setVtEnd (String vtEnd)
    {
        this.vtEnd = vtEnd;
        return this;
    }

    public String getVtStart ()
    {
        return vtStart;
    }

    public AccountState setVtStart (String vtStart)
    {
        this.vtStart = vtStart;
        return this;
    }

    public String getAccountState()
    {
        return accountState;
    }

    public AccountState setAccountState(String accountState)
    {
        this.accountState = accountState;
        return this;
    }
}
