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
            Diary diary = (Diary) Naming.lookup("rmi://localhost/DiaryService");
            List<String> clients = diary.getClientsWithFile(fileName);

            if (clients.isEmpty()) {
                System.out.println("No clients have the file '" + fileName + "'.");
            } else {
                // Télécharger de manière parallèle à partir des clients
                ExecutorService executor = Executors.newFixedThreadPool(clients.size());

                for (String client : clients) {
                    executor.execute(() -> downloadFromClient(client, fileName));
                }

                executor.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadFromClient(String client, String fileName) {
        String[] clientParts = client.split(":"); // Assume client format: "hostname:port"
        String host = clientParts[0];
        int port = Integer.parseInt(clientParts[1]);

        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             FileOutputStream out = new FileOutputStream("downloaded_from_" + host + "_" + fileName)) {

            // Envoyer le nom du fichier au daemon
            writer.println(fileName);

            // Lire et écrire le contenu du fichier
            byte[] buffer = new byte[1024];
            int bytesRead;
            InputStream in = socket.getInputStream();
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("File downloaded successfully from " + host + " as 'downloaded_from_" + host + "_" + fileName + "'.");
        } catch (IOException e) {
            System.err.println("Error downloading from " + host + ": " + e.getMessage());
        }
    }
}
