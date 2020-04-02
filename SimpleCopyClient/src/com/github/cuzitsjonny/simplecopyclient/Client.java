package com.github.cuzitsjonny.simplecopyclient;

import java.io.*;
import java.net.Socket;

public class Client
{
    private static final int CHUNK_SIZE = 1024 * 1024;

    private Socket clientSocket;
    private Mode mode;
    private FileInputStream inFromFile;
    private FileOutputStream outToFile;

    public enum Mode
    {
        SEND,
        RECEIVE
    }

    public Client(File file, Mode mode, String address, int port) throws IOException
    {
        file.getParentFile().mkdirs();

        this.clientSocket = new Socket(address, port);
        this.mode = mode;

        if (mode == Mode.SEND)
        {
            inFromFile = new FileInputStream(file);
        }
        else
        {
            outToFile = new FileOutputStream(file);
        }

        System.out.println("Connected.");
        System.out.println("Transferring.");
    }

    public static void main(String[] args)
    {
        try
        {
            Client client = new Client(new File(args[0]), Mode.valueOf(args[1].toUpperCase()), args[2], Integer.parseInt(args[3]));

            client.transfer();
            client.close();
        }
        catch (IOException exc)
        {
            exc.printStackTrace();
        }
    }

    public void transfer() throws IOException
    {
        try
        {
            int read = 0;
            byte[] buffer = new byte[CHUNK_SIZE];

            if (mode == Mode.SEND)
            {
                OutputStream outToServer = clientSocket.getOutputStream();

                while ((read = inFromFile.read(buffer, 0, CHUNK_SIZE)) != -1)
                {
                    outToServer.write(buffer, 0, read);
                }
            }
            else
            {
                InputStream inFromServer = clientSocket.getInputStream();

                while ((read = inFromServer.read(buffer, 0, CHUNK_SIZE)) != -1)
                {
                    outToFile.write(buffer, 0, read);
                }
            }

            System.out.println("Done.");
        }
        catch (IOException exc)
        {
            System.out.println("Timed out.");
        }
    }

    public void close()
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

            clientSocket.close();
        }
        catch (IOException exc)
        {
            exc.printStackTrace();
        }

        System.out.println("Disconnected.");
    }
}
