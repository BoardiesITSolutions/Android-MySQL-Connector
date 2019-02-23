package com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager;

import android.util.Log;

import com.BoardiesITSolutions.AndroidMySQLConnector.Connection;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.InvalidSQLPacketException;
import com.BoardiesITSolutions.AndroidMySQLConnector.Helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MySQLOKPacket extends BasePacket
{
    private int affectedRows = 0;
    private int lastInsertID = 0;

    public MySQLOKPacket(Connection mysqlConn) throws IOException, InvalidSQLPacketException
    {
        super(mysqlConn);
        processPacket();
    }

    public int getAffectedRows()
    {
        return this.affectedRows;
    }

    public int getLastInsertID()
    {
        return this.lastInsertID;
    }

    private void processPacket() throws IOException, InvalidSQLPacketException
    {
        this.setPacketLength(this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractDataAsString(3)));
        this.setPacketSequenceNumber((byte)this.mysqlConn.getMysqlIO().extractDataAsString(1));


        Helpers.MYSQL_PACKET_TYPE packetType = Helpers.getMySQLPacketType(this.mysqlConn.getMysqlIO().getSocketByteArray());
        //EOF Packets serve the same purpose - so check whether its OK or an EOF packet to process
        if (packetType != Helpers.MYSQL_PACKET_TYPE.MYSQL_OK_PACKET && packetType != Helpers.MYSQL_PACKET_TYPE.MYSQL_EOF_PACKET) {

            throw new InvalidSQLPacketException("Error: Trying to process a MySQL OK Packet but we don't have the expected packet header for a MySQL OK Packet");
        }

        //Shift one is this the packet type header - this is checked above but the check doesn't shift the  byte position
        this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(1);

        //Now process the OK packet payload

        affectedRows = this.mysqlConn.getMysqlIO().getLenEncodedInt();

        lastInsertID = this.mysqlConn.getMysqlIO().getLenEncodedInt();

        int statusFlags = 0;
        int warningsCount = 0;
        if ((this.mysqlConn.getServerCapabilities() & Connection.CLIENT_PROTOCOL_41) == Connection.CLIENT_PROTOCOL_41)
        {
            statusFlags = this.mysqlConn.getMysqlIO().fromByteArray((byte[])this.mysqlConn.getMysqlIO().extractDataAsString(2));

            warningsCount = this.mysqlConn.getMysqlIO().fromByteArray((byte[])this.mysqlConn.getMysqlIO().extractDataAsString(2));
        }

        Log.d("MySQLOKPacket", "Affected Rows: " + affectedRows);
        Log.d("MySQLOKPacket", "Last Insert ID: " + lastInsertID);
        Log.d("MySQLOKPacket", "Status Flags: " + statusFlags);
        Log.d("MySQLOKPacket", "Warnings Count:" + warningsCount);

    }

    /**
     * This is part of the base class but is not needed for a MySQL OK Packet
     * @return
     * @throws IOException
     */
    @Override
    public ByteArrayOutputStream getPacketData() throws IOException
    {
        throw new UnsupportedOperationException();
    }
}
