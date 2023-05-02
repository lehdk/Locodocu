package dk.abandonship.businesslogic;

import dk.abandonship.dataaccess.CustomerDatabaseDAO;
import dk.abandonship.dataaccess.interfaces.ICustomerDAO;
import dk.abandonship.entities.Customer;
import dk.abandonship.entities.CustomerDTO;

import java.sql.SQLException;
import java.util.List;

public class CustomerManager {

    private final ICustomerDAO customerDAO;

    public CustomerManager() {
        customerDAO = new CustomerDatabaseDAO();
    }

    public List<Customer> getAllCustomers() throws SQLException {
        return customerDAO.getAllCustomers();
    }

    public Customer addCustomer(CustomerDTO customerDTO) throws SQLException {
        return customerDAO.addCustomer(customerDTO);
    }
}
