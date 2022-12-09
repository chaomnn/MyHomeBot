package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Database {
    private static final String URL = "jdbc:sqlite:apartments.db";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS Flats (id INTEGER PRIMARY KEY)";
    private static final String INSERT = "INSERT INTO Flats (id) VALUES (?)";
    private static final String SELECT = "SELECT id FROM Flats WHERE id =?";
    private static Database database = new Database();
    private Connection connection = null;

    private Database() {}

    public static Database getInstance() {
        if (database == null) {
            database = new Database();
        }
        return database;
    }

    public void connect() {
        try {
            if (connection == null) {
                connection = DriverManager.getConnection(URL);
                Statement statement = connection.createStatement();
                statement.execute(CREATE_TABLE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void insertData(int aptId) {
        if (connection != null) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT)) {
                preparedStatement.setInt(1, aptId);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    public boolean checkIfExists(int aptId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT)) {
            preparedStatement.setInt(1, aptId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")) + "]" +
                        " Apartment id already in DB, id is: " + resultSet.getInt("id"));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return false;
    }
}
