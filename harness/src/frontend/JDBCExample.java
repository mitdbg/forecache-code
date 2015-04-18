package frontend;

import org.scidb.jdbc.IResultSetWrapper;
import org.scidb.jdbc.IStatementWrapper;

import utils.DBInterface;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

class JDBCExample
{
  public static void main(String [] args) throws IOException
  {
    try
    {
      Class.forName("org.scidb.jdbc.Driver");
    }
    catch (ClassNotFoundException e)
    {
      System.out.println("Driver is not in the CLASSPATH -> " + e);
    }

    try
    {
      Connection conn = DBInterface.getDefaultScidbConnection();
      Statement st = conn.createStatement();
      IStatementWrapper stWrapper = st.unwrap(IStatementWrapper.class);
      stWrapper.setAfl(true);
      ResultSet res = st.executeQuery("build(<a:double>[x=0:2,3,0,y=0:2,3,0],10)");
      ResultSetMetaData meta = res.getMetaData();

      System.out.println("Source array name: " + meta.getTableName(0));
      System.out.println(meta.getColumnCount() + " columns:");

      IResultSetWrapper resWrapper =
         res.unwrap(IResultSetWrapper.class);
      for (int i = 1; i <= meta.getColumnCount(); i++)
      {
        System.out.println(meta.getColumnName(i) + " - " + meta.getColumnTypeName(i)
           + " - is attribute:" + resWrapper.isColumnAttribute(i));
      }
      System.out.println("=====");

      System.out.println("x y a");
      System.out.println("-----");
      while(!res.isAfterLast())
      {
        System.out.println(res.getLong("x") + " " + res.getLong("y") + " "
           + res.getDouble("a"));
        res.next();
      }
    }
    catch (SQLException e)
    {
      System.out.println(e);
    }
  System.exit(0);
  }
}