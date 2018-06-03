package com.BoardiesITSolutions.AndroidMySQLConnector;

import java.io.IOException;

public class Helpers
{
    public enum MYSQL_PACKET_TYPE {MYSQL_ERROR_PACKET, MYSQL_OK_PACKET, MYSQL_EOF_PACKET, NOT_MYSQL_PACKET, MYSQL_UNKNOWN_PACKET}

    /**
     * Checks the packet to see if it is a MySQL Error Packet or an OK packet
     * @param socketData The full socket data byte array that should be checked
     * @return MYSQL_PACKET_TYPE
     */
    public static MYSQL_PACKET_TYPE getMySQLPacketType(byte[] socketData) throws IOException
    {
        //The packet type to check is the 4th byte, the first 3 bytes is the packet length,
        //the 4th byte is the MySQL sequence number and the 5th byte is the packet type
        if (socketData.length < 4)
        {
            IOException exception = new IOException("Unexpected SQL Packet Size. Got Size: " + socketData.length);
            exception.printStackTrace();
            throw exception;
        }

        int packetType = socketData[4] & 0xff;
        if ((packetType & 0xff) == 0xff)
        {
            return MYSQL_PACKET_TYPE.MYSQL_ERROR_PACKET;
        }
        else if ((packetType & 0xfe) == 0xfe)
        {
            return MYSQL_PACKET_TYPE.MYSQL_EOF_PACKET;
        }
        else if (packetType == 0)
        {
            return MYSQL_PACKET_TYPE.MYSQL_OK_PACKET;
        }
        else
        {
            return MYSQL_PACKET_TYPE.NOT_MYSQL_PACKET;
        }
    }

    public static MYSQL_PACKET_TYPE getMySQLPacketTypeFromIntWithoutShift(int packetType)
    {
        packetType = packetType & 0xff;
        if ((packetType & 0xff) == 0xff)
        {
            return MYSQL_PACKET_TYPE.MYSQL_ERROR_PACKET;
        }
        else if ((packetType & 0xfe) == 0xfe)
        {
            return MYSQL_PACKET_TYPE.MYSQL_EOF_PACKET;
        }
        else
        {
            return MYSQL_PACKET_TYPE.MYSQL_OK_PACKET;
        }
    }
}
