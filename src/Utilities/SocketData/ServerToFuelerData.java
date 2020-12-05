package Utilities.SocketData;

import Utilities.Meteor;

import java.util.ArrayList;

public class ServerToFuelerData implements PointToPointData
{
    public String energyLabel;
    public float temperaturePercent;
    public float chargeBatteryCooldown;
    public float chargeShipCooldown;
    public double gameTimePassed;
    public int batteriesCharged;
    public boolean isEnoughToCharge;
    public boolean isHeaterOn;
    public String gameOverReason;
    public boolean isGameOver;

    public ArrayList<Meteor> meteors;
}
