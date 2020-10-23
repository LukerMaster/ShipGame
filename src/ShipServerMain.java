import Utilities.*;
import com.google.gson.Gson;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

public class ShipServerMain
{
    final float batteryCooldownMax = 20.0f;
    final float shipChargeCooldownMax = 20.0f;

    volatile ArrayList<Meteor> meteors = new ArrayList<>();

    double gameTime = 0.0;

    ServerSocket soc;
    volatile Socket driverSocket;
    volatile Socket fuelerSocket;

    Thread shipDriverThread;
    Thread shipServerToFuelerThread;
    Thread shipFuelerToServerThread;
    Thread mainLoopThread;

    volatile DriverToServerData driverData = new DriverToServerData();
    volatile FuelerToServerData fuelerData = new FuelerToServerData();

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

    JFrame window;
    JLabel info;
    JLabel gameTimeLabel;

    private static float clamp(float val, float min, float max)
    {
        return Math.max(min, Math.min(max, val));
    }
    private static int clamp(int val, int min, int max)
    {
        return Math.max(min, Math.min(max, val));
    }

    private void clampValues()
    {
        energy = clamp(energy, 0.0f, 1.0f);
        temperatureFurnace = clamp(temperatureFurnace, 0.0f, 1.0f);
        temperatureShip = clamp(temperatureShip, -273.0f, 42069.0f);
        currentFuel = clamp(currentFuel, 0.0f, 20.0f);
        batteriesCharged = clamp(batteriesCharged, 0, 10);
    }

    private void printToConsoleAndWindow(String text)
    {
        System.out.println(text);
        info.setText(text);
    }

    public static void main(String[] args)
    {
        ShipServerMain shipServer = new ShipServerMain(Integer.parseInt(args[0]));
    }

    ShipServerMain(int port)
    {
        try
        {
            window = new JFrame("Absolutely Worst Space Journey - Server");
            window.setSize(400, 100);
            window.setLayout(null);

            info = new JLabel();
            info.setText("Starting...");
            info.setBounds(10, 10, 380, 20);

            gameTimeLabel = new JLabel();
            gameTimeLabel.setText("Game not started yet.");
            gameTimeLabel.setBounds(10, 40, 380, 20);

            window.add(info);
            window.add(gameTimeLabel);
            window.setVisible(true);


            soc = new ServerSocket(port);
            printToConsoleAndWindow("Started! Waiting for players...");

            ObjectInputStream in;
            Socket first = soc.accept();
            printToConsoleAndWindow("First player connected!");
            in = new ObjectInputStream(first.getInputStream());
            boolean isFirstDriver = in.readBoolean();

            Socket second = soc.accept();
            printToConsoleAndWindow("Second player connected!");
            in = new ObjectInputStream(second.getInputStream());
            boolean isSecondDriver = in.readBoolean();

            if (isFirstDriver != isSecondDriver)
            {
                if (isFirstDriver)
                {
                    driverSocket = first;
                    fuelerSocket = second;
                }
                else
                {
                    driverSocket = second;
                    fuelerSocket = first;
                }
            }
            else
            {
                printToConsoleAndWindow("Wrong player setup! Two fuelers or two drivers connected.");
                return;
            }
            printToConsoleAndWindow("Setup complete.");



            // Main loop of the game.
            mainLoopThread = new Thread(() ->
            {
                try
                {
                    printToConsoleAndWindow("Main game loop started.");
                    while (!isGameOver)
                    {
                        Thread.sleep(2);
                        gameTime += 0.002;
                        gameTimeLabel.setText("Game time: " + String.format("%.1f", gameTime) + "s");

                        batteryChargeCooldown -= 0.002f;
                        shipChargeCooldown -= 0.002f;

                        // Calculate the meteors
                        if (meteors.size() < 10) meteors.add(new Meteor(shipPos, 200, true));


                        meteors.removeIf(meteor -> meteor.y < 0);
                        for (Meteor m : meteors)
                        {
                            m.y += m.velY;
                            m.x += m.velX;
                        }


                        // Drain energy when steering
                        energy -= 0.0001f;
                        if (driverData.isLeftPressed || driverData.isRightPressed && energy > 0.0012f)
                        {
                            energy -= 0.0012f;
                            if (driverData.isRightPressed)
                                shipPos += 0.03f;
                            if (driverData.isLeftPressed)
                                shipPos -= 0.03f;
                        }
                        if (driverData.isCoolingPressed)
                        {
                            energy -= 0.0002f;
                            temperatureFurnace -= 0.0005f;
                            temperatureShip -= 0.01f;
                        }



                        // Burn the fuel
                        if (currentFuel > 0.0f)
                        {
                            currentFuel -= 0.00002f;
                            temperatureFurnace += 0.0004f * currentFuel * currentFuel;

                            // Charge the ship from the boiler
                            if (energy < 1.0f)
                            {
                                temperatureFurnace -= 0.0003f * currentFuel;
                                energy += 0.0005f * temperatureFurnace;
                            }
                        }
                        // Cool down the furnace if no fuel
                        else
                            temperatureFurnace -= 0.001f * temperatureFurnace;



                        // Heating the ship
                        if (isHeaterOn)
                        {
                            temperatureFurnace -= 0.0004f * temperatureFurnace;
                            temperatureShip += 0.005f * temperatureFurnace * temperatureFurnace;
                        }

                        // Game overs.
                        for (Meteor m : meteors)
                        {
                            if (m.y < 1 && Math.abs(shipPos - m.x) < 5)
                            {
                                printToConsoleAndWindow("Ship violently smashed by a meteor, game over.");
                                isGameOver = true;
                            }
                        }
                        if (temperatureFurnace >= 1.0f)
                        {
                            printToConsoleAndWindow("Ship boiler exploded, game over.");
                            isGameOver = true;
                        }
                        else if (temperatureFurnace <= 0.0f)
                        {
                            printToConsoleAndWindow("Ship boiler stalled, game over.");
                            isGameOver = true;
                        }
                        else if (temperatureShip < -10.0f)
                        {
                            printToConsoleAndWindow("Ship driver frozen, game over.");
                            isGameOver = true;
                        }
                        else if (temperatureShip > 50.0f)
                        {
                            printToConsoleAndWindow("Ship driver melted, game over.");
                            isGameOver = true;
                        }

                        clampValues();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
            mainLoopThread.start();

            // Communication with ship driver.
            shipDriverThread = new Thread(() ->
            {
                try
                {
                    ObjectOutputStream outputStream = new ObjectOutputStream(driverSocket.getOutputStream());
                    ObjectInputStream inputStream = new ObjectInputStream(driverSocket.getInputStream());

                    while (!isGameOver)
                    {

                        ServerToDriverData data = new ServerToDriverData();
                        data.energy = energy;
                        data.shipPos = shipPos;
                        data.temperatureShip = temperatureShip;
                        data.meteors = new ArrayList<>(meteors);

                        Gson gson = new Gson();
                        String json = gson.toJson(data);
                        outputStream.writeObject(json);
                        outputStream.flush();

                        driverData = (DriverToServerData) inputStream.readObject();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            );
            shipDriverThread.start();


            shipServerToFuelerThread = new Thread(() ->
            {
                try
                {
                    ObjectOutputStream outputStream = new ObjectOutputStream(fuelerSocket.getOutputStream());

                    while (!isGameOver)
                    {
                        ServerToFuelerData data = new ServerToFuelerData();
                        if (energy > 0.7f)
                            data.EnergyLabel = "Nice";
                        else if (energy > 0.4f)
                            data.EnergyLabel = "Ok";
                        else if (energy > 0.2f)
                            data.EnergyLabel = "monkaS";
                        else
                            data.EnergyLabel = "monkaGIGA";
                        data.temperaturePercent = temperatureFurnace;

                        data.chargeBatteryCooldown = batteryChargeCooldown;
                        data.chargeShipCooldown = shipChargeCooldown;
                        data.batteriesCharged = batteriesCharged;
                        data.isEnoughToCharge = energy >= 0.5f;
                        data.isHeaterOn = isHeaterOn;

                        outputStream.writeObject(data);

                        outputStream.flush();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
            shipServerToFuelerThread.start();


            shipFuelerToServerThread = new Thread(() ->
            {
               try
               {
                   while (!isGameOver)
                   {
                       ObjectInputStream inputStream = new ObjectInputStream(fuelerSocket.getInputStream());

                       FuelerToServerData inData = (FuelerToServerData) inputStream.readObject();

                       switch (inData.action)
                       {
                           case addedFuel:
                           {
                               currentFuel += 0.2f;
                           }
                           break;
                           case chargedBattery:
                           {
                               batteriesCharged++;
                               batteryChargeCooldown = batteryCooldownMax;
                               energy -= 0.5f;
                           }
                           break;
                           case chargedShip:
                           {
                               batteriesCharged--;
                               energy += 0.5f;
                               shipChargeCooldown = shipChargeCooldownMax;
                           }
                           break;
                           case switchedHeater:
                           {
                               isHeaterOn = !isHeaterOn;
                           }
                       }
                   }
               }
               catch (Exception e)
               {
                   e.printStackTrace();
               }
            });
            shipFuelerToServerThread.start();


        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        printToConsoleAndWindow("Quitting the lobby, starting the game.");
    }
}
