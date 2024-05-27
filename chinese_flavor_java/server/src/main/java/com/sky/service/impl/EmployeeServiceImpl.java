package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //将密码转换为MD5格式与数据库中的密码进行比对（数据库中密码以MD5加密后存储）
        password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 添加员工
     * @param employeeDTO
     */
    @Override
    public void sava(EmployeeDTO employeeDTO) {
        //将employeeDTO中的数据copy到employee对象，方便数据库操作
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);

        //补充employeeDTO中没有的属性
        //StatusConstant是一个自定义状态类
        employee.setStatus(StatusConstant.ENABLE);
        /**
         * 改为公共字段自动填充
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());
         */
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //调用Mapper层的方法，将employee存到数据库
        employeeMapper.insert(employee);
    }

    /**
     * 分页查询员工
     */
    @Override
    public Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize());

        Page<Employee> pages = employeeMapper.pageQuery(employeePageQueryDTO);

        return pages;
    }

    /**
     * 更改员工状态
     */
    @Override
    public void startOrStop(Integer status,Long id) {
        Employee employee = Employee.builder()
                .id(id)
                .status(status)
                .build();

        employeeMapper.update(employee);
    }

    /**
     * 编辑员工信息
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        BeanUtils.copyProperties(employeeDTO,employee);

        /*
         * 改为公共字段自动填充
         * employee.setUpdateTime(LocalDateTime.now());
         * employee.setUpdateUser(BaseContext.getCurrentId());
         */
        employeeMapper.update(employee);
    }

    /**
     * 根据id查询信息
     * @param id
     * @return
     */
    @Override
    public Employee selectEmployeeById(Long id) {
        Employee employee = employeeMapper.selectEmployeeById(id);
        employee.setPassword("*******");
        return employee;
    }

    /**
     * 修改密码
     */
    @Override
    public Result editPassword(PasswordEditDTO passwordEditDTO) {
        passwordEditDTO.setEmpId(BaseContext.getCurrentId());
        Employee employee = employeeMapper.selectEmployeeById(passwordEditDTO.getEmpId());

        String oldPassword = passwordEditDTO.getOldPassword();
        oldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes(StandardCharsets.UTF_8));


        if(oldPassword.equals(employee.getPassword())){

            Employee employee1 = new Employee();
            String newPassword = passwordEditDTO.getNewPassword();
            newPassword = DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8));
            employee1.setPassword(newPassword);
            employee1.setId(passwordEditDTO.getEmpId());
            employeeMapper.update(employee1);

            return Result.success();
        }else {
            return Result.error("旧密码错误");
        }
    }
}
