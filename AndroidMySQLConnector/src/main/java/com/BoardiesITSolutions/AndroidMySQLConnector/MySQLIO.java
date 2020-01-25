package com.BoardiesITSolutions.AndroidMySQLConnector;

import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLSocket;

import static com.BoardiesITSolutions.AndroidMySQLConnector.Connection.CLIENT_SSL;

public class MySQLIO
{
    private Connection connection;
    private BufferedInputStream sockStream;
    private byte[] fullData;
    private int currentBytesRead = 0;

    public MySQLIO(Connection connection, Socket mysqlSock) throws IOException
    {
        this.connection = connection;
        this.sockStream = new BufferedInputStream(mysqlSock.getInputStream());
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
                Log.d("MySQLIO", "Breaking from loop");
                break;
            }
        }
        Log.d("MySQLIO", "Loop compleeted");
        if (baos.size() == 0)
        {
            Log.d("MySQLIO", "No data in response");
        }
        fullData = baos.toByteArray();
        Log.d("MySQLIO", "Full data written to: now size: " + fullData.length);
        currentBytesRead = 0;

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
        return this.fullData[currentBytesRead];
    }

    public String extractData(boolean returnAsHex, int length)
    {
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
        System.out.println("MySQLIOExtraction: Returning -1");
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
