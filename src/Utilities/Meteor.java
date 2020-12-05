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
        velY = -(r.nextFloat() * 6.0f + 10.0f);
        velX = (r.nextFloat() - 0.5f) * 1.0f;
    }
    public void move(float dt)
    {
        x += velX * dt;
        y += velY * dt;
    }
}
