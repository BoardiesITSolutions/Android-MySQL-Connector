package com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager;

import android.os.Message;
import android.util.Log;

import com.BoardiesITSolutions.AndroidMySQLConnector.Connection;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.BoardiesITSolutions.AndroidMySQLConnector.Connection.CLIENT_CONNECT_ATTRS;
import static com.BoardiesITSolutions.AndroidMySQLConnector.Connection.CLIENT_CONNECT_WITH_DB;
import static com.BoardiesITSolutions.AndroidMySQLConnector.Connection.CLIENT_PLUGIN_AUTH;
import static com.BoardiesITSolutions.AndroidMySQLConnector.Connection.CLIENT_SECURE_CONNECTION;

public class AuthResponse extends BasePacket
{
    private ByteArrayOutputStream byteArrayOutputStream;
    private DataOutputStream dataOutPacket;
    private Charset charset;
    public AuthResponse(Connection mysqlConn) throws IOException
    {
        super(mysqlConn);
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.dataOutPacket = new DataOutputStream(this.byteArrayOutputStream);
        this.charset = mysqlConn.getCharset();
        this.createAuthenticationPacket();
    }

    @Override
    public ByteArrayOutputStream getPacketData() throws IOException
    {
        return this.createPacketWithPayload(this.byteArrayOutputStream);
    }

    private void createAuthenticationPacket() throws IOException
    {
        try {
            //If the database is not set, update the client capabilities flag, so that the MySQL Server knows not to expect
            //a database in the auth request
            if ((this.mysqlConn.getDatabase() == null) || this.mysqlConn.getDatabase().length() == 0) {
                this.mysqlConn.updateClientCapabilities(~CLIENT_CONNECT_WITH_DB);
            }

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


            //Add the max packet size

            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(Integer.reverseBytes(16777215));
            dataOutPacket.write(buffer.array()); //Max Packet Size
            buffer.clear();

            Log.d("AuthResponse", "Going to write server language: " + String.format("0x%02X", mysqlConn.getServerLanguage()));

            dataOutPacket.writeByte(mysqlConn.getServerLanguage());

            //There is a 23 byte filler
            dataOutPacket.write(new byte[23]);

            //Add the username - ensure a null terminator is added on the end
            byte[] user = (this.mysqlConn.getUsername() + "\0").getBytes(charset);
            dataOutPacket.write(user);

            if (!this.mysqlConn.isConnectedVersionLessThan(5,5,0))
            {
                dataOutPacket.writeByte((byte) this.mysqlConn.getAuthPluginDataLength() - 1);
            }
            //Has the password, only using mysql_native_password currently
            if (this.mysqlConn.isConnectedVersionLessThan(5,5,0) || this.mysqlConn.getAuthPluginName().equals("mysql_native_password"))
            {
                if ((this.mysqlConn.getServerCapabilities() & CLIENT_SECURE_CONNECTION) == CLIENT_SECURE_CONNECTION)
                {
                    try
                    {

                        String authSalt1 = this.mysqlConn.getAuthSalt();
                        String authSalt2 = this.mysqlConn.getAuthSalt2();
                        String seed = this.mysqlConn.getAuthSalt() + this.mysqlConn.getAuthSalt2();
                        //String updateSeed = this.seed.substring(0, 20);
                        byte[] bytes = seed.getBytes(charset);
                        String temp = readString(bytes);
                        byte[] password = scramblePassword(this.mysqlConn.getPassword(), temp);

                        //If before MySQL 5.5.0 we need to add the password length - this will always be 20
                        if (this.mysqlConn.isConnectedVersionLessThan(5,5,0)) {
                            dataOutPacket.writeByte(0x14);
                        }
                        dataOutPacket.write(password);

                        //The documentation states the password should be NULL terminated, however, if its null
                        //terminated and we're connecting to a default database we don't authenticate. We don't get
                        //any error either we receive an EOF packet but running show processlist from the server itself
                        //shows unauthenticated user. Therefore, if we're not connecting to a default database
                        //add the null terminator to the password, if we are using a default database don't add the null terminator
                        //but this also only appears to be required on certain MySQL Versions.
                        if ((this.mysqlConn.getDatabase() == null) || this.mysqlConn.getDatabase().length() == 0)
                        {
                            if (!mysqlConn.getServerVersion().startsWith("5.7"))
                            {
                                dataOutPacket.writeByte(0); //Write the null terminator
                            }
                        }

                    }
                    catch (NoSuchAlgorithmException ex)
                    {
                        Log.e("AuthResponse", ex.toString());
                    }
                }
            }
            else
            {
                Log.e("AuthResponse", "Only mysql_native_password is currently supported");
                Log.d("AuthResponse", "Auth Plugin Name: " + this.mysqlConn.getAuthPluginName());
                if (this.mysqlConn.getAuthPluginName().equals("caching_sha2_password"))
                {
                    Log.d("AuthResponse", "Using MySQL Caching Sha2 Password");

                    String password = this.mysqlConn.getPassword();

                    MessageDigest hash1MD = sha256();
                    byte[] hash1 = hash1MD.digest(password.getBytes(StandardCharsets.UTF_8));
                    MessageDigest hash2MD = sha256();
                    hash2MD.update(hash1MD.digest(hash1));
                    hash2MD.update(this.mysqlConn.getAuthSalt().getBytes(StandardCharsets.UTF_8));
                    byte[] hash2 = hash1MD.digest();

                    byte[] hash3 = new byte[hash1.length];
                    //Perform XOR
                    int i = 0;
                    for (byte b : hash1)
                    {
                        hash3[i] = (byte) (b ^ hash2[i++]);
                    }

                    dataOutPacket.write(hash3);

                    //byte[] hash1 = sha256String(password);
                    //byte[] hash2 = sha256Bytes(hash1);

                }
                else
                {
                    throw new UnsupportedOperationException("mysql_native_password is currently only supported. Please use this authentication method");
                }
            }

            //Add the database if we have a database available
            if ((this.mysqlConn.getClientCapabilities() & CLIENT_CONNECT_WITH_DB) == CLIENT_CONNECT_WITH_DB)
            {
                byte[] database = (this.mysqlConn.getDatabase() + "\0").getBytes(charset);
                dataOutPacket.write(database);
            }

            if ((this.mysqlConn.getServerCapabilities() & CLIENT_PLUGIN_AUTH) == CLIENT_PLUGIN_AUTH)
            {
                if (!this.mysqlConn.isConnectedVersionLessThan(5,5,0))
                {
                    String temp = this.mysqlConn.getAuthPluginName() + "\0";
                    dataOutPacket.write(temp.getBytes(charset));
                }
            }

            //Add the client attributes to the response
            if (this.mysqlConn.doesVersionMeetMinimumRequired(5,5,0)
                    && (this.mysqlConn.getServerCapabilities() & CLIENT_CONNECT_ATTRS) == CLIENT_CONNECT_ATTRS)
            {
                int totalAttrLength = 0;

                byte[] clientVersionName = "_client_version".getBytes(charset);
                byte[] clientVersionValue = "1.0.0.0".getBytes(charset);
                totalAttrLength += clientVersionName.length + clientVersionValue.length;

                byte[] vendorName = "_runtime_vendor".getBytes(charset);
                byte[] vendorValue = "Boardies IT Solutions".getBytes(charset);
                totalAttrLength += vendorName.length + vendorValue.length;

                byte[] clientName = "_client_name".getBytes(charset);
                byte[] clientValue = "Android MySQL Connector".getBytes(charset);
                totalAttrLength += clientName.length + clientValue.length;

                //Add 6 (number of attributes * 2 (2 as each key/value contains a length a byte
                dataOutPacket.writeByte(totalAttrLength + 6);

                dataOutPacket.writeByte(clientVersionName.length);
                dataOutPacket.write(clientVersionName);
                dataOutPacket.writeByte(clientVersionValue.length);
                dataOutPacket.write(clientVersionValue);

                dataOutPacket.writeByte(vendorName.length);
                dataOutPacket.write(vendorName);
                dataOutPacket.writeByte(vendorValue.length);
                dataOutPacket.write(vendorValue);

                dataOutPacket.writeByte(clientName.length);
                dataOutPacket.write(clientName);
                dataOutPacket.writeByte(clientValue.length);
                dataOutPacket.write(clientValue);

            }
        }
        catch (IOException ex)
        {
            Log.e("AuthResponse", ex.toString());
            ex.printStackTrace();
            throw ex;
        }
        catch (Exception ex)
        {
            Log.e("AuthResponse", ex.toString());
            ex.printStackTrace();
        }
    }

    private MessageDigest sha256() throws NoSuchAlgorithmException
    {
        return MessageDigest.getInstance("SHA-256");
        //byte[] encodedHash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

    }

    /**
     * Uses for MySQL Native Password
     * @param password
     * @param seed
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    private byte[] scramblePassword(String password, String seed) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-1");

        byte[] passwordHashStage1 = md.digest(password.getBytes("UTF-8"));
        md.reset();

        byte[] passwordHashStage2 = md.digest(passwordHashStage1);
        md.reset();

        byte[] seedAsBytes = seed.getBytes(charset);
        md.update(seedAsBytes);
        md.update(passwordHashStage2);

        byte[] toBeXored = md.digest();
        int numToXor = toBeXored.length;

        for (int i = 0; i < numToXor; i++) {
            toBeXored[i] = (byte) (toBeXored[i] ^ passwordHashStage1[i]);
        }
        return toBeXored;
    }
}
