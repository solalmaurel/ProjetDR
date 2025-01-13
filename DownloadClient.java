import java.io.*;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadClient {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java DownloadClient <fileName>");
            return;
        }

        String fileName = args[0];

        try {
            // Connexion au Diary pour obtenir les clients possédant le fichier
            Diary diary = (Diary) Naming.lookup("rmi://147.127.135.133/DiaryService");
            List<String> clients = diary.getClientsWithFile(fileName);

            if (clients.isEmpty()) {
                System.out.println("No clients have the file '" + fileName + "'.");
            } else {
                int totalParts = clients.size();
                ExecutorService executor = Executors.newFixedThreadPool(totalParts);

                try (RandomAccessFile outputFile = new RandomAccessFile(fileName, "rw")) {
                    // Préallouer la taille du fichier pour éviter les problèmes de fragmentation
                    outputFile.setLength(getFileSizeFromServer(clients.get(0), fileName));

                    for (int i = 0; i < totalParts; i++) {
                        int partIndex = i;
                        String client = clients.get(i);

                        executor.execute(() -> downloadPartFromClient(client, fileName, partIndex, totalParts, outputFile));
                    }

                    executor.shutdown();
                    while (!executor.isTerminated()) {
                        // Attendre la fin des téléchargements
                    }

                    System.out.println("File '" + fileName + "' assembled successfully.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private static void downloadPartFromClient(String client, String fileName, int partIndex, int totalParts, RandomAccessFile outputFile) {
        String[] clientParts = client.split(":");
        String host = clientParts[0];
        int port = Integer.parseInt(clientParts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             InputStream in = socket.getInputStream()) {

            // Demander une partie spécifique du fichier
            writer.println(fileName + ":" + partIndex + ":" + totalParts);

            // Calculer la position dans le fichier final
            long fileSize = outputFile.length();
            long partSize = fileSize / totalParts;
            long startByte = partIndex * partSize;
            long endByte = (partIndex == totalParts - 1) ? fileSize : startByte + partSize;

            byte[] buffer = new byte[1024];
            int bytesRead;
            long currentByte = startByte;

            synchronized (outputFile) {
                outputFile.seek(currentByte);

                while ((bytesRead = in.read(buffer)) != -1 && currentByte < endByte) {
                    outputFile.write(buffer, 0, bytesRead);
                    currentByte += bytesRead;
                }
            }

            System.out.println("Part " + partIndex + " downloaded and written successfully.");
        } catch (IOException e) {
            System.err.println("Error downloading part " + partIndex + " from " + host + ": " + e.getMessage());
        }
    }
}
