import Utilities.*;

import javax.swing.*;
import java.awt.*;
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

    Thread connectionToServer;

    public static void main(String[] args)
    {
        ShipFuelerMain fuelManager = new ShipFuelerMain();
    }

    ShipFuelerMain()
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

        panel.add(addFuelBtn);
        panel.add(chargeBatteryBtn);
        panel.add(chargeShipBtn);
        panel.add(energyLabel);
        panel.add(switchHeatingBtn);
        panel.add(temperatureBar);

        connectionToServer = new Thread(() ->
        {
            try
            {
                // Checking whether you're a driver or fueler.
                Socket soc = new Socket("127.0.0.1", 10009);
                System.out.println("Connected to server.");
                ObjectOutputStream outputStream = new ObjectOutputStream(soc.getOutputStream());
                outputStream.writeBoolean(false);
                outputStream.flush();


                // Reseting input stream to communicate.
                ObjectInputStream inputStream = new ObjectInputStream(soc.getInputStream());
                while (!soc.isClosed())
                {
                    ServerToFuelerData inData = (ServerToFuelerData) inputStream.readObject();
                    chargeBatteryBtn.setEnabled(inData.canChargeBattery);
                    chargeShipBtn.setEnabled(inData.canChargeShip);
                    temperatureBar.setValue((int) (inData.temperaturePercent * 100));
                    energyLabel.setText(inData.EnergyLabel);

                    StringBuilder heaterText = new StringBuilder("Heating: ");
                    if (inData.isHeaterOn) heaterText.append("On");
                    else heaterText.append("Off");

                    switchHeatingBtn.setText(heaterText.toString());



                }
                System.out.println("Stopped connection.");
            } catch (Exception e)
            {
                e.printStackTrace();
                window.dispose();
            }
        });
        connectionToServer.start();
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
