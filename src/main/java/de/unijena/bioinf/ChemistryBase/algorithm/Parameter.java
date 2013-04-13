package de.unijena.bioinf.ChemistryBase.algorithm;


/*

Auf Deutsch:
Okay, wir müssen dahin kommen das wir die Parameter eines Algorithmus extrahieren können.
Warum? Weil wir nach einer Berechnung nachvollziehen müssen welche Parameter für die Berechnung benutzt wurden
In der Theorie erstmal einfach: Ein Algorithmus hat ja nur ein paar Parameter (alpha, beta, lambda, blabla) die
wir festhalten müssen. In der Praxis experimentieren wir aber auch gerne, fügen neue Parameter ein oder einen neuen
Scorer. Ideal wäre also ein Framework, mit dem ich dokumentieren kann welche Parameter ein Algorithmus verwendet. Unterscheiden
müssen wir dabei:
    - Eingabeparameter des Algorithmus
        -> relativ einfach. Irgendwo gibt es ein CLI das diese Parameter abfragt
    - Interne oder Abgeleitete Parameter der Scorings
        -> hier der komplizierte Teil: Ein Scorer mag eigene Parameter haben. Diese können aber auch abgeleitet sein.
            z.B. könnte ein Scorer seinen Parameter x aus zwei anderen Parametern alpha und beta ableiten. Sollte dieser
            Parameter dann dokumentiert werden? Ja! Denn der Scorer könnte sich ja irgendwann ändern und seine Parameter
            anders ableiten. Lieber zu viel reporten als zu wenig!
            Beispiel: Die Standardscorer! Wir ändern immer wieder das Set an Scorern, welches benutzt wird. Diese Änderungen
            erfolgen meist nicht im CLI sondern direkt im Code. Das muss dann auch reported werden.

    Spezielle Eigenschaften von Parametern:
        - inline: Ein inline Parameter wird selbst nicht als eigener Parameter betrachtet sondern kopiert einfach seine
          Parameter in den umliegenden Scope
        - formatter: Eine Klasse die den Parameter in ein Ausgabeformat ausgibt
        - format: Ein Format-String
    Zusätzliche Annotationen:
        - called: Gibt einem Feld einen neuen Namen, der etwas natürlicher klingt (und ggf. auch UTF-8 enthalten darf)
            z.B. @called("log(lambda)") getLogLambda();

    Parameter dürfen folgende Objekte verweisen:
        - primitive
        - Wrapper über primitive
        - Strings
        - Klassen die Parameter includen
        - beliebige Objekte, wenn es einen Reporter gibt der sie in eines der obigen Objekte konvertiert
Beispiel wie das ablaufen könnte:



@Parameter(version="1.0")
class MyScorer {

    @Parameter
    double getLambda() {
        return lambda;
    }

    @Parameter
    double getLogBeta() {
        return Math.log(beta);
    }

    @Parameter(inline=true)
    Parameterset getOtherParameters() {
        return parameters;
    }


}



 */

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({FIELD, METHOD, TYPE})
public @interface Parameter {
    public String version() default "";
    public boolean inline() default false;
    public Class<? extends Formatter<Object>> formatter() default Default.class;
    public String format() default "";

    public static final class Default extends ParameterFormatter {

    }
}
