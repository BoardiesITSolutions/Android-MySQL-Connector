package com.BoardiesITSolutions.AndroidMySQLConnector;

import android.annotation.TargetApi;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;


import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.InvalidSQLPacketException;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.MySQLConnException;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.UnsupportedMySQLServerException;
import com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager.AuthResponse;
import com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager.COM_Query;
import com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager.MySQLErrorPacket;
import com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager.MySQLOKPacket;
import com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager.SSLRequest;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class Connection
{
    private String hostname;
    private String username;
    private String password;
    private String database;
    private int port = 3306;

    //Connection Details
    private int majorVersion;
    private int minorVersion;
    private int subMinorVersion;
    private int protocolVersion;
    private String serverVersion;
    private int connectionID;
    private String authSalt;
    private String authSalt2;
    private String seed;
    protected int baseServerCapabilities;
    protected int serverCapabilities;
    private int clientCapabilities;
    private int serverLanguage;
    private int serverStatus;
    protected int extendedServerCapabilities;
    private int authPluginDataLength = 0;
    private String authPluginName;
    private int packetSequenceNumber = 1;
    private int lastInsertID = 0;
    private Charset charset;

    //Server/Client Connection Options
    public static final int CLIENT_PLUGIN_AUTH = 0x00080000;
    public static final int CLIENT_MULTI_SET = 0x00010000;
    public static final int CLIENT_SSL = 0x00000800;
    public static final int CLIENT_LONG_PASSWORD = 0x00000001;
    public static final int CLIENT_MULTI_STATEMENTS = 0x00010000;
    public static final int CLIENT_SECURE_CONNECTION = 0x00008000;
    public static final int CLIENT_CONNECT_ATTRS = 0x00100000;
    public static final int CLIENT_CONNECT_WITH_DB = 0x00000008;
    public static final int CLIENT_PROTOCOL_41 = 0x00000200;
    public static final int CLIENT_COMPRESS = 0x00000020;
    public static final int CLIENT_NO_SCHEMA = 0x00000010;
    public static final int CLIENT_IGNORE_SIGPIPE = 0x00001000;
    public static final int CLIENT_INTERACTIVE = 0x00000400;
    public static final int CLIENT_ODBC = 0x00000040;
    public static final int CLIENT_IGNORE_SPACE = 0x00000100;
    public static final int CLIENT_PS_MULTI_RESULTS = 0x00040000;
    public static final int CLIENT_CAN_HANDLE_EXPIRED_PASSWORDS = 0x00400000;
    public static final int CLIENT_SESSION_TRACK = 0x00800000;

    public static final int CLIENT_OPTIONAL_RESULTSET_METADATA = 1 << 25;

    //Character Sets
    private final int LATIN1_SWEDISH_CI = 0x08;
    private final int UTF8_GENERAL_CI = 0x21;
    private final int UTF8_UNICODE_CI =0xc0;
    private final int BINARY = 0x3f;

    private AppCompatActivity activity;
    private Socket mysqlSocket;
    private SSLSocket mysqlSSLSocket;
    private MySQLIO mysqlIO = null;
    private IConnectionInterface iConnectionInterface;
    private boolean returnCallbackToMainThread = false;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Connection(String hostname, String username, String password, int port, String database, IConnectionInterface iConnectionInterface)
    {
        //this.resetPacketSequenceNumber();
        this.iConnectionInterface = iConnectionInterface;
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.database = database;
        this.port = port;

        this.connect();
    }

    public Connection(String hostname, String username, String password, int port, IConnectionInterface iConnectionInterface)
    {
        //this.resetPacketSequenceNumber();
        this.iConnectionInterface = iConnectionInterface;
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.port = port;

        this.connect();
    }

    public Charset getCharset() {
        //If the charset hasn't been set yet - probably because we haven't yet read the server
        //language from the welcome packet, default it to uft-8
        if (charset != null)
        {
            return charset;
        }
        else
        {
            return Charset.forName("UTF-8");
        }
    }

    public int getServerLanguage()
    {
        return serverLanguage;
    }

    public void returnCallbackToMainThread(boolean returnCallbackToMainThread, AppCompatActivity activity)
    {
        this.activity = activity;
        this.returnCallbackToMainThread = returnCallbackToMainThread;
    }

    public boolean getReturnCallbackToMainThread()
    {
        return this.returnCallbackToMainThread;
    }

    public AppCompatActivity getActivity()
    {
        return activity;
    }

    public int getMajorVersion()
    {
        return this.majorVersion;
    }
    public int getMinorVersion()
    {
        return this.minorVersion;
    }
    public int getSubMinorVersion()
    {
        return this.subMinorVersion;
    }

    public void setLastInsertID(int lastInsertID)
    {
        this.lastInsertID = lastInsertID;
    }


    /**
     * Get the last insert id from the last command that was executed
     */
    public int getLastInsertID()
    {
        return this.lastInsertID;
    }

    public MySQLIO getMysqlIO()
    {
        return this.mysqlIO;
    }

    public String getServerVersion()
    {
        return this.serverVersion;
    }

    public String getUsername()
    {
        return this.username;
    }
    public String getPassword()
    {
        return this.password;
    }
    public String getDatabase()
    {
        return this.database;
    }
    public int getServerCapabilities()
    {
        return this.serverCapabilities;
    }
    public int getClientCapabilities()
    {
        return this.clientCapabilities;
    }

    public int getAuthPluginDataLength()
    {
        return this.authPluginDataLength;
    }

    public String getAuthSalt()
    {
        return this.authSalt;
    }
    public String getAuthSalt2()
    {
        return this.authSalt2;
    }
    public String getAuthPluginName()
    {
        return this.authPluginName;
    }

    public int getConnectionPacketSequence()
    {
        Log.d("Connection", "Packet Sequence Number Returned: " + this.packetSequenceNumber);
        return this.packetSequenceNumber;
    }

    public Socket getPlainSocket()
    {
        return this.mysqlSocket;
    }
    public SSLSocket getSSLSocket()
    {
        return this.mysqlSSLSocket;
    }

    public void setSocket(Socket socket)
    {
        this.mysqlSocket = socket;
    }
    public void setMySQLIO(MySQLIO mysqlIO)
    {
        this.mysqlIO = mysqlIO;
    }

    public String getHostname()
    {
        return this.hostname;
    }
    public int getPort()
    {
        return this.port;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void connect()
    {
            this.packetSequenceNumber = 1;
            //mysqlSocket = new Socket(this.hostname, this.port);
            SocketSender socketSender = new SocketSender(this, new IIntConnectionInterface()
            {
                @Override
                public void socketDataSent()
                {
                    try
                    {
                        processWelcomePacket();
                    }
                    catch (MySQLConnException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void handleException(MySQLConnException ex)
                {
                    Log.e("Connection", ex.toString());
                }
            });
            socketSender.execute((byte[]) null);
    }

    private void processWelcomePacket() throws MySQLConnException, IOException {
        try {
            mysqlSocket.setSoTimeout(5000);

            //mysqlIO = new MySQLIO(Connection.this, mysqlSocket);
            if (Helpers.getMySQLPacketType(mysqlIO.getSocketByteArray()) == Helpers.MYSQL_PACKET_TYPE.MYSQL_ERROR_PACKET)
            {
                MySQLErrorPacket mySQLErrorPacket = new MySQLErrorPacket(Connection.this);
                throw new MySQLConnException(mySQLErrorPacket.getErrorMsg(), mySQLErrorPacket.getErrorCode(), mySQLErrorPacket.getSqlState());
            }

            mysqlIO.shiftCurrentBytePosition(4);

            protocolVersion = (byte) (mysqlIO.extractData(1));

            serverVersion = mysqlIO.extractData(false);

            //Pass the server version in to the major, minor and subminor versions - these could
            //be used if things need to be done differently based on the server we're connecting to
            parseVersionNumber();

            byte[] temp = (byte[]) mysqlIO.extractData(4);
            temp = mysqlIO.swapByteArray(temp);
            connectionID = mysqlIO.fromByteArray(temp);
            byte[] salt1 = (byte[]) mysqlIO.extractData(8);
            if (this.majorVersion == 8)
            {
                //Log.d("Connection", "Version 8 detected. Shifting 1 byte");
                //mysqlIO.shiftCurrentBytePosition(1);
            }
            authSalt = Connection.toString(salt1, 0, salt1.length);
            //There is a null terminator at the end of the salt, shift by one as we don't need it
            mysqlIO.shiftCurrentBytePosition(1);


            byte[] serverCapabilities = (byte[]) mysqlIO.extractData(2);
            baseServerCapabilities = (serverCapabilities[0] & 0xff) | ((serverCapabilities[1] & 0xff) << 8);
            Connection.this.serverCapabilities = baseServerCapabilities;

            //serverLanguage = String.format("%02X", (byte) mysqlIO.extractData(1));
            serverLanguage = ((byte)mysqlIO.extractData(1)) & 0xff;

            setCharset();
            serverStatus = mysqlIO.fromByteArray((byte[]) mysqlIO.extractData(2));

            byte[] extendedServerCapabilitiesArray = (byte[]) mysqlIO.extractData(2);

            int extendedServerCapabilities = (extendedServerCapabilitiesArray[0] & 0xff) | ((extendedServerCapabilitiesArray[1] & 0xff) << 8);
            Connection.this.extendedServerCapabilities = extendedServerCapabilities;
            Connection.this.serverCapabilities |= extendedServerCapabilities << 16;

            //Set the client capabilities to match the server capabilities, we'll then update to turn off what we can't support
            //or don't wish to use
            Connection.this.clientCapabilities = Connection.this.serverCapabilities;


            if ((Connection.this.serverCapabilities & CLIENT_PLUGIN_AUTH) == CLIENT_PLUGIN_AUTH)
            {
                authPluginDataLength = (byte) mysqlIO.extractData(1);
            }
            else
            {
                //The CLIENT_AUTH_PLUGIN might not be set if MySQL version is below
                //5.5.0 but there is a null byte here instead so shift it along by 1
                mysqlIO.shiftCurrentBytePosition(1);
            }

            //Check if the server is supporting compression, if it does, we need to turn off as we don't
            if ((Connection.this.serverCapabilities & CLIENT_COMPRESS) == CLIENT_COMPRESS)
            {
                Log.d("Connection", "Disabling client compress");
                clientCapabilities &= ~CLIENT_COMPRESS;
            }

            //If MySQL 8 turn off can accept expired password
            if (this.getMajorVersion() >= 8)
            {
                clientCapabilities &= ~CLIENT_OPTIONAL_RESULTSET_METADATA;
            }

            //Check if the server is set to don't allow database.table, if so unset it so we can
            //Having this enabled means SHOW TABLES and SHOW DATABASES can't be executed as it will only
            //use the database that we are  connected to
            if ((Connection.this.serverCapabilities & CLIENT_NO_SCHEMA) == CLIENT_NO_SCHEMA)
            {
                clientCapabilities &= ~CLIENT_NO_SCHEMA;
            }

            if ((Connection.this.serverCapabilities & CLIENT_ODBC) == CLIENT_ODBC)
            {
                clientCapabilities &= ~CLIENT_ODBC;
            }
            if ((Connection.this.serverCapabilities & CLIENT_INTERACTIVE) == CLIENT_INTERACTIVE)
            {
                clientCapabilities &= ~CLIENT_INTERACTIVE;
            }
            if ((Connection.this.serverCapabilities & CLIENT_IGNORE_SIGPIPE) == CLIENT_IGNORE_SIGPIPE)
            {
                clientCapabilities &= ~CLIENT_IGNORE_SIGPIPE;
            }
            if ((Connection.this.serverCapabilities & CLIENT_IGNORE_SPACE) == CLIENT_IGNORE_SPACE)
            {
                clientCapabilities &= ~ CLIENT_IGNORE_SPACE;
            }

            //There is 10 byte filler so shift on by 10
            mysqlIO.shiftCurrentBytePosition(10);
            if ((Connection.this.serverCapabilities & CLIENT_SECURE_CONNECTION) == CLIENT_SECURE_CONNECTION) {
                int length = 0;
                if (authPluginDataLength > 0)
                {
                    length = authPluginDataLength - 8;
                }
                else
                {
                    length = 13;
                }
                if (length > 13) //Shouldn't be any bigger than 13
                {
                    length = 13;
                }

                byte[] salt = (byte[]) mysqlIO.extractData(length);
                authSalt2 = Connection.toString(salt, 0, salt.length);

                StringBuilder stringBuilder = new StringBuilder(length);
                stringBuilder.append(authSalt);
                stringBuilder.append(authSalt2);
                seed = stringBuilder.toString();
            }

            if ((Connection.this.serverCapabilities & CLIENT_PLUGIN_AUTH) == CLIENT_PLUGIN_AUTH) {
                authPluginName = mysqlIO.extractData(false);
            }

            //Check if we're using TLS connection, if so we need to send an SSL Request Packet
            if ((Connection.this.serverCapabilities & CLIENT_SSL) == CLIENT_SSL)
            {
                sendSSLRequest();
            }
            else
            {
                sendAuthResponse();
            }
        }
        catch (IOException ex)
        {
            Log.e("MySQLConnection", ex.toString());
        }
        catch (MySQLConnException ex)
        {
            throw ex;
        }
        catch (InvalidSQLPacketException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedMySQLServerException ex)
        {
            Log.e("MySQLCOnnection", ex.toString());
        }
    }

    private void setCharset() throws UnsupportedMySQLServerException {
        switch (serverLanguage)
        {
            case LATIN1_SWEDISH_CI:
                charset = Charset.forName("LATIN1");
                break;
            case UTF8_GENERAL_CI:
            case UTF8_UNICODE_CI:
                charset = Charset.forName("UTF-8");
                break;
                default:
                    throw new UnsupportedMySQLServerException(this, "Server Language " + String.format("0x%02X", serverLanguage));
        }
    }

    private void sendSSLRequest()
    {
        try
        {
            Log.d("Connection", "Sending SSL Request");
            SSLRequest sslRequest = new SSLRequest(this);
            this.getMysqlIO().sendDataOnSocket(sslRequest.getPacketData().toByteArray(), true, new IIntConnectionInterface()
            {
                @Override
                public void socketDataSent()
                {
                    Log.d("Connection", "Socket Data Sent");
                    performSSLHandshake();
                }

                @Override
                public void handleException(MySQLConnException ex)
                {
                    Log.e("MySQLConnection", ex.toString());
                    ex.printStackTrace();
                }
            });
        }
        catch (Exception ex)
        {
            Log.e("MySQLCOnnection", ex.toString());
            ex.printStackTrace();
        }
    }

    private void performSSLHandshake()
    {
        Log.d("Connection", "Performing SSL Handshake");
        try
        {
            ServerSocketFactory ssf = ServerSocketFactory.getDefault();
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new SSLTrustManager()}, null);
            SSLSocketFactory sslf = sslContext.getSocketFactory();

            SSLSocket sslSocket = (SSLSocket) sslf.createSocket(mysqlSocket, null,
                    mysqlSocket.getPort(), false);
            sslSocket.setEnabledProtocols(new String[]{"TLSv1.1"});
            sslSocket.setTcpNoDelay(true);
            sslSocket.setReuseAddress(true);
            sslSocket.setSoTimeout(5000);

            List<String> allowedCiphers = null;
            boolean disableDHAlgorithm = true;

            if (disableDHAlgorithm) {
                allowedCiphers = new ArrayList<String>();
                for (String cipher : sslSocket.getEnabledCipherSuites()) {
                    if (!(disableDHAlgorithm && (cipher.indexOf("_DHE_") > -1 || cipher.indexOf("_DH_") > -1))) {
                        allowedCiphers.add(cipher);
                    }
                }
            }

            // if some ciphers were filtered into allowedCiphers
            if (allowedCiphers != null) {
                sslSocket.setEnabledCipherSuites(allowedCiphers.toArray(new String[0]));
            }

            sslSocket.startHandshake();
            mysqlSSLSocket = sslSocket;
            //this.mysqlSocket = null;
            mysqlIO.updateSocketStream(mysqlSSLSocket);

            incrementPacketSequenceNumber();
            sendAuthResponse();
        }
        catch (SSLHandshakeException ex)
        {
            Log.e("MySQLConnection", ex.toString());
        }
        catch (IOException ex)
        {
            Log.e("MySQLConnection",ex.toString());
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public void incrementPacketSequenceNumber()
    {
        this.packetSequenceNumber++;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void sendAuthResponse()
    {
        Log.d("MySQLConnection", "Sending Auth Response");
        try
        {
            AuthResponse authResponse = new AuthResponse(this);

            this.getMysqlIO().sendDataOnSocket(authResponse.getPacketData().toByteArray(), new IIntConnectionInterface()
            {
                @Override
                public void socketDataSent()
                {
                    Log.d("Connection", "Socket Data Sent");
                    try {
                        Helpers.MYSQL_PACKET_TYPE mysqlPacketType = Helpers.getMySQLPacketType(Connection.this.mysqlIO.getSocketByteArray());
                        if (mysqlPacketType == Helpers.MYSQL_PACKET_TYPE.MYSQL_ERROR_PACKET) {
                            MySQLErrorPacket mySQLErrorPacket = new MySQLErrorPacket(Connection.this);
                            //We can't do anything else here, so throw a MySQLConnException

                            final MySQLConnException connException = new MySQLConnException(mySQLErrorPacket.getErrorMsg(), mySQLErrorPacket.getErrorCode(), mySQLErrorPacket.getSqlState());
                            if (getReturnCallbackToMainThread())
                            {
                                getActivity().runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        iConnectionInterface.handleMySQLConnException(connException);
                                    }
                                });
                            }
                            else
                            {
                                iConnectionInterface.handleMySQLConnException(connException);
                            }
                        }
                        else if (mysqlPacketType == Helpers.MYSQL_PACKET_TYPE.MYSQL_OK_PACKET) {
                            MySQLOKPacket mySQLOKPacket = new MySQLOKPacket(Connection.this);
                            if (getReturnCallbackToMainThread())
                            {
                                getActivity().runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        iConnectionInterface.actionCompleted();
                                    }
                                });
                            }
                            else
                            {
                                iConnectionInterface.actionCompleted();
                            }
                        }
                        else if (mysqlPacketType == Helpers.MYSQL_PACKET_TYPE.MYSQL_EOF_PACKET)
                        {
                            MySQLOKPacket mySQLOKPacket = new MySQLOKPacket(Connection.this);
                            if (getReturnCallbackToMainThread())
                            {
                                getActivity().runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        iConnectionInterface.actionCompleted();
                                    }
                                });
                            }
                            else
                            {
                                iConnectionInterface.actionCompleted();
                            }
                        }
                    }
                    catch (final IOException ex)
                    {
                        Log.e("MySQLConnection", ex.toString());
                        if (getReturnCallbackToMainThread())
                        {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    iConnectionInterface.handleIOException(ex);
                                }
                            });
                        }
                        else
                        {
                            iConnectionInterface.handleIOException(ex);
                        }
                    }
                    catch (final InvalidSQLPacketException ex)
                    {
                        Log.e("MySQLConnection", ex.toString());
                        if (getReturnCallbackToMainThread())
                        {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    iConnectionInterface.handleInvalidSQLPacketException(ex);
                                }
                            });
                        }
                        else
                        {
                            iConnectionInterface.handleInvalidSQLPacketException(ex);
                        }
                    }
                }

                @Override
                public void handleException(final MySQLConnException ex)
                {
                    if (getReturnCallbackToMainThread())
                    {
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                iConnectionInterface.handleException(ex);
                            }
                        });
                    }
                    else
                    {
                        iConnectionInterface.handleException(ex);
                    }
                }
            });

            //Check the first byte, if the first byte in the response is 0xff then we have a MySQL Error Packet


        }
        catch (final IOException ex)
        {
            Log.e("MySQLConnection", ex.toString());
            if (getReturnCallbackToMainThread())
            {
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        iConnectionInterface.handleIOException(ex);
                    }
                });
            }
            else
            {
                iConnectionInterface.handleIOException(ex);
            }
        }
    }




    public void updateClientCapabilities(int serverCapabilities)
    {
        this.clientCapabilities &= serverCapabilities;
    }

    public final String readString(byte[] byteBuffer) {
        int position = 0;
        int i = 0;
        int len = 0;
        int maxLen = 21;

        while ((i < maxLen) && (byteBuffer[i] != 0)) {
            len++;
            i++;
        }

        String s = Connection.toString(byteBuffer, position, len);

        return s;
    }

    public void resetPacketSequenceNumber()
    {
        this.packetSequenceNumber = 1;
    }
    public void resetPacketSequenceNumber(boolean resetToZero)
    {
        if (resetToZero)
        {
            this.packetSequenceNumber = 0;
        }
        else
        {
            this.packetSequenceNumber = 1;
        }
    }

    public static String toString(byte[] value, int offset, int length) {
        Charset cs = Charset.forName("UTF-8");

        return cs.decode(ByteBuffer.wrap(value, offset, length)).toString();
    }

    public Statement createStatement()
    {
        return new Statement(this);
    }

    private void parseVersionNumber()
    {
        if (this.serverVersion.contains("-"))
        {
            this.serverVersion = this.serverVersion.substring(0, this.serverVersion.indexOf("-") - 1);
        }

        this.serverVersion = this.serverVersion.replaceAll("[^\\d.]", "");
        if ((this.serverVersion != null) && this.serverVersion.length() > 0)
        {
            String[] versions = this.serverVersion.split("\\.");
            //We expect 3 parts
            if (versions.length == 0)
            {
                //Set default values as we failed to parse
                this.majorVersion = -1;
                this.minorVersion = -1;
                this.subMinorVersion = -1;
            }
            if (versions.length < 3)
            {
                //This shouldn't happen, we should always get a version number of x.y.z but if we did get something
                //unexpected, parse what we can
                this.majorVersion = ((versions[0] != null) && versions[0].length() != 0) ? Integer.parseInt(versions[0]) : -1;
                this.minorVersion = ((versions.length > 1) && (versions[1] != null) && versions[1].length() != 0) ? Integer.parseInt(versions[1]) : -1;
            }
            else
            {
                //We got what we expected at least in size
                this.majorVersion = ((versions[0] != null) && versions[0].length() != 0) ? Integer.parseInt(versions[0]) : -1;
                this.minorVersion = ((versions[1] != null) && versions[1].length() != 0) ? Integer.parseInt(versions[1]) : -1;
                this.subMinorVersion = ((versions[2] != null) && versions[2].length() != 0) ? Integer.parseInt(versions[2]) : -1;
            }
        }
        else
        {
            //Set to invalid options - it means we failed to parse the version number from the connection greeting
            this.majorVersion = -1;
            this.minorVersion =  -1;
            this.subMinorVersion =  -1;
        }
    }

    public boolean isConnectedVersionLessThan(int major, int minor, int subMinor)
    {
        if (this.majorVersion < major)
        {
            return true;
        }
        else if (this.majorVersion == major && this.minorVersion < minor)
        {
            return true;
        }
        else if (this.majorVersion == major && this.minorVersion == minor &&  this.subMinorVersion < subMinor)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean doesVersionMeetMinimumRequired(int major, int minor, int subMinor)
    {
        if (this.majorVersion >= major) {
            if (this.majorVersion == major) {
                if (this.minorVersion >= minor) {
                    if (this.minorVersion == minor) {
                        return (this.minorVersion >= subMinor);
                    }

                    // newer than major.minor
                    return true;
                }

                // older than major.minor
                return false;
            }

            // newer than major
            return true;
        }

        return false;
    }

    public void switchDatabase(final String database, final IConnectionInterface iConnectionInterface)
    {
        try
        {
            this.resetPacketSequenceNumber(true);
            COM_Query com_query = new COM_Query(this, COM_Query.COM_INIT_DB, database);
            final byte[] data = com_query.getPacketData().toByteArray();
            SocketSender socketSender = new SocketSender(Connection.this, new IIntConnectionInterface()
            {
                @Override
                public void socketDataSent()
                {
                    Connection.this.database = database;
                    if (getReturnCallbackToMainThread())
                    {
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                iConnectionInterface.actionCompleted();
                            }
                        });
                    }
                    else
                    {
                        iConnectionInterface.actionCompleted();
                    }
                }

                @Override
                public void handleException(final MySQLConnException ex)
                {
                    if (getReturnCallbackToMainThread())
                    {
                        getActivity().runOnUiThread(new Runnable()
                        {
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
            });
            socketSender.execute(data);
        }
        catch (final IOException ex)
        {
            if (getReturnCallbackToMainThread())
            {
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        iConnectionInterface.handleIOException(ex);
                    }
                });
            }
            else
            {
                iConnectionInterface.handleIOException(ex);
            }
        }
    }

    public String escape_string(String str)
    {
        String data = null;
        if (str != null && str.length() > 0) {
            str = str.replace("\\", "\\\\");
            str = str.replace("'", "\\'");
            str = str.replace("\0", "\\0");
            str = str.replace("\n", "\\n");
            str = str.replace("\r", "\\r");
            str = str.replace("\"", "\\\"");
            str = str.replace("\\x1a", "\\Z");
            data = str;
        }
        return data;
    }

    public void close()
    {
        try
        {
            COM_Query com_query = new COM_Query(this, COM_Query.COM_QUIT, null);
            final byte[] data = com_query.getPacketData().toByteArray();
            SocketSender socketSender = new SocketSender(Connection.this, new IIntConnectionInterface() {
                @Override
                public void socketDataSent() {
                    Log.d("MySQLConnection", "Database successfully closed");
                }

                @Override
                public void handleException(MySQLConnException ex) {
                    Log.e("MySQLConnection", ex.toString());
                }
            });
            socketSender.execute(data);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
