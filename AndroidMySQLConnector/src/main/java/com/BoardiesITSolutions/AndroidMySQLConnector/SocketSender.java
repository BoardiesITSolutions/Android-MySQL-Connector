package com.BoardiesITSolutions.AndroidMySQLConnector;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Semaphore;

import static com.BoardiesITSolutions.AndroidMySQLConnector.Connection.CLIENT_SSL;

public class SocketSender extends AsyncTask<byte[], Void, Void>
{
    IIntConnectionInterface iIntConnectionInterface;
    Connection mysqlConn;
    private static final String TAG = "SocketSender";
    private Semaphore mutex = new Semaphore(1);

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
            mutex.acquire();
            byte[] byteArray = bytes[0];
            if (byteArray == null)
            {
                //If we have no bytes to send - we're just establishing the initial socket connection
                Socket socket = new Socket(this.mysqlConn.getHostname(), this.mysqlConn.getPort());
                //We've create the socket return the callback so we can process the socket data
                this.mysqlConn.setSocket(socket);
                this.mysqlConn.setMySQLIO(new MySQLIO(this.mysqlConn, socket));
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
            mutex.release();
        }
        catch (IOException | InterruptedException ex)
        {
            mutex.release();
            if (this.mysqlConn.getiConnectionInterface() != null && ex instanceof IOException)
            {
                this.mysqlConn.getiConnectionInterface().handleIOException((IOException)ex);
            }
            Log.e(TAG, "IOException: " +  ex.toString());
        }
        return null;
    }


}
