package Utilities;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ConnectionEstablisher
{
    public static Socket ListenForConnections(int port)
    {
        ServerSocket serSoc = null;
        try
        {

            try
            {
                serSoc = new ServerSocket(port);
                serSoc.setSoTimeout(1000);
                Socket soc = serSoc.accept();
                serSoc.close();
                return soc;
            }
            catch (SocketTimeoutException e)
            {
                assert serSoc != null;
                serSoc.close();
                return null;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
