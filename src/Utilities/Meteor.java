package Utilities;

import java.io.Serializable;
import java.util.Random;

public class Meteor implements Serializable
{
    public float x, y;
    public float velX, velY;
    public Meteor(float shipPos, float maxDistance, boolean moveHorizontal)
    {
        Random r = new Random();
        x = (r.nextFloat() - 0.5f) * maxDistance * 2 + shipPos;
        y = 100.0f;
        velY = -(r.nextFloat() * 0.02f + 0.05f);
        velX = (r.nextFloat() - 0.5f) * 0.005f;
    }
}
