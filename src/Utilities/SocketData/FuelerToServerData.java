package Utilities.SocketData;

import Utilities.FuelerAction;

import java.io.Serializable;

public class FuelerToServerData implements Serializable, PointToPointData
{
    public FuelerAction action = FuelerAction.noAction;
}
