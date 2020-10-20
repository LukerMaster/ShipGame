package Utilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class JPanelWithBG extends JPanel
{
    BufferedImage bgImage;
    public JPanelWithBG(String filepath)
    {
        super();
        try
        {
            bgImage = ImageIO.read(new File(filepath));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    protected void paintComponent(Graphics g)
    {
        g.drawImage(bgImage, 0, 0, null);
    }
}
