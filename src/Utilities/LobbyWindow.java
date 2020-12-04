package Utilities;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

public class LobbyWindow
{
    boolean isWindowOpened = true;

    public boolean isWindowOpened()
    {
        return isWindowOpened;
    }

    final int MAX_PLAYERS = 8;
    public int getMaxPlayerSlots()
    {
        return MAX_PLAYERS;
    }

    public JButton setReadyBtn = new JButton();
    JFrame window = new JFrame("Lobby");
    JLabel statusLabel = new JLabel();
    Vector<JLabel> playerLabels = new Vector<>();
    JLabel morePlayersLabel = new JLabel();
    Vector<JCheckBox> readyCheckboxes = new Vector<>();

    public String getRoleString(ERole role)
    {
        switch (role)
        {
            case driver:
                return "Driver";
            case fueler:
                return "Fueler";
            case spectator:
                return "Spectator";
        }
        return "BROKEN!!!11";
    }

    public void setNicknames(int[] IDs, String[] nicknames, String[] roles, boolean[] ready)
    {
        for (int i = 0; i < MAX_PLAYERS; i++)
        {
            if (i < nicknames.length)
            {
                playerLabels.get(i).setText((i + 1) + ". " + "ID: [" + IDs[i] + "] " + nicknames[i] + " - " + roles[i]);
                readyCheckboxes.get(i).setSelected(ready[i]);
                readyCheckboxes.get(i).setVisible(true);
            }
            else
            {
                playerLabels.get(i).setText("");
                readyCheckboxes.get(i).setVisible(false);
            }
        }
        if (IDs.length > MAX_PLAYERS)
        {
            morePlayersLabel.setText("And " + (IDs.length - MAX_PLAYERS) + " more players. Total: " + IDs.length);
        }

    }

    public void printToWindow(String text)
    {
        statusLabel.setText(text);
    }


    public LobbyWindow()
    {
        window.setSize(600, 300);
        window.setResizable(false);
        window.setLayout(null);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent we)
            {
                isWindowOpened = false;
                super.windowClosing(we);
            }
        });
        statusLabel.setText("Idling aggresively.");
        statusLabel.setBounds(20, 10, 400, 30);


        for (int i = 0; i < MAX_PLAYERS; i++)
        {
            playerLabels.add(new JLabel());
            playerLabels.get(i).setBounds(20, 40+20*i, 400, 20);
            playerLabels.get(i).setText("");
            window.add(playerLabels.get(i));
        }
        for (int i = 0; i < MAX_PLAYERS; i++)
        {
            readyCheckboxes.add(new JCheckBox());
            readyCheckboxes.get(i).setBounds(0, 40+20*i,20, 20);
            readyCheckboxes.get(i).setEnabled(false);
            readyCheckboxes.get(i).setVisible(false);
            window.add(readyCheckboxes.get(i));
        }
        morePlayersLabel.setBounds(20, 200, 400, 20);
        morePlayersLabel.setText("And 0 more players.");

        setReadyBtn.setBounds(20, 240, 130, 30);
        setReadyBtn.setText("Ready");


        window.add(morePlayersLabel);
        window.add(statusLabel);
        window.add(setReadyBtn);
        window.setVisible(true);
    }
}
