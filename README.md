# 💸 SplitEasy - Bill Splitter & Expense Sharing System
> **Programming in Java Mini Project**  
> *Full-Stack Architecture: Java REST API Backend + Vanilla JavaScript/HTML/CSS Frontend*

![Java](https://img.shields.io/badge/Backend-Java%20REST%20API-orange?logo=java)
![Frontend](https://img.shields.io/badge/Frontend-HTML5%20%2F%20CSS3%20%2F%20JavaScript-blue)
![Algorithm](https://img.shields.io/badge/Algorithm-Minimum%20Cash%20Flow%20(Greedy)-purple)
![Dependencies](https://img.shields.io/badge/Dependencies-Zero%20External%20Jars-success)

---

## 📌 1. Project Overview
**SplitEasy** is a complete web application designed for friends, roommates, and groups to share expenses, divide bills, calculate balances, and settle debts easily.

### Technology Stack:
- **Backend:** Pure Java (`com.sun.net.httpserver.HttpServer`) exposing clean REST API endpoints.
- **Frontend:** HTML5, Modern CSS3, and JavaScript communicating with the backend via `fetch()`.
- **Zero Dependencies:** Requires no Maven, Gradle, Node.js, or npm. Runs instantly on any computer with Java installed!

---

## 🎯 2. Key Features
1. **👥 Member Management:** Add friends with Name, User ID, Email, and Phone. Remove members as needed.
2. **➕ Split Bills Equally:** Select which friends participated in each expense. The app automatically calculates each person's exact share in real-time.
3. **🏷️ Categorization:** Organize bills by categories (Food & Dining 🍔, Travel & Fuel 🚗, Accommodation 🏠, Entertainment 🎬, Shopping 🛍️, General 📦).
4. **🧠 Smart Debt Minimization (Cash Flow Algorithm):** Eliminates circular borrowing using a greedy graph matching algorithm. Instead of multiple confusing payments between friends, it computes the minimum number of transactions (at most $N - 1$) required to settle all debts!
5. **👤 Real-time Net Balances:** Instantly view who gets money back (green) and who owes money (red).
6. **🤝 Settle Up Functionality:** Record repayments (via UPI, Cash, etc.) with 1-click recommendations from the algorithm.
7. **📜 Expense Ledger:** Complete audit trail of all bills with date, description, payer, amount, and delete options.

---

## 🏗️ 3. REST API Endpoints

| Method | Endpoint | Description | Request Body Example |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users` | List all friends | None |
| `POST` | `/api/users` | Add a new friend | `{"id":"rahul","name":"Rahul","email":"rahul@test.com","phone":"9876543210"}` |
| `DELETE` | `/api/users?id={id}` | Delete a friend | None |
| `GET` | `/api/expenses` | List all expenses | None |
| `POST` | `/api/expenses` | Add shared bill | `{"description":"Dinner","amount":1200,"paidBy":"rahul","category":"Food & Dining","participants":["rahul","priya"]}` |
| `DELETE` | `/api/expenses?id={id}`| Delete a bill | None |
| `GET` | `/api/balances` | Get net balances & simplified debts | None |
| `POST` | `/api/settle` | Settle payment between two users | `{"from":"priya","to":"rahul","amount":600.0}` |

---

## 📁 4. Project Directory Structure

```
BillSplitter/
│
├── src/                         # Java REST Backend Source Code
│   ├── User.java                # User data model & JSON serializer
│   ├── Expense.java             # Expense data model & JSON serializer
│   ├── BillManager.java         # Core business logic & Cash Flow Algorithm
│   └── Server.java              # Java HTTP Server & REST API Handlers
│
├── web/                         # Web Frontend (HTML, CSS, JavaScript)
│   ├── index.html               # Semantic HTML5 single-page application
│   ├── style.css                # Modern responsive UI styling
│   └── app.js                   # JavaScript fetch() logic & DOM renderer
│
├── .vscode/                     # VS Code Run Configurations
│   ├── launch.json
│   └── tasks.json
│
├── run.bat                      # Windows 1-Click Runner
├── .gitignore                   # Git Ignore File
└── README.md                    # Project Documentation
```

---

## 🚀 5. How to Run in VS Code

### Option 1: 1-Click Batch File (Easiest)
1. Open the `BillSplitter` folder in File Explorer or VS Code.
2. Double-click `run.bat`.
3. It will compile the Java code, start the REST server, and automatically open `http://localhost:8080` in your browser!

### Option 2: Using the Terminal
1. Open terminal in VS Code (`Ctrl + ~`).
2. Navigate to the `BillSplitter` directory:
   ```powershell
   cd BillSplitter
   ```
3. Compile all Java source files:
   ```powershell
   javac -d bin src/*.java
   ```
4. Start the server:
   ```powershell
   java -cp bin Server
   ```
5. Open your web browser and navigate to:
   ```
   http://localhost:8080
   ```

---

## 🌐 6. How to Upload to GitHub and Copy the Link

### Method 1: Using the GitHub Website (Quickest & No Git CLI Needed)
1. Go to [https://github.com](https://github.com) and sign in.
2. Click the **`+`** icon in the top-right corner and select **New repository**.
3. Repository name: `bill-splitter-and-expense-sharing`
4. Set visibility to **Public**.
5. Leave "Add a README file" **unchecked** (we already created this README!).
6. Click **Create repository**.
7. On the next screen, click **"uploading an existing file"**.
8. Drag and drop all files from your `BillSplitter` folder (excluding the `bin` folder) into the browser.
9. Click **Commit changes**.
10. Copy your repository link (e.g. `https://github.com/your-username/bill-splitter-and-expense-sharing`) to submit for your project!

---

### Method 2: Using Git Terminal
```bash
cd BillSplitter
git init
git add .
git commit -m "Initial commit: Bill Splitter & Expense Sharing Mini Project"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/bill-splitter-and-expense-sharing.git
git push -u origin main
```

---

## 🎓 7. Viva Voce Questions & Answers

**Q1: How does the Java backend communicate with the frontend?**  
*Answer:* The Java backend runs an HTTP server using `com.sun.net.httpserver.HttpServer` listening on port 8080. The frontend makes asynchronous HTTP requests using the JavaScript `fetch()` API to REST endpoints (`/api/users`, `/api/expenses`, `/api/balances`, etc.) and receives JSON responses.

**Q2: How does the Minimum Cash Flow Algorithm work?**  
*Answer:* It calculates each user's net balance: $\text{Net} = \text{Total Paid} - \text{Total Owed}$. It then greedily pairs the person who owes the most (maximum debtor) with the person who is owed the most (maximum creditor), settling $\min(\text{maxCredit}, -\text{maxDebit})$ between them. This reduces complex $N$-way debts to at most $N - 1$ direct transactions.

**Q3: Why is this project built with zero external JAR files?**  
*Answer:* By utilizing standard JDK classes, the project avoids dependency conflicts, Maven download errors, and version mismatch issues, ensuring it compiles and runs seamlessly on any system with Java installed.

---

## 📜 License
This project is open-source and free to use for educational, lab, and mini-project submissions.

