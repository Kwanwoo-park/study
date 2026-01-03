//package spring.study.entity.forbidden;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import spring.study.forbidden.entity.Forbidden;
//import spring.study.forbidden.entity.Status;
//import spring.study.forbidden.repository.ForbiddenRepository;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
//    connection = EmbeddedDatabaseConnection.H2)
//public class ForbiddenRepositoryTest {
//    @Autowired
//    ForbiddenRepository forbiddenRepository;
//
//    @Test
//    void update() {
//        // given
//        List<Forbidden> list = forbiddenRepository.findAll();
//
//        for (Forbidden f : list) {
//            System.out.println(f.getWord() + " " + f.getRisk() + " " + f.getStatus());
//        }
//
//        Status status = Status.EXAMINE;
//
//        List<Long> idList = new ArrayList<>();
//
//        idList.add(1L);
//        idList.add(2L);
//
//        // when
//        int result = forbiddenRepository.updateStatusInIdList(status, idList);
//
//        // then
//        System.out.println(result);
//
//        list = forbiddenRepository.findAll();
//
//        for (Forbidden f : list) {
//            System.out.println(f.getWord() + " " + f.getRisk() + " " + f.getStatus());
//        }
//    }
//}
