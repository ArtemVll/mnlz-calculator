package ru.spmi.mnlz.auth;

/** Разовая утилита: печатает UPDATE-строки с хэшами пароля start123. */
public class HashGen {
    public static void main(String[] args) {
        String password = "start123";
        String[][] users = {
                {"admin",    "7933551c56d26566"},
                {"petrov",   "9a6fa9a55f645db6"},
                {"sidorova", "857ea2a2b750dfc9"},
                {"guest",    "83b1626800f582e1"}
        };
        for (String[] u : users) {
            System.out.println("UPDATE users SET password_hash = '"
                    + AuthService.sha256Hex(u[1] + password)
                    + "' WHERE username = '" + u[0] + "';");
        }
    }
}
