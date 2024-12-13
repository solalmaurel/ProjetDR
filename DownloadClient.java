import java.io.*;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;

public class DownloadClient {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java DownloadClient <fileName>");
            return;
        }

        String fileName = args[0]; // Récupérer le nom du fichier passé en argument
        try {
            // Connexion au Diary
            Diary diary = (Diary) Naming.lookup("rmi://localhost/DiaryService");
            List<String> clients = diary.getClientsWithFile(fileName);

            if (clients.isEmpty()) {
                System.out.println("No clients have the file '" + fileName + "'.");
            } else {
                // On se connecte au premier client pour l'instant
                String clientToConnect = clients.get(0);
                System.out.println("Connecting to " + clientToConnect + " daemon...");
                
                try (Socket socket = new Socket("localhost", 8080);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                     FileOutputStream out = new FileOutputStream("downloaded_" + fileName)) {

                    // Envoyer le nom du fichier au daemon
                    writer.println(fileName);

                    // Lire et écrire le contenu du fichier
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    InputStream in = socket.getInputStream();
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }

                    System.out.println("File downloaded successfully as 'downloaded_" + fileName + "'.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
