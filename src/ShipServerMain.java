import Utilities.DriverToServerData;
import Utilities.FuelerToServerData;
import Utilities.ServerToDriverData;
import Utilities.ServerToFuelerData;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ShipServerMain
{
    final float batteryCooldownMax = 10.0f;

    ServerSocket soc;
    volatile Socket driverSocket;
    volatile Socket fuelerSocket;

    Thread shipDriverThread;
    Thread shipFuelerThread;
    Thread mainLoopThread;

    volatile DriverToServerData driverData = new DriverToServerData();
    volatile FuelerToServerData fuelerData = new FuelerToServerData();

    boolean isHeaterOn = false;
    float energy = 1.0f;
    float currentFuel = 0.2f;
    float temperatureFurnace = 0.6f;
    float temperatureShip = 21.0f;
    float batteryChargeCooldown = 10.0f;
    float shipChargeCooldown = 10.0f;

    int batteriesCharged = 0;


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


    public static void main(String[] args)
    {
        ShipServerMain shipServer = new ShipServerMain();
    }

    ShipServerMain()
    {
        try
        {
            soc = new ServerSocket(10009);
            System.out.println("Started!");

            ObjectInputStream in;
            Socket first = soc.accept();
            System.out.println("First player connected!");
            in = new ObjectInputStream(first.getInputStream());
            boolean isFirstDriver = in.readBoolean();

            Socket second = soc.accept();
            System.out.println("Second player connected!");
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
                System.out.println("Wrong player setup! Two fuelers or two drivers connected.");
                return;
            }
            System.out.println("Setup complete.");



            // Main loop of the game.
            mainLoopThread = new Thread(() ->
            {
                try
                {
                    System.out.println("Main game loop started.");
                    while (true)
                    {
                        Thread.sleep(2);

                        energy -= 0.0001f;
                        // Drain energy when steering
                        if (driverData.isLeftPressed || driverData.isRightPressed)
                            energy -= 0.0003f;

                        // Burn the fuel
                        if (currentFuel > 0.0f)
                        {
                            energy += 0.0002f;
                            currentFuel -= 0.00002f;
                            temperatureFurnace += 0.0001f * currentFuel;

                            // Overheat the furnace if energy is full
                            if (energy >= 1.0f)
                                temperatureFurnace += 0.0003f * currentFuel;
                        }
                        // Cool down the furnace if no fuel
                        else
                        {
                            temperatureFurnace -= 0.0001f;
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

                    while (!driverSocket.isClosed())
                    {

                        ServerToDriverData data = new ServerToDriverData();
                        data.energy = energy;
                        outputStream.writeObject(data);

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


            shipFuelerThread = new Thread(() ->
            {
                try
                {
                    ObjectOutputStream outputStream = new ObjectOutputStream(fuelerSocket.getOutputStream());

                    while (!fuelerSocket.isClosed())
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

                        data.canChargeBattery = batteryChargeCooldown <= 0.0f;
                        data.canChargeShip = shipChargeCooldown <= 0.0f && batteriesCharged > 0;
                        data.isHeaterOn = isHeaterOn;

                        outputStream.writeObject(data);

                        outputStream.flush();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            );
            shipFuelerThread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Quitting the lobby, starting the game.");
    }
}
