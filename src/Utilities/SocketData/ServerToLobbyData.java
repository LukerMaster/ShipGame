package Utilities.SocketData;

import Utilities.PlayerData;

import java.util.Vector;



public class ServerToLobbyData
{
    public Vector<PlayerData> players = new Vector<>();
    public boolean timeout = false;
    public boolean kicked = false;
    public int yourID = 0;
    public boolean gameStarted = false;

    PointToPointData gameData;
}
