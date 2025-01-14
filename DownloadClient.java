import java.io.*;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadClient {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java DownloadClient <fileName>");
            return;
        }

        String fileName = args[0];

        try {
            // Connexion au Diary pour obtenir les clients possédant le fichier
            Diary diary = (Diary) Naming.lookup("rmi://147.127.135.79/DiaryService");
            List<String> clients = diary.getClientsWithFile(fileName);

            if (clients.isEmpty()) {
                System.out.println("No clients have the file '" + fileName + "'.");
            } else {
                int totalParts = clients.size();
                ExecutorService executor = Executors.newFixedThreadPool(totalParts);

                long fileSize = getFileSizeFromServer(clients.get(0), fileName);

                // Préallouer la taille du fichier pour éviter les problèmes de fragmentation
                try (RandomAccessFile outputFile = new RandomAccessFile(fileName, "rw")) {
                    outputFile.setLength(fileSize);
                }

                // Obtenir l'information de compression envoyée par le serveur
                boolean isCompressed = getCompressionInfoFromServer(clients.get(0),fileName);

                for (int i = 0; i < totalParts; i++) {
                    int partIndex = i;
                    String client = clients.get(i);
                    System.out.println("DEBUT PARALLELISATION");

                    executor.execute(() -> downloadPartFromClient(client, fileName, partIndex, totalParts, fileSize));
                }

                executor.shutdown();
                while (!executor.isTerminated()) {
                    // Attendre la fin des téléchargements
                }

                System.out.println("File '" + fileName + "' assembled successfully.");

                // Si le fichier est compressé, décompresser après téléchargement
                if (isCompressed) {
                    decompressFile(fileName);
                    System.out.println("File decompressed successfully.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean getCompressionInfoFromServer(String client, String fileName) {
        String[] clientParts = client.split(":");
        String host = clientParts[0];
        int port = Integer.parseInt(clientParts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Demander les informations sur la taille et la compression du fichier
            writer.println("SIZE:" + fileName);
            String response = reader.readLine();

            // Le serveur répondra avec la taille du fichier et un indicateur de compression
            String compressionResponse = reader.readLine();
            return compressionResponse.contains("COMPRESSED:true");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static long getFileSizeFromServer(String client, String fileName) {
        String[] clientParts = client.split(":");
        String host = clientParts[0];
        int port = Integer.parseInt(clientParts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println("SIZE:" + fileName);
            String response = reader.readLine();

            return Long.parseLong(response.trim());
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error fetching file size from " + host + ": " + e.getMessage());
        }

        throw new RuntimeException("Unable to determine file size.");
    }

    private static void downloadPartFromClient(String client, String fileName, int partIndex, int totalParts, long fileSize) {
        String[] clientParts = client.split(":");
        String host = clientParts[0];
        int port = Integer.parseInt(clientParts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             InputStream in = socket.getInputStream()) {

            // Demander une partie spécifique du fichier
            writer.println(fileName + ":" + partIndex + ":" + totalParts);

            // Calculer la position dans le fichier final
            long partSize = fileSize / totalParts;
            long startByte = partIndex * partSize;
            long endByte = (partIndex == totalParts - 1) ? fileSize : startByte + partSize;

            byte[] buffer = new byte[1024];
            int bytesRead;
            long currentByte = startByte;

            // Utiliser une instance dédiée de RandomAccessFile pour ce thread
            try (RandomAccessFile threadFile = new RandomAccessFile(fileName, "rw")) {
                threadFile.seek(currentByte);

                while ((bytesRead = in.read(buffer)) != -1 && currentByte < endByte) {
                    threadFile.write(buffer, 0, bytesRead);
                    currentByte += bytesRead;
                    try {
                        Thread.sleep(5);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Part " + partIndex + " downloaded and written successfully.");
        } catch (IOException e) {
            System.err.println("Error downloading part " + partIndex + " from " + host + ": " + e.getMessage());
        }
    }

    // Méthode pour décompresser un fichier ZIP
    private static void decompressFile(String zipFilePath) {
        System.err.println("FONCTION USED");

        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zipIn = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String fileName = entry.getName();
                File outputFile = new File(fileName);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zipIn.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }

                zipIn.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}