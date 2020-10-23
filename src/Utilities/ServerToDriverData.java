package Utilities;

import java.io.Serializable;
import java.util.ArrayList;

public class ServerToDriverData implements Serializable
{
    public float energy;
    public float shipPos;
    public float temperatureShip;
    public ArrayList<Meteor> meteors;
}
