/**
 * 
 */
package net.tfobz.tele.eggale.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @author alex
 * 
 */
public final class ProtocolInterpreter {

    private BufferedReader input = new BufferedReader(new InputStreamReader(
                    System.in));

    private PrintWriter output = new PrintWriter(System.out);

    private String argument = "";

    public ProtocolInterpreter(InputStream input, OutputStream output) {
        setInput(input);
        setOutput(output);
    }

    /**
     * select() returns the type of command sent by the client. The argument
     * must be read using the getArgument()-function.
     * 
     * @return
     * @throws IOException
     */
    public Command select() throws IOException {
        Command ret = null;

        // Wait until input is ready.
        while (input.ready() == false)
            ;

        String command = input.readLine();
        if (command != null) {
            if (command.indexOf(' ') != -1) {
                if (command.charAt(3) == ' ') {
                    this.argument = command.substring(4,
                                    command.length());
                    command = command.substring(0, 3);
                } else {
                    this.argument = command.substring(5,
                                    command.length());
                    command = command.substring(0, 4);
                }
            }

            switch (command) {
            case "USER":
                ret = Command.USER;
                break;
            case "PASS":
                ret = Command.PASS;
                break;
            case "NOOP":
                ret = Command.NOOP;
                break;
            case "PORT":
                ret = Command.PORT;
                break;
            case "PASV":
                ret = Command.PASV;
                break;
            case "MODE":
                ret = Command.MODE;
                break;
            case "TYPE":
                ret = Command.TYPE;
                break;
            case "STRU":
                ret = Command.STRU;
                break;
            case "PWD":
                ret = Command.PWD;
                break;
            case "CWD":
                ret = Command.CWD;
                break;
            case "CDUP":
                ret = Command.CDUP;
                break;
            case "LIST":
                ret = Command.LIST;
                break;
            case "STOR":
                ret = Command.STOR;
                break;
            case "RETR":
                ret = Command.RETR;
                break;
            case "QUIT":
                ret = Command.QUIT;
                break;
            default:
                ret = Command.ERROR;
                break;
            }
        } else {
            ret = Command.ERROR;
        }
        return ret;
    }

    public void reply(int code) {
        switch (code) {
        case 150:
            output.print("150 File status okay. Opening data connection...\r\n");
            break;
        case 200:
            output.print("200 Command successful.\r\n");
            break;
        case 202:
            output.print("202 Command not implemented.\r\n");
            break;
        case 220:
            output.print("220 Service ready.\r\n");
            break;
        case 221:
            output.print("221 Service closing connection.\r\n");
            break;
        case 226:
            output.print("226 Requested file action completed. Closing data connection.\r\n");
            break;
        case 227:
            output.print("227 Entering passive mode.\r\n");
            break;
        case 230:
            output.print("230 User logged in. Proceed.\r\n");
            break;
        case 257:
            // 257 is a non-uniform message and must be composed individually.
            break;
        case 331:
            output.print("331 Username OK. Need password.\r\n");
            break;
        case 332:
            output.print("332 Need account for login.\r\n");
            break;
        case 451:
            output.print("451 Requested action aborted. Local processing error.\r\n");
            break;
        case 500:
            output.print("500 Illegal command.\r\n");
            break;
        case 501:
            output.print("501 Syntax error in arguments.\r\n");
            break;
        case 503:
            output.print("503 Bad sequence of commands.\r\n");
            break;
        case 504:
            output.print("504 Command not implemented for the parameter.\r\n");
            break;
        case 530:
            output.print("530 Login incorrect.\r\n");
            break;
        case 550:
            output.print("550 File not found.\r\n");
            break;
        }
        output.flush();
    }

    public void reply(int code, String message) {
        output.print(code + " " + message + "\r\n");
        output.flush();
    }

    // private static String readInput() throws IOException {
    // String command = null;
    // if (input != null) {
    // command = "";
    // int data = input.read();
    // while (data != -1) {
    // command += (char) data;
    // data = input.read();
    // }
    // }
    // return command;
    // }

    /**
     * Returns the argument of the last command. Once read the argument will be
     * cleared.
     * 
     * @return
     */
    public String getArgument() {
        String temp = this.argument;
        this.argument = "";
        return temp;
    }

    public void setInput(InputStream input) {
        if (input != null) {
            this.input = new BufferedReader(
                            new InputStreamReader(input));
        }
    }

    public void setOutput(OutputStream outputStream) {
        if (outputStream != null) {
            this.output = new PrintWriter(outputStream);
        }
    }
}
