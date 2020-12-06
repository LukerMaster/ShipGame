import Utilities.GameWindow;
import Utilities.SocketData.PointToPointData;
import Utilities.SocketData.ServerToSpectatorData;


public class SpectatorWindow implements GameWindow
{
    FuelerWindow fuelerWindow;
    DriverWindow driverWindow;

    SpectatorWindow()
    {
        fuelerWindow = new FuelerWindow();
        driverWindow = new DriverWindow();
        fuelerWindow.window.setTitle("Spectating: Fueler");
        driverWindow.window.setTitle("Spectating: Driver");
    }

    @Override
    public void close()
    {
        fuelerWindow.window.dispose();
        driverWindow.window.dispose();
    }

    @Override
    public PointToPointData UpdateWindow(PointToPointData data)
    {
        try
        {
            ServerToSpectatorData inData = (ServerToSpectatorData)data;
            fuelerWindow.UpdateWindow(inData.fuelerData);
            driverWindow.UpdateWindow(inData.driverData);
        }
        catch (ClassCastException e)
        {
            System.out.println("Wrong data sent. Ignoring packet.");
        }


        return null;
    }
}
