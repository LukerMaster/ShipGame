package Utilities;

import java.io.Serializable;
import java.util.ArrayList;

public class ServerToDriverData implements Serializable
{
    public double gameTimePassed;
    public float energy;
    public float shipPos;
    public float temperatureShip;
    public ArrayList<Meteor> meteors;
    public String gameOverReason;
    public boolean isGameOver;
}
