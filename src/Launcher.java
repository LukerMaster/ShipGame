import javax.swing.*;
import java.awt.*;

public class Launcher
{
    JFrame window;
    JPanel panel;
    JButton roleBtn;
    JButton startServerBtn;
    JButton joinBtn;

    JLabel ipAddressLabel;
    JTextField ipAddress;
    JLabel portLabel;
    JTextField port;


    Thread serverThread;
    Thread driverThread;
    Thread fuelerThread;
    ShipServerMain server;
    ShipDriverMain driver;
    ShipFuelerMain fueler;

    boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public static void main(String[] args)
    {
        Launcher  l = new Launcher();
    }

    Launcher()
    {
        window = new JFrame();
        window.setSize(300, 240);
        window.setLayout(null);
        window.setResizable(false);


        panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds(0, 0, window.getSize().width, window.getSize().height);

        roleBtn = new JButton();
        roleBtn.setText("Role: Pilot");
        roleBtn.setMargin(new Insets(0, 0, 0, 0));
        roleBtn.setBounds(100, 40, 100, 40);

        roleBtn.addActionListener(action ->
        {
            if (roleBtn.getText().equals("Role: Pilot"))
                roleBtn.setText("Role: Fueler");
            else
                roleBtn.setText("Role: Pilot");
        });

        startServerBtn = new JButton();
        startServerBtn.setText("Start Server");
        startServerBtn.setMargin(new Insets(0, 0, 0, 0));
        startServerBtn.setBounds(100, 10, 100, 30);

        startServerBtn.addActionListener(action ->
        {
            if (tryParseInt(port.getText()))
            {
                System.out.println("Bop");
                serverThread = new Thread(() -> server = new ShipServerMain(Integer.parseInt(port.getText())));
                serverThread.start();
            }
        });

        joinBtn = new JButton();
        joinBtn.setText("Join");
        joinBtn.setMargin(new Insets(0, 0, 0, 0));
        joinBtn.setBounds(100, 170, 100, 30);

        joinBtn.addActionListener(action ->
        {
            if (tryParseInt(port.getText()))
            {
                if (roleBtn.getText().equals("Role: Pilot"))
                {
                    driverThread = new Thread(() -> driver = new ShipDriverMain(ipAddress.getText(), Integer.parseInt(port.getText())));
                    driverThread.start();
                }
                else
                {
                    fuelerThread = new Thread(() -> fueler = new ShipFuelerMain(ipAddress.getText(), Integer.parseInt(port.getText())));
                    fuelerThread.start();
                }

            }
        });


        ipAddressLabel = new JLabel();
        ipAddressLabel.setBounds(10, 90, 100, 30);
        ipAddressLabel.setText("IP Address:");
        portLabel = new JLabel();
        portLabel.setBounds(10, 130, 100, 30);
        portLabel.setText("Port:");

        ipAddress = new JTextField();
        ipAddress.setText("127.0.0.1");
        ipAddress.setBounds(100, 90, 100, 30);

        port = new JTextField();
        port.setText("10009");
        port.setBounds(100, 130, 100, 30);

        window.add(panel);
        panel.add(startServerBtn);
        panel.add(roleBtn);
        panel.add(ipAddress);
        panel.add(port);
        panel.add(ipAddressLabel);
        panel.add(portLabel);
        panel.add(joinBtn);
        window.setVisible(true);

        try
        {
            if (fuelerThread != null)
                fuelerThread.join();
            if (driverThread != null)
                driverThread.join();
            if (serverThread != null)
                serverThread.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}
