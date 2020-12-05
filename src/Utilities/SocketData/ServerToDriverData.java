package Utilities.SocketData;

import Utilities.Meteor;

import java.io.Serializable;
import java.util.ArrayList;

public class ServerToDriverData implements PointToPointData
{
    public double gameTimePassed;
    public float energy;
    public float shipPos;
    public float temperatureShip;
    public String gameOverReason;
    public boolean isGameOver;

    public ArrayList<Meteor> meteors;
}
