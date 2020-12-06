import Utilities.*;
import Utilities.SocketData.*;
import com.google.gson.JsonSyntaxException;
import com.sun.jmx.remote.internal.ArrayQueue;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;


public class ShipServerMain
{
    ServerSocket soc;
    final ArrayList<PlayerServerData> players = new ArrayList<>();
    int nextID = 1; // ID's need to be unique for each player.

    volatile boolean gameStarted = false;

    Thread connectionListenerThread;
    Thread mainThread;

    LinkedBlockingQueue<FuelerToServerData> fuelerData = new LinkedBlockingQueue<>(); // Fueler has "toggle-mechanics" so we keep his last actions as array
    DriverToServerData driverData;
    Ship ship;
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
            nicknames[i] = ((PlayerServerData)playerList[i]).lobbyData.nickname;
            roles[i] = window.getRoleString(((PlayerServerData)playerList[i]).lobbyData.role);
            ids[i] = ((PlayerServerData)playerList[i]).lobbyData.ID;
            readys[i] = ((PlayerServerData)playerList[i]).lobbyData.isReady;
        }
        window.setNicknames(ids, nicknames, roles, readys);
        if (players.size() >= 2)
        {
            boolean fuelerSet = false;
            boolean driverSet = false;
            for (PlayerServerData p : players)
            {
                if (p.lobbyData.role == ERole.fueler)
                    fuelerSet = true;
                if (p.lobbyData.role == ERole.driver)
                    driverSet = true;
            }
            window.startGameBtn.setEnabled(fuelerSet && driverSet); // If both set, button will be enabled
        }
    }

    void setRoleOfPlayer(ERole role)
    {
        synchronized (players)
        {
            try
            {
                int id = Integer.parseInt(window.idTextBox.getText());
                for (PlayerServerData p : players)
                {
                    if (p.lobbyData.ID == id)
                        p.lobbyData.role = role;
                    else if (p.lobbyData.role == role && role != ERole.spectator) // If other player has this role already, reset it to spectator.
                        p.lobbyData.role = ERole.spectator;
                }

            }
            catch (Exception e)
            {
                window.printToConsoleAndWindow("Wrong ID!");
            }
        }
    }

    void swapRolesOfFuelerAndDriver()
    {
        synchronized (players)
        {
            for (PlayerServerData p : players)
            {
                if (p.lobbyData.role == ERole.driver)
                    p.lobbyData.role = ERole.fueler;
                else if (p.lobbyData.role == ERole.fueler)
                    p.lobbyData.role = ERole.driver;
            }
        }
    }

    void exchangeDataWith(PlayerServerData targetPlayer)
    {
        while (!targetPlayer.toDelete && window.isWindowOpened())
        {
            ServerToLobbyData outData = new ServerToLobbyData();
            try
            {
                for (PlayerServerData p : players)
                {
                    outData.players.add(p.lobbyData);
                }
                outData.yourID = targetPlayer.lobbyData.ID;
                outData.gameStarted = gameStarted;

                if (ship != null && gameStarted)
                {
                    if (targetPlayer.lobbyData.role == ERole.driver)
                        outData.gameData = ship.getDriverData();
                    if (targetPlayer.lobbyData.role == ERole.fueler)
                        outData.gameData = ship.getFuelerData();
                    if (targetPlayer.lobbyData.role == ERole.spectator)
                    {
                        ServerToSpectatorData spectatorData = new ServerToSpectatorData();
                        spectatorData.driverData = ship.getDriverData();
                        spectatorData.fuelerData = ship.getFuelerData();
                        outData.gameData = spectatorData;
                    }
                }
                synchronized (players)
                {
                    targetPlayer.outputStream.writeObject(outData);
                    targetPlayer.outputStream.flush();
                }
                // Receiving data
                LobbyToServerData inData = (LobbyToServerData) targetPlayer.inputStream.readObject();
                targetPlayer.lobbyData.nickname = inData.nickname;
                targetPlayer.lobbyData.isReady = inData.isReady;

                if (inData.gameData != null && targetPlayer.lobbyData.role == ERole.driver)
                    driverData = (DriverToServerData) inData.gameData;
                if (inData.gameData != null && targetPlayer.lobbyData.role == ERole.fueler)
                {
                    FuelerToServerData checked = (FuelerToServerData) inData.gameData;
                    if (checked.action != FuelerAction.noAction)
                        fuelerData.add(checked);
                }

                targetPlayer.outputStream.reset();

            } catch (SocketTimeoutException e)
            {
                System.out.println("Timed out.");
                targetPlayer.toDelete = true;
            } catch (IllegalStateException | JsonSyntaxException se)
            {
                System.out.println("Player connection error. Skipping packet.");
            } catch (ConcurrentModificationException e)
            {
                System.out.println("Concurrent modification.");
            } catch (Exception e)
            {
                e.printStackTrace();
                targetPlayer.toDelete = true;
            }
        }
    }

    public void deleteDisconnectedPlayers()
    {
        synchronized (players)
        {
            if (players.removeIf(PlayerServerData -> PlayerServerData.toDelete))
                window.printToConsoleAndWindow("Player disconnected. " + players.size() + " players left.");
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
            while (window.isWindowOpened())
            {
                Socket current = ConnectionEstablisher.ListenForConnections(port);
                if (current != null)
                {
                    try
                    {
                        PlayerServerData newPlayer = new PlayerServerData();

                        newPlayer.lobbyData.ID = nextID;
                        nextID++;
                        newPlayer.socket = current;
                        newPlayer.socket.setSoTimeout(2000);
                        newPlayer.outputStream = new ObjectOutputStream(current.getOutputStream());
                        newPlayer.inputStream = new ObjectInputStream(current.getInputStream());
                        newPlayer.lobbyData.nickname = "Connecting...";
                        newPlayer.lobbyData.role = ERole.spectator;
                        newPlayer.lobbyData.isReady = false;
                        newPlayer.connectionThread = new Thread(() -> exchangeDataWith(newPlayer));
                        newPlayer.connectionThread.start();
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
            float dt = 0.01f;
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
                updateLobbyInfo();
                deleteDisconnectedPlayers();

                if (gameStarted)
                {
                    if (ship == null)
                        ship = new Ship();
                    else
                    {
                        if (!isDriverConnected())
                            ship.notifyAboutDisconnectedDriver();
                        if (!isFuelerConnected())
                            ship.notifyAboutDisconnectedFueler();

                        ship.update(dt, driverData, fuelerData.poll());
                        window.printToWindow("Game started: " + String.format("%.2f", ship.gameTime) + "s.");

                        if (ship.isGameOver())
                        {
                            gameStarted = false;
                            ship = null;
                        }
                    }
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

    private boolean isFuelerConnected()
    {
        for (PlayerServerData p : players)
        {
            if (p.lobbyData.role == ERole.fueler)
                return true;
        }
        return false;
    }

    private boolean isDriverConnected()
    {
        for (PlayerServerData p : players)
        {
            if (p.lobbyData.role == ERole.driver)
                return true;
        }
        return false;
    }
}
