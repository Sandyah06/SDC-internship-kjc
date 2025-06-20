package org.example.EmployeeManagementPortal;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class EmployeeManagementApp {

    private final MongoCollection<Document> collection;

    public EmployeeManagementApp(MongoCollection<Document> collection) {
        this.collection = collection;
        // Create unique index on email field
        collection.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
    }

    public static class Employee {
        ObjectId id;
        String name;
        String email;
        String department;
        List<String> skills;
        Date joiningDate;

        public Employee(String name, String email, String department, List<String> skills, Date joiningDate) {
            this.name = name;
            this.email = email;
            this.department = department;
            this.skills = skills;
            this.joiningDate = joiningDate;
        }

        Document toDocument() {
            Document doc = new Document();
            if (id != null) doc.append("_id", id);
            doc.append("name", name)
                    .append("email", email)
                    .append("department", department)
                    .append("skills", skills)
                    .append("joiningDate", joiningDate);
            return doc;
        }

        static Employee fromDocument(Document doc) {
            Employee e = new Employee(
                    doc.getString("name"),
                    doc.getString("email"),
                    doc.getString("department"),
                    (List<String>) doc.get("skills"),
                    doc.getDate("joiningDate")
            );
            e.id = doc.getObjectId("_id");
            return e;
        }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return "Employee{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    ", department='" + department + '\'' +
                    ", skills=" + skills +
                    ", joiningDate=" + sdf.format(joiningDate) +
                    '}';
        }
    }

    // 1. Add Employee
    public boolean addEmployee(Employee e) {
        if (collection.find(Filters.eq("email", e.email)).first() != null) {
            System.out.println("Employee with this email already exists.");
            return false;
        }
        collection.insertOne(e.toDocument());
        System.out.println("Employee added.");
        return true;
    }

    // 2. Update Employee
    public boolean updateEmployee(String email, Map<String, Object> fieldsToUpdate) {
        if (fieldsToUpdate.isEmpty()) return false;

        List<Bson> updates = fieldsToUpdate.entrySet().stream()
                .map(entry -> Updates.set(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        UpdateResult result = collection.updateOne(Filters.eq("email", email), Updates.combine(updates));
        if (result.getMatchedCount() == 0) {
            System.out.println("No employee found with email: " + email);
            return false;
        }
        System.out.println("Employee updated.");
        return true;
    }

    // 3. Delete Employee by email
    public boolean deleteEmployeeByEmail(String email) {
        DeleteResult result = collection.deleteOne(Filters.eq("email", email));
        if (result.getDeletedCount() == 0) {
            System.out.println("No employee found with email: " + email);
            return false;
        }
        System.out.println("Employee deleted.");
        return true;
    }

    // Delete Employee by MongoDB ObjectId
    public boolean deleteEmployeeById(String id) {
        ObjectId objId;
        try {
            objId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid ObjectId format.");
            return false;
        }
        DeleteResult result = collection.deleteOne(Filters.eq("_id", objId));
        if (result.getDeletedCount() == 0) {
            System.out.println("No employee found with ID: " + id);
            return false;
        }
        System.out.println("Employee deleted.");
        return true;
    }

    // 4. Search Employees
    public List<Employee> searchEmployees(String name, String department, String skill, Date fromDate, Date toDate) {
        List<Bson> filters = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            filters.add(Filters.regex("name", ".*" + name + ".*", "i"));
        }
        if (department != null && !department.isBlank()) {
            filters.add(Filters.eq("department", department));
        }
        if (skill != null && !skill.isBlank()) {
            filters.add(Filters.in("skills", skill));
        }
        if (fromDate != null && toDate != null) {
            filters.add(Filters.and(Filters.gte("joiningDate", fromDate), Filters.lte("joiningDate", toDate)));
        }

        Bson combinedFilter = filters.isEmpty() ? new Document() : Filters.and(filters);

        FindIterable<Document> docs = collection.find(combinedFilter);
        List<Employee> employees = new ArrayList<>();
        for (Document doc : docs) {
            employees.add(Employee.fromDocument(doc));
        }
        return employees;
    }

    // 5. List Employees with Pagination and Sorting
    public List<Employee> listEmployees(int page, int pageSize, String sortBy, boolean ascending) {
        Bson sortOrder;
        if ("joiningDate".equalsIgnoreCase(sortBy)) {
            sortOrder = ascending ? Sorts.ascending("joiningDate") : Sorts.descending("joiningDate");
        } else {
            // Default sort by name
            sortOrder = ascending ? Sorts.ascending("name") : Sorts.descending("name");
        }

        FindIterable<Document> docs = collection.find()
                .sort(sortOrder)
                .skip((page - 1) * pageSize)
                .limit(pageSize);

        List<Employee> employees = new ArrayList<>();
        for (Document doc : docs) {
            employees.add(Employee.fromDocument(doc));
        }
        return employees;
    }

    // 6. Department Statistics Aggregation
    public Map<String, Integer> getDepartmentStats() {
        List<Bson> pipeline = Collections.singletonList(
                Aggregates.group("$department", Accumulators.sum("count", 1))
        );

        AggregateIterable<Document> results = collection.aggregate(pipeline);
        Map<String, Integer> stats = new HashMap<>();
        for (Document doc : results) {
            stats.put(doc.getString("_id"), doc.getInteger("count"));
        }
        return stats;
    }

    // Helper method to parse skills from comma separated string
    private static List<String> parseSkills(String input) {
        if (input == null || input.isBlank()) return Collections.emptyList();
        String[] parts = input.split(",");
        List<String> skills = new ArrayList<>();
        for (String s : parts) {
            skills.add(s.trim());
        }
        return skills;
    }

    // CLI program entry point
    public static void main(String[] args) throws ParseException {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = client.getDatabase("employee_db");
            MongoCollection<Document> collection = database.getCollection("employees");

            EmployeeManagementApp app = new EmployeeManagementApp(collection);

            Scanner scanner = new Scanner(System.in);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            while (true) {
                System.out.println("\nEmployee Management Portal:");
                System.out.println("1. Add Employee");
                System.out.println("2. Update Employee");
                System.out.println("3. Delete Employee");
                System.out.println("4. Search Employees");
                System.out.println("5. List Employees (Paginated)");
                System.out.println("6. Department Statistics");
                System.out.println("7. Exit");
                System.out.print("Select option: ");

                String option = scanner.nextLine();

                switch (option) {
                    case "1":
                        System.out.print("Name: ");
                        String name = scanner.nextLine();
                        System.out.print("Email: ");
                        String email = scanner.nextLine();
                        System.out.print("Department: ");
                        String dept = scanner.nextLine();
                        System.out.print("Skills (comma separated): ");
                        String skillsInput = scanner.nextLine();
                        System.out.print("Joining Date (yyyy-MM-dd): ");
                        String dateInput = scanner.nextLine();

                        Date joiningDate;
                        try {
                            joiningDate = sdf.parse(dateInput);
                        } catch (ParseException e) {
                            System.out.println("Invalid date format.");
                            break;
                        }

                        Employee employee = new Employee(name, email, dept, parseSkills(skillsInput), joiningDate);
                        app.addEmployee(employee);
                        break;

                    case "2":
                        System.out.print("Email of employee to update: ");
                        String updateEmail = scanner.nextLine();

                        Map<String, Object> updates = new HashMap<>();
                        System.out.println("Leave input blank to skip a field.");

                        System.out.print("New Department: ");
                        String newDept = scanner.nextLine();
                        if (!newDept.isBlank()) updates.put("department", newDept);

                        System.out.print("New Skills (comma separated): ");
                        String newSkills = scanner.nextLine();
                        if (!newSkills.isBlank()) updates.put("skills", parseSkills(newSkills));

                        System.out.print("New Joining Date (yyyy-MM-dd): ");
                        String newDateInput = scanner.nextLine();
                        if (!newDateInput.isBlank()) {
                            try {
                                Date newDate = sdf.parse(newDateInput);
                                updates.put("joiningDate", newDate);
                            } catch (ParseException e) {
                                System.out.println("Invalid date format, skipping joiningDate update.");
                            }
                        }

                        app.updateEmployee(updateEmail, updates);
                        break;

                    case "3":
                        System.out.print("Delete by (1) Email or (2) ID? ");
                        String delOpt = scanner.nextLine();
                        if ("1".equals(delOpt)) {
                            System.out.print("Enter email: ");
                            String delEmail = scanner.nextLine();
                            app.deleteEmployeeByEmail(delEmail);
                        } else if ("2".equals(delOpt)) {
                            System.out.print("Enter employee ID: ");
                            String delId = scanner.nextLine();
                            app.deleteEmployeeById(delId);
                        } else {
                            System.out.println("Invalid option.");
                        }
                        break;

                    case "4":
                        System.out.print("Search by name (partial): ");
                        String searchName = scanner.nextLine();

                        System.out.print("Search by department: ");
                        String searchDept = scanner.nextLine();

                        System.out.print("Search by skill: ");
                        String searchSkill = scanner.nextLine();

                        System.out.print("Joining Date From (yyyy-MM-dd): ");
                        String fromDateStr = scanner.nextLine();

                        System.out.print("Joining Date To (yyyy-MM-dd): ");
                        String toDateStr = scanner.nextLine();

                        Date fromDate = null, toDate = null;
                        try {
                            if (!fromDateStr.isBlank()) fromDate = sdf.parse(fromDateStr);
                            if (!toDateStr.isBlank()) toDate = sdf.parse(toDateStr);
                        } catch (ParseException e) {
                            System.out.println("Invalid date format.");
                            break;
                        }

                        List<Employee> found = app.searchEmployees(
                                searchName.isBlank() ? null : searchName,
                                searchDept.isBlank() ? null : searchDept,
                                searchSkill.isBlank() ? null : searchSkill,
                                fromDate,
                                toDate
                        );

                        System.out.println("Search Results:");
                        for (Employee emp : found) {
                            System.out.println(emp);
                        }
                        break;

                    case "5":
                        System.out.print("Page number: ");
                        int page = Integer.parseInt(scanner.nextLine());
                        System.out.print("Sort by (name/joiningDate): ");
                        String sortBy = scanner.nextLine();
                        System.out.print("Ascending? (true/false): ");
                        boolean asc = Boolean.parseBoolean(scanner.nextLine());

                        List<Employee> pageResults = app.listEmployees(page, 5, sortBy, asc);
                        System.out.println("Page " + page + " results:");
                        for (Employee emp : pageResults) {
                            System.out.println(emp);
                        }
                        break;

                    case "6":
                        Map<String, Integer> stats = app.getDepartmentStats();
                        System.out.println("Employees per Department:");
                        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                            System.out.println(entry.getKey() + ": " + entry.getValue());
                        }
                        break;

                    case "7":
                        System.out.println("Exiting.");
                        scanner.close();
                        client.close();
                        return;

                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            }
        }
    }
}
