import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Server {

  private static final int PORT = 12345;
  private static final int FILE_COUNT = 5;

  public static void main(String[] args) throws IOException {
    ServerSocket serverSocket = new ServerSocket(PORT);
    System.out.println("Server is listening on port " + PORT);

    while (true) {
      try (Socket socket = serverSocket.accept()) {
        handleClient(socket);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static void handleClient(Socket clientSocket) throws IOException {
    try (DataInputStream input = new DataInputStream(clientSocket.getInputStream());
         DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {

      for (int i = 0; i < FILE_COUNT; i++) {
        byte[] clientFileHash = new byte[96]; // 3 SHA-256 hashes concatenated
        input.readFully(clientFileHash);

        String serverFilePath = "AliceFiles/alice_file" + (i+1)+ ".txt";
        byte[] serverFileHash = hashFileCombined(serverFilePath);

        boolean filesMatch = Arrays.equals(clientFileHash, serverFileHash);
        System.out.println("File " + (i+1) + " matches: " + filesMatch);
        output.writeBoolean(filesMatch);
      }
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  private static byte[] hashFileCombined(String filePath) throws IOException, NoSuchAlgorithmException {
    File file = new File(filePath);
    long halfLength = file.length() / 2;

    byte[] hashWhole = hashFile(filePath, 0, (int) file.length());
    byte[] hashFirstHalf = hashFile(filePath, 0, (int) halfLength);
    byte[] hashSecondHalf = hashFile(filePath, (int) halfLength, (int) (file.length() - halfLength));

    return concatenateHashes(hashWhole, hashFirstHalf, hashSecondHalf);
  }

  private static byte[] hashFile(String filePath, int start, int length) throws NoSuchAlgorithmException, IOException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
      byte[] bytesBuffer = new byte[length];
      raf.seek(start);
      int bytesRead = raf.read(bytesBuffer, 0, length);
      digest.update(bytesBuffer, 0, bytesRead);
      return digest.digest();
    }
  }

  private static byte[] concatenateHashes(byte[]... hashes) {
    return Arrays.stream(hashes).reduce(new byte[0], Server::concatenate);
  }

  private static byte[] concatenate(byte[] a, byte[] b) {
    byte[] result = Arrays.copyOf(a, a.length + b.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }
}