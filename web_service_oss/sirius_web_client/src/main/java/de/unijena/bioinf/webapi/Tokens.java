/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.webapi;

import com.auth0.jwt.interfaces.Claim;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionData;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Tokens {

    @NotNull
    public static List<Term> getAcceptedTerms(AuthService.Token token){
        Claim appMetadata = token.getDecodedAccessToken().getClaim("https://bright-giant.com/app_metadata");
        if (appMetadata.isNull())
            return List.of();

        List<String> terms = (List<String>) appMetadata.asMap().get("acceptedTerms");
        return  terms.stream().map(Term::of).collect(Collectors.toList());
//        if (rootJson == null) //M2M token, no terms needed or available. User tokens always have this claim.
//            return OAuth2TokenValidatorResult.success();
//
//        final JSONObject acceptedJson = rootJson.containsKey("terms") ? (JSONObject) rootJson.get("terms") : null;
    }

    @NotNull
    public static List<Term> getActiveSubscriptionTerms(AuthService.Token token){
        @Nullable Subscription sub = Tokens.getActiveSubscription(token);
        if (sub == null)
            return List.of();
        return List.of(Term.of(sub.getPp()), Term.of(sub.getTos()));
    }


    @NotNull
    public static Optional<SubscriptionData> getLicenseData(AuthService.Token token) {
        Claim claim = token.getDecodedAccessToken().getClaim("https://bright-giant.com/licenseData");
        return claim.isNull()
                ? Optional.empty()
                : Optional.of(claim.as(SubscriptionData.class));
    }

    @NotNull
    public static List<Subscription> getSubscriptions(AuthService.Token token) {
        return getLicenseData(token).map(SubscriptionData::getSubscriptions).orElse(List.of());
    }

    @Nullable
    public static Subscription getActiveSubscription(@NotNull List<Subscription> subs) {
        final String selectedSubscriptionKey = PropertyManager.getProperty("de.unijena.bioinf.sirius.security.subscription");
        Subscription sub = null;
        if (selectedSubscriptionKey != null && !selectedSubscriptionKey.isBlank()) {
            sub = subs.stream().filter(s -> s.getSid().equals(selectedSubscriptionKey)).findFirst()
                    .orElse(null);
        }
        if (sub == null)
            sub = subs.stream().findFirst().orElse(null);

        return sub;
    }
    @Nullable
    public static Subscription getActiveSubscription(AuthService.Token token) {
        return getActiveSubscription(getSubscriptions(token));
    }

    public static boolean hasSubscriptions(AuthService.Token token) {
        return !getSubscriptions(token).isEmpty();
    }


}
