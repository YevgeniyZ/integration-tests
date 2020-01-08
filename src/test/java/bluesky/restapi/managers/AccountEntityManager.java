package bluesky.restapi.managers;

import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.AccountState;
import bluesky.restapi.models.AccountStatus;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;


public class AccountEntityManager extends BaseManager{



  public AccountEntity createAccountEntity() {
        //create Account View object
        AccountEntity.AccountViews accountViews = new AccountEntity.AccountViews()
                .setViewName("Internal")
                .setProps(new AccountEntity.Props()
                        .setCURRENCY("USD")
                        .setBILLABLE("Y")
                        .setUsageCycleType("DAILY"));

        //Add AccountView object to the list
        List<AccountEntity.AccountViews> accountViewsList = new ArrayList<>();
        accountViewsList.add(accountViews);

        //Create account with all necessary fields

        return createDefaultAccountEntity()
                .setDayOfWeek(5)
                .setAccountViews(accountViewsList);
    }

    private static AccountEntity createDefaultAccountEntity(){
        return new AccountEntity()
                .setUserName("accountEntity"+ incrementIndex()+ RandomStringUtils.randomAlphabetic(5))
                .setPassword("123")
                .setConfirmPassword("123")
                .setNameSpace("mt")
                .setAccountType("CorporateAccount");
    }

  public static void updateAccountStatus(Integer responsibleId, String accountStatus, String startDate) {

    AccountState accountState = new AccountState()
        .setAccountId(responsibleId)
        .setVtStart(startDate)
        .setAccountState(accountStatus);
    AccountEntityServiceMethods.accountStateUpdateRequest(accountState);
  }

  public static void updateAccountStatusToArchived(Integer responsibleId){
    String starDate = DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START);
    updateAccountStatus(responsibleId, AccountStatus.CLOSED, starDate);
    updateAccountStatus(responsibleId, AccountStatus.ARCHIVED, starDate);
  }

  public static void updateAccountStatusToSuspended(Integer responsibleId){
    String starDate = DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START);
    updateAccountStatus(responsibleId, AccountStatus.SUSPENDED, starDate);
  }

  public static void updateAccountStatusToClosed(Integer responsibleId){
    String starDate = DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START);
    updateAccountStatus(responsibleId, AccountStatus.CLOSED, starDate);
  }
}
