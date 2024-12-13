import java.io.*;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;

public class DownloadClient {
    public static void main(String[] args) {
        try {
            // Connexion au Diary
            Diary diary = (Diary) Naming.lookup("rmi://localhost/DiaryService");
            List<String> clients = diary.getClientsWithFile("example.txt");

            if (clients.isEmpty()) {
                System.out.println("No clients have the file 'example.txt'.");
            } else {
                // On se connecte au premier client pour l'instant
                String clientToConnect = clients.get(0);
                System.out.println("Connecting to " + clientToConnect + " daemon...");
                
                try (Socket socket = new Socket("localhost", 8080);
                     InputStream in = socket.getInputStream();
                     FileOutputStream out = new FileOutputStream("downloaded_example.txt")) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }

                    System.out.println("File downloaded successfully as 'downloaded_example.txt'.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
