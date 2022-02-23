import com.hynis.rpc.server.core.RpcNettyServer;
import com.hynis.rpc.server.core.Server;

/**
 * @author hynis
 * @date 2022/2/24 0:06
 */
public class ServerStart {
    public static void main(String[] args) {
        Server server = new RpcNettyServer();
        server.start();
    }
}
