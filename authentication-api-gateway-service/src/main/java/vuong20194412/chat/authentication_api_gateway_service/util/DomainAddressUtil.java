package vuong20194412.chat.authentication_api_gateway_service.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class DomainAddressUtil {

    private final Environment env;

    @Autowired
    public DomainAddressUtil(Environment env) {
        this.env = env;
    }

    public String getDomainAddress() {
        if (Boolean.TRUE.equals(env.getProperty("development-mode", Boolean.class)))
            return InetAddress.getLoopbackAddress().getHostAddress() + ":" + env.getProperty("server.port");

        try {

            if (InetAddress.getLocalHost().isLoopbackAddress())
                return InetAddress.getLoopbackAddress().getHostAddress() + ":" + env.getProperty("server.port");

            return InetAddress.getLocalHost().getHostAddress() + ":" + env.getProperty("server.port");

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
