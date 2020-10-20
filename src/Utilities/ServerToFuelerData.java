package Utilities;

import java.io.Serializable;

public class ServerToFuelerData implements Serializable
{
    public String EnergyLabel;
    public float temperaturePercent;
    public boolean canChargeBattery;
    public boolean canChargeShip;
    public boolean isHeaterOn;
}
