package com.yaindustries.fileshare.utilities;

import android.net.Uri;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yaindustries.fileshare.exceptions.NoPortAvailableException;
import com.yaindustries.fileshare.models.FileMetaData;
import com.yaindustries.fileshare.models.TransferUiElements;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ConnectionHelper {
    private static final int START_PORT = 65535;
    private static final int END_PORT = 65434;
    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream socketInput;
    private DataOutputStream socketOutput;

    /**
     * Creates A Server Socket
     *
     * @return Port Address for the Server Socket
     * @throws NoPortAvailableException when no port is available
     */
    public int createServer() throws NoPortAvailableException {
        boolean serverCreated = false;
        for (int i = START_PORT; i > END_PORT && !serverCreated; i--) {
            try {
                serverSocket = new ServerSocket(i);
                serverCreated = true;
            } catch (IOException ignored) {
            }
        }
        if (serverSocket == null)
            throw new NoPortAvailableException();
        return serverSocket.getLocalPort();
    }

    /**
     * Creates a client Socket
     *
     * @throws IOException
     */
    public void createClientSocket() throws IOException {
        socket = new Socket();
        socket.bind(null);
    }

    /**
     * Connects to a Server Socket
     *
     * @param address InetAddress of the Server
     * @throws IOException
     */
    public void connectToServerSocket(InetAddress address) throws NoPortAvailableException {
        boolean connectedToServer = false;

        for (int i = START_PORT; i > END_PORT && !connectedToServer; i--) {
            try {
                var server = new InetSocketAddress(address.getHostName(), i);
                socket.connect(server, 100);
                connectedToServer = true;
                initializeSocket();
            } catch (IOException ignored) {
            }
        }

        if (!socket.isConnected())
            throw new NoPortAvailableException();
    }

    /**
     * Listens for Connection on Server Socket.
     *
     * @throws IOException
     */
    public void listenForConnection() throws IOException {
        socket = serverSocket.accept();
        initializeSocket();
    }

    /**
     * Reads Data from the Socket and Parses the FileMetaData and pushes data
     * to the FileDirectories
     *
     * @param uiElements  UI elements which will be updated on progress change
     * @param detailsView UI Element where the filename is to be shown
     * @throws IOException
     */
    public void readDataFromSocket(TransferUiElements uiElements, TextView detailsView, List<FileMetaData> fileMetaDataList) throws IOException {
        var metaData = Reader.readFileMetaDataFromNetworkStream(socketInput);
        uiElements.activity.runOnUiThread(() -> detailsView.setText("Now Receiving : " + metaData.name));
        File f = new File(Utils.getDataDirectoryPath() + metaData.name);
        f.getParentFile().mkdirs();
        f.createNewFile();
        var fileOutput = new DataOutputStream(new FileOutputStream(f));
        Reader.readBlockFromNetworkStream(socketInput, fileOutput, uiElements);
        fileOutput.close();
        if(fileMetaDataList != null)
            fileMetaDataList.add(metaData);
    }

    public void readDataFromSocket(TransferUiElements uiElements, TextView detailsView) throws IOException {
        readDataFromSocket(uiElements, detailsView, null);
    }

    /**
     * Reads Data from the File Directories and Parses the FileMetaData
     * and pushes data to the Socket
     *
     * @param uiElements  UI elements which will be updated on progress change
     * @throws IOException
     */
    public void pushDataToSocket(TransferUiElements uiElements, Uri fileUri) throws IOException {
        var activity = uiElements.activity;
        var inputStream = new DataInputStream(activity.getContentResolver().openInputStream(fileUri));
        var metaData = Utils.getFileMetaDataFromUri(activity, fileUri);
        Reader.writeFileMetaDataToNetworkStream(metaData, socketOutput);
        Reader.writeBlockToNetworkStream(inputStream, socketOutput, metaData.size, uiElements);
        inputStream.close();
    }

    /**
     * Closes and Cleans up the socket connections
     *
     * @throws IOException
     */
    public void cleanup() throws IOException {
        if (socketInput != null)
            socketInput.close();
        if (socketOutput != null)
            socketOutput.close();
        if (socket != null && socket.isConnected())
            socket.close();
        if (serverSocket != null && !serverSocket.isClosed())
            serverSocket.close();
    }


    private void initializeSocket() throws IOException {
        if (socket == null)
            return;
        socketInput = new DataInputStream(socket.getInputStream());
        socketOutput = new DataOutputStream(socket.getOutputStream());
    }
}