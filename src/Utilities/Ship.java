package Utilities;

import java.util.ArrayList;
import java.util.LinkedList;

import static com.sun.javafx.util.Utils.clamp;

public class Ship
{
    final float batteryCooldownMax = 20.0f;
    final float shipChargeCooldownMax = 20.0f;

    volatile ArrayList<Meteor> meteors = new ArrayList<>();

    public double gameTime = 0.0;

    LinkedList<FuelerAction> fuelerActions = new LinkedList<>();

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

    public void clampValues()
    {
        energy = clamp(0.0f,energy, 1.0f);
        temperatureFurnace = clamp( 0.0f,temperatureFurnace, 1.0f);
        temperatureShip = clamp(-273.0f,temperatureShip, 42069.0f);
        currentFuel = clamp(0.0f,currentFuel,20.0f);
        batteriesCharged = clamp(0,batteriesCharged, 10);
    }

    public void update(float dt) // dt in seconds
    {
        energy -= 0.02f * dt;
        gameTime += dt;

        batteryChargeCooldown -= dt;
        shipChargeCooldown -= dt;


    }
}
