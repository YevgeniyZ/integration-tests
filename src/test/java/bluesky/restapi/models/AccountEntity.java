package bluesky.restapi.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountEntity {

    private String password;

    private Integer dayOfWeek;

    private String accountType;

    private List<AccountViews> accountViews;

    private String confirmPassword;

    private String nameSpace;

    private String userName;

    private String vtEnd;

    private String vtStart;

    private String accountStartDate;

    private String responsibleEndDate;

    private Integer ancestorId;

    public String getAccountStartDate() {
        return accountStartDate;
    }

    public AccountEntity setAccountStartDate(String accountStartDate) {
        this.accountStartDate = accountStartDate;
        return this;
    }
    public AccountEntity setAccountType(String accountType) {
        this.accountType = accountType;
        return this;
    }

    public Integer getAncestorId() {

        return ancestorId;
    }

    public AccountEntity setAncestorId(Integer ancestorId) {

        this.ancestorId = ancestorId;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AccountEntity setPassword(String password) {
        this.password = password;
        return this;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public AccountEntity setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        return this;
    }

    public String getAccountType() {
        return accountType;
    }

    public AccountEntity setAccountViews(List<AccountViews> accountViews) {
        this.accountViews = accountViews;
        return this;
    }

    public List<AccountViews> getAccountViews() {
        return this.accountViews;
    }


    public String getConfirmPassword() {
        return confirmPassword;
    }

    public AccountEntity setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
        return this;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public AccountEntity setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public AccountEntity setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getVtEnd() {
        return vtEnd;
    }

    public AccountEntity setVtEnd(String vtEnd) {
        this.vtEnd = vtEnd;
        return this;
    }

    public String getVtStart() {
        return vtStart;
    }

    public AccountEntity setVtStart(String vtStart) {
        this.vtStart = vtStart;
        return this;
    }

    public AccountEntity setResponsibleEndDate(String responsibleEndDate) {
        this.responsibleEndDate = responsibleEndDate;
        return this;
    }

    public String getResponsibleEndDate() {
        return responsibleEndDate;
    }


    public static class AccountViews {

        private String viewName;

        private Props props;

        public String getViewName() {
            return viewName;
        }

        public AccountViews setViewName(String viewName) {
            this.viewName = viewName;
            return this;
        }

        public Props getProps() {
            return props;
        }

        public AccountViews setProps(Props props) {
            this.props = props;
            return this;
        }
    }

    public static class Props {

        private String CURRENCY;

        private String UsageCycleType;

        private String BILLABLE;

        public String getCURRENCY() {
            return CURRENCY;
        }

        public Props setCURRENCY(String CURRENCY) {
            this.CURRENCY = CURRENCY;
            return this;
        }

        public String getUsageCycleType() {
            return UsageCycleType;
        }

        public Props setUsageCycleType(String UsageCycleType) {
            this.UsageCycleType = UsageCycleType;
            return this;
        }

        public String getBILLABLE() {
            return BILLABLE;
        }

        public Props setBILLABLE(String BILLABLE) {
            this.BILLABLE = BILLABLE;
            return this;
        }
    }

}