import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Launcher
{
    final String driverTooltip = "As a pilot, avoid hitting meteors, communicate with your fueler " +
                                "to make sure you don't freeze (-10°C) nor melt (50°C) " +
                                "and help him sustain furnace temperature by using emergency cooling.";
    final String fuelerTooltip = "As a fueler, sustain furnace temperature by adding fuel and opening heating valves. " +
                                "By keeping temperature high, energy generation stays high. " +
                                "If temperature gets too low or too high - game over. " +
                                "You can also use batteries to keep backup charge in case pilot needs it for later.";

    boolean applicationRuns = true;

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
        window = new JFrame("AWSJ - Launcher");
        window.setSize(300, 240);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent we)
            {
                applicationRuns = false;
                super.windowClosing(we);
            }
        });
        window.setLayout(null);
        window.setResizable(false);


        panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds(0, 0, window.getSize().width, window.getSize().height);

        roleBtn = new JButton();
        roleBtn.setText("Role: Pilot");
        roleBtn.setToolTipText(driverTooltip);
        roleBtn.setMargin(new Insets(0, 0, 0, 0));
        roleBtn.setBounds(100, 40, 100, 40);

        roleBtn.addActionListener(action ->
        {
            if (roleBtn.getText().equals("Role: Pilot"))
            {
                roleBtn.setText("Role: Fueler");
                roleBtn.setToolTipText(fuelerTooltip);
            }
            else
            {
                roleBtn.setText("Role: Pilot");
                roleBtn.setToolTipText(driverTooltip);
            }

        });

        startServerBtn = new JButton();
        startServerBtn.setText("Start Server");
        startServerBtn.setMargin(new Insets(0, 0, 0, 0));
        startServerBtn.setBounds(100, 10, 100, 30);

        startServerBtn.addActionListener(action ->
        {
            if (tryParseInt(port.getText()))
            {
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
