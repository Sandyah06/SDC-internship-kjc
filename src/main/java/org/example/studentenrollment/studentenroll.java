package org.example.studentenrollment;


import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

public class studentenroll {

    private static final Scanner scanner = new Scanner(System.in);
    private static MongoCollection<Document> students;
    private static MongoCollection<Document> courses;
    private static MongoCollection<Document> enrollments;

    public static void main(String[] args) {
        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = mongoClient.getDatabase("student_enrollment");

            students = db.getCollection("students");
            courses = db.getCollection("courses");
            enrollments = db.getCollection("enrollments");

            System.out.println("Welcome to Student Enrollment Management System\n");

            boolean running = true;
            while (running) {
                printMenu();
                int choice = getIntInput("Enter your choice: ");
                switch (choice) {
                    case 1:
                        insertStudent();
                        break;
                    case 2:
                        insertCourse();
                        break;
                    case 3:
                        addEnrollment();
                        break;
                    case 4:
                        queryAndPrintEnrollments();
                        break;
                    case 5:
                        updateStudentName();
                        break;
                    case 6:
                        createStudentNameIndex();
                        break;
                    case 7:
                        System.out.println("Exiting application. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printMenu() {
        System.out.println("Menu:");
        System.out.println("1. Insert a student");
        System.out.println("2. Insert a course");
        System.out.println("3. Add enrollment (choose embedded or referenced)");
        System.out.println("4. Query and print all enrollments");
        System.out.println("5. Update a student's name");
        System.out.println("6. Create index on students.name");
        System.out.println("7. Exit");
    }

    private static void insertStudent() {
        String name = getStringInput("Enter student name: ");
        String email = getStringInput("Enter student email: ");
        Document student = new Document("_id", new ObjectId())
                .append("name", name)
                .append("email", email);
        students.insertOne(student);
        System.out.println("Inserted student: " + name);
    }

    private static void insertCourse() {
        String title = getStringInput("Enter course title: ");
        String description = getStringInput("Enter course description: ");
        Document course = new Document("_id", new ObjectId())
                .append("title", title)
                .append("description", description);
        courses.insertOne(course);
        System.out.println("Inserted course: " + title);
    }

    private static void addEnrollment() {
        List<Document> studentList = students.find().into(new ArrayList<>());
        List<Document> courseList = courses.find().into(new ArrayList<>());

        if (studentList.isEmpty() || courseList.isEmpty()) {
            System.out.println("Need at least one student and one course to add enrollment.");
            return;
        }

        System.out.println("Choose student:");
        for (int i = 0; i < studentList.size(); i++) {
            Document s = studentList.get(i);
            System.out.printf("%d. %s (email: %s)\n", i + 1, s.getString("name"), s.getString("email"));
        }
        int studentChoice = getIntInput("Select student number: ");
        if (studentChoice < 1 || studentChoice > studentList.size()) {
            System.out.println("Invalid student selection.");
            return;
        }
        Document selectedStudent = studentList.get(studentChoice - 1);

        System.out.println("Choose course:");
        for (int i = 0; i < courseList.size(); i++) {
            Document c = courseList.get(i);
            System.out.printf("%d. %s - %s\n", i + 1, c.getString("title"), c.getString("description"));
        }
        int courseChoice = getIntInput("Select course number: ");
        if (courseChoice < 1 || courseChoice > courseList.size()) {
            System.out.println("Invalid course selection.");
            return;
        }
        Document selectedCourse = courseList.get(courseChoice - 1);

        System.out.println("Enrollment type:");
        System.out.println("1. Embedded");
        System.out.println("2. Referenced");
        int typeChoice = getIntInput("Select enrollment type (1 or 2): ");
        Document enrollment;

        if (typeChoice == 1) {
            // Embedded
            enrollment = new Document("type", "embedded")
                    .append("student", selectedStudent)
                    .append("course", selectedCourse);
            enrollments.insertOne(enrollment);
            System.out.println("Added embedded enrollment.");
        } else if (typeChoice == 2) {
            // Referenced
            enrollment = new Document("type", "referenced")
                    .append("student", selectedStudent.getObjectId("_id"))
                    .append("course", selectedCourse.getObjectId("_id"));
            enrollments.insertOne(enrollment);
            System.out.println("Added referenced enrollment.");
        } else {
            System.out.println("Invalid enrollment type selection.");
        }
    }

    private static void queryAndPrintEnrollments() {
        List<Document> enrollmentList = enrollments.find().into(new ArrayList<>());
        if (enrollmentList.isEmpty()) {
            System.out.println("No enrollments found.");
            return;
        }

        System.out.println("=== Enrollment Details ===");
        for (Document enrollment : enrollmentList) {
            String type = enrollment.getString("type");
            System.out.println("\nType: " + type);

            if ("embedded".equals(type)) {
                Document embeddedStudent = (Document) enrollment.get("student");
                Document embeddedCourse = (Document) enrollment.get("course");
                System.out.println("Student (embedded): " + embeddedStudent.toJson());
                System.out.println("Course (embedded): " + embeddedCourse.toJson());
            } else if ("referenced".equals(type)) {
                ObjectId studentId = enrollment.getObjectId("student");
                ObjectId courseId = enrollment.getObjectId("course");

                Document refStudent = students.find(Filters.eq("_id", studentId)).first();
                Document refCourse = courses.find(Filters.eq("_id", courseId)).first();

                System.out.println("Student (referenced): " + (refStudent != null ? refStudent.toJson() : "Not found"));
                System.out.println("Course (referenced): " + (refCourse != null ? refCourse.toJson() : "Not found"));
            }
        }
    }

    private static void updateStudentName() {
        List<Document> studentList = students.find().into(new ArrayList<>());
        if (studentList.isEmpty()) {
            System.out.println("No students found.");
            return;
        }

        System.out.println("Students:");
        for (int i = 0; i < studentList.size(); i++) {
            Document s = studentList.get(i);
            System.out.printf("%d. %s (email: %s)\n", i + 1, s.getString("name"), s.getString("email"));
        }
        int choice = getIntInput("Select student number to update: ");
        if (choice < 1 || choice > studentList.size()) {
            System.out.println("Invalid selection.");
            return;
        }
        Document selectedStudent = studentList.get(choice - 1);

        String newName = getStringInput("Enter new name for " + selectedStudent.getString("name") + ": ");

        students.updateOne(Filters.eq("_id", selectedStudent.getObjectId("_id")),
                Updates.set("name", newName));

        System.out.println("Student's name updated.");

        System.out.println("\nNote:");
        System.out.println("- Referenced enrollments reflect this change immediately because they fetch student data live.");
        System.out.println("- Embedded enrollments still show the old name unless you update them manually.");

        queryAndPrintEnrollments();
    }

    private static void createStudentNameIndex() {
        students.createIndex(Indexes.ascending("name"));
        System.out.println("Created ascending index on 'name' field in students collection.");
    }

    private static int getIntInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Please enter a valid number: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // consume newline
        return value;
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}
