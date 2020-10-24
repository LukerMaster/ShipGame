import Utilities.*;
import com.google.gson.Gson;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ShipFuelerMain
{
    boolean applicationRuns = true;

    JFrame window;
    JPanelWithBG panel;

    JButton addFuelBtn;
    JButton chargeBatteryBtn;
    JButton chargeShipBtn;
    JButton switchHeatingBtn;

    JProgressBar temperatureBar;
    JLabel energyLabel;
    JLabel batteriesChargedLabel;
    JLabel gameTimeLabel;
    JLabel gameOverText;

    volatile LinkedList<FuelerAction> pendingActions = new LinkedList<FuelerAction>();

    Thread connectionToServer;

    volatile Socket soc;


    public static void main(String[] args)
    {
        ShipFuelerMain fuelManager = new ShipFuelerMain(args[0], Integer.parseInt(args[1]));
    }

    public void setTextOnHeater(boolean isOn)
    {
        StringBuilder heaterText = new StringBuilder("Heating: ");
        if (isOn) heaterText.append("On");
        else heaterText.append("Off");
        switchHeatingBtn.setText(heaterText.toString());
    }


    ShipFuelerMain(String ipAddress, int port)
    {
        window = new JFrame("Fuel Manager");
        window.setSize(600, 630);
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

        panel = new JPanelWithBG("assets/fuel.png");
        panel.setLayout(null);
        panel.setBounds(0, 0, 600, 600);

        window.add(panel);
        window.setResizable(false);

        addFuelBtn = new JButton();
        addFuelBtn.setBounds(110, 510, 60, 40);
        addFuelBtn.setText("Add Fuel");
        addFuelBtn.setMargin(new Insets(0, 0, 0, 0));
        addFuelBtn.setBorder(null);

        chargeBatteryBtn = new JButton();
        chargeBatteryBtn.setBounds(490, 400, 100, 30);
        chargeBatteryBtn.setText("Charge Battery");
        chargeBatteryBtn.setMargin(new Insets(0, 0, 0, 0));
        chargeBatteryBtn.setBorder(null);

        chargeShipBtn = new JButton();
        chargeShipBtn.setBounds(490, 370, 100, 30);
        chargeShipBtn.setText("Charge Ship");
        chargeShipBtn.setMargin(new Insets(0, 0, 0, 0));
        chargeShipBtn.setBorder(null);

        switchHeatingBtn = new JButton();
        switchHeatingBtn.setBounds(330, 230, 70, 40);
        switchHeatingBtn.setText("Heating: Off");
        switchHeatingBtn.setMargin(new Insets(0, 0, 0, 0));
        switchHeatingBtn.setBorder(null);

        temperatureBar = new JProgressBar();
        temperatureBar.setForeground(new Color(255, 0, 0));
        temperatureBar.setValue(50);
        temperatureBar.setBounds(100, 250, 100, 30);

        energyLabel = new JLabel();
        energyLabel.setText("Good");
        energyLabel.setBounds(455, 210, 70, 30);

        gameTimeLabel = new JLabel();
        gameTimeLabel.setText("Not connected.");
        gameTimeLabel.setBounds(275, 560, 120, 30);

        batteriesChargedLabel = new JLabel();
        batteriesChargedLabel.setText("No info.");
        batteriesChargedLabel.setBounds(370, 460, 150, 40);

        gameOverText = new JLabel();
        gameOverText.setText("CONNECTING...");
        gameOverText.setBounds(10, 10, 600, 50);
        gameOverText.setFont(new Font("tahoma", Font.PLAIN, 20));

        panel.add(addFuelBtn);
        panel.add(chargeBatteryBtn);
        panel.add(chargeShipBtn);
        panel.add(energyLabel);
        panel.add(switchHeatingBtn);
        panel.add(temperatureBar);
        panel.add(gameTimeLabel);
        panel.add(batteriesChargedLabel);
        panel.add(gameOverText);
        window.setVisible(true);


        connectionToServer = new Thread(() ->
        {
            ObjectInputStream inputStream = null;
            ObjectOutputStream outputStream = null;

            boolean connected = false;
            while (!connected && applicationRuns)
            {
                try
                {
                    Thread.sleep(1000);
                    System.out.println("Trying to connect...");
                    soc = new Socket(ipAddress, port);
                    ObjectOutputStream currentOut = new ObjectOutputStream(soc.getOutputStream());
                    ObjectInputStream currentIn = new ObjectInputStream(soc.getInputStream());
                    currentOut.writeInt(2);
                    currentOut.flush();

                    connected = currentIn.readBoolean();
                    if (!connected)
                    {
                        soc.close();
                    }
                    else
                    {
                        inputStream = currentIn;
                        outputStream = currentOut;
                    }
                }
                catch (ConnectException e)
                {
                    System.out.println("Trying to reconnect...");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    applicationRuns = false;
                }
            }
            System.out.println("Connected.");

            if (applicationRuns)
            {
                try
                {
                    while (applicationRuns)
                    {
                        // Input
                        String input = (String) inputStream.readObject();
                        ServerToFuelerData inData = new Gson().fromJson(input, ServerToFuelerData.class);

                        chargeBatteryBtn.setEnabled(inData.chargeBatteryCooldown <= 0 && inData.batteriesCharged < 10 && inData.isEnoughToCharge);
                        chargeShipBtn.setEnabled(inData.chargeShipCooldown <= 0 && inData.batteriesCharged > 0);

                        if (inData.chargeBatteryCooldown > 0.0f && inData.batteriesCharged < 10)
                            chargeBatteryBtn.setText("Cooldown: " + String.format("%.1f", inData.chargeBatteryCooldown) + "s");
                        else if (inData.batteriesCharged == 10)
                            chargeBatteryBtn.setText("All charged");
                        else if (!inData.isEnoughToCharge)
                            chargeBatteryBtn.setText("No energy");
                        else
                            chargeBatteryBtn.setText("Charge battery");

                        if (inData.chargeShipCooldown > 0.0f)
                            chargeShipBtn.setText("Cooldown: " + String.format("%.1f", inData.chargeShipCooldown) + "s");
                        else if (inData.batteriesCharged == 0)
                            chargeShipBtn.setText("No batteries");
                        else
                            chargeShipBtn.setText("Charge ship");

                        batteriesChargedLabel.setText("Charged: " + inData.batteriesCharged);


                        temperatureBar.setValue((int) (inData.temperaturePercent * 100));
                        energyLabel.setText(inData.EnergyLabel);
                        gameTimeLabel.setText("Time: " + String.format("%.1f", inData.gameTimePassed) + "s");

                        setTextOnHeater(inData.isHeaterOn);
                        temperatureBar.setForeground(new Color((int) (inData.temperaturePercent * 255), 0, 0));

                        if (inData.isGameOver)
                        {
                            gameOverText.setText(inData.gameOverReason);
                        }
                        else
                        {
                            gameOverText.setText("");
                        }

                        // Output
                        FuelerToServerData outData = new FuelerToServerData();
                        if (!pendingActions.isEmpty())
                            outData.action = pendingActions.poll();
                        else
                            outData.action = FuelerAction.noAction;


                        String outString = new Gson().toJson(outData);
                        outputStream.writeObject(outString);


                    }
                    outputStream.close();
                    inputStream.close();
                    System.out.println("Stopped connection.");
                }
                catch (EOFException | SocketException se)
                {
                    System.out.println("Lost connection to server.");
                    applicationRuns = false;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    applicationRuns = false;
                    window.dispose();
                }
            }
        });
        connectionToServer.start();


        addFuelBtn.addActionListener(e -> pendingActions.add(FuelerAction.addedFuel));
        chargeBatteryBtn.addActionListener(e -> pendingActions.add(FuelerAction.chargedBattery));
        chargeShipBtn.addActionListener(e -> pendingActions.add(FuelerAction.chargedShip));
        switchHeatingBtn.addActionListener(e -> pendingActions.add(FuelerAction.switchedHeater));



        try
        {
            connectionToServer.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}
