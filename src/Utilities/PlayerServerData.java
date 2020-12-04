package Utilities;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PlayerServerData
{
    public Socket socket;
    public ObjectOutputStream outputStream;
    public ObjectInputStream inputStream;

    public PlayerData data = new PlayerData();
}

