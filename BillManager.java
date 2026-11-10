import java.time.LocalDate;
import java.util.*;

public class BillManager {
    private final List<User> users = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();
    private int expenseCounter = 1;

    public BillManager() {
        loadSampleData();
    }

    // --- User Operations ---
    public synchronized boolean addUser(String id, String name, String email, String phone) {
        for (User u : users) {
            if (u.getId().equalsIgnoreCase(id)) return false; // already exists
        }
        users.add(new User(id, name, email, phone));
        return true;
    }

    public synchronized List<User> getUsers() {
        return new ArrayList<>(users);
    }

    public synchronized boolean deleteUser(String id) {
        boolean removed = users.removeIf(u -> u.getId().equalsIgnoreCase(id));
        if (removed) {
            expenses.removeIf(e -> e.getPaidBy().equalsIgnoreCase(id) || e.getParticipants().contains(id));
        }
        return removed;
    }

    // --- Expense Operations ---
    public synchronized Expense addExpense(String description, double amount, String paidBy, 
                                           String category, List<String> participants) {
        if (amount <= 0 || participants == null || participants.isEmpty()) {
            return null;
        }
        String id = "EXP-" + (expenseCounter++);
        String today = LocalDate.now().toString();
        Expense exp = new Expense(id, description, amount, paidBy, category, participants, today);
        expenses.add(exp);
        return exp;
    }

    public synchronized List<Expense> getExpenses() {
        return new ArrayList<>(expenses);
    }

    public synchronized boolean deleteExpense(String id) {
        return expenses.removeIf(e -> e.getId().equalsIgnoreCase(id));
    }

    // --- Settlement Operation ---
    public synchronized boolean settleUp(String fromUser, String toUser, double amount) {
        if (amount <= 0 || fromUser.equalsIgnoreCase(toUser)) return false;
        List<String> part = Collections.singletonList(toUser);
        addExpense("Settlement (" + fromUser + " -> " + toUser + ")", amount, fromUser, "Settlement", part);
        return true;
    }

    // --- Calculate Net Balances ---
    // Net Balance = (Total money spent by user) - (Total money user owes for their shares)
    // Positive (+) means user should RECEIVE money.
    // Negative (-) means user OWES money.
    public synchronized Map<String, Double> getNetBalances() {
        Map<String, Double> net = new LinkedHashMap<>();
        for (User u : users) {
            net.put(u.getId(), 0.0);
        }

        for (Expense exp : expenses) {
            String payer = exp.getPaidBy();
            double amt = exp.getAmount();
            List<String> parts = exp.getParticipants();
            if (parts.isEmpty()) continue;

            double share = Math.round((amt / parts.size()) * 100.0) / 100.0;

            // Payer gets credit
            net.put(payer, net.getOrDefault(payer, 0.0) + amt);

            // Each participant owes their share
            for (String p : parts) {
                net.put(p, net.getOrDefault(p, 0.0) - share);
            }
        }

        // Clean up 2 decimal precision
        for (Map.Entry<String, Double> entry : net.entrySet()) {
            entry.setValue(Math.round(entry.getValue() * 100.0) / 100.0);
        }
        return net;
    }

    // --- Debt Simplification: Minimum Cash Flow Algorithm ---
    // Greedily matches the biggest debtor with the biggest creditor.
    // Minimizes transactions from N*(N-1)/2 down to at most N-1!
    public synchronized List<Map<String, Object>> getSimplifiedDebts() {
        Map<String, Double> net = new HashMap<>(getNetBalances());
        List<Map<String, Object>> settlements = new ArrayList<>();

        while (true) {
            String maxDebtor = null;
            String maxCreditor = null;
            double maxDebit = 0.0;
            double maxCredit = 0.0;

            for (Map.Entry<String, Double> entry : net.entrySet()) {
                double val = entry.getValue();
                if (val < maxDebit) {
                    maxDebit = val;
                    maxDebtor = entry.getKey();
                }
                if (val > maxCredit) {
                    maxCredit = val;
                    maxCreditor = entry.getKey();
                }
            }

            if (maxDebtor == null || maxCreditor == null || maxCredit < 0.01 || maxDebit > -0.01) {
                break; // All debts settled!
            }

            double settleAmount = Math.min(maxCredit, -maxDebit);
            settleAmount = Math.round(settleAmount * 100.0) / 100.0;

            if (settleAmount > 0.009) {
                Map<String, Object> tx = new LinkedHashMap<>();
                tx.put("from", maxDebtor);
                tx.put("to", maxCreditor);
                tx.put("amount", settleAmount);
                settlements.add(tx);
            }

            net.put(maxCreditor, Math.round((maxCredit - settleAmount) * 100.0) / 100.0);
            net.put(maxDebtor, Math.round((maxDebit + settleAmount) * 100.0) / 100.0);
        }

        return settlements;
    }

    // Preloaded Demo Data for immediate demonstration
    private void loadSampleData() {
        addUser("rahul", "Rahul Sharma", "rahul@example.com", "9876543210");
        addUser("priya", "Priya Patel", "priya@example.com", "9876543211");
        addUser("amit", "Amit Verma", "amit@example.com", "9876543212");
        addUser("sneha", "Sneha Rao", "sneha@example.com", "9876543213");

        List<String> allFour = Arrays.asList("rahul", "priya", "amit", "sneha");
        addExpense("Resort Villa Stay", 4800.0, "rahul", "Accommodation", allFour);
        addExpense("Dinner at Fisherman's Wharf", 2400.0, "priya", "Food & Dining", allFour);
        addExpense("Rental Car & Petrol", 2000.0, "amit", "Travel & Fuel", allFour);
    }
}

