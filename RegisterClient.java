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
            String clientName = InetAddress.getLocalHost().getHostAddress() + ":8080"; // IP:Port
            Diary diary = (Diary) Naming.lookup("rmi://localhost/DiaryService");
            diary.registerFile(clientName, fileName);
            System.out.println("File '" + fileName + "' registered by " + clientName + ".");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
