package com.BoardiesITSolutions.AndroidMySQLConnector;

import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.Log;

import com.google.common.primitives.Bytes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.net.ssl.SSLSocket;

import static com.BoardiesITSolutions.AndroidMySQLConnector.Connection.CLIENT_SSL;

public class MySQLIO
{
    private Connection connection;
    private BufferedInputStream sockStream;
    private byte[] fullData;
    private int currentBytesRead = 0;
    private static final Semaphore mutex = new Semaphore(1);
    private static boolean isDBConnected = false;

    public MySQLIO(Connection connection, Socket mysqlSock) throws IOException
    {
        this.connection = connection;
        this.sockStream = new BufferedInputStream(mysqlSock.getInputStream());
    }

    public void setIsDBConnected(boolean isDBConnected)
    {
        MySQLIO.isDBConnected = isDBConnected;
    }

    public void closeMySQLIO()
    {
        if (this.sockStream != null)
        {
            try
            {
                this.sockStream.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public Connection getConnection()
    {
        return this.connection;
    }


    public void updateSocketStream(SSLSocket sslSocket) throws IOException
    {
        this.sockStream = new BufferedInputStream(sslSocket.getInputStream(), 16384);
    }

    public int getCurrentBytesRead()
    {
        return this.currentBytesRead;
    }

    public int getSocketDataLength()
    {
        return this.fullData.length;
    }

    public byte[] getSocketByteArray()
    {
        return this.fullData;
    }

    private void getSocketData() throws IOException
    {
        try {
            mutex.acquire();
            Log.d("MySQLIO", "Reading Socket Data");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Log.d("MySQLIO", "Created baos");
            byte[] data = new byte[1024];
            int bytesRead = -1;
            while ((bytesRead = sockStream.read(data)) != -1) {
                Log.d("MySQLIO", "Read from socket. Bytes Read: " + bytesRead);
                baos.write(data, 0, bytesRead);
                Log.d("MySQLIO", "BAOS written: Bytes Written: " + bytesRead);
                if (bytesRead < 1024) {
                    //Check the beginning of the packet, and see if the payload length matches
                    //the current byte array length, if not, continue reading data
                    byte[] tempFullData = baos.toByteArray();
                    byte[] value = Arrays.copyOfRange(tempFullData, 0, 3);
                    int expectedPayloadLength = fromByteArray(value);
                    //Remove -4 and the first 4 bytes off length as the payload length doesn't include
                    //the header information which is the sequence id and the payload length
                    if (tempFullData.length - 4 < expectedPayloadLength) {
                        if (fullData == null) {
                            fullData = baos.toByteArray();
                        }
                        else {

                            fullData = Bytes.concat(fullData, tempFullData);
                            //byte[] newFullData = new byte[fullData.length + tempFullData.length];
                            //System.arraycopy(tempFullData, 0, newFullData, fullData.length, newFullData.length);
                            //fullData = newFullData;
                        }
                        baos.reset();
                        Log.d("MySQLIO", "Less than 1024 bytes receives. Expected Payload Length: " + expectedPayloadLength + " Current Byte Array Length: " + tempFullData.length);

                    }
                    else {

                        //Check if the 9th byte from the end is an OK or EOF packet, if not,
                        //continue fetching data
                        if (!isDBConnected)
                        {
                            break;
                        }
                        else {
                            int packetType = tempFullData[tempFullData.length - 1];
                            Log.d("MySQIO", "Less than 1024 Bytes Returned. Packet Type at end of array (end of array would have to be an OK: " + packetType);
                            if (packetType == 0x00 || packetType == 0xfe)
                            {
                                break;
                            }
                            packetType = tempFullData[tempFullData.length-1 - 8];
                            Log.d("MySQIO", "Less than 1024 bytes returned. Packet type -8 from the end of the array would need to be an EOF packet");
                            if (packetType == 0xfe)
                            {
                                break;
                            }
                            //The 4th byte can also contain the OK or EOF so check this as well
                            if (tempFullData[4] == 0x00 || tempFullData[4] == 0xfe)
                            {
                                break;
                            }
                                Log.d("MySQIO", "Packet Type at -8 was: " + packetType + " so continuing fetching data");
                                //Print out each byte from the array so can determine how the data looks
                                /*for (int i = 0; i < tempFullData.length; i++) {
                                    Log.d("ByteData", "Index: " + i + " Value: " + tempFullData[i]);
                                }*/

                            }
                        //If we get here continue fetching data

                    }
                }
            }
            Log.d("MySQLIO", "Loop completed");
            if (baos.size() == 0) {
                Log.d("MySQLIO", "No data in response");
            }
            //byte[] tempFullData = baos.toByteArray();

            if (fullData == null)
            {
                fullData = baos.toByteArray();
            }
            else {
                byte[] tempFullData = baos.toByteArray();
                fullData = Bytes.concat(fullData, tempFullData);
            }

            Log.d("MySQLIO", "Get Socket Data Finished. Full Data Length: " + fullData.length);

            /*if (fullData == null) {
                fullData = baos.toByteArray();
            }
            else {
                //byte[] newFullData = new byte[fullData.length + tempFullData.length];
                //System.arraycopy(tempFullData, 0, newFullData, fullData.length, newFullData.length);
                //fullData = newFullData;
                fullData = Bytes.concat(fullData, tempFullData);
                //System.arraycopy(tempFullData, fullData.length, fullData, fullData.length, tempFullData.length);
            }*/
            //fullData = baos.toByteArray();
            Log.d("MySQLIO", "Full data written to: now size: " + fullData.length);
            currentBytesRead = 0;
            mutex.release();
        }
        catch (Exception ex)
        {
            Log.e("MySQIO", ex.toString());
            mutex.release();
        }
    }

    /**
     * Reset the MySQL IO and retrieve the next datastream from the socket
     */
    public void reset(boolean dontExpectResponse) throws IOException
    {
        Log.d("MySQLIO", "Resetting Socket: Don't Expect Response: " + ((dontExpectResponse) ? "true" : "false"));
        this.fullData = null;
        this.currentBytesRead = 0;
        if (!dontExpectResponse)
        {
            this.getSocketData();
        }
    }

    public void reset() throws IOException
    {
        reset(false);
    }


    public byte readCurrentByteWithoutShift()
    {
        if (currentBytesRead >= this.fullData.length)
        {
            return 0;
        }
        return this.fullData[currentBytesRead];
    }

    public String extractData(boolean returnAsHex, int length)
    {
        if (currentBytesRead+length > fullData.length)
        {
            Log.d("MySQLIO", "About to pop extracting data");
            return "";
        }
        byte[] temp = Arrays.copyOfRange(fullData, currentBytesRead, currentBytesRead+length);
        if (returnAsHex)
        {

            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < temp.length; i++)
            {
                stringBuilder.append(String.format("%02X", temp[i]));
            }
            this.shiftCurrentBytePosition(length);
            return stringBuilder.toString();
        }
        else
        {
            this.shiftCurrentBytePosition(length);
            return new String(temp, connection.getCharset());
        }
    }

    /**
     * If returned as hex is false, then it returns the ASCII equivalent
     * @param returnAsHex
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String extractData(boolean returnAsHex)
    {
        if (returnAsHex)
        {
            StringBuilder stringBuilder = new StringBuilder();
            for (; currentBytesRead < fullData.length-1; currentBytesRead++) {
                if (this.fullData[currentBytesRead] == 0) {
                    currentBytesRead++;
                    break;
                }
                stringBuilder.append(String.format("%02X", fullData[currentBytesRead]));
            }
            return stringBuilder.toString();
        }
        else
        {
            List<Byte> bytes = new ArrayList<>();
            //Loop over the full data to find the null terminate creating a list of bytes
            for (; currentBytesRead < fullData.length; currentBytesRead++)
            {
                if (this.fullData[currentBytesRead] == 0)
                {
                    currentBytesRead++;
                    break;
                }
                bytes.add(fullData[currentBytesRead]);
            }
            //Convert the byte list to a byte array so we can convert it to a string
            byte[] byteArray = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++)
            {

                byteArray[i] = bytes.get(i);
            }
            return new String(byteArray, connection.getCharset());
        }
    }

    public Object extractData(int length) throws IndexOutOfBoundsException
    {
        if (length == 1)
        {
            byte value = this.fullData[currentBytesRead];
            this.shiftCurrentBytePosition(length);
            return value;
        }
        else
        {
            byte[] value = Arrays.copyOfRange(fullData, currentBytesRead, currentBytesRead+length);
            this.shiftCurrentBytePosition(length);
            return value;
        }
    }

    public void shiftCurrentBytePosition(int length)
    {
        if ((currentBytesRead + length) > fullData.length)
        {
            throw new IndexOutOfBoundsException();
        }
        currentBytesRead += length;
    }

    private String unHex(String arg) {

        String str = "";
        for(int i=0;i<arg.length();i+=2)
        {
            String s = arg.substring(i, (i + 2));
            int decimal = Integer.parseInt(s, 16);
            str = str + (char) decimal;
        }
        return str.trim();
    }

    public int fromByteArray(byte[] bytes) {
        if (bytes[0] < 0xfb)
        {
            return bytes[0] & 0xff;
        }
        else if (bytes[0] < 0xfc)
        {
            byte[] temp = Arrays.copyOfRange(bytes, 0, 2);
            return fromByteArray(temp);
        }
        else if (bytes[0] < 0xfd)
        {
            byte[] temp = Arrays.copyOfRange(bytes, 0, 3);
            return fromByteArray(temp);
        }
        return -1;
    }

    public int getLenEncodedInt()
    {
        if (currentBytesRead >= this.fullData.length)
        {
            return 0;
        }
        if ((this.fullData[this.currentBytesRead] & 0xff) < 0xfb)
        {
            int value = this.fullData[this.currentBytesRead];
            this.shiftCurrentBytePosition(1);
            return value & 0xff; //Convert to unsigned int
        }
        else if ((this.fullData[this.currentBytesRead] & 0xff) == 0xfb)
        {
            this.shiftCurrentBytePosition(1);
            //If we have 0xfb then we have a NULL value so return -1
            return -1;
        }
        else if ((this.fullData[this.currentBytesRead] & 0xff) == 0xfc)
        {

            byte[] temp = Arrays.copyOfRange(this.fullData, this.currentBytesRead+1, this.currentBytesRead+3);
            this.shiftCurrentBytePosition(3);
            return readInt(temp);
        }
        else if ((this.fullData[this.currentBytesRead] & 0xff) == 0xfd)
        {

            byte[] temp = Arrays.copyOfRange(this.fullData, this.currentBytesRead+1, this.currentBytesRead+4);
            this.shiftCurrentBytePosition(4);
            return readLongInt(temp);
        }
        else if ((this.fullData[this.currentBytesRead] & 0xff) == 0xfe)
        {

            byte[] temp = Arrays.copyOfRange(this.fullData, this.currentBytesRead+1, this.currentBytesRead+9);
            this.shiftCurrentBytePosition(9);
            return (int)readLong(temp);
        }
        return -1;
    }

    private int readInt(byte[] b)
    {

        return (b[0] & 0xff) | ((b[1] & 0xff) << 8);
    }

    private int readLongInt(byte[] b)
    {
        return (b[0] & 0xff) | ((b[1] & 0xff) << 8) | ((b[2] & 0xff) << 16);
    }

    private long readLong(byte[] b)
    {

        return ((long) b[0] & 0xff) | (((long) b[1] & 0xff) << 8) | ((long) (b[2] & 0xff) << 16)
                | ((long) (b[3] & 0xff) << 24);
    }


    public byte[] swapByteArray(byte[] bytes)
    {
        byte[] newByteArray = new byte[bytes.length];
        int counter = 0;
        for (int i = bytes.length-1; i != 0; i--)
        {
            newByteArray[counter] = bytes[i];
            counter++;
        }
        return newByteArray;
    }

    public void sendDataOnSocket(byte[] data, boolean dontExpectResponse, IIntConnectionInterface iIntConnectionInterface) throws IOException
    {
        BufferedOutputStream sockOut = null;
        if ((this.connection.getSSLSocket() != null) && (this.connection.getServerCapabilities() & CLIENT_SSL) == CLIENT_SSL)
        {
            sockOut = new BufferedOutputStream(this.connection.getSSLSocket().getOutputStream());
        }
        else
        {
            sockOut = new BufferedOutputStream(this.connection.getPlainSocket().getOutputStream());
        }

        sockOut.write(data);
        sockOut.flush();

        //Check the response
        this.reset(dontExpectResponse);
        iIntConnectionInterface.socketDataSent();
    }

    public void sendDataOnSocket(byte[] data, IIntConnectionInterface iIntConnectionInterface) throws IOException
    {
        sendDataOnSocket(data, false, iIntConnectionInterface);
    }
}
