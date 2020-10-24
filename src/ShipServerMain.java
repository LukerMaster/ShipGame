import Utilities.*;
import com.google.gson.Gson;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;

public class ShipServerMain
{
    boolean applicationRuns = true;

    final float batteryCooldownMax = 20.0f;
    final float shipChargeCooldownMax = 20.0f;

    volatile ArrayList<Meteor> meteors = new ArrayList<>();

    double gameTime = 0.0;

    ServerSocket soc;

    Thread mainLoopThread;
    Thread connectionThread;

    LinkedList<FuelerAction> fuelerActions = new LinkedList<>();

    volatile DriverToServerData driverDataIn = new DriverToServerData();
    volatile FuelerToServerData fuelerDataIn = new FuelerToServerData();

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
        window = new JFrame("Absolutely Worst Space Journey - Server");
        window.setSize(450, 100);
        window.setResizable(false);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent we)
            {
                applicationRuns = false;
                try
                {
                    if (soc != null)
                        soc.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                super.windowClosing(we);
            }
        });
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




        // Main loop of the game.
        mainLoopThread = new Thread(() ->
        {
            try
            {
                printToConsoleAndWindow("Main game loop started.");
                while (applicationRuns && !isGameOver)
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
                    if (driverDataIn.isLeftPressed || driverDataIn.isRightPressed && energy > 0.0012f)
                    {
                        energy -= 0.0012f;
                        if (driverDataIn.isRightPressed)
                            shipPos += 0.03f;
                        if (driverDataIn.isLeftPressed)
                            shipPos -= 0.03f;
                    }
                    if (driverDataIn.isCoolingPressed)
                    {
                        energy -= 0.0002f;
                        temperatureFurnace -= 0.0008f;
                        temperatureShip -= 0.01f;
                    }
                    // Fueler data processing
                    FuelerAction action = fuelerActions.size() > 0 ? fuelerActions.poll() : FuelerAction.noAction;
                    switch (action)
                    {
                        case noAction:
                            break;
                        case addedFuel:
                        {
                            currentFuel += 0.2f;
                        }
                        break;
                        case chargedShip:
                        {
                            energy += 0.5f;
                            batteriesCharged--;
                            shipChargeCooldown = shipChargeCooldownMax;
                        }
                        break;
                        case chargedBattery:
                        {
                            energy -= 0.5f;
                            batteriesCharged++;
                            batteryChargeCooldown = batteryCooldownMax;
                        }
                        break;
                        case switchedHeater:
                        {
                            isHeaterOn = !isHeaterOn;
                        }
                        break;
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
                        temperatureFurnace -= (0.001f * temperatureFurnace) + 0.00005f;



                    // Heating the ship
                    if (isHeaterOn)
                    {
                        temperatureFurnace -= 0.0002f * temperatureFurnace;
                        temperatureShip += 0.005f * temperatureFurnace * temperatureFurnace;
                    }

                    // Game overs.
                    for (Meteor m : meteors)
                    {
                        if (m.y < 1 && Math.abs(shipPos - m.x) < 5)
                        {
                            gameOverCause = "Ship violently smashed by a meteor.";
                            isGameOver = true;
                            break;
                        }
                    }
                    if (temperatureFurnace >= 1.0f)
                    {
                        gameOverCause = "Ship boiler exploded.";
                        isGameOver = true;
                    }
                    else if (temperatureFurnace <= 0.0f)
                    {
                        gameOverCause = "Ship boiler stalled.";
                        isGameOver = true;
                    }
                    else if (temperatureShip < -10.0f)
                    {
                        gameOverCause = "Ship driver frozen.";
                        isGameOver = true;
                    }
                    else if (temperatureShip > 50.0f)
                    {
                        gameOverCause = "Ship driver melted.";
                        isGameOver = true;
                    }

                    if (isGameOver)
                        printToConsoleAndWindow(gameOverCause);

                    clampValues();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });

        connectionThread = new Thread(() ->
        {
            try
            {
                soc = new ServerSocket(port);
            }
            catch (IOException ie)
            {
                ie.printStackTrace();
            }

            ObjectOutputStream driverOut = null;
            ObjectOutputStream fuelerOut = null;

            ObjectInputStream driverIn = null;
            ObjectInputStream fuelerIn = null;

            Socket driverSoc = null;
            Socket fuelerSoc = null;
            while ((driverSoc == null || fuelerSoc == null) && applicationRuns)
            {
                try
                {
                    boolean decision = false;
                    Socket current = soc.accept();
                    ObjectInputStream currentIn = new ObjectInputStream(current.getInputStream());
                    ObjectOutputStream currentOut = new ObjectOutputStream(current.getOutputStream());
                    int type = currentIn.readInt(); // 1 - Driver, 2 - Fueler, 3 - Spectator
                    if (type == 1 && driverSoc == null)
                    {
                        driverSoc = current;
                        driverIn = currentIn;
                        driverOut = currentOut;
                        decision = true;
                        printToConsoleAndWindow("Driver Connected.");
                    }
                    else if (type == 2 && fuelerSoc == null)
                    {
                        fuelerSoc = current;
                        fuelerIn = currentIn;
                        fuelerOut = currentOut;
                        decision = true;
                        printToConsoleAndWindow("Fueler Connected.");
                    }
                    currentOut.writeBoolean(decision);
                    currentOut.flush();
                    if (decision == false)
                        current.close();

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            if (applicationRuns)
            {
                printToConsoleAndWindow("Both players connected. Game starts in 2 seconds.");
                try
                {
                    Thread.sleep(2000);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                mainLoopThread.start();
                printToConsoleAndWindow("Game started.");
            }


            while (applicationRuns)
            {
                if (!driverSoc.isClosed())
                {
                    ServerToDriverData driverDataOut = new ServerToDriverData();
                    driverDataOut.energy = energy;
                    driverDataOut.gameTimePassed = gameTime;
                    driverDataOut.meteors = new ArrayList<>(meteors);
                    driverDataOut.shipPos = shipPos;
                    driverDataOut.temperatureShip = temperatureShip;
                    driverDataOut.isGameOver = isGameOver;
                    driverDataOut.gameOverReason = gameOverCause;

                    try
                    {
                        Gson gson = new Gson();
                        String outString = gson.toJson(driverDataOut);
                        driverOut.writeObject(outString);

                        driverDataIn = gson.fromJson((String)driverIn.readObject(), DriverToServerData.class);

                    }
                    catch (Exception e)
                    {
                        isGameOver = true;
                        gameOverCause = "Driver decided to commit sudoku.";
                        try
                        {
                            driverSoc.close();
                        }
                        catch (Exception ie)
                        {
                            ie.printStackTrace();
                        }
                    }
                }
                if (!fuelerSoc.isClosed())
                {
                    ServerToFuelerData fuelerDataOut = new ServerToFuelerData();
                    fuelerDataOut.batteriesCharged = batteriesCharged;
                    fuelerDataOut.chargeBatteryCooldown = batteryChargeCooldown;
                    fuelerDataOut.chargeShipCooldown = shipChargeCooldown;
                    fuelerDataOut.EnergyLabel = energy > 0.95f ? "Full" : energy > 0.5f ? "Ok" : energy > 0.25f ? "Not ok" : "NOT OK!";
                    fuelerDataOut.gameTimePassed = gameTime;
                    fuelerDataOut.isEnoughToCharge = energy > 0.5f;
                    fuelerDataOut.isHeaterOn = isHeaterOn;
                    fuelerDataOut.temperaturePercent = temperatureFurnace;
                    fuelerDataOut.isGameOver = isGameOver;
                    fuelerDataOut.gameOverReason = gameOverCause;
                    try
                    {
                        Gson gson = new Gson();
                        String outString = gson.toJson(fuelerDataOut);
                        fuelerOut.writeObject(outString);
                        fuelerDataIn = gson.fromJson((String)fuelerIn.readObject(), FuelerToServerData.class);
                        if (fuelerDataIn.action != FuelerAction.noAction)
                            fuelerActions.add(fuelerDataIn.action);
                    }
                    catch (Exception e)
                    {
                        isGameOver = true;
                        gameOverCause = "Fueler jumped out of the window.";
                        try
                        {
                            fuelerSoc.close();
                        }
                        catch (Exception ie)
                        {
                            ie.printStackTrace();
                        }
                    }
                }

            }


        });

        if (applicationRuns)
        {
            connectionThread.start();
            try
            {
                mainLoopThread.join();
                connectionThread.join();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            printToConsoleAndWindow("Shutting down the server.");
        }


    }
}
