import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<Integer, String> allMessages = new LinkedHashMap<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(5000);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String username = in.readLine();
                ClientHandler clientHandler = new ClientHandler(clientSocket, username);
                clients.add(clientHandler);
                clientHandler.start();
                clientHandler.notifyClients(username + " has connected!");
                clientHandler.sendLastMessages();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private final BufferedReader in;
        private final PrintWriter out;
        private final String username;

        public ClientHandler(Socket clientSocket, String username) throws IOException {
            this.clientSocket = clientSocket;
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.username = username;
        }

        private static final int MAX_MESSAGES = 5;
        private static int messageIndex = 1;

        private synchronized int getNextMessageIndex() {
            return messageIndex++;
        }

        private void sendLastMessages() {
            for (String message : allMessages.values()) {
                out.println(message);
            }
        }

        private void notifyClients(String message) {
            for (ClientHandler client : clients) {
                client.out.println(message);
            }
        }

        private void broadcastAll(String message) {
            for (ClientHandler client : clients) {
                if (client != this) {
                    client.out.println(message);
                }
            }
        }

        private void addMessage(String message) {
            int index = getNextMessageIndex();
            allMessages.put(index, "[" + index + "] " + message);
            if (allMessages.size() > MAX_MESSAGES) {
                allMessages.remove(allMessages.keySet().iterator().next());
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String msg = in.readLine();
                    if (msg == null) {
                        break;
                    }

                    if (msg.startsWith("@")) {
                        String recipient = msg.split(" ")[0].substring(1);
                        for (ClientHandler client : clients) {
                            if (client.username.equals(recipient)) {
                                client.out.println(msg.substring(recipient.length() + 2));
                                break;
                            }
                        }
                    } else {
                        if(msg.indexOf("/") == 1 || msg.indexOf("/") == 2){
                            int msgIndex = Integer.parseInt(msg.split("/")[0]);
                            String selectedMsg = allMessages.get(msgIndex);
                            String hisUsername = selectedMsg.split("] ")[1].split(":")[0];
                            String actualMsg = selectedMsg.split(":")[1];
                            if(Objects.equals(hisUsername, username)){
                                allMessages.remove(msgIndex);
                                String goodMessage = msg.split("/")[1];
                                allMessages.put(msgIndex, "[" + msgIndex + "] " + username + ":" + goodMessage + " *edited");
                                broadcastAll(username + " has changed the message on index [" + msgIndex + "] to: " + goodMessage);
                            }else {
                                String goodMessage = msg.split("/")[1];
                                broadcastAll("[" +messageIndex + "] *Reply to -> " + allMessages.get(msgIndex) + "* -> " + username + ": " + goodMessage);
                                addMessage(username + ": " + "*Reply to -> " + allMessages.get(msgIndex) + "* -> " + username + ": " + goodMessage);
                            }
                        }else {
                            broadcastAll("[" + messageIndex + "] " + username + ": " + msg);
                            addMessage(username + ": " + msg);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}