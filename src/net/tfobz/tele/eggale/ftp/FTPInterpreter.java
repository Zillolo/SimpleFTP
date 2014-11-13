/**
 * 
 */
package net.tfobz.tele.eggale.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * @author alex
 *
 */
public class FTPInterpreter extends ProtocolInterpreter {

    public FTPInterpreter(InputStream input, OutputStream output) {
        super(input, output);
    }

    /* (non-Javadoc)
     * @see net.tfobz.tele.eggale.ftp.ProtocolInterpreter#select()
     */
    @Override
    public Command select() throws IOException {
        Command ret = null;

        // Wait until input is ready.
        while (input.ready() == false)
            ;

        String command = input.readLine();
        if (command != null) {
            if (command.indexOf(' ') != -1) {
                if (command.charAt(3) == ' ') {
                    this.argument = command.substring(4, command.length());
                    command = command.substring(0, 3);
                } else {
                    this.argument = command.substring(5, command.length());
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

    /* (non-Javadoc)
     * @see net.tfobz.tele.eggale.ftp.ProtocolInterpreter#reply(net.tfobz.tele.eggale.ftp.Reply)
     */
    @Override
    public void reply(Reply reply) {
        switch (reply) {
        case FILE_ACTION_OK:
            output.print("150 File status okay. Opening data connection...\r\n");
            break;
        case COMMAND_OK:
            output.print("200 Command successful.\r\n");
            break;
        case COMMAND_NOT_IMPLEMENTED:
            output.print("202 Command not implemented.\r\n");
            break;
        case GREETING:
            output.print("220 " + Server.GREETING_MSG + "\r\n");
            break;
        case CLOSING:
            output.print("221 Service closing connection.\r\n");
            break;
        case FILE_ACTION_COMPLETE:
            output.print("226 Requested file action completed. Closing data connection.\r\n");
            break;
        case PASSIVE_MODE:
            output.print("227 Entering passive mode.\r\n");
            break;
        case USER_LOGGED_IN:
            output.print("230 User logged in. Proceed.\r\n");
            break;
        case USER_OK_PASSWORD_NEEDED:
            output.print("331 Username OK. Need password.\r\n");
            break;
        case NEED_ACCOUNT:
            output.print("332 Need account for login.\r\n");
            break;
        case LOCAL_PROCESSING_ERROR:
            output.print("451 Requested action aborted. Local processing error.\r\n");
            break;
        case ILLEGAL_COMMAND:
            output.print("500 Illegal command.\r\n");
            break;
        case SYNTAX_ERROR:
            output.print("501 Syntax error in arguments.\r\n");
            break;
        case BAD_SEQUENCE:
            output.print("503 Bad sequence of commands.\r\n");
            break;
        case COMMAND_WRONG_PARAM:
            output.print("504 Command not implemented for the parameter.\r\n");
            break;
        case LOGIN_INCORRECT:
            output.print("530 Login incorrect.\r\n");
            break;
        case FILE_NOT_FOUND:
            output.print("550 File not found.\r\n");
            break;
        }
        output.flush();
    }

}
