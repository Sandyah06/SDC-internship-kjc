package org.example;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.util.Scanner;

class BankingException extends Exception {
    public BankingException(String message) {
        super(message);
    }
}

public class BankingSystem {

    private final MongoCollection<Document> accountsCollection;

    public BankingSystem() {
        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = client.getDatabase("banking_system");
        this.accountsCollection = database.getCollection("accounts");
    }

    public void createAccount(String accountNumber, String accountHolder, double initialBalance) throws BankingException {
        if (initialBalance < 0)
            throw new BankingException("Initial balance cannot be negative.");

        Document existing = accountsCollection.find(Filters.eq("accountNumber", accountNumber)).first();
        if (existing != null)
            throw new BankingException("Account with this number already exists.");

        Document account = new Document("accountNumber", accountNumber)
                .append("accountHolder", accountHolder)
                .append("balance", initialBalance);
        accountsCollection.insertOne(account);
        System.out.println("✅ Account created successfully.");
    }

    public void deposit(String accountNumber, double amount) throws BankingException {
        if (amount <= 0)
            throw new BankingException("Deposit amount must be positive.");

        Document account = getAccount(accountNumber);
        double newBalance = account.getDouble("balance") + amount;

        accountsCollection.updateOne(Filters.eq("accountNumber", accountNumber),
                new Document("$set", new Document("balance", newBalance)));
        System.out.println("✅ Deposit successful. New balance: $" + newBalance);
    }

    public void withdraw(String accountNumber, double amount) throws BankingException {
        if (amount <= 0)
            throw new BankingException("Withdrawal amount must be positive.");

        Document account = getAccount(accountNumber);
        double currentBalance = account.getDouble("balance");
        if (currentBalance < amount)
            throw new BankingException("Insufficient funds. Current balance: $" + currentBalance);

        double newBalance = currentBalance - amount;
        accountsCollection.updateOne(Filters.eq("accountNumber", accountNumber),
                new Document("$set", new Document("balance", newBalance)));
        System.out.println("✅ Withdrawal successful. New balance: $" + newBalance);
    }

    public void checkBalance(String accountNumber) throws BankingException {
        Document account = getAccount(accountNumber);
        System.out.println("💰 Account Holder: " + account.getString("accountHolder"));
        System.out.println("💳 Account Number: " + account.getString("accountNumber"));
        System.out.println("📄 Balance: $" + account.getDouble("balance"));
    }

    private Document getAccount(String accountNumber) throws BankingException {
        Document account = accountsCollection.find(Filters.eq("accountNumber", accountNumber)).first();
        if (account == null)
            throw new BankingException("Account not found with number: " + accountNumber);
        return account;
    }

    public static void main(String[] args) {
        BankingSystem bank = new BankingSystem();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n======= BANKING SYSTEM MENU =======");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Check Balance");
            System.out.println("5. Exit");
            System.out.print("Select an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();  // consume newline

            try {
                switch (choice) {
                    case 1:
                        System.out.print("Enter account number: ");
                        String accNum = scanner.nextLine();
                        System.out.print("Enter account holder name: ");
                        String holder = scanner.nextLine();
                        System.out.print("Enter initial balance: ");
                        double initBal = scanner.nextDouble();
                        bank.createAccount(accNum, holder, initBal);
                        break;
                    case 2:
                        System.out.print("Enter account number: ");
                        String depAcc = scanner.nextLine();
                        System.out.print("Enter deposit amount: ");
                        double depAmt = scanner.nextDouble();
                        bank.deposit(depAcc, depAmt);
                        break;
                    case 3:
                        System.out.print("Enter account number: ");
                        String witAcc = scanner.nextLine();
                        System.out.print("Enter withdrawal amount: ");
                        double witAmt = scanner.nextDouble();
                        bank.withdraw(witAcc, witAmt);
                        break;
                    case 4:
                        System.out.print("Enter account number: ");
                        String balAcc = scanner.nextLine();
                        bank.checkBalance(balAcc);
                        break;
                    case 5:
                        System.out.println("👋 Exiting. Goodbye!");
                        return;
                    default:
                        System.out.println("❌ Invalid choice. Please try again.");
                }
            } catch (BankingException e) {
                System.out.println("⚠️ Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("❌ Unexpected error occurred: " + e.getMessage());
            }
        }
    }
}
