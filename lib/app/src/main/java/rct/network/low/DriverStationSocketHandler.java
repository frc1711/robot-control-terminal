package rct.network.low;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public class DriverStationSocketHandler {
    
    private final SocketHandler socketHandler;
    private final Consumer<ResponseMessage> responseReader;
    private final Consumer<IOException> excHandler;
    
    public DriverStationSocketHandler (
            int teamNum,
            int port,
            Consumer<ResponseMessage> responseReader,
            Consumer<IOException> excHandler) throws IOException {
        
        Socket socket = null;
        try {
            socket = new Socket(getRoborioHost(teamNum), port);
            socketHandler = new SocketHandler(socket, this::receiveMessage, this::handleReceiverIOException);
        } catch (IOException e) {
            if (socket != null) socket.close();
            throw e;
        }
        this.responseReader = responseReader;
        this.excHandler = excHandler;
    }
    
    public static String getRoborioHost (int teamNum) {
        return "roboRIO-"+teamNum+"-frc.local";
    }
    
    private void receiveMessage (Message message) {
        ResponseMessage resMessage = null;
        try {
            resMessage = (ResponseMessage)message;
        } catch (ClassCastException e) {
            String messageClass = message.getClass().getName();
            handleReceiverIOException(new IOException("Expected a ResponseMessage but received a "+messageClass));
        }
        
        responseReader.accept(resMessage);
    }
    
    private void handleReceiverIOException (IOException ioException) {
        // Try to close the socket, but do nothing if there is a problem in attempting to close it
        try {
            if (!socketHandler.isClosed())
                socketHandler.close();
        } catch (Exception e) { }
        
        // Handle the IO exception
        excHandler.accept(ioException);
    }
    
    public void sendInstructionMessage (InstructionMessage message) throws IOException {
        socketHandler.sendMessage(message);
    }
    
    public void close () throws IOException {
        socketHandler.close();
    }
    
}
