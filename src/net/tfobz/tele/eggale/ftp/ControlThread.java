package net.tfobz.tele.eggale.ftp;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.imageio.ImageIO;

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

  /**
   * Whether transfers should happen in passive mode.
   */
  private boolean passiveMode = false;

  private Structure structure = Structure.FILE;

  private Mode mode = Mode.STREAM;

  private Type type = Type.ASCII;

  private Format format = Format.NONPRINT;

  private String workingDirectory = Server.DEFAULT_DIR;

  /**
   * The current state of the communication.
   */
  private ComState state = ComState.GREETING;

  public ControlThread(Socket client) {
    this.controlConnection = client;

    try {
      controlInput = new BufferedReader(new InputStreamReader(
              controlConnection.getInputStream()));
      controlOutput = new PrintWriter(controlConnection.getOutputStream(), true);

    } catch (IOException e) {
      System.err.println("Couldn't communicate with Control Socket.");
      System.err.println(e.getMessage());
    }
  }

  public void run( ) {
    boolean userOk = false;

    while (Thread.currentThread().isInterrupted() == false) {
      switch (state) {
      case GREETING: {
        sendResponse(220);
        state = ComState.AUTH_NEEDED;
        break;
      }
      case AUTH_NEEDED: {
        try {
          String cmd = controlInput.readLine();

          // Command is recognized as being login request.
          if (cmd.matches("USER .+")) {
            String user = cmd.substring(5);

            // TODO: Replace with actual check.
            if (user.equals("Anonymous")) {
              userOk = true;

              sendResponse(331);
            } else {
              userOk = false;

              sendResponse(530);
            }
          } else if (cmd.matches("PASS .*")) {
            if (userOk == true) {
              String pass = cmd.substring(5);

              // TODO: Replace with actual check.
              if (pass.equals("")) {
                state = ComState.ARBITRARY;

                sendResponse(230);
              } else {
                sendResponse(530);
              }
            } else {
              sendResponse(503);
            }
            userOk = false;
          } else {
            sendResponse(332);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        break;
      }

      case ARBITRARY: {
        try {
          String cmd = controlInput.readLine();

          if (cmd == null) {
            throw new ConnectionResetException();
          }
          if (cmd.matches("NOOP")) {
            doNoop();
          } else if (cmd.matches("PORT .+")) {
            cmd = cmd.substring(5);
            setPort(cmd);
          } else if (cmd.matches("PASV")) {
            togglePassive(true);
          } else if (cmd.matches("MODE .+")) {
            cmd = cmd.substring(5);
            changeMode(cmd);
          } else if (cmd.matches("TYPE .+")) {
            cmd = cmd.substring(5);
            changeType(cmd);
          } else if (cmd.matches("STRU .+")) {
            cmd = cmd.substring(5);
            changeStructure(cmd);
          } else if (cmd.matches("PWD")) {
            sendWDir();
          } else if (cmd.matches("CWD .+")) {
            cmd = cmd.substring(5);
            changeDir(cmd);
          } else if (cmd.matches("LIST\\s{0,1}.*")) {
            if (cmd.length() > 5)
              cmd = cmd.substring(5);
            else
              cmd = "";
            listFiles(cmd);
          } else if (cmd.matches("STOR .+")) {
            cmd = cmd.substring(5);

            storeFile(cmd);
          } else if (cmd.matches("QUIT")) {
            quit();
          } else {
            sendResponse(202);
          }
        } catch (IOException e) {
          System.err.println("Couldn't communicate with Socket.");
          e.getMessage();

          this.interrupt();
        }

        break;
      }
      }
    }

    System.err.println("Exiting thread.");
  }

  private void sendResponse(int code) {
    switch (code) {
    case 150:
      controlOutput
              .write("150 File status okay. Opening data connection...\r\n");
      break;
    case 200:
      controlOutput.write("200 Command successful.\r\n");
      break;
    case 202:
      controlOutput.write("202 Command not implemented.\r\n");
      break;
    case 220:
      controlOutput.write("220 Service ready.\r\n");
      break;
    case 221:
      controlOutput.write("221 Service closing connection.\r\n");
      break;
    case 226:
      controlOutput
              .write("226 Requested file action completed. Closing data connection.\r\n");
      break;
    case 227:
      controlOutput.write("227 Entering passive mode. (" + dataHost + ":"
              + dataPort + ")\r\n");
      break;
    case 230:
      controlOutput.write("230 User logged in. Proceed.\r\n");
      break;
    case 250:
      //
      break;
    case 257:
      controlOutput.write("257 " + workingDirectory
              + " is current directory.\r\n");
      break;
    case 331:
      controlOutput.write("331 Username OK. Need password.\r\n");
      break;
    case 332:
      controlOutput.write("332 Need account for login.\r\n");
      break;
    case 500:
      controlOutput.write("500 Illegal command.\r\n");
      break;
    case 501:
      controlOutput.write("501 Syntax error in arguments.\r\n");
      break;
    case 503:
      controlOutput.write("503 Bad sequence of commands.\r\n");
      break;
    case 504:
      controlOutput.write("504 Command not implemented for the parameter.\r\n");
      break;
    case 530:
      controlOutput.write("530 Login incorrect.\r\n");
      break;
    case 550:
      controlOutput.write("550 File not found.\r\n");
      break;
    }
    controlOutput.flush();
  }

  private void doNoop( ) {
    sendResponse(200);
  }

  private void setPort(String argument) {
    // Split remaining string by commas.
    String[] arguments = argument.split(",");

    if (arguments.length == 6) {
      String host = arguments[0] + "." + arguments[1] + "." + arguments[2]
              + "." + arguments[3];

      // Checks if host is a regular IPv4 address.
      if (host.matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")) {
        dataHost = host;

        try {
          int port = (Integer.parseInt(arguments[4]) << 8)
                  + Integer.parseInt(arguments[5]);
          if (port > 0 && port < 65535) {
            dataPort = port;
            sendResponse(200);
          } else {
            sendResponse(500);
          }
        } catch (NumberFormatException e) {
          sendResponse(501);
        }
      } else {
        sendResponse(500);
      }
    } else {
      sendResponse(501);
    }
  }

  private void changeStructure(String argument) {
    switch (argument) {
    case "F":
      structure = Structure.FILE;
      sendResponse(200);
      break;
    // case "P":
    // structure = Structure.PAGE;
    // sendResponse(200);
    // break;
    // case "R":
    // structure = Structure.RECORD;
    // sendResponse(200);
    // break;
    default:
      sendResponse(500);
    }
  }

  private void changeMode(String argument) {
    switch (argument) {
    case "S":
      mode = Mode.STREAM;
      sendResponse(200);
      break;
    // case "B":
    // mode = Mode.BLOCK;
    // sendResponse(200);
    // break;
    // case "C":
    // mode = Mode.COMPRESSED;
    // sendResponse(200);
    // break;
    default:
      sendResponse(504);
    }
  }

  private void changeType(String argument) {
    switch (argument.charAt(0)) {
    case 'A':
      type = Type.ASCII;

      if (argument.length() > 2) {
        switch (argument.charAt(2)) {
        case 'N':
          format = Format.NONPRINT;
          break;
        // case 'T':
        // format = Format.TELNET;
        // break;
        // case 'C':
        // format = Format.ASA;
        // break;
        default:
          format = Format.NONPRINT;
        }
      }

      sendResponse(200);
      break;
    // case 'E':
    // type = Type.EBCDIC;
    //
    // if (argument.length() > 2) {
    // switch (argument.charAt(2)) {
    // case 'N':
    // format = Format.NONPRINT;
    // break;
    // case 'T':
    // format = Format.TELNET;
    // break;
    // case 'C':
    // format = Format.ASA;
    // break;
    // }
    // }
    // sendResponse(200);
    // break;
    case 'I':
      type = Type.IMAGE;
      sendResponse(200);
      break;
    // case 'L':
    // type = Type.LOCAL;
    // sendResponse(200);
    // break;
    default:
      sendResponse(500);
    }
  }

  private void changeDir(String directory) {
    File folder = new File("/" + directory);

    if (folder.exists()) {
      if (folder.isDirectory()) {
        workingDirectory = "/" + directory;
        sendResponse(200);
      } else {
        sendResponse(550);
      }
    } else {
      sendResponse(550);
    }
  }

  private void listFiles(String folder) throws IOException {
    File file = null;
    if (folder.isEmpty()) {
      file = new File(workingDirectory);
    } else {
      file = new File(folder);
    }
    if (file.exists()) {
      sendResponse(150);

      ServerSocket dataConnection = null;
      Socket data = null;
      if (passiveMode) {
        dataConnection = new ServerSocket(dataPort);
        data = dataConnection.accept();
      } else {
        data = new Socket(dataHost, dataPort);
      }
      System.out.println("Thread " + Thread.currentThread().getId()
              + " is opening data connection.");

      PrintWriter output = new PrintWriter(data.getOutputStream());

      if (file.isDirectory()) {
        for (File f : file.listFiles()) {
          output.write(f.getName() + "\r\n");
          output.flush();
        }
      } else {
        output.write(file.toString());
        output.flush();
      }

      sendResponse(226);
      output.close();
      data.close();

      if (passiveMode)
        dataConnection.close();
    } else {
      sendResponse(550);
    }
  }

  private void sendWDir( ) {
    sendResponse(257);
  }

  private void storeFile(String file) throws IOException {
    ServerSocket dataConnection = null;
    Socket data = null;

    sendResponse(150);
    if (passiveMode) {
      dataConnection = new ServerSocket(dataPort);
      data = dataConnection.accept();
    } else {
      data = new Socket(dataHost, dataPort);
    }
    System.out.println("Data connection established.");

    FileOutputStream out = new FileOutputStream(workingDirectory + "/" + file);

    switch (mode) {
    case STREAM:
      switch (type) {
      case ASCII:
        BufferedReader input = new BufferedReader(new InputStreamReader(
                data.getInputStream()));

        try {
          int count = 0;

          int b = input.read();
          while (b != -1) {
            out.write(b);
            b = input.read();
          }
        } catch (ConnectionResetException e) {
          // Ignore. Probably means EOF.
        }
        input.close();
        break;
      case IMAGE:
        BufferedImage image = null;
        image = ImageIO.read(data.getInputStream());
        ImageIO.write(image, "JPG", out);
        break;
      }
      break;
    // case BLOCK:
    // break;
    // case COMPRESSED:
    // break;
    }
    sendResponse(226);
    out.close();
    data.close();

    if (passiveMode)
      dataConnection.close();
  }

  private void togglePassive(boolean flag) {
    passiveMode = flag;
    sendResponse(227);
  }

  private void quit( ) throws IOException {
    sendResponse(221);

    controlOutput.close();
    controlInput.close();
    controlConnection.close();

    this.interrupt();
  }
}
