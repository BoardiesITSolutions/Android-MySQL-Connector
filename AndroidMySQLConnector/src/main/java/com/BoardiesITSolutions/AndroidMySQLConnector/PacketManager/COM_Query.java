package com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager;

import com.BoardiesITSolutions.AndroidMySQLConnector.Connection;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class COM_Query extends BasePacket
{
    //Query Command Values
    public static final int COM_SLEEP = 0x00;
    public static final int COM_QUIT = 0x01;
    public static final int COM_INIT_DB = 0x02;
    public static final int COM_QUERY = 0x03;
    public static final int COM_FIELD_LIST = 0x04; //Deprecated as of MySQL 5.7.11
    public static final int COM_CREATE_DB = 0x05;
    public static final int COM_DROP_DB = 0x06;
    public static final int COM_REFRESH = 0x07; //Deprecated as of MySQL 5.7.11
    public static final int COM_SHUTDOWN = 0x08; //Deprecated as of MySQL 5.7.9
    public static final int COM_STATISTICS = 0x09;
    public static final int COM_PROCESS_INFO = 0x0a; //Deprecated as of MySQL 5.7.11
    public static final int COM_CONNECT = 0x0b;
    public static final int COM_PROCESS_KILL = 0x0c; //Deprecated as of MySQL 5.7.11
    public static final int COM_DEBUG = 0x0f;
    public static final int COM_PING = 0x0e;
    public static final int COM_TIME = 0x0f;
    public static final int COM_DELAYED_INSERT = 0x10;
    public static final int COM_CHANGE_USER = 0x11;
    public static final int COM_RESET_CONNECTION = 0x1f;
    public static final int COM_DAEMON = 0x1d;

    private ByteArrayOutputStream byteArrayOutputStream;
    private DataOutputStream dataOutPacket;

    public COM_Query(Connection mysqlConn, int commandCode, String command) throws IOException, UnsupportedOperationException
    {
        super(mysqlConn);
        if (isComQuerySupported(commandCode))
        {
            this.byteArrayOutputStream = new ByteArrayOutputStream();
            this.dataOutPacket = new DataOutputStream(byteArrayOutputStream);
            this.createPacket(commandCode, command);
        }
        else
        {
            throw new UnsupportedOperationException("Command code: " + commandCode + " is not currently supported by the client");
        }
    }

    private  void createPacket(int commandCode, String command) throws IOException
    {
        this.dataOutPacket.writeByte((byte)commandCode);
        if (command != null && commandCode != COM_QUIT)
        {
            this.dataOutPacket.write((command).getBytes("UTF-8"));
        }
    }

    private boolean isComQuerySupported(int commandCode)
    {
        switch (commandCode)
        {
            case COM_QUERY:
            case COM_INIT_DB:
            case COM_QUIT:
                return true;
            default:
                return false;
        }
    }

    @Override
    public ByteArrayOutputStream getPacketData() throws IOException
    {
        return this.createPacketWithPayload(this.byteArrayOutputStream);
    }
}
