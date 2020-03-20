package pt.ulisboa.tecnico.socialsoftware.tutor.suggestions.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.tutor.suggestions.domain.Suggestion;

import java.util.List;

@Repository
@Transactional
public interface SuggestionRepository extends JpaRepository<Suggestion, Integer> {
}