package Utilities.SocketData;

import java.io.Serializable;

public class DriverToServerData implements Serializable, PointToPointData
{
    public boolean isLeftPressed;
    public boolean isRightPressed;
    public boolean isCoolingPressed;
}
