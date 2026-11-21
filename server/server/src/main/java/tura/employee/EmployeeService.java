package tura.employee;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee findById(Integer id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Angajatul nu există"));
    }

    public Employee create(Employee employee) {
        employee.setId(null);
        return employeeRepository.save(employee);
    }

    public Employee update(Integer id, Employee employee) {
        findById(id);
        employee.setId(id);
        return employeeRepository.save(employee);
    }

    public void delete(Integer id) {
        findById(id);
        employeeRepository.deleteById(id);
    }
}