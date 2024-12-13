import java.io.*;
import java.net.*;

public class Daemon {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) { // écoute dur le port 8080
            System.out.println("Daemon is running on port 8080...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                // gestion des requêtes avec des threads
                new Thread(new FileHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class FileHandler implements Runnable {
    private final Socket clientSocket;

    public FileHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (OutputStream out = clientSocket.getOutputStream()) {
            // Simuler un fichier en envoyant un texte
            String simulatedFileContent = "Ayo you are in example.txt";
            out.write(simulatedFileContent.getBytes());
            out.flush();
            System.out.println("File sent to client.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
