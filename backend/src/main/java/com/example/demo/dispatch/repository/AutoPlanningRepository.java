package com.example.demo.dispatch.repository;

import com.example.demo.dispatch.model.AutoPlanning;
import com.example.demo.dispatch.model.AutoPlanningStatus;
import com.example.demo.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface AutoPlanningRepository extends JpaRepository<AutoPlanning, Long> {
    AutoPlanning findFirstByAuthorAndPlanningDateOrderByStartedAtDesc(User author, LocalDate planningDate);

    List<AutoPlanning> findAllByStatus(AutoPlanningStatus status);

    List<AutoPlanning> findAllByAuthor_EmailAndStatus(String email, AutoPlanningStatus status);

    List<AutoPlanning> findAllByConsumedIsFalseAndAuthor_EmailAndStatusIsIn(String email, Collection<AutoPlanningStatus> status);

    AutoPlanning findFirstByAuthor_EmailAndStatusOrderByPlanningDateAsc(String email, AutoPlanningStatus status);

    Long countAllByAuthorAndStatusInAndPlanningDate(User author, Collection<AutoPlanningStatus> statuses, LocalDate planningDate);
}
