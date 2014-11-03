/**
 * 
 */
package net.tfobz.tele.eggale.ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import net.tfobz.tele.eggale.ftp.state.Mode;
import net.tfobz.tele.eggale.ftp.state.Type;
import sun.net.ConnectionResetException;

/**
 * @author alex
 * 
 */
public class DataHandler {

    /**
     * Whether transfers should happen in passive mode.
     */
    private boolean isPassiveMode = false;

    /**
     * The data socket of the client.
     */
    private Socket client = null;

    /**
     * The transmission mode.
     */
    private Mode mode = Mode.STREAM;

    /**
     * The type of data being transmitted.
     */
    private Type type = Type.ASCII;

    /**
     * The current working directory of the client.
     */
    private String workingDirectory = Server.DEFAULT_DIR;

    /**
     * The Communication Handler this Data Handler uses.
     */
    private ProtocolInterpreter comHandler;

    public DataHandler(ProtocolInterpreter comHandler) {
        this.comHandler = comHandler;
    }

    public void openConnection(int port) throws IOException,
                    IllegalStateException {
        openConnection(port, "");
    }

    public void openConnection(int port, String host) throws IOException,
                    IllegalStateException {
        if (isOpen() == false) {
            if (isPassiveMode == true) {
                ServerSocket server = new ServerSocket(port);
                client = server.accept();
                server.close();
            } else {
                client = new Socket(host, port);
            }
        } else {
            throw new IllegalStateException("Connection already opened.");
        }
    }

    public boolean isOpen() {
        return (client != null);
    }

    public void store(File file) throws IOException {
        if (file != null) {
            if (isOpen() == true) {
                comHandler.reply(Reply.FILE_ACTION_OK);

                FileOutputStream output = new FileOutputStream(file);
                switch (mode) {
                case STREAM:
                    switch (type) {
                    case ASCII: {
                        BufferedReader input = new BufferedReader(
                                        new InputStreamReader(client
                                                        .getInputStream()));

                        // TODO: Direct writing is slow. Replace with some kind
                        // of buffering.
                        try {
                            int data = input.read();
                            while (data != -1) {
                                output.write(data);
                                data = input.read();
                            }
                            comHandler.reply(Reply.FILE_ACTION_COMPLETE);
                        } catch (ConnectionResetException e) {
                            // Do nothing. Probably means EOF.
                            client = null;
                        }

                        input.close();
                        break;
                    }
                    case IMAGE: {
                        BufferedInputStream input = new BufferedInputStream(
                                        client.getInputStream());
                        ByteArrayOutputStream bOutput = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int i = 0;
                        while ((i = input.read(buffer)) != -1) {
                            bOutput.write(buffer, 0, i);
                        }
                        comHandler.reply(Reply.FILE_ACTION_COMPLETE);
                        byte result[] = bOutput.toByteArray();

                        output.write(result);
                        input.close();
                        break;
                    }
                    }
                    break;

                }

                output.close();
            }
        }
    }

    public void retrieve(File file) throws IOException {
        if (file != null) {
            if (isOpen() == true) {
                comHandler.reply(Reply.FILE_ACTION_OK);

                FileInputStream input = new FileInputStream(file);
                switch (mode) {
                case STREAM:
                    switch (type) {
                    case ASCII: {
                        PrintWriter output = new PrintWriter(
                                        client.getOutputStream());

                        int data = input.read();
                        while (data != -1) {
                            output.print((char) data);
                            data = input.read();
                        }

                        output.close();
                        break;
                    }
                    case IMAGE: {
                        BufferedOutputStream output = new BufferedOutputStream(
                                        client.getOutputStream());

                        int data = input.read();
                        while (data != -1) {
                            output.write(data);
                            data = input.read();
                        }

                        output.close();
                        break;
                    }
                    }
                    break;
                }

                comHandler.reply(Reply.FILE_ACTION_COMPLETE);
                input.close();
            }
        }
    }

    public void listFiles(File folder) throws IOException {
        if (folder != null) {
            if (isOpen() == true) {
                BufferedReader input = null;
                PrintWriter output = new PrintWriter(client.getOutputStream());

                comHandler.reply(Reply.FILE_ACTION_OK);

                // CAUTION: Runtime.exec() isch dr teifl.
                Process p = Runtime.getRuntime().exec(
                                "/bin/ls -ls1 " + folder.getAbsolutePath());
                System.out.println(folder.getAbsolutePath());
                try {
                    p.waitFor();

                    input = new BufferedReader(new InputStreamReader(
                                    p.getInputStream()));

                    while (input.ready() == false)
                        ;

                    // Skip first line. ls outputs total number of files here.
                    input.readLine();
                    String data = input.readLine();
                    while (data != null) {
                        // Remove leading whitespace cause it breaks the output.
                        data = data.trim();
                        // Remove first part of data. I don't currently know
                        // what it does and it breaks the output.
                        data = data.substring(data.indexOf(' '));
                        output.write(data + "\r\n");
                        data = input.readLine();
                    }
                    input.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (input != null) {
                        input.close();
                    }
                }

                output.close();
                comHandler.reply(Reply.FILE_ACTION_COMPLETE);
            }
        }
    }

    public void changeDirectory(File folder) {
        if (folder != null) {
            if (folder.exists() == true && folder.isDirectory() == true) {
                workingDirectory = folder.getAbsolutePath();
                comHandler.reply(Reply.COMMAND_OK);
            } else {
                comHandler.reply(Reply.FILE_NOT_FOUND);
            }
        }
    }

    public void setPassiveMode(boolean flag) {
        isPassiveMode = flag;

        if (flag == true) {
            comHandler.reply(Reply.PASSIVE_MODE);
        } else {
            // CAUTION: Is this the correct reply?
            comHandler.reply(Reply.COMMAND_OK);
        }
    }

    public void closeConnection() throws IOException {
        if (isOpen() == true) {
            client.close();
            client = null;
        }
    }

    /**
     * @return The Mode of transmission.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * @param mode
     *            The Mode to set.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * @return The Type of transmission.
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type
     *            The Type to set.
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return The working directory of the client.
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }
}
