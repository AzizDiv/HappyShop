package ci553.happyshop.systemSetup;

import ci553.happyshop.storageAccess.DatabaseRWFactory;
import ci553.happyshop.utility.StorageLocation;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The setDB class is responsible for resetting the database when the system is first initialized.
 * This class performs operations that delete and recreate the database tables, as well as insert
 * default values for a fresh start. Ensuring that everything is properly set up for the fresh database state
 *
 * WARNING: This class should only be used once when starting the system for the first time. It
 * will wipe all current data in the database and replace it with a fresh, predefined structure and data.
 *
 * Key operations:
 * 1. Deletes all existing tables in the database.
 * 2. Recreates the database tables based on the initial schema.
 * 3. Inserts default values into the newly created tables.
 * 4. Deletes all existing image files from the working image folder (images/).
 * 5. Copies all image files from the backup folder (images_resetDB/) into the working image folder.
 */

public class SetDatabase {

    // Use the shared database URL from the factory, appending `;create=true` to create the database if it doesn't exist
    private static final String dbURL = DatabaseRWFactory.dbURL + ";create=true";
    // the value is "jdbc:derby:happyShopDB;create=true"

    private static Path imageWorkingFolderPath = StorageLocation.imageFolderPath;
    private static Path imageBackupFolderPath = StorageLocation.imageResetFolderPath;

    private String[] tables = {"ProductTable", "UserTable"};
    // Updated to include both tables

    private static final Lock lock = new ReentrantLock();    // Create a global lock

    public static void main(String[] args) throws SQLException, IOException {
        SetDatabase setDB = new SetDatabase();
        setDB.clearTables(); // clear all tables in the tables array from database if they are existing
        setDB.initializeTable(); // create and initialize database and tables
        setDB.queryTableAfterInitilization();
        deleteFilesInFolder(imageWorkingFolderPath);
        copyFolderContents(imageBackupFolderPath, imageWorkingFolderPath);
    }

    // Deletes all existing tables in the database.
    private void clearTables() throws SQLException {
        lock.lock();  // 🔒 Lock first
        try (Connection con = DriverManager.getConnection(dbURL);
             Statement statement = con.createStatement()) {
            System.out.println("Database happyShopDB is connected successfully!");
            for (String table : tables) {
                try {
                    // Try to drop table directly
                    statement.executeUpdate("DROP TABLE " + table.toUpperCase());
                    System.out.println("Dropped table: " + table);
                } catch (SQLException e) {
                    if ("42Y55".equals(e.getSQLState())) {  // 42Y55 = Table does not exist
                        System.out.println("Table " + table + " does not exist. Skipping...");
                    }
                }
            }
        } finally {
            lock.unlock();  // 🔓 Always unlock in finally block
        }
    }

    // Recreates the database tables Inserts default values into the newly created tables.
    private void initializeTable() throws SQLException {
        lock.lock(); // Lock to ensure thread safety

        // Table creation and insert statements for ProductTable
        String[] iniTableSQL = {
                // Create ProductTable
                "CREATE TABLE ProductTable(" +
                        "productID CHAR(4) PRIMARY KEY," +
                        "description VARCHAR(100)," +
                        "unitPrice DOUBLE," +
                        "image VARCHAR(100)," +
                        "inStock INT," +
                        "CHECK (inStock >= 0)" +
                        ")",

                // Insert data into ProductTable
                "INSERT INTO ProductTable VALUES('0001', '40 inch TV', 269.00,'0001.jpg',100)",
                "INSERT INTO ProductTable VALUES('0002', 'DAB Radio', 29.99, '0002.jpg',100)",
                "INSERT INTO ProductTable VALUES('0003', 'Toaster', 19.99, '0003.jpg',100)",
                "INSERT INTO ProductTable VALUES('0004', 'Watch', 29.99, '0004.jpg',100)",
                "INSERT INTO ProductTable VALUES('0005', 'Digital Camera', 89.99, '0005.jpg',100)",
                "INSERT INTO ProductTable VALUES('0006', 'MP3 player', 7.99, '0006.jpg',100)",
                "INSERT INTO ProductTable VALUES('0007', 'USB drive', 6.99, '0007.jpg',100)",
                "INSERT INTO ProductTable VALUES('0008', 'USB2 drive', 7.99, '0008.jpg',100)",
                "INSERT INTO ProductTable VALUES('0009', 'USB3 drive', 8.99, '0009.jpg',100)",
                "INSERT INTO ProductTable VALUES('0010', 'USB4 drive', 9.99, '0010.jpg',100)",
                "INSERT INTO ProductTable VALUES('0011', 'USB5 drive', 10.99, '0011.jpg',100)",
                "INSERT INTO ProductTable VALUES('0012', 'USB6 drive', 10.99, '0011.jpg',100)",
        };

        try (Connection connection = DriverManager.getConnection(dbURL)) {
            System.out.println("Database happyShopDB is created successfully!");
            connection.setAutoCommit(false); // Disable auto-commit for the batch

            // Create UserTable first
            try (Statement userStatement = connection.createStatement()) {
                try {
                    userStatement.executeUpdate(
                            "CREATE TABLE UserTable (" +
                                    "userId INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                                    "username VARCHAR(50) UNIQUE NOT NULL, " +
                                    "passwordHash VARCHAR(200) NOT NULL, " +
                                    "role VARCHAR(20) NOT NULL, " +
                                    "createdAt TIMESTAMP" +
                                    ")");
                    System.out.println("UserTable created.");
                } catch (SQLException e) {
                    // If table already exists, Derby throws error; just skip
                    System.out.println("UserTable might already exist: " + e.getMessage());
                }
            }

            // Insert a seeded admin user
            try (PreparedStatement psInsert = connection.prepareStatement(
                    "INSERT INTO UserTable (username, passwordHash, role, createdAt) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
                String defaultAdminPassword = "admin123";
                String hashed = BCrypt.hashpw(defaultAdminPassword, BCrypt.gensalt(12));

                psInsert.setString(1, "admin");
                psInsert.setString(2, hashed);
                psInsert.setString(3, "ADMIN");
                psInsert.executeUpdate();
                System.out.println("Seeded admin user created (username=admin).");
            } catch (SQLException e) {
                // If admin exists already, skip insertion
                System.out.println("Admin user insertion skipped or failed: " + e.getMessage());
            }

            // Create ProductTable and insert data
            try (Statement statement = connection.createStatement()) {
                // First, create the table (DDL) - Execute this one separately from DML
                statement.executeUpdate(iniTableSQL[0]);  // Execute Create Table SQL

                // Prepare and execute the insert operations (DML)
                for (int i = 1; i < iniTableSQL.length; i++) {
                    statement.addBatch(iniTableSQL[i]);  // Add insert queries to batch
                }

                // Execute all the insert statements in the batch
                statement.executeBatch();
                connection.commit(); // Commit the transaction if everything was successful

                System.out.println("Table and data initialized successfully.");

            } catch (SQLException e) {
                connection.rollback(); // Rollback the transaction in case of an error
                System.err.println("Transaction rolled back due to an error!");
                e.printStackTrace();
            }
        } finally {
            lock.unlock(); // Ensure the lock is released after the operation
        }
    }

    private void queryTableAfterInitilization() throws SQLException {
        lock.lock();

        try (Connection connection = DriverManager.getConnection(dbURL)) {
            // Query ProductTable
            String sqlQuery = "SELECT * FROM ProductTable";

            System.out.println("-------------Product Information Below -----------------");
            String title = String.format("%-12s %-20s %-10s %-10s %s",
                    "productID",
                    "description",
                    "unitPrice",
                    "inStock",
                    "image");
            System.out.println(title);  // Print formatted output

            try (Statement stat = connection.createStatement()) {
                ResultSet resultSet = stat.executeQuery(sqlQuery);
                while (resultSet.next()) {
                    String productID = resultSet.getString("productID");
                    String description = resultSet.getString("description");
                    double unitPrice = resultSet.getDouble("unitPrice");
                    String image = resultSet.getString("image");
                    int inStock = resultSet.getInt("inStock");
                    String record = String.format("%-12s %-20s %-10.2f %-10d %s", productID, description, unitPrice, inStock, image);
                    System.out.println(record);  // Print formatted output
                }
            }

            // Also query UserTable to show admin user
            System.out.println("\n-------------User Information Below -----------------");
            try (Statement stat = connection.createStatement()) {
                ResultSet resultSet = stat.executeQuery("SELECT userId, username, role, createdAt FROM UserTable");
                while (resultSet.next()) {
                    int userId = resultSet.getInt("userId");
                    String username = resultSet.getString("username");
                    String role = resultSet.getString("role");
                    Timestamp createdAt = resultSet.getTimestamp("createdAt");
                    System.out.printf("User ID: %d, Username: %s, Role: %s, Created: %s%n",
                            userId, username, role, createdAt);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // Recursively deletes all files in a folder
    public static void deleteFilesInFolder(Path folder) throws IOException {
        if (Files.exists(folder)) {
            lock.lock();
            try {
                Files.walkFileTree(folder, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file); // delete individual files
                        return FileVisitResult.CONTINUE;
                    }
                });
                System.out.println("Deleted files in folder: " + folder);
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("Folder " + folder + " does not exist");
        }
    }

    // Copies all files from source folder to destination folder
    public static void copyFolderContents(Path source, Path destination) throws IOException {
        lock.lock();
        if (!Files.exists(source)) {
            throw new IOException("Source folder does not exist: " + source);
        }

        // Create destination folder if it doesn't exist
        if (!Files.exists(destination)) {
            Files.createDirectories(destination);
        }

        // Copy files from source folder to destination folder
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    Path targetFile = destination.resolve(file.getFileName());
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } finally {
            lock.unlock();
        }
        System.out.println("Copied files from: " + source + " → " + destination);
    }
}