import java.io.*;
import java.net.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Daemon {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Daemon <port> [-c]");
            return;
        }

        int port;
        boolean compress = false;

        // Vérification des arguments
        try {
            port = Integer.parseInt(args[0]);

            // Vérification si le flag de compression est présent
            if (args.length > 1 && args[1].equals("-c")) {
                compress = true;
                System.out.println("Compression enabled.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Please provide a valid integer.");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Daemon is running on port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new FileHandler(clientSocket, compress)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class FileHandler implements Runnable {
    private final Socket clientSocket;
    private final boolean isCompressed;

    public FileHandler(Socket clientSocket, boolean isCompressed) {
        this.clientSocket = clientSocket;
        this.isCompressed = isCompressed;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String request = reader.readLine();

            if (request.startsWith("SIZE:")) {
                String requestedFile = request.substring(5); // Extraire le nom du fichier
                File file = new File(requestedFile);

                if (file.exists() && file.isFile()) {
                    long fileSize = file.length();
                    out.write((fileSize + "\n").getBytes());

                    // Informer le client si le fichier est compressé
                    out.write(("COMPRESSED:" + isCompressed + "\n").getBytes());  // Ajout de l'information de compression
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
                        if (isCompressed) {
                            // Si la compression est activée, compresser les données
                            buffer = compressData(buffer, bytesRead);
                        }
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

    private byte[] compressData(byte[] data, int bytesRead) {
        // Implémenter la compression (par exemple avec Zip)
        // Cette méthode est une simple illustration. Utilisez un algorithme de compression réel comme Zip ou GZIP.
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)) {

            ZipEntry zipEntry = new ZipEntry("compressed");
            zipOut.putNextEntry(zipEntry);
            zipOut.write(data, 0, bytesRead);
            zipOut.closeEntry();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data; // Retourner les données non compressées en cas d'erreur
    }
}