package ewm.event.repository.specification;

import ewm.event.model.Event;
import ewm.event.model.EventState;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class EventSpecifications {

    public static Specification<Event> isPublished() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("state"), EventState.PUBLISHED.toString());
    }

    public static Specification<Event> containsText(String text) {
        return (root, query, criteriaBuilder) -> {
            if (text == null || text.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String searchText = "%" + text.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("annotation")), searchText),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchText)
            );
        };
    }

    public static Specification<Event> hasCategories(List<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("category").get("id").in(categoryIds);
        };
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, criteriaBuilder) -> {
            if (paid == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("isPaid"), paid);
        };
    }

    public static Specification<Event> startsAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), dateTime);
        };
    }

    public static Specification<Event> endsBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), dateTime);
        };
    }

    public static Specification<Event> hasAvailableSlots() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("participantLimit"), 0),
                        criteriaBuilder.greaterThan(
                                root.get("participantLimit"),
                                root.get("confirmedRequests")
                        )
                );
    }

    public static Specification<Event> hasUsers(List<Long> userIds) {
        return (root, query, criteriaBuilder) -> {
            if (userIds == null || userIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("initiator").get("id").in(userIds);
        };
    }

    public static Specification<Event> hasStates(List<String> states) {
        return (root, query, criteriaBuilder) -> {
            if (states == null || states.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("state").in(states);
        };
    }
}