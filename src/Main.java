import java.sql.*;

public class Main {

    public static void main(String[] args) {

        System.out.println("Hello World!");

        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/postgres", "postgres", "password");
            Statement stmt = conn.createStatement();
            String query = "Select * from airport.f_frequency limit 8";
            System.out.println("the query is "+query);
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();
            String[] colNames = new String[colCount];
            for (int i=1; i<= colCount; i++) {
                colNames[i] = rsmd.getColumnName(i);
            }
            String[] row = new String[colCount];
            String outStr;
            int rowCount = 0;
            while (rs.next()){
                outStr = "";
                for (int i=0; i<colCount; i++){
                    row[i] = rs.getString(i+1);
                    outStr += "\t" + row[i];
                }
//                int supplierID = rs.getInt("SUP_ID");
//                float price = rs.getFloat("PRICE");
//                int sales = rs.getInt("SALES");
//                int total = rs.getInt("TOTAL");
                System.out.println(outStr);
                rowCount++;
            }

            System.out.println("Total number of rows = " + rowCount);
        }
        catch (SQLException ex){
            System.out.println(ex.toString());
            ex.printStackTrace();
        }
    }
}
