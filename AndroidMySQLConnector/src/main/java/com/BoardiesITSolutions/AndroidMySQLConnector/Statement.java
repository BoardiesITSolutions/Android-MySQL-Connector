package com.BoardiesITSolutions.AndroidMySQLConnector;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.InvalidSQLPacketException;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.MySQLConnException;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.MySQLException;
import com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager.COM_Query;
import com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager.COM_QueryResponse;
import com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager.MySQLErrorPacket;
import com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager.MySQLOKPacket;

import java.io.IOException;
import java.util.concurrent.Semaphore;

public class Statement
{
    Connection mysqlConn;
    int affectedRows = 0;

    public Statement(Connection mysqlConn)
    {
        this.mysqlConn = mysqlConn;
    }

    /**
     * Get the number of rows that were affected by the last query
     * @return
     */
    public int getAffectedRows()
    {
        return this.affectedRows;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void execute(String query, final IConnectionInterface iConnectionInterface)
    {
        try {
            this.mysqlConn.resetPacketSequenceNumber(true);
            COM_Query comQuery = new COM_Query(this.mysqlConn, COM_Query.COM_QUERY, query);
            byte[] data = comQuery.getPacketData().toByteArray();

            SocketSender socketSender = new SocketSender(this.mysqlConn, new IIntConnectionInterface() {
                @Override
                public void socketDataSent()
                {
                    try {
                        //Check the packet response
                        if (Helpers.getMySQLPacketType(Statement.this.mysqlConn.getMysqlIO().getSocketByteArray()) == Helpers.MYSQL_PACKET_TYPE.MYSQL_ERROR_PACKET) {
                            try {
                                MySQLErrorPacket mySQLErrorPacket = new MySQLErrorPacket(Statement.this.mysqlConn);
                                throw new MySQLException(mySQLErrorPacket.getErrorMsg(), mySQLErrorPacket.getErrorCode(), mySQLErrorPacket.getSqlState());
                            }
                            catch (final InvalidSQLPacketException e) {
                                if (mysqlConn.getReturnCallbackToMainThread()) {
                                    mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run()
                                        {
                                            iConnectionInterface.handleInvalidSQLPacketException(e);
                                        }
                                    });
                                }
                                else {
                                    iConnectionInterface.handleInvalidSQLPacketException(e);
                                }
                            }
                            catch (final MySQLException ex) {
                                if (mysqlConn.getReturnCallbackToMainThread()) {
                                    mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run()
                                        {
                                            iConnectionInterface.handleMySQLException(ex);
                                        }
                                    });
                                }
                                else {
                                    iConnectionInterface.handleMySQLException(ex);
                                }
                            }
                        }
                        else {

                            if (Helpers.getMySQLPacketType(Statement.this.mysqlConn.getMysqlIO().getSocketByteArray()) == Helpers.MYSQL_PACKET_TYPE.MYSQL_OK_PACKET) {
                                try {
                                    MySQLOKPacket mySQLOKPacket = new MySQLOKPacket(Statement.this.mysqlConn);
                                    affectedRows = mySQLOKPacket.getAffectedRows();
                                    Statement.this.mysqlConn.setLastInsertID(mySQLOKPacket.getLastInsertID());
                                    if (mysqlConn.getReturnCallbackToMainThread()) {
                                        mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                iConnectionInterface.actionCompleted();
                                            }
                                        });
                                    }
                                    else {
                                        iConnectionInterface.actionCompleted();
                                    }
                                }
                                catch (final InvalidSQLPacketException e) {
                                    if (mysqlConn.getReturnCallbackToMainThread()) {
                                        mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                iConnectionInterface.handleInvalidSQLPacketException(e);
                                            }
                                        });
                                    }
                                    else {
                                        iConnectionInterface.handleInvalidSQLPacketException(e);
                                    }
                                }
                            }
                        }
                    }
                    catch (final IOException ex) {
                        if (mysqlConn.getReturnCallbackToMainThread()) {
                            mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                    iConnectionInterface.handleIOException(ex);
                                }
                            });
                        }
                        else {
                            iConnectionInterface.handleIOException(ex);
                        }
                    }
                    catch (final MySQLConnException ex)
                    {
                        if (mysqlConn.getReturnCallbackToMainThread()) {
                            mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                    iConnectionInterface.handleMySQLConnException(ex);
                                }
                            });
                        }
                        else
                        {
                            iConnectionInterface.handleMySQLConnException(ex);
                        }
                    }
                }

                @Override
                public void handleException(final MySQLConnException ex)
                {
                    if (mysqlConn.getReturnCallbackToMainThread()) {
                        mysqlConn.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run()
                            {
                                iConnectionInterface.handleMySQLConnException(ex);
                            }
                        });
                    }
                    else {
                        iConnectionInterface.handleMySQLConnException(ex);
                    }
                }
            });

            socketSender.execute(data);
        }
        catch (IOException ex) {
            Log.e("Statement", "IOException: " + ex.toString());
            iConnectionInterface.handleIOException(ex);
        }
        catch (Exception ex)
        {
            Log.e("Statement", "Exception: " + ex.toString());
            iConnectionInterface.handleException(ex);
        }
    }

    public void executeQuery(String query, final IResultInterface iResultInterface)
    {

            try {
                this.mysqlConn.resetPacketSequenceNumber(true);

                COM_Query comQuery = new COM_Query(this.mysqlConn, COM_Query.COM_QUERY, query);
                byte[] data = comQuery.getPacketData().toByteArray();
                SocketSender socketSender = new SocketSender(this.mysqlConn, new IIntConnectionInterface() {
                    @Override
                    public void socketDataSent()
                    {
                        try {
                            if (Helpers.getMySQLPacketType(Statement.this.mysqlConn.getMysqlIO().getSocketByteArray()) == Helpers.MYSQL_PACKET_TYPE.MYSQL_ERROR_PACKET) {
                                try {
                                    MySQLErrorPacket mySQLErrorPacket = new MySQLErrorPacket(Statement.this.mysqlConn);
                                    throw new MySQLException(mySQLErrorPacket.getErrorMsg(), mySQLErrorPacket.getErrorCode(), mySQLErrorPacket.getSqlState());
                                }
                                catch (final MySQLException ex) {
                                    if (mysqlConn.getReturnCallbackToMainThread()) {
                                        mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                iResultInterface.handleMySQLException(ex);
                                            }
                                        });
                                    }
                                    else {
                                        iResultInterface.handleMySQLException(ex);
                                    }
                                }
                                catch (final InvalidSQLPacketException e) {
                                    if (mysqlConn.getReturnCallbackToMainThread()) {
                                        mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                iResultInterface.handleInvalidSQLPacketException(e);
                                            }
                                        });
                                    }
                                    else {
                                        iResultInterface.handleInvalidSQLPacketException(e);
                                    }
                                }
                            }
                            else {
                                final COM_QueryResponse comQueryResponse = new COM_QueryResponse(Statement.this.mysqlConn);
                                if (mysqlConn.getReturnCallbackToMainThread())
                                    mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run()
                                        {
                                            iResultInterface.executionComplete(new ResultSet(comQueryResponse.getColumnDefinitions(), comQueryResponse.getRows()));
                                        }
                                    });
                                else {
                                    iResultInterface.executionComplete(new ResultSet(comQueryResponse.getColumnDefinitions(), comQueryResponse.getRows()));
                                }
                            }
                        }
                        catch (final IOException ex) {
                            if (mysqlConn.getReturnCallbackToMainThread()) {
                                mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        iResultInterface.handleIOException(ex);
                                    }
                                });
                            }
                            else {
                                iResultInterface.handleIOException(ex);
                            }
                        }
                        catch (final MySQLConnException ex)
                        {
                            if (mysqlConn.getReturnCallbackToMainThread())
                            {
                                mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        iResultInterface.handleMySQLConnException(ex);
                                    }
                                });
                            }
                            else
                            {
                                iResultInterface.handleMySQLConnException(ex);
                            }
                        }
                    }

                    @Override
                    public void handleException(final MySQLConnException ex)
                    {
                        if (mysqlConn.getReturnCallbackToMainThread()) {
                            mysqlConn.getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                    iResultInterface.handleMySQLConnException(ex);
                                }
                            });
                        }
                        else {
                            iResultInterface.handleMySQLConnException(ex);
                        }

                    }
                });
                socketSender.execute(data);
            }
            catch (IOException ex) {
                iResultInterface.handleException(ex);
            }
    }
}
