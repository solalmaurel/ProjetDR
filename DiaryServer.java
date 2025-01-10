import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class DiaryServer {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099); // Lance le registre RMI sur le port par défaut
            Diary diary = new DiaryImpl();
            Naming.rebind("DiaryService", diary);
            System.out.println("DiaryService is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}