import java.rmi.Naming;

public class RegisterClient {
    public static void main(String[] args) {
        try {
            Diary diary = (Diary) Naming.lookup("rmi://localhost/DiaryService");
            diary.registerFile("Client1", "example.txt");
            System.out.println("File 'example.txt' registered by Client1.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
