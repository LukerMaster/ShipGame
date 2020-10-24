import Utilities.*;
import com.google.gson.Gson;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

public class ShipDriverMain
{
    boolean applicationRuns = true;

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

    volatile Socket soc;
    Thread connectionToServer;

    public static void main(String[] args)
    {
        ShipDriverMain shipDriver = new ShipDriverMain(args[0], Integer.parseInt(args[1]));
    }

    ShipDriverMain(String ipAddress, int port)
    {
        window = new JFrame("Ship Driver");
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
        panel = new JPanelWithBG("assets/ship.png");
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
        window.setVisible(true);

        meteorSprites = new JPanelWithBG[10];
        for (int i = 0; i < meteorSprites.length; i++)
        {
            meteorSprites[i] = new JPanelWithBG("assets/meteor.png");
            panel.add(meteorSprites[i]);
        }

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
                    currentOut.writeInt(1);
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
            if (connected)
                System.out.println("Connected.");


            if (applicationRuns)
            {
                try
                {
                    while (applicationRuns)
                    {
                        // Input
                        Gson gson = new Gson();
                        ServerToDriverData inData = gson.fromJson((String)inputStream.readObject(), ServerToDriverData.class);
                        energyBar.setValue((int)(inData.energy * 100));
                        temperatureLabel.setText(String.format("%.1f", inData.temperatureShip)  + "°C");
                        gameTimeLabel.setText("Time: " + String.format("%.1f", inData.gameTimePassed) + "s");
                        for (int i = 0; i < meteorSprites.length; i++)
                        {
                            if (inData.meteors.size() > i && inData.meteors.get(i) != null)
                            {
                                meteorSprites[i].setVisible(true);
                                meteorSprites[i].setBounds(((int)((inData.meteors.get(i).x - inData.shipPos + 5) * 60) - 100), (int)(-3.5f * inData.meteors.get(i).y + 150.0f), 200, 200);
                            }
                            else
                                meteorSprites[i].setVisible(false);
                        }
                        if (inData.isGameOver)
                        {
                            gameOverText.setText(inData.gameOverReason);
                        }
                        else
                        {
                            gameOverText.setText("");
                        }


                        // Output
                        DriverToServerData outData = new DriverToServerData();
                        outData.isLeftPressed = steerLeftBtn.getModel().isPressed();
                        outData.isRightPressed = steerRightBtn.getModel().isPressed();
                        outData.isCoolingPressed = turnOnFanBtn.getModel().isPressed();


                        outputStream.writeObject(gson.toJson(outData));
                        outputStream.reset();
                    }
                    soc.close();
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
