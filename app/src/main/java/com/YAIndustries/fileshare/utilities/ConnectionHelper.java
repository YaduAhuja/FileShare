package com.YAIndustries.fileshare.utilities;

import android.net.Uri;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionHelper {
    private ServerSocket serverSocket;
    private Socket socket;

    /**
     * Creates A Server Socket
     *
     * @return Port Address for the Server Socket
     * @throws IOException
     */
    public int createServer() throws IOException {
        serverSocket = new ServerSocket(0);
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
     * @param port    Port for the Connection
     * @throws IOException
     */
    public void connectToServerSocket(InetAddress address, int port) throws IOException {
        var server = new InetSocketAddress(address.getHostName(), port);
        socket.connect(server, 500);
    }

    /**
     * Listens for Connection on Server Socket.
     *
     * @throws IOException
     */
    public void listenForConnection() throws IOException {
        socket = serverSocket.accept();
    }

    /**
     * Reads Data from the Socket and Parses the FileMetaData and pushes data
     * to the FileDirectories
     *
     * @param activity    Activity which is invoking this method
     * @param progressBar Progress Bar Reference
     * @throws IOException
     */
    public void readDataFromSocket(AppCompatActivity activity, ProgressBar progressBar) throws IOException {
        var inputStream = socket.getInputStream();
        var metaData = Utils.getFileMetaDataFromStream(inputStream);
        assert metaData != null;
        File f = new File(Utils.getDataDirectoryPath() + metaData.name);
        var outputStream = new FileOutputStream(f);
        Utils.copyFile(activity, inputStream, outputStream, progressBar, metaData.size);
    }

    /**
     * Reads Data from the File Directories and Parses the FileMetaData
     * and pushes data to the Socket
     *
     * @param activity    Activity which is invoking this method
     * @param progressBar Progress Bar Reference
     * @throws IOException
     */
    public void pushDataToSocket(AppCompatActivity activity, ProgressBar progressBar, Uri fileUri) throws IOException {
        var inputStream = activity.getContentResolver().openInputStream(fileUri);
        var metaData = Utils.getFileMetaDataFromUri(activity, fileUri);
        var outputStream = socket.getOutputStream();
        var json = new ObjectMapper().writeValueAsString(metaData);
        outputStream.write(json.getBytes());
        outputStream.write(0);
        Utils.copyFile(activity, inputStream, outputStream, progressBar, metaData.size);
    }

    /**
     * Closes and Cleans up the socket connections
     *
     * @throws IOException
     */
    public void cleanup() throws IOException {
        if (socket != null && socket.isConnected())
            socket.close();
        if (serverSocket != null && !serverSocket.isClosed())
            serverSocket.close();
    }
}
