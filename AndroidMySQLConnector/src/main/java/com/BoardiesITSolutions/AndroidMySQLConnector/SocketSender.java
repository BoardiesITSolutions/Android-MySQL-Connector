package com.BoardiesITSolutions.AndroidMySQLConnector;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.Semaphore;

import static com.BoardiesITSolutions.AndroidMySQLConnector.Connection.CLIENT_SSL;

public class SocketSender extends AsyncTask<byte[], Void, Void>
{
    IIntConnectionInterface iIntConnectionInterface;
    Connection mysqlConn;
    private static final String TAG = "SocketSender";

    public SocketSender(Connection mysqlConn, IIntConnectionInterface iIntConnectionInterface)
    {
        this.iIntConnectionInterface = iIntConnectionInterface;
        this.mysqlConn = mysqlConn;
    }

    @Override
    protected Void doInBackground(byte[]... bytes)
    {
        try
        {
            byte[] byteArray = bytes[0];
            if (byteArray == null)
            {
                //If we have no bytes to send - we're just establishing the initial socket connection
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(this.mysqlConn.getHostname(), this.mysqlConn.getPort());

                this.mysqlConn.setSocket(socket);

                if ((this.mysqlConn.getSSLSocket() != null) && (this.mysqlConn.getServerCapabilities() & CLIENT_SSL) == CLIENT_SSL)
                {
                    Log.d("SocketSender", "Set MySQLIO for SSL socket");
                    this.mysqlConn.getSSLSocket().setSoTimeout(this.mysqlConn.getConnectionTimeout() * 1000);
                    this.mysqlConn.getSSLSocket().connect(socketAddress, this.mysqlConn.getConnectionTimeout() * 1000);
                    this.mysqlConn.setMySQLIO(new MySQLIO(this.mysqlConn, this.mysqlConn.getSSLSocket()));

                }
                else {

                    this.mysqlConn.getPlainSocket().setSoTimeout(this.mysqlConn.getConnectionTimeout() * 1000);
                    this.mysqlConn.getPlainSocket().connect(socketAddress, this.mysqlConn.getConnectionTimeout() * 1000);

                    this.mysqlConn.setMySQLIO(new MySQLIO(this.mysqlConn, this.mysqlConn.getPlainSocket()));


                }


            }
            else
            {
                BufferedOutputStream sockOut;
                if ((this.mysqlConn.getSSLSocket() != null) && (this.mysqlConn.getServerCapabilities() & CLIENT_SSL) == CLIENT_SSL)
                {
                    sockOut = new BufferedOutputStream(this.mysqlConn.getSSLSocket().getOutputStream());
                }
                else
                {
                    sockOut = new BufferedOutputStream(this.mysqlConn.getPlainSocket().getOutputStream());
                }

                sockOut.write(byteArray);
                sockOut.flush();
            }
            this.mysqlConn.getMysqlIO().reset();
            iIntConnectionInterface.socketDataSent();

        }
        catch (SocketTimeoutException ex)
        {
            Log.e("SocketSender", "Socket Timeout Exception: " + ex.toString());
            if (this.mysqlConn.getiConnectionInterface() != null)
            {
                this.mysqlConn.getiConnectionInterface().handleException(ex);
            }
        }
        catch (IOException ex)
        {
            if (this.mysqlConn.getiConnectionInterface() != null && ex instanceof IOException)
            {
                this.mysqlConn.getiConnectionInterface().handleIOException((IOException)ex);
            }
            Log.e(TAG, "IOException: " +  ex.toString());
        }
        return null;
    }


}
