/**
 * 
 */
package net.tfobz.tele.eggale.ftp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
     * A Socket which is used for the active transfer.
     */
    private Socket activeConnection = null;

    /**
     * A ServerSocket which is used for the passive transfer.
     */
    private ServerSocket passiveConnection = null;

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
                passiveConnection = new ServerSocket(port);
            } else {
                activeConnection = new Socket(host, port);
            }
        } else {
            throw new IllegalStateException("Connection already opened.");
        }
    }

    public void store(File file) throws IOException {
        Socket client = null;
        if (file != null) {
            client = getClient();
            if (client != null) {
                comHandler.reply(150);

                FileOutputStream output = new FileOutputStream(file);
                switch (mode) {
                case STREAM:
                    switch (type) {
                    case ASCII: {
                        BufferedReader input = new BufferedReader(
                                        new InputStreamReader(client
                                                        .getInputStream()));

                        // FIX: Direct writing is slow. Replace with some kind
                        // of buffering.
                        try {
                            int data = input.read();
                            while (data != -1) {
                                output.write(data);
                                data = input.read();
                            }
                        } catch (ConnectionResetException e) {
                            // Do nothing. Probably means EOF.
                        }

                        input.close();
                        break;
                    }
                    case IMAGE: {
                        // try {
                        // BufferedImage image = ImageIO.read(client
                        // .getInputStream());
                        // ImageIO.write(image, "JPG", output);
                        // } catch (IOException e) {
                        // comHandler.reply(451);
                        // }

                        BufferedInputStream input = new BufferedInputStream(
                                        client.getInputStream());

                        int b = input.read();
                        while (b != -1) {
                            output.write(b);
                            b = input.read();
                        }
                        input.close();
                        break;
                    }
                    }
                    break;

                }

                comHandler.reply(226);
                output.close();
                client.close();
            }
        }
    }

    public void retrieve(File file) throws IOException {
        Socket client = null;
        if (file != null) {
            client = getClient();
            if (client != null) {
                comHandler.reply(150);

                FileInputStream input = new FileInputStream(file);
                switch (mode) {
                case STREAM:
                    switch (type) {
                    case ASCII:
                        PrintWriter output = new PrintWriter(
                                        client.getOutputStream());

                        int data = input.read();
                        while (data != -1) {
                            output.print((char) data);
                            data = input.read();
                        }
                        output.close();
                        break;
                    case IMAGE:
                        OutputStreamWriter out = new OutputStreamWriter(
                                        client.getOutputStream(), "UTF-8");
                        byte[] b = new byte[(int) file.length()];

                        input.read(b);
                        for (int i = 0; i < file.length(); i++) {
                            System.out.println((char) b[i]);
                            out.write(b[i]);
                        }
                        out.close();
                        break;
                    }
                    break;
                }

                comHandler.reply(226);
                input.close();
                client.close();
            }
        }
    }

    public void listFiles(File folder) throws IOException {
        Socket client = null;
        if (folder != null) {
            client = getClient();
            if (client != null) {
                PrintWriter output = new PrintWriter(client.getOutputStream());

                comHandler.reply(150);
                for (File file : folder.listFiles()) {
                    output.print(file.getName() + "\r\n");
                }
                output.close();
                comHandler.reply(226);
                client.close();
            }
        }
    }

    public void changeDirectory(File folder) {
        if (folder != null) {
            if (folder.exists() == true && folder.isDirectory() == true) {
                workingDirectory = folder.getAbsolutePath();
                comHandler.reply(200);
            } else {
                comHandler.reply(550);
            }
        }
    }

    public boolean isOpen() {
        boolean ret = false;

        if (isPassiveMode == true) {
            if (passiveConnection != null) {
                ret = true;
            }
        } else {
            if (activeConnection != null) {
                ret = true;
            }
        }

        return ret;
    }

    public void setPassiveMode(boolean flag) {
        isPassiveMode = flag;

        if (flag == true) {
            comHandler.reply(227);
        } else {
            // CAUTION: Is this the correct reply?
            comHandler.reply(200);
        }
    }

    private Socket getClient() throws IOException {
        Socket socket = null;

        if (isOpen() == true) {
            if (isPassiveMode == true) {
                socket = passiveConnection.accept();
            } else {
                socket = activeConnection;
            }
        }

        return socket;
    }

    public void closeConnection() throws IOException {
        if (isOpen() == true) {
            if (isPassiveMode == true) {
                passiveConnection.close();
                passiveConnection = null;
            } else {
                activeConnection.close();
                activeConnection = null;
            }
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
