package com.taskmanagement.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserMapperIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void userMapperBeanIsAvailable() {
        assertThat(userMapper).isNotNull();
    }
}
