package com.github.cuzitsjonny.simplecopyserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;

public class Server
{
    private static final int CHUNK_SIZE = 1024 * 1024;

    private ServerSocket serverSocket;
    private Mode mode;
    private Socket client;
    private long connectedTimestamp;
    private long bytesTransferred;
    private FileInputStream inFromFile;
    private FileOutputStream outToFile;
    private long lastStatsTimestamp;
    private DecimalFormat statsDecFormat;

    public enum Mode
    {
        SEND,
        RECEIVE
    }

    public Server(File file, Mode mode, int port) throws IOException
    {
        file.getParentFile().mkdirs();

        this.serverSocket = new ServerSocket(port);
        this.mode = mode;
        this.lastStatsTimestamp = System.currentTimeMillis();
        this.statsDecFormat = new DecimalFormat("0.00");

        if (mode == Mode.SEND)
        {
            inFromFile = new FileInputStream(file);
        }
        else
        {
            outToFile = new FileOutputStream(file);
        }
    }

    public static void main(String[] args)
    {
        try
        {
            Server server = new Server(new File(args[0]), Mode.valueOf(args[1].toUpperCase()), Integer.parseInt(args[2]));

            server.waitThenTransfer();
            server.close();
        }
        catch (IOException exc)
        {
            exc.printStackTrace();
        }
    }

    public void waitThenTransfer()
    {
        System.out.println("Waiting for client to connect.");
        acceptClient();

        System.out.println("Serving client.");
        serveClient();
    }

    private void close()
    {
        try
        {
            if (outToFile != null)
            {
                outToFile.close();
            }

            if (inFromFile != null)
            {
                inFromFile.close();
            }

            serverSocket.close();
        }
        catch (IOException exc)
        {
            exc.printStackTrace();
        }
    }

    private void acceptClient()
    {
        try
        {
            client = serverSocket.accept();

            client.setSoTimeout(3000);

            connectedTimestamp = System.currentTimeMillis();
            bytesTransferred = 0;

            System.out.println("Client connected from " + client.getInetAddress().getHostAddress() + ":" + client.getPort() + ".");
        }
        catch (IOException exc)
        {
            exc.printStackTrace();
        }
    }

    private void serveClient()
    {
        try
        {
            int read = 0;
            byte[] buffer = new byte[CHUNK_SIZE];

            if (mode == Mode.SEND)
            {
                OutputStream outToClient = client.getOutputStream();

                while ((read = inFromFile.read(buffer, 0, CHUNK_SIZE)) != -1)
                {
                    outToClient.write(buffer, 0, read);
                    bytesTransferred += read;

                    printStats();
                }
            }
            else
            {
                InputStream inFromClient = client.getInputStream();

                while ((read = inFromClient.read(buffer, 0, CHUNK_SIZE)) != -1)
                {
                    outToFile.write(buffer, 0, read);
                    bytesTransferred += read;

                    printStats();
                }
            }

            System.out.println("Client disconnected.");
        }
        catch (IOException exc)
        {
            System.out.println("Client timed out.");
        }
    }

    private void printStats()
    {
        long time = System.currentTimeMillis();

        if (time >= lastStatsTimestamp + 1000)
        {
            long millisecondsPassed = time - connectedTimestamp;
            double secondsPassed = (double) millisecondsPassed / 1000.0;
            double bytesPerSecond = (double) bytesTransferred / secondsPassed;
            double kBytesPerSecond = bytesPerSecond / 1000.0;
            double mBytesPerSecond = kBytesPerSecond / 1000.0;

            System.out.println("Speed: " + statsDecFormat.format(mBytesPerSecond) + " MB/s");

            lastStatsTimestamp = time;
        }
    }
}
