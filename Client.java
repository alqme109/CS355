import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Client {

  private static final String HOST = "localhost";
  private static final int PORT = 12345;
  private static final int FILE_COUNT = 5;

  public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
    try (Socket socket = new Socket(HOST, PORT);
         DataOutputStream output = new DataOutputStream(socket.getOutputStream());
         DataInputStream input = new DataInputStream(socket.getInputStream())) {

      for (int i = 0; i < FILE_COUNT; i++) {
        String clientFilePath = "BobFiles/bob_file" + (i+1) + ".txt";
        byte[] fileHash = hashFileCombined(clientFilePath);
        output.write(fileHash);

        boolean filesMatch = input.readBoolean();
        System.out.println("File " + (i+1) + " matches: " + filesMatch);
      }
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
    return Arrays.stream(hashes).reduce(new byte[0], Client::concatenate);
  }

  private static byte[] concatenate(byte[] a, byte[] b) {
    byte[] result = Arrays.copyOf(a, a.length + b.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }
}