import java.net.*;

class Test {
    public int x = 1;

    public void func() {
        return;
    }

}

public class Client {

    public static void main(String[] args) {
        // try {
        // Socket socket = new Socket("127.0.0.1", 9559);
        // System.out.println("Connected");
        // socket.close();
        // } catch (Exception ex) {
        // System.out.println(ex.getMessage());
        // }

        System.out.println(new Test().func);
    }

}