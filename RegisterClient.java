import java.net.InetAddress;
import java.rmi.Naming;

public class RegisterClient {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java RegisterClient <fileName>");
            return;
        }

        String fileName = args[0];  // Nom du fichier à enregistrer (passé en argument)

        try {
            String ip_adress = InetAddress.getLocalHost().getHostAddress();
            String clientName = ip_adress + ":8080"; // IP:Port
            Diary diary = (Diary) Naming.lookup("rmi://147.127.133.199/DiaryService");
            diary.registerFile(clientName, fileName);
            System.out.println("File '" + fileName + "' registered by " + clientName + ".");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
