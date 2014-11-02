package net.tfobz.tele.eggale.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import net.tfobz.tele.eggale.ftp.state.ComState;
import net.tfobz.tele.eggale.ftp.state.Format;
import net.tfobz.tele.eggale.ftp.state.Mode;
import net.tfobz.tele.eggale.ftp.state.Structure;
import net.tfobz.tele.eggale.ftp.state.Type;
import sun.net.ConnectionResetException;

/**
 * 
 */

/**
 * @author 10eggale
 * 
 */
public class ControlThread extends Thread {

    /**
     * The Control Connection to the Client the Thread is currently handling.
     */
    private Socket controlConnection;

    /**
     * The InputStream of the Control Connection.
     */
    private BufferedReader controlInput;

    /**
     * The OutputStream of the Control Connection.
     */
    private PrintWriter controlOutput;

    /**
     * The host of the Data Connection.
     */
    private String dataHost = "localhost";

    /**
     * The port of the Data Connection.
     */
    private int dataPort = Server.CPORT + 1;

    private DataHandler dataHandler;

    private Structure structure = Structure.FILE;

    private Mode mode = Mode.STREAM;

    private Type type = Type.ASCII;

    private Format format = Format.NONPRINT;

    private String workingDirectory = Server.DEFAULT_DIR;

    private ProtocolInterpreter comHandler;

    /**
     * The current state of the communication.
     */
    private ComState state = ComState.GREETING;

    public ControlThread(Socket client) {
        this.controlConnection = client;

        try {
            comHandler = new ProtocolInterpreter(
                            controlConnection.getInputStream(),
                            controlConnection.getOutputStream());
            dataHandler = new DataHandler(comHandler);
        } catch (IOException e) {
            System.err.println("Couldn't communicate with Control Socket.");
            System.err.println(e.getMessage());
        }
    }

    public void run() {
        boolean userOk = false;

        while (Thread.currentThread().isInterrupted() == false) {
            try {
                Command command = null;
                switch (state) {
                case GREETING:
                    comHandler.reply(220);
                    state = ComState.AUTHENTICATION;
                    break;

                case AUTHENTICATION:
                    command = comHandler.select();
                    switch (command) {
                    case USER:
                        String username = comHandler.getArgument();

                        // TODO: Replace with actual check.
                        if (username.equals("Anonymous")
                                        || username.equals("anonymous")) {
                            userOk = true;
                            comHandler.reply(331);
                        } else {
                            userOk = false;
                            comHandler.reply(530);
                        }
                        break;
                    case PASS:
                        if (userOk == true) {
                            String password = comHandler.getArgument();

                            // TODO: Replace with actual check.
                            if (password.equals("")) {
                                state = ComState.ARBITRARY;
                                comHandler.reply(230);
                            } else {
                                userOk = false;
                                comHandler.reply(530);
                            }
                        }
                        break;
                    case ERROR:
                    default:
                        userOk = false;
                        comHandler.reply(503);
                        break;
                    }
                    break;
                case ARBITRARY:
                    command = comHandler.select();
                    switch (command) {
                    case NOOP:
                        doNoop();
                        break;
                    case PORT:
                        setPort(comHandler.getArgument());
                        break;
                    case PASV:
                        setPassiveMode();
                        break;
                    case MODE:
                        setMode(comHandler.getArgument());
                        break;
                    case STRU:
                        setStructure(comHandler.getArgument());
                        break;
                    case TYPE:
                        setType(comHandler.getArgument());
                        break;
                    case CWD:
                        changeDirectory(comHandler.getArgument());
                        break;
                    case CDUP:
                        changeDirectory("..");
                        break;
                    case PWD:
                        getWorkingDirectory();
                        break;
                    case LIST:
                        listFiles(comHandler.getArgument());
                        break;
                    case STOR:
                        storeFile(comHandler.getArgument());
                        break;
                    case RETR:
                        retrieveFile(comHandler.getArgument());
                        break;
                    case QUIT:
                        quit();
                        break;
                    case ERROR:
                    default:
                        comHandler.reply(500);
                        break;
                    }
                    break;
                }
            } catch (IOException e) {
                // TODO: Handle exception.
                quit();
                e.printStackTrace();
            }
        }

        System.err.println("Exiting thread.");
    }

    private void doNoop() {
        comHandler.reply(200);
    }

    private void setPort(String argument) {
        // Split remaining string by commas.
        String[] arguments = argument.split(",");

        if (arguments.length == 6) {
            String host = arguments[0] + "." + arguments[1] + "."
                            + arguments[2] + "." + arguments[3];

            // Checks if host is a regular IPv4 address.
            if (host.matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")) {
                dataHost = host;

                try {
                    int port = (Integer.parseInt(arguments[4]) << 8)
                                    + Integer.parseInt(arguments[5]);
                    if (port > 0 && port < 65535) {
                        dataPort = port;
                        comHandler.reply(200);
                    } else {
                        comHandler.reply(500);
                    }
                } catch (NumberFormatException e) {
                    comHandler.reply(501);
                }
            } else {
                comHandler.reply(500);
            }
        } else {
            comHandler.reply(501);
        }
    }

    private void setPassiveMode() {
        dataHandler.setPassiveMode(true);
    }

    private void setStructure(String argument) {
        switch (argument) {
        case "F":
            // dataHandler.setStructure(Structure.FILE);
            comHandler.reply(200);
            break;
        default:
            comHandler.reply(504);
        }
    }

    private void setMode(String argument) {
        switch (argument) {
        case "S":
            dataHandler.setMode(Mode.STREAM);
            comHandler.reply(200);
            break;
        default:
            comHandler.reply(504);
        }
    }

    private void setType(String argument) {
        switch (argument.charAt(0)) {
        case 'A':
            dataHandler.setType(Type.ASCII);

            if (argument.length() > 2) {
                switch (argument.charAt(2)) {
                case 'N':
                    format = Format.NONPRINT;
                    break;
                default:
                    format = Format.NONPRINT;
                }
            }

            comHandler.reply(200);
            break;
        case 'I':
            dataHandler.setType(Type.IMAGE);
            comHandler.reply(200);
            break;
        default:
            comHandler.reply(504);
        }
    }

    private void changeDirectory(String directory) {
        File file = new File(directory);

        if (directory.isEmpty() == false) {
            if (directory.equals("..")) {
                file = new File(dataHandler.getWorkingDirectory())
                                .getParentFile();
            }
            dataHandler.changeDirectory(file);
        }
    }

    private void listFiles(String folder) throws IOException {
        File file = null;
        if (folder.isEmpty() == false) {
            if (folder.startsWith("/") == true) {
                // Path is absolute.
                file = new File(folder);
            } else {
                // Path is relative.
                file = new File(dataHandler.getWorkingDirectory() + "/"
                                + folder);
            }
        } else {
            file = new File(dataHandler.getWorkingDirectory());
        }

        if (file.exists()) {
            dataHandler.openConnection(dataPort, dataHost);
            dataHandler.listFiles(file);
            dataHandler.closeConnection();
        } else {
            comHandler.reply(550);
        }
    }

    private void getWorkingDirectory() {
        comHandler.reply(257, dataHandler.getWorkingDirectory());
    }

    private void storeFile(String name) throws IOException {
        File file = null;
        if (name.startsWith("/") == true) {
            // Path is absolute.
            file = new File(name);
        } else {
            // Path is relative.
            file = new File(dataHandler.getWorkingDirectory() + "/" + name);
        }

        dataHandler.openConnection(dataPort, dataHost);
        dataHandler.store(file);
        dataHandler.closeConnection();
    }

    private void retrieveFile(String file) {
        File f = new File(dataHandler.getWorkingDirectory() + "/" + file);
        if (file != null && file.isEmpty() == false) {
            if (f.exists() == true && f.isDirectory() == false) {
                try {
                    dataHandler.openConnection(dataPort, dataHost);
                    dataHandler.retrieve(f);
                    dataHandler.closeConnection();
                } catch (IllegalStateException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                comHandler.reply(550);
            }
        }
    }

    private void quit() {
        comHandler.reply(221);

        try {
            controlInput.close();
            controlOutput.close();
            controlConnection.close();
        } catch (IOException e) {
            // TODO: Wtf?
            e.printStackTrace();
        }

        this.interrupt();
    }
}
