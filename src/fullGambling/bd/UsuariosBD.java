package fullGambling.bd;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import at.favre.lib.crypto.bcrypt.BCrypt;
import fullGambling.User;


public class UsuariosBD {

    static final String DB_TABLE_NAME = "user";

    /**
     * Lista los usuarios de la base de datos
     */
    public static void listarUsuarios() {
        Connection conexion = Conexion.conectar();

        Statement sentencia;
        try {
            sentencia = conexion.createStatement();

            ResultSet resultado = sentencia.executeQuery("SELECT * FROM user");

            while (resultado.next()) {
                // Procesa los datos
                int id = resultado.getInt("id");
                String username = resultado.getString("username");
                //String password = resultado.getString("password");
                Timestamp createdAt = resultado.getTimestamp("created_at");

                // Procesa los datos
                System.out.println(
                        "ID: " + id + ", username: " + username + ", createdAt: " + createdAt);
            }

            resultado.close();
            sentencia.close();
            conexion.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Comprueba si un usuario y contraseña son correctos
     * 
     * @param username Usuario
     * @param password Contraseña
     * @return true si el usuario y contraseña son correctos
     */
    public static boolean loginUsuario(String username, String password) {
        boolean loginOk = false;
        Connection conexion = Conexion.conectar();

        try {
            PreparedStatement statement = conexion.prepareStatement("SELECT * FROM " + DB_TABLE_NAME + " WHERE username COLLATE utf8mb4_bin = ?");
            statement.setString(1, username);
            
            ResultSet resultado = statement.executeQuery();


            if (resultado.next()) {
                // Si existe el usuario valida la contraseña con BCrypt
                byte[] passwordHashed = resultado.getString("password").getBytes(StandardCharsets.UTF_8);
                BCrypt.Result resultStrict = BCrypt.verifyer(BCrypt.Version.VERSION_2Y).verifyStrict(
                        password.getBytes(StandardCharsets.UTF_8),
                        passwordHashed);
                loginOk = resultStrict.verified;
                loginOk = validarHash2Y(password, resultado.getString("password"));
                
            }

            resultado.close();
            statement.close();
            conexion.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loginOk;
    }


    private static boolean hasUser( String username ){
        Connection conexion = Conexion.conectar();
        Statement sentencia;

        try {
            PreparedStatement statement = conexion.prepareStatement("SELECT * FROM " + DB_TABLE_NAME + " WHERE username COLLATE utf8mb4_bin = ?");
            statement.setString(1, username);
            
            ResultSet resultado = statement.executeQuery();

            if ( resultado.next() ){
                statement.close();
                conexion.close();
                resultado.close();
                return true;
            }

            statement.close();
            conexion.close();
            resultado.close();

        }
        catch (SQLException e){
            e.printStackTrace();
        }

        return false;
            
    }

    /**
     * Cambia la contraseña de un usuario
     * 
     * @param username Usuario
     * @param password Nueva contraseña
     * @return true si se cambió la contraseña
     */
    public static boolean cambiarPassword(String username, String password) {
        boolean cambiarPassword = false;
        Connection conexion = Conexion.conectar();

        Statement sentencia;
        try {
            sentencia = conexion.createStatement();
            int resultado = sentencia.executeUpdate("UPDATE user SET password='" + generarStringHash2Y(password)
                    + "' WHERE username LIKE '" + username + "'");

            if (resultado == 1) {
                // Si se cambió la contraseña
                cambiarPassword = true;
            }

            sentencia.close();
            conexion.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cambiarPassword;
    }

    /*
     * FUNCIONES BCRYPT: generar hash y validar hash
     */
    /**
     * Valida un hash de BCrypt
     * 
     * @param password Contraseña en texto claro
     * @param hash2y   Hash de BCrypt
     * @return true si la contraseña es correcta
     */
    private static boolean validarHash2Y(String password, String hash2y) {
        return BCrypt.verifyer(BCrypt.Version.VERSION_2Y)
                .verifyStrict(password.getBytes(StandardCharsets.UTF_8),
                        hash2y.getBytes(StandardCharsets.UTF_8)).verified;
    }

    /**
     * Genera un hash de BCrypt
     * 
     * @param password Contraseña en texto claro
     * @return Hash de BCrypt
     */
    private static String generarStringHash2Y(String password) {
        char[] bcryptChars = BCrypt.with(BCrypt.Version.VERSION_2Y).hashToChar(13, password.toCharArray());
        return String.valueOf(bcryptChars);
    }

    /**
     * Inicia sesión de usuario
     * Solicita credenciales de inicio de sesión, y si son correctas devuelve el
     * nombre de usuario.
     * 
     * @return Usuario que ha iniciado sesión
     */
    public static User logInUser() {
        do {
            System.out.println("LOGIN DE USUARIO");
            System.out.print("Usuario: ");
            String userName = System.console().readLine();
            System.out.print("Contraseña: ");
            String password = new String(System.console().readPassword());
            
            
            boolean loginOk = false;
            Connection conexion = Conexion.conectar();
            ResultSet results;
            PreparedStatement statement;
    
            try {
                statement = conexion.prepareStatement("SELECT * FROM " + DB_TABLE_NAME + " WHERE username COLLATE utf8mb4_bin = ?");
                statement.setString(1, userName);
                
                results = statement.executeQuery();
    
    
                if (results.next()) {
                    // Si existe el usuario valida la contraseña con BCrypt
                    byte[] passwordHashed = results.getString("password").getBytes(StandardCharsets.UTF_8);
                    BCrypt.Result resultStrict = BCrypt.verifyer(BCrypt.Version.VERSION_2Y).verifyStrict(
                            password.getBytes(StandardCharsets.UTF_8),
                            passwordHashed);
                    loginOk = resultStrict.verified;
                    loginOk = validarHash2Y(password, results.getString("password"));
                    
                }

                if (loginOk) {
                    User user = new User( results.getInt("id"), userName, results.getInt("chips") );
    
                    results.close();
                    statement.close();
                    conexion.close();
    
                    return user;
                } 
                else {
                    System.out.println("Usuario o contraseña incorrectos");
                }
            } 
            catch (SQLException e) {
                e.printStackTrace();
            }


        } while (true);
    }


    /**
     * Crea un nuevo usuario
     * Solicita credenciales de nuevo usuario, y si se crea correctamente devuelve
     * el nombre del usuario
     * 
     * @return el nombre si se creó el usuario
     */
    public static User createUser() {

        String userName;
        boolean isUserNameDuplicated = false;

        do{
            System.out.print("Usuario: ");
            userName = System.console().readLine();

            if (hasUser(userName)){
                isUserNameDuplicated = true;
                System.out.println("El nombre de usuario "+userName+" no está disponible, intente con un nombre nuevo..");
            }

        }while(isUserNameDuplicated);

        System.out.print("Contraseña: ");
        String password = new String(System.console().readPassword());
        
        
        Connection conexion = Conexion.conectar();
        User user = null;

        try {
            String query = "INSERT INTO user (username, password) VALUES (?, ?)";
            String hashedPassword = generarStringHash2Y(password);
            
            PreparedStatement statement = conexion.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, userName);
            statement.setString(2, hashedPassword);
            
            statement.executeUpdate();

            ResultSet results = statement.getGeneratedKeys();

            if (results.next()){
                user = new User(results.getInt(1),userName,0);
            }

            results.close();
            statement.close();
            conexion.close();
            return user;

        } 
        catch (SQLException e) {
            e.printStackTrace();
            //System.out.println("Error al crear el usuario");
            return null;
        }
    }


    /**
     * Método principal de ejemplo
     */
    public static void main(String[] args) {
        System.out.println("\n*******************");
        System.out.println("GESTIÓN DE USUARIOS");
        System.out.println("*******************\n");
        System.out.println("BASE DE DATOS: " + Conexion.DATABASE + " en " + Conexion.HOST + ":" + Conexion.PORT);
        String opcion;
        User user;
        
        do {
            System.out.println();
            System.out.println("1. LISTADO DE USUARIOS:");
            System.out.println("2. CREACIÓN DE USUARIO");
            System.out.println("3. LOGIN DE USUARIO");
            System.out.println("4. CAMBIO DE CONTRASEÑA");
            System.out.println("0. SALIR");

            System.out.println();
            System.out.print("Opción: ");
            opcion = System.console().readLine();
            System.out.println();

            switch (opcion) {
                case "1":
                    listarUsuarios();
                    break;
                case "2":
                    user = createUser();
                    System.out.println(user != null ? " usuario "+user.getUserName()+" creado" : "Error al crear el usuario");
                    break;
                case "3":
                    System.out.println("LOGIN DE USUARIO");
                    System.out.print("Usuario: ");
                    String usuario = System.console().readLine();
                    System.out.print("Contraseña: ");
                    String password = new String(System.console().readPassword());
                    System.out.println(loginUsuario(usuario, password) ? "Login OK" : "Login KO");
                    break;
                case "4":
                    System.out.println("CAMBIO DE CONTRASEÑA");
                    System.out.print("Usuario: ");
                    String usuarioCambio = System.console().readLine();
                    System.out.print("Nueva contraseña: ");
                    String newPassword = new String(System.console().readPassword());
                    System.out.println(
                            cambiarPassword(usuarioCambio, newPassword) ? "Contraseña cambiada"
                                    : "Error al cambiar la contraseña");
                    break;
                case "0":
                    System.out.println("Hasta pronto...\n");
                    break;
                default:
                    System.out.println("Opción no válida");
                    break;
            }
        } while (!opcion.equals("0"));

    }

}
