import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.*;

public class DiaryImpl extends UnicastRemoteObject implements Diary {
    private final Map<String, List<String>> fileRegistry = new HashMap<>();

    public DiaryImpl() throws RemoteException {}

    @Override
    public synchronized void registerFile(String clientId, String fileName) throws RemoteException {
        fileRegistry.computeIfAbsent(fileName, k -> new ArrayList<>()).add(clientId);
        System.out.println("File registered: " + fileName + " by " + clientId);
    }

    @Override
    public synchronized List<String> getClientsWithFile(String fileName) throws RemoteException {
        return fileRegistry.getOrDefault(fileName, Collections.emptyList());
    }
}

