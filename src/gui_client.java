import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

/**
 * Created by shashwat on 7/27/14.
 */
public class gui_client extends JFrame{
    private JPanel rootPanel;
    private JTabbedPane tabPanel;
    private JTextField textInput;
    private JButton bttnExecute;
    private JTable tableOutput;
    private JList list1;
    private JButton clearButton;
    private JButton resetButton;
    private Connection dbConnection;
    Statement stmt;


    public gui_client(){
        super("SQL on Hadoop Client");
        setContentPane(rootPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 400);
        //pack();

        //Connect Database
        try {
            //dbConnection = DriverManager.getConnection(
            //        "jdbc:postgresql://localhost:5432/postgres", "postgres", "password");
            dbConnection = DriverManager.getConnection("jdbc:mysql://localhost/sampledb?" +
                    "user=root&password=password");
            DatabaseMetaData dbmd = dbConnection.getMetaData();
            ResultSet dbrs = dbmd.getTables(null, null, "%", null);
            while (dbrs.next()) {
                System.out.println(dbrs.getString(3));
            }
            stmt = dbConnection.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
        }
        catch (SQLException ex){
            //ERROR Message Box
            System.out.println(ex.toString());
            ex.printStackTrace();
            return;
        }
        String[] emptyArray = {" ", " ", " ", " "};
        tableOutput.setModel(new DefaultTableModel(new String[][]{emptyArray, emptyArray, emptyArray, emptyArray, emptyArray, emptyArray, emptyArray}, emptyArray));

        bttnExecute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) { // Button Clicked
                //Get the query
                String inputQuery = textInput.getText();
                System.out.println(inputQuery);
                //Validate query
                validateQuery(inputQuery);
                //execute query
                System.out.println("Executing \"" + inputQuery + "\"");
                try {
                    ResultSet rs = stmt.executeQuery(inputQuery);
                    System.out.println("Query executed");
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int colCount = rsmd.getColumnCount();
                    String[] colNames = new String[colCount];
                    for (int i = 0; i < colCount; i++) {
                        colNames[i] = rsmd.getColumnName(i+1);
                    }
                    String[] row = new String[colCount];
                    int rowCount = 0;
                    try {
                        rs.last();
                        rowCount = rs.getRow();
                        rs.beforeFirst();
                    }
                    catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    String[][] allRows = new String[rowCount][colCount];
                    int count = 0;
                    while (rs.next()) {
                        for (int i = 0; i < colCount; i++) {
                            allRows[count][i] = rs.getString(i + 1);
                        }
                        count++;
                    }
                    //output results
                    tableOutput.setModel(new DefaultTableModel(allRows, colNames));
                }
                catch (SQLException ex){
                    //ERROR Message Box
                    System.out.println("Something's wrong - " + ex.toString());
                    ex.printStackTrace();
                }

            }
        });
        textInput.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
                if (textInput.getText().equals("Write Query Here")) {
                    textInput.selectAll();
                }
            }
        });
    }

    private boolean validateQuery(String query){
        //code
        return true;
    }
}
