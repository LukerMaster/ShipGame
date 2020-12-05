package Utilities.SocketData;

import java.io.Serializable;

public class LobbyToServerData implements Serializable
{
    public String nickname = "Unknown";
    public boolean isReady = false;

    public PointToPointData gameData;
}
