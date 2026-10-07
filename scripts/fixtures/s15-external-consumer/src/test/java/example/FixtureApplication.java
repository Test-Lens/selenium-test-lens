package example;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FixtureApplication {
    public static void main(String[] args)throws Exception{
        int port=Integer.parseInt(args[0]);HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
        AtomicBoolean changed=new AtomicBoolean();
        server.createContext("/change",exchange->{changed.set(true);byte[] body="changed".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});
        server.createContext("/",exchange->{String locator=changed.get()?"data-testid='login-submit'":"id='old-login-button'";byte[] html=("""
                <!doctype html><html><head><title>Login</title></head><body>
                <label for="username">Username</label><input id="username">
                <label for="password">Password</label><input id="password" type="password">
                <button %s aria-label="Log in" onclick="document.getElementById('error').hidden=false">Login</button>
                <div id="error" role="alert" hidden>Invalid password</div>
                </body></html>
                """.formatted(locator)).getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.sendResponseHeaders(200,html.length);exchange.getResponseBody().write(html);exchange.close();});
        server.start();System.out.println("fixture ready");Thread.currentThread().join();
    }
}
