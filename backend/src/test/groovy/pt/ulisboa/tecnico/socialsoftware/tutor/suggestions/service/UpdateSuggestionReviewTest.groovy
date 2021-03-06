package pt.ulisboa.tecnico.socialsoftware.tutor.suggestions.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import pt.ulisboa.tecnico.socialsoftware.tutor.course.Course
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseExecution
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseRepository
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.tutor.suggestions.SuggestionReviewService
import pt.ulisboa.tecnico.socialsoftware.tutor.suggestions.domain.Suggestion
import pt.ulisboa.tecnico.socialsoftware.tutor.suggestions.domain.SuggestionReview
import pt.ulisboa.tecnico.socialsoftware.tutor.suggestions.dto.SuggestionReviewDto
import pt.ulisboa.tecnico.socialsoftware.tutor.suggestions.repository.SuggestionRepository
import pt.ulisboa.tecnico.socialsoftware.tutor.suggestions.repository.SuggestionReviewRepository
import pt.ulisboa.tecnico.socialsoftware.tutor.user.User
import pt.ulisboa.tecnico.socialsoftware.tutor.user.UserRepository
import spock.lang.Specification

@DataJpaTest
class UpdateSuggestionReviewTest extends Specification {
    public static final String COURSE_NAME = "Software Architecture"
    public static final String ACRONYM = "AS1"
    public static final String ACADEMIC_TERM = "1 SEM"
    public static final String QUESTION_TITLE = 'a question title'
    public static final String QUESTION_CONTENT = 'a question content'
    public static final String OPTION_CONTENT = "option id content"
    public static final String STUDENT_NAME = "Anonymous User"
    public static final String STUDENT_USERNAME = "anon"
    public static final String TEACHER_NAME = "Teacher"
    public static final String TEACHER_USERNAME = "teacher"
    public static final String SUGGESTION_REVIEW_JUSTIFICATION = "suggestion review justification"
    public static final String CHANGED_SUGGESTION_REVIEW_JUSTIFICATION = "changed suggestion review justification"

    @Autowired
    CourseRepository courseRepository

    @Autowired
    CourseExecutionRepository courseExecutionRepository

    @Autowired
    QuestionRepository questionRepository

    @Autowired
    SuggestionRepository suggestionRepository

    @Autowired
    SuggestionReviewRepository suggestionReviewRepository

    @Autowired
    SuggestionReviewService suggestionReviewService

    @Autowired
    UserRepository userRepository

    def course
    def courseExecution
    def student
    def question
    def teacher
    def suggestion
    def suggestionReview

    def setup () {
        course = new Course(COURSE_NAME, Course.Type.TECNICO)
        courseRepository.save(course)

        courseExecution = new CourseExecution(course, ACRONYM, ACADEMIC_TERM, Course.Type.TECNICO)
        courseExecutionRepository.save(courseExecution)

        student = new User(STUDENT_NAME, STUDENT_USERNAME, 1, User.Role.STUDENT)
        student.getCourseExecutions().add(courseExecution)
        courseExecution.getUsers().add(student)
        userRepository.save(student)

        teacher = new User(TEACHER_NAME, TEACHER_USERNAME, 2, User.Role.TEACHER)
        teacher.getCourseExecutions().add(courseExecution)
        courseExecution.getUsers().add(teacher)
        userRepository.save(teacher)

        question = new Question()
        question.setKey(1)
        question.setTitle(QUESTION_TITLE)
        question.setContent(QUESTION_CONTENT)
        question.setStatus(Question.Status.AVAILABLE)
        question.setNumberOfAnswers(2)
        question.setNumberOfCorrect(1)
        question.setCourse(course)
        course.addQuestion(question)
        questionRepository.save(question)

        suggestion = new Suggestion()
        suggestion.setStudent(student)
        suggestion.setStatus(Suggestion.Status.PENDING)
        suggestion.setQuestion(question)
        suggestionRepository.save(suggestion)

        suggestionReview = new SuggestionReview()
        suggestionReview.setTeacher(teacher)
        suggestionReview.setSuggestion(suggestion)
        suggestionReview.setApproved(false)
        suggestionReview.setJustification(SUGGESTION_REVIEW_JUSTIFICATION)
        suggestionReviewRepository.save(suggestionReview)
    }
    
    def "dont change suggestion review approval and change justification"() {
        given: "an updated SuggestionReviewDto"
        def suggestionReviewDto = new SuggestionReviewDto(suggestionReview)
        suggestionReviewDto.setJustification(CHANGED_SUGGESTION_REVIEW_JUSTIFICATION)

        when:
        suggestionReviewService.updateSuggestionReview(suggestionReview.getId(), suggestionReviewDto)

        then: "suggestion review must have new justification"
        suggestionReviewRepository.count() == 1L
        def result = suggestionReviewRepository.findAll().get(0)
        result.getId() != null
        result.getJustification() == CHANGED_SUGGESTION_REVIEW_JUSTIFICATION
    }

    def "change suggestion review approval to approved when suggestion is still rejected"() {
        given: "an updated SuggestionReviewDto"
        def suggestionReviewDto = new SuggestionReviewDto(suggestionReview)
        suggestionReviewDto.setApproved(true)

        when:
        suggestionReviewService.updateSuggestionReview(suggestionReview.getId(), suggestionReviewDto)

        then: "suggestion review and suggestion must be approved"
        suggestionReviewRepository.count() == 1L
        def result = suggestionReviewRepository.findAll().get(0)
        result.getId() != null
        suggestionReview.getSuggestion().getStatus() == Suggestion.Status.APPROVED
        suggestionReview.getApproved()
    }

    def "change suggestion review approval to rejected when suggestion is already approved"() {
        given: "an updated SuggestionReviewDto"
        suggestion.setStatus(Suggestion.Status.APPROVED)
        def suggestionReviewDto = new SuggestionReviewDto(suggestionReview)

        when:
        suggestionReviewService.updateSuggestionReview(suggestionReview.getId(), suggestionReviewDto)

        then: "suggestion review must be rejected and suggestion must be approved"
        suggestionReviewRepository.count() == 1L
        def result = suggestionReviewRepository.findAll().get(0)
        result.getId() != null
        suggestionReview.getSuggestion().getStatus() == Suggestion.Status.REJECTED
        !suggestionReview.getApproved()
    }

    @TestConfiguration
    static class SuggestionReviewServiceImplTestContextConfiguration {

        @Bean
        SuggestionReviewService suggestionReviewService() {
            return new SuggestionReviewService()
        }
    }
}