import java.util.List;

public class Expense {
    private String id;
    private String description;
    private double amount;
    private String paidBy;
    private String category;
    private List<String> participants;
    private String date;

    public Expense(String id, String description, double amount, String paidBy, 
                   String category, List<String> participants, String date) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.category = category;
        this.participants = participants;
        this.date = date;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getPaidBy() { return paidBy; }
    public String getCategory() { return category; }
    public List<String> getParticipants() { return participants; }
    public String getDate() { return date; }

    public String toJson() {
        StringBuilder partsJson = new StringBuilder("[");
        for (int i = 0; i < participants.size(); i++) {
            partsJson.append("\"").append(participants.get(i)).append("\"");
            if (i < participants.size() - 1) partsJson.append(",");
        }
        partsJson.append("]");

        return String.format(
            "{\"id\":\"%s\",\"description\":\"%s\",\"amount\":%.2f,\"paidBy\":\"%s\",\"category\":\"%s\",\"date\":\"%s\",\"participants\":%s}",
            escape(id), escape(description), amount, escape(paidBy), escape(category), escape(date), partsJson.toString()
        );
    }

    private String escape(String s) {
        return (s == null) ? "" : s.replace("\"", "\\\"");
    }
}

