import java.io.*;
import java.net.*;

public class Daemon {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) { // écoute sur le port 8080
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            // Lire le nom du fichier demandé
            String requestedFile = reader.readLine();
            File file = new File(requestedFile);

            if (file.exists() && file.isFile()) {
                System.out.println("Sending file: " + file.getName());

                try (FileInputStream fileInput = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileInput.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    System.out.println("File sent successfully.");
                }
            } else {
                // Si le fichier n'existe pas, envoyer un message d'erreur
                String errorMessage = "ERROR: File not found.\n";
                out.write(errorMessage.getBytes());
                System.out.println("Requested file not found: " + requestedFile);
            }

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
