import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Server {
    private static final int PORT = 8080;
    private static final BillManager manager = new BillManager();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // --- REST API Endpoints ---
        server.createContext("/api/users", Server::handleUsers);
        server.createContext("/api/expenses", Server::handleExpenses);
        server.createContext("/api/balances", Server::handleBalances);
        server.createContext("/api/settle", Server::handleSettle);

        // --- Frontend Static Files (HTML, CSS, JavaScript) ---
        server.createContext("/", Server::handleStaticFiles);

        server.setExecutor(null); // Default executor
        server.start();

        System.out.println("==================================================================");
        System.out.println("🚀 Bill Splitter & Expense Sharing Server is RUNNING!");
        System.out.println("👉 Open Website in Browser: http://localhost:" + PORT);
        System.out.println("👉 REST API Base URL:      http://localhost:" + PORT + "/api/");
        System.out.println("==================================================================");
    }

    // --- Users API Handler ---
    private static void handleUsers(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        String method = ex.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            List<User> list = manager.getUsers();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                json.append(list.get(i).toJson());
                if (i < list.size() - 1) json.append(",");
            }
            json.append("]");
            sendJson(ex, 200, json.toString());
        } 
        else if ("POST".equals(method)) {
            String body = readBody(ex);
            Map<String, String> data = parseSimpleJson(body);
            String id = data.getOrDefault("id", "").trim();
            String name = data.getOrDefault("name", "").trim();
            String email = data.getOrDefault("email", "").trim();
            String phone = data.getOrDefault("phone", "").trim();

            if (id.isEmpty()) id = name.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (id.isEmpty() || name.isEmpty()) {
                sendJson(ex, 400, "{\"error\":\"Name and ID are required\"}");
                return;
            }

            boolean ok = manager.addUser(id, name, email, phone);
            if (ok) {
                sendJson(ex, 201, "{\"message\":\"User created\",\"id\":\"" + id + "\"}");
            } else {
                sendJson(ex, 400, "{\"error\":\"User ID already exists\"}");
            }
        } 
        else if ("DELETE".equals(method)) {
            String query = ex.getRequestURI().getQuery();
            String id = getQueryParam(query, "id");
            if (id != null && manager.deleteUser(id)) {
                sendJson(ex, 200, "{\"message\":\"User deleted\"}");
            } else {
                sendJson(ex, 404, "{\"error\":\"User not found\"}");
            }
        } 
        else if ("OPTIONS".equals(method)) {
            ex.sendResponseHeaders(204, -1);
        } else {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    // --- Expenses API Handler ---
    private static void handleExpenses(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        String method = ex.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            List<Expense> list = manager.getExpenses();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                json.append(list.get(i).toJson());
                if (i < list.size() - 1) json.append(",");
            }
            json.append("]");
            sendJson(ex, 200, json.toString());
        } 
        else if ("POST".equals(method)) {
            String body = readBody(ex);
            Map<String, String> data = parseSimpleJson(body);

            String desc = data.getOrDefault("description", "Bill");
            double amount = 0;
            try { amount = Double.parseDouble(data.getOrDefault("amount", "0")); } catch (Exception ignored) {}
            String paidBy = data.getOrDefault("paidBy", "");
            String category = data.getOrDefault("category", "General");
            
            // Parse participants array from raw JSON
            List<String> participants = parseArrayField(body, "participants");

            Expense exp = manager.addExpense(desc, amount, paidBy, category, participants);
            if (exp != null) {
                sendJson(ex, 201, exp.toJson());
            } else {
                sendJson(ex, 400, "{\"error\":\"Invalid expense data. Please check amount and participants.\"}");
            }
        } 
        else if ("DELETE".equals(method)) {
            String query = ex.getRequestURI().getQuery();
            String id = getQueryParam(query, "id");
            if (id != null && manager.deleteExpense(id)) {
                sendJson(ex, 200, "{\"message\":\"Expense deleted\"}");
            } else {
                sendJson(ex, 404, "{\"error\":\"Expense not found\"}");
            }
        } 
        else if ("OPTIONS".equals(method)) {
            ex.sendResponseHeaders(204, -1);
        } else {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    // --- Balances & Simplified Debts Handler ---
    private static void handleBalances(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        Map<String, Double> net = manager.getNetBalances();
        List<Map<String, Object>> simplified = manager.getSimplifiedDebts();

        StringBuilder json = new StringBuilder("{");

        // Net balances
        json.append("\"net\":{");
        int count = 0;
        for (Map.Entry<String, Double> entry : net.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            if (++count < net.size()) json.append(",");
        }
        json.append("},");

        // Simplified transactions
        json.append("\"simplified\":[");
        for (int i = 0; i < simplified.size(); i++) {
            Map<String, Object> tx = simplified.get(i);
            json.append(String.format("{\"from\":\"%s\",\"to\":\"%s\",\"amount\":%.2f}", 
                tx.get("from"), tx.get("to"), (Double) tx.get("amount")));
            if (i < simplified.size() - 1) json.append(",");
        }
        json.append("]}");

        sendJson(ex, 200, json.toString());
    }

    // --- Settle Debt Handler ---
    private static void handleSettle(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
            String body = readBody(ex);
            Map<String, String> data = parseSimpleJson(body);
            String from = data.getOrDefault("from", "");
            String to = data.getOrDefault("to", "");
            double amount = 0;
            try { amount = Double.parseDouble(data.getOrDefault("amount", "0")); } catch (Exception ignored) {}

            boolean ok = manager.settleUp(from, to, amount);
            if (ok) {
                sendJson(ex, 200, "{\"message\":\"Settlement recorded successfully\"}");
            } else {
                sendJson(ex, 400, "{\"error\":\"Invalid settlement data\"}");
            }
        } else if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
        } else {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    // --- Static Frontend File Handler ---
    private static void handleStaticFiles(HttpExchange ex) throws IOException {
        String pathStr = ex.getRequestURI().getPath();
        if (pathStr.equals("/") || pathStr.isEmpty()) {
            pathStr = "/index.html";
        }

        Path file = Paths.get("web" + pathStr);
        if (!Files.exists(file)) {
            file = Paths.get("BillSplitter/web" + pathStr);
        }

        if (Files.exists(file) && !Files.isDirectory(file)) {
            byte[] bytes = Files.readAllBytes(file);
            String mime = "text/plain";
            if (pathStr.endsWith(".html")) mime = "text/html; charset=utf-8";
            else if (pathStr.endsWith(".css")) mime = "text/css; charset=utf-8";
            else if (pathStr.endsWith(".js")) mime = "application/javascript; charset=utf-8";
            else if (pathStr.endsWith(".json")) mime = "application/json";

            ex.getResponseHeaders().set("Content-Type", mime);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        } else {
            String notFound = "<h1>404 Not Found</h1><p>File not found: " + pathStr + "</p>";
            ex.sendResponseHeaders(404, notFound.length());
            try (OutputStream os = ex.getResponseBody()) { os.write(notFound.getBytes()); }
        }
    }

    // --- Utility Methods ---
    private static void addCorsHeaders(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        json = json.trim().replace("{", "").replace("}", "");
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replace("\"", "");
                String v = kv[1].trim().replace("\"", "");
                map.put(k, v);
            }
        }
        return map;
    }

    private static List<String> parseArrayField(String json, String field) {
        List<String> list = new ArrayList<>();
        int idx = json.indexOf("\"" + field + "\"");
        if (idx == -1) return list;
        int start = json.indexOf("[", idx);
        int end = json.indexOf("]", start);
        if (start != -1 && end != -1) {
            String content = json.substring(start + 1, end).trim();
            if (!content.isEmpty()) {
                String[] parts = content.split(",");
                for (String p : parts) {
                    list.add(p.trim().replace("\"", ""));
                }
            }
        }
        return list;
    }

    private static String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String p : query.split("&")) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals(param)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}

