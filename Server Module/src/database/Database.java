package Database;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Base64;

/**
 * To save the user's username/password on local database
 *
 * password will be encode before insert - base on base64
 */
public class Database {

    private Statement stmt;

    public Database() {
        try {
            connect();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void connect() throws ClassNotFoundException, SQLException {

        Class.forName("com.mysql.cj.jdbc.Driver");

        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/messenger", "root", "password");

        stmt = connection.createStatement();
    }

    public void addNewUser(String username, String password) throws SQLException, UnsupportedEncodingException {

        String encodedPassword = Base64.getUrlEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8.toString()));
//        System.out.println(encodedPassword.length());
        stmt.executeUpdate("INSERT INTO user VALUES(\"" + username + "\", \"" + encodedPassword + "\")");
    }

    public String getUserPass(String username) throws SQLException, UnsupportedEncodingException {

        ResultSet rs = stmt.executeQuery("SELECT password FROM user WHERE username = \"" + username + "\"");

        String pass = null;
        while (rs.next()) {
            pass = rs.getString("password");
        }
        byte[] decodedPassword = Base64.getUrlDecoder().decode(pass);
        return new String(decodedPassword, StandardCharsets.UTF_8.toString());
    }

    public boolean doesExist(String username) throws SQLException {

        ResultSet rs = stmt.executeQuery("SELECT password FROM user WHERE username = \"" + username + "\"");
        return rs.next();

//        return getUserPass(username) != null;
    }
}

