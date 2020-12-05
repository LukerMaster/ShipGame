package Utilities.SocketData;

import Utilities.FuelerAction;

import java.io.Serializable;

public class FuelerToServerData implements PointToPointData
{
    public FuelerAction action = FuelerAction.noAction;
}
