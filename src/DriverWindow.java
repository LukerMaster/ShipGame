import Utilities.*;
import Utilities.SocketData.DriverToServerData;
import Utilities.SocketData.PointToPointData;
import Utilities.SocketData.ServerToDriverData;

import javax.swing.*;
import java.awt.*;

public class DriverWindow implements GameWindow
{
    JFrame window;
    JPanelWithBG panel;

    JButton steerLeftBtn;
    JButton steerRightBtn;
    JButton turnOnFanBtn;
    JProgressBar energyBar;

    JLabel temperatureLabel;
    JLabel gameTimeLabel;
    JLabel gameOverText;

    JPanelWithBG[] meteorSprites;

    @Override
    public void close()
    {
        window.dispose();
    }

    @Override
    public PointToPointData UpdateWindow(PointToPointData inData)
    {
        ServerToDriverData data;
        try
        {
            data = (ServerToDriverData)inData;
            energyBar.setValue((int) (data.energy * 100));
            temperatureLabel.setText(String.format("%.1f", data.temperatureShip) + "°C");
            gameTimeLabel.setText("Time: " + String.format("%.1f", data.gameTimePassed) + "s");
            for (int i = 0; i < meteorSprites.length; i++)
            {
                if (data.meteors.size() > i && data.meteors.get(i) != null)
                {
                    meteorSprites[i].setVisible(true);
                    meteorSprites[i].setBounds(((int) ((data.meteors.get(i).x - data.shipPos + 5) * 60) - 100), (int) (-3.5f * data.meteors.get(i).y + 150.0f), 200, 200);
                } else
                    meteorSprites[i].setVisible(false);
            }
            if (data.isGameOver)
            {
                gameOverText.setText(data.gameOverReason);
            } else
            {
                gameOverText.setText("");
            }
            // Output
            DriverToServerData outData = new DriverToServerData();
            outData.isLeftPressed = steerLeftBtn.getModel().isPressed();
            outData.isRightPressed = steerRightBtn.getModel().isPressed();
            outData.isCoolingPressed = turnOnFanBtn.getModel().isPressed();

            return outData;
        }
        catch (ClassCastException e)
        {
            System.out.println("Wrong data sent. Ignoring packet.");
        }
        return null;
    }

    DriverWindow()
    {
        window = new JFrame("Ship Driver");
        window.setSize(600, 630);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        panel = new JPanelWithBG("/ship.png");
        panel.setLayout(null);
        panel.setBounds(0, 0, 600, 600);

        window.add(panel);
        window.setResizable(false);

        steerLeftBtn = new JButton();
        steerLeftBtn.setBounds(120, 470, 50, 50);
        steerLeftBtn.setText("Go Left");
        steerLeftBtn.setMargin(new Insets(0, 0,0 ,0));
        steerLeftBtn.setBorder(null);

        steerRightBtn = new JButton();
        steerRightBtn.setBounds(375, 470, 50, 50);
        steerRightBtn.setText("Go Right");
        steerRightBtn.setMargin(new Insets(0, 0,0 ,0));
        steerRightBtn.setBorder(null);

        turnOnFanBtn = new JButton();
        turnOnFanBtn.setBounds(470, 490, 110, 60);
        turnOnFanBtn.setText("Emergency Cooling");
        turnOnFanBtn.setMargin(new Insets(0, 0,0 ,0));
        turnOnFanBtn.setBorder(null);

        energyBar = new JProgressBar();
        energyBar.setBounds(200, 410, 200, 15);

        temperatureLabel = new JLabel();
        temperatureLabel.setText("No info.");
        temperatureLabel.setBounds(10, 520, 100, 30);
        temperatureLabel.setToolTipText("Temperature of the ship. Don't go below -10 or above 50.");

        gameTimeLabel = new JLabel();
        gameTimeLabel.setText("Not connected.");
        gameTimeLabel.setBounds(222, 505, 100, 30);

        gameOverText = new JLabel();
        gameOverText.setText("CONNECTING...");
        gameOverText.setBounds(10, 10, 600, 50);
        gameOverText.setFont(new Font("tahoma", Font.PLAIN, 20));

        panel.add(steerLeftBtn);
        panel.add(steerRightBtn);
        panel.add(turnOnFanBtn);
        panel.add(energyBar);
        panel.add(gameTimeLabel);
        panel.add(temperatureLabel);
        panel.add(gameOverText);
        panel.setVisible(true);
        window.setVisible(true);

        meteorSprites = new JPanelWithBG[10];
        for (int i = 0; i < meteorSprites.length; i++)
        {
            meteorSprites[i] = new JPanelWithBG("/meteor.png");
            panel.add(meteorSprites[i]);
        }
    }
}
