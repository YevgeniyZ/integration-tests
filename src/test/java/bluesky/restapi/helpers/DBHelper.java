package bluesky.restapi.helpers;

import bluesky.restapi.models.LightWeightAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBHelper {

  private static String dbUrl;
  private static String dbUsername;
  private static String dbPassword;

  private static Statement stmt = null;
  private static Connection con = null;

  private static String SELECT_NODE_SQL =
      "select a.end_date, a.json_attributes, a.settlement_id, a.node_type_id, a.external_id, a.ownership_mode, a"
          + ".responsible_id, a.id, a.order_id, a.status, a.start_date from t_lwa_node a , t_lwa_node_type b \n"
          + "where \n"
          + "a.node_type_id = b.node_type_id and \n"
          + "a.external_id = ? and b.node_Type = ?";

  static {
    try {
      dbUrl = String.format("jdbc:sqlserver://%s;DatabaseName=NetMeter", FileProcessingHelper.getProperty("dbUrl"));
      dbPassword = FileProcessingHelper.getProperty("dbPassword");
      dbUsername = FileProcessingHelper.getProperty("dbAdmin");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Step("Run SQL query: {0}")
  public static void runSQLQuery(String query) throws SQLException {

    try {
      stmt = createSQLServerConnection().createStatement();
      stmt.execute(query);
    } catch (SQLException e) {
      e.printStackTrace();
      throw new SQLException("Something wrong with SQL");
    } finally {
      if (stmt != null) {
        stmt.close();
      }
      if (con != null) {
        con.close();
      }
    }
  }

  @Step("Get value of {1} from DB")
  public static String getValueFromDB(String query, String columnLabel) throws SQLException {

    ResultSet rs = null;
    String dbValue = "";

    try {
      stmt = createSQLServerConnection().createStatement();
      rs = stmt.executeQuery(query);

      while (rs.next()) {
        dbValue = rs.getString(columnLabel);
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new SQLException("Something wrong with SQL");
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (stmt != null) {
        stmt.close();
      }
      if (con != null) {
        con.close();
      }
    }
    try {
      Allure.addAttachment("Value", "text/plain", dbValue);
    } catch (NullPointerException ignored) {
    }

    return dbValue;
  }

  /**
   * Getting all the result set Can be used only if the single record returns.
   * In other case only first row will be returned to map.
   *
   * @param sqlQuery sqlQuery
   * @return map
   */
  public static Map<String, Object> getResultMap(String sqlQuery) {

    Map<String, Object> columns = new HashMap<>();
    try {
      stmt = createSQLServerConnection().createStatement();
      ResultSet rs = stmt.executeQuery(sqlQuery);
      ResultSetMetaData metaData = rs.getMetaData();
      int colCount = metaData.getColumnCount();
      while (rs.next()) {
        for (int i = 1; i <= colCount; i++) {
          columns.put(metaData.getColumnLabel(i), rs.getObject(i));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return columns;
  }

  /**
   * Get a single node or a set of  node matching externalId and nodeType
   *
   * @param externalId externalId
   * @param nodeType   nodeType
   * @return List of LightWeightAccounts.
   */
  public static List<LightWeightAccount> getNodes(String externalId, String nodeType) {

    List<LightWeightAccount> lightWeightAccounts = new ArrayList<>();
    try {
      PreparedStatement statement = createSQLServerConnection().prepareStatement(SELECT_NODE_SQL);
      statement.setString(1, externalId);
      statement.setString(2, nodeType);
      ResultSet rs = statement.executeQuery();

      ObjectMapper objectMapper = new ObjectMapper();
      while (rs.next()) {
        LightWeightAccount lwa = new LightWeightAccount();
        lwa.setEndDate(rs.getTimestamp("end_date").toString());
        lwa.setStartDate(rs.getTimestamp("start_date").toString());
        LightWeightAccount.Node node = new LightWeightAccount.Node();
        node.setAttributes(objectMapper.readValue(rs.getString("json_attributes"), HashMap.class));
        lwa.setSettlementId(rs.getString("settlement_id"));
        lwa.setExternalId(rs.getString("external_id"));
        lwa.setOwnershipMode(rs.getString("ownership_mode"));
        lwa.setResponsibleId(rs.getString("responsible_id"));
        lwa.setOrderId(rs.getString("order_id"));
        lwa.setNodeStatus(rs.getString("status"));

        lwa.setNode(node);
        lightWeightAccounts.add(lwa);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return lightWeightAccounts;
  }

  public static void updateAccountStatusInDB(Integer responsibleId, String accountState) {

    String queryUpdateAccountState = String
        .format("update t_account_state set status = '%s' where id_acc = '%s'", accountState, responsibleId.toString());

    try {
      DBHelper.runSQLQuery(queryUpdateAccountState);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static String getCapabilityIdFromDB(String capability) throws SQLException {
    String query = String.format("select * from t_composite_capability_type where tx_progid = '%s'",
        capability);
    return DBHelper.getValueFromDB(query, "id_cap_type");
  }

  private static Connection createSQLServerConnection() {

    try {
      Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
      con = DriverManager.getConnection(
          dbUrl, dbUsername, dbPassword);
    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
    }
    return con;
  }

}


