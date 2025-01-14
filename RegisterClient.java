import java.net.InetAddress;
import java.rmi.Naming;

public class RegisterClient {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java RegisterClient <fileName> <port>");
            return;
        }

        String fileName = args[0]; // Nom du fichier à enregistrer
        String port = args[1];     // Port passé en argument

        try {
            // Obtenir l'adresse IP locale
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            String clientName = ipAddress + ":" + port; // Format IP:Port

            // Connexion au service RMI
            Diary diary = (Diary) Naming.lookup("rmi://147.127.135.79/DiaryService");

            // Enregistrer le fichier
            diary.registerFile(clientName, fileName);
            System.out.println("File '" + fileName + "' registered by " + clientName + ".");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}