package com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager;

import android.os.Build;
import androidx.annotation.RequiresApi;

import com.BoardiesITSolutions.AndroidMySQLConnector.Connection;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.InvalidSQLPacketException;
import com.BoardiesITSolutions.AndroidMySQLConnector.Helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MySQLErrorPacket extends BasePacket
{
    private int errorCode = 0;
    private int sqlState = 0;
    private String errorMsg;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public MySQLErrorPacket(Connection mysqlConn) throws IOException, InvalidSQLPacketException
    {
        super(mysqlConn);
        this.processPacket();
    }

    public int getErrorCode()
    {
        return this.errorCode;
    }
    public int getSqlState()
    {
        return this.sqlState;
    }
    public String getErrorMsg()
    {
        return this.errorMsg;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void processPacket() throws IOException, InvalidSQLPacketException
    {
        this.setPacketLength(this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractData(3)));

        this.setPacketSequenceNumber((byte)this.mysqlConn.getMysqlIO().extractData(1));


        if (Helpers.getMySQLPacketType(this.mysqlConn.getMysqlIO().getSocketByteArray()) != Helpers.MYSQL_PACKET_TYPE.MYSQL_ERROR_PACKET) {
            throw new InvalidSQLPacketException("Error: Trying to process a MySQL Error Packet but we don't have the expected packet header for a MySQL Error Packet");
        }

        //Shift one is this the packet type header - this is checked above but the check doesn't shift the  byte position
        this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(1);
        byte[] errorCodeArray = (byte[]) this.mysqlConn.getMysqlIO().extractData(2);
        errorCodeArray = this.mysqlConn.getMysqlIO().swapByteArray(errorCodeArray);
        this.errorCode = (this.mysqlConn.getMysqlIO().fromByteArray(errorCodeArray) << 8);

        byte sqlStateSplitter = (byte) this.mysqlConn.getMysqlIO().extractData(1);
        if ((sqlStateSplitter & 0xff) == 0x23) {
            //We'll have the SQL state here
            byte[] sqlStateArray = (byte[]) this.mysqlConn.getMysqlIO().extractData(5);
            this.sqlState = (this.mysqlConn.getMysqlIO().fromByteArray(sqlStateArray) << 8);
        }
        this.errorMsg = this.mysqlConn.getMysqlIO().extractData(false);
    }

    /**
     * This function is part of the base class but should not be needed for MySQL Error Packets
     * @return
     * @throws IOException
     */
    @Override
    public ByteArrayOutputStream getPacketData()
    {
        throw new UnsupportedOperationException();
    }
}
