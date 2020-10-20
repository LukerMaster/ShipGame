import Utilities.DriverToServerData;
import Utilities.JPanelWithBG;
import Utilities.ServerToDriverData;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ShipDriverMain
{
    JFrame window;
    JPanelWithBG panel;

    JButton steerLeftBtn;
    JButton steerRightBtn;
    JButton turnOnFanBtn;
    JProgressBar energyBar;


    Thread connectionToServer;

    public static void main(String[] args)
    {
        ShipDriverMain shipDriver = new ShipDriverMain();
    }

    ShipDriverMain()
    {
        window = new JFrame("Ship Driver");
        window.setSize(600, 630);
        panel = new JPanelWithBG("ship.png");
        panel.setLayout(null);
        panel.setBounds(0, 0, 600, 600);

        window.add(panel);
        window.setVisible(true);
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

        panel.add(steerLeftBtn);
        panel.add(steerRightBtn);
        panel.add(turnOnFanBtn);
        panel.add(energyBar);


        connectionToServer = new Thread(() ->
        {
            try
            {
                // Checking whether you're a driver or fueler.
                Socket soc = new Socket("127.0.0.1", 10009);
                System.out.println("Connected to server.");
                ObjectOutputStream outputStream = new ObjectOutputStream(soc.getOutputStream());
                System.out.println("Sending data of being driver.");
                outputStream.writeBoolean(true);
                outputStream.flush();

                // Reseting streams to communicate.
                outputStream = new ObjectOutputStream(soc.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(soc.getInputStream());
                while (!soc.isClosed())
                {
                    ServerToDriverData inData = (ServerToDriverData) inputStream.readObject();
                    energyBar.setValue((int)(inData.energy * 100));

                    DriverToServerData outData = new DriverToServerData();
                    outData.isLeftPressed = steerLeftBtn.getModel().isPressed();
                    outData.isRightPressed = steerRightBtn.getModel().isPressed();
                    outData.isCoolingPressed = turnOnFanBtn.getModel().isPressed();
                    outputStream.writeObject(outData);
                }
                System.out.println("Stopped connection.");
            }
            catch (Exception e)
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
