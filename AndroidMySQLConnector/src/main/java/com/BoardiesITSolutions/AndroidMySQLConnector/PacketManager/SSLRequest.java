package com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager;

import com.BoardiesITSolutions.AndroidMySQLConnector.Connection;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class SSLRequest extends BasePacket
{
    private ByteArrayOutputStream byteArrayOutputStream;
    private DataOutputStream dataOutPacket;
    private Charset charset;

    public SSLRequest(Connection mysqlConn) throws IOException
    {
        super(mysqlConn);
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.dataOutPacket = new DataOutputStream(byteArrayOutputStream);
        this.charset = charset.forName("UTF-8");
        this.createSSLRequestPacket();
    }

    private void createSSLRequestPacket() throws IOException
    {
        //Add the server capabilities flag
        //The lower two bytes of the server capabilities
        byte[] byteArray = new byte[2];
        byteArray[0] = (byte) (this.mysqlConn.getClientCapabilities() & 0xff);
        byteArray[1] = (byte) (this.mysqlConn.getClientCapabilities() >>> 8 & 0xff);
        dataOutPacket.write(byteArray); //Capability Flags

        //Add the extended server capabilities flag
        //The upper two bytes of the server capabilities flag
        byteArray = new byte[2];
        byteArray[0] = (byte) (this.mysqlConn.getClientCapabilities() >>> 16 & 0xff);
        byteArray[1] = (byte) (this.mysqlConn.getClientCapabilities() >>> 24 & 0xff);
        dataOutPacket.write(byteArray);

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(Integer.reverseBytes(16777215));
        dataOutPacket.write(buffer.array()); //Max Packet Size
        buffer.clear();

        //TODO Set the charset based on the server connection properties
        dataOutPacket.writeByte(0x21); //Charset (Hardcoded to UTF-8)
        dataOutPacket.write(new byte[23]);
    }

    @Override
    public ByteArrayOutputStream getPacketData() throws IOException
    {
        return this.createPacketWithPayload(this.byteArrayOutputStream);
    }
}
