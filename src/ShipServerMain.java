import Utilities.*;
import Utilities.SocketData.DriverToServerData;
import Utilities.SocketData.FuelerToServerData;
import Utilities.SocketData.LobbyToServerData;
import Utilities.SocketData.ServerToLobbyData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShipServerMain
{
    ServerSocket soc;
    volatile CopyOnWriteArrayList<PlayerServerData> players = new CopyOnWriteArrayList<>();
    int nextID = 1; // ID's need to be unique for each player.

    volatile DriverToServerData driverDataIn = new DriverToServerData();
    volatile FuelerToServerData fuelerDataIn = new FuelerToServerData();

    volatile boolean gameStarted = false;

    Thread connectionListenerThread;
    Thread mainThread;

    Ship ship = new Ship();
    ServerWindow window;

    void updateLobbyInfo()
    {
        Object[] playerList = players.toArray();
        String[] nicknames = new String[players.size()];
        String[] roles =     new String[players.size()];
        int[] ids =          new int[players.size()];
        boolean[] readys =   new boolean[players.size()];
        for (int i = 0; i < players.size(); i++)
        {
            nicknames[i] = ((PlayerServerData)playerList[i]).data.nickname;
            roles[i] = window.getRoleString(((PlayerServerData)playerList[i]).data.role);
            ids[i] = ((PlayerServerData)playerList[i]).data.ID;
            readys[i] = ((PlayerServerData)playerList[i]).data.isReady;
        }
        window.setNicknames(ids, nicknames, roles, readys);
        if (players.size() >= 2)
        {
            boolean fuelerSet = false;
            boolean driverSet = false;
            for (PlayerServerData p : players)
            {
                if (p.data.role == ERole.fueler)
                    fuelerSet = true;
                if (p.data.role == ERole.driver)
                    driverSet = true;
            }
            window.startGameBtn.setEnabled(fuelerSet && driverSet); // If both set, button will be enabled
        }
    }

    void setRoleOfPlayer(ERole role)
    {
        try
        {
            int id = Integer.parseInt(window.idTextBox.getText());
            for (PlayerServerData p : players)
            {
                if (p.data.ID == id)
                    p.data.role = role;
                else if (p.data.role == role && role != ERole.spectator) // If other player has this role already, reset it to spectator.
                    p.data.role = ERole.spectator;
            }

        }
        catch (Exception e)
        {
            window.printToConsoleAndWindow("Wrong ID!");
        }
    }

    void swapRolesOfFuelerAndDriver()
    {
        for (PlayerServerData p : players)
        {
            if (p.data.role == ERole.driver)
                p.data.role = ERole.fueler;
            else if (p.data.role == ERole.fueler)
                p.data.role = ERole.driver;
        }
    }

    void exchangeDataWithClients()
    {
        for (PlayerServerData targetPlayer: players) // Handling player connections.
        {
            new Thread(() ->
            {
                // Sending data
                ServerToLobbyData outData = new ServerToLobbyData();
                for (PlayerServerData player : players)
                {
                    outData.players.add(player.data);
                    outData.yourID = targetPlayer.data.ID;
                    outData.gameStarted = gameStarted;
                }
                try
                {
                    synchronized (targetPlayer)
                    {
                        targetPlayer.outputStream.writeObject(new Gson().toJson(outData));
                        targetPlayer.outputStream.flush();

                        // Receiving data
                        LobbyToServerData inData = new Gson().fromJson((String) targetPlayer.inputStream.readObject(), LobbyToServerData.class);
                        targetPlayer.data.nickname = inData.nickname;
                        targetPlayer.data.isReady = inData.isReady;

                        targetPlayer.outputStream.reset();
                    }
                } catch (SocketTimeoutException e)
                {
                    outData.timeout = true;
                    try
                    {
                        targetPlayer.outputStream.writeObject(new Gson().toJson(outData));
                    } catch (IOException ie)
                    {
                        ie.printStackTrace();
                    }
                    players.remove(targetPlayer);
                    window.printToConsoleAndWindow("Player timed out. " + players.size() + " players left.");
                }
                catch (IllegalStateException | JsonSyntaxException se)
                {
                    System.out.println("Player connection error. Skipping packet.");
                }
                catch (Exception e)
                {
                    e.toString();
                    players.remove(targetPlayer);
                    window.printToConsoleAndWindow("Player encountered an error while connecting. Kicked. " + players.size() + " players left. | " + e.toString());
                }
            }).start();
        }
    }

    public static void main(String[] args)
    {
        ShipServerMain shipServer = new ShipServerMain(Integer.parseInt(args[0]));
    }

    ShipServerMain(int port)
    {
        window = new ServerWindow();
        window.printToWindow("Server started. Waiting for players.");

        window.setFuelerBtn.addActionListener(a -> setRoleOfPlayer(ERole.fueler));
        window.setDriverBtn.addActionListener(a -> setRoleOfPlayer(ERole.driver));
        window.swapRolesBtn.addActionListener(a -> swapRolesOfFuelerAndDriver());
        window.startGameBtn.addActionListener(a -> {gameStarted = true; ship = new Ship();});

        connectionListenerThread = new Thread(() ->
        {
            while (!gameStarted && window.isWindowOpened())
            {
                Socket current = ConnectionEstablisher.ListenForConnections(port);
                if (current != null)
                {
                    try
                    {
                        PlayerServerData newPlayer = new PlayerServerData();

                        newPlayer.data.ID = nextID;
                        nextID++;

                        newPlayer.socket = current;
                        newPlayer.socket.setSoTimeout(2000);
                        newPlayer.outputStream = new ObjectOutputStream(current.getOutputStream());
                        newPlayer.inputStream = new ObjectInputStream(current.getInputStream());
                        newPlayer.data.nickname = "Connecting...";
                        newPlayer.data.role = ERole.spectator;
                        newPlayer.data.isReady = false;
                        players.add(newPlayer);
                        window.printToConsoleAndWindow("Connected " + players.size() + " players.");
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Connection thread shut off.");
        });
        mainThread = new Thread(()->
        {
            float dt = 0.05f;
            while (window.isWindowOpened())
            {
                try
                {
                    Thread.sleep((long)(dt*1000));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                exchangeDataWithClients();
                updateLobbyInfo();

                if (gameStarted)
                {
                    ship.update(dt);
                    window.printToConsoleAndWindow("Game started: " + String.format("%.2f", ship.gameTime) + "s.");
                }
                
            }
            System.out.println("Lobby thread shut off.");
        });


        if (window.isWindowOpened())
        {
            connectionListenerThread.start();
            mainThread.start();
            try
            {
                connectionListenerThread.join();
                mainThread.join();
                System.out.println("Server stopped.");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            window.printToConsoleAndWindow("Shutting down the server.");
        }
    }
}
