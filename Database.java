import java.sql.*;
import java.util.ArrayList;

public class Database {
    static ArrayList<String> Groups;
    public static Connection establishConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/myneighbour";
        String username = "root";
        String password = "";

        // Establishing the database connection
        Connection connection = DriverManager.getConnection(url, username, password);
        System.out.println("Connected to the database.");

        return connection;
    }

    public static void retrieveGroupNames() throws SQLException {
        Connection connection = establishConnection();

        // Creating a statement object to execute SQL queries
        Statement statement = connection.createStatement();

        // Executing the query to retrieve all values from the GroupName column
        String query = "SELECT GroupName FROM groups";
        ResultSet resultSet = statement.executeQuery(query);


        // Storing the retrieved values in a dynamic array of strings
        Groups = new ArrayList<>();
        while (resultSet.next()) {
            String groupName = resultSet.getString("GroupName");
            Groups.add(groupName);
        }

        // Closing the resources
        resultSet.close();
        statement.close();
        connection.close();
        System.out.println("Disconnected from the database.");
    }

    public static void createGroup(String groupName) throws SQLException {
        Connection connection = establishConnection();

        // Creating a prepared statement for the insert query
        String query = "INSERT INTO groups (GroupName) VALUES (?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, groupName);

        // Executing the insert query
       preparedStatement.executeUpdate();

        // Closing the resources
        preparedStatement.close();
        connection.close();
        System.out.println("Disconnected from the database.");
    }

    public static void deleteGroup(String groupName) throws SQLException {
        Connection connection = establishConnection();

        // Creating a prepared statement for the delete query
        String query = "DELETE FROM groups WHERE GroupName = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, groupName);

        // Executing the delete query
        preparedStatement.executeUpdate();

        // Closing the resources
        preparedStatement.close();
        connection.close();
        System.out.println("Disconnected from the database.");
    }
}
