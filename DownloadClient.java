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
            Diary diary = (Diary) Naming.lookup("rmi://147.127.133.199/DiaryService");
            List<String> clients = diary.getClientsWithFile(fileName);

            if (clients.isEmpty()) {
                System.out.println("No clients have the file '" + fileName + "'.");
            } else {
                int totalParts = clients.size();
                ExecutorService executor = Executors.newFixedThreadPool(totalParts);
                File[] partFiles = new File[totalParts];

                for (int i = 0; i < totalParts; i++) {
                    int partIndex = i;
                    String client = clients.get(i);

                    executor.execute(() -> downloadPartFromClient(client, fileName, partIndex, totalParts, partFiles));
                }

                executor.shutdown();
                while (!executor.isTerminated()) {
                    // Attendre la fin des téléchargements
                }

                // Assembler les parties téléchargées
                assembleFile(fileName, partFiles);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadPartFromClient(String client, String fileName, int partIndex, int totalParts, File[] partFiles) {
        String[] clientParts = client.split(":"); // Assume client format: "hostname:port"
        String host = clientParts[0];
        int port = Integer.parseInt(clientParts[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             InputStream in = socket.getInputStream()) {

            // Demander une partie spécifique du fichier
            writer.println(fileName + ":" + partIndex + ":" + totalParts);

            // Sauvegarder la partie téléchargée dans un fichier temporaire
            File partFile = new File("part_" + partIndex + "_" + fileName);
            partFiles[partIndex] = partFile;

            try (FileOutputStream out = new FileOutputStream(partFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("Part " + partIndex + " downloaded successfully from " + host + ".");
        } catch (IOException e) {
            System.err.println("Error downloading part " + partIndex + " from " + host + ": " + e.getMessage());
        }
    }

    private static void assembleFile(String fileName, File[] partFiles) {
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            for (File partFile : partFiles) {
                if (partFile != null) {
                    try (FileInputStream in = new FileInputStream(partFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    // Supprimer le fichier temporaire après assemblage
                    partFile.delete();
                }
            }

            System.out.println("File '" + fileName + "' assembled successfully.");
        } catch (IOException e) {
            System.err.println("Error assembling file: " + e.getMessage());
        }
    }
}
