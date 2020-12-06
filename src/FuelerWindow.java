import Utilities.*;
import Utilities.SocketData.FuelerToServerData;
import Utilities.SocketData.PointToPointData;
import Utilities.SocketData.ServerToFuelerData;
import com.google.gson.Gson;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class FuelerWindow implements GameWindow
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
    JLabel gameTimeLabel;
    JLabel gameOverText;

    volatile LinkedList<FuelerAction> pendingActions = new LinkedList<FuelerAction>();


    public void setTextOnHeater(boolean isOn)
    {
        StringBuilder heaterText = new StringBuilder("Heating: ");
        if (isOn) heaterText.append("On");
        else heaterText.append("Off");
        switchHeatingBtn.setText(heaterText.toString());
    }

    @Override
    public void close()
    {
        window.dispose();
    }

    @Override
    public PointToPointData UpdateWindow(PointToPointData inData)
    {
        try
        {
            ServerToFuelerData data = (ServerToFuelerData)inData;
            chargeBatteryBtn.setEnabled(data.chargeBatteryCooldown <= 0 && data.batteriesCharged < 10 && data.isEnoughToCharge);
            chargeShipBtn.setEnabled(data.chargeShipCooldown <= 0 && data.batteriesCharged > 0);

            if (data.chargeBatteryCooldown > 0.0f && data.batteriesCharged < 10)
                chargeBatteryBtn.setText("Cooldown: " + String.format("%.1f", data.chargeBatteryCooldown) + "s");
            else if (data.batteriesCharged == 10)
                chargeBatteryBtn.setText("All charged");
            else if (!data.isEnoughToCharge)
                chargeBatteryBtn.setText("No energy");
            else
                chargeBatteryBtn.setText("Charge battery");

            if (data.chargeShipCooldown > 0.0f)
                chargeShipBtn.setText("Cooldown: " + String.format("%.1f", data.chargeShipCooldown) + "s");
            else if (data.batteriesCharged == 0)
                chargeShipBtn.setText("No batteries");
            else
                chargeShipBtn.setText("Charge ship");

            batteriesChargedLabel.setText("Charged: " + data.batteriesCharged);


            temperatureBar.setValue((int) (data.temperaturePercent * 100));
            energyLabel.setText(data.energyLabel);
            gameTimeLabel.setText("Time: " + String.format("%.1f", data.gameTimePassed) + "s");

            setTextOnHeater(data.isHeaterOn);
            temperatureBar.setForeground(new Color(Math.min(255, Math.max(0, (int)(data.temperaturePercent * 255))), 0, 0));

            if (data.isGameOver)
            {
                gameOverText.setText(data.gameOverReason);
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

            return outData;
        }
        catch (ClassCastException e)
        {
            System.out.println("Wrong data sent. Ignoring packet.");
        }
        return null;
    }


    FuelerWindow()
    {
        window = new JFrame("Fuel Manager");
        window.setSize(600, 630);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

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
        panel.setVisible(true);
        window.setVisible(true);

        addFuelBtn.addActionListener(e -> pendingActions.add(FuelerAction.addedFuel));
        chargeBatteryBtn.addActionListener(e -> pendingActions.add(FuelerAction.chargedBattery));
        chargeShipBtn.addActionListener(e -> pendingActions.add(FuelerAction.chargedShip));
        switchHeatingBtn.addActionListener(e -> pendingActions.add(FuelerAction.switchedHeater));
    }
}
