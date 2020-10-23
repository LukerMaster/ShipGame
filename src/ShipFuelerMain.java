import Utilities.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ShipFuelerMain
{
    JFrame window;
    JPanelWithBG panel;

    JButton addFuelBtn;
    JButton chargeBatteryBtn;
    JButton chargeShipBtn;
    JButton switchHeatingBtn;

    JProgressBar temperatureBar;
    JLabel energyLabel;
    JLabel batteriesChargedLabel;

    Thread connectionToServer;

    volatile Socket soc;
    ServerToFuelerData inData;

    public static void main(String[] args)
    {
        ShipFuelerMain fuelManager = new ShipFuelerMain(args[0], Integer.parseInt(args[1]));
    }

    public void SendAction(FuelerAction action)
    {
        try
        {
            ObjectOutputStream outputStream = new ObjectOutputStream(soc.getOutputStream());
            FuelerToServerData data = new FuelerToServerData();
            data.action = action;
            outputStream.writeObject(data);
            outputStream.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
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
        panel = new JPanelWithBG("fuel.png");
        panel.setLayout(null);
        panel.setBounds(0, 0, 600, 600);

        window.add(panel);
        window.setVisible(true);
        window.setResizable(false);

        addFuelBtn = new JButton();
        addFuelBtn.setBounds(110, 510, 60, 40);
        addFuelBtn.setText("Add Fuel");
        addFuelBtn.setMargin(new Insets(0, 0, 0, 0));
        addFuelBtn.setBorder(null);

        chargeBatteryBtn = new JButton();
        chargeBatteryBtn.setBounds(500, 400, 90, 30);
        chargeBatteryBtn.setText("Charge Battery");
        chargeBatteryBtn.setMargin(new Insets(0, 0, 0, 0));
        chargeBatteryBtn.setBorder(null);

        chargeShipBtn = new JButton();
        chargeShipBtn.setBounds(500, 370, 90, 30);
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

        batteriesChargedLabel = new JLabel();
        batteriesChargedLabel.setText("No info.");
        batteriesChargedLabel.setBounds(370, 460, 150, 40);

        panel.add(addFuelBtn);
        panel.add(chargeBatteryBtn);
        panel.add(chargeShipBtn);
        panel.add(energyLabel);
        panel.add(switchHeatingBtn);
        panel.add(temperatureBar);
        panel.add(batteriesChargedLabel);


        connectionToServer = new Thread(() ->
        {
            try
            {
                // Checking whether you're a driver or fueler.
                soc = new Socket(ipAddress, port);
                System.out.println("Connected to server.");
                ObjectOutputStream outputStream = new ObjectOutputStream(soc.getOutputStream());
                outputStream.writeBoolean(false);
                outputStream.flush();


                // Reseting input stream to communicate.
                ObjectInputStream inputStream = new ObjectInputStream(soc.getInputStream());
                while (!soc.isClosed())
                {
                    inData = (ServerToFuelerData) inputStream.readObject();
                    chargeBatteryBtn.setEnabled(inData.chargeBatteryCooldown <= 0 && inData.batteriesCharged < 10);
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

                    setTextOnHeater(inData.isHeaterOn);
                    temperatureBar.setForeground(new Color((int)(inData.temperaturePercent * 255), 0, 0));
                }
                System.out.println("Stopped connection.");
            } catch (Exception e)
            {
                e.printStackTrace();
                window.dispose();
            }
        });
        connectionToServer.start();


        addFuelBtn.addActionListener(e -> SendAction(FuelerAction.addedFuel));
        chargeBatteryBtn.addActionListener(e -> SendAction(FuelerAction.chargedBattery));
        chargeShipBtn.addActionListener(e -> SendAction(FuelerAction.chargedShip));
        switchHeatingBtn.addActionListener(e ->
        {
            SendAction(FuelerAction.switchedHeater);
            setTextOnHeater(!inData.isHeaterOn);
        });



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
