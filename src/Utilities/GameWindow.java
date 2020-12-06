package Utilities;

import Utilities.SocketData.PointToPointData;

public interface GameWindow
{
    PointToPointData UpdateWindow(PointToPointData data);
    void close();
}
