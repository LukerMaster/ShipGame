package Utilities.SocketData;

import Utilities.Meteor;

import java.io.Serializable;
import java.util.ArrayList;

public class ServerToFuelerData implements Serializable, PointToPointData
{
    public String EnergyLabel;
    public float temperaturePercent;
    public float chargeBatteryCooldown;
    public float chargeShipCooldown;
    public double gameTimePassed;
    public int batteriesCharged;
    public boolean isEnoughToCharge;
    public boolean isHeaterOn;
    public ArrayList<Meteor> meteors;
    public String gameOverReason;
    public boolean isGameOver;
}