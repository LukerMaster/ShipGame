package Utilities;

import javax.swing.*;
import java.awt.*;

public class ServerWindow extends LobbyWindow
{
    public JButton startGameBtn = new JButton();
    public JButton swapRolesBtn = new JButton();

    public JTextField idTextBox = new JTextField();
    public JButton setFuelerBtn = new JButton();
    public JButton setDriverBtn = new JButton();

    public void printToConsoleAndWindow(String text)
    {
        System.out.println(text);
        printToWindow(text);
    }

    public ServerWindow()
    {
        super();
        setReadyBtn.setVisible(false);
        setReadyBtn.setEnabled(false);
        window.setTitle("Absolutely Worst Space Journey - Server");
        startGameBtn.setBounds(480, 240, 100, 30);
        startGameBtn.setText("Start Game!");
        startGameBtn.setEnabled(false);

        swapRolesBtn.setBounds(480, 20, 100, 30);
        swapRolesBtn.setText("Swap roles");

        setFuelerBtn.setBounds(420, 150, 80, 30);
        setFuelerBtn.setText("Set fueler");
        setFuelerBtn.setMargin(new Insets(0, 0, 0, 0));
        setDriverBtn.setBounds(500, 150, 80, 30);
        setDriverBtn.setText("Set driver");
        setDriverBtn.setMargin(new Insets(0, 0, 0, 0));

        idTextBox.setBounds(420, 130, 160, 20);
        idTextBox.setText("0");

        window.add(startGameBtn);
        window.add(swapRolesBtn);
        window.add(setFuelerBtn);
        window.add(setDriverBtn);
        window.add(idTextBox);

        window.setVisible(true);
    }
}
