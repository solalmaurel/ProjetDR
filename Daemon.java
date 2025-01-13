import java.io.*;
import java.net.*;

public class Daemon {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Daemon <port>");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Please provide a valid integer.");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) { // écoute sur le port spécifié
            System.out.println("Daemon is running on port " + port + "...");

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

            String request = reader.readLine();

            if (request.startsWith("SIZE:")) {
                // Traiter la commande SIZE
                String requestedFile = request.substring(5); // Extraire le nom du fichier
                File file = new File(requestedFile);

                if (file.exists() && file.isFile()) {
                    long fileSize = file.length();
                    out.write((fileSize + "\n").getBytes());
                } else {
                    out.write("ERROR: File not found.\n".getBytes());
                }
                return;
            }

            // Autres commandes (comme le téléchargement de parties)
            // Lire la requête : "fileName:partIndex:totalParts"
            String[] requestParts = request.split(":");
            if (requestParts.length != 3) {
                out.write("ERROR: Invalid request format.\n".getBytes());
                return;
            }

            String requestedFile = requestParts[0];
            int partIndex = Integer.parseInt(requestParts[1]);
            int totalParts = Integer.parseInt(requestParts[2]);

            File file = new File(requestedFile);

            if (file.exists() && file.isFile()) {
                long fileSize = file.length();
                long partSize = fileSize / totalParts;
                long startByte = partIndex * partSize;
                long endByte = (partIndex == totalParts - 1) ? fileSize : startByte + partSize;

                System.out.println("Sending part " + partIndex + " of file: " + file.getName());

                try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                    randomAccessFile.seek(startByte);

                    byte[] buffer = new byte[1024];
                    long bytesToSend = endByte - startByte;
                    int bytesRead;

                    while (bytesToSend > 0 && (bytesRead = randomAccessFile.read(buffer, 0, (int) Math.min(buffer.length, bytesToSend))) != -1) {
                        out.write(buffer, 0, bytesRead);
                        bytesToSend -= bytesRead;
                    }

                    System.out.println("Part " + partIndex + " sent successfully.");
                }
            } else {
                out.write("ERROR: File not found.\n".getBytes());
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