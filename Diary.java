import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Diary extends Remote {

    // Méthode pour enregister un couple (client, fichier) dans l'annuaire
    void registerFile(String clientId, String fileName) throws RemoteException;

    // Méthode pour avoir la liste des clients ayant le fichier
    List<String> getClientsWithFile(String fileName) throws RemoteException;
}