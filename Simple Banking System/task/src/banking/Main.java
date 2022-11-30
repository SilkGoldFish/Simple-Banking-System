package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank(args);
        bank.menu();
    }
}

class DataBaseManager {
    final private SQLiteDataSource dataSource;
    DataBaseManager(String[] args) {
        dataSource = new SQLiteDataSource();
        if (args.length == 2 && args[0].equals("-fileName")) {
            dataSource.setUrl("jdbc:sqlite:" + args[1]);
        } else {
            dataSource.setUrl("jdbc:sqlite:bank.db");
        }
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS card (id INTEGER, number TEXT, pin TEXT, balance INTEGER DEFAULT 0);");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean exists(String number) {
        String query = "SELECT * FROM card WHERE number = ?";
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
                preparedStatement.setString(1, number);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public void insert(Card card) {
        String insert = "INSERT INTO card VALUES (?, ?, ?, 0)";
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement(insert)) {
                preparedStatement.setInt(1, card.getId());
                preparedStatement.setString(2, card.getNumber());
                preparedStatement.setString(3, card.getPin());
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(String number, int income) {
        String update = "UPDATE card SET balance = balance + ? where number = ?";
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement(update)) {
                preparedStatement.setInt(1, income);
                preparedStatement.setString(2, number);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean check(String number, String pin) {
        String query = "SELECT * FROM card WHERE number = ? and pin = ?";
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
                preparedStatement.setString(1, number);
                preparedStatement.setString(2, pin);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void delete(String number) {
        String delete = "DELETE FROM card where number = ?";
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement(delete)) {
                preparedStatement.setString(1, number);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int query(String number) {
        String query = "SELECT balance FROM card where number = ?";
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
                preparedStatement.setString(1, number);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getInt("balance");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void transfer(String number, String toNumber, int amount) {
        String update = "UPDATE card SET balance = balance + ? WHERE number = ?";
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement sqlStatement = con.prepareStatement(update)) {
                //
                sqlStatement.setInt(1, -amount);
                sqlStatement.setString(2, number);
                sqlStatement.executeUpdate();
                //
                sqlStatement.setInt(1, amount);
                sqlStatement.setString(2, toNumber);
                sqlStatement.executeUpdate();
                //
                con.commit();
            } catch (SQLException e) {
                try {
                    System.err.print("Transaction is being rolled back");
                    con.rollback();
                } catch (SQLException exc) {
                    exc.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
class Bank {

    final private DataBaseManager dataBaseManager;

    Bank(String[] args) {
        this.dataBaseManager = new DataBaseManager(args);
    }
    public void menu() {
        Scanner scanner = new Scanner(System.in);
        printMenuBeforeLogIn();
        int mode = scanner.nextInt();
        while (mode != 0) {
            if (mode == 1) {
                createAnAccount();
            } else {
                if (!logIntoAccount()) {
                    break;
                }
            }
            printMenuBeforeLogIn();
            mode = scanner.nextInt();
        }
        System.out.println("Bye!");
    }

    private void printMenuBeforeLogIn() {
        System.out.println("1. Create an account");
        System.out.println("2. Log into account");
        System.out.println("0. Exit");
    }
    private void createAnAccount() {
        Card card = new Card();
        while (dataBaseManager.exists(card.getNumber())) {
            card = new Card();
        }
        dataBaseManager.insert(card);
    }

    private boolean logIntoAccount() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your card number:");
        String number = scanner.next();
        System.out.println("Enter your PIN:");
        String pin = scanner.next();
        if (Luhn.check(number)) {
            if (dataBaseManager.check(number, pin)) {
                System.out.println("You have successfully logged in!");
                return transaction(number);
            }
        }
        System.out.println("Wrong card number or PIN!");
        return true;
    }

    private void printMenuAfterLogIn() {
        System.out.println("1. Balance");
        System.out.println("2. Add income");
        System.out.println("3. Do transfer");
        System.out.println("4. Close account");
        System.out.println("5. Log out");
        System.out.println("0. Exit");
    }

    private void getBalance(String number) {
        int balance = dataBaseManager.query(number);
        System.out.println("Balance: " + balance);
    }
    private void addIncome(String number) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter income:");
        int income = scanner.nextInt();
        dataBaseManager.update(number, income);
        System.out.println("Income was added!");
    }

    private void doTransfer(String number) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter card number:");
        String toNumber = scanner.next();
        if (!Luhn.check(toNumber)) {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
        } else if (number.equals(toNumber)) {
            System.out.println("You can't transfer money to the same account!");
        } else if (!dataBaseManager.exists(toNumber)) {
            System.out.println("Such a card does not exist.");
        } else {
            System.out.println("Enter how much money you want to transfer:");
            int amount = scanner.nextInt();
            int balance = dataBaseManager.query(number);
            if (amount > balance) {
                System.out.println("Not enough money!");
            } else {
                dataBaseManager.transfer(number, toNumber, amount);
                System.out.println("Success!");
            }
        }
    }

    private void closeAccount(String number) {
        dataBaseManager.delete(number);
        System.out.println("The account has been closed!");
    }
    private boolean transaction(String number) {
        Scanner scanner = new Scanner(System.in);
        printMenuAfterLogIn();
        int mode = scanner.nextInt();
        while (mode != 0) {
            switch (mode) {
                case 1 -> getBalance(number);
                case 2 -> addIncome(number);
                case 3 -> doTransfer(number);
                case 4 -> {
                    closeAccount(number);
                    return true;
                }
                case 5 -> {
                    System.out.println("You have successfully logged out!");
                    return true;
                }
                default -> {
                }
            }
            printMenuAfterLogIn();
            mode = scanner.nextInt();
        }
        return false;
    }
}


class Luhn {
    public static String generate(String number) {
        int count = 0;
        for (int i = 0; i < number.length(); i++) {
            int digit = number.charAt(i) - '0';
            if (i % 2 == 0) {
                digit *= 2;
            }
            count += digit > 9? digit - 9: digit;
        }
        count = count % 10 == 0? 0: 10 - count % 10;
        return number + count;
    }

    public static boolean check(String number) {
        return number.equals(generate(number.substring(0, number.length() - 1)));
    }
}

class Card {
    final private String number;  //BIN(400000) + CAN(*********) + checksum(*)
    final private String pin;
    final private int id;

    Card() {
        StringBuilder id = new StringBuilder();
        StringBuilder pin = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 9; i++) {
            id.append(random.nextInt(10));
        }
        this.id = Integer.parseInt(id.toString());
        this.number = Luhn.generate("400000" + id.toString());    //append checksum
        for (int i = 0; i < 4; i++) {
            pin.append(random.nextInt(10));
        }
        this.pin = pin.toString();
        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(this.number);
        System.out.println("Your card PIN:");
        System.out.println(this.pin);
    }

    public String getNumber() {
        return number;
    }

    public String getPin() {
        return pin;
    }

    public int getId() {
        return id;
    }

}
