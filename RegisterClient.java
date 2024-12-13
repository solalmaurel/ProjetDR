import java.rmi.Naming;

public class RegisterClient {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java RegisterClient <clientName> <fileName>");
            return;
        }

        String clientName = args[0]; // Nom du client (passé en argument)
        String fileName = args[1];  // Nom du fichier à enregistrer (passé en argument)

        try {
            Diary diary = (Diary) Naming.lookup("rmi://localhost/DiaryService");
            diary.registerFile(clientName, fileName);
            System.out.println("File '" + fileName + "' registered by " + clientName + ".");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
