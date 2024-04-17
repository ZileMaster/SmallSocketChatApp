import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        final Scanner sc = new Scanner(System.in);

        System.out.print("Enter your username: ");
        String username = sc.nextLine();

        try {
            Socket clientSocket = new Socket("127.0.0.1", 5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println(username);

            Thread sender = new Thread(() -> {
                String msg;
                while (true) {
                    msg = sc.nextLine();
                    if (msg.startsWith("@")) {
                        out.println(msg);
                    } else {
                        out.println(msg);
                    }
                }
            });
            sender.start();

            String msg;
            while (true) {
                msg = in.readLine();
                if (msg == null) {
                    break;
                }
                System.out.println(msg);
            }

            in.close();
            out.close();
            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}