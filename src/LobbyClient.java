import Utilities.*;
import Utilities.SocketData.LobbyToServerData;
import Utilities.SocketData.PointToPointData;
import Utilities.SocketData.ServerToLobbyData;
import com.google.gson.Gson;

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class LobbyClient
{
    String nickname;
    boolean isReady;
    Socket connectionSocket = null;
    ObjectInputStream inStream = null;
    ObjectOutputStream outStream = null;

    LobbyWindow window;
    GameWindow gameWindow;

    ServerToLobbyData inData;

    PointToPointData gameData;

    public static void main(String[] args)
    {
        LobbyClient l = new LobbyClient(args[0], args[1], Integer.parseInt(args[2]));
    }

    boolean tryToConnect(String ipAddress,int port)
    {
        try
        {
            if (connectionSocket != null)
                connectionSocket.close();
            connectionSocket = new Socket(ipAddress, port);
        }
        catch (Exception e)
        {
            System.out.println("Connection failed. Reconnecting... " + e.toString());
        }
        if (connectionSocket != null)
        {
            try
            {
                outStream = new ObjectOutputStream(connectionSocket.getOutputStream());
                inStream = new ObjectInputStream(connectionSocket.getInputStream());
                connectionSocket.setSoTimeout(2000);
                return true;
            } catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    boolean communicateWithServer()
    {
        try
        {
            LobbyToServerData outData = new LobbyToServerData();
            outData.isReady = isReady;
            outData.nickname = nickname;
            outData.gameData = gameData;

            outStream.writeObject(outData);
            outStream.flush();
            outStream.reset();
            inData = (ServerToLobbyData)inStream.readObject();
            return true;
        }
        catch (SocketTimeoutException e)
        {
            window.printToWindow("Disconnected. Server not responding.");
            return false;
        }
        catch (Exception e)
        {
            window.printToWindow("Connection Error. Disconnected.");
            return false;
        }
    }

    void setWindowInfo()
    {
        String[] nicknames = new String[inData.players.size()];
        String[] roles = new String[inData.players.size()];
        int[] ids = new int[inData.players.size()];
        boolean[] ready = new boolean[inData.players.size()];
        for (int i = 0; i < window.getMaxPlayerSlots(); i++)
        {
            if (i < inData.players.size())
            {
                nicknames[i] = inData.players.get(i).nickname;
                ids[i] = inData.players.get(i).ID;
                ready[i] = inData.players.get(i).isReady;
                roles[i] = window.getRoleString(inData.players.get(i).role);
            }
            else break;
        }
        window.setNicknames(ids, nicknames, roles, ready);
        window.printToWindow("Connected! Your ID: " + inData.yourID);
    }

    public LobbyClient(String nick, String ipAddress, int port)
    {
        nickname = nick;
        window = new LobbyWindow();
        window.setReadyBtn.addActionListener(ActionEvent -> isReady = !isReady);
        window.printToWindow("Trying to connect to server...");

        while (!tryToConnect(ipAddress, port) && window.isWindowOpened())
        {
            window.printToWindow("Connection failed. Reconnecting...");
        }
        window.printToWindow("Connected!");

        while (window.isWindowOpened())
        {
            if (communicateWithServer())
            {
                setWindowInfo();
                UpdateGameWindow();
            }
            else
            {
                System.out.println("Connection lost.");
                while (!tryToConnect(ipAddress, port) && window.isWindowOpened())
                {
                    window.printToWindow("Connection failed. Reconnecting...");
                }
                window.printToWindow("Connected!");
            }
        }
    }

    private void UpdateGameWindow()
    {
        if (inData.gameStarted)
        {
            if (gameWindow == null)
            {
                for (PlayerData p : inData.players)
                {
                    if (p.ID == inData.yourID)
                    {
                        if (p.role == ERole.driver)
                            gameWindow = new DriverWindow();
                        else if (p.role == ERole.fueler)
                            gameWindow = new FuelerWindow();
                    }
                }
            }
            else if (inData.gameData != null)
            {
                 gameData = gameWindow.UpdateWindow(inData.gameData);
            }
        }
    }
}
