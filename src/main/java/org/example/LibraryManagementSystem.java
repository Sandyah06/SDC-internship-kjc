package org.example;
import com.mongodb.client.*;
import org.bson.Document;
import java.util.Scanner;

public class LibraryManagementSystem {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Connect to MongoDB
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase db = mongoClient.getDatabase("LibraryDB");
        MongoCollection<Document> books = db.getCollection("books");

        while (true) {
            System.out.println("\n--- Library Menu ---");
            System.out.println("1. Add Book");
            System.out.println("2. View All Books");
            System.out.println("3. Update Book by ISBN");
            System.out.println("4. Delete Book by ISBN");
            System.out.println("5. Exit");
            System.out.print("Choose: ");
            int choice = sc.nextInt(); sc.nextLine(); // consume newline

            if (choice == 1) {
                System.out.print("Title: ");
                String title = sc.nextLine();
                System.out.print("Author: ");
                String author = sc.nextLine();
                System.out.print("ISBN: ");
                String isbn = sc.nextLine();
                System.out.print("Type (Fiction/NonFiction): ");
                String type = sc.nextLine();
                System.out.print("Genre or Subject: ");
                String extra = sc.nextLine();

                Document doc = new Document("title", title)
                        .append("author", author)
                        .append("isbn", isbn)
                        .append("type", type.equalsIgnoreCase("Fiction") ? "FictionBook" : "NonFictionBook");

                if (type.equalsIgnoreCase("Fiction")) {
                    doc.append("genre", extra);
                } else {
                    doc.append("subject", extra);
                }

                books.insertOne(doc);
                System.out.println("Book added.");

            } else if (choice == 2) {
                FindIterable<Document> allBooks = books.find();
                for (Document book : allBooks) {
                    System.out.println(book.toJson());
                }

            } else if (choice == 3) {
                System.out.print("Enter ISBN to update: ");
                String isbn = sc.nextLine();
                System.out.print("New Title: ");
                String newTitle = sc.nextLine();

                books.updateOne(new Document("isbn", isbn), new Document("$set", new Document("title", newTitle)));
                System.out.println("Book updated.");

            } else if (choice == 4) {
                System.out.print("Enter ISBN to delete: ");
                String isbn = sc.nextLine();
                books.deleteOne(new Document("isbn", isbn));
                System.out.println("Book deleted.");

            } else if (choice == 5) {
                break;
            } else {
                System.out.println("Invalid choice.");
            }
        }

        mongoClient.close();
        sc.close();
        System.out.println("Goodbye!");
    }
}
