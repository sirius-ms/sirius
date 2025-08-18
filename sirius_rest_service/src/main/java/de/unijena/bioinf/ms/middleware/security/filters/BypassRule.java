package de.unijena.bioinf.ms.middleware.security.filters;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class BypassRule implements Function<HttpServletRequest, Collection<GrantedAuthority>> {
    @Nullable
    private final Predicate<HttpServletRequest> bypassRule;
    @NotNull
    private final Collection<GrantedAuthority> bypassAuthorities;

    public static BypassRule of(@Nullable Predicate<HttpServletRequest> bypassRule, @NotNull GrantedAuthority... bypassAuthorities) {
        return new BypassRule(bypassRule, Arrays.asList(bypassAuthorities));
    }

    public static BypassRule of(@Nullable Predicate<HttpServletRequest> bypassRule, @Nullable Collection<GrantedAuthority> bypassAuthorities) {
        return new BypassRule(bypassRule, bypassAuthorities);
    }

    public BypassRule(@Nullable Predicate<HttpServletRequest> bypassRule, @Nullable Collection<GrantedAuthority> bypassAuthorities) {
        this.bypassRule = bypassRule;
        this.bypassAuthorities = bypassAuthorities == null ? List.of() : Collections.unmodifiableCollection(bypassAuthorities);
    }

    @Override
    @NotNull
    public Collection<GrantedAuthority> apply(HttpServletRequest request) {
        if (bypassRule == null || !bypassRule.test(request))
            return List.of();
        return bypassAuthorities;
    }


}
