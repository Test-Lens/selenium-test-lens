package example;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public final class FixtureApplication {
    public static void main(String[] args)throws Exception{
        int port=Integer.parseInt(args[0]);HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
        AtomicReference<String> mode=new AtomicReference<>("baseline");
        server.createContext("/change",exchange->{mode.set("changed");respond(exchange,"changed");});
        server.createContext("/ambiguous",exchange->{mode.set("ambiguous");respond(exchange,"ambiguous");});
        server.createContext("/product-defect",exchange->{mode.set("product-defect");respond(exchange,"product-defect");});
        server.createContext("/reset",exchange->{mode.set("baseline");respond(exchange,"baseline");});
        server.createContext("/",exchange->{String state=mode.get();boolean ambiguousMode="ambiguous".equals(state);String locator="baseline".equals(state)||"product-defect".equals(state)?"id='old-login-button'":ambiguousMode?"":"data-testid='sign-in-submit'";
            String loginAction="product-defect".equals(state)
                    ?"document.getElementById('session-state').dataset.authenticated='true'"
                    :"document.getElementById('error').hidden=false;document.getElementById('session-state').dataset.authenticated='false'";
            String primaryHandler=ambiguousMode?"":"onclick=\""+loginAction+"\"";
            String secondary=ambiguousMode
                    ?"<button class='account-action' aria-label='Log in'>Login</button>"
                    :"<button class='account-action' id='register-submit' aria-label='Register'>Register</button>";
            String handlers=ambiguousMode
                    ?"<script>const actions=document.querySelectorAll('.account-action');actions[0].addEventListener('click',()=>{"+loginAction+"});actions[1].addEventListener('click',()=>{document.body.dataset.route='register';});</script>"
                    :"";
            byte[] html=("""
                <!doctype html><html><head><title>Login</title></head><body>
                <label for="username">Username</label><input id="username">
                <label for="password">Password</label><input id="password" type="password">
                <button class="account-action" %s aria-label="Log in" %s>Login</button>
                %s
                %s
                <div id="error" role="alert" hidden>Invalid password</div>
                <div id="session-state" data-authenticated="false">anonymous</div>
                </body></html>
                """.formatted(locator,primaryHandler,secondary,handlers)).getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.sendResponseHeaders(200,html.length);exchange.getResponseBody().write(html);exchange.close();});
        server.start();System.out.println("fixture ready");Thread.currentThread().join();
    }
    private static void respond(com.sun.net.httpserver.HttpExchange exchange,String value)throws java.io.IOException{byte[] body=value.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();}
}
