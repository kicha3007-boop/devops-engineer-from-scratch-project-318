package io.hexlet.project_devops_deploy.specification;

import io.hexlet.project_devops_deploy.model.Bulletin;
import io.hexlet.project_devops_deploy.model.bulletin.BulletinState;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class BulletinSpecifications {

    private BulletinSpecifications() {}

    public static Specification<Bulletin> fromFilters(Map<String, String> filters) {
        Specification<Bulletin> specification = null;
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        specification = combine(specification, byState(filters.get("state")));
        specification = combine(specification, byId(filters.get("id")));
        specification =
                combine(
                        specification,
                        search(
                                filters.containsKey("search")
                                        ? filters.get("search")
                                        : filters.get("q")));

        return specification;
    }

    private static Specification<Bulletin> byState(String rawState) {
        if (!StringUtils.hasText(rawState)) {
            return null;
        }

        try {
            BulletinState state = BulletinState.valueOf(rawState.trim().toUpperCase(Locale.US));
            return (root, query, cb) -> cb.equal(root.get("state"), state);
        } catch (IllegalArgumentException ex) {
            return (root, query, cb) -> cb.disjunction();
        }
    }

    private static Specification<Bulletin> byId(String rawId) {
        if (!StringUtils.hasText(rawId)) {
            return null;
        }

        try {
            long id = Long.parseLong(rawId.trim());
            return (root, query, cb) -> cb.equal(root.get("id"), id);
        } catch (NumberFormatException ex) {
            return (root, query, cb) -> cb.disjunction();
        }
    }

    private static Specification<Bulletin> search(String term) {
        if (!StringUtils.hasText(term)) {
            return null;
        }

        String likePattern = "%" + term.trim().toLowerCase(Locale.US) + "%";
        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern),
                        cb.like(cb.lower(root.get("contact")), likePattern));
    }

    private static Specification<Bulletin> combine(
            Specification<Bulletin> base, Specification<Bulletin> addition) {
        if (addition == null) {
            return base;
        }

        return base == null ? Specification.where(addition) : base.and(addition);
    }
}
