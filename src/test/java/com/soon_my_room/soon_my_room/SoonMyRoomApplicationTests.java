package com.soon_my_room.soon_my_room;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2 사용 명시 (기본값은 ANY)
class SoonMyRoomApplicationTests {

  @Test
  void contextLoads() {}
}
