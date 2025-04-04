package com.soon_my_room.soon_my_room;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2 사용 명시 (기본값은 ANY)
@TestPropertySource(locations = "classpath:application-test.properties")
class SoonMyRoomApplicationTests {

  @Test
  void contextLoads() {}
}
