package Utilities.SocketData;

import Utilities.PlayerData;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Vector;



public class ServerToLobbyData implements Serializable
{
    public Vector<PlayerData> players = new Vector<>();
    public boolean timeout = false;
    public boolean kicked = false;
    public int yourID = 0;
    public boolean gameStarted = false;

    public PointToPointData gameData = null;
}
