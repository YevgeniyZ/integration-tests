package bluesky.restapi.helpers;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeHelper {

  public static final String DATE_PATTERN_CUSTOMER_API = "yyyy-MM-dd'T'HH:MM:ss.ss'Z'";
  public static final String DATE_PATTERN_DEFAULT_START = "yyyy-MM-dd'T'00:00:00'Z'";
  public static final String DATE_PATTERN_DEFAULT_END = "yyyy-MM-dd'T'23:59:59.000'Z'";
  public static final String DATE_PATTERN_DEFAULT_START_DB = "yyyy-MM-dd 00:00:00.0";
  public static final String DATE_PATTERN_DEFAULT_END_DB = "yyyy-MM-dd 23:59:59.0";

  public static String setCustomDatePattern(String customDatePattern) {

    return customDatePattern;
  }

  public static String getMaxDateValueDb() {

    try {
      return DBHelper.getValueFromDB("select dbo.MTMaxDate ()", "");  // "2106-01-01 00:00:00.0";
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String getCurrentDate(String pattern) {

    ZonedDateTime zoneDate = getZonedDateTime();
    return zoneDate.format(DateTimeFormatter.ofPattern(pattern));
  }

  public static String getTomorrowDate(String pattern) {

    return getDatePlusDays(pattern, 1);
  }

  public static String getDatePlusDays(String pattern, int days) {

    ZonedDateTime zoneDate = getZonedDateTime();
    return zoneDate.plusDays(days).format(DateTimeFormatter.ofPattern(pattern));
  }

  public static String getYesterdayDay(String pattern) {

    return getDateMinusDays(pattern, 1);
  }

  public static String getDateMinusDays(String pattern, int days) {

    ZonedDateTime zoneDate = getZonedDateTime();
    return zoneDate.minusDays(days).format(DateTimeFormatter.ofPattern(pattern));
  }

  public static String getDateMinusSeconds(String dateTime, String pattern, int seconds) {

    SimpleDateFormat formattedDate = new SimpleDateFormat(pattern);
    Date date = null;
    try {
      date = formattedDate.parse(dateTime);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    Calendar calendar = Calendar.getInstance();
    assert date != null;
    calendar.setTime(date);
    calendar.add(Calendar.SECOND, -seconds);

    return formattedDate.format(calendar.getTime());
  }

  public static void waitForSeconds(int seconds) {

    try {
      Thread.sleep(1000 * seconds);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Converts MTMaxDate String to API format.(2038-01-01 00:00:00.0 to 2038-01-01T00:00:00Z)
   *
   * @param dateStr input
   * @return APIformat
   * @throws ParseException
   */
  public static String convertToAPIFormat(String dateStr) throws ParseException {

    SimpleDateFormat destFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    sourceFormat.setTimeZone(TimeZone.getDefault());
    Date date = sourceFormat.parse(dateStr);
    return destFormat.format(date);
  }

  public static String getValueFromDBForExpectedDateStringNoTimezone() {
    String expectedDateStringNoTimezone = "";
    expectedDateStringNoTimezone = DateTimeHelper.getMaxDateValueDb();
    return expectedDateStringNoTimezone;
  }

  private static ZonedDateTime getZonedDateTime() {

    Date date = new Date();
    return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
  }

}
