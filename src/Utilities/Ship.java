package Utilities;

import Utilities.SocketData.DriverToServerData;
import Utilities.SocketData.FuelerToServerData;
import Utilities.SocketData.ServerToDriverData;
import Utilities.SocketData.ServerToFuelerData;

import java.util.ArrayList;
import java.util.LinkedList;

import static com.sun.javafx.util.Utils.clamp;

public class Ship
{
    final float batteryCooldownMax = 20.0f;
    final float shipChargeCooldownMax = 20.0f;

    volatile ArrayList<Meteor> meteors = new ArrayList<>();

    public double gameTime = 0.0;

    boolean isHeaterOn = false;
    float shipPos = 0.0f;
    float energy = 1.0f;
    float currentFuel = 0.2f;
    float temperatureFurnace = 0.6f;
    float temperatureShip = 21.0f;
    float batteryChargeCooldown = 10.0f;
    float shipChargeCooldown = 10.0f;

    int batteriesCharged = 0;

    boolean isGameOver = false;
    String gameOverCause;



    public ServerToFuelerData getFuelerData()
    {
        ServerToFuelerData data = new ServerToFuelerData();
        data.energyLabel = energy > 0.90f ? "FULL." : energy > 0.5f ? "Ok." : energy > 0.35f ? "Not ok." : "NOT OK!";
        data.temperaturePercent = temperatureFurnace;
        data.chargeBatteryCooldown = batteryChargeCooldown;
        data.chargeShipCooldown = shipChargeCooldown;
        data.gameTimePassed = gameTime;
        data.batteriesCharged = batteriesCharged;
        data.isEnoughToCharge = energy >= 0.5f;
        data.isHeaterOn = isHeaterOn;
        data.meteors = meteors; // To rethink
        data.gameOverReason = gameOverCause;
        data.isGameOver = isGameOver;
        return data;
    }

    public ServerToDriverData getDriverData()
    {
        ServerToDriverData data = new ServerToDriverData();
        data.energy = energy;
        data.gameOverReason = gameOverCause;
        data.gameTimePassed = gameTime;
        data.isGameOver = isGameOver;
        data.meteors = meteors; // To rethink
        data.shipPos = shipPos;
        data.temperatureShip = temperatureShip;
        return data;
    }

    public void clampValues()
    {
        energy = clamp(0.0f,energy, 1.0f);
        temperatureFurnace = clamp( 0.0f,temperatureFurnace, 1.0f);
        temperatureShip = clamp(-273.0f,temperatureShip, 42069.0f);
        currentFuel = clamp(0.0f,currentFuel,20.0f);
        batteriesCharged = clamp(0,batteriesCharged, 10);
    }

    void handleDriverInput(float dt, DriverToServerData data)
    {
        if (data.isCoolingPressed)
        {
            temperatureFurnace -= dt * 0.2f;
            temperatureShip -= dt * 4.0f;
        }
        if (energy > 0.02f * dt)
        {
            if (data.isLeftPressed)
            {
                shipPos -= 5 * dt;
                energy -= 0.2f * dt;
            }
            if (data.isRightPressed)
            {
                shipPos += 5 * dt;
                energy -= 0.2f * dt;
            }
        }
    }
    void handleFuelerInput(float dt, FuelerToServerData data)
    {
        switch (data.action)
        {
            case switchedHeater:
                isHeaterOn = !isHeaterOn;
                break;
            case addedFuel:
                currentFuel += 2.0f;
                break;
            case chargedShip:
                batteriesCharged--;
                shipChargeCooldown = shipChargeCooldownMax;
                energy += 0.5f;
                break;
            case chargedBattery:
                batteriesCharged++;
                batteryChargeCooldown = batteryCooldownMax;
                energy -= 0.5f;
                break;
        }
    }

    public void update(float dt, DriverToServerData driverData, FuelerToServerData fuelerData) // dt in seconds
    {
        if (driverData != null)
            handleDriverInput(dt, driverData);
        if (fuelerData != null)
            handleFuelerInput(dt, fuelerData);

        energy -= 0.02f * dt;
        gameTime += dt;

        batteryChargeCooldown -= dt;
        shipChargeCooldown -= dt;

        synchronized (meteors)
        {
            meteors.forEach(meteor -> meteor.move(dt));
            meteors.removeIf(meteor -> {return meteor.y < 0;});
            while (meteors.size() < 10)
            {
                meteors.add(new Meteor(shipPos, 30, true));
            }
        }

    }
}
