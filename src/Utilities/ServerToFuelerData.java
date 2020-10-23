package Utilities;

import java.io.Serializable;

public class ServerToFuelerData implements Serializable
{
    public String EnergyLabel;
    public float temperaturePercent;
    public float chargeBatteryCooldown;
    public float chargeShipCooldown;
    public int batteriesCharged;
    public boolean isEnoughToCharge;
    public boolean isHeaterOn;
}
