package com.eureka.provider.controllers;import com.eureka.provider.models.Student;import com.eureka.provider.models.StudentDao;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.beans.factory.annotation.Value;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.GetMapping;import org.springframework.web.bind.annotation.RestController;/** * Created by DJ_ZJ. */@RestControllerpublic class StudentController {    @Value("${server.port}")    private String port;    @GetMapping("/findAll")    public ResponseEntity findAll(){        System.out.println("通过端口:"+port+" 查询所有学生信息");        Iterable<Student> iterable = studentDao.findAll();        ResponseEntity entity = new ResponseEntity(iterable,HttpStatus.OK);        return entity;    }    @Autowired    private StudentDao studentDao;}