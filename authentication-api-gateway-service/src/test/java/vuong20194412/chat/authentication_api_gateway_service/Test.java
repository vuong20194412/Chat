package vuong20194412.chat.authentication_api_gateway_service;

import vuong20194412.chat.authentication_api_gateway_service.util.SettingUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class Test {

    public static void main(String[] args) {
        try {
            SettingUtil settingUtil = new SettingUtil();
            String email = (String) settingUtil.readSetting().get("test_email");
            System.out.println("Test email: " + email);

            String signInUrl = "http://localhost:8000/api/signin";
            String signInBody = "{\"password\": \"password\", \"email\": \"" + email + "\"}";
            HttpResponse<String> _signinResponse = signIn(signInUrl, signInBody);

            if (_signinResponse.statusCode() < 400) {
                String authorizationHeader = String.join(",", _signinResponse.headers().allValues( "Authorization"));
                if (authorizationHeader.isEmpty()) {
                    System.out.println("Miss AUTHORIZATION Header");
                    return;
                }
                String removeAccountUrl = "http://localhost:8000/api/account/remove";
                HttpResponse<String> removeAccountResponse = deleteAccount(removeAccountUrl, authorizationHeader);
                Scanner scanner = new Scanner(System.in);
            }

            String signupUrl = "http://localhost:8000/api/signup";
            String signupBody = "{\"password\": \"password\", \"email\": \"" + email + "\", \"fullname\": \"test_fullname\"}";
            HttpResponse<String> signupResponse = signUp(signupUrl, signupBody);

//            Thread.sleep(10000);
//            Scanner scanner = new Scanner(System.in);
//
//            System.out.println("Confirmed signup email:");
//            scanner.nextLine();

//            String signInUrl = "http://localhost:8000/api/signin";
//            String signInBody = "{\"password\": \"password\", \"email\": \"" + email + "\"}";
            HttpResponse<String> signinResponse = signIn(signInUrl, signInBody);

            //java.net.http.HttpHeaders has many headers and each header has List<String> from String value by split ","
            String authorizationHeader = String.join(",", signinResponse.headers().allValues( "Authorization"));
            if (authorizationHeader.isEmpty()) {
                System.out.println("Miss AUTHORIZATION Header");
                return;
            }

            String usersUrl = "http://localhost:8000/api/user";
            HttpResponse<String> usersResponse = getUsers(usersUrl, authorizationHeader);

            String logoutUrl = "http://localhost:8000/api/logout";
            HttpResponse<String> logoutResponse = logOut(logoutUrl, authorizationHeader);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    static HttpResponse<String> signUp(String signupUrl, String signupBody) throws IOException {
        HttpResponse<String> response = sendHttpPOST(signupUrl, Map.of("content-type", "application/json"), signupBody);
        System.out.println("SIGN UP: " + response.headers() + "\n" + response.body());
        return response;
    }

    static HttpResponse<String> signIn(String signInUrl, String signInBody) throws IOException {
        HttpResponse<String> response = sendHttpPOST(signInUrl, Map.of("content-type", "application/json"), signInBody);
        System.out.println("SIGN IN: " + response.headers() + "\n" + response.body());
        return response;
    }

    static HttpResponse<String> getUsers(String usersUrl, String authorizationHeader) throws IOException {
        HttpResponse<String> response = sendHttpGet(usersUrl, Map.of("authorization", authorizationHeader));
        System.out.println("GET USERS: " + response.headers() + "\n" + response.body());
        return response;
    }

    static HttpResponse<String> logOut(String logoutUrl, String authorizationHeader) throws IOException {
        HttpResponse<String> response = sendHttpPOST(logoutUrl, Map.of("authorization", authorizationHeader), null);
        System.out.println("LOG OUT: " + response.headers() + "\n" + response.body());
        return response;
    }

    static HttpResponse<String> deleteAccount(String removeAccountUrl, String authorizationHeader) throws IOException {
        HttpResponse<String> response = sendHttpDelete(removeAccountUrl, Map.of("authorization", authorizationHeader));
        System.out.println("DELETE ACCOUNT: " + response.headers() + "\n" + response.body());
        return response;
    }

    /**
     * java.net.http.HttpHeaders has many headers and each header has List&lt;String&gt; from String value by split ","
     * @param url notnull, valid url format
     * @param headers nullable. with each header, join with delimiter "," if header has many value
     * @return HttpResponse has body type is String
     * @throws IOException -
     */
    private static HttpResponse<String> sendHttpGet(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        setHeaders(connection, headers);

        Thread hook = new Thread(connection::disconnect);
        Runtime.getRuntime().addShutdownHook(hook);

        try {
            HttpResponse<String> httpResponse = createHttpResponse(connection);
            connection.disconnect();
            Runtime.getRuntime().removeShutdownHook(hook);
            return httpResponse;
        } catch (IOException ex) {
            connection.disconnect();
            throw new IOException(ex.getCause());
        }
    }

    /**
     * java.net.http.HttpHeaders has many headers and each header has List&lt;String&gt; from String value by split ","
     * @param url notnull, valid url format
     * @param headers nullable. with each header, join with delimiter "," if header has many value
     * @param body nullable
     * @return HttpResponse has body type is String
     * @throws IOException -
     */
    private static HttpResponse<String> sendHttpPOST(String url, Map<String, String> headers, /* nullable */String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        setHeaders(connection, headers);

        Thread hook = new Thread(connection::disconnect);
        Runtime.getRuntime().addShutdownHook(hook);

        if (body != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(body.getBytes(StandardCharsets.UTF_8));

            connection.setDoOutput(true);

            if (byteArrayOutputStream.size() > 0) {
                try (OutputStream outputStream = connection.getOutputStream()) {
                    byteArrayOutputStream.writeTo(outputStream);
                } catch (IOException ex) {
                    connection.disconnect();
                    Runtime.getRuntime().removeShutdownHook(hook);
                    throw new IOException(ex.getCause());
                }
            }
        }

        try {
            HttpResponse<String> httpResponse = createHttpResponse(connection);
            connection.disconnect();
            Runtime.getRuntime().removeShutdownHook(hook);
            return httpResponse;
        } catch (IOException ex) {
            connection.disconnect();
            throw new IOException(ex.getCause());
        }
    }

    private static HttpResponse<String> sendHttpDelete(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("DELETE");
        setHeaders(connection, headers);

        Thread hook = new Thread(connection::disconnect);
        Runtime.getRuntime().addShutdownHook(hook);

        try {
            HttpResponse<String> httpResponse = createHttpResponse(connection);
            connection.disconnect();
            Runtime.getRuntime().removeShutdownHook(hook);
            return httpResponse;
        } catch (IOException ex) {
            connection.disconnect();
            throw new IOException(ex.getCause());
        }
    }

    private static void setHeaders(/*Not null*/ HttpURLConnection connection, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (header.getKey() != null && header.getValue() != null && !header.getValue().isEmpty())
                    connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
    }

    private static HttpResponse<String> createHttpResponse(/*Not null*/ HttpURLConnection connection) throws IOException {
        String statusLine = connection.getHeaderField(0); // eg: HTTP/1.1 200
        String httpVersion = statusLine != null ? statusLine.split(" ")[0] : "";

        int responseCode = connection.getResponseCode();

        InputStream inputStream = connection.getInputStream();
        InputStream errorStream = connection.getErrorStream();
        String responseBody = new String((errorStream == null ? inputStream : errorStream).readAllBytes());

        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return responseCode;
            }

            @Override
            public java.net.http.HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                Map<String, java.util.List<String>> headers = new HashMap<>(connection.getHeaderFields());
                headers.remove(null);
                return HttpHeaders.of(headers, (headerName, headerValue) -> true);
            }

            @Override
            public String body() {
                return responseBody;
            }

            @Override
            public Optional<javax.net.ssl.SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                if (statusCode() == HttpURLConnection.HTTP_MOVED_PERM || statusCode() == HttpURLConnection.HTTP_MOVED_TEMP || statusCode() == HttpURLConnection.HTTP_SEE_OTHER) {
                    String url = connection.getHeaderField("Location");
                    return url != null ? URI.create(url) : null;
                }
                try {
                    return connection.getURL().toURI();
                } catch (URISyntaxException e) {
                    System.out.println("Test.java Line 193" + e.getMessage());
                    return null;
                }
            }

            @Override
            public java.net.http.HttpClient.Version version() {
                try {
                    return java.net.http.HttpClient.Version.valueOf(httpVersion);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        };
    }

}

