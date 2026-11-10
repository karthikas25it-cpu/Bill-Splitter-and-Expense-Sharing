public class User {
    private String id;
    private String name;
    private String email;
    private String phone;

    public User(String id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    // Simple JSON serializer without any third-party jars
    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\"}",
            escape(id), escape(name), escape(email), escape(phone)
        );
    }

    private String escape(String s) {
        return (s == null) ? "" : s.replace("\"", "\\\"");
    }
}

