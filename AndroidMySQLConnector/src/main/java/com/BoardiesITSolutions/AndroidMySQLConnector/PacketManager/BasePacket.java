package com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager;

import com.BoardiesITSolutions.AndroidMySQLConnector.Connection;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class BasePacket
{
    private ByteArrayOutputStream byteArrayOutputStream;
    private int packetLength = 0;
    private int packetSequenceNumber = 0;

    protected Connection mysqlConn;


    public BasePacket(Connection mysqlConn)
    {
        this.mysqlConn = mysqlConn;
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        //this.dataOutPacket = new DataOutputStream(this.byteArrayOutputStream);
    }

    protected void setPacketLength(int packetLength)
    {
        this.packetLength = packetLength;
    }

    protected int getPacketLength()
    {
        return this.packetLength;
    }

    protected void setPacketSequenceNumber(int sequenceNumber)
    {
        this.packetSequenceNumber = sequenceNumber;
    }

    protected int getPacketSequenceNumber()
    {
        return this.packetSequenceNumber;
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

    public ByteArrayOutputStream createPacketWithPayload(ByteArrayOutputStream payload) throws IOException
    {
        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayStream);

        int packetLength = payload.size();

        byte[] packetLengthArray = new byte[3];
        packetLengthArray[0] = (byte)(packetLength & 0xff);
        packetLengthArray[1] = (byte)((packetLength >>> 8) & 0xff);
        packetLengthArray[2] = (byte)((packetLength >>> 16) & 0xff);
        dataOutputStream.write(packetLengthArray);

        //Add the sequence number
        dataOutputStream.writeByte((byte)this.mysqlConn.getConnectionPacketSequence());

        //Add the payload to the packet
        dataOutputStream.write(payload.toByteArray());
        return byteArrayStream;
    }



    public abstract ByteArrayOutputStream getPacketData() throws IOException;
}
