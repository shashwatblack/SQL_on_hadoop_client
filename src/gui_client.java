import com.sun.org.apache.xpath.internal.operations.Variable;
import com.sun.webpane.sg.CursorManagerImpl;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by shashwat on 7/27/14.
 */
public class gui_client extends JFrame{
    private JPanel rootPanel;
    private JTabbedPane tabPanel;
    private JTextField textInput;
    private JButton bttnExecute;
    private JTable tableOutput;
    private JList listTables;
    private JButton bttnClear;
    private JButton bttnReset;
    private JTable batchTableOutput;
    private JList batchTablesList;
    private JTextArea batchQueriesTextArea;
    private JButton loadFileButton;
    private JButton executeNextButton;
    private JButton executeAllButton;
    private JButton batchResetButton;
    private JButton bttnSave;
    private JButton batchSaveButton;

    private FileDialog fd;

    private Connection dbConnection;
    private Statement stmt;
    private List<String> tables;

    public gui_client(){
        super("SQL on Hadoop Client");
        setContentPane(rootPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        tabPanel.setSelectedIndex(0);
        try {
            bttnSave.setIcon(new ImageIcon(ImageIO.read(new File("resources/16_save.png"))));
            bttnSave.setBorder(BorderFactory.createEmptyBorder());
            bttnSave.setContentAreaFilled(false);
            batchSaveButton.setIcon(new ImageIcon(ImageIO.read(new File("resources/16_save.png"))));
            batchSaveButton.setBorder(BorderFactory.createEmptyBorder());
            batchSaveButton.setContentAreaFilled(false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //setSize(550, 400);
        pack();

        //Connect Database
        try {
            // == For postgres connection ==
            //dbConnection = DriverManager.getConnection(
            //        "jdbc:postgresql://localhost:5432/postgres", "postgres", "password");
            // dbConnection = DriverManager.getConnection(
            //        "jdbc:postgresql://192.168.0.209:5432/postgres", "postgres", "password");
            // == For mysql connection ==
             dbConnection = DriverManager.getConnection("jdbc:mysql://localhost/sampledb?" +
                    "user=root&password=password");
            // create Statement to work with
            stmt = dbConnection.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
        }
        catch (SQLException ex){
            //ERROR Message Box
            JOptionPane.showMessageDialog(this, ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }

        // initialize file dialog
        fd = new FileDialog(this, "Choose a file", FileDialog.LOAD);
        fd.setDirectory("\\");
        //fd.setFile("*.csv");   // Doesn't work for unknown reasons
        resetClient();

        ///////////////////////////////////////////////////////
        /////////// ALL EVENT ACTIONS WRITTEN BELOW ///////////
        ///////////////////////////////////////////////////////
        bttnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                textInput.setText("");
                textInput.requestFocusInWindow();
            }
        });
        bttnReset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                resetClient();
            }
        });
        batchResetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                resetClient();
            }
        });
        batchTablesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                insertTableName(mouseEvent, batchQueriesTextArea);
            }
        });

        listTables.addMouseListener(new MouseAdapter() {
            //@Override
            public void mouseClicked(MouseEvent mouseEvent) {
                insertTableName(mouseEvent, textInput);
            }
        });
        batchSaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveResults((DefaultTableModel) batchTableOutput.getModel(), "batchQueryResult");
            }
        });
        bttnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveResults((DefaultTableModel)tableOutput.getModel(), "queryResult");
            }
        });
        loadFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fd.setVisible(true);
                String filename = fd.getDirectory() + fd.getFile();
                if (filename != null)
                    readQueriesFromFile(filename);
            }
        });
        executeAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                boolean choice = JOptionPane.showConfirmDialog(null, "Do you want to save the output?",
                        "Save Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                File file; String fileName = "";
                if (choice){
                    //retrive save file name/location
                    JFileChooser chooser = new JFileChooser( );
                    chooser.setSelectedFile(new File("batchQueryResult" + ".csv"));
                    int state = chooser.showSaveDialog(null);
                    file = chooser.getSelectedFile();
                    //if filename == null choice = NO
                    if (file == null || state != JFileChooser.APPROVE_OPTION) {
                        choice = false;
                    } else {
                        fileName = file.getPath();
                    }
                }
                String[] queries = batchQueriesTextArea.getText().toString().split(";");
                for(int i = 0; i<queries.length; i++)
                {
                    // we ensure that there are no spaces before or after the request string
                    // in order to not execute empty statements
                    queries[i] = queries[i].trim();
                    if(!queries[i].equals(""))
                    {
                        if (!executeQuery(queries[i], batchTableOutput)) {
                            String [] msg = {"Do you wish to continue with execution?", "There's been an error"};
                            if (JOptionPane.showConfirmDialog(null, msg[0], msg[1], JOptionPane.YES_NO_OPTION)
                                    != JOptionPane.YES_OPTION){
                                break;
                            }
                        } else if (choice) {    //successful query execution + File Save approval
                            //save to file filename%i%.csv
                            String[] parts = fileName.split("\\.(?!.*\\..*$)");
                            file = new File(parts[0]+"_"+i+(parts.length>1?"."+parts[1]:""));
                            if (file.exists() == true) {
                                    file.delete();
                            }
                            writeToFile((DefaultTableModel)batchTableOutput.getModel(), file);
                        }
                    }
                }
                JOptionPane.showMessageDialog(null, "All queries executed.");
            }
        });
        executeNextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int cursorPosition = batchQueriesTextArea.getCaretPosition();
                String[] queries = batchQueriesTextArea.getText().toString().split(";");
                int indices = 0;
                for (int i = 0; i < queries.length; i++) {
                    indices += queries[i].length()+1; //one for the semicolon by which it is split
                    if (indices>cursorPosition){
                        String q = queries[i];
                        if(!q.trim().equals("")) {
                            executeQuery(q, batchTableOutput);
                        }
                        try {
                            batchQueriesTextArea.setCaretPosition(indices + queries[i + 1].length() - queries[i + 1].replaceAll("^\\s*", "").length());
                        } catch (IndexOutOfBoundsException ex){
                            batchQueriesTextArea.setCaretPosition(batchQueriesTextArea.getText().length());
                        }
                        batchQueriesTextArea.requestFocusInWindow();
                        break;
                    }
                }
            }
        });
        bttnExecute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) { // Button Clicked
                //Get the query
                String inputQuery = textInput.getText();
                System.out.println(inputQuery);
                //Validate query
                validateQuery(inputQuery);
                //execute query
                executeQuery(inputQuery, tableOutput);

            }
        });
        textInput.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
                if (textInput.getText().equals("Write Query Here")) textInput.selectAll();
            }
        });
        textInput.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                if ( keyEvent.getKeyChar() == '\n'){
                    bttnExecute.doClick();
                }
            }
            @Override
            public void keyPressed(KeyEvent keyEvent) {}
            @Override
            public void keyReleased(KeyEvent keyEvent) {}
        });
    }
    private void insertTableName(MouseEvent mouseEvent, javax.swing.text.JTextComponent textBox){
        if (mouseEvent.getClickCount() == 2) {
            String tableName = (tables.get(((JList) mouseEvent.getSource()).locationToIndex(mouseEvent.getPoint())));
            String query = textBox.getText();
            int cursorBegin = textBox.getSelectionStart();
            int cursorEnd = textBox.getSelectionEnd();
            if (cursorBegin == cursorEnd) {
                if (cursorEnd == query.length()) {
                    textBox.setText(query + " " + tableName + " ");
                    textBox.setCaretPosition(cursorEnd + tableName.length() + 2);
                } else {
                    textBox.setText(query.substring(0, cursorEnd) + tableName + query.substring(cursorEnd));
                    textBox.setCaretPosition(cursorEnd + tableName.length());
                }
            } else {
                textBox.setText(query.substring(0, cursorBegin) + tableName + query.substring(cursorEnd));
                textBox.setCaretPosition(cursorBegin + tableName.length());
            }
            textBox.requestFocusInWindow();
        }
    }

    private boolean executeQuery(String inputQuery, JTable outputTable){
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
            outputTable.setModel(new DefaultTableModel(allRows, colNames));
            //enable save buttons
            if (outputTable == tableOutput) {
                bttnSave.setEnabled(true);
            } else {
                batchSaveButton.setEnabled(true);
            }
        }
        catch (SQLException ex){
            //ERROR Message Box
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.out.println("Something's wrong - " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean validateQuery(String query){
        //code
        return true;
    }

    private boolean saveResults(DefaultTableModel tableModel, String fileName){
        JFileChooser chooser = new JFileChooser( );
        chooser.setSelectedFile(new File(fileName + ".csv"));
        int state = chooser.showSaveDialog(null);
        File file = chooser.getSelectedFile();
        if (file != null && state == JFileChooser.APPROVE_OPTION)
        {
            if (file.exists() == true) {
                // confirm with the user
                int i = JOptionPane.showConfirmDialog(null, "Overwrite " + file.getName() + "?", "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
                if (!(i != JOptionPane.YES_OPTION)) {
                    file.delete();
                }
            }
            if (writeToFile(tableModel, file)){
                JOptionPane.showMessageDialog(null, "Success. Results saved in the file.");
                return true;
            }
        }
        return true;
    }

    private boolean writeToFile(DefaultTableModel tableModel, File file) {
        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file,true));
            PrintWriter fileWriter = new PrintWriter(bufferedWriter);
            for(int i=0; i<tableModel.getRowCount() ; ++i)
            {
                for(int j=0; j<tableModel.getColumnCount(); ++j)
                {
                    String s;
                    Object obj = tableModel.getValueAt(i, j);
                    if (obj != null) {
                        s = obj.toString().replaceAll(",", "");
                        fileWriter.print(s + ",");
                    }
                    else {
                        fileWriter.print(",");
                    }
                }
                fileWriter.println("");
            }
            fileWriter.close();
        } catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage());
            return false;
        }
        return true;
    }

    private boolean readQueriesFromFile(String filePath) {
        BufferedReader br = null;
        String line;
        StringBuffer lineBuffer = new StringBuffer();

        try {
            br = new BufferedReader(new FileReader(filePath));
            while((line = br.readLine()) != null)
            {
                lineBuffer.append(line + " ");
            }
            br.close();

            // here is our splitter ! We use ";" as a delimiter for each request
            // then we are sure to have well formed statements
            String[] queries = lineBuffer.toString().split(";");
            batchQueriesTextArea.setText("");
            for(int i = 0; i<queries.length; i++)
            {
                // we ensure that there is no spaces before or after the request string
                // in order to not execute empty statements
                queries[i] = queries[i].trim();
                if(!queries[i].equals(""))
                {
                    batchQueriesTextArea.append(queries[i] + ";\n");
                    System.out.println(">> "+queries[i]);
                }
            }
            batchQueriesTextArea.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    private boolean resetClient(){
        // Clear output tables
        String[] eA = {" ", " ", " ", " "};
        tableOutput.setModel(new DefaultTableModel(new String[][]{eA, eA, eA, eA, eA, eA, eA}, eA));
        batchTableOutput.setModel(new DefaultTableModel(new String[][]{eA, eA, eA, eA, eA, eA, eA}, eA));
        // Disable save buttons
        bttnSave.setEnabled(false);
        batchSaveButton.setEnabled(false);
        // Set queries input area
        textInput.setText("Write Query Here");
        textInput.setText("Select * from offices");
        batchQueriesTextArea.setText("");
        // Reset table names in database
        try{
            DatabaseMetaData dbmd = dbConnection.getMetaData();

            // initialize table names into list
            ResultSet dbrs = dbmd.getTables(null, null, "%", null);
            tables = new ArrayList<String>();
            while (dbrs.next()) {
                tables.add(dbrs.getString(3));
            }
            listTables.setListData(tables.toArray());
            batchTablesList.setListData(tables.toArray());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private void showMessage(JFrame frame, String msg){
        JOptionPane.showMessageDialog(
                frame,
                "<html><body><p style='width: 300px;'>"+msg+"</body></html>",
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
