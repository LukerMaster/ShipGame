package Utilities;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PlayerServerData
{
    public Socket socket;
    public ObjectOutputStream outputStream;
    public ObjectInputStream inputStream;
    public boolean toDelete = false;
    public Thread connectionThread;

    public PlayerData lobbyData = new PlayerData();
}

