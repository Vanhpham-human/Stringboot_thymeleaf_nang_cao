package com.example.library.integration;

import com.example.library.dto.form.BorrowingForm;
import com.example.library.dto.form.BorrowingItemForm;
import com.example.library.entity.Book;
import com.example.library.entity.Category;
import com.example.library.entity.FinePolicy;
import com.example.library.entity.Member;
import com.example.library.entity.enums.BookStatus;
import com.example.library.entity.enums.MemberStatus;
import com.example.library.repository.BookRepository;
import com.example.library.repository.CategoryRepository;
import com.example.library.repository.FinePolicyRepository;
import com.example.library.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Nền chung cho integration test: Spring context thật + H2. Không dùng @Transactional ở mức test
 * để mỗi lời gọi service tự commit / rollback như khi chạy thật.
 */
@SpringBootTest
@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected BookRepository bookRepository;
    @Autowired
    protected MemberRepository memberRepository;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected FinePolicyRepository finePolicyRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    protected Category category;

    @BeforeEach
    void prepareFixtures() {
        cleanDatabase();
        Category c = new Category();
        c.setName("Thể loại test " + SEQUENCE.incrementAndGet());
        category = categoryRepository.save(c);

        FinePolicy policy = new FinePolicy("Chính sách test", BigDecimal.valueOf(5000), null, 0);
        policy.setActive(true);
        finePolicyRepository.save(policy);
    }

    @AfterEach
    void cleanUp() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        for (String table : List.of("fine_payments", "fine_waivers", "borrowing_details", "borrowings",
                "books", "categories", "members", "fine_policies")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    protected Book book(String title, int total, int available) {
        Book book = new Book();
        int seq = SEQUENCE.incrementAndGet();
        book.setIsbn("978%010d".formatted(seq));
        book.setTitle(title);
        book.setAuthor("Tác giả test");
        book.setCategory(category);
        book.setTotalQuantity(total);
        book.setAvailableQuantity(available);
        book.setStatus(BookStatus.ACTIVE);
        return bookRepository.save(book);
    }

    protected Member member(String name) {
        Member member = new Member();
        member.setFullName(name);
        member.setEmail("member" + SEQUENCE.incrementAndGet() + "@example.com");
        member.setStatus(MemberStatus.ACTIVE);
        return memberRepository.save(member);
    }

    protected int available(Book book) {
        return bookRepository.findById(book.getId()).orElseThrow().getAvailableQuantity();
    }

    protected static BorrowingForm borrowingForm(Member member, LocalDate borrowDate, LocalDate dueDate,
                                                 BorrowingItemForm... items) {
        BorrowingForm form = new BorrowingForm();
        form.setMemberId(member.getId());
        form.setBorrowDate(borrowDate);
        form.setDueDate(dueDate);
        form.setItems(List.of(items));
        return form;
    }

    protected static BorrowingItemForm item(Book book, int quantity) {
        return new BorrowingItemForm(book.getId(), quantity);
    }
}
