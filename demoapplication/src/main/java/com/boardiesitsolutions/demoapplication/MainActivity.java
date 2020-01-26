package com.boardiesitsolutions.demoapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.BoardiesITSolutions.AndroidMySQLConnector.Connection;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.InvalidSQLPacketException;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.MySQLConnException;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.MySQLException;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.SQLColumnNotFoundException;
import com.BoardiesITSolutions.AndroidMySQLConnector.IConnectionInterface;
import com.BoardiesITSolutions.AndroidMySQLConnector.IResultInterface;
import com.BoardiesITSolutions.AndroidMySQLConnector.MySQLRow;
import com.BoardiesITSolutions.AndroidMySQLConnector.ResultSet;
import com.BoardiesITSolutions.AndroidMySQLConnector.Statement;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity
{
    private boolean databaseDetailsExpanded = false;

    private ImageButton btnExpandCollapseDBDetails;
    private LinearLayout dbDetailsContainer;

    private EditText txtServer;
    private EditText txtUsername;
    private EditText txtPassword;
    private EditText txtDatabase;
    private EditText txtPort;
    private Button btnSaveDetails;
    private SharedPreferences settings;
    private Button btnAddRandomRecord;

    //Create class instance of your connection. You should re-use this connection for
    //every action on your database.
    private Connection connection;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings = getPreferences(Context.MODE_PRIVATE);

        dbDetailsContainer = findViewById(R.id.dbSettingsContainer);

        btnExpandCollapseDBDetails = findViewById(R.id.btnExpandCollapse);
        btnExpandCollapseDBDetails.setOnClickListener(mBtnExpandCollapseDBDetailsClickListener);

        txtServer = findViewById(R.id.txtServer);
        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        txtDatabase = findViewById(R.id.txtDatabase);
        txtPort = findViewById(R.id.txtDatabasePort);
        btnSaveDetails = findViewById(R.id.btnSaveDBDetails);
        btnSaveDetails.setOnClickListener(mBtnSaveDetailsClickListener);

        txtServer.setText(settings.getString("server", ""));
        txtUsername.setText(settings.getString("username", ""));
        txtPassword.setText(settings.getString("password", ""));
        txtDatabase.setText(settings.getString("database", ""));
        txtPort.setText(settings.getString("port", "3306"));

        btnAddRandomRecord = findViewById(R.id.btnAddRandomRecord);
        btnAddRandomRecord.setOnClickListener(mBtnAddRandomRecordClickListener);
    }

    /**
     * Save the connection details to the application preferences.
     * Once the details are saved, a connection attempt will be made to the database
     * In the connectToDatabase method, if successfully connected (in the actionCompleted)
     * callback it will then call selectTableRecords to retrieve and display the test data in the database
     */
    private View.OnClickListener mBtnSaveDetailsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view)
        {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("server", txtServer.getText().toString());
            editor.putString("username", txtUsername.getText().toString());
            editor.putString("password", txtPassword.getText().toString());
            editor.putString("database", txtDatabase.getText().toString());
            editor.putString("port", txtPort.getText().toString());
            editor.apply();
            Toast.makeText(MainActivity.this, "Connection details saved successfully", Toast.LENGTH_SHORT).show();
            connectToDatabase();

        }
    };

    /**
     * Uses the saved connection settings to connect the database
     * Once done, it will retrieve the data from the database and display
     */
    private void connectToDatabase()
    {
        String server = settings.getString("server", "");
        String username = settings.getString("username", "");
        String password = settings.getString("password", "");
        String database = settings.getString("database", "");
        int port = Integer.parseInt(settings.getString("port", "3306"));

        //Set up the class instance of your connection
        connection = new Connection(server, username, password, port, database, new IConnectionInterface() {
            @Override
            public void actionCompleted()
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        Toast.makeText(MainActivity.this, "Connected to database successfully", Toast.LENGTH_SHORT).show();
                        selectTableRecords();
                    }
                });
            }

            @Override
            public void handleInvalidSQLPacketException(InvalidSQLPacketException ex)
            {
                showError("Invalid SQL Packet Exception: ", ex.toString());
            }

            @Override
            public void handleMySQLException(MySQLException ex)
            {
                showError("MySQL Exception", ex.toString());
            }

            @Override
            public void handleIOException(IOException ex)
            {
                showError("IO Error", ex.toString());
            }

            @Override
            public void handleMySQLConnException(MySQLConnException ex)
            {
                showError("MySQLCon Exception", ex.toString());
            }

            @Override
            public void handleException(Exception exception)
            {
                showError("General Exception", exception.toString());
            }
        });
    }

    /**
     * Adds a new record to the database. Just a string with the current date and time
     * The same process is done here for any query that doesn't return a resultset, e.g. INSERT,
     * UPDATE, REPLACE and DELETE. Once record has been added, it will run the selectRecords
     * method again to fetch the data
     */
    private View.OnClickListener mBtnAddRandomRecordClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view)
        {
            //We'll add a random record using the current date/time string
            Date currentTime = Calendar.getInstance().getTime();
            String fieldValue = "Newly added record at: " + currentTime.toString();
            //Escape the string - should always do this especially if value is from user input
            //to avoid SQL injection
            fieldValue = connection.escape_string(fieldValue);
            String query = "INSERT INTO test_table VALUES (NULL, '"+fieldValue+"')";
            Statement statement = connection.createStatement();
            statement.execute(query, new IConnectionInterface() {
                @Override
                public void actionCompleted()
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            Toast.makeText(MainActivity.this, "New record added successfully", Toast.LENGTH_SHORT).show();
                            selectTableRecords();
                        }
                    });
                }

                @Override
                public void handleInvalidSQLPacketException(InvalidSQLPacketException ex)
                {
                    showError("Invalid SQL Packet Exception", ex.toString());
                }

                @Override
                public void handleMySQLException(MySQLException ex)
                {
                    showError("MySQL Exception", ex.toString());
                }

                @Override
                public void handleIOException(IOException ex)
                {
                    showError("IO Exception", ex.toString());
                }

                @Override
                public void handleMySQLConnException(MySQLConnException ex)
                {
                    showError("MySQL Conn Exception", ex.toString());
                }

                @Override
                public void handleException(Exception exception)
                {
                    showError("General Exception", exception.toString());
                }
            });
        }
    };

    /**
     * Retrieves all records from the database and displays it
     */
    private void selectTableRecords()
    {
        Statement statement = connection.createStatement();
        statement.executeQuery("SELECT * FROM test_table", new IResultInterface() {
            @Override
            public void executionComplete(final ResultSet resultSet)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        TableLayout tableLayout = findViewById(R.id.resultTable);
                        tableLayout.removeAllViews();

                        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                        tableLayout.addView(inflater.inflate(R.layout.table_header, null, false));
                        MySQLRow row;
                        int i = 0;
                        while ((row = resultSet.getNextRow()) != null)
                        {
                            try
                            {

                                TableRow newRow = (TableRow) inflater.inflate(R.layout.result_row, null, false);
                                if (i % 2 == 0)
                                {
                                    newRow.setBackgroundColor(getResources().getColor(R.color.alternateRowColor));
                                }
                                TextView idCol = newRow.findViewById(R.id.lblId);
                                TextView fieldCol = newRow.findViewById(R.id.lblFieldValue);

                                idCol.setText(String.valueOf(row.getInt("id")));
                                fieldCol.setText(row.getString("Field1"));
                                tableLayout.addView(newRow);
                            }
                            catch (SQLColumnNotFoundException ex)
                            {
                                showError("SQL Column Not Found Exception", ex.toString());
                            }
                            i++;
                        }
                    }
                });
            }

            @Override
            public void handleInvalidSQLPacketException(InvalidSQLPacketException ex)
            {
                showError("Invalid SQL Packet Exception", ex.toString());
            }

            @Override
            public void handleMySQLException(MySQLException ex)
            {
                showError("Invalid MySQL Exception", ex.toString());
            }

            @Override
            public void handleIOException(IOException ex)
            {
                showError("IO Exception", ex.toString());
            }

            @Override
            public void handleMySQLConnException(MySQLConnException ex)
            {
                showError("MySQLConn Exception", ex.toString());
            }

            @Override
            public void handleException(Exception ex)
            {
                showError("General Exception", ex.toString());
            }
        });
    }

    /**
     * Hides and shows the database connection details
     */
    private View.OnClickListener mBtnExpandCollapseDBDetailsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view)
        {
            if (!databaseDetailsExpanded)
            {
                dbDetailsContainer.setVisibility(View.VISIBLE);
                btnExpandCollapseDBDetails.setImageResource(R.drawable.ic_arrow_drop_up);
                databaseDetailsExpanded = true;
            }
            else
            {
                dbDetailsContainer.setVisibility(View.GONE);
                btnExpandCollapseDBDetails.setImageResource(R.drawable.ic_arrow_drop_down);
                databaseDetailsExpanded = false;
            }
        }
    };

    /**
     * Called in any of the error call backs when the DB connection is used
     * to display an error if something has gone wrong
     * @param title
     * @param message
     */
    private void showError(final String title, final String message)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(message)
                        .setTitle(title);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }
}
