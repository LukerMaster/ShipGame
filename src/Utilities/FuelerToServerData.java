package Utilities;

import java.io.Serializable;

enum FuelerAction
{
    noAction,
    addedFuel,
    chargedShip,
    chargedBattery,
    switchedHeater
}

public class FuelerToServerData implements Serializable
{
    private FuelerAction action;
    public FuelerAction receiveAction()
    {
        FuelerAction temp = action;
        action = FuelerAction.noAction;
        return temp;
    }
}
