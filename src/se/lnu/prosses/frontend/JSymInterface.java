package se.lnu.prosses.frontend;

import java.io.IOException;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import se.lnu.prosses.configs.Configuration;
import se.lnu.prosses.core.ProjectHelper;
import se.lnu.prosses.utils.Utils;
//import se.lnu.prosses.frontend.JSymCompiler;


import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class JSymInterface {
    public static void main(String[] args) {
        try {
            // Instantiate the interface so we can use non-static methods/fields
            JSymInterface interfaceInstance = new JSymInterface();

            // Hand off the logic to an instance method
            interfaceInstance.init(args);

        } catch (Exception e) {
            System.err.println("Failed to initialize JSymInterface: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    /**
     * Non-static initialization logic
     */
    public void init(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            System.out.println("Starting in Webhook Mode...");

            // This is now an instance method call
            this.startWebhookServer(args);

            try {
                System.out.println("Server is idling... Press Ctrl+C to stop.");
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            // This is now an instance method call
            this.executeAnalysis(args);
        }
    }

    private void startWebhookServer(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 5000), 0);
        server.createContext("/webhook", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println(">>> DEBUG: REQUEST RECEIVED! Method: " + exchange.getRequestMethod());
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Read the body properly for Java 8
                    java.io.InputStream is = exchange.getRequestBody();
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[1024];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    byte[] bodyBytes = buffer.toByteArray();

                    String jsonBody = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
                    System.out.println(">>> Received Webhook Body: " + jsonBody);

                    //Send 200 OK back to Nexus immediately
                    String response = "Analysis Triggered";
                    exchange.sendResponseHeaders(200, response.length());
                    try (java.io.OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }

                    // Read the JSON from the request body
                    try {
                        String assetName = parseValueFromJson(jsonBody, "name");
                        String fullVersion = parseValueFromJson(jsonBody, "version");
                        String action = parseValueFromJson(jsonBody, "action");
                        System.out.println(">>> Checking asset: " + assetName);

                        // Handle delete event, stop it from attempting analysis/download on a file being deleted
                        if ("DELETED".equals(action)) {
                            System.out.println(">>> Component deleted. Ignoring.");
                            //exchange.sendResponseHeaders(200, 0); // Tell Nexus we received it -done earlier so maybe not needed
                            //exchange.getResponseBody().close();
                            return; // Stop here
                        }

                        // Filter for JAR files and handle the download
                        if (assetName.endsWith(".jar")) {
                            System.out.println(">>> Found JAR! Processing version: " + fullVersion);
                            // Construct URL manually if downloadUrl is missing in the JSON
                            String downloadUrl = parseValueFromJson(jsonBody, "downloadUrl");
                            if (downloadUrl.isEmpty()) {
                                // Fallback: Construct it from the name
                                downloadUrl = "http://nexus-server:8081/repository/symm-repo" + assetName;
                            }
                            // Download
                            String internalUrl = downloadUrl.replace("localhost", "nexus-server");
                            java.nio.file.Path targetPath = java.nio.file.Paths.get("/tmp/downloaded.jar");
                            downloadFileWithAuth(internalUrl, targetPath);
                            System.out.println("Download complete: ");

                            // Reconstruct the CLI args array
                            List<String> argList = new ArrayList<>();
                            // The first argument for your compiler is likely the path to the JAR
                            argList.add(targetPath.toString());

                            // Try to get args from the version field
                            String argsPart = "";
                            if (fullVersion != null && fullVersion.contains("--args--")) {
                                argsPart = fullVersion.split("--args--")[1];
                            }
                            // Fallback: Try to get args from the asset name/path if version was empty
                            else if (assetName != null && assetName.contains("--args--")) {
                                // Splits the path and looks for the segment containing '--args--'
                                for (String part : assetName.split("/")) {
                                    if (part.contains("--args--")) {
                                        argsPart = part.split("--args--")[1];
                                        break;
                                    }
                                }
                            }
                            // If we found arguments, parse them into the list
                            if (!argsPart.isEmpty()) {
                                java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^\"\\s]+|\"([^\"]*)\")").matcher(argsPart);
                                while (m.find()) {
                                    argList.add(m.group(2) != null ? m.group(2) : m.group(1));
                                }
                            }

                            String[] finalArgs = argList.toArray(new String[0]);
                            System.out.println(">>> Automated Analysis Triggered with Args: " + String.join(" ", finalArgs));
                            //Run the analysis
                            executeAnalysis(finalArgs); //Passing new argument array
                            System.out.println("Analysis finished!");
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing webhook: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    exchange.close();
                }
            }
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("Server is live. Listening for Nexus at http://analysis-engine:5000/webhook");
    }

    private void executeAnalysis(String[] args) {        //Can we just call JSymCompiler and do these bits?
        try {
            JSymCompiler jsym = new JSymCompiler();
            //JSymCompiler.main(args);    //Call JSymCompiler with the arguments
            // Instead of calling the static .main(), call the logic methods
            // (Changed buildConfigSetting and run to 'public'
            Configuration config = jsym.buildConfigSetting(args);
            // Initialize the project on the INSTANCE, not the class
            jsym.project = new ProjectHelper(config);
            // Run the analysis
            jsym.run(config);

            System.out.println("Analysis finished successfully.");
        } catch (Exception e) {
            Utils.log(JSymCompiler.class, "Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* Original version - doesn't handle sep argument list section
    private static String parseUrlFromJson(String json) {
        // \\s* handles any amount of whitespace (or none) around the colon
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"downloadUrl\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }
     */
    // Generic helper to extract any key from the Nexus JSON payload
    private static String parseValueFromJson(String json, String key) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }
    private void downloadFileWithAuth(String urlString, java.nio.file.Path target) throws Exception {
        System.out.println(">>> Attempting download from: " + urlString);

        java.net.URL url = new java.net.URL(urlString);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

        // 1. Setup Authentication (Using the Environment Variable logic for safety)
        String user = System.getenv("NEXUS_USER");
        String pass = System.getenv("NEXUS_PASS");

        // Fallback for your current testing if you haven't set up the Docker env yet
        if (user == null || user.isEmpty()) user = "admin";
        if (pass == null || pass.isEmpty()) pass = "newcastle98";

        String auth = user + ":" + pass;
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

        // 2. Check the response code
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            // 3. Save the file
            try (java.io.InputStream in = conn.getInputStream()) {
                java.nio.file.Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println(">>> Download complete! Saved to: " + target.toAbsolutePath());
        } else {
            throw new IOException("Failed to download file. Nexus returned HTTP " + responseCode);
        }
    }
}